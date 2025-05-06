package mx.edu.itesca.happybox_10

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import es.dmoral.toasty.Toasty
import mx.edu.itesca.happybox_10.databinding.ActivityCartBinding

class CartActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCartBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var sharedPreferences: SharedPreferences
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val cartItems = mutableListOf<CartItem>()
    private lateinit var adapter: CartAdapter
    private lateinit var paymentSheet: PaymentSheet
    private var pendingItems: List<CartItem> = emptyList()
    private lateinit var functions: FirebaseFunctions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Stripe initialization
        PaymentConfiguration.init(applicationContext, getString(R.string.stripe_publishable_key))
        paymentSheet = PaymentSheet(this, ::onPaymentResult)
        functions = FirebaseFunctions.getInstance()

        // Toolbar & navigation setup
        setSupportActionBar(binding.appBarMain.toolbar)
        toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.appBarMain.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        // Navigation menu
        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> startActivity(Intent(this, Principal::class.java))
                R.id.nav_purchases -> startActivity(Intent(this, PurchasesActivity::class.java))
                R.id.nav_logout -> logout()
            }
            true
        }

        // RecyclerView setup
        adapter = CartAdapter(
            cartItems,
            onQuantityChange = { item, delta -> updateQuantity(item, delta) },
            onItemRemove = { item -> removeItem(item) },
            onSelectionChange = { updateTotal() }
        )
        binding.rvCart.layoutManager = LinearLayoutManager(this)
        binding.rvCart.adapter = adapter

        // Load cart items
        loadCartItems()
        binding.checkoutButton.setOnClickListener { checkoutCart() }
    }

    private fun loadCartItems() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("Usuarios").document(uid)
            .collection("Carrito")
            .addSnapshotListener { snaps, _ ->
                cartItems.clear()
                snaps?.documents?.forEach { doc ->
                    doc.toObject(CartItem::class.java)?.copy(id = doc.id)?.let { cartItems.add(it) }
                }
                adapter.notifyDataSetChanged()
                toggleEmptyState()
                updateTotal()
            }
    }

    private fun updateQuantity(item: CartItem, delta: Int) {
        val uid = auth.currentUser?.uid ?: return
        val newQty = (item.quantity + delta).coerceIn(1, 10)
        db.collection("Usuarios").document(uid)
            .collection("Carrito").document(item.id)
            .update("quantity", newQty)
    }

    private fun removeItem(item: CartItem) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("Usuarios").document(uid)
            .collection("Carrito").document(item.id)
            .delete()
    }

    private fun toggleEmptyState() {
        val empty = cartItems.isEmpty()
        binding.rvCart.visibility = if (empty) View.GONE else View.VISIBLE
        binding.emptyCartMessage.visibility = if (empty) View.VISIBLE else View.GONE
        binding.checkoutButton.isEnabled = !empty
    }

    private fun updateTotal() {
        val selectedItems = cartItems.filter { it.selected }
        val total = selectedItems.sumOf { it.price * it.quantity }
        binding.cartTotal.text = "Total: $${"%.2f".format(total)}"
        binding.checkoutButton.isEnabled = selectedItems.isNotEmpty()
    }

    // Antes de procesar el pago, verifica stock
    private fun validateStockBeforePayment(items: List<CartItem>, callback: (Boolean, String?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val productRefs = items.map { db.collection("Productos").document(it.productId) }

        // Usamos Transaction para verificar el stock atómicamente
        db.runTransaction { transaction ->
            val outOfStockItems = mutableListOf<String>()

            productRefs.forEach { docRef ->
                val document = transaction.get(docRef)
                val productId = docRef.id
                val item = items.find { it.productId == productId } ?: return@runTransaction

                val stock = document.getLong("stockProducto") ?: 0
                if (stock < item.quantity) {
                    outOfStockItems.add(item.name)
                }
            }

            if (outOfStockItems.isNotEmpty()) {
                throw Exception("Productos sin stock suficiente: ${outOfStockItems.joinToString()}")
            }

            // Si llegamos aquí, todo el stock está disponible
            true
        }.addOnSuccessListener {
            callback(true, null)
        }.addOnFailureListener { e ->
            callback(false, e.message ?: "Error al verificar stock")
        }
    }

    // Luego modifica checkoutCart():
    private fun checkoutCart() {
        if (!isNetworkAvailable()) {
            Toasty.error(this, "No hay conexión a internet", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser ?: run {
            showLoginRequired()
            return
        }

        pendingItems = cartItems.filter { it.selected }
        if (pendingItems.isEmpty()) {
            Toasty.info(this, "Selecciona productos", Toast.LENGTH_SHORT).show()
            return
        }

        validateStockBeforePayment(pendingItems) { success, message ->
            if (!success) {
                message?.let { Toasty.error(this, it, Toast.LENGTH_LONG).show() }
                return@validateStockBeforePayment
            }

            user.getIdToken(true).addOnSuccessListener {
                processPayment(user)
            }.addOnFailureListener { e ->
                handleAuthError(e)
            }
        }
    }

    private fun refreshTokenWithRetry(user: FirebaseUser, retries: Int = 2) {
        user.getIdToken(true).addOnCompleteListener { tokenTask ->
            if (tokenTask.isSuccessful) {
                // Verificar items seleccionados
                pendingItems = cartItems.filter { it.selected }
                if (pendingItems.isEmpty()) {
                    Toasty.info(this, "Selecciona productos", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }
                // Procesar pago
                processPayment(user)
            } else {
                if (retries > 0) {
                    Toasty.warning(this, "Reintentando autenticación...", Toast.LENGTH_SHORT).show()
                    refreshTokenWithRetry(user, retries - 1)
                } else {
                    handleAuthError(tokenTask.exception)
                }
            }
        }
    }

    private fun processPayment(user: FirebaseUser) {
        val amount = (pendingItems.sumOf { it.price * it.quantity } * 100).toInt()
        Log.d("PaymentFlow", "Iniciando pago. Monto calculado: $amount")

        val data = hashMapOf(
            "amount" to amount,
            "currency" to "mxn",
            "userId" to user.uid
        )

        // Verificar token de autenticación
        user.getIdToken(false).addOnCompleteListener { tokenTask ->
            if (tokenTask.isSuccessful) {
                val token = tokenTask.result?.token
                Log.d("AuthToken", "Token ID: ${token?.take(10)}...") // Log parcial por seguridad
            } else {
                Log.e("AuthToken", "Error al obtener token", tokenTask.exception)
            }
        }

        functions.getHttpsCallable("createPaymentIntent")
            .call(data)
            .addOnSuccessListener { result ->
                Log.d("PaymentFlow", "Respuesta de createPaymentIntent recibida: ${result.data}")

                try {
                    val response = result.data as? Map<*, *>
                    Log.d("PaymentFlow", "Respuesta parseada: $response")

                    val clientSecret = response?.get("clientSecret") as? String
                    if (clientSecret != null) {
                        Log.d("PaymentFlow", "ClientSecret obtenido. Iniciando PaymentSheet...")
                        paymentSheet.presentWithPaymentIntent(
                            clientSecret,
                            PaymentSheet.Configuration(
                                merchantDisplayName = "HappyBox",
                                allowsDelayedPaymentMethods = false
                            )
                        )
                    } else {
                        Log.e("PaymentFlow", "ClientSecret no encontrado en la respuesta")
                        showPaymentError("Error en la configuración del pago")
                    }
                } catch (e: Exception) {
                    Log.e("PaymentFlow", "Error al procesar respuesta", e)
                    showPaymentError("Error al procesar la respuesta del servidor")
                }
            }
            .addOnFailureListener { e ->
                Log.e("PaymentFlow", "Error en createPaymentIntent", e)

                when {
                    e is FirebaseFunctionsException && e.code == FirebaseFunctionsException.Code.UNAUTHENTICATED -> {
                        Log.e("PaymentFlow", "Error de autenticación", e)
                        handleAuthError(e)
                    }
                    e.message?.contains("PERMISSION_DENIED") == true -> {
                        Log.e("PaymentFlow", "Permisos insuficientes", e)
                        showPaymentError("Permisos insuficientes. Contacta al soporte.")
                    }
                    else -> {
                        showPaymentError(e.message ?: "Error desconocido al procesar pago")
                    }
                }
            }
    }

    private fun handleAuthError(exception: Exception?) {
        Toasty.error(this, "Error de autenticación: ${exception?.message ?: "Intenta de nuevo"}", Toast.LENGTH_LONG).show()
        // No desloguear al usuario automáticamente
        // auth.signOut()
        // startActivity(Intent(this, LoginActivity::class.java).apply {
        //     flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        // })
    }

    private fun onPaymentResult(result: PaymentSheetResult) {
        when (result) {
            is PaymentSheetResult.Completed -> {
                Log.d("PaymentFlow", "Pago completado con éxito")
                processSuccessfulPayment()
            }
            is PaymentSheetResult.Canceled -> {
                Log.d("PaymentFlow", "Pago cancelado por el usuario")
                Toasty.info(this, "Pago cancelado", Toast.LENGTH_SHORT).show()
            }
            is PaymentSheetResult.Failed -> {
                Log.e("PaymentFlow", "Error en el pago: ${result.error}", result.error)
                Toasty.error(
                    this,
                    "Error en el pago: ${result.error.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun processSuccessfulPayment() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Confirmando compra...")
            setCancelable(false)
            show()
        }

        val uid = auth.currentUser?.uid ?: run {
            progressDialog.dismiss()
            Toasty.error(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        // Prepara los datos para la función
        val data = hashMapOf(
            "items" to pendingItems.map {
                mapOf(
                    "id" to it.id,
                    "productId" to it.productId,
                    "name" to it.name,
                    "price" to it.price,
                    "quantity" to it.quantity
                )
            },
            "paymentIntentId" to "ID_DEL_PAGO" // Debes obtener este ID del resultado de Stripe
        )

        functions.getHttpsCallable("confirmPurchase")
            .call(data)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toasty.success(this, "Compra completada!", Toast.LENGTH_SHORT).show()
                pendingItems = emptyList()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Log.e("Purchase", "Error al confirmar", e)
                Toasty.error(this, "Error al confirmar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showLoginRequired() {
        AlertDialog.Builder(this)
            .setTitle("Sesión requerida")
            .setMessage("Debes iniciar sesión para realizar compras")
            .setPositiveButton("Iniciar sesión") { _, _ ->
                startActivity(Intent(this, LoginActivity::class.java))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showPaymentError(message: String) {
        Toasty.error(this, "Error en pago: $message", Toast.LENGTH_LONG).show()
        Log.e("CartActivity", "Payment error: $message")
    }

    private fun logout() {
        auth.signOut()
        sharedPreferences.edit().clear().apply()
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }


    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }
}
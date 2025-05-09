package mx.edu.itesca.happybox_10

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import android.widget.RatingBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import mx.edu.itesca.happybox_10.databinding.ActivityProductDetailBinding

class ProductDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProductDetailBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions
    private lateinit var paymentSheet: PaymentSheet

    private var quantity = 1
    private lateinit var reviewsAdapter: ReviewsAdapter
    private val reviewsList = mutableListOf<Review>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Stripe initialization
        PaymentConfiguration.init(applicationContext, getString(R.string.stripe_publishable_key))
        paymentSheet = PaymentSheet(this, ::onPaymentResult)
        functions = FirebaseFunctions.getInstance()

        // Toolbar + Drawer setup
        setSupportActionBar(binding.appBarMain.toolbar)
        drawerLayout = binding.drawerLayout
        toggle = ActionBarDrawerToggle(
            this, drawerLayout, binding.appBarMain.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Firebase initialization
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupNavMenu()

        // Get product data from intent
        val productId = intent.getStringExtra("productId") ?: return
        val productImages = intent.getStringArrayListExtra("productImages")
            ?: arrayListOf(intent.getStringExtra("productImage")!!)
        val productName = intent.getStringExtra("productName") ?: ""
        val productPrice = intent.getDoubleExtra("productPrice", 0.0)
        val productDesc = intent.getStringExtra("productDescription") ?: ""

        // Configure image slider
        binding.imageSlider.adapter = ImageSliderAdapter(productImages)
        autoScrollSlider(binding.imageSlider)

        // Set product data
        binding.productName.text = productName
        binding.productPrice.text = "$${"%.2f".format(productPrice)}"
        binding.productDescription.text = productDesc
        binding.quantityText.text = quantity.toString()

        // Quantity controls
        binding.decreaseButton.setOnClickListener {
            if (quantity > 1) binding.quantityText.text = (--quantity).toString()
        }
        binding.increaseButton.setOnClickListener {
            binding.quantityText.text = (++quantity).toString()
        }

        // Purchase/Cart buttons
        binding.buyNowButton.setOnClickListener {
            if (auth.currentUser == null) {
                Toast.makeText(this, "Debes iniciar sesión para comprar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            processPurchase(productId, quantity, productPrice)
        }
        binding.addToCartButton.setOnClickListener {
            addToCart(productId, productImages, productName, productPrice, quantity)
        }

        // Reviews setup
        setupReviewsRecyclerView()
        loadReviews(productId)
        checkUserReview(productId)
        binding.btnAddReview.setOnClickListener { showReviewDialog(productId) }

        // Search and cart buttons in toolbar
        setupStaticToolbarButtons()

        // Window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }
    }

    private fun addToCart(productId: String, productImages: List<String>, productName: String,
                          productPrice: Double, quantity: Int) {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Debes iniciar sesión para agregar al carrito", Toast.LENGTH_SHORT).show()
            return
        }

        val imageUrl = productImages.firstOrNull()?.toString() ?: ""

        val cartItem = hashMapOf(
            "productId" to productId,
            "name" to productName,
            "price" to productPrice,
            "quantity" to quantity,
            "imageUrl" to imageUrl,
            "selected" to false
        )

        db.collection("Usuarios").document(uid)
            .collection("Carrito")
            .whereEqualTo("productId", productId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    if (quantity > 10) {
                        Toast.makeText(this, "Máximo 10 unidades por producto", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    db.collection("Usuarios").document(uid)
                        .collection("Carrito")
                        .add(cartItem)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Producto agregado al carrito", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    val doc = documents.documents[0]
                    val currentQty = doc.getLong("quantity")?.toInt() ?: 0
                    if (currentQty + quantity > 10) {
                        Toast.makeText(this, "Límite de 10 unidades alcanzado", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    db.collection("Usuarios").document(uid)
                        .collection("Carrito")
                        .document(doc.id)
                        .update("quantity", FieldValue.increment(quantity.toLong()))
                        .addOnSuccessListener {
                            Toast.makeText(this, "Cantidad actualizada en carrito", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    private fun processPurchase(productId: String, quantity: Int, price: Double) {
        val amount = (price * quantity * 100).toInt()
        functions.getHttpsCallable("createPaymentIntent")
            .call(mapOf("amount" to amount))
            .addOnSuccessListener { res ->
                val clientSecret = (res.data as Map<*, *>)["clientSecret"] as String
                paymentSheet.presentWithPaymentIntent(
                    clientSecret,
                    PaymentSheet.Configuration(
                        merchantDisplayName = "HappyBox",
                        allowsDelayedPaymentMethods = false
                    )
                )
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al procesar pago: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun onPaymentResult(result: PaymentSheetResult) {
        when (result) {
            is PaymentSheetResult.Completed -> {
                val productId = intent.getStringExtra("productId") ?: return
                val quantity = quantity
                db.collection("Productos").document(productId)
                    .update("stockProducto", FieldValue.increment(-quantity.toLong()))
                    .addOnSuccessListener {
                        Toast.makeText(this, "Compra exitosa! Stock actualizado", Toast.LENGTH_SHORT).show()
                    }
            }
            is PaymentSheetResult.Canceled -> {
                Toast.makeText(this, "Pago cancelado", Toast.LENGTH_SHORT).show()
            }
            is PaymentSheetResult.Failed -> {
                Toast.makeText(this, "Error en el pago: ${result.error}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupNavMenu() {
        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> navigateTo(Principal::class.java)
                R.id.nav_regalos -> navigateToFragment("regalos")
                R.id.nav_detalles -> navigateToFragment("detalles")
                R.id.nav_peluches -> navigateToFragment("peluches")
                R.id.nav_tazas -> navigateToFragment("tazas")
                R.id.nav_globos -> navigateToFragment("globos")
                R.id.nav_about -> navigateTo(AboutActivity::class.java)
                R.id.nav_logout -> logout()
                else -> false
            }
        }
    }

    private fun setupStaticToolbarButtons() {
        val searchView = binding.appBarMain.toolbar.findViewById<SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true.also {
                Toast.makeText(this@ProductDetailActivity, "Búsqueda no funcional aún", Toast.LENGTH_SHORT).show()
            }
            override fun onQueryTextChange(newText: String?) = true
        })
        binding.appBarMain.toolbar.findViewById<View>(R.id.cartButton)
            .setOnClickListener { startActivity(Intent(this, CartActivity::class.java)) }
    }

    private fun setupReviewsRecyclerView() {
        binding.rvReviews.apply {
            layoutManager = LinearLayoutManager(this@ProductDetailActivity)
            reviewsAdapter = ReviewsAdapter(reviewsList)
            adapter = reviewsAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun loadReviews(productId: String) {
        db.collection("Productos").document(productId)
            .collection("Reviews")
            .orderBy("fechaReview", Query.Direction.DESCENDING)
            .addSnapshotListener { snaps, err ->
                if (err != null) {
                    Log.e("ProductDetail", "Error loading reviews", err)
                    return@addSnapshotListener
                }

                reviewsList.clear()
                snaps?.documents?.forEach { doc ->
                    doc.toObject(Review::class.java)?.copy(id = doc.id)?.let {
                        reviewsList.add(it)
                        if (it.idUsuario.isNotEmpty()) loadUserName(it.idUsuario) { name ->
                            it.nombreUsuario = name
                            reviewsAdapter.notifyDataSetChanged()
                        }
                    }
                }
                updateReviewsUI(reviewsList.isNotEmpty())
                reviewsAdapter.notifyDataSetChanged()
            }
    }

    private fun checkUserReview(productId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("Productos").document(productId)
            .collection("Reviews")
            .whereEqualTo("idUsuario", uid)
            .get().addOnSuccessListener { if (!it.isEmpty) {
                binding.btnAddReview.isEnabled = false
                binding.btnAddReview.text = "Ya has reseñado"
            }}
    }

    private fun updateReviewsUI(hasReviews: Boolean) {
        if (hasReviews) {
            val avg = reviewsList.map { it.ratingFloat() }.average().toFloat()
            binding.ratingBar.rating = avg
            binding.tvAvgRating.text = "%.1f".format(avg)
            binding.tvReviewCount.text = "(${reviewsList.size} reseñas)"

            // Mostrar elementos de rating
            binding.ratingBar.visibility = View.VISIBLE
            binding.tvAvgRating.visibility = View.VISIBLE
            binding.tvReviewCount.visibility = View.VISIBLE

            // Mostrar sección de reseñas con contenido
            binding.reviewsSection.visibility = View.VISIBLE
            binding.tvReviewsTitle.visibility = View.VISIBLE
            binding.rvReviews.visibility = View.VISIBLE
            binding.tvNoReviews.visibility = View.GONE
        } else {
            // Ocultar elementos de rating
            binding.ratingBar.visibility = View.GONE
            binding.tvAvgRating.visibility = View.GONE
            binding.tvReviewCount.visibility = View.GONE

            // Mostrar sección de reseñas con mensaje de vacío
            binding.reviewsSection.visibility = View.VISIBLE
            binding.tvReviewsTitle.visibility = View.VISIBLE
            binding.rvReviews.visibility = View.GONE
            binding.tvNoReviews.visibility = View.VISIBLE
        }
    }

    private fun loadUserName(userId: String, cb: (String) -> Unit) {
        db.collection("Usuarios").document(userId).get()
            .addOnSuccessListener { cb(it.getString("nombreUsuario") ?: "") }
            .addOnFailureListener { cb("") }
    }

    private fun showReviewDialog(productId: String) {
        if (auth.currentUser == null) {
            Toast.makeText(this, "Debes iniciar sesión para dejar una reseña", Toast.LENGTH_SHORT).show()
            return
        }
        val view = layoutInflater.inflate(R.layout.dialog_add_review, null)
        val rb = view.findViewById<RatingBar>(R.id.dialogRatingBar)
        val et = view.findViewById<EditText>(R.id.dialogComment)

        AlertDialog.Builder(this)
            .setTitle("Tu reseña")
            .setView(view)
            .setPositiveButton("Enviar") { _, _ ->
                val rating = rb.rating.toInt().coerceIn(1, 5)
                val comment = et.text.toString().trim()
                if (comment.isEmpty()) return@setPositiveButton Toast.makeText(this, "Escribe un comentario", Toast.LENGTH_SHORT).show()
                val uid = auth.currentUser!!.uid
                val name = sharedPreferences.getString("nombreUsuario", "") ?: ""
                val data = hashMapOf(
                    "calificacionReview" to rating.toString(),
                    "comentarioReview" to comment,
                    "fechaReview" to Timestamp.now(),
                    "nombreUsuario" to name,
                    "idUsuario" to uid
                )
                db.collection("Productos").document(productId)
                    .collection("Reviews").add(data)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Reseña agregada", Toast.LENGTH_SHORT).show()
                        binding.btnAddReview.isEnabled = false
                        binding.btnAddReview.text = "Ya has reseñado"
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun navigateTo(act: Class<*>) = startActivity(Intent(this, act)).let { finish(); true }
    private fun navigateToFragment(tag: String) = Intent(this, Principal::class.java).apply { putExtra("fragment", tag) }.also { startActivity(it); finish() }.let { true }
    private fun logout() = auth.signOut().let { sharedPreferences.edit().clear().apply() }.let {
        startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish(); true
    }

    private fun autoScrollSlider(pager: ViewPager2) {
        val handler = android.os.Handler(mainLooper)
        val task = object : Runnable { override fun run() { pager.currentItem = (pager.currentItem + 1) % (pager.adapter?.itemCount ?: 1); handler.postDelayed(this, 3000) } }
        handler.postDelayed(task, 3000)
    }


}
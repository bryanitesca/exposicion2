package mx.edu.itesca.happybox_10

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import mx.edu.itesca.happybox_10.databinding.ActivityCartBinding

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var sharedPreferences: SharedPreferences
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val cartItems = mutableListOf<CartItem>()
    private lateinit var adapter: CartAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbarAndNav()
        setupRecyclerView()
        loadCartItems()

        binding.checkoutButton.setOnClickListener { checkoutCart() }

        // Asegurarse de que el RecyclerView sea visible cuando hay items
        binding.rvCart.visibility = if (cartItems.isEmpty()) View.GONE else View.VISIBLE
        binding.emptyCartMessage.visibility = if (cartItems.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun setupToolbarAndNav() {
        setSupportActionBar(binding.appBarMain.toolbar)
        drawerLayout = binding.drawerLayout
        toggle = ActionBarDrawerToggle(
            this, drawerLayout, binding.appBarMain.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> startActivity(Intent(this, Principal::class.java))
                R.id.nav_logout -> logout()
            }
            true
        }
    }

    private fun setupRecyclerView() {
        adapter = CartAdapter(cartItems,
            onQuantityChange = { item, delta -> updateQuantity(item, delta) },
            onItemRemove = { item -> removeItem(item) }
        )

        binding.rvCart.apply {
            layoutManager = LinearLayoutManager(this@CartActivity)
            adapter = this@CartActivity.adapter
        }
    }

    private fun loadCartItems() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("Usuarios").document(uid)
            .collection("Carrito")
            .addSnapshotListener { snaps, err ->
                if (err != null) {
                    Log.e("CartActivity", "Error loading cart", err)
                    return@addSnapshotListener
                }

                cartItems.clear()
                snaps?.documents?.forEach { doc ->
                    val item = doc.toObject(CartItem::class.java)?.copy(id = doc.id)
                    Log.d("CART_ITEM", "Loaded item: ${item?.name} - ${item?.imageUrl}") // Para debug
                    item?.let { cartItems.add(it) }
                }

                adapter.notifyDataSetChanged()
                updateTotal()
                updateEmptyState()
            }
    }

    private fun updateQuantity(item: CartItem, delta: Int) {
        val uid = auth.currentUser?.uid ?: return
        val newQty = (item.quantity + delta).coerceAtLeast(1)

        if (newQty == 0) {
            removeItem(item)
            return
        }

        db.collection("Usuarios").document(uid)
            .collection("Carrito").document(item.id)
            .update("quantity", newQty)
            .addOnFailureListener { e ->
                Log.e("CartActivity", "Error updating quantity", e)
                Toast.makeText(this, "Error actualizando cantidad", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeItem(item: CartItem) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("Usuarios").document(uid)
            .collection("Carrito").document(item.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Producto eliminado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("CartActivity", "Error removing item", e)
                Toast.makeText(this, "Error eliminando producto", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateTotal() {
        val total = cartItems.sumOf { it.price * it.quantity }
        binding.cartTotal.text = "Total: $${"%.2f".format(total)}"
    }

    private fun updateEmptyState() {
        binding.rvCart.visibility = if (cartItems.isEmpty()) View.GONE else View.VISIBLE
        binding.emptyCartMessage.visibility = if (cartItems.isEmpty()) View.VISIBLE else View.GONE
        binding.checkoutButton.isEnabled = cartItems.isNotEmpty()
    }

    private fun checkoutCart() {
        val uid = auth.currentUser?.uid ?: return
        val total = cartItems.sumOf { it.price * it.quantity }

        val sale = hashMapOf(
            "userId" to uid,
            "date" to Timestamp.now(),
            "total" to total,
            "status" to "pending"
        )

        db.collection("Ventas").add(sale)
            .addOnSuccessListener { doc ->
                // Aquí puedes agregar los items de la venta como subcolección
                Toast.makeText(this, "Compra realizada", Toast.LENGTH_SHORT).show()
                // Limpiar el carrito después de la compra
                clearCart()
            }
            .addOnFailureListener { e ->
                Log.e("CartActivity", "Error during checkout", e)
                Toast.makeText(this, "Error al procesar compra", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearCart() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("Usuarios").document(uid)
            .collection("Carrito")
            .get()
            .addOnSuccessListener { docs ->
                docs.forEach { doc ->
                    doc.reference.delete()
                }
            }
    }

    private fun logout() {
        auth.signOut()
        sharedPreferences.edit().clear().apply()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
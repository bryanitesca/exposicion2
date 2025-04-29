package mx.edu.itesca.happybox_10

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.navigation.NavigationView
import mx.edu.itesca.happybox_10.databinding.ActivityCartBinding

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var sharedPreferences: SharedPreferences
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)
        drawerLayout = binding.drawerLayout
        toggle = ActionBarDrawerToggle(
            this, drawerLayout, binding.appBarMain.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        val navView: NavigationView = binding.navView
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home     -> navigateTo(Principal::class.java)
                R.id.nav_regalos  -> navigateToFragment("regalos")
                R.id.nav_detalles -> navigateToFragment("detalles")
                R.id.nav_peluches -> navigateToFragment("peluches")
                R.id.nav_tazas    -> navigateToFragment("tazas")
                R.id.nav_globos   -> navigateToFragment("globos")
                R.id.nav_about    -> navigateTo(AboutActivity::class.java)
                R.id.nav_logout   -> { logout(); true }
                else              -> false
            }
        }

        // Header clicks
        val header = navView.getHeaderView(0)
        header.findViewById<android.widget.ImageView>(R.id.userImageView).setOnClickListener {
            startActivity(Intent(this, UserDetailsActivity::class.java)); finish()
        }
        header.findViewById<android.widget.TextView>(R.id.userDetailsTextView).setOnClickListener {
            startActivity(Intent(this, UserDetailsActivity::class.java)); finish()
        }

        // Bind cart item
        val pid    = intent.getStringExtra("productId") ?: ""
        val imgs   = intent.getStringArrayListExtra("productImages") ?: listOf<String>()
        val name   = intent.getStringExtra("productName") ?: ""
        val price  = intent.getDoubleExtra("productPrice", 0.0)
        val qty    = intent.getIntExtra("quantity", 1)
        val total  = price * qty

        Glide.with(this).load(imgs.firstOrNull()).centerCrop().into(binding.cartProductImage)
        binding.cartProductName.text = name
        binding.cartProductDescription.text = ""
        binding.cartProductPrice.text = "$${"%.2f".format(total)} (x$qty)"

        binding.checkoutButton.setOnClickListener { processCheckout(pid, qty, price) }

        // Search stub
        binding.appBarMain.toolbar.findViewById<SearchView>(R.id.searchView)
            .setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(q: String?) = true.also {
                    Toast.makeText(this@CartActivity, "Búsqueda no funcional aún", Toast.LENGTH_SHORT).show()
                }
                override fun onQueryTextChange(t: String?) = true
            })

        // Cart button
        binding.appBarMain.toolbar.findViewById<android.widget.ImageButton>(R.id.cartButton)
            .setOnClickListener { /* ya estás aquí */ }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }
    }

    private fun navigateTo(activity: Class<*>): Boolean {
        startActivity(Intent(this, activity))
        finish()
        return true
    }

    private fun navigateToFragment(tag: String): Boolean {
        Intent(this, Principal::class.java).apply {
            putExtra("fragment", tag)
        }.also { startActivity(it); finish() }
        return true
    }

    private fun logout() {
        auth.signOut()
        sharedPreferences.edit().clear().apply()
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    private fun processCheckout(pid: String, qty: Int, price: Double) {
        val uid = auth.currentUser?.uid ?: return Toast.makeText(this, "Inicia sesión primero", Toast.LENGTH_SHORT).show()
        db.runTransaction { tx ->
            val ref = db.collection("Productos").document(pid)
            val prod = tx.get(ref).toObject(Product::class.java)
                ?: throw Exception("Producto no encontrado")
            if (prod.stockProducto < qty) throw Exception("Stock insuficiente")
            tx.update(ref, "stockProducto", prod.stockProducto - qty)
            val ventaRef = db.collection("Ventas").document()
            tx.set(ventaRef, mapOf(
                "idUsuario" to uid,
                "fechaCompra" to Timestamp.now(),
                "totalCompra" to price * qty,
                "estatusCompra" to "pendiente",
                "direccionEntregaCompra" to sharedPreferences.getString("direccionUsuario",""),
                "metodoPago" to "pendiente"
            ))
            tx.set(ventaRef.collection("DetalleVenta").document(), mapOf(
                "idProducto" to pid,
                "precioProducto" to price,
                "cantidadProducto" to qty
            ))
        }.addOnSuccessListener {
            Toast.makeText(this, "Compra exitosa!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

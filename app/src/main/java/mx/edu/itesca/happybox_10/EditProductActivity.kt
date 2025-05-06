package mx.edu.itesca.happybox_10

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import es.dmoral.toasty.Toasty

class EditProductActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var spCategory: Spinner
    private lateinit var etSearchName: EditText
    private lateinit var btnSearch: Button
    private lateinit var rvProducts: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private val productList = mutableListOf<Product>()
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_product)

        // Configurar la Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Configurar DrawerLayout y NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        // Configurar ActionBarDrawerToggle
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Configurar visibilidad de opciones de administrador
        val menu = navView.menu
        val isAdmin = true // Cambia según tu lógica de autenticación
        menu.findItem(R.id.nav_add_product).isVisible = isAdmin
        menu.findItem(R.id.nav_edit_product).isVisible = isAdmin
        menu.findItem(R.id.nav_delete_product).isVisible = isAdmin

        // Manejar clics en el menú
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, Principal::class.java))
                    finish()
                }
                R.id.nav_regalos -> {
                    startActivity(Intent(this, Principal::class.java).putExtra("section", "regalos"))
                    finish()
                }
                R.id.nav_detalles -> {
                    startActivity(Intent(this, Principal::class.java).putExtra("section", "detalles"))
                    finish()
                }
                R.id.nav_peluches -> {
                    startActivity(Intent(this, Principal::class.java).putExtra("section", "peluches"))
                    finish()
                }
                R.id.nav_tazas -> {
                    startActivity(Intent(this, Principal::class.java).putExtra("section", "tazas"))
                    finish()
                }
                R.id.nav_globos -> {
                    startActivity(Intent(this, Principal::class.java).putExtra("section", "globos"))
                    finish()
                }
                R.id.nav_add_product -> {
                    startActivity(Intent(this, AddProductActivity::class.java))
                    finish()
                }
                R.id.nav_edit_product -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.nav_delete_product -> {
                    startActivity(Intent(this, DeleteProductActivity::class.java))
                    finish()
                }
                R.id.nav_about -> {
                    startActivity(Intent(this, Principal::class.java).putExtra("section", "about"))
                    finish()
                }
                R.id.nav_logout -> {
                    // Implementar lógica de cierre de sesión
                    finish()
                }
            }
            true
        }

        // Configurar vistas
        spCategory = findViewById(R.id.spCategory)
        etSearchName = findViewById(R.id.etName)
        btnSearch = findViewById(R.id.btnSearch)
        rvProducts = findViewById(R.id.rvProducts)

        spCategory.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Detalles", "Regalos", "Tazas", "Peluches", "Globos")
        )

        productAdapter = ProductAdapter(productList).apply {
            setOnItemClickListener { product ->
                val intent = Intent(this@EditProductActivity, AddProductActivity::class.java).apply {
                    putExtra("mode", "edit")
                    putExtra("productId", product.id)
                }
                startActivity(intent)
            }
        }

        rvProducts.layoutManager = LinearLayoutManager(this)
        rvProducts.adapter = productAdapter

        btnSearch.setOnClickListener { searchProducts() }
    }

    private fun searchProducts() {
        val tipo = spCategory.selectedItem as String
        val nombre = etSearchName.text.toString().trim()
        if (nombre.isEmpty()) {
            Toasty.info(this, "Introduce un nombre para buscar", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("Productos")
            .whereEqualTo("tipoProducto", tipo)
            .whereGreaterThanOrEqualTo("nombreProducto", nombre)
            .whereLessThanOrEqualTo("nombreProducto", nombre + "\uf8ff")
            .get()
            .addOnSuccessListener { snap ->
                productList.clear()
                snap.documents.forEach { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)?.let {
                        productList.add(it)
                    }
                }
                productAdapter.notifyDataSetChanged()
                if (productList.isEmpty()) {
                    Toasty.info(this, "No se encontraron productos", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toasty.error(this, "Error al buscar productos", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
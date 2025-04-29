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
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import mx.edu.itesca.happybox_10.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var sharedPreferences: SharedPreferences
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar + Drawer
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
        val menu = navView.menu

        // Mostrar opción “Agregar Producto” solo si es Administrador
        db.collection("Usuarios").document(auth.currentUser!!.uid).get()
            .addOnSuccessListener { doc ->
                if (doc.getString("rolUsuario") == "Administrador") {
                    // hacer visible el grupo y el ítem
                    menu.setGroupVisible(R.id.group_admin, true)
                    menu.findItem(R.id.nav_add_product).isVisible = true
                }
            }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home        -> navigateTo(Principal::class.java)
                R.id.nav_detalles    -> navigateToFragment("detalles")
                R.id.nav_regalos     -> navigateToFragment("regalos")
                R.id.nav_peluches    -> navigateToFragment("peluches")
                R.id.nav_tazas       -> navigateToFragment("tazas")
                R.id.nav_globos      -> navigateToFragment("globos")
                R.id.nav_about       -> { drawerLayout.closeDrawers(); true }
                R.id.nav_add_product -> {
                    startActivity(Intent(this, AddProductActivity::class.java))
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_logout      -> {
                    auth.signOut()
                    sharedPreferences.edit().clear().apply()
                    startActivity(
                        Intent(this, LoginActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    finish()
                    true
                }
                else -> false
            }
        }

        // cabecera clickable
        val header = navView.getHeaderView(0)
        header.findViewById<android.widget.ImageView>(R.id.userImageView)
            .setOnClickListener {
                startActivity(Intent(this, UserDetailsActivity::class.java)); finish()
            }
        header.findViewById<android.widget.TextView>(R.id.userDetailsTextView)
            .setOnClickListener {
                startActivity(Intent(this, UserDetailsActivity::class.java)); finish()
            }

        // SearchView (stub)
        binding.appBarMain.toolbar.findViewById<SearchView>(R.id.searchView)
            .setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(q: String?) = true.also {
                    Toast.makeText(this@AboutActivity, "Búsqueda no funcional aún", Toast.LENGTH_SHORT).show()
                }
                override fun onQueryTextChange(t: String?) = true
            })

        // Botón carrito
        binding.appBarMain.toolbar.findViewById<android.widget.ImageButton>(R.id.cartButton)
            .setOnClickListener {
                startActivity(Intent(this, CartActivity::class.java))
            }

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
}

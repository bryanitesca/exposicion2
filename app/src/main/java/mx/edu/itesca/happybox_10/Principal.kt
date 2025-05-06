package mx.edu.itesca.happybox_10

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.*
import com.google.android.material.navigation.NavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import mx.edu.itesca.happybox_10.databinding.ActivityPrincipalBinding

class Principal : AppCompatActivity() {
    private lateinit var binding: ActivityPrincipalBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var navController: NavController
    private lateinit var appBarConfig: AppBarConfiguration
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPrincipalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)
        drawerLayout = binding.drawerLayout
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        // Drawer toggle
        toggle = ActionBarDrawerToggle(this, drawerLayout, binding.appBarMain.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawerLayout.addDrawerListener(toggle); toggle.syncState()

        // NavController
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        navController = navHost.navController
        appBarConfig = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_regalos, R.id.nav_detalles,
                R.id.nav_peluches, R.id.nav_tazas, R.id.nav_globos),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfig)
        binding.navView.setupWithNavController(navController)

        // Dynamic Admin menu
        db.collection("Usuarios").document(auth.currentUser!!.uid).get()
            .addOnSuccessListener { doc ->
                if (doc.getString("rolUsuario") == "Administrador") {
                    // Agregar "Agregar Producto"
                    binding.navView.menu.add(
                        R.id.group_admin, // define <group android:id="@+id/group_admin"/> en tu menu XML
                        R.id.nav_add_product, // define este id en res/values/ids.xml
                        Menu.NONE,
                        "Agregar Producto"
                    ).setIcon(R.drawable.agregar)
                        .setOnMenuItemClickListener {
                            startActivity(Intent(this, AddProductActivity::class.java))
                            drawerLayout.closeDrawers()
                            true
                        }

                    // Agregar "Editar Producto"
                    binding.navView.menu.add(
                        R.id.group_admin,
                        R.id.nav_edit_product,
                        Menu.NONE,
                        "Editar Producto"
                    ).setIcon(R.drawable.edit) // Asegúrate de tener un ícono llamado "editar" en drawable
                        .setOnMenuItemClickListener {
                            startActivity(Intent(this, EditProductActivity::class.java))
                            drawerLayout.closeDrawers()
                            true
                        }

                    // Agregar "Eliminar Producto"
                    binding.navView.menu.add(
                        R.id.group_admin,
                        R.id.nav_delete_product,
                        Menu.NONE,
                        "Eliminar Producto"
                    ).setIcon(R.drawable.delete) // Asegúrate de tener un ícono llamado "eliminar" en drawable
                        .setOnMenuItemClickListener {
                            startActivity(Intent(this, DeleteProductActivity::class.java))
                            drawerLayout.closeDrawers()
                            true
                        }
                }
            }

        // Logout & other
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_purchases -> {
                    // Navegación manual a la Activity
                    startActivity(Intent(this, PurchasesActivity::class.java))
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_logout -> {
                    // Lógica de logout
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                else -> {
                    // Para los fragments usa Navigation Component
                    try {
                        NavigationUI.onNavDestinationSelected(menuItem, navController)
                        binding.drawerLayout.closeDrawer(GravityCompat.START)
                        true
                    } catch (e: IllegalArgumentException) {
                        false
                    }
                }
            }
        }

        // Search & Cart
        binding.appBarMain.toolbar.findViewById<SearchView>(R.id.searchView)
            .setOnQueryTextListener(object: SearchView.OnQueryTextListener{
                override fun onQueryTextSubmit(q:String?)= true
                override fun onQueryTextChange(s:String?) = true
            })
        binding.appBarMain.toolbar.findViewById<android.widget.ImageButton>(R.id.cartButton)
            .setOnClickListener { startActivity(Intent(this, CartActivity::class.java)) }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

    }

    override fun onSupportNavigateUp() =
        navController.navigateUp(appBarConfig) || super.onSupportNavigateUp()
}

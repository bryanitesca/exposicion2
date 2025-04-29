package mx.edu.itesca.happybox_10

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import mx.edu.itesca.happybox_10.databinding.ActivityUserDetailsBinding
import kotlinx.coroutines.launch

class UserDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserDetailsBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var sharedPreferences: SharedPreferences
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val credentialManager by lazy { CredentialManager.create(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // toolbar + drawer
        setSupportActionBar(binding.appBarMain.toolbar)
        drawerLayout = binding.drawerLayout
        toggle = ActionBarDrawerToggle(
            this, drawerLayout, binding.appBarMain.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        setupNavMenu()
        setupStaticToolbarButtons()

        // Load user data from Firestore
        val uid = auth.currentUser?.uid ?: return
        val userRef = db.collection("Usuarios").document(uid)
        userRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                binding.nameEditText.setText(doc.getString("nombreUsuario"))
                binding.emailEditText.setText(doc.getString("correoUsuario"))
                binding.emailEditText.isEnabled = false
                binding.addressEditText.setText(doc.getString("direccionUsuario"))
                binding.phoneEditText.setText(doc.getString("telefonoUsuario"))
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error al cargar datos: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        // Save changes
        binding.saveButton.setOnClickListener {
            val name = binding.nameEditText.text.toString().trim()
            val address = binding.addressEditText.text.toString().trim()
            val phone = binding.phoneEditText.text.toString().trim()
            if (name.isEmpty() || address.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val updates = mapOf(
                "nombreUsuario" to name,
                "direccionUsuario" to address,
                "telefonoUsuario" to phone
            )
            userRef.update(updates).addOnSuccessListener {
                // also save address locally
                sharedPreferences.edit().putString("direccionUsuario", address).apply()
                Toast.makeText(this, "Datos actualizados", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupNavMenu() {
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
                R.id.nav_logout   -> {
                    auth.signOut()
                    lifecycleScope.launch {
                        try {
                            credentialManager.clearCredentialState(ClearCredentialStateRequest())
                            Log.d("UserDetails", "Credenciales borradas")
                        } catch (e: Exception) {
                            Log.e("UserDetails", "Error clearing credentials: ${e.message}")
                        }
                    }
                    sharedPreferences.edit().clear().apply()
                    startActivity(Intent(this, LoginActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                    finish()
                    true
                }
                else -> false
            }
        }
        // header clicks
        val header = navView.getHeaderView(0)
        header.findViewById<android.widget.ImageView>(R.id.userImageView)
            .setOnClickListener { /* already here */ }
        header.findViewById<android.widget.TextView>(R.id.userDetailsTextView)
            .setOnClickListener { /* already here */ }
    }

    private fun setupStaticToolbarButtons() {
        binding.appBarMain.toolbar.findViewById<SearchView>(R.id.searchView)
            .setOnQueryTextListener(object: SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(q: String?) = true.also {
                    Toast.makeText(this@UserDetailsActivity, "Búsqueda no funcional aún", Toast.LENGTH_SHORT).show()
                }
                override fun onQueryTextChange(t: String?) = true
            })
        binding.appBarMain.toolbar.findViewById<android.widget.ImageButton>(R.id.cartButton)
            .setOnClickListener {
                startActivity(Intent(this, CartActivity::class.java))
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

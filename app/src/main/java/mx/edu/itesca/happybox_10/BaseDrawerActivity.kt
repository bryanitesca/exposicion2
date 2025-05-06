package mx.edu.itesca.happybox_10

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

abstract class BaseDrawerActivity : AppCompatActivity() {

    /** Cada subclase debe proporcionar su propio layout de contenido */
    protected abstract val contentLayoutId: Int

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1) Inflar el layout base con Drawer
        setContentView(R.layout.activity_base_drawer)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        // 2) Inflar el layout específico dentro de content_frame
        val contentFrame = findViewById<FrameLayout>(R.id.content_frame)
        layoutInflater.inflate(contentLayoutId, contentFrame, true)

        // 3) Setup toolbar + toggle
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // 4) Configurar menú de navegación
        navView.setNavigationItemSelectedListener { menu ->
            when (menu.itemId) {
                R.id.nav_home     -> navigateTo(Principal::class.java)
                R.id.nav_purchases-> navigateTo(PurchasesActivity::class.java)
                R.id.nav_about    -> navigateTo(AboutActivity::class.java)
                R.id.nav_logout   -> {
                    FirebaseAuth.getInstance().signOut()
                    navigateTo(LoginActivity::class.java, finishCurrent = true)
                }
                else -> false
            }
        }

        // 5) Configurar header (usuario)
        val header = navView.getHeaderView(0)
        val tvName    = header.findViewById<TextView>(R.id.usernameTextView)
        val tvDetails = header.findViewById<TextView>(R.id.userDetailsTextView)
        val ivUser    = header.findViewById<ImageView>(R.id.userImageView)
        FirebaseFirestore.getInstance()
            .collection("Usuarios")
            .document(FirebaseAuth.getInstance().currentUser!!.uid)
            .get()
            .addOnSuccessListener { doc ->
                tvName.text = doc.getString("nombreUsuario")
                // si tienes URL de foto: Glide.with(this).load(doc.getString("fotoUrl")).into(ivUser)
            }
        header.setOnClickListener {
        }
    }

    private fun navigateTo(cls: Class<*>, finishCurrent: Boolean = false): Boolean {
        startActivity(Intent(this, cls))
        drawerLayout.closeDrawer(GravityCompat.START)
        if (finishCurrent) finish()
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else super.onBackPressed()
    }
}

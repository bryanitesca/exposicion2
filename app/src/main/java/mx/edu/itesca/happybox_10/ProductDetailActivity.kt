package mx.edu.itesca.happybox_10

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import android.widget.RatingBar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import mx.edu.itesca.happybox_10.databinding.ActivityProductDetailBinding

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var quantity = 1
    private lateinit var reviewsAdapter: ReviewsAdapter
    private val reviewsList = mutableListOf<Review>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
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

        // Firebase
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupNavMenu()

        // Recoger datos de Intent
        val productId       = intent.getStringExtra("productId") ?: return
        val productImages   = intent.getStringArrayListExtra("productImages") ?: arrayListOf(intent.getStringExtra("productImage")!!)
        val productName     = intent.getStringExtra("productName") ?: ""
        val productPrice    = intent.getDoubleExtra("productPrice", 0.0)
        val productDesc     = intent.getStringExtra("productDescription") ?: ""

        // Slider de imágenes
        val slider = binding.imageSlider
        slider.adapter = ImageSliderAdapter(productImages)
        autoScrollSlider(slider)

        // Datos estáticos
        binding.productName.text = productName
        binding.productPrice.text = "$${"%.2f".format(productPrice)}"
        binding.productDescription.text = productDesc
        binding.quantityText.text = quantity.toString()

        // Controles de cantidad
        binding.decreaseButton.setOnClickListener {
            if (quantity>1) binding.quantityText.text = (--quantity).toString()
        }
        binding.increaseButton.setOnClickListener {
            binding.quantityText.text = (++quantity).toString()
        }

        // Compra directa
        binding.buyNowButton.setOnClickListener {
            processPurchase(productId, quantity, productPrice)
        }
        // Añadir al carrito
        binding.addToCartButton.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java).apply {
                putExtra("productId", productId)
                putExtra("productImages", productImages)
                putExtra("productName", productName)
                putExtra("productPrice", productPrice)
                putExtra("quantity", quantity)
            })
        }

        // Setup Reviews
        setupReviewsRecyclerView()
        loadReviews(productId)

        // Search & Cart buttons
        setupStaticToolbarButtons()

        // Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }
    }

    private fun setupNavMenu() {
        val navView = binding.navView
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home      -> navigateTo(Principal::class.java)
                R.id.nav_regalos   -> navigateToFragment("regalos")
                R.id.nav_detalles  -> navigateToFragment("detalles")
                R.id.nav_peluches  -> navigateToFragment("peluches")
                R.id.nav_tazas     -> navigateToFragment("tazas")
                R.id.nav_globos    -> navigateToFragment("globos")
                R.id.nav_about     -> navigateTo(AboutActivity::class.java)
                R.id.nav_logout    -> logout()
                else               -> false
            }
        }
    }

    private fun setupStaticToolbarButtons() {
        val searchView = binding.appBarMain.toolbar.findViewById<SearchView>(R.id.searchView)
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query:String?) = true.also {
                Toast.makeText(this@ProductDetailActivity,"Búsqueda no funcional aún",Toast.LENGTH_SHORT).show()
            }
            override fun onQueryTextChange(newText:String?) = true
        })
        binding.appBarMain.toolbar.findViewById<android.widget.ImageButton>(R.id.cartButton)
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
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(this, "Error cargando reseñas", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                reviewsList.clear()
                snapshots?.documents?.forEach { doc ->
                    val review = doc.toObject(Review::class.java)?.copy(id = doc.id)
                    review?.let {
                        reviewsList.add(it)
                        // Cargar nombre de usuario si está disponible
                        if (!it.idUsuario.isNullOrEmpty()) {
                            loadUserName(it.idUsuario) { userName ->
                                it.nombreUsuario = userName
                                reviewsAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }

                updateReviewsUI(reviewsList.isNotEmpty())
                reviewsAdapter.notifyDataSetChanged()
            }
    }

    private fun updateReviewsUI(hasReviews: Boolean) {
        if (hasReviews) {
            val avgRating = reviewsList.map { it.ratingFloat() }.average().toFloat()
            binding.ratingBar.rating = avgRating
            binding.tvAvgRating.text = "%.1f".format(avgRating)
            binding.tvReviewCount.text = "(${reviewsList.size} reseñas)"

            // Mostrar elementos
            binding.ratingBar.visibility = View.VISIBLE
            binding.tvAvgRating.visibility = View.VISIBLE
            binding.tvReviewCount.visibility = View.VISIBLE
            binding.tvReviewsTitle.visibility = View.VISIBLE
            binding.rvReviews.visibility = View.VISIBLE
        } else {
            // Ocultar elementos si no hay reseñas
            binding.ratingBar.visibility = View.GONE
            binding.tvAvgRating.visibility = View.GONE
            binding.tvReviewCount.visibility = View.GONE
            binding.tvReviewsTitle.visibility = View.GONE
            binding.rvReviews.visibility = View.GONE
        }
    }

    private fun loadUserName(userId: String, callback: (String) -> Unit) {
        db.collection("Usuarios").document(userId).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("nombreUsuario") ?: ""
                callback(name)
            }
            .addOnFailureListener {
                callback("")
            }
    }

    private fun showReviewDialog(productId: String) {
        if (auth.currentUser == null) {
            Toast.makeText(this, "Debes iniciar sesión para dejar una reseña", Toast.LENGTH_SHORT).show()
            return
        }

        val view = layoutInflater.inflate(R.layout.dialog_add_review, null)
        val rb = view.findViewById<RatingBar>(R.id.dialogRatingBar)
        val etComm = view.findViewById<EditText>(R.id.dialogComment)

        AlertDialog.Builder(this)
            .setTitle("Tu reseña")
            .setView(view)
            .setPositiveButton("Enviar") { _, _ ->
                val rating = rb.rating.toInt().coerceIn(1, 5).toString()
                val comment = etComm.text.toString().trim()

                if (comment.isEmpty()) {
                    Toast.makeText(this, "Por favor escribe un comentario", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Obtener nombre del usuario desde SharedPreferences
                val userName = sharedPreferences.getString("nombreUsuario", "") ?: ""

                val reviewData = hashMapOf(
                    "calificacionReview" to rating,
                    "comentarioReview" to comment,
                    "fechaReview" to Timestamp.now(),
                    "nombreUsuario" to userName
                )

                db.collection("Productos").document(productId)
                    .collection("Reviews")
                    .add(reviewData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Reseña agregada", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al agregar reseña", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun navigateTo(activity: Class<*>) : Boolean {
        startActivity(Intent(this, activity)); finish(); return true
    }

    private fun navigateToFragment(tag:String) : Boolean {
        val intent = Intent(this, Principal::class.java)
        intent.putExtra("fragment", tag)
        startActivity(intent); finish(); return true
    }

    private fun logout() : Boolean {
        auth.signOut()
        sharedPreferences.edit().clear().apply()
        startActivity(Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
        return true
    }

    private fun autoScrollSlider(pager: ViewPager2) {
        val handler = android.os.Handler(mainLooper)
        val task = object: Runnable {
            override fun run() {
                pager.currentItem = (pager.currentItem + 1) % (pager.adapter?.itemCount ?: 1)
                handler.postDelayed(this, 3000)
            }
        }
        handler.postDelayed(task, 3000)
    }

    private fun processPurchase(pid:String, qty:Int, price:Double) {
        val uid = auth.currentUser?.uid ?: return Toast.makeText(this,"Inicia sesión primero",Toast.LENGTH_SHORT).show()
        val venta = mapOf(
            "idUsuario"               to uid,
            "fechaCompra"             to Timestamp.now(),
            "totalCompra"             to price*qty,
            "estatusCompra"           to "pendiente",
            "direccionEntregaCompra"  to sharedPreferences.getString("direccionUsuario",""),
            "metodoPago"              to "pendiente"
        )
        db.collection("Ventas").add(venta).addOnSuccessListener { doc ->
            db.collection("Ventas").document(doc.id)
                .collection("DetalleVenta")
                .add(mapOf("idProducto" to pid, "cantidadProducto" to qty, "precioProducto" to price))
                .addOnSuccessListener {
                    db.collection("Productos").document(pid)
                        .update("stockProducto", FieldValue.increment(-qty.toLong()))
                    Toast.makeText(this,"Compra realizada",Toast.LENGTH_SHORT).show()
                }
        }
    }
}
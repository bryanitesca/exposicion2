package mx.edu.itesca.happybox_10

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
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

        // Obtener datos de Intent
        val productId = intent.getStringExtra("productId") ?: return
        val productImages = intent.getStringArrayListExtra("productImages")
            ?: arrayListOf(intent.getStringExtra("productImage")!!)
        val productName = intent.getStringExtra("productName") ?: ""
        val productPrice = intent.getDoubleExtra("productPrice", 0.0)
        val productDesc = intent.getStringExtra("productDescription") ?: ""

        // Configurar slider
        binding.imageSlider.adapter = ImageSliderAdapter(productImages)
        autoScrollSlider(binding.imageSlider)

        // Datos del producto
        binding.productName.text = productName
        binding.productPrice.text = "$${"%.2f".format(productPrice)}"
        binding.productDescription.text = productDesc
        binding.quantityText.text = quantity.toString()

        // Control de cantidad
        binding.decreaseButton.setOnClickListener {
            if (quantity > 1) binding.quantityText.text = (--quantity).toString()
        }
        binding.increaseButton.setOnClickListener {
            binding.quantityText.text = (++quantity).toString()
        }

        // Compra / Carrito
        binding.buyNowButton.setOnClickListener { processPurchase(productId, quantity, productPrice) }
        binding.addToCartButton.setOnClickListener {
            addToCart(productId, productImages, productName, productPrice, quantity)
        }

        // Reviews
        setupReviewsRecyclerView()
        loadReviews(productId)
        checkUserReview(productId)
        binding.btnAddReview.setOnClickListener { showReviewDialog(productId) }

        // Búsqueda y carrito en toolbar
        setupStaticToolbarButtons()

        // Ajuste de insets
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

        // Asegurarnos de que la URL de la imagen sea String
        val imageUrl = productImages.firstOrNull()?.toString() ?: ""

        val cartItem = hashMapOf(
            "productId" to productId,
            "name" to productName,
            "price" to productPrice,
            "quantity" to quantity,
            "imageUrl" to imageUrl  // Ahora es definitivamente un String
        )

        // Verificar si el producto ya está en el carrito
        db.collection("Usuarios").document(uid)
            .collection("Carrito")
            .whereEqualTo("productId", productId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Producto no existe en carrito, agregar nuevo
                    db.collection("Usuarios").document(uid)
                        .collection("Carrito")
                        .add(cartItem)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Producto agregado al carrito", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Log.e("ProductDetail", "Error adding to cart", e)
                            Toast.makeText(this, "Error al agregar al carrito", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // Producto ya existe, actualizar cantidad
                    val docId = documents.documents[0].id
                    db.collection("Usuarios").document(uid)
                        .collection("Carrito")
                        .document(docId)
                        .update("quantity", FieldValue.increment(quantity.toLong()))
                        .addOnSuccessListener {
                            Toast.makeText(this, "Cantidad actualizada en carrito", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Log.e("ProductDetail", "Error updating cart", e)
                            Toast.makeText(this, "Error al actualizar carrito", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProductDetail", "Error checking cart", e)
                Toast.makeText(this, "Error al verificar carrito", Toast.LENGTH_SHORT).show()
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

    private fun processPurchase(pid: String, qty: Int, price: Double) {
        val uid = auth.currentUser?.uid ?: return Toast.makeText(this, "Inicia sesión primero", Toast.LENGTH_SHORT).show()
        val sale = mapOf(
            "idUsuario" to uid,
            "fechaCompra" to Timestamp.now(),
            "totalCompra" to price * qty,
            "estatusCompra" to "pendiente",
            "direccionEntregaCompra" to sharedPreferences.getString("direccionUsuario", ""),
            "metodoPago" to "pendiente"
        )
        db.collection("Ventas").add(sale).addOnSuccessListener { doc ->
            db.collection("Ventas").document(doc.id).collection("DetalleVenta")
                .add(mapOf("idProducto" to pid, "cantidadProducto" to qty, "precioProducto" to price))
                .addOnSuccessListener {
                    db.collection("Productos").document(pid).update("stockProducto", FieldValue.increment(-qty.toLong()))
                    Toast.makeText(this, "Compra realizada", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
package mx.edu.itesca.happybox_10

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.setPadding
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.tasks.Tasks
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*
import es.dmoral.toasty.Toasty

class AddProductActivity : AppCompatActivity() {

    private val PICK_IMAGES_CODE = 1001
    private val storage = FirebaseStorage.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var mode = "add"
    private var productId: String? = null

    private var newImageUris = mutableListOf<Uri>()
    private var existingImageUrls = mutableListOf<String>()

    private lateinit var etName: EditText
    private lateinit var etDescription: EditText
    private lateinit var etPrice: EditText
    private lateinit var etDiscount: EditText
    private lateinit var etStock: EditText
    private lateinit var spCategory: Spinner
    private lateinit var btnPickImages: Button
    private lateinit var btnSave: Button
    private lateinit var imagesContainer: LinearLayout
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

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
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.nav_edit_product -> {
                    startActivity(Intent(this, EditProductActivity::class.java))
                    finish()
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

        // Inicializar vistas
        etName = findViewById(R.id.etName)
        etDescription = findViewById(R.id.etDescription)
        etPrice = findViewById(R.id.etPrice)
        etDiscount = findViewById(R.id.etDiscount)
        etStock = findViewById(R.id.etStock)
        spCategory = findViewById(R.id.spCategory)
        btnPickImages = findViewById(R.id.btnPickImages)
        btnSave = findViewById(R.id.btnSave)
        imagesContainer = findViewById(R.id.imagesContainer)

        spCategory.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Detalles", "Regalos", "Tazas", "Peluches", "Globos")
        )

        intent.getStringExtra("mode")?.let {
            mode = it
            if (mode == "edit") {
                productId = intent.getStringExtra("productId")
                productId?.let { id -> loadExistingProduct(id) }
                btnSave.text = "Actualizar Producto"
            }
        }

        btnPickImages.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            startActivityForResult(
                Intent.createChooser(intent, "Selecciona imágenes"),
                PICK_IMAGES_CODE
            )
        }

        btnSave.setOnClickListener {
            if (mode == "add") uploadProduct() else updateProduct()
        }
    }

    private fun loadExistingProduct(id: String) {
        db.collection("Productos").document(id).get()
            .addOnSuccessListener { doc ->
                etName.setText(doc.getString("nombreProducto"))
                etDescription.setText(doc.getString("descripcionProducto"))
                etPrice.setText(doc.getDouble("precioProducto")?.toString())
                etDiscount.setText(doc.getDouble("descuentoProducto")?.toString())
                etStock.setText(doc.get("stockProducto")?.toString())
                val tipo = doc.getString("tipoProducto")
                (spCategory.adapter as ArrayAdapter<String>).getPosition(tipo).let { pos ->
                    if (pos >= 0) spCategory.setSelection(pos)
                }
                existingImageUrls = (doc.get("imagenes") as? List<String>)?.toMutableList()
                    ?: mutableListOf()
                displayExistingImages()
            }
    }

    private fun displayExistingImages() {
        imagesContainer.removeAllViews()
        val imageSizePx = (100 * resources.displayMetrics.density).toInt()
        existingImageUrls.forEachIndexed { index, url ->
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val lp = LinearLayout.LayoutParams(imageSizePx, LinearLayout.LayoutParams.WRAP_CONTENT)
                layoutParams = lp
                setPadding((4 * resources.displayMetrics.density).toInt())
            }
            val imageView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(imageSizePx, imageSizePx)
                com.bumptech.glide.Glide.with(this@AddProductActivity)
                    .load(url)
                    .centerCrop()
                    .into(this)
            }
            val removeBtn = Button(this).apply {
                text = "Eliminar"
                isAllCaps = false
                setSingleLine()
                layoutParams = LinearLayout.LayoutParams(imageSizePx, LinearLayout.LayoutParams.WRAP_CONTENT)
                setOnClickListener {
                    existingImageUrls.removeAt(index)
                    displayExistingImages()
                }
            }
            container.addView(imageView)
            container.addView(removeBtn)
            imagesContainer.addView(container)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGES_CODE && resultCode == Activity.RESULT_OK) {
            newImageUris.clear()
            data?.clipData?.let { clip ->
                for (i in 0 until clip.itemCount) newImageUris.add(clip.getItemAt(i).uri)
            } ?: data?.data?.let { newImageUris.add(it) }
            newImageUris.forEach { uri ->
                val iv = ImageView(this).apply {
                    val size = (200 * resources.displayMetrics.density).toInt()
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        setMargins(8, 8, 8, 8)
                    }
                    setImageURI(uri)
                }
                imagesContainer.addView(iv)
            }
        }
    }

    private fun uploadProduct() {
        val name = etName.text.toString().trim()
        val desc = etDescription.text.toString().trim()
        val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
        val discount = etDiscount.text.toString().toDoubleOrNull() ?: 0.0
        val stock = etStock.text.toString().toIntOrNull() ?: 0
        val category = spCategory.selectedItem as String

        if (name.isEmpty() || (newImageUris.isEmpty() && existingImageUrls.isEmpty())) {
            //Toast.makeText(this, "Nombre e imágenes requeridas", Toast.LENGTH_SHORT).show()
            Toasty.error(this, "Nombre e imagenes requeridas", Toast.LENGTH_LONG, true).show()
            return
        }

        val urls = mutableListOf<String>().apply { addAll(existingImageUrls) }
        val folder = storage.reference.child("productos/${UUID.randomUUID()}")
        val tasks = newImageUris.map { uri ->
            val ref = folder.child(UUID.randomUUID().toString())
            ref.putFile(uri).continueWithTask { it.result!!.storage.downloadUrl }
        }
        Tasks.whenAllSuccess<Uri>(tasks)
            .addOnSuccessListener { downloadUrls ->
                downloadUrls.forEach { urls.add(it.toString()) }
                val product = hashMapOf(
                    "nombreProducto" to name,
                    "descripcionProducto" to desc,
                    "precioProducto" to price,
                    "descuentoProducto" to discount,
                    "stockProducto" to stock,
                    "tipoProducto" to category,
                    "imagenes" to urls
                )
                db.collection("Productos").add(product)
                    .addOnSuccessListener {
                        //Toast.makeText(this, "Producto agregado", Toast.LENGTH_SHORT).show()
                        Toasty.success(this, "¡Producto agregado!", Toast.LENGTH_SHORT, true).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        //Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        Toasty.error(this, "\"Error: ${e.message}\"", Toast.LENGTH_LONG, true).show()
                    }
            }
            .addOnFailureListener { e ->
               // Toast.makeText(this, "Error al subir imágenes: ${e.message}", Toast.LENGTH_LONG).show()
                Toasty.error(this, "Error al subir imágenes: ${e.message}", Toast.LENGTH_LONG, true).show()
            }
    }

    private fun updateProduct() {
        val id = productId ?: return
        val name = etName.text.toString().trim()
        val desc = etDescription.text.toString().trim()
        val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
        val discount = etDiscount.text.toString().toDoubleOrNull() ?: 0.0
        val stock = etStock.text.toString().toIntOrNull() ?: 0
        val category = spCategory.selectedItem as String

        if (name.isEmpty() || (newImageUris.isEmpty() && existingImageUrls.isEmpty())) {
            //Toast.makeText(this, "Nombre e imágenes requeridas", Toast.LENGTH_SHORT).show()
            Toasty.error(this, "Nombre e imagenes requeridas", Toast.LENGTH_LONG, true).show()
            return
        }

        val urls = mutableListOf<String>().apply { addAll(existingImageUrls) }
        val folder = storage.reference.child("productos/$id")
        val tasks = newImageUris.map { uri ->
            val ref = folder.child(UUID.randomUUID().toString())
            ref.putFile(uri).continueWithTask { it.result!!.storage.downloadUrl }
        }
        Tasks.whenAllSuccess<Uri>(tasks)
            .addOnSuccessListener { downloadUrls ->
                downloadUrls.forEach { urls.add(it.toString()) }
                val updates = mapOf(
                    "nombreProducto" to name,
                    "descripcionProducto" to desc,
                    "precioProducto" to price,
                    "descuentoProducto" to discount,
                    "stockProducto" to stock,
                    "tipoProducto" to category,
                    "imagenes" to urls
                )
                db.collection("Productos").document(id)
                    .update(updates)
                    .addOnSuccessListener {
                        Toasty.info(this, "Producto actualizado", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toasty.error(this, "Error al actualizar", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toasty.error(this, "Error al subir imágenes: ${it.message}", Toast.LENGTH_LONG).show()
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
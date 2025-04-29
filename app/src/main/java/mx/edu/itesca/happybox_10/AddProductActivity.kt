package mx.edu.itesca.happybox_10

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class AddProductActivity : AppCompatActivity() {

    private val PICK_IMAGES_CODE = 1001
    private val storage = FirebaseStorage.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var imageUris = mutableListOf<Uri>()

    private lateinit var etName: EditText
    private lateinit var etDescription: EditText
    private lateinit var etPrice: EditText
    private lateinit var etDiscount: EditText
    private lateinit var etStock: EditText
    private lateinit var spCategory: Spinner
    private lateinit var btnPickImages: Button
    private lateinit var btnSave: Button
    private lateinit var imagesContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        etName = findViewById(R.id.etName)
        etDescription = findViewById(R.id.etDescription)
        etPrice = findViewById(R.id.etPrice)
        etDiscount = findViewById(R.id.etDiscount)
        etStock = findViewById(R.id.etStock)
        spCategory = findViewById(R.id.spCategory)
        btnPickImages = findViewById(R.id.btnPickImages)
        btnSave = findViewById(R.id.btnSave)
        imagesContainer = findViewById(R.id.imagesContainer)

        // spinner de categorías
        spCategory.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Detalles","Regalos","Tazas","Peluches","Globos")
        )

        btnPickImages.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            startActivityForResult(Intent.createChooser(intent, "Selecciona imágenes"), PICK_IMAGES_CODE)
        }

        btnSave.setOnClickListener { uploadProduct() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGES_CODE && resultCode == Activity.RESULT_OK) {
            imageUris.clear()
            imagesContainer.removeAllViews()
            data?.clipData?.let { clip ->
                for (i in 0 until clip.itemCount) imageUris.add(clip.getItemAt(i).uri)
            } ?: data?.data?.let { imageUris.add(it) }

            // previsualizar thumbnails
            imageUris.forEach { uri ->
                val iv = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(200,200).apply {
                        setMargins(8,8,8,8)
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
        if (name.isEmpty() || imageUris.isEmpty()) {
            Toast.makeText(this, "Nombre e imágenes requeridas", Toast.LENGTH_SHORT).show()
            return
        }
        // 1) subir imágenes a Storage
        val urls = mutableListOf<String>()
        val folder = storage.reference.child("productos/${UUID.randomUUID()}")
        val tasks = imageUris.map { uri ->
            val ref = folder.child(UUID.randomUUID().toString())
            ref.putFile(uri).continueWithTask { it.result!!.storage.downloadUrl }
        }
        // cuando todas finalicen
        com.google.android.gms.tasks.Tasks.whenAllSuccess<Uri>(tasks)
            .addOnSuccessListener { downloadUrls ->
                downloadUrls.forEach { urls.add(it.toString()) }
                // 2) guardar documento en Firestore
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
                        Toast.makeText(this, "Producto agregado", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al subir imágenes: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}

package mx.edu.itesca.happybox_10

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class EditProductActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var spCategory: Spinner
    private lateinit var etName: EditText
    private lateinit var etPrice: EditText
    private lateinit var etDiscount: EditText
    private lateinit var etStock: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnSearch: Button
    private lateinit var btnUpdate: Button
    private lateinit var rvProducts: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private var productId: String? = null
    private val productList = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_product)

        spCategory = findViewById(R.id.spCategory)
        etName = findViewById(R.id.etName)
        etPrice = findViewById(R.id.etPrice)
        etDiscount = findViewById(R.id.etDiscount)
        etStock = findViewById(R.id.etStock)
        etDescription = findViewById(R.id.etDescription)
        btnSearch = findViewById(R.id.btnSearch)
        btnUpdate = findViewById(R.id.btnUpdate)
        rvProducts = findViewById(R.id.rvProducts)

        // Configurar Spinner
        spCategory.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Detalles", "Regalos", "Tazas", "Peluches", "Globos")
        )

        // Configurar RecyclerView
        productAdapter = ProductAdapter(productList).apply {
            // Override onClick to populate fields
            setOnItemClickListener { product ->
                productId = product.id
                etName.setText(product.nombreProducto)
                etPrice.setText(product.precioProducto.toString())
                etDiscount.setText(product.descuentoProducto.toString())
                etStock.setText(product.stockProducto.toString())
                etDescription.setText(product.descripcionProducto)
                Toast.makeText(this@EditProductActivity, "Producto seleccionado", Toast.LENGTH_SHORT).show()
            }
        }
        rvProducts.layoutManager = LinearLayoutManager(this)
        rvProducts.adapter = productAdapter

        btnSearch.setOnClickListener { searchProduct() }
        btnUpdate.setOnClickListener { updateProduct() }
    }

    private fun searchProduct() {
        val tipo = spCategory.selectedItem as String
        val nombre = etName.text.toString().trim()

        if (nombre.isEmpty()) {
            Toast.makeText(this, "Nombre requerido", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("Productos")
            .whereEqualTo("tipoProducto", tipo)
            .whereGreaterThanOrEqualTo("nombreProducto", nombre)
            .whereLessThanOrEqualTo("nombreProducto", nombre + "\uf8ff")
            .get()
            .addOnSuccessListener { snap ->
                productList.clear()
                if (snap.isEmpty) {
                    Toast.makeText(this, "No se encontraron productos", Toast.LENGTH_SHORT).show()
                } else {
                    snap.documents.forEach { doc ->
                        val product = doc.toObject(Product::class.java)?.copy(id = doc.id)
                        if (product != null) {
                            productList.add(product)
                        }
                    }
                    productAdapter.notifyDataSetChanged()
                    Toast.makeText(this, "${productList.size} productos encontrados", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al buscar productos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateProduct() {
        val id = productId ?: return Toast.makeText(this, "Primero selecciona un producto", Toast.LENGTH_SHORT).show()
        val updates = mapOf(
            "precioProducto" to (etPrice.text.toString().toDoubleOrNull() ?: 0.0),
            "descuentoProducto" to (etDiscount.text.toString().toDoubleOrNull() ?: 0.0),
            "stockProducto" to (etStock.text.toString().toIntOrNull() ?: 0),
            "descripcionProducto" to etDescription.text.toString().trim()
        )
        db.collection("Productos").document(id)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Producto actualizado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show()
            }
    }
}
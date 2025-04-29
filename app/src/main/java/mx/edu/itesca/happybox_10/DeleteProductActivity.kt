package mx.edu.itesca.happybox_10

import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class DeleteProductActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var spCategory: Spinner
    private lateinit var etName: EditText
    private lateinit var btnSearch: Button
    private lateinit var btnDelete: Button
    private lateinit var rvProducts: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private var productId: String? = null
    private var selectedProductName: String? = null
    private val productList = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delete_product)

        spCategory = findViewById(R.id.spCategory)
        etName = findViewById(R.id.etName)
        btnSearch = findViewById(R.id.btnSearch)
        btnDelete = findViewById(R.id.btnDelete)
        rvProducts = findViewById(R.id.rvProducts)

        // Configurar Spinner
        spCategory.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Detalles", "Regalos", "Tazas", "Peluches", "Globos")
        )

        // Configurar RecyclerView
        productAdapter = ProductAdapter(productList).apply {
            // Override onClick to show confirmation dialog
            setOnItemClickListener { product ->
                productId = product.id
                selectedProductName = product.nombreProducto
                showDeleteConfirmationDialog(product.nombreProducto)
            }
        }
        rvProducts.layoutManager = LinearLayoutManager(this)
        rvProducts.adapter = productAdapter

        btnSearch.setOnClickListener { searchProduct() }
        btnDelete.setOnClickListener { deleteProduct() }
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

    private fun showDeleteConfirmationDialog(productName: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar el producto \"$productName\"?")
            .setPositiveButton("Aceptar") { _, _ ->
                deleteProduct()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                productId = null
                selectedProductName = null
            }
            .show()
    }

    private fun deleteProduct() {
        val id = productId ?: return Toast.makeText(this, "Primero selecciona un producto", Toast.LENGTH_SHORT).show()
        db.collection("Productos").document(id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Producto eliminado", Toast.LENGTH_SHORT).show()
                productId = null
                selectedProductName = null
                productList.clear()
                productAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show()
            }
    }
}
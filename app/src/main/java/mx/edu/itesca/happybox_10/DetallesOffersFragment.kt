package mx.edu.itesca.happybox_10

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class DetallesOffersFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_detalles_offers, container, false)
        recyclerView = view.findViewById(R.id.offersRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        loadOffers()
        return view
    }

    private fun loadOffers() {
        db.collection("Productos")
            .whereEqualTo("tipoProducto", "Detalles")
            .whereGreaterThan("descuentoProducto", 0.0)
            .get()
            .addOnSuccessListener { result ->
                val offers = result.map { doc ->
                    val stockStr = doc.getString("stockProducto") ?: "0"
                    Product(
                        id               = doc.id,
                        nombreProducto   = doc.getString("nombreProducto") ?: "",
                        precioProducto   = doc.getDouble("precioProducto") ?: 0.0,
                        descuentoProducto= doc.getDouble("descuentoProducto") ?: 0.0,
                        tipoProducto     = doc.getString("tipoProducto") ?: "",
                        descripcionProducto = doc.getString("descripcionProducto") ?: "",
                        stockProducto    = stockStr.toIntOrNull() ?: 0,
                        imagenes         = doc.get("imagenes") as? List<String> ?: emptyList()
                    )
                }
                recyclerView.adapter = ProductAdapter(offers)
            }
    }
}

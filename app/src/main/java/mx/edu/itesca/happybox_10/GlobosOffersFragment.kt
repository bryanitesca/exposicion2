package mx.edu.itesca.happybox_10

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class GlobosOffersFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_globos_offers, container, false)
        val recycler: RecyclerView = view.findViewById(R.id.offersRecyclerView)
        recycler.layoutManager = LinearLayoutManager(context)

        db.collection("Productos")
            .whereEqualTo("tipoProducto", "Globos")
            .whereGreaterThan("descuentoProducto", 0.0)
            .get()
            .addOnSuccessListener { snap ->
                val offers = snap.map { doc ->
                    val stockStr = doc.getString("stockProducto") ?: "0"
                    Product(
                        id               = doc.id,
                        nombreProducto   = doc.getString("nombreProducto") ?: "",
                        precioProducto   = doc.getDouble("precioProducto") ?: 0.0,
                        descuentoProducto= doc.getDouble("descuentoProducto") ?: 0.0,
                        tipoProducto     = "Globos",
                        descripcionProducto = doc.getString("descripcionProducto") ?: "",
                        stockProducto    = stockStr.toIntOrNull() ?: 0,
                        imagenes         = doc.get("imagenes") as? List<String> ?: emptyList()
                    )
                }
                recycler.adapter = ProductAdapter(offers)
            }

        return view
    }
}

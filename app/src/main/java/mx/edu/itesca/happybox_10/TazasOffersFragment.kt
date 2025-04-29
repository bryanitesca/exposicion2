package mx.edu.itesca.happybox_10

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class TazasOffersFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tazas_offers, container, false)
        val rv: RecyclerView = view.findViewById(R.id.offersRecyclerView)
        rv.layoutManager = LinearLayoutManager(context)

        db.collection("Productos")
            .whereEqualTo("tipoProducto", "Tazas")
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
                        tipoProducto     = "Tazas",
                        descripcionProducto = doc.getString("descripcionProducto") ?: "",
                        stockProducto    = stockStr.toIntOrNull() ?: 0,
                        imagenes         = doc.get("imagenes") as? List<String> ?: emptyList()
                    )
                }
                rv.adapter = ProductAdapter(offers)
            }

        return view
    }
}

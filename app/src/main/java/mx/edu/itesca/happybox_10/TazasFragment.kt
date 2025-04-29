package mx.edu.itesca.happybox_10

import android.os.Bundle
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore

class TazasFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tazas, container, false)

        val vp: ViewPager2 = view.findViewById(R.id.bannerViewPager)
        val tabs: TabLayout = view.findViewById(R.id.bannerIndicator)
        val prev: ImageButton = view.findViewById(R.id.previousButton)
        val next: ImageButton = view.findViewById(R.id.nextButton)
        val rv: RecyclerView = view.findViewById(R.id.productsRecyclerView)

        // Banner Tazas
        db.collection("Productos")
            .whereEqualTo("tipoProducto", "Tazas")
            .limit(5)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.map { doc ->
                    Product(
                        id = doc.id,
                        nombreProducto = "",
                        precioProducto = 0.0,
                        descuentoProducto = 0.0,
                        tipoProducto = "Tazas",
                        descripcionProducto = "",
                        stockProducto = parseStock(doc.get("stockProducto")),
                        imagenes = doc.get("imagenes") as? List<String> ?: emptyList()
                    )
                }
                if (list.isNotEmpty()) {
                    val adapter = BannerAdapter(list) { pos ->
                        val args = Bundle().apply { putInt("bannerPosition", pos) }
                        val opts = NavOptions.Builder()
                            .setPopUpTo(R.id.nav_tazas, false)
                            .build()
                        findNavController().navigate(
                            R.id.action_nav_tazas_to_tazasOffersFragment,
                            args,
                            opts
                        )
                    }
                    vp.adapter = adapter
                    val mid = Int.MAX_VALUE / 2
                    vp.setCurrentItem(mid - (mid % list.size), false)
                    tabs.removeAllTabs()
                    repeat(list.size) { tabs.addTab(tabs.newTab()) }
                    vp.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(i: Int) {
                            tabs.selectTab(tabs.getTabAt(i % list.size))
                        }
                    })
                    prev.setOnClickListener { vp.currentItem-- }
                    next.setOnClickListener { vp.currentItem++ }
                }
            }

        // Lista completa de Tazas
        rv.layoutManager = LinearLayoutManager(context)
        db.collection("Productos")
            .whereEqualTo("tipoProducto", "Tazas")
            .get()
            .addOnSuccessListener { snap ->
                val items = snap.map { doc ->
                    Product(
                        id = doc.id,
                        nombreProducto = doc.getString("nombreProducto") ?: "",
                        precioProducto = doc.getDouble("precioProducto") ?: 0.0,
                        descuentoProducto = doc.getDouble("descuentoProducto") ?: 0.0,
                        tipoProducto = "Tazas",
                        descripcionProducto = doc.getString("descripcionProducto") ?: "",
                        stockProducto = parseStock(doc.get("stockProducto")),
                        imagenes = doc.get("imagenes") as? List<String> ?: emptyList()
                    )
                }
                rv.adapter = ProductAdapter(items)
            }

        return view
    }

    private fun parseStock(value: Any?): Int {
        return when (value) {
            is Long   -> value.toInt()
            is Double -> value.toInt()
            is String -> value.toIntOrNull() ?: 0
            else      -> 0
        }
    }
}

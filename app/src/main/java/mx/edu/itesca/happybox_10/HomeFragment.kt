package mx.edu.itesca.happybox_10

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout

class HomeFragment : Fragment() {
    private lateinit var viewPager: ViewPager2
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        viewPager = view.findViewById(R.id.bannerViewPager)
        val tabLayout: TabLayout = view.findViewById(R.id.bannerIndicator)
        val previousButton: View = view.findViewById(R.id.previousButton)
        val nextButton: View = view.findViewById(R.id.nextButton)

        // 1) Banners
        BannerAdapter.fetchBannerProducts { bannerProducts ->
            if (bannerProducts.isEmpty()) {
                // opcional: ocultar viewPager y tabLayout
                viewPager.visibility = View.GONE
                tabLayout.visibility = View.GONE
                return@fetchBannerProducts
            }
            val bannerAdapter = BannerAdapter(bannerProducts) { position ->
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.nav_home, false)
                    .build()
                when (bannerProducts[position].tipoProducto) {
                    "Regalos"  -> findNavController().navigate(
                        HomeFragmentDirections.actionNavHomeToRegalosOffersFragment(position),
                        navOptions
                    )
                    "Detalles" -> findNavController().navigate(
                        HomeFragmentDirections.actionNavHomeToDetallesOffersFragment(position),
                        navOptions
                    )
                    "Peluches" -> findNavController().navigate(
                        HomeFragmentDirections.actionNavHomeToPeluchesOffersFragment(position),
                        navOptions
                    )
                    "Tazas"    -> findNavController().navigate(
                        HomeFragmentDirections.actionNavHomeToTazasOffersFragment(position),
                        navOptions
                    )
                    "Globos"   -> findNavController().navigate(
                        HomeFragmentDirections.actionNavHomeToGlobosOffersFragment(position),
                        navOptions
                    )
                }
            }
            viewPager.adapter = bannerAdapter

            // ahora es seguro calcular módulo
            val middle = Int.MAX_VALUE / 2
            val start = middle - (middle % bannerProducts.size)
            viewPager.setCurrentItem(start, false)

            // puntos
            tabLayout.removeAllTabs()
            repeat(bannerProducts.size) { tabLayout.addTab(tabLayout.newTab()) }
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(pos: Int) {
                    tabLayout.selectTab(tabLayout.getTabAt(pos % bannerProducts.size))
                }
            })
        }

        previousButton.setOnClickListener { viewPager.currentItem-- }
        nextButton.setOnClickListener     { viewPager.currentItem++ }

        // 2) Lista de productos
        recyclerView = view.findViewById(R.id.productsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        fetchProducts { products ->
            recyclerView.adapter = ProductAdapter(products)
        }

        return view
    }

    private fun fetchProducts(callback: (List<Product>) -> Unit) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        db.collection("Productos")
            .get()
            .addOnSuccessListener { result ->
                val products = result.map { doc ->
                    // parseo robusto de stockProducto
                    val rawStock = doc.get("stockProducto")
                    val stockInt = when (rawStock) {
                        is Long   -> rawStock.toInt()
                        is Double -> rawStock.toInt()
                        is String -> rawStock.toIntOrNull() ?: 0
                        else      -> 0
                    }
                    Product(
                        id              = doc.id,
                        nombreProducto  = doc.getString("nombreProducto") ?: "",
                        precioProducto  = doc.getDouble("precioProducto") ?: 0.0,
                        descuentoProducto = doc.getDouble("descuentoProducto") ?: 0.0,
                        tipoProducto    = doc.getString("tipoProducto") ?: "",
                        descripcionProducto = doc.getString("descripcionProducto") ?: "",
                        stockProducto   = stockInt,
                        imagenes        = doc.get("imagenes") as? List<String> ?: emptyList()
                    )
                }
                callback(products)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }
}

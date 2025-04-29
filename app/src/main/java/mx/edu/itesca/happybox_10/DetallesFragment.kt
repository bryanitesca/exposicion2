package mx.edu.itesca.happybox_10

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class DetallesFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var previousButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_detalles, container, false)

        viewPager = view.findViewById(R.id.bannerViewPager)
        tabLayout = view.findViewById(R.id.bannerIndicator)
        previousButton = view.findViewById(R.id.previousButton)
        nextButton = view.findViewById(R.id.nextButton)
        recyclerView = view.findViewById(R.id.productsRecyclerView)

        setupBanner()
        setupProductList()

        return view
    }

    private fun setupBanner() {
        db.collection("Productos")
            .whereEqualTo("tipoProducto", "Detalles")
            .limit(5)
            .get()
            .addOnSuccessListener { result ->
                // mapeo usando extensión para evitar duplicar código
                val bannerProducts = result.map { it.toProduct() }
                val bannerAdapter = BannerAdapter(bannerProducts) { position ->
                    val args = Bundle().apply { putInt("bannerPosition", position) }
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.nav_detalles, false)
                        .build()
                    findNavController().navigate(
                        R.id.action_nav_detalles_to_detallesOffersFragment,
                        args,
                        navOptions
                    )
                }
                viewPager.adapter = bannerAdapter

                // efecto banner infinito
                val mid = Int.MAX_VALUE / 2
                viewPager.setCurrentItem(mid - (mid % bannerProducts.size), false)

                // indicadores
                tabLayout.removeAllTabs()
                repeat(bannerProducts.size) { tabLayout.addTab(tabLayout.newTab()) }
                viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(pos: Int) {
                        tabLayout.selectTab(tabLayout.getTabAt(pos % bannerProducts.size))
                    }
                })

                previousButton.setOnClickListener { viewPager.currentItem-- }
                nextButton.setOnClickListener { viewPager.currentItem++ }
            }
    }

    private fun setupProductList() {
        recyclerView.layoutManager = LinearLayoutManager(context)
        db.collection("Productos")
            .whereEqualTo("tipoProducto", "Detalles")
            .get()
            .addOnSuccessListener { result ->
                val products = result.map { it.toProduct() }
                recyclerView.adapter = ProductAdapter(products)
            }
    }

    /** Extensión para convertir un DocumentSnapshot en Product de forma robusta */
    private fun DocumentSnapshot.toProduct(): Product {
        // leemos stock de forma genérica
        val rawStock = this.get("stockProducto")
        val stockInt = when (rawStock) {
            is Long   -> rawStock.toInt()
            is Double -> rawStock.toInt()
            is String -> rawStock.toIntOrNull() ?: 0
            else      -> 0
        }
        return Product(
            id                  = id,
            nombreProducto      = getString("nombreProducto") ?: "",
            precioProducto      = getDouble("precioProducto") ?: 0.0,
            descuentoProducto   = getDouble("descuentoProducto") ?: 0.0,
            tipoProducto        = getString("tipoProducto") ?: "",
            descripcionProducto = getString("descripcionProducto") ?: "",
            stockProducto       = stockInt,
            imagenes            = get("imagenes") as? List<String> ?: emptyList()
        )
    }
}

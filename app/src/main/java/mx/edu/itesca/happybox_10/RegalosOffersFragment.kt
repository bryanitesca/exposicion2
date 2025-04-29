package mx.edu.itesca.happybox_10

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RegalosOffersFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_regalos_offers, container, false)

        val recyclerView: RecyclerView = view.findViewById(R.id.offersRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // obtenemos la posición del banner clicado
        val bannerPos = arguments?.getInt("bannerPosition") ?: 0

        // Volvemos a traer sólo los banners de Regalos y mostramos ofertas a partir de ahí
        BannerAdapter.fetchBannerProducts { allBanners ->
            val banners = allBanners.filter { it.tipoProducto == "Regalos" }
            if (banners.size > bannerPos) {
                // podrías usar banners[bannerPos] para algo; aquí solo cargamos ofertas genéricas
            }
            // Ejemplo de ofertas estáticas o puedes cargarlas también de Firestore
            val ofertas = listOf(
                Product(
                    id = "",
                    nombreProducto = "Oferta Regalo A",
                    precioProducto = 19.99,
                    descuentoProducto = 0.0,
                    tipoProducto = "Regalos",
                    descripcionProducto = "Descripción oferta A",
                    stockProducto = 10,
                    imagenes = listOf()
                ),
                Product(
                    id = "",
                    nombreProducto = "Oferta Regalo B",
                    precioProducto = 9.99,
                    descuentoProducto = 0.0,
                    tipoProducto = "Regalos",
                    descripcionProducto = "Descripción oferta B",
                    stockProducto = 5,
                    imagenes = listOf()
                )
            )
            recyclerView.adapter = ProductAdapter(ofertas)
        }

        return view
    }
}

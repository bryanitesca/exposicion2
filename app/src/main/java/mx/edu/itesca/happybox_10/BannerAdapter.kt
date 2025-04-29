package mx.edu.itesca.happybox_10

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class BannerAdapter(
    private val bannerProducts: List<Product>,
    private val onBannerClick: (Int) -> Unit
) : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.banner_item, parent, false)
        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        val realPosition = position % bannerProducts.size
        holder.bind(bannerProducts[realPosition], realPosition)
    }

    override fun getItemCount(): Int = Int.MAX_VALUE

    inner class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.bannerImage)

        fun bind(product: Product, realPosition: Int) {
            // Cargar la primera imagen del array 'imagenes'
            if (product.imagenes.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(product.imagenes[0]) // Usar la primera URL de la lista
                    .centerCrop()
                    .into(imageView)
            }
            imageView.setOnClickListener { onBannerClick(realPosition) }
        }
    }

    companion object {
        // Método para obtener productos de Firestore (puedes llamarlo desde el Fragment)
        fun fetchBannerProducts(callback: (List<Product>) -> Unit) {
            val db = FirebaseFirestore.getInstance()
            db.collection("Productos")
                .whereIn("tipoProducto", listOf("Globos", "Detalles", "Regalos", "Tazas", "Peluches"))
                .limit(5) // Limitar a 5 banners
                .get()
                .addOnSuccessListener { result ->
                    val products = result.map { doc ->
                        Product(
                            id = doc.id,
                            nombreProducto = doc.getString("nombreProducto") ?: "",
                            precioProducto = doc.getDouble("precioProducto") ?: 0.0,
                            descuentoProducto = doc.getDouble("descuentoProducto") ?: 0.0,
                            tipoProducto = doc.getString("tipoProducto") ?: "",
                            descripcionProducto = doc.getString("descripcionProducto") ?: "",
                            stockProducto = doc.getLong("stockProducto")?.toInt() ?: 0,
                            imagenes = doc.get("imagenes") as? List<String> ?: emptyList()
                        )
                    }
                    callback(products)
                }
                .addOnFailureListener { e ->
                    callback(emptyList())
                }
        }
    }
}
package mx.edu.itesca.happybox_10

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ProductAdapter(
    private val products: List<Product>
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    // Si se fija, anula el comportamiento por defecto
    private var onItemClickListener: ((Product) -> Unit)? = null

    fun setOnItemClickListener(listener: (Product) -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ProductViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.product_item, parent, false)
        )

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView         = itemView.findViewById<ImageView>(R.id.productImage)
        private val nameTextView      = itemView.findViewById<TextView>(R.id.productName)
        private val priceTextView     = itemView.findViewById<TextView>(R.id.productPrice)
        private val discountTextView  = itemView.findViewById<TextView>(R.id.productDiscount)
        private val stockTextView     = itemView.findViewById<TextView>(R.id.productStock)
        private val descriptionTextView = itemView.findViewById<TextView>(R.id.productDescription)

        fun bind(product: Product) {
            // Imagen
            if (product.imagenes.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(product.imagenes[0])
                    .centerCrop()
                    .into(imageView)
            }

            // Texto
            nameTextView.text        = product.nombreProducto
            priceTextView.text       = "$${"%.2f".format(product.precioProducto)}"
            descriptionTextView.text = product.descripcionProducto

            // Descuento
            if (product.descuentoProducto > 0) {
                discountTextView.text = "-${product.descuentoProducto.toInt()}%"
                discountTextView.visibility = View.VISIBLE
            } else {
                discountTextView.visibility = View.GONE
            }

            // Stock
            stockTextView.text = "Stock: ${product.stockProducto}"

            // Click
            itemView.setOnClickListener {
                onItemClickListener?.invoke(product) ?: run {
                    // comportamiento por defecto: abrir detalle
                    val intent = Intent(itemView.context, ProductDetailActivity::class.java).apply {
                        putExtra("productId", product.id)
                        putStringArrayListExtra("productImages", ArrayList(product.imagenes))
                        putExtra("productName", product.nombreProducto)
                        putExtra("productPrice", product.precioProducto)
                        putExtra("productDescription", product.descripcionProducto)
                    }
                    itemView.context.startActivity(intent)
                }
            }
        }
    }
}

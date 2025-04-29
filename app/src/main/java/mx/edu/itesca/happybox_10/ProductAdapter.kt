package mx.edu.itesca.happybox_10

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ProductAdapter(private val products: List<Product>) :
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private var onItemClickListener: ((Product) -> Unit)? = null

    fun setOnItemClickListener(listener: (Product) -> Unit) {
        onItemClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.product_item, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView       = itemView.findViewById(R.id.productImage)
        private val nameTextView: TextView     = itemView.findViewById(R.id.productName)
        private val priceTextView: TextView    = itemView.findViewById(R.id.productPrice)
        private val discountTextView: TextView = itemView.findViewById(R.id.productDiscount)
        private val stockTextView: TextView    = itemView.findViewById(R.id.productStock)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.productDescription)

        fun bind(product: Product) {
            // Cargar imagen
            if (product.imagenes.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(product.imagenes[0])
                    .centerCrop()
                    .into(imageView)
            }

            // Nombre
            nameTextView.text = product.nombreProducto

            // Precio
            priceTextView.text = "$${"%.2f".format(product.precioProducto)}"

            // Descuento
            if (product.descuentoProducto > 0.0) {
                val pct = product.descuentoProducto.toInt()
                discountTextView.text = "-${pct}%"
                discountTextView.visibility = View.VISIBLE
            } else {
                discountTextView.visibility = View.GONE
            }

            // Stock
            stockTextView.text = "Stock: ${product.stockProducto}"

            // Descripción
            descriptionTextView.text = product.descripcionProducto

            // Click en el producto
            itemView.setOnClickListener {
                onItemClickListener?.invoke(product)
            }
        }
    }
}
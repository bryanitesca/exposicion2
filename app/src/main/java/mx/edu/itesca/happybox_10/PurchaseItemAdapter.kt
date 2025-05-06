package mx.edu.itesca.happybox_10

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class PurchaseItemAdapter(private val items: List<PurchaseItem>) :
    RecyclerView.Adapter<PurchaseItemAdapter.ViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvItemName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvItemPrice)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvItemQuantity)
        val ivImage: ImageView = itemView.findViewById(R.id.ivProductImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_purchase_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with(holder) {
            tvName.text = item.nombre
            tvPrice.text = "$${"%.2f".format(item.precioUnitario)} c/u"
            tvQuantity.text = "Cantidad: ${item.cantidad}"

            val productId = item.idProducto
            if (!productId.isNullOrEmpty()) {
                db.collection("Productos").document(productId).get()
                    .addOnSuccessListener { document ->
                        val imagenes = document.get("imagenes") as? List<*>
                        val primeraImagen = imagenes?.firstOrNull() as? String

                        if (!primeraImagen.isNullOrEmpty()) {
                            Glide.with(ivImage.context)
                                .load(primeraImagen)
                                .placeholder(R.drawable.ic_image_placeholder)
                                .into(ivImage)
                        } else {
                            ivImage.setImageResource(R.drawable.ic_image_placeholder)
                        }
                    }
                    .addOnFailureListener {
                        ivImage.setImageResource(R.drawable.ic_image_placeholder)
                    }
            } else {
                ivImage.setImageResource(R.drawable.ic_image_placeholder)
            }
        }
    }

    override fun getItemCount() = items.size
}

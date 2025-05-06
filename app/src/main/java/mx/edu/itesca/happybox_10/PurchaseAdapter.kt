package mx.edu.itesca.happybox_10

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import mx.edu.itesca.happybox_10.R
import java.text.SimpleDateFormat
import java.util.*

class PurchaseAdapter(
    private val purchases: List<Purchase>,
    private val onItemClick: (Purchase) -> Unit
) : RecyclerView.Adapter<PurchaseAdapter.PurchaseViewHolder>() {

    inner class PurchaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvPurchaseDate)
        val tvTotal: TextView = itemView.findViewById(R.id.tvPurchaseTotal)
        val tvStatus: TextView = itemView.findViewById(R.id.tvPurchaseStatus)
        val tvItemsCount: TextView = itemView.findViewById(R.id.tvItemsCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PurchaseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_purchase, parent, false)
        return PurchaseViewHolder(view)
    }

    override fun onBindViewHolder(holder: PurchaseViewHolder, position: Int) {
        val purchase = purchases[position]
        val dateFormat = SimpleDateFormat("dd MMM yyyy - HH:mm", Locale.getDefault())

        with(holder) {
            tvDate.text = dateFormat.format(purchase.date.toDate())
            tvTotal.text = "$${"%.2f".format(purchase.total)}"
            tvStatus.text = when(purchase.status) {
                "completado" -> "Completada"
                "pendiente" -> "Pendiente"
                else -> purchase.status.replaceFirstChar { it.uppercaseChar() }
            }
            tvItemsCount.text = "${purchase.getRealItemsCount()} ${if(purchase.getRealItemsCount() > 1) "artículos" else "artículo"}"

            // Configura colores según estado
            val (textColor, bgColor) = when(purchase.status.lowercase(Locale.getDefault())) {
                "completado" -> Pair(R.color.green, R.color.green)
                "pendiente" -> Pair(R.color.orange, R.color.orange)
                else -> Pair(R.color.gray, R.color.gray)
            }
            tvStatus.setTextColor(ContextCompat.getColor(itemView.context, textColor))
            tvStatus.background = ContextCompat.getDrawable(itemView.context, bgColor)

            // 👉 Agregar clic al item
            itemView.setOnClickListener {
                onItemClick(purchase)
            }
        }
    }


    override fun getItemCount() = purchases.size
}

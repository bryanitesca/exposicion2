package mx.edu.itesca.happybox_10

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CartAdapter(
    private val items: MutableList<CartItem>,
    private val onQuantityChange: (CartItem, Int) -> Unit,
    private val onItemRemove: (CartItem) -> Unit,
    private val onSelectionChange: () -> Unit
) : RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.cartProductImage)
        val name: TextView = itemView.findViewById(R.id.cartProductName)
        val price: TextView = itemView.findViewById(R.id.cartProductPrice)
        val quantity: TextView = itemView.findViewById(R.id.cartProductQuantity)
        val decreaseBtn: View = itemView.findViewById(R.id.decreaseButton)
        val increaseBtn: View = itemView.findViewById(R.id.increaseButton)
        val removeBtn: View = itemView.findViewById(R.id.removeButton)
        val selectionCheckbox: CheckBox = itemView.findViewById(R.id.selectionCheckbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .into(holder.image)

        holder.name.text = item.name
        holder.price.text = "$${"%.2f".format(item.price * item.quantity)}"
        holder.quantity.text = item.quantity.toString()
        holder.selectionCheckbox.isChecked = item.selected

        holder.selectionCheckbox.setOnCheckedChangeListener { _, isChecked ->
            item.selected = isChecked
            onSelectionChange()
        }

        holder.decreaseBtn.setOnClickListener {
            if (item.quantity > 1) {
                onQuantityChange(item, -1)
            }
        }

        holder.increaseBtn.setOnClickListener {
            if (item.quantity < 10) {
                onQuantityChange(item, 1)
            } else {
                Toast.makeText(holder.itemView.context,
                    "Límite máximo de 10 unidades por producto", Toast.LENGTH_SHORT).show()
            }
        }

        holder.removeBtn.setOnClickListener {
            onItemRemove(item)
        }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<CartItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
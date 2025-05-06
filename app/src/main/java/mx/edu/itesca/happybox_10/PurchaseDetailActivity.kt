package mx.edu.itesca.happybox_10

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore

class PurchaseDetailActivity : BaseDrawerActivity() {

    override val contentLayoutId = R.layout.activity_purchase_detail

    private val db = FirebaseFirestore.getInstance()
    private val items = mutableListOf<PurchaseItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rvItems = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvItems)
        rvItems.layoutManager = LinearLayoutManager(this)
        rvItems.adapter = PurchaseItemAdapter(items)

        val purchaseId = intent.getStringExtra("purchaseId") ?: return
        db.collection("Ventas").document(purchaseId)
            .get()
            .addOnSuccessListener { doc ->
                findViewById<android.widget.TextView>(R.id.tvPurchaseId).text = doc.id
                findViewById<android.widget.TextView>(R.id.tvPurchaseDate).text = doc.getTimestamp("fecha")?.toDate().toString()
                findViewById<android.widget.TextView>(R.id.tvPurchaseTotal).text = "$${doc.getDouble("total")}"
                findViewById<android.widget.TextView>(R.id.tvPurchaseStatus).text = doc.getString("estado")
            }

        // Cargar subcolección DetalleVenta
        db.collection("Ventas").document(purchaseId)
            .collection("DetalleVenta")
            .get()
            .addOnSuccessListener { docs ->
                items.clear()
                for (d in docs) {
                    val item = d.toObject(PurchaseItem::class.java)
                    items.add(item)
                }
                rvItems.adapter?.notifyDataSetChanged()
            }
    }
}

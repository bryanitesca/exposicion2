package mx.edu.itesca.happybox_10

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class PurchasesActivity : BaseDrawerActivity() {

    override val contentLayoutId = R.layout.activity_purchases

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()
    private val purchasesList = mutableListOf<Purchase>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // RecyclerView & SwipeRefresh ya están en activity_purchases.xml
        val rv    = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvPurchases)
        val swipe = findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
        val empty = findViewById<android.widget.TextView>(R.id.emptyView)

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = PurchaseAdapter(purchasesList) { purchase ->
            startActivity(
                android.content.Intent(this, PurchaseDetailActivity::class.java)
                    .putExtra("purchaseId", purchase.id)
                    .putExtra("paymentIntentId", purchase.paymentIntentId)
            )
        }

        swipe.setOnRefreshListener { loadPurchases(rv, empty, swipe) }
        loadPurchases(rv, empty, swipe)
    }

    private fun loadPurchases(
        rv: androidx.recyclerview.widget.RecyclerView,
        empty: android.widget.TextView,
        swipe: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    ) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("Ventas")
            .whereEqualTo("userId", uid)
            .orderBy("fecha", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { docs ->
                purchasesList.clear()
                for (d in docs) {
                    val p = d.toObject(Purchase::class.java).apply {
                        id = d.id
                        if (itemsCount == 0) itemsCount = d.get("itemsCount") as? Int ?: 1
                    }
                    purchasesList.add(p)
                }
                swipe.isRefreshing = false
                if (purchasesList.isEmpty()) {
                    rv.visibility = View.GONE; empty.visibility = View.VISIBLE
                } else {
                    rv.visibility = View.VISIBLE; empty.visibility = View.GONE
                    rv.adapter?.notifyDataSetChanged()
                }
            }
    }
}

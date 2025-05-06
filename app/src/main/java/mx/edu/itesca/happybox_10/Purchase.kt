package mx.edu.itesca.happybox_10

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Purchase(
    var id: String = "",
    val fecha: Timestamp? = null,
    val estado: String? = null,

    @PropertyName("userId")
    val userId: String = "",

    @PropertyName("fecha")
    val date: Timestamp = Timestamp.now(),

    @PropertyName("total")
    val total: Double = 0.0,

    @PropertyName("estado")
    val status: String = "completado",

    @PropertyName("paymentIntentId")
    val paymentIntentId: String = "",

    @PropertyName("itemsCount")
    var itemsCount: Int = 0
) {
    // Función para obtener la cantidad correcta de artículos
    fun getRealItemsCount(): Int {
        return if (itemsCount > 0) itemsCount else 1
    }
}
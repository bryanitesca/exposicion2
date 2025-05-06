package mx.edu.itesca.happybox_10

data class PurchaseItem(
    val idProducto: String? = null,
    var id: String = "",
    val productId: String = "",
    val nombre: String = "",
    val precioUnitario: Double = 0.0,
    val cantidad: Int = 0,
    val subtotal: Double = 0.0
)

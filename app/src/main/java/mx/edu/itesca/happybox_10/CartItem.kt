package mx.edu.itesca.happybox_10

data class CartItem(
    val id: String = "",
    val productId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val imageUrl: String = ""
) {
    // Constructor sin parámetros necesario para Firestore
    constructor() : this("", "", "", 0.0, 1, "")
}
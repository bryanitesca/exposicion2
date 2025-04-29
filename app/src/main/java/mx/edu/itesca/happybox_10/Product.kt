package mx.edu.itesca.happybox_10

data class Product(
    val id: String = "",
    val nombreProducto: String = "",
    val precioProducto: Double = 0.0,
    val descuentoProducto: Double = 0.0,
    val tipoProducto: String = "",
    val descripcionProducto: String = "",
    val stockProducto: Int = 0,
    val imagenes: List<String> = emptyList()
)

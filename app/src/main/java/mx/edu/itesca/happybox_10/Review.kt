package mx.edu.itesca.happybox_10

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Review(
    @DocumentId val id: String = "",
    val calificacionReview: String = "", // Firestore lo guarda como String
    val comentarioReview: String = "",
    val fechaReview: Timestamp? = null,
    val idUsuario: String = "",
    var nombreUsuario: String = "" // Cambiado a var para permitir modificación
) {
    // Función para convertir calificación a Float para mostrar en RatingBar
    fun ratingFloat(): Float = calificacionReview.toFloatOrNull() ?: 0f
}
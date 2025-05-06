package mx.edu.itesca.happybox_10

import com.google.firebase.Timestamp

data class Sale(
    val id: String = "",
    val userId: String = "",
    val date: Timestamp = Timestamp.now(),
    val total: Double = 0.0,
    val status: String = ""
) {
    constructor(): this("","",Timestamp.now(),0.0,"")
}
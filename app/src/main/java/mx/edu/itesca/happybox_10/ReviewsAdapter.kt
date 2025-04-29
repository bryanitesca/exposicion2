package mx.edu.itesca.happybox_10

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ReviewsAdapter(private val reviews: List<Review>) : RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(reviews[position])
    }

    override fun getItemCount(): Int = reviews.size

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rbRating: RatingBar = itemView.findViewById(R.id.rbReviewRating)
        private val tvComment: TextView = itemView.findViewById(R.id.tvReviewComment)
        private val tvDate: TextView = itemView.findViewById(R.id.tvReviewDate)
        private val tvUser: TextView = itemView.findViewById(R.id.tvReviewUser)

        fun bind(review: Review) {
            rbRating.rating = review.ratingFloat()
            tvComment.text = review.comentarioReview

            // Formatear fecha
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            tvDate.text = review.fechaReview?.toDate()?.let { dateFormat.format(it) } ?: "Fecha desconocida"

            // Mostrar nombre de usuario o "Anónimo" si no hay
            tvUser.text = review.nombreUsuario.ifEmpty { "Anónimo" }
        }
    }
}
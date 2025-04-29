package mx.edu.itesca.happybox_10

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import mx.edu.itesca.happybox_10.databinding.ItemImageSliderBinding

class ImageSliderAdapter(
    private val images: List<String>
) : RecyclerView.Adapter<ImageSliderAdapter.SliderViewHolder>() {

    inner class SliderViewHolder(val binding: ItemImageSliderBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
        val binding = ItemImageSliderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SliderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
        Glide.with(holder.binding.root)
            .load(images[position])
            .centerCrop()
            .into(holder.binding.sliderImage)
    }

    override fun getItemCount(): Int = images.size
}

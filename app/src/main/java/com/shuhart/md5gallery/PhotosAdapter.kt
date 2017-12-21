package com.shuhart.md5gallery

import android.net.Uri
import android.support.v7.widget.RecyclerView
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.shuhart.md5gallery.media.Photo
import com.squareup.picasso.Picasso
import java.io.File

class PhotosAdapter(
        private val itemWidth: Int,
        private val itemHeight: Int,
        private val picasso: Picasso) : RecyclerView.Adapter<PhotosAdapter.ViewHolder>() {
    private var photos = mutableListOf<Photo>()

    fun addPhoto(photo: Photo) {
        photos.add(photo)
        notifyItemInserted(photos.size - 1)
    }

    fun setPhotos(photos: MutableList<Photo>) {
        this.photos = photos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        view.layoutParams = RecyclerView.LayoutParams(itemWidth, itemHeight)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = photos.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(photos[position])
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.image)
        val nameTextView: TextView = itemView.findViewById(R.id.name)
        val md5TextView: TextView = itemView.findViewById(R.id.md5)
        val sizeTextView: TextView = itemView.findViewById(R.id.size)

        fun bind(photo: Photo) {
            if (photo.isProviderPath) {
                picasso.load(photo.thumbPath).tag(imageView.context).into(imageView)
            } else {
                picasso.load(File(photo.thumbPath)).tag(imageView.context).into(imageView)
            }
            sizeTextView.text = Formatter.formatShortFileSize(imageView.context, photo.size)
            nameTextView.text = Uri.parse(photo.path).lastPathSegment
            md5TextView.text = photo.md5
        }
    }
}
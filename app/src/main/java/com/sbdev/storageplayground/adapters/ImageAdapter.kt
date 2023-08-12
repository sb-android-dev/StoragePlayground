package com.sbdev.storageplayground.adapters

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sbdev.storageplayground.databinding.ItemImageDataBinding
import com.sbdev.storageplayground.models.FileData
import com.sbdev.storageplayground.toReadableFileSize

class ImageAdapter(private val resolver: ContentResolver, private val list: List<FileData>, private val onClick: (Uri) -> Unit) :
    RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding =
            ItemImageDataBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bindView(list[position])
    }

    override fun getItemCount(): Int = list.size

    inner class ImageViewHolder(private val binding: ItemImageDataBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindView(data: FileData) {
            binding.ivThumbnail.setImageURI(data.fileUri)
            binding.tvFileName.text = data.fileName
            binding.tvFileSize.text = data.fileSize.toReadableFileSize()
            binding.tvFileType.text = data.fileMimeType

            val parcelFileDescriptor = resolver.openFileDescriptor(data.fileUri, "r")
            parcelFileDescriptor?.apply {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFileDescriptor(this.fileDescriptor, null, options)
                val width = options.outWidth
                val height = options.outHeight

                binding.tvImageResolution.text = String.format("%d x %d px", width, height)
            }.also {
                it?.close()
            }

            binding.btnView.setOnClickListener {
                onClick(data.fileUri)
            }

        }
    }
}
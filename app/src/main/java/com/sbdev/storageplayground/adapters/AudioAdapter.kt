package com.sbdev.storageplayground.adapters

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sbdev.storageplayground.R
import com.sbdev.storageplayground.databinding.ItemAudioDataBinding
import com.sbdev.storageplayground.models.FileData
import com.sbdev.storageplayground.toReadableBitrate
import com.sbdev.storageplayground.toReadableDuration
import com.sbdev.storageplayground.toReadableFileSize

class AudioAdapter(
    private val context: Context,
    private val list: List<FileData>,
    private val onClick: (uri: Uri, name: String, duration: Long) -> Unit
) : RecyclerView.Adapter<AudioAdapter.AudioViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val binding =
            ItemAudioDataBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AudioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        holder.bindView(list[position])
    }

    override fun getItemCount(): Int = list.size

    inner class AudioViewHolder(private val binding: ItemAudioDataBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bindView(item: FileData) {
            binding.tvFileName.text = item.fileName
            binding.tvFileSize.text = item.fileSize.toReadableFileSize()
            binding.tvFileType.text = item.fileMimeType

            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, item.fileUri)

            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            binding.tvFileDuration.text = duration?.toLong()?.toReadableDuration() ?: "0s"

            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            binding.tvAudioBitrate.text = bitrate?.toLong()?.toReadableBitrate() ?: "No bitrate found"

            binding.ivThumbnail.setImageResource(R.drawable.select_audio)

            binding.btnPlay.setOnClickListener {
                onClick(item.fileUri, item.fileName, duration?.toLong() ?: 0)
            }
        }
    }
}
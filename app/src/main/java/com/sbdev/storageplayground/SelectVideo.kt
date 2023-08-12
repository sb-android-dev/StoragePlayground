package com.sbdev.storageplayground

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.sbdev.storageplayground.adapters.VideoAdapter
import com.sbdev.storageplayground.databinding.ActivitySelectVideoBinding
import com.sbdev.storageplayground.models.FileData

class SelectVideo : AppCompatActivity() {

    private lateinit var binding: ActivitySelectVideoBinding

    private var adapter: VideoAdapter? = null

    private var fileUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySelectVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSelectVideo.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_MEDIA_VIDEO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionRequester.launch(Manifest.permission.READ_MEDIA_VIDEO)
                    return@setOnClickListener
                }
            } else {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionRequester.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    return@setOnClickListener
                }
            }

            if (binding.switchMultipleFiles.isChecked) {
                multipleVideoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            } else {
                singleVideoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            }
        }

        binding.fabPlay.setOnClickListener {
            Intent(this, VideoPlayer::class.java).apply {
                putExtra("file_uri", fileUri.toString())
            }.also { intent ->
                startActivity(intent)
            }
        }
    }

    private val singleVideoLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { result ->
            result?.let {
                fileUri = it
                binding.clPlaceholder.visibility = View.INVISIBLE
                binding.clSelectedVideo.visibility = View.VISIBLE
                binding.rvVideos.visibility = View.GONE

                showVideoDetails()
            }
        }

    private val multipleVideoLauncher =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { result ->
            result?.let { list ->
                val videoList = mutableListOf<FileData>()

                list.forEach { uri ->
                    val cursor = contentResolver.query(uri, projection, null, null, null)

                    try {
                        cursor?.use {
                            val nameIndex = it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                            val sizeIndex = it.getColumnIndexOrThrow(OpenableColumns.SIZE)
                            val mimeIndex =
                                it.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

                            it.moveToFirst()

                            videoList.add(
                                FileData(
                                    fileUri = uri,
                                    fileName = cursor.getString(nameIndex),
                                    fileSize = cursor.getLong(sizeIndex),
                                    fileMimeType = cursor.getString(mimeIndex)
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(
                            SelectImage::class.java.name,
                            "multipleImageLauncher: ${e.localizedMessage}"
                        )
                    } finally {
                        cursor?.close()
                    }
                }

                if (list.isNotEmpty()) {
                    adapter = VideoAdapter(this, videoList) {
                        Intent(this, VideoPlayer::class.java).apply {
                            putExtra("file_uri", it.toString())
                        }.also { intent ->
                            startActivity(intent)
                        }
                    }
                    binding.rvVideos.adapter = adapter
                    binding.rvVideos.visibility = View.VISIBLE
                    binding.clSelectedVideo.visibility = View.GONE
                    binding.clPlaceholder.visibility = View.GONE
                }
            }
        }

    private val permissionRequester =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            result?.let {
                if (result) {
                    if (binding.switchMultipleFiles.isChecked) {
                        multipleVideoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    } else {
                        singleVideoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    }
                }
            }
        }

    private fun showVideoDetails() {
        Log.d(SelectVideo::class.java.name, "showVideoDetails: $fileUri")

        fileUri?.let { uri ->
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, uri)

            val cursor = contentResolver.query(uri, projection, null, null, null)

            try {
                cursor?.use {
                    val nameIndex = it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndexOrThrow(OpenableColumns.SIZE)
                    val mimeIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

                    it.moveToFirst()

                    binding.tvVideoFileName.text = it.getString(nameIndex)
                    binding.tvVideoFileSize.text = it.getLong(sizeIndex).toReadableFileSize()
                    binding.tvVideoFileType.text = it.getString(mimeIndex)
                }

                val width =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val height =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                binding.tvVideoResolution.text = String.format("%s x %s", width, height)

                val duration =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                binding.tvVideoDuration.text = duration?.toLong()?.toReadableDuration() ?: "0s"

                binding.ivVideoThumbnail.setImageBitmap(retriever.getFrameAtTime(1))

            } catch (e: Exception) {
                Log.e(SelectVideo::class.java.name, "showVideoDetails: ${e.localizedMessage}")
            } finally {
                cursor?.close()
                retriever.release()
            }
        }
    }
}
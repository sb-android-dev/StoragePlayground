package com.sbdev.storageplayground

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
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
import com.sbdev.storageplayground.adapters.ImageAdapter
import com.sbdev.storageplayground.databinding.ActivitySelectImageBinding
import com.sbdev.storageplayground.models.FileData

class SelectImage : AppCompatActivity() {

    private lateinit var binding: ActivitySelectImageBinding

    private var adapter: ImageAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySelectImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSelectImage.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionRequester.launch(Manifest.permission.READ_MEDIA_IMAGES)
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
                multipleImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                singleImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
    }

    private val singleImageLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { result ->
            result?.let {
                binding.clPlaceholder.visibility = View.INVISIBLE
                binding.clSelectedImage.visibility = View.VISIBLE
                binding.rvImages.visibility = View.GONE
                binding.clPlaceholder.visibility = View.GONE
                binding.ivSelectedImage.setImageURI(it)
                showImageDetails(it)
            }
        }

    private val multipleImageLauncher =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { result ->
            result?.let { list ->
                val imageList = mutableListOf<FileData>()

                list.forEach { uri ->
                    val cursor = contentResolver.query(uri, projection, null, null, null)

                    try {
                        cursor?.use {
                            val nameIndex = it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                            val sizeIndex = it.getColumnIndexOrThrow(OpenableColumns.SIZE)
                            val mimeIndex =
                                it.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

                            it.moveToFirst()

                            imageList.add(
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

                if(list.isNotEmpty()) {
                    adapter = ImageAdapter(contentResolver, imageList) {
                        Intent(this, ImagePreview::class.java).apply {
                            putExtra("file_uri", it.toString())
                        }.also { intent ->
                            startActivity(intent)
                        }
                    }
                    binding.rvImages.adapter = adapter
                    binding.rvImages.visibility = View.VISIBLE
                    binding.clSelectedImage.visibility = View.GONE
                    binding.clPlaceholder.visibility = View.GONE
                }
            }
        }

    private val permissionRequester =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            result?.let {
                if (result) {
                    if (binding.switchMultipleFiles.isChecked) {
                        multipleImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    } else {
                        singleImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }
            }
        }

    private fun showImageDetails(uri: Uri) {
        Log.d(SelectImage::class.java.name, "showImageDetails: $uri")

        val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")

        val cursor = contentResolver.query(uri, projection, null, null, null)

        try {
            cursor?.use {
                val nameIndex = it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndexOrThrow(OpenableColumns.SIZE)
                val mimeIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

                it.moveToFirst()

                binding.tvImageFileName.text = cursor.getString(nameIndex)
                binding.tvImageFileSize.text = cursor.getLong(sizeIndex).toReadableFileSize()
                binding.tvImageFileType.text = cursor.getString(mimeIndex)
            }

            parcelFileDescriptor?.let {
                val fileDescriptor = it.fileDescriptor
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options)
                val width = options.outWidth
                val height = options.outHeight

                binding.tvImageFileResolution.text = String.format("%d x %d", width, height)

                Log.d(SelectImage::class.java.name, "showImageDetails: width: $width")
                Log.d(SelectImage::class.java.name, "showImageDetails: height: $height")
            }

        } catch (e: Exception) {
            Log.e(SelectImage::class.java.name, "showImageDetails: ${e.localizedMessage}")
        } finally {
            cursor?.close()
            parcelFileDescriptor?.close()
        }
    }
}
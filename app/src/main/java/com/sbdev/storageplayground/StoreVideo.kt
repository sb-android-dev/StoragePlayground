package com.sbdev.storageplayground

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.sbdev.storageplayground.databinding.ActivityStoreVideoBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService

class StoreVideo : AppCompatActivity() {

    private lateinit var binding: ActivityStoreVideoBinding

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStoreVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermission()
        }

        binding.btnRecordVideo.setOnClickListener {
            if (!this.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                Toast.makeText(
                    this,
                    "Sorry!, could not find any camera hardware",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            recordVideo()
        }
    }

    private fun recordVideo() {
        val videoCapture = this.videoCapture ?: return

        binding.btnRecordVideo.isEnabled = false

        val curRecording = recording
        if(curRecording != null) {
            curRecording.stop()
            recording = null
            return
        }

        val name = SimpleDateFormat(
            FILENAME_FORMAT,
            Locale.getDefault()
        ).format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val outputOptions = MediaStoreOutputOptions.Builder(
            contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        recording = videoCapture.output.prepareRecording(this, outputOptions).apply {
            if(PermissionChecker.checkSelfPermission(this@StoreVideo, Manifest.permission.RECORD_AUDIO) == PermissionChecker.PERMISSION_GRANTED) {
                withAudioEnabled()
            }
        }.start(ContextCompat.getMainExecutor(this)) { recordEvent ->
            when(recordEvent) {
                is VideoRecordEvent.Start -> {
                    binding.btnRecordVideo.apply {
                        text = getString(R.string.stop)
                        isEnabled = true
                    }
                }

                is VideoRecordEvent.Finalize -> {
                    if(!recordEvent.hasError()) {
                        val msg = "Video record succeeded: ${recordEvent.outputResults.outputUri}"
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        Log.d(StoreVideo::class.java.name, msg)
                    } else {
                        recording?.close()
                        recording = null
                        Log.e(StoreVideo::class.java.name, "Video recording ends with error: ${recordEvent.error}")
                    }

                    binding.btnRecordVideo.apply {
                        text = getString(R.string.record_video)
                        isEnabled = true
                    }
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HIGHEST)).build()
            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)
            } catch (e: Exception) {
                Log.e(StoreImage::class.java.name, "Use case binding failed", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun requestPermission() {
        resultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }

            if (!permissionGranted) {
                Toast.makeText(baseContext, "Permission request denied", Toast.LENGTH_SHORT)
            } else {
                startCamera()
            }
        }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::cameraExecutor.isInitialized)
            cameraExecutor.shutdown()
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss"
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}
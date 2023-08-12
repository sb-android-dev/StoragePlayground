package com.sbdev.storageplayground

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.sbdev.storageplayground.databinding.ActivityStoreAudioBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

class StoreAudio : AppCompatActivity() {

    private lateinit var binding: ActivityStoreAudioBinding

    private var recorder: MediaRecorder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStoreAudioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!allPermissionsGranted()) {
            requestPermission()
        }

        binding.btnRecordAudio.setOnClickListener {
            if (!this.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
                Toast.makeText(
                    this,
                    "Sorry!, could not find any microphone hardware",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            recordAudio()
        }
    }

    private fun recordAudio() {

        if (recorder != null) {
            stopRecording()
            return
        }

        val name = SimpleDateFormat(
            FILENAME_FORMAT,
            Locale.getDefault()
        ).format(System.currentTimeMillis())

        val file = File(getExternalFilesDir(DIRECTORY_DOWNLOADS), "$name.mp3")

        Log.d(StoreAudio::class.java.name, "recordAudio: file path: ${file.absolutePath}")

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/MediaRecorder")
            }
        }

        val fileUri = contentResolver.insert(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
//
//        val outputOptions = MediaStoreOutputOptions.Builder(
//            contentResolver, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
//        ).setContentValues(contentValues).build()

        fileUri?.let { uri ->
            contentResolver.openFileDescriptor(uri, "rw")?.use {
                recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(this)
                } else {
                    MediaRecorder()
                }.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setOutputFile(it.fileDescriptor)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

                    try {
                        prepare()
                    } catch (e: IOException) {
                        Log.e(StoreAudio::class.java.name, "recordAudio: ${e.message}", e)
                    }

                    start()
                }
            }
        }


//        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            MediaRecorder(this)
//        } else {
//            MediaRecorder()
//        }.apply {
//            setAudioSource(MediaRecorder.AudioSource.MIC)
//            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
//            setOutputFile(FileOutputStream(file).fd)
//            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
//
//            try {
//                prepare()
//            } catch (e: IOException) {
//                Log.e(StoreAudio::class.java.name, "recordAudio: ${e.message}", e)
//            }
//
//            start()
//        }
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
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
//                startCamera()
            }
        }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onStop() {
        super.onStop()
        recorder?.release()
        recorder = null
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss"
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}
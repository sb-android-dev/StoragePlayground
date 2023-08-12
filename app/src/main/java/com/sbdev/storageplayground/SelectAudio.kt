package com.sbdev.storageplayground

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import com.sbdev.storageplayground.adapters.AudioAdapter
import com.sbdev.storageplayground.databinding.ActivitySelectAudioBinding
import com.sbdev.storageplayground.dialogs.AudioDialog
import com.sbdev.storageplayground.models.FileData

class SelectAudio : AppCompatActivity() {

    private lateinit var binding: ActivitySelectAudioBinding

    private var mediaPlayer: MediaPlayer? = null

    private var adapter: AudioAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySelectAudioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSelectAudio.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_MEDIA_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionRequester.launch(Manifest.permission.READ_MEDIA_AUDIO)
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
                multipleAudioLauncher.launch(arrayOf("audio/*"))
            } else {
                singleAudioLauncher.launch(arrayOf("audio/*"))
            }
        }

        binding.btnPlayPause.setOnClickListener {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    binding.btnPlayPause.text = getString(R.string.play)
                    binding.btnPlayPause.setIconResource(R.drawable.play)
                } else {
                    player.start()
                    binding.btnPlayPause.text = getString(R.string.pause)
                    binding.btnPlayPause.setIconResource(R.drawable.pause)
                    updateProgress()
                }
            }
        }

        binding.btnStop.setOnClickListener {
            mediaPlayer?.let { player ->
                player.pause()
                jumpToInitialPosition()
            }
        }
    }

    private val singleAudioLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { result ->
            result?.let {
                binding.clPlaceholder.visibility = View.INVISIBLE
                binding.clSelectedAudio.visibility = View.VISIBLE
                binding.rvAudios.visibility = View.GONE

                mediaPlayer?.release()
                mediaPlayer = null
                mediaPlayer = MediaPlayer.create(this, it)

                mediaPlayer?.setOnCompletionListener {
                    Log.d(SelectAudio::class.java.name, "onCompletion")
                    jumpToInitialPosition()
                }

                showAudioDetails(it)
            }
        }

    private val multipleAudioLauncher =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { result ->
            result?.let { list ->
                val audioList = mutableListOf<FileData>()

                list.forEach { uri ->
                    val cursor = contentResolver.query(uri, projection, null, null, null)

                    try {
                        cursor?.use {
                            val nameIndex = it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                            val sizeIndex = it.getColumnIndexOrThrow(OpenableColumns.SIZE)
                            val mimeIndex =
                                it.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

                            it.moveToFirst()

                            audioList.add(
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
                            "multipleAudioLauncher: ${e.localizedMessage}"
                        )
                    } finally {
                        cursor?.close()
                    }
                }

                if (list.isNotEmpty()) {
                    adapter = AudioAdapter(this, audioList) { uri, name, duration ->
                        Log.d(SelectAudio::class.java.name, "AudioAdapter: $name")
                        val dialog = AudioDialog()
                        val bundle = bundleOf().apply {
                            putString("file_uri", uri.toString())
                            putString("file_name", name)
                            putLong("duration", duration)
                        }
                        dialog.arguments = bundle
                        dialog.show(supportFragmentManager, "audio_dialog")
                    }
                    binding.rvAudios.adapter = adapter
                    binding.rvAudios.visibility = View.VISIBLE
                    binding.clSelectedAudio.visibility = View.GONE
                    binding.clPlaceholder.visibility = View.GONE
                }
            }
        }


    private fun jumpToInitialPosition() {
        mediaPlayer?.let {
            it.seekTo(0)
            changeProgressUI()
            binding.btnPlayPause.setIconResource(R.drawable.play)
            binding.btnPlayPause.text = getString(R.string.play)
        }
    }

    private val permissionRequester =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            result?.let {
                if (result) {
                    singleAudioLauncher.launch(arrayOf("audio/*"))
                }
            }
        }

    private fun showAudioDetails(fileUri: Uri) {
        Log.d(SelectAudio::class.java.name, "showAudioDetails: $fileUri")

        val cursor = contentResolver.query(fileUri, projection, null, null, null)

        try {
            cursor?.use {
                val nameIndex = it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndexOrThrow(OpenableColumns.SIZE)
                val mimeIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                it.moveToFirst()

                binding.tvAudioFileName.text = cursor.getString(nameIndex)
                binding.tvAudioFileSize.text = cursor.getLong(sizeIndex).toReadableFileSize()
                binding.tvAudioFileType.text = cursor.getString(mimeIndex)
            }

            val totalDuration = (mediaPlayer?.duration ?: 0) / 1000
            Log.d(SelectAudio::class.java.name, "showAudioDetails: $totalDuration")
            val minutes = totalDuration / 60
            val seconds = totalDuration % 60

            binding.lpiAudioTrack.max = mediaPlayer?.duration ?: 0

            binding.tvTotalDuration.text = String.format("%02d:%02d", minutes, seconds)
            binding.tvCurrentDuration.text = getString(R.string.initial_point)

        } catch (e: Exception) {
            Log.e(SelectAudio::class.java.name, "showAudioDetails: ${e.localizedMessage}")
        } finally {
            cursor?.close()
        }
    }

    private fun updateProgress() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                changeProgressUI()

                Handler(Looper.getMainLooper()).postDelayed({
                    updateProgress()
                }, 1000L)
            }
        }
    }

    private fun changeProgressUI() {
        mediaPlayer?.let { player ->
            binding.lpiAudioTrack.progress = player.currentPosition

            val currentDuration = player.currentPosition / 1000
            val minutes = currentDuration / 60
            val seconds = currentDuration % 60
            binding.tvCurrentDuration.text = String.format("%02d:%02d", minutes, seconds)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
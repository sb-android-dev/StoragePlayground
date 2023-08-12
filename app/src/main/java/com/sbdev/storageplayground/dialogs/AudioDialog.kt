package com.sbdev.storageplayground.dialogs

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sbdev.storageplayground.R
import com.sbdev.storageplayground.databinding.DialogAudioBinding
import com.sbdev.storageplayground.toReadableDuration

class AudioDialog: BottomSheetDialogFragment() {

    private lateinit var binding: DialogAudioBinding

    private var mediaPlayer: MediaPlayer? = null
    private var fileUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogAudioBinding.inflate(inflater, container, false)

        arguments?.let {
            fileUri = it.getString("file_uri")?.toUri()

            val name = it.getString("file_name")
            binding.tvAudioFileName.text = name

            val duration = it.getLong("duration", 0)
            binding.tvTotalDuration.text = duration.toReadableDuration()

            binding.lpiAudioTrack.max = duration.toInt()

            initializePlayer()
        }

        binding.fabPlayPause.setOnClickListener {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    binding.fabPlayPause.setImageResource(R.drawable.play)
                } else {
                    player.start()
                    binding.fabPlayPause.setImageResource(R.drawable.pause)
                    binding.fabStop.visibility = View.VISIBLE
                    updateProgress()
                }
            }
        }

        binding.fabStop.setOnClickListener {
            mediaPlayer?.let { player ->
                player.pause()
                jumpToInitialPosition()
            }
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        return binding.root
    }

    private fun initializePlayer() {
        context?.let { ctx ->
            mediaPlayer = MediaPlayer()

            fileUri?.let {
                mediaPlayer?.setDataSource(ctx, it)
                mediaPlayer?.prepareAsync()
            }


            mediaPlayer?.setOnPreparedListener {
                it.start()
                binding.fabPlayPause.setImageResource(R.drawable.pause)
                binding.fabStop.visibility = View.VISIBLE
                Handler(Looper.getMainLooper()).postDelayed({
                    updateProgress()
                }, 1000L)
            }

            mediaPlayer?.setOnCompletionListener {
                Log.d(AudioDialog::class.java.name, "onCompletion")
                jumpToInitialPosition()
            }
        }
    }

    private fun jumpToInitialPosition() {
        mediaPlayer?.let {
            it.seekTo(0)
            changeProgressUI()
            binding.fabPlayPause.setImageResource(R.drawable.play)
            binding.fabStop.visibility = View.GONE
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

            Log.d(AudioDialog::class.java.name, "changeProgressUI: currentPosition: ${player.currentPosition}")
            val currentDuration = player.currentPosition / 1000
            Log.d(AudioDialog::class.java.name, "changeProgressUI: currentDuration: $currentDuration")
            val minutes = currentDuration / 60
            val seconds = currentDuration % 60
            binding.tvCurrentDuration.text = String.format("%02d:%02d", minutes, seconds)
        }
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}
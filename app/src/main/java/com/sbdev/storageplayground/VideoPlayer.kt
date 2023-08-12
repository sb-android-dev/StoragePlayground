package com.sbdev.storageplayground

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.MediaController
import androidx.core.net.toUri
import com.sbdev.storageplayground.databinding.ActivityVideoPlayerBinding

class VideoPlayer : AppCompatActivity() {

    private lateinit var binding: ActivityVideoPlayerBinding

    private var mediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        intent?.let { it ->
            if (it.hasExtra("file_uri")) {
                val fileUri = it.getStringExtra("file_uri")

                if (!fileUri.isNullOrEmpty()) {
                    binding.vvVideoPlayer.setVideoURI(fileUri.toUri())
                    mediaController = MediaController(this)
                    binding.vvVideoPlayer.setMediaController(mediaController)
                    mediaController?.setAnchorView(binding.vvVideoPlayer)
                    binding.vvVideoPlayer.setOnPreparedListener {player ->
                        player.start()
                        mediaController?.show()

                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
}
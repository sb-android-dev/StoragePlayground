package com.sbdev.storageplayground

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.sbdev.storageplayground.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.crdReadImage.setOnClickListener {
            Intent(this, SelectImage::class.java).also { intent ->
                startActivity(intent)
            }
        }

        binding.crdReadVideo.setOnClickListener {
            Intent(this, SelectVideo::class.java).also { intent ->
                startActivity(intent)
            }
        }

        binding.crdReadAudio.setOnClickListener {
            Intent(this, SelectAudio::class.java).also { intent ->
                startActivity(intent)
            }
        }

        binding.crdWriteImage.setOnClickListener {
            Intent(this, StoreImage::class.java).also { intent ->
                startActivity(intent)
            }
        }

        binding.crdWriteVideo.setOnClickListener {
            Intent(this, StoreVideo::class.java).also { intent ->
                startActivity(intent)
            }
        }

        binding.crdWriteAudio.setOnClickListener {
            Intent(this, StoreAudio::class.java).also { intent ->
                startActivity(intent)
            }
        }
    }
}
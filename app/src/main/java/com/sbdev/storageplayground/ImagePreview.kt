package com.sbdev.storageplayground

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.core.net.toUri
import com.sbdev.storageplayground.databinding.ActivityImagePreviewBinding

class ImagePreview : AppCompatActivity() {

    private lateinit var binding: ActivityImagePreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityImagePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        intent?.let {
            if(it.hasExtra("file_uri")) {
                val fileUri = it.getStringExtra("file_uri")

                fileUri?.let {uriString ->
                    binding.ivImagePreview.setImageURI(uriString.toUri())
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if(item.itemId == android.R.id.home) {
            finish()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
}
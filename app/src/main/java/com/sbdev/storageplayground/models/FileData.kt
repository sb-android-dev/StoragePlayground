package com.sbdev.storageplayground.models

import android.net.Uri

data class FileData(
    val fileUri: Uri,
    val fileName: String,
    val fileSize: Long,
    val fileMimeType: String
)

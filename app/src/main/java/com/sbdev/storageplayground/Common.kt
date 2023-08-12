package com.sbdev.storageplayground

import android.provider.MediaStore
import android.provider.OpenableColumns

val projection = arrayOf(
    OpenableColumns.DISPLAY_NAME,
    OpenableColumns.SIZE,
    MediaStore.MediaColumns.MIME_TYPE
)
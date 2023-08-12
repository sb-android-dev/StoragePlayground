package com.sbdev.storageplayground

import android.util.Log
import java.math.BigDecimal
import java.math.RoundingMode

fun Long.toReadableFileSize(): String {
    if(this <= 1000L)
        return "$this B"

    val originalSize = BigDecimal.valueOf(this)
    Log.d("ReadableFileSize", "originalSize: $originalSize")

    val sizeInKB = originalSize.divide(BigDecimal.valueOf(1024), 2, RoundingMode.HALF_EVEN)
    Log.d("ReadableFileSize", "sizeInKB: $sizeInKB")
    if(sizeInKB <= BigDecimal.valueOf(1000))
        return "$sizeInKB kB"

    val sizeInMB = sizeInKB.divide(BigDecimal.valueOf(1024), 2, RoundingMode.HALF_EVEN)
    Log.d("ReadableFileSize", "sizeInMB: $sizeInMB")
    if(sizeInMB <= BigDecimal.valueOf(1000))
        return "$sizeInMB MB"

    val sizeInGB = sizeInMB.divide(BigDecimal.valueOf(1024), 2, RoundingMode.HALF_EVEN)
    Log.d("ReadableFileSize", "sizeInGB: $sizeInGB")
    return "$sizeInGB GB"
}

fun Long.toReadableDuration(): String {
    Log.d("ReadableDuration", "totalDuration: $this")
    val totalDuration = BigDecimal(this).divide(BigDecimal(1000), 2, RoundingMode.HALF_EVEN)
    Log.d("ReadableDuration", "totalDuration: $totalDuration")

    if(totalDuration < BigDecimal(60)) {
        return "${totalDuration.toLong()}s"
    }

    val minutes = totalDuration.divide(BigDecimal(60), 0, RoundingMode.HALF_EVEN)
    Log.d("ReadableDuration", "minutes: $minutes")
    val seconds = totalDuration.remainder(BigDecimal(60))
    Log.d("ReadableDuration", "seconds: $seconds")

    return String.format("%02d:%02d Min", minutes.toLong(), seconds.toLong())
}

fun Long.toReadableBitrate(): String {
    if(this < 1000)
        return this.toString()

    val originalBitrate = BigDecimal(this)

    val bitrateInKB = originalBitrate.divide(BigDecimal(1000), 0, RoundingMode.HALF_EVEN)
    return "$bitrateInKB kbps"
}
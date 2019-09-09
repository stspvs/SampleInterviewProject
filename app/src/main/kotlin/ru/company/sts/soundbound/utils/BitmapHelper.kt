package ru.company.sts.soundbound.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


// Max read limit that we allow our input stream to mark/reset.
private const val MAX_READ_LIMIT_PER_IMG = 1024 * 1024

fun scaleBitmap(src: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
    val scaleFactor = Math.min(
            maxWidth.toDouble() / src.width, maxHeight.toDouble() / src.height)
    return Bitmap.createScaledBitmap(src,
            (src.width * scaleFactor).toInt(), (src.height * scaleFactor).toInt(), false)
}

fun scaleBitmap(scaleFactor: Int, inStr: InputStream): Bitmap {
    // Get the dimensions of the bitmap
    val bmOptions = BitmapFactory.Options()

    // Decode the image file into a Bitmap sized to fill the View
    bmOptions.inJustDecodeBounds = false
    bmOptions.inSampleSize = scaleFactor

    return BitmapFactory.decodeStream(inStr, null, bmOptions)
}

fun findScaleFactor(targetW: Int, targetH: Int, inStr: InputStream): Int {
    // Get the dimensions of the bitmap
    val bmOptions = BitmapFactory.Options()
    bmOptions.inJustDecodeBounds = true
    BitmapFactory.decodeStream(inStr, null, bmOptions)
    val actualW = bmOptions.outWidth
    val actualH = bmOptions.outHeight

    // Determine how much to scale down the image
    return Math.min(actualW / targetW, actualH / targetH)
}

@Throws(IOException::class)
fun fetchAndRescaleBitmap(uri: String, width: Int, height: Int): Bitmap {
    val url = URL(uri)
    var inStr: BufferedInputStream? = null
    try {
        val urlConnection = url.openConnection() as HttpURLConnection
        inStr = BufferedInputStream(urlConnection.inputStream)
        inStr.mark(MAX_READ_LIMIT_PER_IMG)
        val scaleFactor = findScaleFactor(width, height, inStr)
        inStr.reset()
        return scaleBitmap(scaleFactor, inStr)
    } finally {
        if (inStr != null) {
            inStr.close()
        }
    }
}
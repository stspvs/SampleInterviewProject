package ru.company.sts.soundbound.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.support.annotation.IntegerRes
import android.text.TextUtils
import android.util.LruCache
import ru.company.sts.soundbound.R

import java.io.IOException
import java.lang.ref.WeakReference

/**
 * Implements a basic cache of album arts, with async loading support.
 */
class AlbumArtCache private constructor() {

    companion object {
        private const val TAG = "AlbumArtCache"

        private const val MAX_ALBUM_ART_CACHE_SIZE = 12 * 1024 * 1024  // 12 MB
        private const val MAX_ART_WIDTH = 800  // pixels
        private const val MAX_ART_HEIGHT = 480  // pixels

        // Resolution reasonable for carrying around as an icon (generally in
        // MediaDescription.getIconBitmap). This should not be bigger than necessary, because
        // the MediaDescription object should be lightweight. If you set it too high and try to
        // serialize the MediaDescription, you may get FAILED BINDER TRANSACTION errors.
        private const val MAX_ART_WIDTH_ICON = 128  // pixels
        private const val MAX_ART_HEIGHT_ICON = 128  // pixels

        private const val BIG_BITMAP_INDEX = 0
        private const val ICON_BITMAP_INDEX = 1

        private object Holder {
            val INSTANCE = AlbumArtCache()
        }

        val instance: AlbumArtCache by lazy { Holder.INSTANCE }
    }

    private val mCache: LruCache<String, Array<Bitmap>>

    private class AsyncTaskFetch(context: Context, val albumArtCache: AlbumArtCache, val artUrl: String, val listener: FetchListener) : AsyncTask<Void, Void, Array<Bitmap>>() {

        val contextRef: WeakReference<Context> = WeakReference(context)

        override fun doInBackground(objects: Array<Void>): Array<Bitmap>? {
            var bitmaps: Array<Bitmap>? = null
            try {
                var bitmap: Bitmap? = null
                if (albumArtCache.isUrl(artUrl)) {
                    //URL
                    bitmap = fetchAndRescaleBitmap(artUrl, MAX_ART_WIDTH, MAX_ART_HEIGHT)
                } else if (albumArtCache.isResource(artUrl) || TextUtils.isEmpty(artUrl)) {
                    //RESOURCES
                    var resId: Int? = null

                    if (!TextUtils.isEmpty(artUrl)) {
                        resId = artUrl.toIntOrNull()
                    }
                    contextRef.get()?.let {
                        bitmap = if (resId == null) albumArtCache.getResourceBitmap(it, albumArtCache.getDefaultResBitmap())
                        else albumArtCache.getResourceBitmap(it, resId)
                    }
                } else {
                    //FILES
                    bitmap = albumArtCache.getFileBitmap(artUrl)
                }

                bitmap?.let {
                    val bmp = scaleBitmap(it,
                            MAX_ART_WIDTH_ICON, MAX_ART_HEIGHT_ICON)

                    val icon = scaleBitmap(it,
                            MAX_ART_WIDTH_ICON, MAX_ART_HEIGHT_ICON)
                    bitmaps = arrayOf(bmp, icon)
                    albumArtCache.mCache.put(artUrl, bitmaps)
                }
            } catch (e: IOException) {
                Log.e(TAG, e.message)
                return null
            }


            return bitmaps
        }

        override fun onPostExecute(bitmaps: Array<Bitmap>?) {
            if (bitmaps == null) {
                listener.onError(artUrl, IllegalArgumentException("got null bitmaps"))
            } else {
                listener.onFetched(artUrl,
                        bitmaps[BIG_BITMAP_INDEX], bitmaps[ICON_BITMAP_INDEX])
            }
        }

    }

    init {
        // Holds no more than MAX_ALBUM_ART_CACHE_SIZE bytes, bounded by maxmemory/4 and
        // Integer.MAX_VALUE:
        val maxSize = Math.min(MAX_ALBUM_ART_CACHE_SIZE,
                Math.min(Integer.MAX_VALUE.toLong(), Runtime.getRuntime().maxMemory() / 4).toInt())
        mCache = object : LruCache<String, Array<Bitmap>>(maxSize) {
            override fun sizeOf(key: String, value: Array<Bitmap>): Int {
                return value[BIG_BITMAP_INDEX].byteCount + value[ICON_BITMAP_INDEX].byteCount
            }
        }
    }

    private fun isUrl(path: String?): Boolean {
        path?.let { return it.contains("http") }
        return false
    }

    private fun isFile(path: String?): Boolean {
        path?.let { return it.contains("file") || it.contains("content") }
        return false
    }

    private fun isResource(path: String?): Boolean {
        path?.let { return it.matches("^[0-9]*$".toRegex()) }
        return false
    }

    fun getResourceBitmap(context: Context, @IntegerRes resBitmap: Int): Bitmap? {
        return BitmapFactory.decodeResource(context.resources, resBitmap)
    }

    fun getFileBitmap(pathFile: String?): Bitmap? {
        pathFile?.let { return BitmapFactory.decodeFile(pathFile) }
        return null
    }

    fun getDefaultResBitmap(): Int {
        return R.drawable.bg_main
    }

    fun getBigImage(artUrl: String): Bitmap? {
        val result = mCache.get(artUrl)
        return if (result == null) null else result[BIG_BITMAP_INDEX]
    }

    fun getIconImage(artUrl: String): Bitmap? {
        val result = mCache.get(artUrl)
        return if (result == null) null else result[ICON_BITMAP_INDEX]
    }

    fun fetch(context: Context, artUrl: String, listener: FetchListener) {
        // WARNING: for the sake of simplicity, simultaneous multi-thread fetch requests
        // are not handled properly: they may cause redundant costly operations, like HTTP
        // requests and bitmap rescales. For production-level apps, we recommend you use
        // a proper image loading library, like Glide.
        val bitmap = mCache.get(artUrl)
        if (bitmap != null) {
            listener.onFetched(artUrl, bitmap[BIG_BITMAP_INDEX], bitmap[ICON_BITMAP_INDEX])
            return
        }
        AsyncTaskFetch(context, this, artUrl, listener).execute()
    }

    abstract class FetchListener {
        abstract fun onFetched(artUrl: String, bigImage: Bitmap, iconImage: Bitmap)
        fun onError(artUrl: String, e: Exception) {
            Log.e(TAG, "AlbumArtFetchListener: error while downloading $artUrl message: ${e.message}")
        }
    }

}
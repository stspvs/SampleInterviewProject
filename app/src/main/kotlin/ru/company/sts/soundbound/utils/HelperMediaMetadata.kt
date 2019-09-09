package ru.company.sts.soundbound.utils

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.database.Cursor
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.browse.MediaBrowser
import android.net.Uri
import android.provider.MediaStore
import android.support.annotation.DrawableRes
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.text.TextUtils
import ru.company.sts.soundbound.model.ItemMediaCommon
import ru.company.sts.soundbound.model.ItemMediaInList
import ru.company.sts.soundbound.model.ItemMediaStorage


/**
 * Created by sts on 29.03.2018.
 */
/**
 * create an itemMedia from system media metadata
 */
fun getItemFromMetadata(metadata: MediaMetadataCompat): ItemMediaCommon {
    return ItemMediaInList(
            metadata.bundle.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: "",
            metadata.bundle.getString(MediaMetadataCompat.METADATA_KEY_COMPOSER) ?: "",
            metadata.bundle.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) ?: "",
            Uri.parse(metadata.bundle.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI))
                    ?: null,
            Uri.parse(metadata.bundle.getString(MediaMetadataCompat.METADATA_KEY_ART_URI)) ?: null,
            metadata.bundle.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
    )
}

/**
 *create  metadata object from application item media
 */
fun getMetadataFromItem(item: ItemMediaCommon, metadataBuilder: MediaMetadataCompat.Builder, resources: Resources): MediaMetadataCompat {

    metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, item.title)
    metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, item.artist)
    item.duration?.let { metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, it) }
    metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, item.mediaId)
    val iconUri = getUri(resources, item)

    metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ART_URI, iconUri.toString())

    if ((item is ItemMediaInList) && item.bitmapResId != null) {
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, BitmapFactory.decodeResource(resources, item.bitmapResId!!))
    }

    metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, item.uriSource.toString())

    return metadataBuilder.build()
}

/**
 * get Uri by resource identificator
 */
fun getUriFromResId(resources: Resources, @DrawableRes resId: Int): Uri {
    return Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(resId))
            .appendPath(resources.getResourceTypeName(resId))
            .appendPath(resources.getResourceEntryName(resId))
            .build()

}

fun getUri(resources: Resources, track: ItemMediaCommon): Uri? {
    var iconUri: Uri? = track.imgUri
    if (track.imgUri == null && track is ItemMediaInList) {
        iconUri = if (track.bitmapResId != null) getUriFromResId(resources, track.bitmapResId!!) else track.imgUri
    }
    return iconUri
}

/**
 *
 */
fun getMediaItemFromItem(resources: Resources, track: ItemMediaCommon): MediaBrowserCompat.MediaItem {
    val descriptionBuilder = MediaDescriptionCompat.Builder()
    val description = descriptionBuilder
            .setDescription(track.artist)
            .setTitle(track.title)
            .setSubtitle(track.artist)
            .setMediaId(track.mediaId)
            .setIconUri(getUri(resources, track))
            .build()
    return MediaBrowserCompat.MediaItem(description, MediaBrowser.MediaItem.FLAG_PLAYABLE)
}


private fun getValueFromCursor(cursor: Cursor, column: String): String? {
    val index: Int = cursor.getColumnIndex(column)
    var data: String? = null
    if (index >= 0) {
        data = cursor.getString(index)
    }
    return data

}

fun getMediaFromCursor(context: Context, cursor: Cursor?): ItemMediaCommon? {

    cursor.let {
        val durStr = getValueFromCursor(cursor!!, MediaStore.Audio.Media.DURATION)
        val duration: Long? = durStr?.toLongOrNull()
        val dataIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)

        val dataString = cursor.getString(dataIndex)
        val mediaUri = Uri.parse("file://$dataString")
        val mediaId = getValueFromCursor(cursor, MediaStore.Audio.Media._ID)
        val title = getValueFromCursor(cursor, MediaStore.Audio.Media.TITLE)
        val composer = getValueFromCursor(cursor, MediaStore.Audio.Media.COMPOSER)
        val artist = getValueFromCursor(cursor, MediaStore.Audio.Media.ARTIST)

        val artAlb: String? = null
        val idAlb = getValueFromCursor(cursor, MediaStore.Audio.Media.ALBUM_ID)


        val item: ItemMediaCommon = ItemMediaStorage(
                mediaId,
                title ?: "title unknown",
                composer ?: "composer unknown",
                artist,
                mediaUri, if (artAlb != null) Uri.parse(artAlb) else null,
                duration
        )
        idAlb?.let {
            updateFromAlbumInfo(context, it, item)
        }
        if (item.artist == null) {
            updateFromArtistInfo(context, item)
        }
        return item
    }
}

fun updateFromAlbumInfo(context: Context, albumId: String, item: ItemMediaCommon) {
    val projection = arrayOf(MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART,
            MediaStore.Audio.Albums.ARTIST)
    val selection = MediaStore.Audio.Albums._ID + "=" + albumId
    val cursor = context.contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            projection, selection, null, null)

    val path: String?
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART))
            // do whatever you need to do
            cursor.close()
            path?.let { item.imgUri = Uri.parse(it) }

        }
    }
}


fun updateFromArtistInfo(context: Context, item: ItemMediaCommon) {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(context, item.uriSource)
    item.artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
}


//last----------------------------------------------------------

fun getMetadataFromCursor(context: Context, cursor: Cursor?): MediaMetadataCompat? {

    cursor.let {
        val durStr = getValueFromCursor(cursor!!, MediaStore.Audio.Media.DURATION)
        val duration: Long? = durStr?.toLongOrNull()
        val dataIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)

        val dataString = cursor.getString(dataIndex)
        val mediaUri = Uri.parse("file://$dataString")
        val mediaId = getValueFromCursor(cursor, MediaStore.Audio.Media._ID)
        val title = getValueFromCursor(cursor, MediaStore.Audio.Media.TITLE)
        val composer = getValueFromCursor(cursor, MediaStore.Audio.Media.COMPOSER)
        var artist = getValueFromCursor(cursor, MediaStore.Audio.Media.ARTIST)
        if (TextUtils.isEmpty(artist)) {
            artist = getFromArtistInfo(context, mediaUri)
        }
        val idAlb = getValueFromCursor(cursor, MediaStore.Audio.Media.ALBUM_ID)
        val iconUri: Uri? = getAlbumArtUri(context, idAlb)

        val id = mediaId ?: (title ?: "no title"+(artist ?: "no artist")+(duration ?: "-1")
        +(mediaUri ?: "no source"))

        val metadataBuilder = MediaMetadataCompat.Builder()

        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_COMPOSER, composer)
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, mediaUri.toString())
        duration?.let { metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, it) }
        iconUri?.let { metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ART_URI, iconUri.toString()) }

        return metadataBuilder.build()
    }
}


fun getAlbumArtUri(context: Context, albumId: String?): Uri? {
    if (albumId != null) {
        val projection = arrayOf(MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART,
                MediaStore.Audio.Albums.ARTIST)
        val selection = MediaStore.Audio.Albums._ID + "=" + albumId
        val cursor = context.contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                projection, selection, null, null)

        val path: String?
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART))
                // do whatever you need to do
                cursor.close()
                path?.let { return Uri.parse(it) }

            }
        }
    }
    return null
}

fun getFromArtistInfo(context: Context, uriSource: Uri): String {
    var artist: String? = null
    try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uriSource)
        artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
    } catch (e: Exception) {
        Log.d("getFromArtistInfo", "error: ${e.message}")
    }
    return artist ?: ""
}
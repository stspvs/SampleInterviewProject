package ru.company.sts.soundbound.utils

val CUSTOM_METADATA_TRACK_SOURCE = "__SOURCE__"
const val EXTRA_START_FULLSCREEN = "ru.company.sts.soundbound.utils.EXTRA_START_FULLSCREEN"

/**
 * Optionally used with [.EXTRA_START_FULLSCREEN] to carry a MediaDescription to
 * the [FullScreenPlayerActivity], speeding up the screen rendering
 * while the [android.support.v4.media.session.MediaControllerCompat] is connecting.
 */
const val EXTRA_CURRENT_MEDIA_DESCRIPTION = "ru.company.sts.soundbound.CURRENT_MEDIA_DESCRIPTION"

const val CUSTOM_ACTION_GET_CURRENT_QUEUE_ITEM = "ru.stsproject.android.GET_CURRENT_QUEUE_ITEM"
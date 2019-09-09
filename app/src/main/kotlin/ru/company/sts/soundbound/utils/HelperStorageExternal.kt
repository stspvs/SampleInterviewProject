package ru.company.sts.soundbound.utils

import android.os.Environment

fun isExternalStorageAvailable(): Boolean {
    val state = Environment.getExternalStorageState()
    if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
        return true
    }
    return false
}

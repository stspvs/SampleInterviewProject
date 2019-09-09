package ru.company.sts.soundbound.utils

import android.os.Looper

/**
 * Created by sts on 28.03.2018.
 */
class Log {

    companion object {
        @JvmStatic private var ENABLED:Boolean = false

        fun setEnabled(enabled: Boolean) {
            ENABLED = enabled
        }

        fun v(tag: String, msg: String): Int {
            return if (!ENABLED) {
                -1
            } else android.util.Log.v(tag, msg)
        }

        fun v(tag: String, msg: String, tr: Throwable): Int {
            return if (!ENABLED) {
                -1
            } else android.util.Log.v(tag, msg, tr)
        }

        fun d(tag: String, msg: String): Int {
            return if (!ENABLED) {
                -1
            } else android.util.Log.d(tag, msg)
        }

        fun d(tag: String, msg: String, tr: Throwable): Int {
            return if (!ENABLED) {
                -1
            } else android.util.Log.d(tag, msg, tr)
        }

        fun i(tag: String, msg: String): Int {
            return if (!ENABLED) {
                -1
            } else android.util.Log.i(tag, msg)
        }

        fun i(tag: String, msg: String, tr: Throwable): Int {
            return if (!ENABLED) {
                -1
            } else android.util.Log.i(tag, msg, tr)
        }

        fun w(tag: String, msg: String): Int {
            return if (!ENABLED) {
                -1
            } else android.util.Log.w(tag, msg)
        }

        fun w(tag: String, msg: String, tr: Throwable): Int {
            return if (!ENABLED) {
                -1
            } else android.util.Log.w(tag, msg, tr)
        }

        fun w(tag: String, tr: Throwable): Int {
            return if (!ENABLED) {
                -1
            } else android.util.Log.w(tag, tr)
        }

        fun e(tag: String, msg: String?): Int {
            return if (!ENABLED) {
                -1
            } else android.util.Log.e(tag, msg?:"null message")
        }

        fun e(tag: String, msg: String, tr: Throwable): Int {
            return if (!ENABLED) {
                -1
            } else android.util.Log.e(tag, msg, tr)
        }

        fun wtf(tag: String, msg: String): Int {
            return if (!ENABLED) {
                -1
            } else android.util.Log.wtf(tag, msg)
        }

        fun wtf(tag: String, tr: Throwable): Int {
            return if (!ENABLED) {
                -1
            } else android.util.Log.wtf(tag, tr)
        }

        fun wtf(tag: String, msg: String, tr: Throwable): Int {
            return if (!ENABLED) {
                -1
            } else android.util.Log.wtf(tag, msg, tr)
        }

        fun getStackTraceString(throwable: Throwable): String {
            return android.util.Log.getStackTraceString(throwable)
        }

        fun isEnabled(): Boolean {
            return ENABLED
        }

        fun isMainThread(): String {
            return if (ENABLED) " isMainThread = " + (Looper.getMainLooper() == Looper.myLooper()) + "; " else ""
        }
    }
}
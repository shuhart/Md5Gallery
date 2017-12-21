package com.shuhart.md5gallery.utils

import android.util.Log
import com.shuhart.md5gallery.BuildConfig

inline fun <reified T> T.log(e: Throwable) {
    if (BuildConfig.DEBUG) {
        Log.e(T::class.java.simpleName, e.message, e)
    }
}

inline fun <reified T> T.log(message: String, e: Throwable) {
    if (BuildConfig.DEBUG) {
        Log.e(T::class.java.simpleName, message, e)
    }
}

inline fun <reified T> T.log(message: String) {
    if (BuildConfig.DEBUG) {
        Log.d(T::class.java.simpleName, message)
    }
}
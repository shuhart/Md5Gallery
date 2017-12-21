package com.shuhart.md5gallery.utils

import android.util.Log

inline fun <reified T> T.log(e: Throwable) {
    Log.e(T::class.java.simpleName, e.message, e)
}

inline fun <reified T> T.log(message: String) {
    Log.d(T::class.java.simpleName, message)
}
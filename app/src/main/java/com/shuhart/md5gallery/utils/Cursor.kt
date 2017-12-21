package com.shuhart.md5gallery.utils

import android.database.Cursor

inline fun Cursor?.using(block: Cursor.() -> Unit) {
    this ?: return
    try {
        block()
    } catch (e: Throwable) {
        log(e)
    } finally {
        try {
            close()
        } catch (e: Exception) {
            log(e)
        }
    }
}
package com.shuhart.md5gallery.utils

import android.content.Context
import android.preference.PreferenceManager

object PrefUtils {
    fun putString(context: Context, key: String, value: String?) {
        val sp = PreferenceManager
                .getDefaultSharedPreferences(context.applicationContext)
        sp.edit().putString(key, value).apply()
    }

    fun getString(context: Context, key: String, defaultValue: String?): String? {
        val sp = PreferenceManager
                .getDefaultSharedPreferences(context.applicationContext)
        return sp.getString(key, defaultValue)
    }
}
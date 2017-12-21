package com.shuhart.md5gallery

import android.os.Handler
import android.util.Log
import android.view.View
import com.shuhart.md5gallery.media.Photo
import com.shuhart.md5gallery.utils.PrefUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object RxEventQueue {

    @Volatile
    var handler: Handler? = null

    fun connect() {

    }


}
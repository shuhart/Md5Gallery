package com.shuhart.md5gallery

import com.shuhart.md5gallery.media.Photo

class LoadDeviceAlbumsResult(val resultCode: LoadDeviceAlbumsResultCode,
                             val devicePhotos: List<Photo> = listOf())

enum class LoadDeviceAlbumsResultCode {
    SUCCESS,
    NO_PHOTOS,
    NO_PERMISSION
}
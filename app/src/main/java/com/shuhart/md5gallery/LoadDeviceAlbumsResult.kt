package com.shuhart.md5gallery

import com.shuhart.md5gallery.media.DeviceAlbums

class LoadDeviceAlbumsResult(val resultCode: LoadDeviceAlbumsResultCode,
                             val deviceAlbums: DeviceAlbums = DeviceAlbums.EMPTY)

enum class LoadDeviceAlbumsResultCode {
    SUCCESS,
    NO_ALBUMS,
    NO_PERMISSION
}
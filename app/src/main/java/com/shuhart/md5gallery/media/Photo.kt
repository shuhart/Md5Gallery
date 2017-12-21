package com.shuhart.md5gallery.media

data class Photo(val bucketId: Int,
                 val imageId: Long,
                 val dateTaken: Long,
                 val path: String,
                 val size: Long,
                 val orientation: Int,
                 var md5: String = "",
                 var thumbPath: String = "",
                 val isProviderPath: Boolean) {
    companion object {
        val EMPTY = Photo(0, 0, 0, "", 0, 0, isProviderPath = false)
    }
}
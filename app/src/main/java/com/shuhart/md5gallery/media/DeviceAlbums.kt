package com.shuhart.md5gallery.media

data class DeviceAlbums(val albums: List<Album>,
                        val allMediaAlbum: Album,
                        val cameraAlbumId: Int) {
    companion object {
        val EMPTY = DeviceAlbums(listOf(), Album.EMPTY, -1)
    }
}
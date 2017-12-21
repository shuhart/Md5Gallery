package com.shuhart.md5gallery.media

data class Album(val bucketId: Int,
                 val bucketName: String,
                 val coverPhoto: Photo,
                 var photos: MutableList<Photo> = mutableListOf()) {

    companion object {
        val EMPTY = Album(0, "", Photo.EMPTY)
    }

    fun addPhoto(photo: Photo) {
        photos.add(photo)
    }
}
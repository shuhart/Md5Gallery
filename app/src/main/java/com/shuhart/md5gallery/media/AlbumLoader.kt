package com.shuhart.md5gallery.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.shuhart.md5gallery.utils.log
import com.shuhart.md5gallery.utils.using


class AlbumLoader {
    private val projectionPhotos = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.ORIENTATION)

    fun queryAlbums(context: Context): DeviceAlbums {
        if (Build.VERSION.SDK_INT >= 23 &&
                context.applicationContext.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException()
        }

        val mediaAlbumsSorted = mutableListOf<Album>()
        val mediaAlbums = mutableMapOf<Int, Album>()
        var allMediaAlbum: Album? = null
        var cameraFolder: String? = null
        try {
            cameraFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath + "/" + "Camera/"
        } catch (e: Exception) {
            log(e)
        }

        var mediaCameraAlbumId = -1

        val cursor = MediaStore.Images.Media.query(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projectionPhotos,
                null,
                null,
                MediaStore.Images.Media.DATE_TAKEN + " DESC")
        cursor.using {
            val imageIdColumn = getColumnIndex(MediaStore.Images.Media._ID)
            val bucketIdColumn = getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameColumn = getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dataColumn = getColumnIndex(MediaStore.Images.Media.DATA)
            val dateColumn = getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
            val sizeColumn = getColumnIndex(MediaStore.Images.Media.SIZE)
            val orientationColumn = getColumnIndex(MediaStore.Images.Media.ORIENTATION)

            while (moveToNext()) {
                val imageId = getLong(imageIdColumn)
                val bucketId = getInt(bucketIdColumn)
                val bucketName = getString(bucketNameColumn)
                val path = getString(dataColumn)
                val dateTaken = getLong(dateColumn)
                val size = getLong(sizeColumn)
                val orientation = getInt(orientationColumn)

                if (path == null || path.isEmpty()) {
                    continue
                }

                val photo = Photo(bucketId, imageId, dateTaken, path, size, orientation, isProviderPath = Uri.parse(path).scheme != null)

                getThumbnail(context, photo)

                if (allMediaAlbum == null) {
                    allMediaAlbum = Album(0, "All Media", photo).apply {
                        mediaAlbumsSorted.add(0, this)
                    }
                }
                allMediaAlbum?.addPhoto(photo)

                var album = mediaAlbums[bucketId]
                if (album == null) {
                    album = Album(bucketId, bucketName, photo)
                    mediaAlbums.put(bucketId, album)
                    if (mediaCameraAlbumId == -1 && cameraFolder != null && path.startsWith(cameraFolder)) {
                        mediaAlbumsSorted.add(0, album)
                        mediaCameraAlbumId = bucketId
                    } else {
                        mediaAlbumsSorted.add(album)
                    }
                }
                album.addPhoto(photo)
            }
        }

        allMediaAlbum?.let {
            it.photos = it.photos.sortedBy { it.dateTaken }.toMutableList()
        }

        return DeviceAlbums(mediaAlbumsSorted, allMediaAlbum ?: Album.EMPTY, mediaCameraAlbumId)
    }

    fun getThumbnail(context: Context, photo: Photo) {
        val cursor = MediaStore.Images.Thumbnails.queryMiniThumbnail(
                context.contentResolver,
                photo.imageId,
                MediaStore.Images.Thumbnails.MINI_KIND,
                null)
        cursor.using {
            if (count > 0) {
                cursor.moveToFirst()
                val uri = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA))
                photo.thumbPath = uri
            }
        }
    }
}
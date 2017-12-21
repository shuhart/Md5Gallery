package com.shuhart.md5gallery.media

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI
import com.shuhart.md5gallery.utils.using


class PhotosLoader {
    private val projectionPhotos = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.ORIENTATION)

    private val projectionThumbs = arrayOf(
            MediaStore.Images.Thumbnails.IMAGE_ID,
            MediaStore.Images.Thumbnails.DATA
    )

    @SuppressLint("Recycle")
    fun queryPhotos(context: Context): List<Photo> {
        if (Build.VERSION.SDK_INT >= 23 &&
                context.applicationContext.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException()
        }

        val photos = mutableListOf<Photo>()
        val photoByIds = mutableMapOf<Long, Photo>()

        val mediaCursor = MediaStore.Images.Media.query(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projectionPhotos,
                null,
                null,
                MediaStore.Images.Media.DATE_TAKEN + " DESC")

        mediaCursor.using {
            val imageIdColumn = getColumnIndex(MediaStore.Images.Media._ID)
            val dataColumn = getColumnIndex(MediaStore.Images.Media.DATA)
            val dateColumn = getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
            val sizeColumn = getColumnIndex(MediaStore.Images.Media.SIZE)
            val orientationColumn = getColumnIndex(MediaStore.Images.Media.ORIENTATION)

            while (moveToNext()) {
                val imageId = getLong(imageIdColumn)
                val path = getString(dataColumn)
                val dateTaken = getLong(dateColumn)
                val size = getLong(sizeColumn)
                val orientation = getInt(orientationColumn)

                if (path == null || path.isEmpty()) {
                    continue
                }

                val photo = Photo(imageId, dateTaken, path, size, orientation, isProviderPath = Uri.parse(path).scheme != null)
                photos.add(photo)
                photoByIds.put(photo.imageId, photo)
            }
        }

        val thumbCursor = context.contentResolver.query(
                EXTERNAL_CONTENT_URI,
                projectionThumbs,
                "KIND = ${MediaStore.Images.Thumbnails.MINI_KIND}",
                null,
                null)

        thumbCursor.using {
            val imageIdColumn = getColumnIndex(MediaStore.Images.Thumbnails.IMAGE_ID)
            val dataColumn = getColumnIndex(MediaStore.Images.Thumbnails.DATA)

            while (moveToNext()) {
                val imageId = getLong(imageIdColumn)
                val path = getString(dataColumn)
                val photo = photoByIds[imageId] ?: continue
                photo.thumbPath = path
            }
        }

        return photos
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
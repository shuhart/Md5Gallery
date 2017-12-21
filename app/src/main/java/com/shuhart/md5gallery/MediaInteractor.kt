package com.shuhart.md5gallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import com.shuhart.md5gallery.media.Photo
import com.shuhart.md5gallery.media.PhotosLoader
import com.shuhart.md5gallery.utils.PrefUtils
import com.shuhart.md5gallery.utils.log
import io.reactivex.Observable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.MessageDigest


class MediaInteractor {
    /* Maximum pixels size for created bitmap. */
    private val MAX_NUM_PIXELS_THUMBNAIL = 512 * 384
    private val UNCONSTRAINED = -1

    /**
     * Constant used to indicate the dimension of mini thumbnail.
     * @hide Only used by media framework and media provider internally.
     */
    private val TARGET_SIZE_MINI_THUMBNAIL = 320

    private val photosLoader = PhotosLoader()

    fun getDevicePhotos(context: Context): Observable<LoadDeviceAlbumsResult> {
        return Observable.defer {
            val photos = photosLoader.queryPhotos(context)
            val code = if (photos.isEmpty())
                LoadDeviceAlbumsResultCode.NO_PHOTOS else
                LoadDeviceAlbumsResultCode.SUCCESS
            Observable.just(LoadDeviceAlbumsResult(resultCode = code, devicePhotos = photos))
        }.onErrorResumeNext { _: Throwable ->
            Observable.just(LoadDeviceAlbumsResult(resultCode = LoadDeviceAlbumsResultCode.NO_PERMISSION))
        }
    }

    fun getBytes(context: Context, photo: Photo): ByteArray {
        if (!photo.isProviderPath) {
            return FileInputStream(photo.path).buffered().use {
                it.readBytes()
            }
        }
        return context.contentResolver.openInputStream(Uri.parse(photo.path)).buffered().use {
            it.readBytes()
        }
    }

    fun md5(byteArray: ByteArray): String {
        val md5 = MessageDigest.getInstance("md5")
        return BigInteger(1, md5.digest(byteArray)).toString(16)
    }

    fun findLocalThumbnails(context: Context, photo: Photo): Boolean {
        // first check our local thumbnails cache
        val thumbKey = "thumb://${photo.imageId}"
        val thumbPath = PrefUtils.getString(context, thumbKey, null)
        if (thumbPath != null) {
            log("A thumbnail was found in a local cache.")
            photo.thumbPath = thumbPath
            return true
        }
        return false
    }

    fun createThumbnailIfMissed(context: Context, photo: Photo, bytes: ByteArray) {
        if (findLocalThumbnails(context, photo)) {
            return
        }
        createThumbnailWithFramework(context, photo, bytes)
    }

    fun createThumbnailWithFramework(context: Context, photo: Photo, bytes: ByteArray) {
        // Try generate a thumbnail using framework.
        val bitmap = MediaStore.Images.Thumbnails.getThumbnail(
                context.contentResolver,
                photo.imageId,
                MediaStore.Images.Thumbnails.MINI_KIND,
                null)
        if (bitmap != null) {
            log("Thumbnail was generated in the MiniThumbFile by the framework.")
            ensureThumbnailPathInMedia(context, photo, bytes)
        } else {
            ensureThumbnailPathInMedia(context, photo, bytes)
        }
    }

    private fun ensureThumbnailPathInMedia(context: Context, photo: Photo, bytes: ByteArray) {
        photosLoader.getThumbnail(context, photo)
        if (photo.thumbPath.isEmpty()) {
            // Oh, crap.
            // Well, now we have to generate and store a thumbnail locally.
            log("Thumbnail was not found in the device MiniThumbFile or in the local cache. Generating a one...")
            generateAndStoreThumbnail(context, photo, bytes)
        }
    }

    private fun generateAndStoreThumbnail(context: Context, photo: Photo, bytes: ByteArray) {
        val options = BitmapFactory.Options()
        options.inSampleSize = 1
        options.inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        if (options.mCancel || options.outWidth == -1 || options.outHeight == -1) {
            return
        }

        val targetSize = TARGET_SIZE_MINI_THUMBNAIL
        val maxPixels = MAX_NUM_PIXELS_THUMBNAIL

        options.inSampleSize = computeSampleSize(
                options, targetSize, maxPixels)
        options.inJustDecodeBounds = false

        options.inDither = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        val thumb = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        log("Manually generated thumbnail size: ${thumb.width} x ${thumb.height}")
        val path = context.cacheDir.absolutePath + "/thumb_" + photo.imageId
        if (saveThumbnail(path, thumb)) {
            photo.thumbPath = Uri.fromFile(File(path)).toString()
        } else {
            // give up
        }
    }

    private fun saveThumbnail(path: String, thumb: Bitmap): Boolean {
        try {
            FileOutputStream(path).use {
                thumb.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }
            return true
        } catch (e: Throwable) {
            log("An exception occurred while saving a thumbnail to the local cache", e)
        }
        return false
    }

    /*
     * Compute the sample size as a function of minSideLength
     * and maxNumOfPixels.
     * minSideLength is used to specify that minimal width or height of a
     * bitmap.
     * maxNumOfPixels is used to specify the maximal size in pixels that is
     * tolerable in terms of memory usage.
     *
     * The function returns a sample size based on the constraints.
     * Both size and minSideLength can be passed in as IImage.UNCONSTRAINED,
     * which indicates no care of the corresponding constraint.
     * The functions prefers returning a sample size that
     * generates a smaller bitmap, unless minSideLength = IImage.UNCONSTRAINED.
     *
     * Also, the function rounds up the sample size to a power of 2 or multiple
     * of 8 because BitmapFactory only honors sample size this way.
     * For example, BitmapFactory downsamples an image by 2 even though the
     * request is 3. So we round up the sample size to avoid OOM.
     */
    private fun computeSampleSize(options: BitmapFactory.Options,
                                  minSideLength: Int, maxNumOfPixels: Int): Int {
        val initialSize = computeInitialSampleSize(options, minSideLength,
                maxNumOfPixels)

        var roundedSize: Int
        if (initialSize <= 8) {
            roundedSize = 1
            while (roundedSize < initialSize) {
                roundedSize = roundedSize shl 1
            }
        } else {
            roundedSize = (initialSize + 7) / 8 * 8
        }

        return roundedSize
    }

    private fun computeInitialSampleSize(options: BitmapFactory.Options,
                                         minSideLength: Int, maxNumOfPixels: Int): Int {
        val w = options.outWidth.toDouble()
        val h = options.outHeight.toDouble()

        val lowerBound = if (maxNumOfPixels == UNCONSTRAINED)
            1
        else
            Math.ceil(Math.sqrt(w * h / maxNumOfPixels)).toInt()
        val upperBound = if (minSideLength == UNCONSTRAINED)
            128
        else
            Math.min(Math.floor(w / minSideLength),
                    Math.floor(h / minSideLength)).toInt()

        if (upperBound < lowerBound) {
            // return the larger one when there is no overlapping zone.
            return lowerBound
        }

        return if (maxNumOfPixels == UNCONSTRAINED && minSideLength == UNCONSTRAINED) {
            1
        } else if (minSideLength == UNCONSTRAINED) {
            lowerBound
        } else {
            upperBound
        }
    }

}
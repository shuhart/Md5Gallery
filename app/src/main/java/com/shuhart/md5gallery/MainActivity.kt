package com.shuhart.md5gallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.View
import android.widget.ProgressBar
import com.shuhart.md5gallery.media.Photo
import com.shuhart.md5gallery.utils.PrefUtils
import com.squareup.picasso.Picasso
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class MainActivity : AppCompatActivity() {
    private val interactor = MediaInteractor()

    private val requestCodePermissions = 1000

    private lateinit var adapter: PhotosAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var empty: View
    private lateinit var progressBar: ProgressBar
    private lateinit var permissionView: View
    private lateinit var picasso: Picasso
    private lateinit var horizontalProgressBar: ProgressBar

    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        picasso = Picasso.Builder(this).loggingEnabled(!BuildConfig.DEBUG).build()
        setContentView(R.layout.activity_main)
        recyclerView = findViewById(R.id.recycler_view)
        empty = findViewById(R.id.empty)
        progressBar = findViewById(R.id.progress_bar)
        permissionView = findViewById(R.id.no_permission)
        horizontalProgressBar = findViewById(R.id.progress_bar_horizontal)
        findViewById<View>(R.id.btn_grant).setOnClickListener {
            requestExternalStoragePermissions()
        }
        configureRecyclerView()
        loadPhotos(savedInstanceState)
    }

    private fun configureRecyclerView() {
        val spansCount = getSpansCount()
        val itemWidth = getItemWidth(spansCount)
        val itemHeight = itemWidth / 3 * 4
        adapter = PhotosAdapter(itemWidth, itemHeight, picasso)
        recyclerView.layoutManager = GridLayoutManager(this, spansCount)
        if (recyclerView.adapter != null) {
            recyclerView.swapAdapter(adapter, false)
        } else {
            recyclerView.adapter = adapter
        }
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState != SCROLL_STATE_IDLE) {
                    picasso.pauseTag(this)
                } else {
                    picasso.resumeTag(this)
                }
            }
        })
    }

    private fun getItemWidth(spanCount: Int): Int {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels
        return width / spanCount
    }

    private fun getSpansCount(): Int {
        val rotation = windowManager.defaultDisplay.rotation
        return if (isTablet()) {
            if ((rotation == Surface.ROTATION_270 || rotation == Surface.ROTATION_90)) 4 else 3
        } else if ((rotation == Surface.ROTATION_270 || rotation == Surface.ROTATION_90)) 3 else 2
    }

    private fun isTablet(): Boolean = resources.getBoolean(R.bool.isTablet)

    private fun loadPhotos(savedInstanceState: Bundle? = null) {
        changeState(MainActivity.ViewState.PROGRESS)
        disposable = interactor.getDeviceAlbums(this)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    if (it.resultCode == LoadDeviceAlbumsResultCode.NO_ALBUMS) {
                        changeState(MainActivity.ViewState.EMPTY)
                    } else if (it.resultCode == LoadDeviceAlbumsResultCode.NO_PERMISSION) {
                        requestExternalStoragePermissions()
                    }
                }
                .filter { it.deviceAlbums.albums.isNotEmpty() }
                .doOnNext {
                    horizontalProgressBar.max = it.deviceAlbums.allMediaAlbum.photos.size
                }
                .flatMap { Observable.fromIterable(it.deviceAlbums.allMediaAlbum.photos) }
                .observeOn(Schedulers.io())
                .flatMap { updateCache(it) }
                .doOnError {
                    Log.e(javaClass.simpleName, it.message, it)
                }
                // skip the photo if md5 was not generated for some reason
                .onErrorResumeNext(Observable.just(Photo.EMPTY))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    horizontalProgressBar.progress = horizontalProgressBar.progress + 1
                    if (horizontalProgressBar.progress == horizontalProgressBar.max) {
                        horizontalProgressBar.visibility = View.GONE
                    }
                    if (it != Photo.EMPTY) {
                        if (recyclerView.visibility != View.VISIBLE) {
                            changeState(MainActivity.ViewState.IMAGES)
                        }
                        adapter.addPhoto(it)
                    }
                })
    }

    private fun updateCache(photo: Photo): Observable<Photo> {
        return Observable.fromCallable {
            val cached = PrefUtils.getString(this, photo.imageId.toString(), null)
            if (cached != null) {
                photo.md5 = cached
                if (photo.thumbPath.isEmpty()) {
                    if (!interactor.findLocalThumbnails(this, photo)) {
                        val bytes = interactor.getBytes(this, photo)
                        interactor.createThumbnailWithFramework(this, photo, bytes)
                    }
                }
            } else {
                val bytes = interactor.getBytes(this, photo)
                if (photo.thumbPath.isEmpty()) {
                    generateThumbnailIfNeeded(photo, bytes)
                }
                photo.md5 = interactor.md5(bytes)
                PrefUtils.putString(this, photo.imageId.toString(), photo.md5)
            }
            photo
        }
    }

    private fun generateThumbnailIfNeeded(photo: Photo, bytes: ByteArray) {
        try {
            interactor.createThumbnailIfMissed(this, photo, bytes)
        } catch (e: Throwable) {
            Log.e(javaClass.simpleName, "Failed to generate a thumbnail", e)
        }
    }

    private fun changeState(state: ViewState) {
        when (state) {
            ViewState.EMPTY -> {
                empty.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.GONE
                permissionView.visibility = View.GONE
            }
            ViewState.PROGRESS -> {
                empty.visibility = View.GONE
                progressBar.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                permissionView.visibility = View.GONE
            }
            ViewState.NO_PERMISSION -> {
                empty.visibility = View.GONE
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.GONE
                permissionView.visibility = View.VISIBLE
            }
            ViewState.IMAGES -> {
                empty.visibility = View.GONE
                progressBar.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                permissionView.visibility = View.GONE
            }
        }
    }

    private fun requestExternalStoragePermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), requestCodePermissions)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (grantResults.isEmpty()) {
            return
        }
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadPhotos()
        } else {
            changeState(ViewState.NO_PERMISSION)
        }
    }

    private enum class ViewState {
        EMPTY,
        PROGRESS,
        NO_PERMISSION,
        IMAGES
    }
}

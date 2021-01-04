package uk.ac.warwick.postroom

import android.app.Application
import android.net.http.HttpResponseCache
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import uk.ac.warwick.postroom.activities.TAG
import java.io.File
import java.io.IOException

private const val HttpCacheSize: Long = 10_485_760L // 10 MiB

@HiltAndroidApp
class PostroomApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            val httpCacheDir = File(this.cacheDir, "http")
            HttpResponseCache.install(httpCacheDir, HttpCacheSize)
        } catch (e: IOException) {
            Log.i(TAG, "HTTP response cache installation failed:$e")
        }
    }

    override fun onTerminate() {
        val cache = HttpResponseCache.getInstalled()
        cache?.flush()
        super.onTerminate()
    }
}
package com.example.np_nilson.data.report

import android.graphics.Bitmap
import android.util.LruCache
import com.example.np_nilson.data.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Downloads report images (which require the session token) and keeps
 * recently shown ones in memory so lists don't re-download on every scroll.
 */
object ReportImageRepository {

    // Cache size in kilobytes: 1/8 of the app's available memory
    private val cache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun getCached(url: String, maxDimension: Int): Bitmap? = cache.get(cacheKey(url, maxDimension))

    suspend fun loadImage(url: String, token: String, maxDimension: Int): Bitmap? {
        getCached(url, maxDimension)?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                // Relative path so it resolves against the configured base URL
                val response = ApiClient.getReportsApi().getReportImage("Bearer $token", url.removePrefix("/"))
                val bytes = response.body()?.use { it.bytes() }
                if (!response.isSuccessful || bytes == null) return@withContext null

                ReportImageLoader.decodeBytes(bytes, maxDimension)?.also {
                    cache.put(cacheKey(url, maxDimension), it)
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun clear() {
        cache.evictAll()
    }

    private fun cacheKey(url: String, maxDimension: Int) = "$url@$maxDimension"
}

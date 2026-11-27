package com.padmakar.imageloader.data.cache

import android.graphics.Bitmap
import android.util.LruCache
import com.padmakar.imageloader.data.boundary.ImageCacheBoundary

/**
 * In-memory LRU cache storing decoded bitmaps like Glide / Coil.
 * Dynamically sized to a configurable fraction of available JVM heap (default 25%) to prevent OutOfMemory errors.
 */
class LruMemoryCache(
    maxSizeBytes: Int = defaultMemoryCacheSize()
) : ImageCacheBoundary {

    companion object {
        fun defaultMemoryCacheSize(): Int {
            val maxMemory = Runtime.getRuntime().maxMemory()
            // Allocate 25% of available JVM heap to bitmap cache
            return (maxMemory / 4).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        }
    }

    private val cache = object : LruCache<String, Any>(maxSizeBytes) {
        override fun sizeOf(key: String, value: Any): Int {
            return if (value is Bitmap) {
                value.byteCount
            } else {
                1
            }
        }
    }

    override suspend fun get(key: String): Any? {
        return synchronized(cache) {
            cache.get(key)
        }
    }

    override suspend fun put(key: String, value: Any) {
        synchronized(cache) {
            cache.put(key, value)
        }
    }

    override suspend fun remove(key: String): Boolean {
        return synchronized(cache) {
            cache.remove(key) != null
        }
    }

    override suspend fun clear() {
        synchronized(cache) {
            cache.evictAll()
        }
    }
}

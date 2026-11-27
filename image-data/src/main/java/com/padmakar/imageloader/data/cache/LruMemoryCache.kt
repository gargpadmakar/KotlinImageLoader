package com.padmakar.imageloader.data.cache

import android.graphics.Bitmap
import com.padmakar.imageloader.core.cache.CacheStats
import com.padmakar.imageloader.data.boundary.ImageCacheBoundary
import java.util.concurrent.atomic.AtomicLong

/**
 * In-memory LRU cache storing decoded bitmaps like Glide / Coil.
 * Dynamically sized to a configurable fraction of available JVM heap (default 25%) to prevent OutOfMemory errors.
 * Thread-safe implementation using LinkedHashMap with access-order eviction.
 */
class LruMemoryCache(
    private val maxSizeBytes: Int = defaultMemoryCacheSize()
) : ImageCacheBoundary {

    companion object {
        fun defaultMemoryCacheSize(): Int {
            val maxMemory = Runtime.getRuntime().maxMemory()
            // Allocate 25% of available JVM heap to bitmap cache
            return (maxMemory / 4).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        }
    }

    private val hitCount = AtomicLong(0)
    private val missCount = AtomicLong(0)
    private val putCount = AtomicLong(0)
    private val evictionCount = AtomicLong(0)

    private val lock = Any()
    private var currentSize = 0L

    // access-order LinkedHashMap
    private val map = LinkedHashMap<String, Any>(16, 0.75f, true)

    private fun sizeOf(value: Any): Int {
        return if (value is Bitmap) {
            value.byteCount.coerceAtLeast(1)
        } else {
            1
        }
    }

    override suspend fun get(key: String): Any? = synchronized(lock) {
        val value = map[key]
        if (value != null) {
            hitCount.incrementAndGet()
        } else {
            missCount.incrementAndGet()
        }
        value
    }

    override suspend fun put(key: String, value: Any): Unit = synchronized(lock) {
        putCount.incrementAndGet()
        val old = map.put(key, value)
        if (old != null) {
            currentSize -= sizeOf(old)
        }
        currentSize += sizeOf(value)

        // Evict if over budget
        val iterator = map.entries.iterator()
        while (currentSize > maxSizeBytes && iterator.hasNext()) {
            val entry = iterator.next()
            iterator.remove()
            currentSize -= sizeOf(entry.value)
            evictionCount.incrementAndGet()
        }
    }

    override suspend fun remove(key: String): Boolean = synchronized(lock) {
        val removed = map.remove(key)
        if (removed != null) {
            currentSize -= sizeOf(removed)
            true
        } else false
    }

    override suspend fun clear(): Unit = synchronized(lock) {
        map.clear()
        currentSize = 0L
    }

    fun stats(): CacheStats = synchronized(lock) {
        CacheStats(
            hitCount = hitCount.get(),
            missCount = missCount.get(),
            putCount = putCount.get(),
            evictionCount = evictionCount.get(),
            currentSizeBytes = currentSize,
            maxSizeBytes = maxSizeBytes.toLong()
        )
    }
}

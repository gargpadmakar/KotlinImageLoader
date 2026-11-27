package com.padmakar.imageloader.data.cache

import android.content.Context
import java.io.File

/**
 * Global singleton access for internal app disk cache.
 */
object DefaultDiskCacheInstance {
    @Volatile
    private var instanceInternal: DiskLruCache? = null

    fun get(context: Context): DiskLruCache {
        return instanceInternal ?: synchronized(this) {
            instanceInternal ?: run {
                val cacheDir = File(context.applicationContext.cacheDir, "image_cache")
                DiskLruCache(directory = cacheDir).also { instanceInternal = it }
            }
        }
    }

    fun setCustom(diskCache: DiskLruCache) {
        synchronized(this) {
            instanceInternal = diskCache
        }
    }
}

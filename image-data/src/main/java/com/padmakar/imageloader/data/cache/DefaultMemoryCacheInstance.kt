package com.padmakar.imageloader.data.cache

import com.padmakar.imageloader.data.boundary.ImageCacheBoundary

/**
 * Global shared memory cache instance ensuring loaded images persist across activities,
 * navigation intents, and rotation like Glide.
 */
object DefaultMemoryCacheInstance {
    val instance: ImageCacheBoundary by lazy {
        LruMemoryCache()
    }
}

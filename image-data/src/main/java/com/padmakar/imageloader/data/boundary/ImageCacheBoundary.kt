package com.padmakar.imageloader.data.boundary

/**
 * Architectural boundary for caching strategies (memory and disk cache).
 * Actual caching implementation is implemented in subsequent phases.
 */
interface ImageCacheBoundary {
    suspend fun get(key: String): Any?
    suspend fun put(key: String, value: Any)
    suspend fun remove(key: String): Boolean
    suspend fun clear()
}

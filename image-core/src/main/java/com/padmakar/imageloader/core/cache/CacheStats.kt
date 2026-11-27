package com.padmakar.imageloader.core.cache

/**
 * Metrics and statistics for cache monitoring and performance profiling.
 */
data class CacheStats(
    val hitCount: Long,
    val missCount: Long,
    val putCount: Long,
    val evictionCount: Long,
    val currentSizeBytes: Long,
    val maxSizeBytes: Long
) {
    val hitRate: Float
        get() {
            val total = hitCount + missCount
            return if (total == 0L) 0f else hitCount.toFloat() / total
        }
}

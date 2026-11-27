package com.padmakar.imageloader.core.model

/**
 * Defines the caching behavior for memory and disk caches.
 */
enum class CachePolicy {
    /** Both read and write operations are enabled. */
    ENABLED,

    /** Only read operations from cache are allowed; new items will not be written. */
    READ_ONLY,

    /** Only write operations to cache are allowed; cache entries will not be read. */
    WRITE_ONLY,

    /** Caching is completely bypassed (no read, no write). */
    DISABLED;

    val isReadAllowed: Boolean
        get() = this == ENABLED || this == READ_ONLY

    val isWriteAllowed: Boolean
        get() = this == ENABLED || this == WRITE_ONLY
}

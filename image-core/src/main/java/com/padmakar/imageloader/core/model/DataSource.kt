package com.padmakar.imageloader.core.model

/**
 * Represents the origin from where an image was loaded.
 */
enum class DataSource {
    /** Resolved from in-memory cache. */
    MEMORY,

    /** Resolved from local persistent disk cache or file system. */
    DISK,

    /** Downloaded over the network. */
    NETWORK
}

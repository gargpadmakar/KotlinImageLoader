package com.padmakar.imageloader.core.model

/**
 * Immutable configuration for an image loading request.
 *
 * @param data The data source to load (e.g., URL String, Uri, File, resource ID).
 * @param size Target dimensions for image decoding/sampling.
 * @param memoryCachePolicy Policy for reading and writing to in-memory cache.
 * @param diskCachePolicy Policy for reading and writing to disk cache.
 * @param placeholderResId Optional placeholder drawable resource ID.
 * @param errorResId Optional error drawable resource ID.
 * @param headers Optional HTTP headers for network requests.
 */
data class ImageRequest(
    val data: Any?,
    val size: ImageSize = ImageSize.Unspecified,
    val memoryCachePolicy: CachePolicy = CachePolicy.ENABLED,
    val diskCachePolicy: CachePolicy = CachePolicy.ENABLED,
    val placeholderResId: Int? = null,
    val errorResId: Int? = null,
    val headers: Map<String, String> = emptyMap()
) {
    /**
     * Builder for constructing [ImageRequest] instances fluently.
     */
    class Builder(private var data: Any? = null) {
        private var size: ImageSize = ImageSize.Unspecified
        private var memoryCachePolicy: CachePolicy = CachePolicy.ENABLED
        private var diskCachePolicy: CachePolicy = CachePolicy.ENABLED
        private var placeholderResId: Int? = null
        private var errorResId: Int? = null
        private var headers: MutableMap<String, String> = mutableMapOf()

        fun data(data: Any?) = apply { this.data = data }
        fun size(size: ImageSize) = apply { this.size = size }
        fun size(width: Int, height: Int) = apply { this.size = ImageSize.Exact(width, height) }
        fun memoryCachePolicy(policy: CachePolicy) = apply { this.memoryCachePolicy = policy }
        fun diskCachePolicy(policy: CachePolicy) = apply { this.diskCachePolicy = policy }
        fun placeholder(resId: Int?) = apply { this.placeholderResId = resId }
        fun error(resId: Int?) = apply { this.errorResId = resId }
        fun addHeader(key: String, value: String) = apply { this.headers[key] = value }
        fun headers(headers: Map<String, String>) = apply {
            this.headers.clear()
            this.headers.putAll(headers)
        }

        fun build(): ImageRequest = ImageRequest(
            data = data,
            size = size,
            memoryCachePolicy = memoryCachePolicy,
            diskCachePolicy = diskCachePolicy,
            placeholderResId = placeholderResId,
            errorResId = errorResId,
            headers = headers.toMap()
        )
    }
}

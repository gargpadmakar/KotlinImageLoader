package com.padmakar.imageloader.core.model

import com.padmakar.imageloader.core.transform.Transformation

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
 * @param progressive Whether intermediate preview frames should be streamed during fetch.
 * @param transformations List of [Transformation]s applied sequentially after decoding.
 * @param priority Request priority scheduling ([Priority.NORMAL] default).
 * @param crossfade Whether to smoothly crossfade transition to the loaded image.
 * @param crossfadeDurationMillis Duration of the crossfade transition in milliseconds.
 * @param retryAttempts Number of retry attempts for network failures before returning error.
 * @param retryDelayMillis Backoff delay between retry attempts in milliseconds.
 * @param thumbnail Optional lower-resolution thumbnail data loaded before the main image.
 */
data class ImageRequest(
    val data: Any?,
    val size: ImageSize = ImageSize.Unspecified,
    val memoryCachePolicy: CachePolicy = CachePolicy.ENABLED,
    val diskCachePolicy: CachePolicy = CachePolicy.ENABLED,
    val placeholderResId: Int? = null,
    val errorResId: Int? = null,
    val headers: Map<String, String> = emptyMap(),
    val progressive: Boolean = true,
    val transformations: List<Transformation> = emptyList(),
    val priority: Priority = Priority.NORMAL,
    val crossfade: Boolean = false,
    val crossfadeDurationMillis: Int = 300,
    val retryAttempts: Int = 2,
    val retryDelayMillis: Long = 300L,
    val thumbnail: Any? = null
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
        private var progressive: Boolean = true
        private var transformations: MutableList<Transformation> = mutableListOf()
        private var priority: Priority = Priority.NORMAL
        private var crossfade: Boolean = false
        private var crossfadeDurationMillis: Int = 300
        private var retryAttempts: Int = 2
        private var retryDelayMillis: Long = 300L
        private var thumbnail: Any? = null

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
        fun progressive(enabled: Boolean) = apply { this.progressive = enabled }
        fun transform(transformation: Transformation) = apply {
            this.transformations.add(transformation)
        }
        fun transformations(transformations: List<Transformation>) = apply {
            this.transformations.clear()
            this.transformations.addAll(transformations)
        }
        fun priority(priority: Priority) = apply { this.priority = priority }
        fun crossfade(enabled: Boolean, durationMillis: Int = 300) = apply {
            this.crossfade = enabled
            this.crossfadeDurationMillis = durationMillis
        }
        fun retry(attempts: Int, delayMillis: Long = 300L) = apply {
            this.retryAttempts = attempts
            this.retryDelayMillis = delayMillis
        }
        fun thumbnail(thumbnail: Any?) = apply { this.thumbnail = thumbnail }

        fun build(): ImageRequest = ImageRequest(
            data = data,
            size = size,
            memoryCachePolicy = memoryCachePolicy,
            diskCachePolicy = diskCachePolicy,
            placeholderResId = placeholderResId,
            errorResId = errorResId,
            headers = headers.toMap(),
            progressive = progressive,
            transformations = transformations.toList(),
            priority = priority,
            crossfade = crossfade,
            crossfadeDurationMillis = crossfadeDurationMillis,
            retryAttempts = retryAttempts,
            retryDelayMillis = retryDelayMillis,
            thumbnail = thumbnail
        )
    }
}

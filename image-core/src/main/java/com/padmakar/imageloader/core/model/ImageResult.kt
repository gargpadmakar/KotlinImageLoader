package com.padmakar.imageloader.core.model

/**
 * Result representing the outcome of executing an [ImageRequest].
 */
sealed interface ImageResult {
    /** The original request associated with this result. */
    val request: ImageRequest

    /**
     * Indicates successful resolution and decoding of the image.
     *
     * @param image The loaded image entity (e.g., Bitmap, Drawable, or decoded payload).
     * @param dataSource Origin where the image was fetched from.
     * @param request Original request.
     */
    data class Success(
        val image: Any,
        val dataSource: DataSource,
        override val request: ImageRequest
    ) : ImageResult

    /**
     * Indicates failure during image fetching or decoding.
     *
     * @param throwable Root cause of the failure.
     * @param request Original request.
     */
    data class Error(
        val throwable: Throwable,
        override val request: ImageRequest
    ) : ImageResult
}

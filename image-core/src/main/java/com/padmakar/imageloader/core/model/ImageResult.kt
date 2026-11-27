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
     * Indicates progressive intermediate frame (partial download / low-resolution preview).
     *
     * @param image The intermediate decoded image bitmap.
     * @param progress Progress percentage in range [0.0, 1.0], or -1.0 if unknown.
     * @param request Original request.
     */
    data class Progressive(
        val image: Any,
        val progress: Float,
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

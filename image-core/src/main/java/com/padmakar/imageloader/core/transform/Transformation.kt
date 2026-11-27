package com.padmakar.imageloader.core.transform

import android.graphics.Bitmap
import com.padmakar.imageloader.core.model.ImageSize

/**
 * An immutable transformation applied to an image [Bitmap].
 */
interface Transformation {
    /**
     * Unique cache key representing this transformation and its parameters.
     * Guaranteed to be consistent for cache key generation.
     */
    val cacheKey: String

    /**
     * Transforms the [input] bitmap into an output bitmap.
     *
     * @param input The input bitmap to transform.
     * @param size The requested target dimensions.
     * @return The transformed bitmap. If the bitmap was transformed, a new bitmap should be returned.
     */
    suspend fun transform(input: Bitmap, size: ImageSize): Bitmap
}

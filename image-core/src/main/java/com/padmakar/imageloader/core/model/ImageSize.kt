package com.padmakar.imageloader.core.model

/**
 * Represents the target dimensions for resizing or sampling an image.
 */
sealed interface ImageSize {
    /** Explicitly maintain original intrinsic image dimensions. */
    data object Original : ImageSize

    /** Target size is not specified explicitly; UI target will measure its dimensions. */
    data object Unspecified : ImageSize

    /** Specific target width and height in pixels. */
    data class Exact(val width: Int, val height: Int) : ImageSize {
        init {
            require(width > 0) { "Width must be positive, was $width" }
            require(height > 0) { "Height must be positive, was $height" }
        }
    }
}

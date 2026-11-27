package com.padmakar.imageloader.core.model

/**
 * Represents the target dimensions for resizing or sampling an image.
 */
sealed interface ImageSize {
    /** Maintain original intrinsic image dimensions. */
    data object Original : ImageSize

    /** Specific target width and height in pixels. */
    data class Exact(val width: Int, val height: Int) : ImageSize {
        init {
            require(width > 0) { "Width must be positive, was $width" }
            require(height > 0) { "Height must be positive, was $height" }
        }
    }
}

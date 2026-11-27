package com.padmakar.imageloader.mvi

import com.padmakar.imageloader.core.model.DataSource

/**
 * Single-shot side-effects or one-off events produced by [ImageStore].
 */
sealed interface ImageEffect {
    /** Emitted when an error is encountered. */
    data class ShowError(val message: String, val cause: Throwable? = null) : ImageEffect

    /** Emitted when image is loaded successfully. */
    data class SuccessEffect(val dataSource: DataSource) : ImageEffect
}

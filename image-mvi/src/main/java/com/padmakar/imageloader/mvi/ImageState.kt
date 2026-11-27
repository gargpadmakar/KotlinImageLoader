package com.padmakar.imageloader.mvi

import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageResult

/**
 * Immutable state representing the UI rendering state of an image.
 */
sealed interface ImageState {
    /** Initial or idle state prior to loading or following cancellation. */
    data object Idle : ImageState

    /** Image is currently being fetched or decoded. */
    data class Loading(
        val request: ImageRequest,
        val progress: Float? = null
    ) : ImageState

    /** Image was successfully retrieved and is ready for display. */
    data class Success(val result: ImageResult.Success) : ImageState

    /** Image load failed. */
    data class Error(val error: ImageResult.Error) : ImageState
}

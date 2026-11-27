package com.padmakar.imageloader.mvi

import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageResult

/**
 * Internal actions dispatched to the [ImageReducer] to alter [ImageState].
 */
sealed interface ImageAction {
    /** Signifies start of image loading. */
    data class StartLoading(val request: ImageRequest) : ImageAction

    /** Signifies progress update for download or decoding (0.0 to 1.0). */
    data class LoadingProgress(val progress: Float) : ImageAction

    /** Signifies successful loading and decoding of the image. */
    data class LoadingSuccess(val result: ImageResult.Success) : ImageAction

    /** Signifies an error occurred during image loading. */
    data class LoadingFailure(val error: ImageResult.Error) : ImageAction

    /** Resets the state back to [ImageState.Idle]. */
    data object Reset : ImageAction
}

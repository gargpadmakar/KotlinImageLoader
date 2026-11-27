package com.padmakar.imageloader.compose

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import com.padmakar.imageloader.core.model.ImageResult

/**
 * State representing asynchronous image loading in Jetpack Compose.
 */
@Immutable
sealed interface AsyncImageState {
    /** Request is currently in flight or waiting for dimensions. */
    data object Loading : AsyncImageState

    /** Progressive intermediate low-resolution preview / partial frame. */
    data class Progressive(
        val bitmap: ImageBitmap,
        val progress: Float
    ) : AsyncImageState

    /** Image loaded and decoded successfully. */
    data class Success(
        val bitmap: ImageBitmap,
        val result: ImageResult.Success
    ) : AsyncImageState

    /** Image loading failed. */
    data class Error(
        val result: ImageResult.Error
    ) : AsyncImageState
}

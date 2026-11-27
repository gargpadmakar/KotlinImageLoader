package com.padmakar.imageloader.compose

import androidx.compose.runtime.Immutable
import com.padmakar.imageloader.mvi.ImageState

/**
 * State holder for Compose AsyncImage rendering.
 */
@Immutable
data class AsyncImageState(
    val imageState: ImageState = ImageState.Idle
) {
    val isLoading: Boolean get() = imageState is ImageState.Loading
    val isSuccess: Boolean get() = imageState is ImageState.Success
    val isError: Boolean get() = imageState is ImageState.Error
}

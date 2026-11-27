package com.padmakar.imageloader.mvi

/**
 * Pure function contract for state reductions.
 */
interface ImageReducer {
    /**
     * Produces a new [ImageState] deterministically based on [currentState] and [action].
     */
    fun reduce(currentState: ImageState, action: ImageAction): ImageState
}

/**
 * Predictable, side-effect-free implementation of [ImageReducer].
 */
class DefaultImageReducer : ImageReducer {
    override fun reduce(currentState: ImageState, action: ImageAction): ImageState {
        return when (action) {
            is ImageAction.StartLoading -> {
                ImageState.Loading(request = action.request, progress = null)
            }
            is ImageAction.LoadingProgress -> {
                if (currentState is ImageState.Loading) {
                    currentState.copy(progress = action.progress)
                } else {
                    currentState
                }
            }
            is ImageAction.LoadingSuccess -> {
                ImageState.Success(result = action.result)
            }
            is ImageAction.LoadingFailure -> {
                ImageState.Error(error = action.error)
            }
            is ImageAction.Reset -> {
                ImageState.Idle
            }
        }
    }
}

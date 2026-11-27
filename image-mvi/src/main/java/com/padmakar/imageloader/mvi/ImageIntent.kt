package com.padmakar.imageloader.mvi

import com.padmakar.imageloader.core.model.ImageRequest

/**
 * User or UI intents dispatched to [ImageStore].
 */
sealed interface ImageIntent {
    /**
     * Dispatched to initiate loading an image.
     */
    data class Load(val request: ImageRequest) : ImageIntent

    /**
     * Dispatched to retry a previously failed request.
     */
    data class Retry(val request: ImageRequest) : ImageIntent

    /**
     * Dispatched to cancel ongoing image loading.
     */
    data object Cancel : ImageIntent
}

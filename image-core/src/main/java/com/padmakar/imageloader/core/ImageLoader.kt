package com.padmakar.imageloader.core

import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageResult

/**
 * Public API contract for the image loading engine.
 */
interface ImageLoader {
    /**
     * Enqueues an [ImageRequest] asynchronously.
     *
     * @param request The request configuration.
     * @return A [Disposable] handle allowing cancellation of the request.
     */
    fun enqueue(request: ImageRequest): Disposable

    /**
     * Executes an [ImageRequest] suspending the caller until completion.
     *
     * @param request The request configuration.
     * @return An [ImageResult] representing either success or error.
     */
    suspend fun execute(request: ImageRequest): ImageResult
}

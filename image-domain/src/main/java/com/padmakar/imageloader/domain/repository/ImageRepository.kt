package com.padmakar.imageloader.domain.repository

import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageResult

/**
 * Domain-level contract for image retrieval operations.
 */
interface ImageRepository {
    /**
     * Executes the process of loading and decoding an image according to [request].
     *
     * @param request The image loading specification.
     * @return [ImageResult] containing either decoded image or failure details.
     */
    suspend fun loadImage(request: ImageRequest): ImageResult

    /**
     * Streams progressive intermediate previews during download and final result.
     */
    fun streamImage(request: ImageRequest): kotlinx.coroutines.flow.Flow<ImageResult> =
        kotlinx.coroutines.flow.flow {
            emit(loadImage(request))
        }
}

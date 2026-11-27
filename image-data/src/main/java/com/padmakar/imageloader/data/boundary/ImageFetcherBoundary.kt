package com.padmakar.imageloader.data.boundary

import com.padmakar.imageloader.core.model.ImageRequest

/**
 * Architectural boundary for fetching raw image data from a data source
 * (network, disk, content provider, resource, etc.).
 * Actual fetcher engines are implemented in subsequent phases.
 */
interface ImageFetcherBoundary {
    suspend fun fetch(request: ImageRequest): FetchResult

    /**
     * Optional streaming fetch with progress and partial bytes emission.
     */
    suspend fun fetchStreaming(
        request: ImageRequest,
        onProgress: suspend (bytes: ByteArray, progress: Float) -> Unit
    ): FetchResult = fetch(request)
}

package com.padmakar.imageloader.data

import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageResult
import com.padmakar.imageloader.data.boundary.FetchResult
import com.padmakar.imageloader.data.boundary.ImageDecoderBoundary
import com.padmakar.imageloader.data.boundary.ImageFetcherBoundary
import com.padmakar.imageloader.domain.repository.ImageRepository

/**
 * Data layer implementation of [ImageRepository].
 *
 * Coordinates data flow across [ImageFetcherBoundary] and [ImageDecoderBoundary].
 * Concrete network fetching, disk caching, and bitmap decoding algorithms will be plugged
 * into these boundaries in Phase 2.
 */
class DefaultImageRepository(
    private val fetcherBoundary: ImageFetcherBoundary? = null,
    private val decoderBoundary: ImageDecoderBoundary? = null
) : ImageRepository {

    override suspend fun loadImage(request: ImageRequest): ImageResult {
        val fetcher = fetcherBoundary
        val decoder = decoderBoundary
        if (fetcher == null || decoder == null) {
            return ImageResult.Error(
                throwable = UnsupportedOperationException(
                    "Image engine boundaries are not configured in Phase 1 foundation."
                ),
                request = request
            )
        }

        return when (val fetchResult = fetcher.fetch(request)) {
            is FetchResult.Success -> {
                try {
                    val decoded = decoder.decode(fetchResult.data, request)
                    ImageResult.Success(
                        image = decoded,
                        dataSource = fetchResult.dataSource,
                        request = request
                    )
                } catch (t: Throwable) {
                    ImageResult.Error(throwable = t, request = request)
                }
            }
            is FetchResult.Error -> {
                ImageResult.Error(throwable = fetchResult.throwable, request = request)
            }
        }
    }
}

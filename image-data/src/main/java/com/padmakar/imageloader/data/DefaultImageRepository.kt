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
    private val fetcherBoundary: ImageFetcherBoundary = com.padmakar.imageloader.data.fetcher.HttpUrlConnectionFetcher(),
    private val decoderBoundary: ImageDecoderBoundary = com.padmakar.imageloader.data.decoder.BitmapDecoder()
) : ImageRepository {

    override suspend fun loadImage(request: ImageRequest): ImageResult {
        return when (val fetchResult = fetcherBoundary.fetch(request)) {
            is FetchResult.Success -> {
                try {
                    val decoded = decoderBoundary.decode(fetchResult.data, request)
                    ImageResult.Success(
                        image = decoded,
                        dataSource = fetchResult.dataSource,
                        request = request
                    )
                } catch (ce: kotlinx.coroutines.CancellationException) {
                    throw ce
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

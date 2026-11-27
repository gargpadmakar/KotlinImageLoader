package com.padmakar.imageloader.data

import com.padmakar.imageloader.core.model.DataSource
import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageResult
import com.padmakar.imageloader.data.boundary.FetchResult
import com.padmakar.imageloader.data.boundary.ImageCacheBoundary
import com.padmakar.imageloader.data.boundary.ImageDecoderBoundary
import com.padmakar.imageloader.data.boundary.ImageFetcherBoundary
import com.padmakar.imageloader.data.cache.DefaultMemoryCacheInstance
import com.padmakar.imageloader.domain.repository.ImageRepository

/**
 * Data layer implementation of [ImageRepository].
 *
 * Coordinates data flow across [ImageCacheBoundary], [ImageFetcherBoundary], and [ImageDecoderBoundary].
 * Follows Glide-style caching:
 * 1. Checks in-memory cache first if [ImageRequest.memoryCachePolicy] allows reads.
 * 2. On cache hit, immediately returns [DataSource.MEMORY] without touching network or decoding.
 * 3. On cache miss, fetches raw bytes, decodes, saves to memory cache, and returns [DataSource.NETWORK].
 */
class DefaultImageRepository(
    private val fetcherBoundary: ImageFetcherBoundary = com.padmakar.imageloader.data.fetcher.HttpUrlConnectionFetcher(),
    private val decoderBoundary: ImageDecoderBoundary = com.padmakar.imageloader.data.decoder.BitmapDecoder(),
    private val memoryCache: ImageCacheBoundary? = DefaultMemoryCacheInstance.instance
) : ImageRepository {

    override suspend fun loadImage(request: ImageRequest): ImageResult {
        val cacheKey = request.data?.let { "${it}_${request.size}" }

        // 1. Check in-memory cache
        if (cacheKey != null && request.memoryCachePolicy.isReadAllowed && memoryCache != null) {
            val cached = memoryCache.get(cacheKey)
            if (cached != null) {
                return ImageResult.Success(
                    image = cached,
                    dataSource = DataSource.MEMORY,
                    request = request
                )
            }
        }

        // 2. Cache miss: Fetch and decode
        return when (val fetchResult = fetcherBoundary.fetch(request)) {
            is FetchResult.Success -> {
                try {
                    val decoded = decoderBoundary.decode(fetchResult.data, request)

                    // 3. Save to in-memory cache for subsequent requests / intent navigations
                    if (cacheKey != null && request.memoryCachePolicy.isWriteAllowed && memoryCache != null) {
                        memoryCache.put(cacheKey, decoded)
                    }

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
                // Check if we have a thumbnail in memory cache to fallback to!
                val thumbKey = request.data?.let { "${it}_thumb" }
                if (thumbKey != null && memoryCache != null) {
                    val thumb = memoryCache.get(thumbKey)
                    if (thumb != null) {
                        return ImageResult.Success(
                            image = thumb,
                            dataSource = DataSource.MEMORY,
                            request = request
                        )
                    }
                }
                ImageResult.Error(throwable = fetchResult.throwable, request = request)
            }
        }
    }

    override fun streamImage(request: ImageRequest): kotlinx.coroutines.flow.Flow<ImageResult> = kotlinx.coroutines.flow.flow {
        val cacheKey = request.data?.let { "${it}_${request.size}" }
        val thumbKey = request.data?.let { "${it}_thumb" }

        // 1. Check in-memory cache
        if (cacheKey != null && request.memoryCachePolicy.isReadAllowed && memoryCache != null) {
            val cached = memoryCache.get(cacheKey)
            if (cached != null) {
                emit(ImageResult.Success(image = cached, dataSource = DataSource.MEMORY, request = request))
                return@flow
            }
        }

        // 2. If thumbnail is cached, emit it immediately while fetching
        if (thumbKey != null && request.memoryCachePolicy.isReadAllowed && memoryCache != null) {
            val cachedThumb = memoryCache.get(thumbKey)
            if (cachedThumb != null) {
                emit(ImageResult.Progressive(image = cachedThumb, progress = 0.1f, request = request))
            }
        }

        // 3. Streaming fetch
        var lastEmittedProgressive: Any? = null
        val fetchResult = fetcherBoundary.fetchStreaming(request) { partialBytes, progress ->
            if (request.progressive) {
                val partialBitmap = decoderBoundary.decodePartial(partialBytes, request)
                if (partialBitmap != null) {
                    lastEmittedProgressive = partialBitmap
                    if (thumbKey != null && memoryCache != null) {
                        memoryCache.put(thumbKey, partialBitmap)
                    }
                    emit(ImageResult.Progressive(image = partialBitmap, progress = progress, request = request))
                }
            }
        }

        when (fetchResult) {
            is FetchResult.Success -> {
                try {
                    val decoded = decoderBoundary.decode(fetchResult.data, request)
                    if (cacheKey != null && request.memoryCachePolicy.isWriteAllowed && memoryCache != null) {
                        memoryCache.put(cacheKey, decoded)
                    }
                    emit(ImageResult.Success(image = decoded, dataSource = fetchResult.dataSource, request = request))
                } catch (ce: kotlinx.coroutines.CancellationException) {
                    throw ce
                } catch (t: Throwable) {
                    if (lastEmittedProgressive != null) {
                        emit(ImageResult.Success(image = lastEmittedProgressive!!, dataSource = DataSource.MEMORY, request = request))
                    } else {
                        emit(ImageResult.Error(throwable = t, request = request))
                    }
                }
            }
            is FetchResult.Error -> {
                // If network failed but we have a progressive frame or cached thumbnail, fallback gracefully!
                val fallback = lastEmittedProgressive ?: (thumbKey?.let { memoryCache?.get(it) })
                if (fallback != null) {
                    emit(ImageResult.Success(image = fallback, dataSource = DataSource.MEMORY, request = request))
                } else {
                    emit(ImageResult.Error(throwable = fetchResult.throwable, request = request))
                }
            }
        }
    }
}

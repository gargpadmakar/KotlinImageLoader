package com.padmakar.imageloader.data

import com.padmakar.imageloader.core.model.DataSource
import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageResult
import com.padmakar.imageloader.data.boundary.FetchResult
import com.padmakar.imageloader.data.boundary.ImageCacheBoundary
import com.padmakar.imageloader.data.boundary.ImageDecoderBoundary
import com.padmakar.imageloader.data.boundary.ImageFetcherBoundary
import com.padmakar.imageloader.data.cache.DefaultMemoryCacheInstance
import com.padmakar.imageloader.data.cache.DiskLruCache
import com.padmakar.imageloader.data.dedup.RequestDeduplicator
import com.padmakar.imageloader.domain.repository.ImageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Data layer implementation of [ImageRepository].
 *
 * Coordinates data flow across [ImageCacheBoundary], [DiskLruCache], [ImageFetcherBoundary], and [ImageDecoderBoundary].
 * Follows multi-tier caching with request deduplication, configurable retries, and offline fallback:
 * 1. Checks in-memory cache first if [ImageRequest.memoryCachePolicy] allows reads.
 * 2. On memory miss, checks persistent disk cache if [ImageRequest.diskCachePolicy] allows reads.
 * 3. On disk hit, decodes, transforms, saves to memory cache, and returns [DataSource.DISK].
 * 4. On cache miss, deduplicates identical in-flight network requests.
 * 5. Retries transient network failures according to [ImageRequest.retryAttempts] with exponential backoff.
 * 6. Falls back to offline disk cache or cached thumbnails if network is unreachable.
 * 7. Saves fetched raw bytes to disk cache and transformed bitmap to memory cache.
 */
class DefaultImageRepository(
    private val fetcherBoundary: ImageFetcherBoundary = com.padmakar.imageloader.data.fetcher.HttpUrlConnectionFetcher(),
    private val decoderBoundary: ImageDecoderBoundary = com.padmakar.imageloader.data.decoder.CompositeImageDecoder(),
    val memoryCache: ImageCacheBoundary? = DefaultMemoryCacheInstance.instance,
    val diskCache: DiskLruCache? = null
) : ImageRepository {

    private val deduplicator = RequestDeduplicator<ImageResult>()

    private fun computeMemoryCacheKey(request: ImageRequest): String? {
        val base = request.data ?: return null
        val transformKeys = if (request.transformations.isNotEmpty()) {
            "_" + request.transformations.joinToString(",") { it.cacheKey }
        } else ""
        return "${base}_${request.size}$transformKeys"
    }

    private suspend fun applyTransformations(image: Any, request: ImageRequest): Any {
        if (request.transformations.isEmpty() || image !is android.graphics.Bitmap) return image
        var currentBitmap: android.graphics.Bitmap = image
        for (transformation in request.transformations) {
            currentBitmap = transformation.transform(currentBitmap, request.size)
        }
        return currentBitmap
    }

    private suspend fun fetchWithRetry(request: ImageRequest): FetchResult {
        var lastResult: FetchResult = fetcherBoundary.fetch(request)
        if (lastResult is FetchResult.Success || request.retryAttempts <= 0) {
            return lastResult
        }

        var attempts = 0
        while (lastResult is FetchResult.Error && attempts < request.retryAttempts) {
            attempts++
            delay(request.retryDelayMillis * attempts)
            lastResult = fetcherBoundary.fetch(request)
            if (lastResult is FetchResult.Success) {
                return lastResult
            }
        }
        return lastResult
    }

    private suspend fun tryOfflineFallback(request: ImageRequest): ImageResult? {
        val diskKey = request.data?.toString()
        if (diskKey != null && diskCache != null) {
            val cachedBytes = diskCache.get(diskKey)
            if (cachedBytes != null) {
                try {
                    val decoded = decoderBoundary.decode(cachedBytes, request)
                    val transformed = applyTransformations(decoded, request)
                    return ImageResult.Success(image = transformed, dataSource = DataSource.DISK, request = request)
                } catch (_: Throwable) {}
            }
        }

        // Try thumbnail in disk or memory if specified
        if (request.thumbnail != null) {
            val thumbDiskKey = request.thumbnail.toString()
            val thumbBytes = diskCache?.get(thumbDiskKey)
            if (thumbBytes != null) {
                try {
                    val decoded = decoderBoundary.decode(thumbBytes, request)
                    val transformed = applyTransformations(decoded, request)
                    return ImageResult.Success(image = transformed, dataSource = DataSource.DISK, request = request)
                } catch (_: Throwable) {}
            }

            val thumbMem = memoryCache?.get("${request.thumbnail}")
            if (thumbMem != null) {
                return ImageResult.Success(image = thumbMem, dataSource = DataSource.MEMORY, request = request)
            }
        }

        // Try progressive thumb key
        val thumbKey = request.data?.let { "${it}_thumb" }
        if (thumbKey != null && memoryCache != null) {
            val thumb = memoryCache.get(thumbKey)
            if (thumb != null) {
                return ImageResult.Success(image = thumb, dataSource = DataSource.MEMORY, request = request)
            }
        }

        return null
    }

    override suspend fun loadImage(request: ImageRequest): ImageResult {
        val cacheKey = computeMemoryCacheKey(request)
        val diskKey = request.data?.toString()

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

        // 2. Check disk cache
        if (diskKey != null && request.diskCachePolicy.isReadAllowed && diskCache != null) {
            val cachedBytes = diskCache.get(diskKey)
            if (cachedBytes != null) {
                try {
                    val decoded = decoderBoundary.decode(cachedBytes, request)
                    val transformed = applyTransformations(decoded, request)
                    if (cacheKey != null && request.memoryCachePolicy.isWriteAllowed && memoryCache != null) {
                        memoryCache.put(cacheKey, transformed)
                    }
                    return ImageResult.Success(
                        image = transformed,
                        dataSource = DataSource.DISK,
                        request = request
                    )
                } catch (_: Throwable) {
                    diskCache.remove(diskKey)
                }
            }
        }

        // 3. Deduplicate concurrent network fetch & decode
        val dedupKey = "${request.data}_${request.size}"
        return deduplicator.execute(dedupKey) {
            // Re-check memory in case parallel request populated it
            if (cacheKey != null && request.memoryCachePolicy.isReadAllowed && memoryCache != null) {
                val cached = memoryCache.get(cacheKey)
                if (cached != null) {
                    return@execute ImageResult.Success(
                        image = cached,
                        dataSource = DataSource.MEMORY,
                        request = request
                    )
                }
            }

            when (val fetchResult = fetchWithRetry(request)) {
                is FetchResult.Success -> {
                    try {
                        val rawBytes = fetchResult.data as? ByteArray
                        // Save raw bytes to disk cache
                        if (rawBytes != null && diskKey != null && request.diskCachePolicy.isWriteAllowed && diskCache != null) {
                            diskCache.put(diskKey, rawBytes)
                        }

                        val decoded = decoderBoundary.decode(fetchResult.data, request)
                        val transformed = applyTransformations(decoded, request)

                        // Save to in-memory cache
                        if (cacheKey != null && request.memoryCachePolicy.isWriteAllowed && memoryCache != null) {
                            memoryCache.put(cacheKey, transformed)
                        }

                        ImageResult.Success(
                            image = transformed,
                            dataSource = fetchResult.dataSource,
                            request = request
                        )
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (t: Throwable) {
                        val fallback = tryOfflineFallback(request)
                        fallback ?: ImageResult.Error(throwable = t, request = request)
                    }
                }
                is FetchResult.Error -> {
                    val fallback = tryOfflineFallback(request)
                    fallback ?: ImageResult.Error(throwable = fetchResult.throwable, request = request)
                }
            }
        }
    }

    override fun streamImage(request: ImageRequest): Flow<ImageResult> = flow {
        val cacheKey = computeMemoryCacheKey(request)
        val diskKey = request.data?.toString()
        val thumbKey = request.data?.let { "${it}_thumb" }

        // 1. Check in-memory cache
        if (cacheKey != null && request.memoryCachePolicy.isReadAllowed && memoryCache != null) {
            val cached = memoryCache.get(cacheKey)
            if (cached != null) {
                emit(ImageResult.Success(image = cached, dataSource = DataSource.MEMORY, request = request))
                return@flow
            }
        }

        // 2. Check disk cache
        if (diskKey != null && request.diskCachePolicy.isReadAllowed && diskCache != null) {
            val cachedBytes = diskCache.get(diskKey)
            if (cachedBytes != null) {
                try {
                    val decoded = decoderBoundary.decode(cachedBytes, request)
                    val transformed = applyTransformations(decoded, request)
                    if (cacheKey != null && request.memoryCachePolicy.isWriteAllowed && memoryCache != null) {
                        memoryCache.put(cacheKey, transformed)
                    }
                    emit(ImageResult.Success(image = transformed, dataSource = DataSource.DISK, request = request))
                    return@flow
                } catch (_: Throwable) {
                    diskCache.remove(diskKey)
                }
            }
        }

        // 3. Emit thumbnail if specified and cached, or check progressive thumb
        if (request.thumbnail != null) {
            val thumbDiskKey = request.thumbnail.toString()
            val thumbBytes = diskCache?.get(thumbDiskKey)
            if (thumbBytes != null) {
                try {
                    val thumbDecoded = decoderBoundary.decode(thumbBytes, request)
                    emit(ImageResult.Progressive(image = thumbDecoded, progress = 0.2f, request = request))
                } catch (_: Throwable) {}
            } else {
                val thumbMem = memoryCache?.get("${request.thumbnail}")
                if (thumbMem != null) {
                    emit(ImageResult.Progressive(image = thumbMem, progress = 0.2f, request = request))
                }
            }
        } else if (thumbKey != null && request.memoryCachePolicy.isReadAllowed && memoryCache != null) {
            val cachedThumb = memoryCache.get(thumbKey)
            if (cachedThumb != null) {
                emit(ImageResult.Progressive(image = cachedThumb, progress = 0.1f, request = request))
            }
        }

        // 4. Streaming fetch
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
                    val rawBytes = fetchResult.data as? ByteArray
                    if (rawBytes != null && diskKey != null && request.diskCachePolicy.isWriteAllowed && diskCache != null) {
                        diskCache.put(diskKey, rawBytes)
                    }

                    val decoded = decoderBoundary.decode(fetchResult.data, request)
                    val transformed = applyTransformations(decoded, request)
                    if (cacheKey != null && request.memoryCachePolicy.isWriteAllowed && memoryCache != null) {
                        memoryCache.put(cacheKey, transformed)
                    }
                    emit(ImageResult.Success(image = transformed, dataSource = fetchResult.dataSource, request = request))
                } catch (ce: CancellationException) {
                    throw ce
                } catch (t: Throwable) {
                    val fallback = lastEmittedProgressive ?: tryOfflineFallback(request)?.let { (it as? ImageResult.Success)?.image }
                    if (fallback != null) {
                        emit(ImageResult.Success(image = fallback, dataSource = DataSource.MEMORY, request = request))
                    } else {
                        emit(ImageResult.Error(throwable = t, request = request))
                    }
                }
            }
            is FetchResult.Error -> {
                val fallback = lastEmittedProgressive ?: tryOfflineFallback(request)?.let { (it as? ImageResult.Success)?.image }
                if (fallback != null) {
                    emit(ImageResult.Success(image = fallback, dataSource = DataSource.MEMORY, request = request))
                } else {
                    emit(ImageResult.Error(throwable = fetchResult.throwable, request = request))
                }
            }
        }
    }
}

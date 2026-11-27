package com.padmakar.imageloader.data

import com.padmakar.imageloader.core.model.CachePolicy
import com.padmakar.imageloader.core.model.DataSource
import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageResult
import com.padmakar.imageloader.data.boundary.FetchResult
import com.padmakar.imageloader.data.boundary.ImageCacheBoundary
import com.padmakar.imageloader.data.boundary.ImageDecoderBoundary
import com.padmakar.imageloader.data.boundary.ImageFetcherBoundary
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultImageRepositoryCacheTest {

    private class SimpleMemoryCache : ImageCacheBoundary {
        private val map = mutableMapOf<String, Any>()
        override suspend fun get(key: String): Any? = map[key]
        override suspend fun put(key: String, value: Any) { map[key] = value }
        override suspend fun remove(key: String): Boolean = map.remove(key) != null
        override suspend fun clear() { map.clear() }
    }

    @Test
    fun `loadImage caches decoded image and returns MEMORY on subsequent load`() = runBlocking {
        var fetchCount = 0
        var decodeCount = 0

        val fakeFetcher = object : ImageFetcherBoundary {
            override suspend fun fetch(request: ImageRequest): FetchResult {
                fetchCount++
                return FetchResult.Success("raw-bytes".toByteArray(), DataSource.NETWORK)
            }
        }

        val fakeDecoder = object : ImageDecoderBoundary {
            override suspend fun decode(source: Any, request: ImageRequest): Any {
                decodeCount++
                return "decoded-bitmap-object"
            }
        }

        val memoryCache = SimpleMemoryCache()
        val repository = DefaultImageRepository(
            fetcherBoundary = fakeFetcher,
            decoderBoundary = fakeDecoder,
            memoryCache = memoryCache
        )

        val request = ImageRequest(data = "https://example.com/photo.jpg")

        // First call: Cache miss -> fetches & decodes
        val result1 = repository.loadImage(request)
        assertTrue(result1 is ImageResult.Success)
        assertEquals(DataSource.NETWORK, (result1 as ImageResult.Success).dataSource)
        assertEquals(1, fetchCount)
        assertEquals(1, decodeCount)

        // Second call (e.g. after navigating back or intent switch): Cache HIT!
        val result2 = repository.loadImage(request)
        assertTrue(result2 is ImageResult.Success)
        assertEquals(DataSource.MEMORY, (result2 as ImageResult.Success).dataSource)
        assertEquals(1, fetchCount)   // Fetcher not called again!
        assertEquals(1, decodeCount)  // Decoder not called again!
    }

    @Test
    fun `loadImage respects DISABLED cache policy`() = runBlocking {
        var fetchCount = 0

        val fakeFetcher = object : ImageFetcherBoundary {
            override suspend fun fetch(request: ImageRequest): FetchResult {
                fetchCount++
                return FetchResult.Success("raw-bytes".toByteArray(), DataSource.NETWORK)
            }
        }

        val fakeDecoder = object : ImageDecoderBoundary {
            override suspend fun decode(source: Any, request: ImageRequest): Any = "bitmap"
        }

        val memoryCache = SimpleMemoryCache()
        val repository = DefaultImageRepository(
            fetcherBoundary = fakeFetcher,
            decoderBoundary = fakeDecoder,
            memoryCache = memoryCache
        )

        val request = ImageRequest(
            data = "https://example.com/photo.jpg",
            memoryCachePolicy = CachePolicy.DISABLED
        )

        repository.loadImage(request)
        repository.loadImage(request)

        // Cache was bypassed, fetched twice
        assertEquals(2, fetchCount)
    }
}

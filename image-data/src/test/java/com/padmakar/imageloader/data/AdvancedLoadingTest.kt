package com.padmakar.imageloader.data

import com.padmakar.imageloader.core.model.DataSource
import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageResult
import com.padmakar.imageloader.data.boundary.FetchResult
import com.padmakar.imageloader.data.boundary.ImageCacheBoundary
import com.padmakar.imageloader.data.boundary.ImageDecoderBoundary
import com.padmakar.imageloader.data.boundary.ImageFetcherBoundary
import com.padmakar.imageloader.data.cache.DiskLruCache
import com.padmakar.imageloader.data.cache.LruMemoryCache
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class AdvancedLoadingTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testRetryPolicySuccessAfterFailure() = runBlocking {
        var callCount = 0
        val mockFetcher = object : ImageFetcherBoundary {
            override suspend fun fetch(request: ImageRequest): FetchResult {
                callCount++
                return if (callCount < 2) {
                    FetchResult.Error(IOException("Temporary network glitch"))
                } else {
                    FetchResult.Success(byteArrayOf(1, 2, 3), DataSource.NETWORK)
                }
            }
        }

        val mockDecoder = object : ImageDecoderBoundary {
            override suspend fun decode(source: Any, request: ImageRequest): Any = "DecodedImage"
        }

        val repo = DefaultImageRepository(
            fetcherBoundary = mockFetcher,
            decoderBoundary = mockDecoder,
            memoryCache = null,
            diskCache = null
        )

        val request = ImageRequest.Builder("https://example.com/retry.jpg")
            .retry(attempts = 2, delayMillis = 10L)
            .build()

        val result = repo.loadImage(request)
        assertTrue(result is ImageResult.Success)
        assertEquals(2, callCount)
    }

    @Test
    fun testOfflineFallbackToDiskCache() = runBlocking {
        val diskCacheDir = tempFolder.newFolder("disk_cache")
        val diskCache = DiskLruCache(diskCacheDir, 1024 * 1024)

        // Seed disk cache
        val url = "https://example.com/offline.jpg"
        diskCache.put(url, byteArrayOf(9, 8, 7))

        // Failing fetcher (simulating no internet)
        val mockFetcher = object : ImageFetcherBoundary {
            override suspend fun fetch(request: ImageRequest): FetchResult {
                return FetchResult.Error(IOException("No network connectivity"))
            }
        }

        val mockDecoder = object : ImageDecoderBoundary {
            override suspend fun decode(source: Any, request: ImageRequest): Any = "OfflineDecodedImage"
        }

        val repo = DefaultImageRepository(
            fetcherBoundary = mockFetcher,
            decoderBoundary = mockDecoder,
            memoryCache = null,
            diskCache = diskCache
        )

        val request = ImageRequest.Builder(url)
            .retry(attempts = 0)
            .build()

        val result = repo.loadImage(request)
        assertTrue("Expected offline fallback to disk cache", result is ImageResult.Success)
        val success = result as ImageResult.Success
        assertEquals(DataSource.DISK, success.dataSource)
        assertEquals("OfflineDecodedImage", success.image)
    }

    @Test
    fun testCacheStatsTracking() = runBlocking {
        val diskCacheDir = tempFolder.newFolder("disk_stats")
        val diskCache = DiskLruCache(diskCacheDir, 1024 * 1024)

        val memCache = LruMemoryCache(1024 * 1024)

        // 1. Initial stats
        var memStats = memCache.stats()
        assertEquals(0L, memStats.hitCount)
        assertEquals(0L, memStats.missCount)

        // 2. Put and get
        memCache.put("key1", "sample")
        val found = memCache.get("key1")
        val notFound = memCache.get("key2")

        memStats = memCache.stats()
        assertEquals(1L, memStats.hitCount)
        assertEquals(1L, memStats.missCount)
        assertEquals(1L, memStats.putCount)
        assertEquals(0.5f, memStats.hitRate, 0.01f)

        // Disk stats
        diskCache.put("file1", byteArrayOf(1, 2, 3))
        diskCache.get("file1")
        diskCache.get("file2")

        val diskStats = diskCache.stats()
        assertEquals(1L, diskStats.hitCount)
        assertEquals(1L, diskStats.missCount)
        assertEquals(1L, diskStats.putCount)
    }
}

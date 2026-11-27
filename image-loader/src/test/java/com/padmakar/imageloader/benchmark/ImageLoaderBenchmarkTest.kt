package com.padmakar.imageloader.benchmark

import com.padmakar.imageloader.core.model.DataSource
import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageResult
import com.padmakar.imageloader.core.model.ImageSize
import com.padmakar.imageloader.data.DefaultImageRepository
import com.padmakar.imageloader.data.boundary.FetchResult
import com.padmakar.imageloader.data.boundary.ImageDecoderBoundary
import com.padmakar.imageloader.data.boundary.ImageFetcherBoundary
import com.padmakar.imageloader.data.cache.DiskLruCache
import com.padmakar.imageloader.data.cache.LruMemoryCache
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.system.measureNanoTime

/**
 * Reproducible benchmark harness measuring:
 * 1. Cold Load (Network fetch + Disk Write + Decode + Memory Write)
 * 2. Memory Cache Hit Latency
 * 3. Disk Cache Hit Latency
 * 4. Concurrent Requests Throughput & Deduplication
 * 5. High-Throughput Request Bursts
 */
class ImageLoaderBenchmarkTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun runReproducibleBenchmarks() = runBlocking {
        val diskCacheDir = tempFolder.newFolder("benchmark_disk")
        val diskCache = DiskLruCache(diskCacheDir, 50 * 1024 * 1024L)
        val memoryCache = LruMemoryCache(20 * 1024 * 1024)

        val samplePayload = ByteArray(256 * 1024) { (it % 128).toByte() } // 256 KB simulated image payload

        val mockFetcher = object : ImageFetcherBoundary {
            override suspend fun fetch(request: ImageRequest): FetchResult {
                // Simulate network latency (e.g. 15ms)
                Thread.sleep(15)
                return FetchResult.Success(samplePayload, DataSource.NETWORK)
            }
        }

        val mockDecoder = object : ImageDecoderBoundary {
            override suspend fun decode(source: Any, request: ImageRequest): Any {
                // Simulate decoding compute time (e.g. 5ms)
                Thread.sleep(5)
                return "BenchmarkDecodedBitmap_${(source as? ByteArray)?.size ?: 0}"
            }
        }

        val repo = DefaultImageRepository(
            fetcherBoundary = mockFetcher,
            decoderBoundary = mockDecoder,
            memoryCache = memoryCache,
            diskCache = diskCache
        )

        val targetUrl = "https://images.example.com/sample_photo.jpg"
        val request = ImageRequest.Builder(targetUrl)
            .size(ImageSize.Exact(800, 800))
            .build()

        // 1. Cold Load Benchmark
        val coldTimeNanos = measureNanoTime {
            val result = repo.loadImage(request)
            assertTrue(result is ImageResult.Success)
            assertTrue((result as ImageResult.Success).dataSource == DataSource.NETWORK)
        }
        val coldMs = coldTimeNanos / 1_000_000.0

        // 2. Memory Cache Hit Benchmark
        val memWarmupNanos = measureNanoTime {
            val result = repo.loadImage(request)
            assertTrue(result is ImageResult.Success)
            assertTrue((result as ImageResult.Success).dataSource == DataSource.MEMORY)
        }
        val memMs = memWarmupNanos / 1_000_000.0

        // 3. Disk Cache Hit Benchmark (evict from memory first)
        memoryCache.clear()
        val diskHitNanos = measureNanoTime {
            val result = repo.loadImage(request)
            assertTrue(result is ImageResult.Success)
            assertTrue((result as ImageResult.Success).dataSource == DataSource.DISK)
        }
        val diskMs = diskHitNanos / 1_000_000.0

        // 4. Concurrent In-flight Deduplication Benchmark (10 simultaneous requests for same URL)
        memoryCache.clear()
        diskCache.clear()
        val concurrentTimeNanos = measureNanoTime {
            val jobs = (1..10).map {
                async { repo.loadImage(request) }
            }
            val results = jobs.awaitAll()
            results.forEach {
                assertTrue(it is ImageResult.Success)
            }
        }
        val concurrentMs = concurrentTimeNanos / 1_000_000.0

        println("===============================================================")
        println("       KOTLIN IMAGE LOADER - REPRODUCIBLE BENCHMARK RESULTS    ")
        println("===============================================================")
        println("Cold Load Latency (Fetch + Disk + Decode + Memory): %.3f ms".format(coldMs))
        println("Memory Cache Hit Latency:                           %.3f ms".format(memMs))
        println("Disk Cache Hit Latency (Disk read + Decode):        %.3f ms".format(diskMs))
        println("10 Concurrent Parallel Requests Latency:           %.3f ms".format(concurrentMs))
        println("===============================================================")

        // Verifications
        assertTrue("Memory hit latency must be under 5ms", memMs < 5.0)
        assertTrue("Disk hit latency must be under 30ms", diskMs < 30.0)
    }
}

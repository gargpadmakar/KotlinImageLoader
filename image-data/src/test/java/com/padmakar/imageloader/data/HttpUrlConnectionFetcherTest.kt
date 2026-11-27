package com.padmakar.imageloader.data

import com.padmakar.imageloader.core.model.DataSource
import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.data.boundary.FetchResult
import com.padmakar.imageloader.data.fetcher.HttpException
import com.padmakar.imageloader.data.fetcher.HttpUrlConnectionFetcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class HttpUrlConnectionFetcherTest {

    private class MockHttpURLConnection(
        url: URL,
        private val responseCodeValue: Int,
        private val responseData: ByteArray = ByteArray(0),
        private val responseHeaders: Map<String, String> = emptyMap(),
        private val onDisconnect: () -> Unit = {}
    ) : HttpURLConnection(url) {

        var disconnected = false

        override fun connect() {}

        override fun disconnect() {
            disconnected = true
            onDisconnect()
        }

        override fun usingProxy(): Boolean = false

        override fun getResponseCode(): Int = responseCodeValue

        override fun getResponseMessage(): String = when (responseCodeValue) {
            200 -> "OK"
            404 -> "Not Found"
            500 -> "Internal Server Error"
            301 -> "Moved Permanently"
            302 -> "Found"
            else -> "Status $responseCodeValue"
        }

        override fun getHeaderField(name: String?): String? = responseHeaders[name]

        override fun getInputStream(): InputStream = ByteArrayInputStream(responseData)

        override fun getErrorStream(): InputStream = ByteArrayInputStream(responseData)
    }

    @Test
    fun `fetch returns Success with NETWORK data source on HTTP 200`() = runBlocking {
        val testBytes = "mock-image-data-payload".toByteArray()
        val fetcher = HttpUrlConnectionFetcher(
            urlOpener = { url ->
                MockHttpURLConnection(
                    url = url,
                    responseCodeValue = 200,
                    responseData = testBytes
                )
            }
        )

        val request = ImageRequest(data = "https://example.com/test.png")
        val result = fetcher.fetch(request)

        assertTrue(result is FetchResult.Success)
        val success = result as FetchResult.Success
        assertEquals(DataSource.NETWORK, success.dataSource)
        assertArrayEquals(testBytes, success.data as ByteArray)
    }

    @Test
    fun `fetch returns Error with HttpException on HTTP 404`() = runBlocking {
        val fetcher = HttpUrlConnectionFetcher(
            urlOpener = { url ->
                MockHttpURLConnection(
                    url = url,
                    responseCodeValue = 404
                )
            }
        )

        val request = ImageRequest(data = "https://example.com/notfound.png")
        val result = fetcher.fetch(request)

        assertTrue(result is FetchResult.Error)
        val error = result as FetchResult.Error
        assertTrue(error.throwable is HttpException)
        assertEquals(404, (error.throwable as HttpException).statusCode)
    }

    @Test
    fun `fetch follows manual redirects up to limit`() = runBlocking {
        val finalPayload = "final-redirected-image".toByteArray()
        val fetcher = HttpUrlConnectionFetcher(
            urlOpener = { url ->
                if (url.toString() == "https://example.com/initial.jpg") {
                    MockHttpURLConnection(
                        url = url,
                        responseCodeValue = 301,
                        responseHeaders = mapOf("Location" to "https://example.com/target.jpg")
                    )
                } else {
                    MockHttpURLConnection(
                        url = url,
                        responseCodeValue = 200,
                        responseData = finalPayload
                    )
                }
            }
        )

        val result = fetcher.fetch(ImageRequest(data = "https://example.com/initial.jpg"))

        assertTrue(result is FetchResult.Success)
        val success = result as FetchResult.Success
        assertArrayEquals(finalPayload, success.data as ByteArray)
    }

    @Test
    fun `fetch rejects insecure redirect from HTTPS to HTTP`() = runBlocking {
        val fetcher = HttpUrlConnectionFetcher(
            urlOpener = { url ->
                MockHttpURLConnection(
                    url = url,
                    responseCodeValue = 302,
                    responseHeaders = mapOf("Location" to "http://insecure.com/image.jpg")
                )
            }
        )

        val result = fetcher.fetch(ImageRequest(data = "https://secure.com/image.jpg"))

        assertTrue(result is FetchResult.Error)
        val error = result as FetchResult.Error
        assertTrue(error.throwable is SecurityException)
    }

    @Test
    fun `fetch throws on exceeding max redirects`() = runBlocking {
        val fetcher = HttpUrlConnectionFetcher(
            urlOpener = { url ->
                MockHttpURLConnection(
                    url = url,
                    responseCodeValue = 302,
                    responseHeaders = mapOf("Location" to "https://example.com/loop.jpg")
                )
            }
        )

        val result = fetcher.fetch(ImageRequest(data = "https://example.com/loop.jpg"))

        assertTrue(result is FetchResult.Error)
        val error = result as FetchResult.Error
        assertTrue(error.throwable is IOException)
        assertTrue(error.throwable.message!!.contains("Too many redirects"))
    }

    @Test
    fun `fetch rejects download when exceeding max download limit`() = runBlocking {
        val largeBytes = ByteArray(1024)
        val fetcher = HttpUrlConnectionFetcher(
            maxDownloadBytes = 500L, // Limit 500 bytes
            urlOpener = { url ->
                MockHttpURLConnection(
                    url = url,
                    responseCodeValue = 200,
                    responseData = largeBytes
                )
            }
        )

        val result = fetcher.fetch(ImageRequest(data = "https://example.com/large.jpg"))

        assertTrue(result is FetchResult.Error)
        val error = result as FetchResult.Error
        assertTrue(error.throwable is IllegalStateException)
        assertTrue(error.throwable.message!!.contains("exceeded maximum"))
    }

    @Test
    fun `fetch actively disconnects connection when coroutine is cancelled`() = runBlocking {
        var disconnected = false
        val latch = java.util.concurrent.CountDownLatch(1)

        val stallingInputStream = object : InputStream() {
            override fun read(): Int {
                latch.await(2, java.util.concurrent.TimeUnit.SECONDS)
                return -1
            }
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                latch.await(2, java.util.concurrent.TimeUnit.SECONDS)
                return -1
            }
        }

        val fetcher = HttpUrlConnectionFetcher(
            urlOpener = { url ->
                object : HttpURLConnection(url) {
                    override fun connect() {}
                    override fun disconnect() {
                        disconnected = true
                        latch.countDown()
                    }
                    override fun usingProxy(): Boolean = false
                    override fun getResponseCode(): Int = 200
                    override fun getInputStream(): InputStream = stallingInputStream
                }
            }
        )

        val job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                fetcher.fetch(ImageRequest(data = "https://example.com/slow.jpg"))
            } catch (_: CancellationException) {}
        }

        delay(50)
        job.cancel()
        job.join()

        assertTrue(disconnected)
    }
}

package com.padmakar.imageloader.data.fetcher

import com.padmakar.imageloader.core.model.DataSource
import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.data.boundary.FetchResult
import com.padmakar.imageloader.data.boundary.ImageFetcherBoundary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Network fetcher using standard [HttpURLConnection].
 *
 * Handles:
 * - Active cancellation interruption via [Job.invokeOnCompletion] disconnect.
 * - In-flight cancellation checks during chunked reads.
 * - Manual redirect handling for HTTP 301, 302, 303, 307, 308 with relative Location resolution.
 * - HTTPS to HTTP downgrade redirect protection.
 * - Maximum download size limit to prevent memory exhaustion.
 * - Full stream and connection closure in finally blocks.
 */
class HttpUrlConnectionFetcher(
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 20_000,
    private val maxDownloadBytes: Long = 50 * 1024 * 1024L, // 50MB safety ceiling
    private val urlOpener: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }
) : ImageFetcherBoundary {

    companion object {
        const val MAX_REDIRECTS = 5
        private const val BUFFER_SIZE = 8192

        val REDIRECT_CODES = setOf(
            HttpURLConnection.HTTP_MOVED_PERM, // 301
            HttpURLConnection.HTTP_MOVED_TEMP, // 302
            HttpURLConnection.HTTP_SEE_OTHER,  // 303
            307, // Temporary Redirect
            308  // Permanent Redirect
        )
    }

    override suspend fun fetch(request: ImageRequest): FetchResult {
        val rawData = request.data
        if (rawData == null) {
            return FetchResult.Error(IllegalArgumentException("Request data cannot be null"))
        }

        val urlString = rawData.toString().trim()
        if (urlString.isBlank()) {
            return FetchResult.Error(IllegalArgumentException("Request URL string cannot be blank"))
        }

        var currentUrl: URL
        try {
            currentUrl = URL(urlString)
        } catch (e: Exception) {
            return FetchResult.Error(IllegalArgumentException("Invalid URL format: $urlString", e))
        }

        val initialProtocol = currentUrl.protocol.lowercase()
        if (initialProtocol != "http" && initialProtocol != "https") {
            return FetchResult.Error(
                IllegalArgumentException("Unsupported protocol '$initialProtocol'. Only HTTP and HTTPS are supported.")
            )
        }

        var redirectCount = 0

        while (true) {
            currentCoroutineContext().ensureActive()

            var connection: HttpURLConnection? = null
            var completionHandle: kotlinx.coroutines.DisposableHandle? = null

            try {
                connection = urlOpener(currentUrl).apply {
                    instanceFollowRedirects = false
                    connectTimeout = connectTimeoutMs
                    readTimeout = readTimeoutMs
                    requestMethod = "GET"
                    doInput = true

                    // Set standard User-Agent if not provided
                    if (!request.headers.containsKey("User-Agent")) {
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) KotlinImageLoader/1.0")
                    }

                    // Apply custom request headers
                    request.headers.forEach { (key, value) ->
                        setRequestProperty(key, value)
                    }
                }

                // Actively disconnect if the calling coroutine job is cancelled
                val job = currentCoroutineContext()[Job]
                completionHandle = job?.invokeOnCompletion {
                    try {
                        connection.disconnect()
                    } catch (_: Throwable) {}
                }

                connection.connect()
                val responseCode = connection.responseCode

                // Handle manual redirects
                if (responseCode in REDIRECT_CODES) {
                    val location = connection.getHeaderField("Location")
                        ?: throw IOException("HTTP $responseCode redirect received without Location header")

                    val nextUrl = try {
                        URL(currentUrl, location)
                    } catch (e: Exception) {
                        throw IOException("Invalid redirect Location header: $location", e)
                    }

                    // Security check: reject HTTPS to HTTP downgrade
                    if (currentUrl.protocol.equals("https", ignoreCase = true) &&
                        nextUrl.protocol.equals("http", ignoreCase = true)
                    ) {
                        throw SecurityException(
                            "Insecure redirect from HTTPS to HTTP is forbidden: $currentUrl -> $nextUrl"
                        )
                    }

                    redirectCount++
                    if (redirectCount > MAX_REDIRECTS) {
                        throw IOException("Too many redirects (exceeded limit of $MAX_REDIRECTS)")
                    }

                    currentUrl = nextUrl
                    continue
                }

                // Handle HTTP errors
                if (responseCode !in 200..299) {
                    val errorMessage = try {
                        connection.errorStream?.bufferedReader()?.use { it.readText() }
                    } catch (_: Exception) {
                        null
                    } ?: connection.responseMessage ?: "Error $responseCode"

                    return FetchResult.Error(HttpException(responseCode, errorMessage))
                }

                // Check declared Content-Length
                val contentLength = connection.contentLengthLong
                if (contentLength > maxDownloadBytes) {
                    return FetchResult.Error(
                        IllegalStateException(
                            "Content-Length ($contentLength bytes) exceeds maximum download size ($maxDownloadBytes bytes)"
                        )
                    )
                }

                // Stream content into safe re-readable ByteArray with active cancellation and bounds checking
                val bytes = readStreamFully(
                    stream = connection.inputStream,
                    maxBytes = maxDownloadBytes,
                    contentLength = contentLength,
                    onProgress = null
                )

                return FetchResult.Success(
                    data = bytes,
                    dataSource = DataSource.NETWORK
                )

            } catch (ce: CancellationException) {
                // Never swallow CancellationException
                throw ce
            } catch (t: Throwable) {
                return FetchResult.Error(t)
            } finally {
                completionHandle?.dispose()
                try {
                    connection?.disconnect()
                } catch (_: Throwable) {}
            }
        }
    }

    override suspend fun fetchStreaming(
        request: ImageRequest,
        onProgress: suspend (bytes: ByteArray, progress: Float) -> Unit
    ): FetchResult {
        val rawData = request.data ?: return FetchResult.Error(IllegalArgumentException("Request data cannot be null"))
        val urlString = rawData.toString().trim()
        if (urlString.isBlank()) return FetchResult.Error(IllegalArgumentException("Request URL string cannot be blank"))

        var currentUrl: URL
        try {
            currentUrl = URL(urlString)
        } catch (e: Exception) {
            return FetchResult.Error(IllegalArgumentException("Invalid URL format: $urlString", e))
        }

        var redirectCount = 0
        while (true) {
            currentCoroutineContext().ensureActive()
            var connection: HttpURLConnection? = null
            var completionHandle: kotlinx.coroutines.DisposableHandle? = null

            try {
                connection = urlOpener(currentUrl).apply {
                    instanceFollowRedirects = false
                    connectTimeout = connectTimeoutMs
                    readTimeout = readTimeoutMs
                    requestMethod = "GET"
                    doInput = true

                    if (!request.headers.containsKey("User-Agent")) {
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) KotlinImageLoader/1.0")
                    }

                    request.headers.forEach { (k, v) -> setRequestProperty(k, v) }
                }

                val job = currentCoroutineContext()[Job]
                completionHandle = job?.invokeOnCompletion {
                    try { connection.disconnect() } catch (_: Throwable) {}
                }

                connection.connect()
                val responseCode = connection.responseCode

                if (responseCode in REDIRECT_CODES) {
                    val location = connection.getHeaderField("Location")
                        ?: throw IOException("HTTP $responseCode redirect without Location")
                    val nextUrl = URL(currentUrl, location)
                    if (currentUrl.protocol.equals("https", ignoreCase = true) && nextUrl.protocol.equals("http", ignoreCase = true)) {
                        throw SecurityException("Insecure redirect from HTTPS to HTTP forbidden")
                    }
                    redirectCount++
                    if (redirectCount > MAX_REDIRECTS) throw IOException("Too many redirects")
                    currentUrl = nextUrl
                    continue
                }

                if (responseCode !in 200..299) {
                    val msg = connection.responseMessage ?: "Error $responseCode"
                    return FetchResult.Error(HttpException(responseCode, msg))
                }

                val contentLength = connection.contentLengthLong
                if (contentLength > maxDownloadBytes) {
                    return FetchResult.Error(IllegalStateException("Content-Length exceeds maximum limit"))
                }

                val bytes = readStreamFully(
                    stream = connection.inputStream,
                    maxBytes = maxDownloadBytes,
                    contentLength = contentLength,
                    onProgress = onProgress
                )

                return FetchResult.Success(bytes, DataSource.NETWORK)
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                return FetchResult.Error(t)
            } finally {
                completionHandle?.dispose()
                try { connection?.disconnect() } catch (_: Throwable) {}
            }
        }
    }

    private suspend fun readStreamFully(
        stream: InputStream,
        maxBytes: Long,
        contentLength: Long = -1L,
        onProgress: (suspend (ByteArray, Float) -> Unit)? = null
    ): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        var totalBytesRead = 0L
        var lastEmitBytes = 0L

        stream.use { input ->
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = input.read(buffer)
                if (read == -1) break
                totalBytesRead += read
                if (totalBytesRead > maxBytes) {
                    throw IllegalStateException(
                        "Download size exceeded maximum allowed limit of $maxBytes bytes"
                    )
                }
                output.write(buffer, 0, read)

                // Progressive notification if enabled and at least 32KB read since last emit
                if (onProgress != null && (totalBytesRead - lastEmitBytes >= 32 * 1024L || totalBytesRead >= 16 * 1024L && lastEmitBytes == 0L)) {
                    lastEmitBytes = totalBytesRead
                    val progress = if (contentLength > 0) (totalBytesRead.toFloat() / contentLength).coerceIn(0f, 1f) else -1f
                    onProgress(output.toByteArray(), progress)
                }
            }
        }
        return output.toByteArray()
    }
}

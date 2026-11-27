package com.padmakar.imageloader.data.fetcher

import java.io.IOException

/**
 * Exception thrown when an HTTP request fails with a non-2xx status code.
 *
 * @param statusCode The HTTP status code received (e.g. 404, 500).
 * @param responseMessage The HTTP status message received from the server.
 */
class HttpException(
    val statusCode: Int,
    val responseMessage: String
) : IOException("HTTP $statusCode: $responseMessage")

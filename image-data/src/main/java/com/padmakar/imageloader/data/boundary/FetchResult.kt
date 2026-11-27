package com.padmakar.imageloader.data.boundary

import com.padmakar.imageloader.core.model.DataSource

/**
 * Result abstraction produced by an [ImageFetcherBoundary].
 */
sealed interface FetchResult {
    /**
     * Successfully fetched raw data (e.g., InputStream, ByteArray, or File).
     */
    data class Success(
        val data: Any,
        val dataSource: DataSource
    ) : FetchResult

    /**
     * Error encountered during fetching.
     */
    data class Error(val throwable: Throwable) : FetchResult
}

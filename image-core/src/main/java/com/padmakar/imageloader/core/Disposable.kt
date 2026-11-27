package com.padmakar.imageloader.core

/**
 * Handle representing an ongoing asynchronous image loading task that can be cancelled.
 */
interface Disposable {
    /** Cancels the ongoing image request execution. */
    fun dispose()

    /** Returns true if this request has already been cancelled or completed. */
    val isDisposed: Boolean
}

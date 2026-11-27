package com.padmakar.imageloader.data.dedup

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Multiplexes and deduplicates identical in-flight asynchronous operations.
 *
 * If multiple callers request the same key concurrently, only one execution runs;
 * all callers receive the shared [Deferred] result without redundant network or disk overhead.
 */
class RequestDeduplicator<T> {
    private val mutex = Mutex()
    private val inFlightRequests = mutableMapOf<String, Deferred<T>>()

    suspend fun execute(key: String, block: suspend () -> T): T {
        var deferredToAwait: Deferred<T>? = null

        mutex.withLock {
            val existing = inFlightRequests[key]
            if (existing != null && existing.isActive) {
                deferredToAwait = existing
            }
        }

        if (deferredToAwait != null) {
            return deferredToAwait!!.await()
        }

        return coroutineScope {
            var newDeferred: Deferred<T>? = null

            mutex.withLock {
                val existing = inFlightRequests[key]
                if (existing != null && existing.isActive) {
                    deferredToAwait = existing
                } else {
                    val created = async {
                        try {
                            block()
                        } finally {
                            mutex.withLock {
                                inFlightRequests.remove(key)
                            }
                        }
                    }
                    inFlightRequests[key] = created
                    newDeferred = created
                }
            }

            (deferredToAwait ?: newDeferred!!).await()
        }
    }
}

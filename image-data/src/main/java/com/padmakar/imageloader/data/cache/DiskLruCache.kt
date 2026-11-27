package com.padmakar.imageloader.data.cache

import com.padmakar.imageloader.core.cache.CacheStats
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe Disk LRU Cache for caching raw image data on internal disk storage.
 *
 * Characteristics:
 * - Mutex-protected operations preventing concurrent read/write races.
 * - Evicts least-recently-accessed files when total cache size exceeds [maxSizeBytes].
 * - Safe atomic file writes using temp file replacement.
 * - MD5 hashed filenames for filesystem safety.
 */
class DiskLruCache(
    private val directory: File,
    private val maxSizeBytes: Long = 100 * 1024 * 1024L // 100MB default
) {
    private val mutex = Mutex()
    private val hitCount = AtomicLong(0)
    private val missCount = AtomicLong(0)
    private val putCount = AtomicLong(0)
    private val evictionCount = AtomicLong(0)

    init {
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }

    suspend fun get(key: String): ByteArray? = mutex.withLock {
        val file = getFile(key)
        if (!file.exists()) {
            missCount.incrementAndGet()
            return null
        }

        try {
            // Update last modified timestamp for LRU tracking
            file.setLastModified(System.currentTimeMillis())
            val bytes = file.readBytes()
            hitCount.incrementAndGet()
            bytes
        } catch (_: IOException) {
            missCount.incrementAndGet()
            null
        }
    }

    suspend fun put(key: String, bytes: ByteArray): Boolean = mutex.withLock {
        if (bytes.isEmpty()) return false

        try {
            if (!directory.exists()) {
                directory.mkdirs()
            }

            // Write atomically via temporary file
            val targetFile = getFile(key)
            val tempFile = File(directory, "${targetFile.name}.tmp_${System.currentTimeMillis()}")
            tempFile.writeBytes(bytes)

            if (tempFile.renameTo(targetFile)) {
                targetFile.setLastModified(System.currentTimeMillis())
                putCount.incrementAndGet()
                trimToSize()
                true
            } else {
                tempFile.delete()
                false
            }
        } catch (_: IOException) {
            false
        }
    }

    suspend fun remove(key: String): Boolean = mutex.withLock {
        val file = getFile(key)
        if (file.exists()) {
            file.delete()
        } else false
    }

    suspend fun clear(): Unit = mutex.withLock {
        directory.listFiles()?.forEach { it.delete() }
    }

    suspend fun size(): Long = mutex.withLock {
        calculateCurrentSize()
    }

    suspend fun stats(): CacheStats = mutex.withLock {
        CacheStats(
            hitCount = hitCount.get(),
            missCount = missCount.get(),
            putCount = putCount.get(),
            evictionCount = evictionCount.get(),
            currentSizeBytes = calculateCurrentSize(),
            maxSizeBytes = maxSizeBytes
        )
    }

    private fun trimToSize() {
        var currentSize = calculateCurrentSize()
        if (currentSize <= maxSizeBytes) return

        val files = directory.listFiles() ?: return
        // Sort oldest modified first
        val sortedFiles = files.filter { it.isFile && !it.name.endsWith(".tmp") }
            .sortedBy { it.lastModified() }

        for (file in sortedFiles) {
            val length = file.length()
            if (file.delete()) {
                evictionCount.incrementAndGet()
                currentSize -= length
                if (currentSize <= maxSizeBytes) break
            }
        }
    }

    private fun calculateCurrentSize(): Long {
        val files = directory.listFiles() ?: return 0L
        return files.filter { it.isFile }.sumOf { it.length() }
    }

    private fun getFile(key: String): File {
        val safeName = hashKey(key)
        return File(directory, safeName)
    }

    companion object {
        fun hashKey(key: String): String {
            return try {
                val md = MessageDigest.getInstance("MD5")
                val digest = md.digest(key.toByteArray())
                digest.joinToString("") { "%02x".format(it) }
            } catch (_: Exception) {
                key.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            }
        }
    }
}

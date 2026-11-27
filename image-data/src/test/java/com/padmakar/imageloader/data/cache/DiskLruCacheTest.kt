package com.padmakar.imageloader.data.cache

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DiskLruCacheTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `put and get stores and retrieves bytes correctly`() = runTest {
        val cacheDir = tempFolder.newFolder("cache")
        val cache = DiskLruCache(directory = cacheDir, maxSizeBytes = 1024 * 1024)

        val testData = "Hello Image Loader".toByteArray()
        val key = "https://example.com/test.jpg"

        val putSuccess = cache.put(key, testData)
        assertTrue(putSuccess)

        val retrieved = cache.get(key)
        assertArrayEquals(testData, retrieved)
    }

    @Test
    fun `get returns null for missing key`() = runTest {
        val cacheDir = tempFolder.newFolder("cache_missing")
        val cache = DiskLruCache(directory = cacheDir, maxSizeBytes = 1024 * 1024)

        assertNull(cache.get("non_existing_key"))
    }

    @Test
    fun `remove deletes entry from disk`() = runTest {
        val cacheDir = tempFolder.newFolder("cache_remove")
        val cache = DiskLruCache(directory = cacheDir, maxSizeBytes = 1024 * 1024)

        val key = "item_to_remove"
        cache.put(key, byteArrayOf(1, 2, 3, 4))
        assertTrue(cache.remove(key))
        assertNull(cache.get(key))
    }

    @Test
    fun `lru eviction removes oldest entry when exceeding max size`() = runTest {
        val cacheDir = tempFolder.newFolder("cache_lru")
        // Max size: 100 bytes
        val cache = DiskLruCache(directory = cacheDir, maxSizeBytes = 100)

        val bytes50_1 = ByteArray(50) { 1 }
        val bytes50_2 = ByteArray(50) { 2 }
        val bytes50_3 = ByteArray(50) { 3 }

        cache.put("key1", bytes50_1)
        Thread.sleep(50)
        cache.put("key2", bytes50_2)
        Thread.sleep(50)

        // Adding 3rd item (50 bytes) exceeds 100 bytes limit -> key1 must be evicted!
        cache.put("key3", bytes50_3)

        assertNull("key1 should be evicted", cache.get("key1"))
        assertArrayEquals(bytes50_2, cache.get("key2"))
        assertArrayEquals(bytes50_3, cache.get("key3"))
    }

    @Test
    fun `clear removes all cached files`() = runTest {
        val cacheDir = tempFolder.newFolder("cache_clear")
        val cache = DiskLruCache(directory = cacheDir, maxSizeBytes = 1024 * 1024)

        cache.put("k1", byteArrayOf(1))
        cache.put("k2", byteArrayOf(2))

        cache.clear()
        assertEquals(0L, cache.size())
        assertNull(cache.get("k1"))
        assertNull(cache.get("k2"))
    }
}

package com.padmakar.imageloader.data.dedup

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class RequestDeduplicatorTest {

    @Test
    fun `concurrent requests for same key execute block only once`() = runTest {
        val dedup = RequestDeduplicator<String>()
        val executionCounter = AtomicInteger(0)

        val task1 = async {
            dedup.execute("url_1") {
                executionCounter.incrementAndGet()
                delay(100)
                "Result_1"
            }
        }

        val task2 = async {
            dedup.execute("url_1") {
                executionCounter.incrementAndGet()
                delay(100)
                "Result_1"
            }
        }

        val result1 = task1.await()
        val result2 = task2.await()

        assertEquals("Result_1", result1)
        assertEquals("Result_1", result2)
        assertEquals(1, executionCounter.get())
    }

    @Test
    fun `different keys execute independently`() = runTest {
        val dedup = RequestDeduplicator<String>()
        val executionCounter = AtomicInteger(0)

        val task1 = async {
            dedup.execute("key_a") {
                executionCounter.incrementAndGet()
                "Result_A"
            }
        }

        val task2 = async {
            dedup.execute("key_b") {
                executionCounter.incrementAndGet()
                "Result_B"
            }
        }

        assertEquals("Result_A", task1.await())
        assertEquals("Result_B", task2.await())
        assertEquals(2, executionCounter.get())
    }
}

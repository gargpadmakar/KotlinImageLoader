package com.padmakar.imageloader.core

import com.padmakar.imageloader.core.model.CachePolicy
import com.padmakar.imageloader.core.model.DataSource
import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageResult
import com.padmakar.imageloader.core.model.ImageSize
import com.padmakar.imageloader.core.model.Priority
import com.padmakar.imageloader.core.transform.CircleCropTransformation
import com.padmakar.imageloader.core.transform.RoundedCornersTransformation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreEngineIntegrationTest {

    @Test
    fun testRequestPriorityAndTransformationsPreserved() {
        val circleCrop = CircleCropTransformation()
        val roundedCorners = RoundedCornersTransformation(16f)

        val request = ImageRequest.Builder("https://example.com/photo.jpg")
            .size(800, 600)
            .priority(Priority.HIGH)
            .crossfade(true, 400)
            .retry(attempts = 3, delayMillis = 150L)
            .transform(circleCrop)
            .transform(roundedCorners)
            .build()

        assertEquals(Priority.HIGH, request.priority)
        assertTrue(request.crossfade)
        assertEquals(400, request.crossfadeDurationMillis)
        assertEquals(3, request.retryAttempts)
        assertEquals(150L, request.retryDelayMillis)
        assertEquals(2, request.transformations.size)
        assertEquals(ImageSize.Exact(800, 600), request.size)
        assertEquals(CachePolicy.ENABLED, request.memoryCachePolicy)
        assertEquals(CachePolicy.ENABLED, request.diskCachePolicy)
    }

    @Test
    fun testCancellationPropagation() = runBlocking {
        var completed = false
        val job: Job = launch {
            try {
                delay(2000L)
                completed = true
            } catch (ce: CancellationException) {
                // successfully cancelled
                throw ce
            }
        }

        delay(50L)
        job.cancelAndJoin()
        assertTrue("Job must be cancelled before completing", !completed)
    }
}

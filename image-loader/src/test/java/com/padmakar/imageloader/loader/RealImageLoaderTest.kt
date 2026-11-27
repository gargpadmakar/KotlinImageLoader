package com.padmakar.imageloader.loader

import com.padmakar.imageloader.core.model.DataSource
import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageResult
import com.padmakar.imageloader.domain.repository.ImageRepository
import com.padmakar.imageloader.domain.usecase.LoadImageUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RealImageLoaderTest {

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun `execute runs off thread and returns Success from repository`() = runTest(testDispatcher) {
        val expectedImage = "test-bitmap"
        val repository = object : ImageRepository {
            override suspend fun loadImage(request: ImageRequest): ImageResult {
                return ImageResult.Success(expectedImage, DataSource.MEMORY, request)
            }
        }

        val loader = RealImageLoader(
            useCase = LoadImageUseCase(repository),
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher
        )

        val request = ImageRequest(data = "https://example.com/test.jpg")
        val result = loader.execute(request)

        assertTrue(result is ImageResult.Success)
        assertEquals(expectedImage, (result as ImageResult.Success).image)
    }

    @Test
    fun `enqueue returns Disposable that cancels the in flight task`() = runTest(testDispatcher) {
        var completed = false
        val repository = object : ImageRepository {
            override suspend fun loadImage(request: ImageRequest): ImageResult {
                delay(1000)
                completed = true
                return ImageResult.Success("done", DataSource.MEMORY, request)
            }
        }

        val loader = RealImageLoader(
            useCase = LoadImageUseCase(repository),
            ioDispatcher = testDispatcher,
            mainDispatcher = testDispatcher
        )

        val disposable = loader.enqueue(ImageRequest(data = "https://example.com/test.jpg"))
        disposable.dispose()
        advanceUntilIdle()

        assertTrue(disposable.isDisposed)
        assertEquals(false, completed)
    }
}

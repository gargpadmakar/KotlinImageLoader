package com.padmakar.imageloader.mvi

import com.padmakar.imageloader.core.model.DataSource
import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageResult
import com.padmakar.imageloader.domain.repository.ImageRepository
import com.padmakar.imageloader.domain.usecase.LoadImageUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ImageStoreTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val testRequest = ImageRequest(data = "https://example.com/sample.png")

    @Test
    fun `initial state is Idle`() = testScope.runTest {
        val dummyRepo = object : ImageRepository {
            override suspend fun loadImage(request: ImageRequest): ImageResult =
                ImageResult.Success("data", DataSource.MEMORY, request)
        }
        val useCase = LoadImageUseCase(dummyRepo)
        val store = DefaultImageStore(
            loadImageUseCase = useCase,
            coroutineScope = testScope,
            dispatcher = testDispatcher
        )

        assertEquals(ImageState.Idle, store.state.value)
    }

    @Test
    fun `dispatch Load transitions to Success on successful execution`() = testScope.runTest {
        val expectedImage = "decoded-test-image"
        val repository = object : ImageRepository {
            override suspend fun loadImage(request: ImageRequest): ImageResult {
                return ImageResult.Success(expectedImage, DataSource.NETWORK, request)
            }
        }
        val useCase = LoadImageUseCase(repository)
        val store = DefaultImageStore(
            loadImageUseCase = useCase,
            coroutineScope = testScope,
            dispatcher = testDispatcher
        )

        store.dispatch(ImageIntent.Load(testRequest))
        advanceUntilIdle()

        val currentState = store.state.value
        assertTrue(currentState is ImageState.Success)
        val success = currentState as ImageState.Success
        assertEquals(expectedImage, success.result.image)
        assertEquals(DataSource.NETWORK, success.result.dataSource)
    }

    @Test
    fun `dispatch Load transitions to Error on failure`() = testScope.runTest {
        val expectedException = RuntimeException("Connection timed out")
        val repository = object : ImageRepository {
            override suspend fun loadImage(request: ImageRequest): ImageResult {
                return ImageResult.Error(expectedException, request)
            }
        }
        val useCase = LoadImageUseCase(repository)
        val store = DefaultImageStore(
            loadImageUseCase = useCase,
            coroutineScope = testScope,
            dispatcher = testDispatcher
        )

        store.dispatch(ImageIntent.Load(testRequest))
        advanceUntilIdle()

        val currentState = store.state.value
        assertTrue(currentState is ImageState.Error)
        val errorState = currentState as ImageState.Error
        assertEquals(expectedException, errorState.error.throwable)
    }

    @Test
    fun `dispatch Cancel resets active state back to Idle`() = testScope.runTest {
        val repository = object : ImageRepository {
            override suspend fun loadImage(request: ImageRequest): ImageResult {
                kotlinx.coroutines.delay(1000)
                return ImageResult.Success("data", DataSource.MEMORY, request)
            }
        }
        val useCase = LoadImageUseCase(repository)
        val store = DefaultImageStore(
            loadImageUseCase = useCase,
            coroutineScope = testScope,
            dispatcher = testDispatcher
        )

        store.dispatch(ImageIntent.Load(testRequest))
        // Before delay finishes, cancel
        store.dispatch(ImageIntent.Cancel)
        advanceUntilIdle()

        assertEquals(ImageState.Idle, store.state.value)
    }
}

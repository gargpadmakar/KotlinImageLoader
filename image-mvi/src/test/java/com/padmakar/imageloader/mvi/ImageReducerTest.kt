package com.padmakar.imageloader.mvi

import com.padmakar.imageloader.core.model.DataSource
import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ImageReducerTest {

    private lateinit var reducer: ImageReducer
    private val testRequest = ImageRequest(data = "https://example.com/test.jpg")
    private val successResult = ImageResult.Success(
        image = Any(),
        dataSource = DataSource.NETWORK,
        request = testRequest
    )
    private val errorResult = ImageResult.Error(
        throwable = IllegalStateException("Test error"),
        request = testRequest
    )

    @Before
    fun setUp() {
        reducer = DefaultImageReducer()
    }

    @Test
    fun `reduce StartLoading transitions from Idle to Loading`() {
        val initial = ImageState.Idle
        val action = ImageAction.StartLoading(testRequest)

        val nextState = reducer.reduce(initial, action)

        assertTrue(nextState is ImageState.Loading)
        val loadingState = nextState as ImageState.Loading
        assertEquals(testRequest, loadingState.request)
        assertEquals(null, loadingState.progress)
    }

    @Test
    fun `reduce StartLoading transitions from Error to Loading`() {
        val initial = ImageState.Error(errorResult)
        val action = ImageAction.StartLoading(testRequest)

        val nextState = reducer.reduce(initial, action)

        assertTrue(nextState is ImageState.Loading)
        assertEquals(testRequest, (nextState as ImageState.Loading).request)
    }

    @Test
    fun `reduce StartLoading transitions from Success to Loading`() {
        val initial = ImageState.Success(successResult)
        val action = ImageAction.StartLoading(testRequest)

        val nextState = reducer.reduce(initial, action)

        assertTrue(nextState is ImageState.Loading)
        assertEquals(testRequest, (nextState as ImageState.Loading).request)
    }

    @Test
    fun `reduce LoadingProgress updates progress when in Loading state`() {
        val initial = ImageState.Loading(testRequest, progress = 0.2f)
        val action = ImageAction.LoadingProgress(progress = 0.8f)

        val nextState = reducer.reduce(initial, action)

        assertTrue(nextState is ImageState.Loading)
        assertEquals(0.8f, (nextState as ImageState.Loading).progress)
    }

    @Test
    fun `reduce LoadingProgress does not change state when in Idle or Success`() {
        val initial = ImageState.Idle
        val action = ImageAction.LoadingProgress(progress = 0.5f)

        val nextState = reducer.reduce(initial, action)

        assertEquals(ImageState.Idle, nextState)
    }

    @Test
    fun `reduce LoadingSuccess transitions from Loading to Success`() {
        val initial = ImageState.Loading(testRequest)
        val action = ImageAction.LoadingSuccess(successResult)

        val nextState = reducer.reduce(initial, action)

        assertTrue(nextState is ImageState.Success)
        val successState = nextState as ImageState.Success
        assertEquals(successResult, successState.result)
    }

    @Test
    fun `reduce LoadingFailure transitions from Loading to Error`() {
        val initial = ImageState.Loading(testRequest)
        val action = ImageAction.LoadingFailure(errorResult)

        val nextState = reducer.reduce(initial, action)

        assertTrue(nextState is ImageState.Error)
        val errorState = nextState as ImageState.Error
        assertEquals(errorResult, errorState.error)
    }

    @Test
    fun `reduce Reset transitions to Idle from any state`() {
        val loadingState = ImageState.Loading(testRequest)
        val successState = ImageState.Success(successResult)
        val errorState = ImageState.Error(errorResult)

        assertEquals(ImageState.Idle, reducer.reduce(loadingState, ImageAction.Reset))
        assertEquals(ImageState.Idle, reducer.reduce(successState, ImageAction.Reset))
        assertEquals(ImageState.Idle, reducer.reduce(errorState, ImageAction.Reset))
    }
}

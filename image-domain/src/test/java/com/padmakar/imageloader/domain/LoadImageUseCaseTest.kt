package com.padmakar.imageloader.domain

import com.padmakar.imageloader.core.model.DataSource
import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageResult
import com.padmakar.imageloader.domain.policy.DefaultImageRequestValidator
import com.padmakar.imageloader.domain.repository.ImageRepository
import com.padmakar.imageloader.domain.usecase.LoadImageUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadImageUseCaseTest {

    @Test
    fun `use case delegates execution to repository on valid request`() = runBlocking {
        val testRequest = ImageRequest(data = "https://example.com/image.png")
        val expectedResult = ImageResult.Success(
            image = "test-bitmap",
            dataSource = DataSource.MEMORY,
            request = testRequest
        )

        val repository = object : ImageRepository {
            override suspend fun loadImage(request: ImageRequest): ImageResult = expectedResult
        }

        val useCase = LoadImageUseCase(
            repository = repository,
            validator = DefaultImageRequestValidator()
        )

        val result = useCase(testRequest)
        assertEquals(expectedResult, result)
    }

    @Test
    fun `use case rejects request when validator fails with null data`() = runBlocking {
        val invalidRequest = ImageRequest(data = null)

        val repository = object : ImageRepository {
            override suspend fun loadImage(request: ImageRequest): ImageResult {
                throw AssertionError("Repository should not be called when validation fails")
            }
        }

        val useCase = LoadImageUseCase(
            repository = repository,
            validator = DefaultImageRequestValidator()
        )

        val result = useCase(invalidRequest)
        assertTrue(result is ImageResult.Error)
        val error = result as ImageResult.Error
        assertTrue(error.throwable is IllegalArgumentException)
        assertTrue(error.throwable.message!!.contains("cannot be null"))
    }
}

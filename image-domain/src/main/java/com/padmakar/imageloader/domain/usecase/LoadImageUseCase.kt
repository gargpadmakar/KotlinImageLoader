package com.padmakar.imageloader.domain.usecase

import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageResult
import com.padmakar.imageloader.domain.policy.ImageRequestValidator
import com.padmakar.imageloader.domain.policy.ValidationResult
import com.padmakar.imageloader.domain.repository.ImageRepository

/**
 * UseCase encapsulating the business rules for requesting and loading an image.
 */
class LoadImageUseCase(
    private val repository: ImageRepository,
    private val validator: ImageRequestValidator? = null
) {
    /**
     * Executes the use case: validates the request if a validator is supplied,
     * then coordinates with [ImageRepository].
     */
    suspend operator fun invoke(request: ImageRequest): ImageResult {
        if (validator != null) {
            when (val validation = validator.validate(request)) {
                is ValidationResult.Invalid -> {
                    return ImageResult.Error(
                        throwable = IllegalArgumentException("Invalid ImageRequest: ${validation.reason}"),
                        request = request
                    )
                }
                ValidationResult.Valid -> {
                    // Passed validation
                }
            }
        }
        return repository.loadImage(request)
    }

    /**
     * Executes the usecase streaming progressive frames.
     */
    fun flow(request: ImageRequest): kotlinx.coroutines.flow.Flow<ImageResult> {
        if (validator != null) {
            when (val validation = validator.validate(request)) {
                is ValidationResult.Invalid -> {
                    return kotlinx.coroutines.flow.flowOf(
                        ImageResult.Error(
                            throwable = IllegalArgumentException("Invalid ImageRequest: ${validation.reason}"),
                            request = request
                        )
                    )
                }
                ValidationResult.Valid -> {}
            }
        }
        return repository.streamImage(request)
    }
}

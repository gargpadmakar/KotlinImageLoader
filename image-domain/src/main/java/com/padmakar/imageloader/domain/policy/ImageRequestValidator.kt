package com.padmakar.imageloader.domain.policy

import com.padmakar.imageloader.core.model.ImageRequest

/**
 * Outcome of an [ImageRequest] policy validation.
 */
sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val reason: String) : ValidationResult
}

/**
 * Policy contract for validating domain invariants of an [ImageRequest].
 */
interface ImageRequestValidator {
    fun validate(request: ImageRequest): ValidationResult
}

/**
 * Default domain validator ensuring non-null data payload.
 */
class DefaultImageRequestValidator : ImageRequestValidator {
    override fun validate(request: ImageRequest): ValidationResult {
        val data = request.data ?: return ValidationResult.Invalid("ImageRequest data cannot be null")
        if (data is String && data.isBlank()) {
            return ValidationResult.Invalid("ImageRequest data URL/URI cannot be blank")
        }
        return ValidationResult.Valid
    }
}

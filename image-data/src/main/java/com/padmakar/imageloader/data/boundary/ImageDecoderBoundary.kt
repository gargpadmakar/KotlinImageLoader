package com.padmakar.imageloader.data.boundary

import com.padmakar.imageloader.core.model.ImageRequest

/**
 * Architectural boundary for decoding raw source payloads into visual bitmap/drawable formats.
 * Actual decoding engines (BitmapFactory, ImageDecoder, etc.) are implemented in subsequent phases.
 */
interface ImageDecoderBoundary {
    suspend fun decode(source: Any, request: ImageRequest): Any
}

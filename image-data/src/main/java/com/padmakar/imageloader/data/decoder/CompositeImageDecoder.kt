package com.padmakar.imageloader.data.decoder

import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.data.boundary.ImageDecoderBoundary
import java.io.File

/**
 * Composite Decoder that automatically inspects raw byte signatures and routes to
 * the optimal decoder ([SvgDecoder], [AnimatedDecoder], or standard downsampling [BitmapDecoder]).
 */
class CompositeImageDecoder(
    private val bitmapDecoder: ImageDecoderBoundary = BitmapDecoder(),
    private val animatedDecoder: ImageDecoderBoundary = AnimatedDecoder(bitmapDecoder),
    private val svgDecoder: ImageDecoderBoundary = SvgDecoder()
) : ImageDecoderBoundary {

    override suspend fun decode(source: Any, request: ImageRequest): Any {
        val bytes = when (source) {
            is ByteArray -> source
            is File -> source.readBytes()
            else -> throw IllegalArgumentException("Expected ByteArray or File")
        }

        val format = ImageFormat.detect(bytes)

        return when (format) {
            ImageFormat.SVG -> svgDecoder.decode(bytes, request)
            ImageFormat.GIF, ImageFormat.ANIMATED_WEBP -> animatedDecoder.decode(bytes, request)
            ImageFormat.STATIC_WEBP, ImageFormat.JPEG, ImageFormat.PNG, ImageFormat.UNKNOWN -> {
                bitmapDecoder.decode(bytes, request)
            }
        }
    }

    override suspend fun decodePartial(source: ByteArray, request: ImageRequest): Any? {
        val format = ImageFormat.detect(source)
        if (format == ImageFormat.SVG || format == ImageFormat.GIF) {
            return null // Vector & GIF don't support partial raster downsampling
        }
        return bitmapDecoder.decodePartial(source, request)
    }
}

package com.padmakar.imageloader.data.decoder

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageSize
import com.padmakar.imageloader.data.boundary.ImageDecoderBoundary
import java.io.File
import java.nio.ByteBuffer

/**
 * Animated Image Decoder supporting Animated WebP and GIF.
 * Uses Android 9+ (API 28+) [ImageDecoder] and [AnimatedImageDrawable] with graceful fallback
 * to standard static first-frame decoding on older API levels.
 */
class AnimatedDecoder(
    private val fallbackDecoder: ImageDecoderBoundary = BitmapDecoder()
) : ImageDecoderBoundary {

    override suspend fun decode(source: Any, request: ImageRequest): Any {
        val bytes = when (source) {
            is ByteArray -> source
            is File -> source.readBytes()
            else -> throw IllegalArgumentException("Expected ByteArray or File for Animated image")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val buffer = ByteBuffer.wrap(bytes)
                val decoderSource = ImageDecoder.createSource(buffer)

                val drawable = ImageDecoder.decodeDrawable(decoderSource) { decoder, info, _ ->
                    when (val size = request.size) {
                        is ImageSize.Exact -> {
                            decoder.setTargetSize(size.width, size.height)
                        }
                        else -> {}
                    }
                }

                if (drawable is AnimatedImageDrawable) {
                    drawable.start()
                    return drawable
                }

                return drawable
            } catch (_: Throwable) {
                // Fallback to static bitmap decode if platform animated decode fails
            }
        }

        // Fallback for API < 28 or non-animated files
        return fallbackDecoder.decode(bytes, request)
    }

    override suspend fun decodePartial(source: ByteArray, request: ImageRequest): Any? {
        return fallbackDecoder.decodePartial(source, request)
    }
}

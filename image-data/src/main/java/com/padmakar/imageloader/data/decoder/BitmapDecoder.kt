package com.padmakar.imageloader.data.decoder

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageSize
import com.padmakar.imageloader.data.boundary.ImageDecoderBoundary
import java.io.File

/**
 * Bitmap decoder with memory-safe downsampling.
 *
 * Capabilities:
 * - Operates on safe, re-readable byte arrays without reading single-use network streams twice.
 * - Inspects image dimensions without loading full pixels into memory (pass 1).
 * - Downsamples images via [BitmapFactory.Options.inSampleSize] based on [ImageSize.Exact].
 * - Imposes a maximum dimension boundary ([MAX_BITMAP_DIMENSION]) even on [ImageSize.Original] to protect against OOM crashes.
 * - Rejects corrupt, empty, or un-decodable data.
 *
 * Note on EXIF orientation:
 * EXIF orientation metadata tags (such as camera JPEG rotation tags) are not parsed or transformed
 * in Phase 2A; images will be rendered in their intrinsic decoded orientation.
 */
class BitmapDecoder(
    private val maxDimension: Int = MAX_BITMAP_DIMENSION
) : ImageDecoderBoundary {

    companion object {
        const val MAX_BITMAP_DIMENSION = 4096

        /**
         * Computes the largest power-of-2 inSampleSize value that keeps both raw dimensions
         * larger than or equal to the requested dimensions.
         */
        fun calculateInSampleSize(
            rawWidth: Int,
            rawHeight: Int,
            reqWidth: Int,
            reqHeight: Int,
            maxAllowedDimension: Int = MAX_BITMAP_DIMENSION
        ): Int {
            var inSampleSize = 1

            if (rawHeight > reqHeight || rawWidth > reqWidth) {
                val halfHeight = rawHeight / 2
                val halfWidth = rawWidth / 2

                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            // Ensure dimensions do not exceed the absolute max dimension safety ceiling
            while ((rawWidth / inSampleSize) > maxAllowedDimension || (rawHeight / inSampleSize) > maxAllowedDimension) {
                inSampleSize *= 2
            }

            return inSampleSize.coerceAtLeast(1)
        }
    }

    override suspend fun decode(source: Any, request: ImageRequest): Any {
        val bytes = when (source) {
            is ByteArray -> source
            is File -> source.readBytes()
            else -> throw IllegalArgumentException("Unsupported source type: ${source::class.java.name}. Expected ByteArray or File.")
        }

        if (bytes.isEmpty()) {
            throw IllegalArgumentException("Cannot decode empty byte array.")
        }

        // Pass 1: Decode bounds only
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

        val rawWidth = boundsOptions.outWidth
        val rawHeight = boundsOptions.outHeight

        if (rawWidth <= 0 || rawHeight <= 0) {
            throw IllegalArgumentException(
                "Corrupted or unsupported image data: invalid intrinsic dimensions (${rawWidth}x${rawHeight})"
            )
        }

        // Calculate sample size
        val sampleSize = when (val size = request.size) {
            is ImageSize.Exact -> {
                calculateInSampleSize(
                    rawWidth = rawWidth,
                    rawHeight = rawHeight,
                    reqWidth = size.width,
                    reqHeight = size.height,
                    maxAllowedDimension = maxDimension
                )
            }
            ImageSize.Original, ImageSize.Unspecified -> {
                var sizeMultiplier = 1
                while ((rawWidth / sizeMultiplier) > maxDimension || (rawHeight / sizeMultiplier) > maxDimension) {
                    sizeMultiplier *= 2
                }
                sizeMultiplier
            }
        }

        // Pass 2: Actual bitmap decoding
        val decodeOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            ?: throw IllegalStateException(
                "BitmapFactory failed to decode byte array of length ${bytes.size} with inSampleSize=$sampleSize"
            )

        return bitmap
    }

    override suspend fun decodePartial(source: ByteArray, request: ImageRequest): Any? {
        if (source.size < 4096) return null
        return try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(source, 0, source.size, boundsOptions)
            val w = boundsOptions.outWidth
            val h = boundsOptions.outHeight
            if (w <= 0 || h <= 0) return null

            val sample = when (val size = request.size) {
                is ImageSize.Exact -> calculateInSampleSize(w, h, size.width, size.height, maxDimension) * 2
                else -> 4
            }.coerceAtLeast(2)

            val decodeOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeByteArray(source, 0, source.size, decodeOptions)
        } catch (_: Throwable) {
            null
        }
    }
}

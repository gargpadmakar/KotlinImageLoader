package com.padmakar.imageloader.data.decoder

import android.graphics.Bitmap
import android.graphics.Matrix
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Handles EXIF orientation parsing and matrix transformations for JPEG images.
 *
 * Supported EXIF orientation tags:
 * 1 = [ORIENTATION_NORMAL]: 0 degrees (default)
 * 2 = [ORIENTATION_FLIP_HORIZONTAL]: Horizontal mirror
 * 3 = [ORIENTATION_ROTATE_180]: 180 degrees rotation
 * 4 = [ORIENTATION_FLIP_VERTICAL]: Vertical mirror
 * 5 = [ORIENTATION_TRANSPOSE]: 90 degrees rotation + horizontal mirror
 * 6 = [ORIENTATION_ROTATE_90]: 90 degrees clockwise rotation
 * 7 = [ORIENTATION_TRANSVERSE]: 270 degrees rotation + horizontal mirror
 * 8 = [ORIENTATION_ROTATE_270]: 270 degrees clockwise rotation
 */
object ExifOrientationHandler {

    const val ORIENTATION_UNDEFINED = 0
    const val ORIENTATION_NORMAL = 1
    const val ORIENTATION_FLIP_HORIZONTAL = 2
    const val ORIENTATION_ROTATE_180 = 3
    const val ORIENTATION_FLIP_VERTICAL = 4
    const val ORIENTATION_TRANSPOSE = 5
    const val ORIENTATION_ROTATE_90 = 6
    const val ORIENTATION_TRANSVERSE = 7
    const val ORIENTATION_ROTATE_270 = 8

    /**
     * Extracts EXIF orientation from raw bytes.
     * Works in pure Kotlin on both JVM and Android without platform dependencies,
     * with secondary fallback to [android.media.ExifInterface] when available.
     */
    fun getExifOrientation(bytes: ByteArray): Int {
        if (bytes.size < 12) return ORIENTATION_NORMAL

        // Verify JPEG SOI marker (0xFF, 0xD8)
        if (bytes[0] != 0xFF.toByte() || bytes[1] != 0xD8.toByte()) {
            return ORIENTATION_NORMAL
        }

        try {
            // Pure Kotlin JPEG APP1 parser
            val parsed = parseJpegApp1ExifOrientation(bytes)
            if (parsed in 1..8) {
                return parsed
            }
        } catch (_: Throwable) {
            // Fall through to framework fallback
        }

        return try {
            val exifInterface = android.media.ExifInterface(ByteArrayInputStream(bytes))
            exifInterface.getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                ORIENTATION_NORMAL
            )
        } catch (_: Throwable) {
            ORIENTATION_NORMAL
        }
    }

    /**
     * Applies orientation transformation to [bitmap].
     * Avoids any allocations if orientation is 1 (normal) or undefined.
     */
    fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        if (orientation <= ORIENTATION_NORMAL || orientation > ORIENTATION_ROTATE_270) {
            return bitmap
        }

        val matrix = Matrix()
        when (orientation) {
            ORIENTATION_FLIP_HORIZONTAL -> {
                matrix.postScale(-1f, 1f)
            }
            ORIENTATION_ROTATE_180 -> {
                matrix.postRotate(180f)
            }
            ORIENTATION_FLIP_VERTICAL -> {
                matrix.postScale(1f, -1f)
            }
            ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ORIENTATION_ROTATE_90 -> {
                matrix.postRotate(90f)
            }
            ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            ORIENTATION_ROTATE_270 -> {
                matrix.postRotate(270f)
            }
            else -> return bitmap
        }

        return try {
            val transformed = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
            transformed ?: bitmap
        } catch (_: Throwable) {
            bitmap
        }
    }

    private fun parseJpegApp1ExifOrientation(bytes: ByteArray): Int {
        var offset = 2
        val length = bytes.size

        while (offset + 4 <= length) {
            if (bytes[offset] != 0xFF.toByte()) break

            val marker = bytes[offset + 1].toInt() and 0xFF
            offset += 2

            // Standalone markers
            if (marker == 0xD8 || marker == 0xD9 || marker == 0x01) continue

            if (offset + 2 > length) break
            val segmentLength = ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
            offset += 2

            // APP1 marker (0xE1)
            if (marker == 0xE1) {
                if (segmentLength >= 8 && offset + 6 <= length) {
                    val isExif = bytes[offset] == 'E'.code.toByte() &&
                            bytes[offset + 1] == 'x'.code.toByte() &&
                            bytes[offset + 2] == 'i'.code.toByte() &&
                            bytes[offset + 3] == 'f'.code.toByte() &&
                            bytes[offset + 4] == 0.toByte() &&
                            bytes[offset + 5] == 0.toByte()

                    if (isExif) {
                        val tiffHeaderOffset = offset + 6
                        return parseTiffOrientation(bytes, tiffHeaderOffset, segmentLength - 8)
                    }
                }
            }

            offset += segmentLength - 2
        }

        return ORIENTATION_NORMAL
    }

    private fun parseTiffOrientation(bytes: ByteArray, startOffset: Int, length: Int): Int {
        if (startOffset + 8 > bytes.size) return ORIENTATION_NORMAL

        val buffer = ByteBuffer.wrap(bytes, startOffset, length)
        val endianByte1 = bytes[startOffset]
        val endianByte2 = bytes[startOffset + 1]

        val byteOrder = when {
            endianByte1 == 'I'.code.toByte() && endianByte2 == 'I'.code.toByte() -> ByteOrder.LITTLE_ENDIAN
            endianByte1 == 'M'.code.toByte() && endianByte2 == 'M'.code.toByte() -> ByteOrder.BIG_ENDIAN
            else -> return ORIENTATION_NORMAL
        }
        buffer.order(byteOrder)

        val magic = buffer.getShort(startOffset + 2)
        if (magic.toInt() != 0x002A) return ORIENTATION_NORMAL

        val ifd0Offset = buffer.getInt(startOffset + 4)
        var currentOffset = startOffset + ifd0Offset
        if (currentOffset + 2 > startOffset + length) return ORIENTATION_NORMAL

        val tagCount = buffer.getShort(currentOffset).toInt() and 0xFFFF
        currentOffset += 2

        for (i in 0 until tagCount) {
            if (currentOffset + 12 > startOffset + length) break

            val tag = buffer.getShort(currentOffset).toInt() and 0xFFFF
            // 0x0112 is EXIF Orientation tag
            if (tag == 0x0112) {
                val value = buffer.getShort(currentOffset + 8).toInt() and 0xFFFF
                return value
            }
            currentOffset += 12
        }

        return ORIENTATION_NORMAL
    }
}

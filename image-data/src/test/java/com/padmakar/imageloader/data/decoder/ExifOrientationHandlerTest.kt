package com.padmakar.imageloader.data.decoder

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ExifOrientationHandlerTest {

    @Test
    fun testNonJpegReturnsNormalOrientation() {
        val nonJpeg = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val orientation = ExifOrientationHandler.getExifOrientation(nonJpeg)
        assertEquals(ExifOrientationHandler.ORIENTATION_NORMAL, orientation)
    }

    @Test
    fun testEmptyAndShortBytesReturnNormalOrientation() {
        assertEquals(ExifOrientationHandler.ORIENTATION_NORMAL, ExifOrientationHandler.getExifOrientation(ByteArray(0)))
        assertEquals(ExifOrientationHandler.ORIENTATION_NORMAL, ExifOrientationHandler.getExifOrientation(byteArrayOf(0xFF.toByte(), 0xD8.toByte())))
    }

    @Test
    fun testTruncatedJpegReturnsNormalOrientation() {
        val truncated = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE1.toByte(), 0x00, 0x04)
        val orientation = ExifOrientationHandler.getExifOrientation(truncated)
        assertEquals(ExifOrientationHandler.ORIENTATION_NORMAL, orientation)
    }

    @Test
    fun testAllEightExifOrientationsParsing() {
        for (expectedOrientation in 1..8) {
            val jpegBytes = createSyntheticJpegWithOrientation(expectedOrientation)
            val parsedOrientation = ExifOrientationHandler.getExifOrientation(jpegBytes)
            assertEquals("Expected orientation $expectedOrientation", expectedOrientation, parsedOrientation)
        }
    }

    @Test
    fun testMalformedTiffReturnsNormalOrientation() {
        val malformedJpeg = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(),
            0xFF.toByte(), 0xE1.toByte(),
            0x00, 0x0A, // length
            'E'.code.toByte(), 'x'.code.toByte(), 'i'.code.toByte(), 'f'.code.toByte(), 0, 0,
            0x00, 0x00 // corrupt TIFF header
        )
        val orientation = ExifOrientationHandler.getExifOrientation(malformedJpeg)
        assertEquals(ExifOrientationHandler.ORIENTATION_NORMAL, orientation)
    }

    /**
     * Helper synthesizing a valid JPEG SOI + APP1 EXIF segment containing tag 0x0112 (Orientation).
     */
    private fun createSyntheticJpegWithOrientation(orientation: Int): ByteArray {
        val tiffStream = ByteArrayOutputStream()
        val tiffHeader = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).apply {
            put('M'.code.toByte())
            put('M'.code.toByte())
            putShort(0x002A) // TIFF magic
            putInt(8)        // offset to IFD0
        }
        tiffStream.write(tiffHeader.array())

        // IFD0: 1 tag
        val ifd0 = ByteBuffer.allocate(2 + 12 + 4).order(ByteOrder.BIG_ENDIAN).apply {
            putShort(1) // 1 directory entry
            // Tag entry (12 bytes)
            putShort(0x0112.toShort()) // Orientation tag
            putShort(3)                // Type: SHORT
            putInt(1)                  // Count: 1
            putShort(orientation.toShort()) // Value
            putShort(0)                // Padding
            putInt(0)                  // Next IFD offset (0 = none)
        }
        tiffStream.write(ifd0.array())
        val tiffBytes = tiffStream.toByteArray()

        val app1Payload = ByteArrayOutputStream().apply {
            write("Exif".toByteArray(Charsets.US_ASCII))
            write(0)
            write(0)
            write(tiffBytes)
        }.toByteArray()

        val app1Length = app1Payload.size + 2 // includes length bytes

        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte())) // SOI
        out.write(byteArrayOf(0xFF.toByte(), 0xE1.toByte())) // APP1
        out.write((app1Length shr 8) and 0xFF)
        out.write(app1Length and 0xFF)
        out.write(app1Payload)
        out.write(byteArrayOf(0xFF.toByte(), 0xD9.toByte())) // EOI

        return out.toByteArray()
    }
}

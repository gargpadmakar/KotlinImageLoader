package com.padmakar.imageloader.data.decoder

import com.padmakar.imageloader.core.model.ImageRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ImageFormatTest {

    @Test
    fun `detect detects JPEG magic bytes`() {
        val jpegHeader = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0, 0, 0, 0, 0, 0, 0, 0)
        assertEquals(ImageFormat.JPEG, ImageFormat.detect(jpegHeader))
    }

    @Test
    fun `detect detects PNG magic bytes`() {
        val pngHeader = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 0, 0, 0, 0, 0, 0, 0, 0)
        assertEquals(ImageFormat.PNG, ImageFormat.detect(pngHeader))
    }

    @Test
    fun `detect detects GIF magic bytes`() {
        val gifHeader = "GIF89aSampleData".toByteArray()
        assertEquals(ImageFormat.GIF, ImageFormat.detect(gifHeader))
    }

    @Test
    fun `detect detects Static WebP magic bytes`() {
        val webpHeader = "RIFF1234WEBPVP8 ".toByteArray()
        assertEquals(ImageFormat.STATIC_WEBP, ImageFormat.detect(webpHeader))
    }

    @Test
    fun `detect detects SVG xml header`() {
        val svgHeader = "<svg width=\"100\" height=\"100\"><circle r=\"50\"/></svg>".toByteArray()
        assertEquals(ImageFormat.SVG, ImageFormat.detect(svgHeader))
    }

    @Test
    fun `detect returns UNKNOWN for random bytes`() {
        val random = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        assertEquals(ImageFormat.UNKNOWN, ImageFormat.detect(random))
    }
}

class SvgDecoderTest {

    @Test
    fun `decode parses basic svg into bitmap`() = runTest {
        val svgDecoder = SvgDecoder()
        val svgData = """<svg width="60" height="40" xmlns="http://www.w3.org/2000/svg"><rect width="60" height="40"/></svg>""".toByteArray()
        val request = ImageRequest.Builder("test.svg").build()

        val result = svgDecoder.decode(svgData, request)
        assertNotNull(result)
    }
}

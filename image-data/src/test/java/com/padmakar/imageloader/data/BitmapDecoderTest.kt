package com.padmakar.imageloader.data

import com.padmakar.imageloader.data.decoder.BitmapDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BitmapDecoderTest {

    @Test
    fun `calculateInSampleSize returns 1 when requested dimensions equal or exceed raw dimensions`() {
        val sampleSize = BitmapDecoder.calculateInSampleSize(
            rawWidth = 100,
            rawHeight = 100,
            reqWidth = 100,
            reqHeight = 100
        )
        assertEquals(1, sampleSize)

        val largerReq = BitmapDecoder.calculateInSampleSize(
            rawWidth = 100,
            rawHeight = 100,
            reqWidth = 200,
            reqHeight = 200
        )
        assertEquals(1, largerReq)
    }

    @Test
    fun `calculateInSampleSize calculates correct power of two for downsampling`() {
        // 800x600 down to 200x150: half is 400x300 (sampleSize 2), half again is 200x150 (sampleSize 4)
        val sampleSize = BitmapDecoder.calculateInSampleSize(
            rawWidth = 800,
            rawHeight = 600,
            reqWidth = 200,
            reqHeight = 150
        )
        assertEquals(4, sampleSize)
    }

    @Test
    fun `calculateInSampleSize bounds large images to max allowed dimension`() {
        // 10000x10000 with max 4096: must be downsampled at least by 4 so 10000/4 = 2500 <= 4096
        val sampleSize = BitmapDecoder.calculateInSampleSize(
            rawWidth = 10000,
            rawHeight = 10000,
            reqWidth = 10000,
            reqHeight = 10000,
            maxAllowedDimension = 4096
        )
        assertTrue(sampleSize >= 4)
        assertTrue(10000 / sampleSize <= 4096)
    }
}

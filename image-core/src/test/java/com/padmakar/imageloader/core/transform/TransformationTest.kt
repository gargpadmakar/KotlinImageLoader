package com.padmakar.imageloader.core.transform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransformationTest {

    @Test
    fun `CircleCropTransformation cacheKey is consistent`() {
        val transform = CircleCropTransformation()
        assertEquals("CircleCropTransformation", transform.cacheKey)
    }

    @Test
    fun `RoundedCornersTransformation cacheKey reflects radius parameter`() {
        val transform1 = RoundedCornersTransformation(16f)
        val transform2 = RoundedCornersTransformation(32f)

        assertEquals("RoundedCornersTransformation(radius=16.0)", transform1.cacheKey)
        assertEquals("RoundedCornersTransformation(radius=32.0)", transform2.cacheKey)
    }

    @Test
    fun `RotateTransformation cacheKey reflects degrees parameter`() {
        val rotate = RotateTransformation(90f)
        assertEquals("RotateTransformation(degrees=90.0)", rotate.cacheKey)
    }

    @Test
    fun `BlurTransformation cacheKey reflects radius parameter`() {
        val blur = BlurTransformation(15)
        assertEquals("BlurTransformation(radius=15)", blur.cacheKey)
    }
}

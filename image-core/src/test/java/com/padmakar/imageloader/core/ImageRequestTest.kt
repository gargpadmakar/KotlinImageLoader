package com.padmakar.imageloader.core

import com.padmakar.imageloader.core.model.CachePolicy
import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ImageRequestTest {

    @Test
    fun `builder constructs ImageRequest with configured properties`() {
        val request = ImageRequest.Builder("https://test.com/photo.jpg")
            .size(300, 200)
            .memoryCachePolicy(CachePolicy.READ_ONLY)
            .diskCachePolicy(CachePolicy.DISABLED)
            .addHeader("Authorization", "Bearer token123")
            .placeholder(101)
            .error(202)
            .build()

        assertEquals("https://test.com/photo.jpg", request.data)
        assertEquals(ImageSize.Exact(300, 200), request.size)
        assertEquals(CachePolicy.READ_ONLY, request.memoryCachePolicy)
        assertEquals(CachePolicy.DISABLED, request.diskCachePolicy)
        assertEquals("Bearer token123", request.headers["Authorization"])
        assertEquals(101, request.placeholderResId)
        assertEquals(202, request.errorResId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ImageSize Exact throws when dimensions are non-positive`() {
        ImageSize.Exact(width = 0, height = 100)
    }

    @Test
    fun `cache policy helper properties identify read and write permissions`() {
        assertEquals(true, CachePolicy.ENABLED.isReadAllowed)
        assertEquals(true, CachePolicy.ENABLED.isWriteAllowed)

        assertEquals(true, CachePolicy.READ_ONLY.isReadAllowed)
        assertEquals(false, CachePolicy.READ_ONLY.isWriteAllowed)

        assertEquals(false, CachePolicy.WRITE_ONLY.isReadAllowed)
        assertEquals(true, CachePolicy.WRITE_ONLY.isWriteAllowed)

        assertEquals(false, CachePolicy.DISABLED.isReadAllowed)
        assertEquals(false, CachePolicy.DISABLED.isWriteAllowed)
    }
}

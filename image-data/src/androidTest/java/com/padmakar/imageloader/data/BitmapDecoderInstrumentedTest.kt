package com.padmakar.imageloader.data

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageSize
import com.padmakar.imageloader.data.decoder.BitmapDecoder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class BitmapDecoderInstrumentedTest {

    private fun createSamplePngBytes(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }

    @Test
    fun decode_validPngBytes_returnsDecodedBitmap() = runBlocking {
        val bytes = createSamplePngBytes(400, 300)
        val decoder = BitmapDecoder()
        val request = ImageRequest(data = bytes, size = ImageSize.Original)

        val result = decoder.decode(bytes, request)

        assertTrue(result is Bitmap)
        val bitmap = result as Bitmap
        assertEquals(400, bitmap.width)
        assertEquals(300, bitmap.height)
    }

    @Test
    fun decode_withExactTargetSize_downsamplesCorrectly() = runBlocking {
        val bytes = createSamplePngBytes(800, 600)
        val decoder = BitmapDecoder()
        val request = ImageRequest(data = bytes, size = ImageSize.Exact(200, 150))

        val result = decoder.decode(bytes, request)

        assertTrue(result is Bitmap)
        val bitmap = result as Bitmap
        // Downsampled with inSampleSize = 4 (800/4 = 200, 600/4 = 150)
        assertEquals(200, bitmap.width)
        assertEquals(150, bitmap.height)
    }

    @Test(expected = IllegalArgumentException::class)
    fun decode_corruptBytes_throwsException() = runBlocking {
        val corruptBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val decoder = BitmapDecoder()
        val request = ImageRequest(data = corruptBytes)

        decoder.decode(corruptBytes, request)
        Unit
    }
}

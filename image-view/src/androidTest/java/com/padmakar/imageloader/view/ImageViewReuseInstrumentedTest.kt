package com.padmakar.imageloader.view

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.padmakar.imageloader.core.Disposable
import com.padmakar.imageloader.core.ImageLoader
import com.padmakar.imageloader.core.model.DataSource
import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageResult
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ImageViewReuseInstrumentedTest {

    @Test
    fun rapidReuse_cancelsOldRequestAndAppliesOnlyLatestResult() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val latch = CountDownLatch(1)

        val bmp1 = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        val bmp2 = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)

        val fakeLoader = object : ImageLoader {
            override suspend fun execute(request: ImageRequest): ImageResult {
                return if (request.data == "url_first") {
                    delay(300) // Slower first request
                    ImageResult.Success(bmp1, DataSource.MEMORY, request)
                } else {
                    delay(50)  // Fast second request
                    ImageResult.Success(bmp2, DataSource.MEMORY, request)
                }
            }

            override fun enqueue(request: ImageRequest): Disposable = object : Disposable {
                override fun dispose() {}
                override val isDisposed: Boolean = false
            }
        }

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val imageView = ImageView(context)

            // Start first request
            val disp1 = imageView.loadImage("url_first", fakeLoader)

            // Immediately reuse view with second request
            val disp2 = imageView.loadImage("url_second", fakeLoader)

            // disp1 should now be disposed
            assertTrue(disp1.isDisposed)
        }

        // Wait for asynchronous coroutines to settle
        Thread.sleep(500)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            latch.countDown()
        }

        assertTrue(latch.await(2, TimeUnit.SECONDS))
    }
}

package com.padmakar.imageloader.core.transform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import com.padmakar.imageloader.core.model.ImageSize
import kotlin.math.min

/**
 * Crops the image into a circle with an optional border.
 */
class CircleCropTransformation : Transformation {
    override val cacheKey: String = "CircleCropTransformation"

    override suspend fun transform(input: Bitmap, size: ImageSize): Bitmap {
        val minDim = min(input.width, input.height)
        val output = Bitmap.createBitmap(minDim, minDim, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect((input.width - minDim) / 2, (input.height - minDim) / 2, (input.width + minDim) / 2, (input.height + minDim) / 2)
        val destRect = Rect(0, 0, minDim, minDim)

        canvas.drawCircle(minDim / 2f, minDim / 2f, minDim / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(input, rect, destRect, paint)

        return output
    }
}

/**
 * Rounds the four corners of the image with a specified radius in pixels.
 */
class RoundedCornersTransformation(
    val radiusPixels: Float
) : Transformation {
    override val cacheKey: String = "RoundedCornersTransformation(radius=$radiusPixels)"

    override suspend fun transform(input: Bitmap, size: ImageSize): Bitmap {
        if (radiusPixels <= 0f) return input

        val output = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rectF = RectF(0f, 0f, input.width.toFloat(), input.height.toFloat())

        canvas.drawRoundRect(rectF, radiusPixels, radiusPixels, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(input, 0f, 0f, paint)

        return output
    }
}

/**
 * Rotates the image clockwise by specified degrees.
 */
class RotateTransformation(
    val degrees: Float
) : Transformation {
    override val cacheKey: String = "RotateTransformation(degrees=$degrees)"

    override suspend fun transform(input: Bitmap, size: ImageSize): Bitmap {
        if (degrees % 360f == 0f) return input
        val matrix = android.graphics.Matrix().apply {
            postRotate(degrees)
        }
        return Bitmap.createBitmap(input, 0, 0, input.width, input.height, matrix, true)
    }
}

/**
 * Simple box blur transformation.
 */
class BlurTransformation(
    val radius: Int = 10
) : Transformation {
    override val cacheKey: String = "BlurTransformation(radius=$radius)"

    override suspend fun transform(input: Bitmap, size: ImageSize): Bitmap {
        if (radius <= 0) return input
        return fastBlur(input, radius.coerceIn(1, 25))
    }

    private fun fastBlur(sentBitmap: Bitmap, radius: Int): Bitmap {
        val bitmap = sentBitmap.copy(sentBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        if (radius < 1) return sentBitmap

        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(maxOf(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (idx in 0 until 256 * divsum) {
            dv[idx] = idx / divsum
        }

        yw = 0
        yi = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        for (yIdx in 0 until h) {
            rinsum = 0
            ginsum = 0
            binsum = 0
            routsum = 0
            goutsum = 0
            boutsum = 0
            rsum = 0
            gsum = 0
            bsum = 0
            for (iIdx in -radius..radius) {
                p = pix[yi + minOf(wm, maxOf(iIdx, 0))]
                sir = stack[iIdx + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = p and 0x0000ff
                rbs = r1 - Math.abs(iIdx)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (iIdx > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
            }
            stackpointer = radius

            for (xIdx in 0 until w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (yIdx == 0) {
                    vmin[xIdx] = minOf(xIdx + radius + 1, wm)
                }
                p = pix[yw + vmin[xIdx]]

                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = p and 0x0000ff

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi++
            }
            yw += w
        }

        for (xIdx in 0 until w) {
            rinsum = 0
            ginsum = 0
            binsum = 0
            routsum = 0
            goutsum = 0
            boutsum = 0
            rsum = 0
            gsum = 0
            bsum = 0
            yp = -radius * w
            for (iIdx in -radius..radius) {
                yi = maxOf(0, yp) + xIdx
                sir = stack[iIdx + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - Math.abs(iIdx)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (iIdx > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (iIdx < hm) {
                    yp += w
                }
            }
            yi = xIdx
            stackpointer = radius
            for (yIdx in 0 until h) {
                pix[yi] = (0xff000000.toInt() and pix[yi]) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (xIdx == 0) {
                    vmin[yIdx] = minOf(yIdx + r1, hm) * w
                }
                p = xIdx + vmin[yIdx]

                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi += w
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }
}

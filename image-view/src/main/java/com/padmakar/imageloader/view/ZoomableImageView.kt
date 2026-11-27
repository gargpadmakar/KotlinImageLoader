package com.padmakar.imageloader.view

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView

/**
 * Custom Zoomable ImageView supporting pinch-to-zoom, panning, double-tap zoom/reset,
 * and seamless integration with KotlinImageLoader's [loadImage].
 */
class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attrs, defStyleAttr) {

    private val imageMatrixInternal = Matrix()
    private val matrixValues = FloatArray(9)

    private var currentScale = 1.0f
    private val minScale = 1.0f
    private val maxScale = 5.0f

    private val lastTouchPoint = PointF()
    private var isDragging = false

    private val scaleDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector

    init {
        scaleType = ScaleType.MATRIX

        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                val targetScale = currentScale * scaleFactor

                if (targetScale in minScale..maxScale) {
                    currentScale = targetScale
                    imageMatrixInternal.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
                    clampTranslation()
                    imageMatrix = imageMatrixInternal
                }
                return true
            }
        })

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (currentScale > minScale) {
                    resetZoom()
                } else {
                    val targetScale = 2.5f
                    val scaleFactor = targetScale / currentScale
                    currentScale = targetScale
                    imageMatrixInternal.postScale(scaleFactor, scaleFactor, e.x, e.y)
                    clampTranslation()
                    imageMatrix = imageMatrixInternal
                }
                return true
            }
        })
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        post { fitImageToCenter() }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        fitImageToCenter()
    }

    private fun fitImageToCenter() {
        val d = drawable ?: return
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f) return

        val drawableWidth = d.intrinsicWidth.toFloat()
        val drawableHeight = d.intrinsicHeight.toFloat()
        if (drawableWidth <= 0f || drawableHeight <= 0f) return

        imageMatrixInternal.reset()
        val scale = Math.min(viewWidth / drawableWidth, viewHeight / drawableHeight)
        val dx = (viewWidth - drawableWidth * scale) / 2f
        val dy = (viewHeight - drawableHeight * scale) / 2f

        imageMatrixInternal.postScale(scale, scale)
        imageMatrixInternal.postTranslate(dx, dy)
        currentScale = 1.0f
        imageMatrix = imageMatrixInternal
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchPoint.set(event.x, event.y)
                isDragging = true
                parent?.requestDisallowInterceptTouchEvent(currentScale > minScale)
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging && currentScale > minScale && !scaleDetector.isInProgress) {
                    val dx = event.x - lastTouchPoint.x
                    val dy = event.y - lastTouchPoint.y
                    imageMatrixInternal.postTranslate(dx, dy)
                    clampTranslation()
                    imageMatrix = imageMatrixInternal
                    lastTouchPoint.set(event.x, event.y)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    private fun clampTranslation() {
        val d = drawable ?: return
        imageMatrixInternal.getValues(matrixValues)
        val transX = matrixValues[Matrix.MTRANS_X]
        val transY = matrixValues[Matrix.MTRANS_Y]
        val scaleX = matrixValues[Matrix.MSCALE_X]
        val scaleY = matrixValues[Matrix.MSCALE_Y]

        val imageW = d.intrinsicWidth * scaleX
        val imageH = d.intrinsicHeight * scaleY
        val viewW = width.toFloat()
        val viewH = height.toFloat()

        val minTransX = if (imageW > viewW) viewW - imageW else (viewW - imageW) / 2f
        val maxTransX = if (imageW > viewW) 0f else (viewW - imageW) / 2f
        val minTransY = if (imageH > viewH) viewH - imageH else (viewH - imageH) / 2f
        val maxTransY = if (imageH > viewH) 0f else (viewH - imageH) / 2f

        val clampedX = Math.max(minTransX, Math.min(transX, maxTransX))
        val clampedY = Math.max(minTransY, Math.min(transY, maxTransY))

        matrixValues[Matrix.MTRANS_X] = clampedX
        matrixValues[Matrix.MTRANS_Y] = clampedY
        imageMatrixInternal.setValues(matrixValues)
    }

    fun resetZoom() {
        currentScale = minScale
        fitImageToCenter()
    }
}

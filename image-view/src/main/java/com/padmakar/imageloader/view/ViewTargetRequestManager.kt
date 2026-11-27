package com.padmakar.imageloader.view

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import com.padmakar.imageloader.core.Disposable
import com.padmakar.imageloader.core.ImageLoader
import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageResult
import com.padmakar.imageloader.core.model.ImageSize
import com.padmakar.imageloader.loader.SingletonImageLoader
import com.padmakar.imageloader.view.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Manages request lifecycles, target size measurement, request identity verification,
 * and view reuse/detachment on [ImageView]s.
 */
internal object ViewTargetRequestManager {

    private val TAG_REQUEST_ID = R.id.tag_image_loader_request_id
    private val TAG_DISPOSABLE = R.id.tag_image_loader_disposable
    private val TAG_ATTACH_LISTENER = R.id.tag_image_loader_attach_listener

    fun loadImage(
        imageView: ImageView,
        request: ImageRequest,
        loader: ImageLoader
    ): Disposable {
        // Cancel any active request on this ImageView
        val oldDisposable = imageView.getTag(TAG_DISPOSABLE) as? Disposable
        oldDisposable?.dispose()

        // Generate unique request identity
        val requestId = UUID.randomUUID().toString()
        imageView.setTag(TAG_REQUEST_ID, requestId)

        // Apply placeholder immediately if configured, otherwise clear current drawable
        val placeholder = request.placeholderResId
        if (placeholder != null) {
            imageView.setImageResource(placeholder)
        } else {
            imageView.setImageDrawable(null)
        }

        // Active request handle
        var requestJob: Job? = null
        var preDrawListener: ViewTreeObserver.OnPreDrawListener? = null

        fun cleanUp() {
            preDrawListener?.let {
                imageView.viewTreeObserver.removeOnPreDrawListener(it)
                preDrawListener = null
            }
            requestJob?.cancel()
            requestJob = null
        }

        val disposable = object : Disposable {
            private var disposed = false
            override fun dispose() {
                if (!disposed) {
                    disposed = true
                    cleanUp()
                    if (imageView.getTag(TAG_REQUEST_ID) == requestId) {
                        imageView.setTag(TAG_REQUEST_ID, null)
                    }
                    if (imageView.getTag(TAG_DISPOSABLE) == this) {
                        imageView.setTag(TAG_DISPOSABLE, null)
                    }
                }
            }

            override val isDisposed: Boolean
                get() = disposed || (requestJob?.isCancelled == true)
        }

        imageView.setTag(TAG_DISPOSABLE, disposable)

        // Ensure attach state listener is installed
        ensureAttachListener(imageView)

        // Resolve size and launch
        resolveSizeAndExecute(
            imageView = imageView,
            request = request,
            requestId = requestId,
            loader = loader,
            onJobCreated = { job -> requestJob = job },
            onPreDrawCreated = { listener -> preDrawListener = listener }
        )

        return disposable
    }

    private fun resolveSizeAndExecute(
        imageView: ImageView,
        request: ImageRequest,
        requestId: String,
        loader: ImageLoader,
        onJobCreated: (Job) -> Unit,
        onPreDrawCreated: (ViewTreeObserver.OnPreDrawListener) -> Unit
    ) {
        // If explicit size is specified, respect it directly
        when (val explicitSize = request.size) {
            is ImageSize.Exact, ImageSize.Original -> {
                executeRequest(imageView, request, requestId, loader, onJobCreated)
                return
            }
            ImageSize.Unspecified -> {
                // Measure target size
            }
        }

        // Check current measured dimensions
        val measuredWidth = imageView.width
        val measuredHeight = imageView.height

        if (measuredWidth > 0 && measuredHeight > 0) {
            val measuredRequest = request.copy(size = ImageSize.Exact(measuredWidth, measuredHeight))
            executeRequest(imageView, measuredRequest, requestId, loader, onJobCreated)
            return
        }

        // Check layout parameters
        val lp = imageView.layoutParams
        if (lp != null && lp.width > 0 && lp.height > 0) {
            val exactRequest = request.copy(size = ImageSize.Exact(lp.width, lp.height))
            executeRequest(imageView, exactRequest, requestId, loader, onJobCreated)
            return
        }

        // Check for wrap_content on both: fallback safely to display metrics to avoid deadlock
        if (lp != null && (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT || lp.height == ViewGroup.LayoutParams.WRAP_CONTENT)) {
            val dm = imageView.resources.displayMetrics
            val safeWidth = if (lp.width > 0) lp.width else dm.widthPixels
            val safeHeight = if (lp.height > 0) lp.height else dm.heightPixels
            val safeRequest = request.copy(size = ImageSize.Exact(safeWidth, safeHeight))
            executeRequest(imageView, safeRequest, requestId, loader, onJobCreated)
            return
        }

        // View is waiting for layout pass; hook into PreDraw safely
        val observer = imageView.viewTreeObserver
        var listener: ViewTreeObserver.OnPreDrawListener? = null

        listener = ViewTreeObserver.OnPreDrawListener {
            imageView.viewTreeObserver.removeOnPreDrawListener(listener)

            // Verify identity
            if (imageView.getTag(TAG_REQUEST_ID) != requestId) {
                return@OnPreDrawListener true
            }

            val w = if (imageView.width > 0) imageView.width else imageView.resources.displayMetrics.widthPixels
            val h = if (imageView.height > 0) imageView.height else imageView.resources.displayMetrics.heightPixels
            val resolvedRequest = request.copy(size = ImageSize.Exact(w, h))

            executeRequest(imageView, resolvedRequest, requestId, loader, onJobCreated)
            true
        }

        observer.addOnPreDrawListener(listener)
        onPreDrawCreated(listener)
    }

    private fun executeRequest(
        imageView: ImageView,
        request: ImageRequest,
        requestId: String,
        loader: ImageLoader,
        onJobCreated: (Job) -> Unit
    ) {
        val job = CoroutineScope(Dispatchers.Main.immediate).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    loader.execute(request)
                }

                // Verify request identity before updating view:
                // If view was reused or request cancelled, discard result!
                if (imageView.getTag(TAG_REQUEST_ID) == requestId) {
                    when (result) {
                        is ImageResult.Success -> {
                            val bitmap = result.image as? Bitmap
                            if (bitmap != null) {
                                imageView.setImageBitmap(bitmap)
                            }
                        }
                        is ImageResult.Error -> {
                            val errorResId = request.errorResId
                            if (errorResId != null) {
                                imageView.setImageResource(errorResId)
                            }
                        }
                    }
                }
            } catch (_: CancellationException) {
                // Request cancelled normally
            }
        }
        onJobCreated(job)
    }

    private fun ensureAttachListener(imageView: ImageView) {
        if (imageView.getTag(TAG_ATTACH_LISTENER) == null) {
            val listener = object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    // Re-attached: request can resume or be managed if needed
                }

                override fun onViewDetachedFromWindow(v: View) {
                    // View detached: cancel active request to free resources
                    val activeDisposable = v.getTag(TAG_DISPOSABLE) as? Disposable
                    activeDisposable?.dispose()
                }
            }
            imageView.addOnAttachStateChangeListener(listener)
            imageView.setTag(TAG_ATTACH_LISTENER, listener)
        }
    }
}

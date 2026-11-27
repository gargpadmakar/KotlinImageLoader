package com.padmakar.imageloader.view

import android.widget.ImageView
import com.padmakar.imageloader.core.Disposable
import com.padmakar.imageloader.core.ImageLoader
import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.loader.SingletonImageLoader

/**
 * Loads an image into an [ImageView] using the specified [request] and optional [imageLoader].
 *
 * Guarantees:
 * - Cancels any previous in-flight request on this [ImageView].
 * - Measures the target [ImageView] dimensions when no explicit [ImageRequest.size] is provided.
 * - Handles wrap_content, zero, and unbounded dimensions safely without deadlocks.
 * - Verifies request identity before applying placeholders, success bitmaps, or error resources,
 *   completely preventing stale images during fast view recycling (e.g. in RecyclerView).
 * - Cancels active work on view detachment.
 */
fun ImageView.loadImage(
    request: ImageRequest,
    imageLoader: ImageLoader? = null
): Disposable {
    val loader = imageLoader ?: SingletonImageLoader.get(context)
    return ViewTargetRequestManager.loadImage(this, request, loader)
}

/**
 * Fluent builder extension to load an image into an [ImageView].
 */
fun ImageView.loadImage(
    data: Any?,
    imageLoader: ImageLoader? = null,
    builder: ImageRequest.Builder.() -> Unit = {}
): Disposable {
    val request = ImageRequest.Builder(data).apply(builder).build()
    return loadImage(request, imageLoader)
}

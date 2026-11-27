package com.padmakar.imageloader.view

import android.widget.ImageView
import com.padmakar.imageloader.core.Disposable
import com.padmakar.imageloader.core.model.ImageRequest

/**
 * Extension methods providing the entry point for traditional Android View image loading.
 */
fun ImageView.loadImage(
    request: ImageRequest
): Disposable {
    // Foundation API contract for Phase 1.
    // In Phase 2, this initiates request dispatch through ImageLoader / ImageStore.
    return object : Disposable {
        private var disposed = false
        override fun dispose() {
            disposed = true
        }
        override val isDisposed: Boolean
            get() = disposed
    }
}

/**
 * Fluent extension method for [ImageView] with builder block.
 */
fun ImageView.loadImage(
    data: Any?,
    builder: ImageRequest.Builder.() -> Unit = {}
): Disposable {
    val request = ImageRequest.Builder(data).apply(builder).build()
    return loadImage(request)
}

package com.padmakar.imageloader.view

import android.view.View
import android.widget.ImageView

/**
 * Contract representing an Android View target that can receive image payloads.
 */
interface ViewTarget<T : View> {
    val view: T
}

/**
 * View target specialization for standard Android [ImageView].
 */
class ImageViewTarget(override val view: ImageView) : ViewTarget<ImageView>

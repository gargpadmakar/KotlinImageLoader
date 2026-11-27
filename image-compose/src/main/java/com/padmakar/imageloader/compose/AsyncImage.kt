package com.padmakar.imageloader.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.mvi.ImageState

/**
 * Foundation API boundary for rendering asynchronous images in Jetpack Compose.
 *
 * @param model Data model to load (e.g. String URL, Uri, or [ImageRequest]).
 * @param contentDescription Accessibility description for the image.
 * @param modifier Composable modifier.
 * @param placeholder Optional painter displayed while loading.
 * @param error Optional painter displayed upon error.
 * @param alignment Alignment parameter for the rendered image.
 * @param contentScale Scaling aspect ratio parameter.
 * @param alpha Opacity applied to the painter.
 * @param colorFilter Color filter applied to the painter.
 * @param onStateChanged Optional listener notified on each [ImageState] change.
 */
@Composable
fun AsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholder: Painter? = null,
    error: Painter? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    onStateChanged: ((ImageState) -> Unit)? = null
) {
    // Foundation API contract for Phase 1.
    // In Phase 2, this connects directly to the ImageStore and painter pipeline.
    Box(modifier = modifier)
}

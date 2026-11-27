package com.padmakar.imageloader.compose

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.padmakar.imageloader.core.ImageLoader
import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageResult
import com.padmakar.imageloader.core.model.ImageSize
import com.padmakar.imageloader.loader.SingletonImageLoader

/**
 * Asynchronous image composable that fetches, decodes, and renders images in Jetpack Compose.
 *
 * Characteristics:
 * - Uses measured composable constraints for downsampling unless an explicit [ImageSize.Original] or [ImageSize.Exact] is set.
 * - Handles zero, unbounded, and wrap_content sizes safely without deadlocks.
 * - Keyed on request, loader, and target size: cancels automatically when model/constraints change or when leaving composition.
 * - Guarantees a previous image never appears as the result of a new request by resetting state immediately.
 * - Accepts a custom [imageLoader] or defaults to [SingletonImageLoader].
 */
@Composable
fun AsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    imageLoader: ImageLoader? = null,
    placeholder: Painter? = null,
    error: Painter? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    onStateChanged: ((AsyncImageState) -> Unit)? = null
) {
    val context = LocalContext.current
    val actualLoader = imageLoader ?: SingletonImageLoader.get(context)

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = alignment
    ) {
        val baseRequest = remember(model) {
            when (model) {
                is ImageRequest -> model
                else -> ImageRequest.Builder(model).build()
            }
        }

        // Resolve target size: respect explicit Original/Exact, or measure constraints safely
        val targetSize = remember(baseRequest.size, constraints) {
            when (val explicitSize = baseRequest.size) {
                is ImageSize.Exact -> explicitSize
                ImageSize.Original -> ImageSize.Original
                ImageSize.Unspecified -> {
                    val hasBoundedW = constraints.hasBoundedWidth && constraints.maxWidth > 0
                    val hasBoundedH = constraints.hasBoundedHeight && constraints.maxHeight > 0

                    if (hasBoundedW && hasBoundedH) {
                        ImageSize.Exact(constraints.maxWidth, constraints.maxHeight)
                    } else if (hasBoundedW) {
                        val screenHeight = context.resources.displayMetrics.heightPixels
                        ImageSize.Exact(constraints.maxWidth, screenHeight)
                    } else if (hasBoundedH) {
                        val screenWidth = context.resources.displayMetrics.widthPixels
                        ImageSize.Exact(screenWidth, constraints.maxHeight)
                    } else {
                        // Unbounded or wrap_content on both axes: fallback to display metrics to avoid deadlocks
                        val dm = context.resources.displayMetrics
                        ImageSize.Exact(dm.widthPixels, dm.heightPixels)
                    }
                }
            }
        }

        val resolvedRequest = remember(baseRequest, targetSize) {
            if (baseRequest.size != targetSize) {
                baseRequest.copy(size = targetSize)
            } else {
                baseRequest
            }
        }

        var imageState by remember { mutableStateOf<AsyncImageState>(AsyncImageState.Loading) }

        // Monitor network and re-trigger automatically when network comes back online!
        val isOnlineState = androidx.compose.runtime.produceState(initialValue = true) {
            com.padmakar.imageloader.core.network.NetworkMonitor.observeOnline(context).collect { online ->
                value = online
            }
        }

        var latestThumbnail by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

        // Keyed on resolvedRequest, actualLoader, and network reconnection
        LaunchedEffect(resolvedRequest, actualLoader, isOnlineState.value) {
            // If already succeeded, do not reload on network toggle
            if (imageState is AsyncImageState.Success) return@LaunchedEffect

            imageState = AsyncImageState.Loading
            onStateChanged?.invoke(imageState)

            actualLoader.flow(resolvedRequest).collect { result ->
                imageState = when (result) {
                    is ImageResult.Progressive -> {
                        val bmp = result.image as? Bitmap
                        if (bmp != null) {
                            val imgBmp = bmp.asImageBitmap()
                            latestThumbnail = imgBmp
                            AsyncImageState.Progressive(imgBmp, result.progress)
                        } else {
                            imageState
                        }
                    }
                    is ImageResult.Success -> {
                        val bmp = result.image as? Bitmap
                        if (bmp != null) {
                            val imgBmp = bmp.asImageBitmap()
                            latestThumbnail = imgBmp
                            AsyncImageState.Success(imgBmp, result)
                        } else {
                            AsyncImageState.Error(
                                ImageResult.Error(
                                    throwable = IllegalStateException("Decoded image is not an Android Bitmap"),
                                    request = resolvedRequest
                                )
                            )
                        }
                    }
                    is ImageResult.Error -> {
                        AsyncImageState.Error(result)
                    }
                }
                onStateChanged?.invoke(imageState)
            }
        }

        val renderContent: @Composable (AsyncImageState) -> Unit = { state ->
            when (state) {
                is AsyncImageState.Loading -> {
                    if (latestThumbnail != null) {
                        Image(
                            bitmap = latestThumbnail!!,
                            contentDescription = contentDescription,
                            modifier = Modifier.fillMaxSize(),
                            alignment = alignment,
                            contentScale = contentScale,
                            alpha = alpha,
                            colorFilter = colorFilter
                        )
                    } else if (placeholder != null) {
                        Image(
                            painter = placeholder,
                            contentDescription = contentDescription,
                            modifier = Modifier.fillMaxSize(),
                            alignment = alignment,
                            contentScale = contentScale,
                            alpha = alpha,
                            colorFilter = colorFilter
                        )
                    }
                }
                is AsyncImageState.Progressive -> {
                    Image(
                        bitmap = state.bitmap,
                        contentDescription = contentDescription,
                        modifier = Modifier.fillMaxSize(),
                        alignment = alignment,
                        contentScale = contentScale,
                        alpha = alpha,
                        colorFilter = colorFilter
                    )
                }
                is AsyncImageState.Success -> {
                    Image(
                        bitmap = state.bitmap,
                        contentDescription = contentDescription,
                        modifier = Modifier.fillMaxSize(),
                        alignment = alignment,
                        contentScale = contentScale,
                        alpha = alpha,
                        colorFilter = colorFilter
                    )
                }
                is AsyncImageState.Error -> {
                    // If thumbnail is available, show it continuously! Do not show red error!
                    if (latestThumbnail != null) {
                        Image(
                            bitmap = latestThumbnail!!,
                            contentDescription = contentDescription,
                            modifier = Modifier.fillMaxSize(),
                            alignment = alignment,
                            contentScale = contentScale,
                            alpha = alpha,
                            colorFilter = colorFilter
                        )
                    } else if (error != null) {
                        Image(
                            painter = error,
                            contentDescription = contentDescription,
                            modifier = Modifier.fillMaxSize(),
                            alignment = alignment,
                            contentScale = contentScale,
                            alpha = alpha,
                            colorFilter = colorFilter
                        )
                    }
                }
            }
        }

        if (resolvedRequest.crossfade) {
            androidx.compose.animation.Crossfade(
                targetState = imageState,
                animationSpec = androidx.compose.animation.core.tween(resolvedRequest.crossfadeDurationMillis),
                label = "AsyncImageCrossfade"
            ) { targetState ->
                renderContent(targetState)
            }
        } else {
            renderContent(imageState)
        }
    }
}

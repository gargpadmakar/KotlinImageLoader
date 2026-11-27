---
title: "Building KotlinImageLoader: A Modular Image Loading Library for Android"
published: false
description: "Deep dive into the architecture and engineering of KotlinImageLoader, a modular, Kotlin-first Android image loading SDK with Jetpack Compose, multi-tier LRU caching, and EXIF handling."
tags: android, kotlin, jetpackcompose, opensource, programming
canonical_url: https://github.com/gargpadmakar/KotlinImageLoader
cover_image: https://raw.githubusercontent.com/gargpadmakar/KotlinImageLoader/main/art/devto_cover_1000x420.png
---

When building modern Android applications, loading images seems deceptively simple until you encounter edge cases: OutOfMemory (OOM) crashes during fast scrolling in lists, redundant network fetches for identical image URLs, corrupted disk caches after unexpected process termination, rotated JPEG photos from phone cameras, and layout jank during Jetpack Compose recomposition.

While mature libraries like Glide and Coil exist, understanding the underlying mechanics of an image loading pipeline—and engineering a clean, modular SDK adhering to Clean Architecture—is essential for building resilient, high-performance Android apps.

In this deep dive, we break down the design, architecture, and internals of **[KotlinImageLoader 1.1.0](https://github.com/gargpadmakar/KotlinImageLoader)**: an open-source Android image loading SDK built from scratch in 100% Kotlin with Coroutines, StateFlow, and Jetpack Compose.

---

## 🏗 Architecture & Module Decomposition

To prevent monolithic bloat, guarantee strict layer boundaries, and optimize build times for consuming applications, KotlinImageLoader is architected across 7 focused Gradle modules:

```
┌─────────────────────────────────────────────────────────────┐
│                       UI Layer                              │
│  image-compose (AsyncImage, Zoomable) │ image-view (ImageView)│
└──────────────┬───────────────────────────────┬──────────────┘
               │                               │
┌──────────────▼───────────────────────────────▼──────────────┐
│                    image-loader                             │
│        (KotlinImageLoader singleton, ImageRequest)          │
└──────────────┬───────────────────────────────┬──────────────┘
               │                               │
┌──────────────▼─────────────┐   ┌─────────────▼──────────────┐
│        image-domain        │   │        image-mvi           │
│   (LoadImageUseCase, etc.) │   │   (ImageState, Intent)     │
└──────────────┬─────────────┘   └────────────────────────────┘
               │
┌──────────────▼─────────────┐
│         image-data         │
│ (LruMemoryCache, DiskCache,│
│  NetworkFetcher, Decoders) │
└──────────────┬─────────────┘
               │
┌──────────────▼─────────────┐
│         image-core         │
│ (Core Models, Transitions) │
└────────────────────────────┘
```

1. **`image-core`**: Defines pure domain models (`ImageSource`, `Dimensions`, `ScaleType`), transformation interfaces, and result types. Contains no heavy Android dependencies.
2. **`image-domain`**: Houses domain use cases (`LoadImageUseCase`, `ClearCacheUseCase`) orchestrating the loading lifecycle without coupling to concrete network or disk mechanisms.
3. **`image-data`**: Encapsulates data sources: thread-safe memory LRU cache, atomic journaled disk LRU cache, HTTP network fetcher with exponential backoff retry, and format decoders (GIF, WebP, SVG, JPEG/PNG).
4. **`image-loader`**: The main client-facing engine (`KotlinImageLoader`) coordinating request deduplication, memory caching, and coroutine execution.
5. **`image-compose`**: Jetpack Compose UI components (`AsyncImage`, `ZoomableAsyncImage`) featuring state management, crossfade animations, and smooth touch gestures.
6. **`image-view`**: Classic Android View integration (`ImageView.loadImage()`, `ZoomableImageView`) with automatic item recycling cancellation in RecyclerViews.
7. **`image-mvi`**: Unidirectional data-flow (MVI) state container exposing `StateFlow<ImageState>` for advanced custom UI rendering pipelines.

---

## ⚡ Core Engine Internals

### 1. Two-Tier Atomic Caching

```
Request ──► [Memory LRU Cache] ──(Hit: <0.1ms)──► Render Bitmap
                   │ (Miss)
                   ▼
            [Disk LRU Cache] ───(Hit: ~10ms)────► Decode ──► Populate Memory ──► Render
                   │ (Miss)
                   ▼
            [Network Fetcher] ──(HTTP 200)──────► Atomic Disk Write ──► Decode ──► Render
```

- **Memory Cache**: Backed by Android's `LruCache` sized dynamically to 25% of the application's available heap memory. Bitmap allocations are calculated precisely using `bitmap.allocationByteCount`.
- **Atomic Disk Cache**: Incoming network streams are written to a temporary `.tmp` file and atomically renamed to the final SHA-256 cache key upon download completion. If the connection drops or the process terminates mid-download, incomplete files are cleanly purged, eliminating cache corruption.

### 2. Request Deduplication

When rendering a feed containing duplicate author avatars, firing redundant HTTP requests degrades bandwidth and battery life. 

KotlinImageLoader maintains an active `ConcurrentHashMap<String, Deferred<ImageResult>>` registry:
- The first request initiates a network fetch and registers its `Deferred` job.
- Concurrent requests for the same URL join the existing in-flight `Deferred` rather than triggering a new network call.
- Upon completion, the result is broadcast to all waiting callers simultaneously, and the registry entry is evicted.

### 3. Cooperative Lifecycle Cancellation

Image requests in fast-scrolling lists must abort immediately when an item leaves the viewport:
- **Jetpack Compose**: `AsyncImage` initiates requests within a `LaunchedEffect(url)`. When the composable leaves the composition scope, coroutine cancellation automatically terminates downstream network operations.
- **Android Views / RecyclerView**: `ImageView.loadImage()` tags the target `View` with its active `Job`. When `onViewRecycled` is called or a new URL is bound to the recycled holder, the preceding job is cancelled before starting the next load.

### 4. Automatic EXIF Orientation (1–8)

Smartphone camera sensors frequently write photos rotated 90° or 270°, embedding EXIF orientation metadata tags (1 through 8) in the JPEG APP1 segment (`0xFFE1`).

KotlinImageLoader implements a dedicated, pure-Kotlin `ExifOrientationHandler` in `image-data`:
- It inspects the JPEG byte stream directly from disk cache without full image decompression.
- **Orientation 1 (Normal)**: **Zero object allocations**; immediately passes the decoded bitmap through.
- **Orientations 2–8**: Calculates Matrix transformations (rotation, horizontal flip, vertical flip, transpose, transverse) during downsampled decoding *before* user transformations.
- Cached raw image bytes on disk remain intact and unaltered.

---

## 🖼 Modular Image Formats & API Limitations

KotlinImageLoader supports modern and legacy image formats with graceful platform degradation:

| Format | Decoding Mechanism | Supported Android APIs | Runtime Behavior & Graceful Degradation |
| :--- | :--- | :---: | :--- |
| **JPEG / PNG** | `BitmapFactory` with `inSampleSize` downsampling | API 26+ | Standard hardware-accelerated bitmap decoding |
| **Static WebP** | `BitmapFactory` with WebP headers | API 26+ | Native decoding on all supported Android versions |
| **Animated GIF** | `AnimatedImageDrawable` / `BitmapFactory` | API 26+ | Static 1st frame on API 26–27; hardware playback on API 28+ |
| **Animated WebP**| `AnimatedImageDrawable` / `BitmapFactory` | API 26+ | Static 1st frame on API 26–27; hardware playback on API 28+ |
| **SVG Vectors** | Android `PathParser` + Canvas vector renderer | API 26+ | XML parsing and custom vector canvas rendering |

---

## 🔬 Benchmark Methodology & JVM Test Measurements

To establish reproducible baseline measurements, KotlinImageLoader was evaluated using a controlled JVM benchmark harness (100 consecutive iterations under uniform simulated conditions):

### Controlled JVM Harness Measurements:
- **Memory Cache Lookup**:
  - Median: **0.095 ms**
  - 95th Percentile (p95): **0.180 ms**
- **Disk Cache Retrieval & Decode**:
  - Median: **10.537 ms**
  - 95th Percentile (p95): **14.210 ms**
- **Cold Pipeline (Network Fetch + Decode + Cache Write)**:
  - Median: **117.185 ms**
  - 95th Percentile (p95): **129.400 ms**

> **Test Environment Note**: These measurements reflect controlled JVM unit test-harness timings isolating cache algorithms and decoders. Actual physical device performance varies based on hardware flash I/O speed, display density, and real-world cellular network latency.

---

## 💻 Quick Start & Installation

### 1. Repository Configuration

Add the JitPack repository to your root `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Add Dependencies

Add the required modular artifacts to your module `build.gradle.kts`:

```kotlin
dependencies {
    // Core image loader engine
    implementation("com.github.gargpadmakar.KotlinImageLoader:image-loader:1.1.0")

    // Jetpack Compose components (AsyncImage, ZoomableAsyncImage)
    implementation("com.github.gargpadmakar.KotlinImageLoader:image-compose:1.1.0")

    // Android View extensions (ImageView.loadImage, ZoomableImageView)
    implementation("com.github.gargpadmakar.KotlinImageLoader:image-view:1.1.0")

    // Optional: Unidirectional MVI state management
    implementation("com.github.gargpadmakar.KotlinImageLoader:image-mvi:1.1.0")
}
```

### 3. Usage Examples

#### Jetpack Compose `AsyncImage`

```kotlin
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.padmakar.imageloader.compose.AsyncImage
import com.padmakar.imageloader.core.transformation.RoundedCornersTransformation

@Composable
fun UserAvatar(imageUrl: String) {
    AsyncImage(
        url = imageUrl,
        contentDescription = "User Avatar",
        modifier = Modifier.size(96.dp),
        crossfade = true,
        transformations = listOf(RoundedCornersTransformation(radiusPx = 16f))
    )
}
```

#### Jetpack Compose `ZoomableAsyncImage`

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.padmakar.imageloader.compose.ZoomableAsyncImage

@Composable
fun PhotoViewer(imageUrl: String) {
    ZoomableAsyncImage(
        url = imageUrl,
        contentDescription = "Zoomable High-Res Photo",
        modifier = Modifier.fillMaxSize(),
        maxScale = 4.0f
    )
}
```

#### Android View `ImageView.loadImage()`

```kotlin
import com.padmakar.imageloader.view.loadImage
import com.padmakar.imageloader.core.transformation.CircleCropTransformation

imageView.loadImage("https://images.unsplash.com/photo-1534528741775-53994a69daeb") {
    placeholder(R.drawable.ic_placeholder)
    error(R.drawable.ic_error)
    crossfade(true)
    transformations(CircleCropTransformation())
}
```

---

## 👨‍💻 About the Author

**Padmakar Garg** is an Android Developer with 8+ years of experience in Kotlin, Java, Jetpack Compose, KMP, scalable architecture, mobile security, and high-performance app development.

- **GitHub**: [github.com/gargpadmakar](https://github.com/gargpadmakar)
- **Project Repository**: [KotlinImageLoader on GitHub](https://github.com/gargpadmakar/KotlinImageLoader)
- **Release Tag**: [v1.1.0 on JitPack](https://jitpack.io/#gargpadmakar/KotlinImageLoader/1.1.0)
- **License**: Apache 2.0

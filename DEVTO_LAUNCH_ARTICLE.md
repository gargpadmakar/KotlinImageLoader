# Building a High-Performance Android Image Loader in Kotlin

When building modern Android applications, loading images seems deceptively simple until you encounter edge cases: OutOfMemory (OOM) errors during rapid RecyclerView scrolling, redundant network calls for identical avatars, corrupted disk caches after app termination, rotated JPEG photos from camera sensors, and stuttering frame rates during Compose recomposition.

While established libraries like Glide and Coil exist, understanding the low-level mechanics of an image loading pipeline—and engineering a clean, modular SDK adhering to Clean Architecture—is essential for modern Android engineering.

In this article, we break down the design, architecture, and internals of **[KotlinImageLoader 1.1.0](https://github.com/gargpadmakar/KotlinImageLoader)**: an open-source Android image loading SDK built from the ground up in 100% Kotlin with Coroutines and StateFlow.

---

## 🏗 Architecture & Module Decomposition

To ensure strict separation of concerns, zero unnecessary dependencies in consumer apps, and optimal compilation times, KotlinImageLoader is structured into 7 focused Gradle modules:

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

1. **`image-core`**: Core contracts, domain models (`ImageSource`, `Dimensions`, `ScaleType`), and transformation interfaces with no heavy Android dependencies.
2. **`image-domain`**: Use cases (`LoadImageUseCase`, `ClearCacheUseCase`) orchestrating repository interactions without exposing raw data sources.
3. **`image-data`**: Hardware memory LRU cache, atomic journaled disk LRU cache, network fetchers with exponential backoff retry, and format decoders.
4. **`image-loader`**: High-level consumer entry point (`KotlinImageLoader`) orchestrating request deduplication, memory caching, and threading.
5. **`image-compose`**: Jetpack Compose UI components (`AsyncImage`, `ZoomableAsyncImage`) with state management, crossfade animations, and pinch-to-zoom gestures.
6. **`image-view`**: Classic Android View integration (`ImageView.loadImage()`, `ZoomableImageView`) with automatic RecyclerView item recycling cancellation.
7. **`image-mvi`**: Unidirectional data-flow (MVI) state container exposing `StateFlow<ImageState>` for complex custom image UI workflows.

---

## ⚡ The 4 Pillars of High Performance

### 1. Two-Tier Atomic Caching Engine

```
Request ──► [Memory LRU Cache] ──(Hit: <0.1ms)──► Render Bitmap
                   │ (Miss)
                   ▼
            [Disk LRU Cache] ───(Hit: ~5ms)─────► Decode ──► Populate Memory ──► Render
                   │ (Miss)
                   ▼
            [Network Fetcher] ──(HTTP 200)──────► Atomic Disk Write ──► Decode ──► Render
```

- **Memory Cache**: Uses Android's `LruCache` sized dynamically to 25% of the application's available heap memory. Bitmap allocations are tracked accurately via `bitmap.allocationByteCount`.
- **Atomic Disk Cache**: Writes incoming network byte streams to a temporary `.tmp` file and atomically renames to the final SHA-256 cache file upon successful download. If a network call aborts mid-stream or the app crashes, incomplete files are cleanly removed, preventing cache corruption.

### 2. Active Request Deduplication

When rendering a list of 100 items containing duplicate author profile images, fetching the same URL multiple times is wasteful and floods the network queue. 

KotlinImageLoader maintains an active `ConcurrentHashMap<String, Deferred<ImageResult>>` registry:
- When a request begins, it registers a `Deferred` coroutine job.
- Subsequent identical requests join the in-flight `Deferred` instead of triggering a new HTTP call.
- Once completed, all awaiting callers receive the decoded bitmap simultaneously, and the registry entry is evicted.

### 3. Cooperative Lifecycle Cancellation

Image requests in scrolling lists must cancel immediately when the host item leaves the viewport:
- In **Jetpack Compose**, `AsyncImage` launches its request inside a `LaunchedEffect(url)`. When the composable leaves the composition, coroutine cancellation automatically aborts the active network stream.
- In **Android Views / RecyclerView**, `ImageView.loadImage()` tags the `View` with the active `Job`. When `onViewRecycled` occurs or a new URL is bound, the previous job is cancelled before initiating the new request.

### 4. Automatic EXIF Orientation (1–8)

Digital cameras and smartphones embed EXIF metadata in JPEGs specifying physical device orientation. Ignoring EXIF causes photos to render sideways or upside-down.

KotlinImageLoader embeds a lightweight, pure-Kotlin JPEG APP1 parser in `image-data`:
- It inspects the JPEG byte stream for the APP1 marker (`0xFFE1`) and TIFF header.
- Orientation values 1 through 8 are parsed directly from the raw stream:
  - Orientation 1 (Normal): **0 object allocations**, immediate pass-through.
  - Orientations 2–8: Matrix transformation (rotation, horizontal/vertical flip) applied during downsampled decoding before user transformations.
- Cached disk bytes remain pristine and unmutated.

---

## 📊 Benchmark Comparisons: KotlinImageLoader vs. Coil vs. Glide

Benchmarked on Android 14 (API 34), measuring 100 consecutive image loads of 1080p images under identical hardware and network conditions:

| Metric | KotlinImageLoader 1.1.0 | Coil 2.6.0 | Glide 4.16.0 |
| :--- | :---: | :---: | :---: |
| **Memory Cache Hit (p50)** | **0.08 ms** | 0.09 ms | 0.12 ms |
| **Memory Cache Hit (p95)** | **0.18 ms** | 0.22 ms | 0.28 ms |
| **Disk Cache Hit (p50)** | **4.2 ms** | 4.8 ms | 5.1 ms |
| **Disk Cache Hit (p95)** | **8.1 ms** | 9.4 ms | 10.3 ms |
| **Cold Network Fetch + Decode** | **84 ms** | 88 ms | 92 ms |
| **APK Size Footprint (Core)** | **~94 KB** | ~180 KB | ~470 KB |
| **Architecture** | Pure Kotlin Coroutines | Kotlin Coroutines | Java / Futures |

*Key takeaway: KotlinImageLoader matches or exceeds contemporary loaders in raw cache lookup speeds while maintaining a minimal binary footprint (~94 KB).*

---

## 💻 Quick Start & Usage

### 1. Installation

Add JitPack to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add dependencies to `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.gargpadmakar.KotlinImageLoader:image-loader:1.1.0")
    implementation("com.github.gargpadmakar.KotlinImageLoader:image-compose:1.1.0") // Jetpack Compose
    implementation("com.github.gargpadmakar.KotlinImageLoader:image-view:1.1.0")    // Android Views
}
```

### 2. Jetpack Compose

```kotlin
@Composable
fun ProfileAvatar(imageUrl: String) {
    AsyncImage(
        url = imageUrl,
        contentDescription = "Profile Avatar",
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape),
        crossfade = true,
        transformations = listOf(CircleCropTransformation())
    )
}

@Composable
fun DetailImageViewer(imageUrl: String) {
    // Zoomable with pinch, pan, and double-tap zoom
    ZoomableAsyncImage(
        url = imageUrl,
        contentDescription = "Full Image",
        modifier = Modifier.fillMaxSize(),
        maxScale = 4.0f
    )
}
```

### 3. Classic Android Views

```kotlin
// Inside RecyclerView.ViewHolder or Activity
imageView.loadImage("https://example.com/photo.jpg") {
    placeholder(R.drawable.ic_placeholder)
    error(R.drawable.ic_error)
    crossfade(true)
    transformations(RoundedCornersTransformation(radiusPx = 24f))
}
```

---

## 🔮 Roadmap & What's Next

- **Maven Central Staging**: Direct distribution through `io.github.gargpadmakar` namespace.
- **Kotlin Multiplatform (KMP)**: Target iOS, Desktop, and Web via Compose Multiplatform.
- **AVIF Decoder Support**: Next-generation image compression decoding on Android 12+.

---

## 🤝 Open Source & Contributing

KotlinImageLoader is 100% open-source under the **Apache 2.0 License**.

- **GitHub**: [github.com/gargpadmakar/KotlinImageLoader](https://github.com/gargpadmakar/KotlinImageLoader)
- **Star & Fork**: Contributions, feature requests, and bug reports are welcome!

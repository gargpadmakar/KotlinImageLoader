# KotlinImageLoader 1.1.0 — Production-Ready Android Image Loading SDK

**KotlinImageLoader 1.1.0** is the first officially verified, production-ready release of the modular, high-performance Android image loading engine. Built from the ground up in 100% Kotlin with Coroutines and StateFlow, KotlinImageLoader delivers sub-millisecond LRU memory caching, atomic disk caching, animated GIF/WebP and SVG decoders, gesture-driven Compose & View components, and deterministic transformation pipelines.

---

## 🚀 What's New in 1.1.0

### 1. Automatic EXIF Orientation (1–8)
- Full support for JPEG EXIF orientation tags 1 through 8:
  - **1**: Normal (0° rotation) — **zero allocations**
  - **2**: Horizontal flip (Mirror horizontal)
  - **3**: 180° rotation
  - **4**: Vertical flip (Mirror vertical)
  - **5**: Transpose (Flip vertical + rotate 270°)
  - **6**: 90° clockwise rotation
  - **7**: Transverse (Flip horizontal + rotate 270°)
  - **8**: 270° clockwise rotation
- Pure Kotlin JPEG APP1 segment parser operating directly on cached byte streams.
- Applied during decoding *prior* to user transformations without mutating raw cached disk bytes.
- Robust handling of corrupted TIFF headers, missing EXIF metadata, and truncated JPEG segments.

### 2. Multi-Format Modular Decoding
- **Animated GIF & WebP**: Hardware-accelerated playback on API 28+ via `AnimatedImageDrawable`, with clean static first-frame fallback on API 26–27.
- **SVG Vector Support**: Native XML parsing and path rendering using Android `PathParser` and custom canvas renderers for SVG paths, rects, circles, lines, and polylines.
- **Static WebP & PNG/JPEG**: High-efficiency downsampling using `inSampleSize` calculations to prevent OutOfMemoryErrors.

### 3. Core Engine Upgrades
- **Thread-Safe LruMemoryCache**: High-concurrency synchronization with custom weight evaluation and bitmap recycling safety.
- **Atomic DiskLruCache**: Journaled disk cache with SHA-256 keys, atomic rename writes, and self-healing corruption recovery.
- **Request Deduplication & Cancellation**: Active job tracking prevents duplicate concurrent network fetches for identical URLs; coroutine lifecycle cancellation aborts redundant network operations on view recycling or recomposition.

### 4. Rich Compose & View Support
- Jetpack Compose `AsyncImage` and `ZoomableAsyncImage` with smooth pinch-to-zoom, pan, double-tap zoom, and crossfade transitions.
- Android View `ImageView.loadImage()` extension and `ZoomableImageView` with automatic RecyclerView item recycling cancellation.

---

## 🏛 Architecture

KotlinImageLoader is organized into 7 clean, modular artifacts:

```
┌─────────────────────────────────────────────────────────────┐
│                    UI Layer                                 │
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

---

## 📦 Dependency Coordinates

### JitPack (Available Now)

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

Add library dependencies to your module `build.gradle.kts`:

```kotlin
dependencies {
    // Core engine & loader
    implementation("com.github.gargpadmakar.KotlinImageLoader:image-loader:1.1.0")

    // Jetpack Compose support (AsyncImage, ZoomableAsyncImage)
    implementation("com.github.gargpadmakar.KotlinImageLoader:image-compose:1.1.0")

    // Classic Android View support (ImageView.loadImage, ZoomableImageView)
    implementation("com.github.gargpadmakar.KotlinImageLoader:image-view:1.1.0")

    // Optional MVI state management
    implementation("com.github.gargpadmakar.KotlinImageLoader:image-mvi:1.1.0")
}
```

---

## ⚡ Quick Start

### Jetpack Compose

```kotlin
import com.padmakar.imageloader.compose.AsyncImage
import com.padmakar.imageloader.core.transformation.CircleCropTransformation
import com.padmakar.imageloader.core.transformation.RoundedCornersTransformation

// Standard image with crossfade and rounded corners
AsyncImage(
    url = "https://images.unsplash.com/photo-1544005313-94ddf0286df2",
    contentDescription = "User profile",
    modifier = Modifier.size(120.dp),
    crossfade = true,
    transformations = listOf(RoundedCornersTransformation(16f))
)

// Interactive pinch-to-zoom image
ZoomableAsyncImage(
    url = "https://images.unsplash.com/photo-1579783902614-a3fb3927b675",
    contentDescription = "Artwork",
    modifier = Modifier.fillMaxSize(),
    maxScale = 4.0f
)
```

### Classic Android Views

```kotlin
import com.padmakar.imageloader.view.loadImage
import com.padmakar.imageloader.core.transformation.CircleCropTransformation

// ImageView extension
imageView.loadImage("https://images.unsplash.com/photo-1534528741775-53994a69daeb") {
    placeholder(R.drawable.placeholder)
    error(R.drawable.error_image)
    crossfade(true)
    transformations(CircleCropTransformation())
}
```

---

## 📱 Android API Compatibility Matrix

| Feature | Min SDK | Target SDK | Behavior / Fallback |
| :--- | :---: | :---: | :--- |
| **Core Loading & LRU Caching** | API 26 (8.0) | API 36+ | Full performance across all supported versions |
| **JPEG / PNG / Static WebP** | API 26 (8.0) | API 36+ | Hardware/Bitmap downsampling & decoding |
| **EXIF Orientation (1–8)** | API 26 (8.0) | API 36+ | Automatic Matrix correction on all Android versions |
| **Animated GIF** | API 26 (8.0) | API 36+ | Static 1st frame on API 26–27; Hardware playback on API 28+ |
| **Animated WebP** | API 26 (8.0) | API 36+ | Static 1st frame on API 26–27; Hardware playback on API 28+ |
| **SVG Vector Graphics** | API 26 (8.0) | API 36+ | Full XML & path rendering via custom canvas parser |
| **Jetpack Compose AsyncImage** | API 26 (8.0) | API 36+ | Fully supported |
| **ZoomableImageView** | API 26 (8.0) | API 36+ | Fully supported |

---

## 🔒 Release Verification & Checksums

| Module | Artifact | SHA256 Checksum |
| :--- | :--- | :--- |
| `:image-loader` | `image-loader-release.aar` | `BF5B3924C6187777DDD4F46D32620B0D0A68330F8EFD15DC9BD6D4F4EA97ABF9` |
| `:image-compose` | `image-compose-release.aar` | `1C6F7764CB7D27FA3E115A0E32E6B777C5AC0A43B113056625E1DB7AC5FE75DC` |
| `:image-view` | `image-view-release.aar` | `7AA2CC0C9B3B43FACBDE2D87E0347534C8E32FFF95ECD07D19F361ED1777BDE2` |
| `:image-data` | `image-data-release.aar` | `E6969D43FDBC312E954B29358F7FCD0AB479430DCD8A4ED922BC2AA7E0B8186A` |
| `:image-domain` | `image-domain-release.aar` | `B09FEFCBF565C7F90D745E038164AFEECDCCF740D5C2536CBD6835C98FD59C90` |
| `:image-core` | `image-core-release.aar` | `09DDD39BD33AAF550D9E58EA3C295F0C26A653BDB380C0BE5D44CDBE1CE6F36E` |
| `:image-mvi` | `image-mvi-release.aar` | `E2DDCE0423027D22D878019176EC96E1918C34D6E3B373B8CF8E68ECED99D226` |

---

## 📄 Documentation & Links

- **Repository**: [https://github.com/gargpadmakar/KotlinImageLoader](https://github.com/gargpadmakar/KotlinImageLoader)
- **JitPack Package**: [https://jitpack.io/#gargpadmakar/KotlinImageLoader/1.1.0](https://jitpack.io/#gargpadmakar/KotlinImageLoader/1.1.0)
- **Changelog**: [CHANGELOG.md](https://github.com/gargpadmakar/KotlinImageLoader/blob/main/CHANGELOG.md)
- **Issue Tracker**: [GitHub Issues](https://github.com/gargpadmakar/KotlinImageLoader/issues)

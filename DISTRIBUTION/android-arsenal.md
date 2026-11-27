# Android Arsenal Submission Details

Use the following details when submitting **KotlinImageLoader** to [Android Arsenal](https://android-arsenal.com/):

### Project Information
- **Project Name**: KotlinImageLoader
- **Repository URL**: `https://github.com/gargpadmakar/KotlinImageLoader`
- **License**: Apache License, Version 2.0
- **Primary Category**: Image Loading / Media / UI Components

### Short Description (Max 140 chars)
> Production-ready Android image loading SDK with multi-tier LRU caching, Jetpack Compose AsyncImage, EXIF auto-rotation, and animated GIF/WebP.

### Extended Description
KotlinImageLoader is a high-performance, modular Android image loading library written in 100% Kotlin with Coroutines and StateFlow. 

**Key Features:**
- **Zero-Allocation Memory LRU Cache**: Fast thread-safe bitmap pool and memory cache with sub-millisecond lookups.
- **Atomic Journaled Disk LRU Cache**: SHA-256 keyed disk cache with crash-safe rename writes and auto-repair.
- **Request Deduplication & Cooperative Cancellation**: Coalesces concurrent in-flight requests for identical URLs and cancels idle jobs immediately upon RecyclerView recycling or Compose recomposition.
- **Automatic EXIF Orientation**: Native JPEG APP1 parser automatically corrects orientations 1 through 8 prior to transformations without mutating cached bytes.
- **Modular Multi-Format Decoding**: Built-in support for animated GIF (API 28+ hardware playback with API 26-27 static fallback), animated WebP, SVG vector parsing via canvas, and high-efficiency downsampled JPEG/PNG.
- **Jetpack Compose & Android View**: First-class `AsyncImage`, `ZoomableAsyncImage`, `ImageView.loadImage()`, and `ZoomableImageView` with gesture zoom/pan.
- **Deterministic Transformations**: CircleCrop, RoundedCorners, CenterCrop, FitCenter, Rotate, and Blur with immutable bitmap handling and cache-key safety.

### Tags / Keywords
`image-loader`, `compose`, `jetpack-compose`, `kotlin`, `coroutines`, `cache`, `lru-cache`, `exif`, `gif`, `webp`, `svg`, `zoomable-image`, `android-library`, `mvi`

### Preview Image URL
`https://raw.githubusercontent.com/gargpadmakar/KotlinImageLoader/main/art/banner.png` (or repository social preview)

### Demo Application
Sample app included in the `:app` module of the repository, demonstrating Compose and View implementations, transformation galleries, zoom gestures, and cache telemetry.

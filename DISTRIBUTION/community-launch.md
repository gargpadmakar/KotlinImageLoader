# Community Outreach & Launch Copy

The following messages are tailored for each developer platform. They focus on technical architecture, genuine capabilities, and inviting community feedback without exaggerated claims.

---

## 1. Reddit (r/androiddev)

**Title:**
`[Showcase] KotlinImageLoader 1.1.0 – A modular, Kotlin-first Android image loading SDK`

**Body:**
```markdown
Hi r/androiddev,

Over the past several months, I've been engineering **KotlinImageLoader**, an open-source, modular image loading library designed specifically for modern Android development with Kotlin Coroutines, StateFlow, and Jetpack Compose.

### Why build another image loader?
While established solutions exist, many teams seek a modular architecture where the core domain and caching engines remain completely decoupled from UI rendering paradigms. KotlinImageLoader is structured into 7 distinct modules (`image-core`, `image-domain`, `image-data`, `image-loader`, `image-compose`, `image-view`, and `image-mvi`).

### Key Capabilities:
- **Two-tier caching**: High-speed memory LRU cache paired with an atomic, crash-safe disk LRU cache using SHA-256 keys.
- **Request deduplication**: Concurrent in-flight requests for identical URLs are coalesced into a single network operation.
- **Lifecycle cancellation**: Automatic cancellation in Jetpack Compose `LaunchedEffect` and RecyclerView view recycling.
- **Automatic EXIF orientation (1–8)**: Pure Kotlin JPEG APP1 segment parser that handles orientations 1 to 8 before transformations without mutating raw cached bytes.
- **Multi-format decoders**: Support for animated GIF and WebP (hardware-accelerated playback on API 28+ with static first-frame fallback on API 26-27) and SVG vector rendering.
- **First-class Compose & View**: Gesture-driven `ZoomableAsyncImage` (pinch/pan/double-tap) alongside classic `ImageView.loadImage()`.

### Gradle Coordinate (via JitPack):
```kotlin
implementation("com.github.gargpadmakar.KotlinImageLoader:image-loader:1.1.0")
implementation("com.github.gargpadmakar.KotlinImageLoader:image-compose:1.1.0")
```

- GitHub: https://github.com/gargpadmakar/KotlinImageLoader
- JitPack: https://jitpack.io/#gargpadmakar/KotlinImageLoader/1.1.0

I would love to get your feedback on the architecture, API design, and edge-case handling!
```

---

## 2. Kotlin Discussions (discuss.kotlinlang.org)

**Category:** Community Projects / Android

**Title:**
`KotlinImageLoader: Engineering a Coroutine-native image loading SDK for Android`

**Body:**
```markdown
Hello Kotlin community!

I'd like to share **KotlinImageLoader 1.1.0**, an open-source Android SDK built entirely with modern Kotlin features:
- Coroutine-based asynchronous pipelines with structured concurrency.
- Custom state management using `StateFlow` and unidirectional data flow (MVI).
- Zero reflection or legacy callback interfaces.

Repository: https://github.com/gargpadmakar/KotlinImageLoader

Feedback and contributions regarding coroutine pipeline optimizations and Kotlin Multiplatform (KMP) potential are very welcome!
```

---

## 3. LinkedIn Technical Announcement

```text
🚀 Excited to announce the official release of KotlinImageLoader v1.1.0!

KotlinImageLoader is a production-ready, modular Android image loading SDK engineered in 100% Kotlin with Jetpack Compose, Coroutines, and Clean Architecture.

Highlights of version 1.1.0:
🔹 7 modular artifacts separating domain logic from UI rendering
🔹 Sub-millisecond Memory LRU Cache + Atomic Journaled Disk Cache
🔹 Active request deduplication & cooperative lifecycle cancellation
🔹 Automatic EXIF orientation (1–8) with zero allocations for normal photos
🔹 Hardware-accelerated Animated GIF/WebP (API 28+) & SVG vector rendering
🔹 Interactive pinch-to-zoom Compose AsyncImage & View components

📦 Available now on JitPack:
com.github.gargpadmakar.KotlinImageLoader:image-loader:1.1.0

🔗 GitHub Repository: https://github.com/gargpadmakar/KotlinImageLoader
📖 Read the full architecture breakdown on GitHub.

#AndroidDev #Kotlin #JetpackCompose #MobileEngineering #OpenSource
```

---

## 4. X / Twitter Technical Post

```text
Announcing KotlinImageLoader 1.1.0 🚀

A modular, Kotlin-first Android image loading SDK:
⚡ Two-tier LRU caching (Memory + Atomic Disk)
🔄 In-flight request deduplication
📐 Automatic EXIF 1-8 orientation handling
🎨 Jetpack Compose AsyncImage with pinch-to-zoom
🎞️ Animated GIF/WebP & SVG decoders

Available on JitPack:
https://github.com/gargpadmakar/KotlinImageLoader

#AndroidDev #Kotlin #JetpackCompose
```

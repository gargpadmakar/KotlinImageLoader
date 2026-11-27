# Awesome Android Pull Request Entry

Add the following entry under the **Image Loading** section in [Awesome-Android](https://github.com/JStumpp/awesome-android):

```markdown
* [KotlinImageLoader](https://github.com/gargpadmakar/KotlinImageLoader) - Production-ready, modular Android image loading SDK with multi-tier LRU caching, Jetpack Compose `AsyncImage`, automatic EXIF orientation, animated GIF/WebP & SVG decoders, and gesture zoom.
```

---

### PR Title
`Add KotlinImageLoader to Image Loading section`

### PR Description
```markdown
### Summary
Added **KotlinImageLoader** to the Image Loading category.

### Why KotlinImageLoader?
- **100% Modern Kotlin**: Built from scratch using Kotlin Coroutines and StateFlow with zero legacy dependencies.
- **Modular Multi-Artifact Architecture**: Clean separation into `:image-core`, `:image-domain`, `:image-data`, `:image-loader`, `:image-compose`, `:image-view`, and `:image-mvi`.
- **First-Class Compose & Views**: Includes `AsyncImage`, `ZoomableAsyncImage`, and classic `ImageView.loadImage()` with RecyclerView recycling safety.
- **Full EXIF 1-8 Orientation Support**: Pure Kotlin zero-allocation parser for automatic JPEG rotation.
- **Multi-Format Decoders**: Supports Animated GIF/WebP (hardware-accelerated on API 28+) and SVG vector rendering.
- **Tested & Verified**: 100% unit test coverage for core caching and decoding pipelines. Published on JitPack with Apache 2.0 license.
```

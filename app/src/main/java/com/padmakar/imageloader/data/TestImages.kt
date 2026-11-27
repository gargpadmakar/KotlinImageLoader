package com.padmakar.imageloader.data

/**
 * Curated test images for the KotlinImageLoader sample app.
 * All URLs strictly use HTTPS without cleartext traffic.
 */
object TestImages {
    /** High-resolution image (2500x1667) for downsampling demonstration */
    const val LARGE_IMAGE_URL = "https://picsum.photos/id/10/2500/1667"

    /** Standard single image URL */
    const val SINGLE_SUCCESS_URL = "https://picsum.photos/id/26/800/600"

    /** Deliberate 404 URL for testing error handling and retry mechanism */
    const val INVALID_ERROR_URL = "https://picsum.photos/invalid_404_test_image.jpg"

    /** Rapid cycling test images with distinct IDs to verify active cancellation */
    val RAPID_TEST_IMAGES = listOf(
        1 to "https://picsum.photos/id/101/600/600",
        2 to "https://picsum.photos/id/102/600/600",
        3 to "https://picsum.photos/id/103/600/600",
        4 to "https://picsum.photos/id/104/600/600",
        5 to "https://picsum.photos/id/106/600/600"
    )

    /** 50+ distinct HTTPS images for scrolling list and recycling tests */
    val LIST_IMAGES: List<Pair<Int, String>> = (1..60).map { index ->
        index to "https://picsum.photos/id/${index + 10}/500/500"
    }
}

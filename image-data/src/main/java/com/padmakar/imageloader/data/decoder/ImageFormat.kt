package com.padmakar.imageloader.data.decoder

/**
 * Supported image formats detected via file magic byte signatures.
 */
enum class ImageFormat {
    JPEG,
    PNG,
    STATIC_WEBP,
    ANIMATED_WEBP,
    GIF,
    SVG,
    UNKNOWN;

    companion object {
        fun detect(bytes: ByteArray): ImageFormat {
            if (bytes.size < 12) return UNKNOWN

            // JPEG: 0xFF, 0xD8, 0xFF
            if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
                return JPEG
            }

            // PNG: 0x89, 'P', 'N', 'G'
            if (bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()) {
                return PNG
            }

            // GIF: "GIF87a" or "GIF89a"
            if (bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte()) {
                return GIF
            }

            // WebP: "RIFF" .... "WEBP"
            if (bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
                bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() && bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
            ) {
                // Check if animated WebP (contains VP8X chunk with animation bit set)
                return if (bytes.size >= 16 && bytes[12] == 'V'.code.toByte() && bytes[13] == 'P'.code.toByte() && bytes[14] == '8'.code.toByte() && bytes[15] == 'X'.code.toByte()) {
                    if (bytes.size >= 21 && (bytes[20].toInt() and 0x02) != 0) {
                        ANIMATED_WEBP
                    } else {
                        STATIC_WEBP
                    }
                } else {
                    STATIC_WEBP
                }
            }

            // SVG text probe (e.g. <?xml or <svg)
            val headerText = String(bytes.copyOfRange(0, minOf(bytes.size, 100)), Charsets.UTF_8).trim()
            if (headerText.startsWith("<?xml") || headerText.startsWith("<svg") || headerText.contains("<svg")) {
                return SVG
            }

            return UNKNOWN
        }
    }
}

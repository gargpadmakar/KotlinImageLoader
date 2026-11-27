package com.padmakar.imageloader.core.model

/**
 * Priority levels for image loading requests.
 * Used to sequence requests when bandwidth or thread pool capacity is constrained.
 */
enum class Priority {
    LOW,
    NORMAL,
    HIGH,
    IMMEDIATE
}

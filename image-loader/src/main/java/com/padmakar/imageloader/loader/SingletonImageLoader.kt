package com.padmakar.imageloader.loader

import android.content.Context
import com.padmakar.imageloader.core.ImageLoader

/**
 * Accessor for the default singleton [ImageLoader] instance across the application.
 *
 * This singleton does NOT hold any shared state or shared ImageStore; it acts solely as a stateless
 * engine provider for UI targets.
 */
object SingletonImageLoader {
    @Volatile
    private var instance: ImageLoader? = null

    /**
     * Retrieves the default [ImageLoader], initializing it lazily with [context] if necessary.
     */
    fun get(context: Context): ImageLoader {
        return instance ?: synchronized(this) {
            instance ?: RealImageLoader.Builder(context.applicationContext).build().also {
                instance = it
            }
        }
    }

    /**
     * Sets a custom [ImageLoader] for testing or custom engine configuration.
     */
    fun set(loader: ImageLoader) {
        synchronized(this) {
            instance = loader
        }
    }

    /**
     * Resets the singleton instance (primarily for test teardowns).
     */
    fun reset() {
        synchronized(this) {
            instance = null
        }
    }
}

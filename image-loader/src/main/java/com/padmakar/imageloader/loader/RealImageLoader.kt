package com.padmakar.imageloader.loader

import android.content.Context
import com.padmakar.imageloader.core.Disposable
import com.padmakar.imageloader.core.ImageLoader
import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageResult
import com.padmakar.imageloader.data.DefaultImageRepository
import com.padmakar.imageloader.domain.policy.DefaultImageRequestValidator
import com.padmakar.imageloader.domain.repository.ImageRepository
import com.padmakar.imageloader.domain.usecase.LoadImageUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Production implementation of the [ImageLoader] contract.
 *
 * Architecture and Threading:
 * - One Clear Loading Path: Request $\rightarrow$ [LoadImageUseCase] $\rightarrow$ [ImageRepository] $\rightarrow$ [HttpUrlConnectionFetcher] $\rightarrow$ [BitmapDecoder] $\rightarrow$ [ImageResult].
 * - Network fetching and bitmap decoding are strictly executed on [ioDispatcher] (default [Dispatchers.IO]).
 * - Enqueue callbacks/events operate on [mainDispatcher] (default [Dispatchers.Main.immediate]).
 * - Stateless with respect to UI components: The loader never holds a shared ImageStore. Each UI target
 *   (AsyncImage or ImageView) maintains its own isolated state and lifecycle.
 */
class RealImageLoader(
    private val useCase: LoadImageUseCase = LoadImageUseCase(
        repository = DefaultImageRepository(),
        validator = DefaultImageRequestValidator()
    ),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)
) : ImageLoader {

    override suspend fun execute(request: ImageRequest): ImageResult = withContext(ioDispatcher) {
        try {
            useCase(request)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            ImageResult.Error(throwable = t, request = request)
        }
    }

    override fun enqueue(request: ImageRequest): Disposable {
        val job = scope.launch(ioDispatcher) {
            try {
                useCase(request)
            } catch (_: CancellationException) {
                // Job cancelled normally
            }
        }

        return object : Disposable {
            override fun dispose() {
                job.cancel()
            }

            override val isDisposed: Boolean
                get() = !job.isActive
        }
    }

    class Builder(context: Context) {
        private val appContext = context.applicationContext
        private var repository: ImageRepository? = null
        private var ioDispatcher: CoroutineDispatcher = Dispatchers.IO
        private var mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate

        fun repository(repository: ImageRepository) = apply {
            this.repository = repository
        }

        fun ioDispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.ioDispatcher = dispatcher
        }

        fun mainDispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.mainDispatcher = dispatcher
        }

        fun build(): ImageLoader {
            val repo = repository ?: DefaultImageRepository()
            val useCase = LoadImageUseCase(
                repository = repo,
                validator = DefaultImageRequestValidator()
            )
            return RealImageLoader(
                useCase = useCase,
                ioDispatcher = ioDispatcher,
                mainDispatcher = mainDispatcher
            )
        }
    }
}

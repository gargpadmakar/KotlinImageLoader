package com.padmakar.imageloader.mvi

import com.padmakar.imageloader.core.model.ImageRequest
import com.padmakar.imageloader.core.model.ImageResult
import com.padmakar.imageloader.domain.usecase.LoadImageUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Reactive MVI store orchestrating [ImageIntent], emitting immutable [ImageState] via [StateFlow],
 * and dispatching one-off [ImageEffect] events.
 */
interface ImageStore {
    /** Current observable state flow. */
    val state: StateFlow<ImageState>

    /** Single-shot side effect flow. */
    val effect: Flow<ImageEffect>

    /** Dispatches an intent to the store. */
    fun dispatch(intent: ImageIntent)
}

/**
 * Default implementation of [ImageStore].
 */
class DefaultImageStore(
    private val loadImageUseCase: LoadImageUseCase,
    private val reducer: ImageReducer = DefaultImageReducer(),
    private val coroutineScope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ImageStore {

    private val _state = MutableStateFlow<ImageState>(ImageState.Idle)
    override val state: StateFlow<ImageState> = _state.asStateFlow()

    private val _effect = Channel<ImageEffect>(capacity = Channel.BUFFERED)
    override val effect: Flow<ImageEffect> = _effect.receiveAsFlow()

    private var activeJob: Job? = null

    override fun dispatch(intent: ImageIntent) {
        when (intent) {
            is ImageIntent.Load -> executeLoad(intent.request)
            is ImageIntent.Retry -> executeLoad(intent.request)
            is ImageIntent.Cancel -> cancelLoading()
        }
    }

    private fun executeLoad(request: ImageRequest) {
        activeJob?.cancel()
        _state.value = reducer.reduce(_state.value, ImageAction.StartLoading(request))

        activeJob = coroutineScope.launch(dispatcher) {
            try {
                when (val result = loadImageUseCase(request)) {
                    is ImageResult.Success -> {
                        _state.value = reducer.reduce(_state.value, ImageAction.LoadingSuccess(result))
                        _effect.send(ImageEffect.SuccessEffect(result.dataSource))
                    }
                    is ImageResult.Error -> {
                        _state.value = reducer.reduce(_state.value, ImageAction.LoadingFailure(result))
                        _effect.send(
                            ImageEffect.ShowError(
                                message = result.throwable.message ?: "Unknown image load error",
                                cause = result.throwable
                            )
                        )
                    }
                }
            } catch (ce: CancellationException) {
                _state.value = reducer.reduce(_state.value, ImageAction.Reset)
                throw ce
            } catch (t: Throwable) {
                val errorResult = ImageResult.Error(throwable = t, request = request)
                _state.value = reducer.reduce(_state.value, ImageAction.LoadingFailure(errorResult))
                _effect.send(
                    ImageEffect.ShowError(
                        message = t.message ?: "Unexpected failure",
                        cause = t
                    )
                )
            }
        }
    }

    private fun cancelLoading() {
        activeJob?.cancel()
        activeJob = null
        _state.value = reducer.reduce(_state.value, ImageAction.Reset)
    }
}

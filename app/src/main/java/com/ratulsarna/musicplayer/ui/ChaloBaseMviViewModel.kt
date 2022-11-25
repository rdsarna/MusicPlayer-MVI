package com.ratulsarna.musicplayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.zophop.mvibase.ChaloBasePartialChange
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import timber.log.Timber

@Suppress("LeakingThis")
abstract class ChaloBaseMviViewModel<ViewIntent, ViewState, ViewSideEffect, Change : ChaloBasePartialChange<ViewState>> :
    ViewModel() {

    abstract fun initialViewState(): ViewState
    abstract fun Flow<ViewIntent>.toPartialChangeFlow(): Flow<Change>
    abstract fun changeToSideEffect(change: Change): List<ViewSideEffect>

    open fun raiseAnalyticsEventOnIntent(intent: ViewIntent) {}
    open fun raiseAnalyticsEventOnPartialChange(change: Change) {}

    private val _intentFlow = MutableSharedFlow<ViewIntent>()
    private val sideEffectChannel = Channel<ViewSideEffect>(Channel.BUFFERED)
    private fun Flow<ViewIntent>.raiseAnalyticsEventOnIntent(): Flow<ViewIntent> {
        return this.onEach {
            raiseAnalyticsEventOnIntent(it)
        }
    }
    private fun Flow<Change>.raiseAnalyticsEventOnPartialChange(): Flow<Change> {
        return this.onEach {
            raiseAnalyticsEventOnPartialChange(it)
        }
    }

    val viewState: StateFlow<ViewState>
    val sideEffects: Flow<ViewSideEffect> = sideEffectChannel.receiveAsFlow()

    init {
        val initialVS = initialViewState()
        viewState = _intentFlow
            .onEach { Timber.d("ViewIntent = $it") }
            .raiseAnalyticsEventOnIntent()
            .toPartialChangeFlow()
            .raiseAnalyticsEventOnPartialChange()
            .onEach { Timber.d("PartialChange = $it") }
            .toSideEffect()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .catch { Timber.e(it) }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                initialVS
            )
    }

    suspend fun processIntent(intent: ViewIntent) = _intentFlow.emit(intent)

    private fun Flow<Change>.toSideEffect(): Flow<Change> {
        return onEach { change ->
            changeToSideEffect(change).forEach {
                Timber.d("SideEffect = $it")
                sideEffectChannel.send(it)
            }
        }
    }
}

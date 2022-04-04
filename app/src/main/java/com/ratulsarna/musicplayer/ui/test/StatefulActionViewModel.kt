package com.ratulsarna.musicplayer.ui.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ratulsarna.musicplayer.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

class StatefulActionViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(TestState())
    private val _event = MutableSharedFlow<TestEffect>()

    val state = _state.asStateFlow()
    val event = _event.asSharedFlow()

    private val eventEmitter = MutableSharedFlow<TestEvent>()

    val singleThreadedContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    init {

    }

    fun send(event: TestEvent) {
        viewModelScope.launch {
            Timber.d("SPECIAL-- $event, ${Thread.currentThread()}")
            when (event) {
                TestEvent.TestEvent1 -> {
                    resultToViewState(TestResult("Loading"))
                    delay(8000)
                    resultToViewState(TestResult("$event"))
                }
                TestEvent.TestEvent2 -> {
                    viewModelScope.launch(Dispatchers.IO) {
                        delay(4000)
                        send(TestEvent.TestEvent3)
                    }
                    resultToViewState(TestResult("Loading2"))
                }
                TestEvent.TestEvent3 -> {
                    resultToViewState(TestResult("$event"))
                }
            }
        }
    }

    private fun resultToViewState(result: TestResult) {
        Timber.d("SPECIAL-- $result")
        _state.update {
            it.copy(test = result.test)
        }
    }

//    abstract suspend fun Action.reduce(): State

    protected fun emitEvent(event: TestEffect) {
        viewModelScope.launch {
            if (BuildConfig.DEBUG) {
                Timber.d("EVENT EMITTED: $event")
            }

            _event.emit(event)
        }
    }

//    private suspend fun handleAction(event: Event) {
//        viewModelScope.launch {
//            val currentState = _state.value
//            val newState = event.reduce()
//
//            _state.value = newState
//        }
//    }
}
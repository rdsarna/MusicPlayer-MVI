package com.ratulsarna.musicplayer.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun setupEventChannel(
    lifecycleOwner: LifecycleOwner,
    viewModel: MusicPlayerViewModel
): Channel<MusicPlayerEvent> {
    val eventChannel = remember { Channel<MusicPlayerEvent>(Channel.BUFFERED) }
    LaunchedEffect(
        key1 = eventChannel,
        key2 = lifecycleOwner,
    ) {
        eventChannel.receiveAsFlow()
            .onEach {
                viewModel.processInput(it)
            }
            .collect()
    }
    return eventChannel
}

@Composable
fun ViewEffects(
    lifecycleOwner: LifecycleOwner,
    viewModel: MusicPlayerViewModel,
) {
    val context = LocalContext.current
    val effectFlowLifecycleAware =
        remember(
            viewModel.viewEffects,
            lifecycleOwner
        ) {
            viewModel.viewEffects
                .flowWithLifecycle(
                    lifecycleOwner.lifecycle,
                    Lifecycle.State.STARTED
                )
        }
    LaunchedEffect(
        key1 = viewModel.viewEffects,
        key2 = lifecycleOwner,
    ) {
        effectFlowLifecycleAware.collect { effect ->
            when (effect) {
                is MusicPlayerEffect.ShowErrorEffect -> {
                    Toast.makeText(
                        context,
                        effect.errorMessage,
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
                is MusicPlayerEffect.ForceScreenOnEffect,
                MusicPlayerEffect.NoOpEffect -> {
                    // ignore
                }
            }
        }
    }
}

@Composable
fun LifecycleEvents(
    viewModel: MusicPlayerViewModel,
    lifecycleOwner: LifecycleOwner,
    eventChannel: Channel<MusicPlayerEvent>
) {
    LaunchedEffect(true) {
        viewModel.processInput(MusicPlayerEvent.UiCreateEvent)
    }
    // If `lifecycleOwner` changes, dispose and reset the effect
    DisposableEffect(lifecycleOwner) {
        // Create an observer that triggers our remembered callbacks
        // for sending analytics events
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                    eventChannel.trySend(MusicPlayerEvent.UiCreateEvent)
                }
                Lifecycle.Event.ON_START -> {
                    eventChannel.trySend(MusicPlayerEvent.UiStartEvent)
                }
                Lifecycle.Event.ON_STOP -> {
                    eventChannel.trySend(MusicPlayerEvent.UiStopEvent)
                }
                else -> {
                    // ignore
                }
            }
        }
        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)
        // When the effect leaves the Composition, remove the observer
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
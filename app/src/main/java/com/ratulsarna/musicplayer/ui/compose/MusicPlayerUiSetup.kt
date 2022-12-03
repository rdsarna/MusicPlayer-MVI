package com.ratulsarna.musicplayer.ui.compose

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
import com.ratulsarna.musicplayer.ui.MusicPlayerIntent
import com.ratulsarna.musicplayer.ui.MusicPlayerSideEffect
import com.ratulsarna.musicplayer.ui.vm.MusicPlayerViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun setupEventChannel(
    lifecycleOwner: LifecycleOwner,
    viewModel: MusicPlayerViewModel
): Channel<MusicPlayerIntent> {
    val eventChannel = remember { Channel<MusicPlayerIntent>(Channel.BUFFERED) }
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
            viewModel.sideEffects,
            lifecycleOwner
        ) {
            viewModel.sideEffects
                .flowWithLifecycle(
                    lifecycleOwner.lifecycle,
                    Lifecycle.State.STARTED
                )
        }
    LaunchedEffect(
        key1 = viewModel.sideEffects,
        key2 = lifecycleOwner,
    ) {
        effectFlowLifecycleAware.collect { effect ->
            when (effect) {
                is MusicPlayerSideEffect.ShowErrorSideEffect -> {
                    Toast.makeText(
                        context,
                        effect.errorMessage,
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            }
        }
    }
}

@Composable
fun LifecycleEvents(
    viewModel: MusicPlayerViewModel,
    lifecycleOwner: LifecycleOwner,
    eventChannel: Channel<MusicPlayerIntent>
) {
    // If `lifecycleOwner` changes, dispose and reset the effect
    DisposableEffect(lifecycleOwner) {
        // Create an observer that triggers our remembered callbacks
        // for sending analytics events
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    eventChannel.trySend(MusicPlayerIntent.UiStartIntent)
                }
                Lifecycle.Event.ON_STOP -> {
                    eventChannel.trySend(MusicPlayerIntent.UiStopIntent)
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
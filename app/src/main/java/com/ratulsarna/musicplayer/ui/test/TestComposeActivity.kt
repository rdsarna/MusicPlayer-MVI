package com.ratulsarna.musicplayer.ui.test

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.*
import com.ratulsarna.musicplayer.R
import com.ratulsarna.musicplayer.ui.ui.theme.BottomBlue
import com.ratulsarna.musicplayer.ui.ui.theme.MidBlue
import com.ratulsarna.musicplayer.ui.ui.theme.MusicPlayerTheme
import com.ratulsarna.musicplayer.ui.ui.theme.TopBlue
import com.ratulsarna.musicplayer.utils.viewModelProvider
import dagger.android.support.DaggerAppCompatActivity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.roundToInt

class TestComposeActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: TestViewModel by lazy {
        viewModelProvider(viewModelFactory)
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(
            window,
            false
        )
        setContent {
            MusicPlayerTheme {
                TestScreen(viewModel = viewModel)
            }
        }
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun TestScreen(
    modifier: Modifier = Modifier,
    viewModel: TestViewModel,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {
    val stateFlowLifecycleAware =
        remember(
            viewModel.viewState,
            lifecycleOwner
        ) {
            viewModel.viewState.flowWithLifecycle(
                lifecycleOwner.lifecycle,
                Lifecycle.State.STARTED
            )
        }
    val musicPlayerViewState by stateFlowLifecycleAware.collectAsState(viewModel.viewState.value)

    val eventChannel = remember { Channel<TestIntent>(Channel.BUFFERED) }
    val eventFlowLifecycleAware =
        remember(
            eventChannel,
            lifecycleOwner
        ) {
            eventChannel.receiveAsFlow()
                .flowWithLifecycle(
                    lifecycleOwner.lifecycle,
                    Lifecycle.State.STARTED
                )
        }
    LaunchedEffect(
        key1 = eventChannel,
        key2 = lifecycleOwner,
    ) {
        eventFlowLifecycleAware.onEach {
            viewModel.processIntent(it)
        }
            .collect()
    }

    LaunchedEffect(true) {
        viewModel.processIntent(TestIntent.UiCreateIntent)
    }
    // If `lifecycleOwner` changes, dispose and reset the effect
    DisposableEffect(lifecycleOwner) {
        // Create an observer that triggers our remembered callbacks
        // for sending analytics events
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                eventChannel.trySend(TestIntent.UiStartIntent)
            } else if (event == Lifecycle.Event.ON_STOP) {
                eventChannel.trySend(TestIntent.UiStopIntent)
            }
        }
        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)
        // When the effect leaves the Composition, remove the observer
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    TestScreenContent(
        modifier = modifier
            .fillMaxSize(),
        state = musicPlayerViewState,
        onCLick = {
            Timber.d("-----------here1")
            eventChannel.trySend(TestIntent.CountIntent)
        }
    )
}

@Composable
fun TestScreenContent(
    modifier: Modifier = Modifier,
    state: TestState,
    onCLick: () -> Unit
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.size(32.dp))
        Button(onClick = onCLick) {
            Text(text = "count=${state.count}")
        }
        Spacer(modifier = Modifier.size(32.dp))
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_2,
    showSystemUi = true
)
@Composable
fun DefaultPreview() {
    MusicPlayerTheme {
        TestScreenContent(
            state = TestState(1),
            onCLick = {}
        )
    }
}
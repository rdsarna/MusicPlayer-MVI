package com.ratulsarna.musicplayer.ui

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.roundToInt

class ComposeActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: MusicPlayerViewModel by lazy {
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
                MusicPlayerScreen(viewModel = viewModel)
            }
        }
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun MusicPlayerScreen(
    modifier: Modifier = Modifier,
    viewModel: MusicPlayerViewModel,
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

    val eventChannel = remember { Channel<MusicPlayerEvent>(Channel.BUFFERED) }
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
            viewModel.processInput(it)
        }
            .collect()
    }

    LaunchedEffect(true) {
        viewModel.processInput(MusicPlayerEvent.UiCreateEvent)
    }
    val scope = rememberCoroutineScope()
    // If `lifecycleOwner` changes, dispose and reset the effect
    DisposableEffect(lifecycleOwner) {
        // Create an observer that triggers our remembered callbacks
        // for sending analytics events
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                eventChannel.trySend(MusicPlayerEvent.UiStartEvent)
                scope.launch {
                    delay(1000)
                    eventChannel.send(MusicPlayerEvent.PlayEvent)
                }
            } else if (event == Lifecycle.Event.ON_STOP) {
                eventChannel.trySend(MusicPlayerEvent.UiStopEvent)
            }
        }
        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)
        // When the effect leaves the Composition, remove the observer
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    MusicPlayerScreenContent(
        modifier = modifier
            .fillMaxSize(),
        state = musicPlayerViewState,
        sendUiEvent = {
            Timber.d("-----------here1")
            eventChannel.trySend(it)
        }
    )
}

@Composable
fun MusicPlayerScreenContent(
    modifier: Modifier = Modifier,
    state: MusicPlayerViewState,
    sendUiEvent: (MusicPlayerEvent) -> Unit
) {
    Column(
        modifier = modifier.background(
            brush = Brush.radialGradient(
                colors = listOf(
                    BottomBlue,
                    MidBlue,
                    TopBlue,
                ),
                center = Offset(
                    0f,
                    Float.POSITIVE_INFINITY
                ),
                radius = 2800f
            )
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
//        AlbumArt(
//            modifier = Modifier
//                .fillMaxHeight(.55f)
//                .fillMaxWidth()
//                .padding(
//                    horizontal = 48.dp,
//                    vertical = 16.dp
//                ),
//            state.albumArt,
//        )
        Spacer(modifier = Modifier.height(32.dp))
        SongTitle(
            title = "${state.songTitle}+${state.elapsedTime}",
            subTitle = state.songInfoLabel
        )
        Spacer(modifier = Modifier.height(32.dp))
//        SongSeekBar(
//            modifier = Modifier
//                .wrapContentHeight()
//                .fillMaxWidth(),
//            currentTimeLabel = state.elapsedTime.getTimeLabel(),
//            totalDurationTimeLabel = state.totalDuration.toInt()
//                .getTimeLabel(),
//            totalDuration = state.totalDuration,
//            onSliderValueChange = {
//                Timber.d("-----------here10")
//                sendUiEvent(MusicPlayerEvent.SeekToEvent(it.roundToInt()))
//            },
//            sliderValue = min(state.elapsedTime.toFloat(), state.totalDuration),
//        )
//        Spacer(modifier = Modifier.height(8.dp))
//        Controls(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 32.dp),
//            showPlayButton = state.playing.not(),
//            onPlay = { sendUiEvent(MusicPlayerEvent.PlayEvent) },
//            onPause = { sendUiEvent(MusicPlayerEvent.PauseEvent) },
//            onNext = { sendUiEvent(MusicPlayerEvent.NextSongEvent) },
//            onPrevious = { sendUiEvent(MusicPlayerEvent.PreviousSongEvent) },
//            onSeekForward = { sendUiEvent(MusicPlayerEvent.SeekForwardEvent) },
//            onSeekBackward = { sendUiEvent(MusicPlayerEvent.SeekBackwardEvent) },
//        )
//        Spacer(modifier = Modifier.height(32.dp))
//        Box(
//            modifier = Modifier
//                .height(
//                    32.dp + WindowInsets.navigationBars
//                        .asPaddingValues()
//                        .calculateBottomPadding()
//                )
//                .fillMaxWidth()
//        )
    }
}

@Composable
fun AlbumArt(modifier: Modifier, @DrawableRes albumRes: Int) {
    Box(
        modifier = modifier
    ) {
        Card(
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .aspectRatio(1f)
                .align(Alignment.BottomCenter)
        ) {
            Image(
                painter = painterResource(id = albumRes),
                contentDescription = "",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun SongTitle(modifier: Modifier = Modifier, title: String, subTitle: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        Text(
            text = subTitle,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongSeekBar(
    modifier: Modifier = Modifier,
    currentTimeLabel: String,
    totalDurationTimeLabel: String,
    totalDuration: Float,
    onSliderValueChange: (Float) -> Unit,
    sliderValue: Float,
) {
    Box(
        modifier = modifier
    ) {
        TimeStampText(
            text = currentTimeLabel,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .align(Alignment.TopStart)
        )
        TimeStampText(
            text = totalDurationTimeLabel,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .align(Alignment.TopEnd)
        )
        Slider(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
            value = sliderValue,
            onValueChange = onSliderValueChange,
            valueRange = 0f..totalDuration,
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                inactiveTrackColor = Color(0x82FFFFFF),
                activeTrackColor = Color.White,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            thumb = {

            }
        )
    }
}

@Composable
fun TimeStampText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Color.White,
        modifier = modifier
    )
}

@Composable
fun Controls(
    modifier: Modifier = Modifier,
    showPlayButton: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ControlImage(
            id = R.drawable.ic_seek_backward,
            onClick = { onSeekBackward() },
            contentScale = ContentScale.Inside
        )
        ControlImage(
            modifier = Modifier.padding(end = 8.dp),
            id = R.drawable.ic_previous,
            onClick = { onPrevious() },
            contentScale = ContentScale.Inside
        )
        Box {
            if (showPlayButton) {
                ControlImage(
                    id = R.drawable.ic_play,
                    onClick = { onPlay() },
                    contentScale = ContentScale.Fit
                )
            } else {
                ControlImage(
                    id = R.drawable.ic_pause,
                    onClick = { onPause() },
                    contentScale = ContentScale.Fit
                )
            }
        }
        ControlImage(
            modifier = Modifier.padding(start = 8.dp),
            id = R.drawable.ic_next,
            onClick = { onNext() },
            contentScale = ContentScale.Inside
        )
        ControlImage(
            id = R.drawable.ic_seek_forward,
            onClick = { onSeekForward() },
            contentScale = ContentScale.Inside
        )
    }
}

@Composable
fun ControlImage(
    modifier: Modifier = Modifier,
    @DrawableRes id: Int,
    onClick: () -> Unit,
    contentScale: ContentScale,
    contentDescription: String = "",
) {
    Button(
        onClick = { onClick() },
        modifier = modifier.size(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Image(
            painter = painterResource(id = id),
            contentDescription = contentDescription,
            contentScale = contentScale,
        )
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
        MusicPlayerScreenContent(
            state = MusicPlayerViewState.INITIAL,
            sendUiEvent = {}
        )
    }
}

fun Int.getTimeLabel(): String {
    val minutes = this / (1000 * 60)
    val seconds = this / 1000 % 60
    return "$minutes:${if (seconds < 10) "0$seconds" else seconds}"
}
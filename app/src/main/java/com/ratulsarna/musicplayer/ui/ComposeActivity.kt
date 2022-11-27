@file:OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
)

package com.ratulsarna.musicplayer.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.*
import com.ratulsarna.musicplayer.R
import com.ratulsarna.musicplayer.ui.ui.theme.*
import com.ratulsarna.musicplayer.utils.viewModelProvider
import dagger.android.support.DaggerAppCompatActivity
import fr.swarmlab.beta.ui.screens.components.material3.BottomSheetScaffold
import fr.swarmlab.beta.ui.screens.components.material3.BottomSheetValue
import fr.swarmlab.beta.ui.screens.components.material3.rememberBottomSheetScaffoldState
import fr.swarmlab.beta.ui.screens.components.material3.rememberBottomSheetState
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.roundToInt

class ComposeActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: MusicPlayerViewModel by lazy {
        viewModelProvider(viewModelFactory)
    }

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
    // ViewState
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

    // ViewEvents
    val eventChannel = setupEventChannel(
        lifecycleOwner,
        viewModel
    )

    // ViewEffects
    ViewEffects(
        lifecycleOwner,
        viewModel,
    )

    LifecycleEvents(
        viewModel,
        lifecycleOwner,
        eventChannel
    )

    val sheetState = rememberBottomSheetState(initialValue = BottomSheetValue.Collapsed)
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState
    )
    val scope = rememberCoroutineScope()
    val statusBarHeight = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetShape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp
        ),
        sheetContent = {
            Column(
                modifier = Modifier
                    .height(
                        LocalConfiguration.current.screenHeightDp.dp + statusBarHeight - 100.dp
                    )
                    .navigationBarsPadding()
                    .background(LightGrayTransparent)
            ) {
                Text(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (sheetState.isCollapsed) {
                                scope.launch { sheetState.expand() }
                            } else {
                                scope.launch { sheetState.collapse() }
                            }
                        },
                    text = "Up Next",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    content = {
                        items(musicPlayerViewState.playlist) { playlistSong ->
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 32.dp,
                                    vertical = 16.dp
                                )
                            ) {
                                Image(
                                    modifier = Modifier
                                        .height(48.dp)
                                        .aspectRatio(1f),
                                    painter = painterResource(id = playlistSong.albumArt),
                                    contentDescription = ""
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.CenterVertically),
                                ) {
                                    Text(
                                        text = playlistSong.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                )
            }
        },
        sheetBackgroundColor = Color.Transparent,
        sheetPeekHeight = 48.dp + WindowInsets.navigationBars
            .asPaddingValues()
            .calculateBottomPadding()
    ) {
        MusicPlayerScreenContent(
            modifier = modifier
                .fillMaxSize(),
            state = musicPlayerViewState,
            statusBarHeight = statusBarHeight,
            sendUiEvent = {
                eventChannel.trySend(it)
            }
        )
    }
}

@Composable
fun MusicPlayerScreenContent(
    modifier: Modifier = Modifier,
    state: MusicPlayerViewState,
    sendUiEvent: (MusicPlayerEvent) -> Unit,
    statusBarHeight: Dp = 0.dp,
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
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
            AlbumArt(
                modifier = Modifier
                    .fillMaxHeight(.55f)
                    .fillMaxWidth()
                    .padding(
                        horizontal = 48.dp,
                        vertical = 16.dp
                    ),
                state.albumArt,
            )
            Spacer(modifier = Modifier.height(32.dp))
            SongTitle(
                title = state.songTitle,
                subTitle = state.songInfoLabel
            )
            Spacer(modifier = Modifier.height(32.dp))
            SongSeekBar(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth(),
                currentTimeLabel = state.elapsedTime.getTimeLabel(),
                totalDurationTimeLabel = state.totalDuration.toInt()
                    .getTimeLabel(),
                totalDuration = state.totalDuration,
                onSliderValueChange = {
                    sendUiEvent(MusicPlayerEvent.SeekToEvent(it.roundToInt()))
                },
                sliderValue = min(
                    state.elapsedTime.toFloat(),
                    state.totalDuration
                ),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Controls(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                showPlayButton = state.playing.not(),
                onPlay = { sendUiEvent(MusicPlayerEvent.PlayEvent) },
                onPause = { sendUiEvent(MusicPlayerEvent.PauseEvent) },
                onNext = { sendUiEvent(MusicPlayerEvent.NextSongEvent) },
                onPrevious = { sendUiEvent(MusicPlayerEvent.PreviousSongEvent) },
                onSeekForward = { sendUiEvent(MusicPlayerEvent.SeekForwardEvent) },
                onSeekBackward = { sendUiEvent(MusicPlayerEvent.SeekBackwardEvent) },
            )
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .height(
                        48.dp + WindowInsets.navigationBars
                            .asPaddingValues()
                            .calculateBottomPadding()
                    )
                    .fillMaxWidth()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp + statusBarHeight)
                .align(Alignment.TopCenter)
                .background(Color.Red)
                .statusBarsPadding()
        )
    }
}

@Composable
fun AlbumArt(modifier: Modifier, @DrawableRes albumRes: Int) {
    Box(
        modifier = modifier
    ) {
        Image(
            painter = painterResource(id = albumRes),
            contentDescription = "",
            modifier = Modifier
                .aspectRatio(1f)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(8.dp))
        )
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
                thumbColor = Color.White,
                inactiveTrackColor = Color(0x82FFFFFF),
                activeTrackColor = Color.White,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
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
            androidx.compose.animation.AnimatedVisibility(
                visible = showPlayButton,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                ControlImage(
                    id = R.drawable.ic_play,
                    onClick = { onPlay() },
                    contentScale = ContentScale.Fit
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = !showPlayButton,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
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
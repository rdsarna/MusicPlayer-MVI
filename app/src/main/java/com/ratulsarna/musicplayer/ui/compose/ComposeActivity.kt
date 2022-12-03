@file:OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
)

package com.ratulsarna.musicplayer.ui.compose

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.core.view.WindowCompat
import androidx.lifecycle.*
import com.ratulsarna.musicplayer.R
import com.ratulsarna.musicplayer.ui.LifecycleEvents
import com.ratulsarna.musicplayer.ui.MusicPlayerEvent
import com.ratulsarna.musicplayer.ui.MusicPlayerViewModel
import com.ratulsarna.musicplayer.ui.MusicPlayerViewState
import com.ratulsarna.musicplayer.ui.ViewEffects
import com.ratulsarna.musicplayer.ui.model.PlaylistViewSong
import com.ratulsarna.musicplayer.ui.setupEventChannel
import com.ratulsarna.musicplayer.ui.ui.theme.*
import com.ratulsarna.musicplayer.utils.viewModelProvider
import dagger.android.support.DaggerAppCompatActivity
import fr.swarmlab.beta.ui.screens.components.material3.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max
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
    val statusBarHeight = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()
    val navigationBarHeight = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()

    val scope = rememberCoroutineScope()
    BackHandler(enabled = sheetState.isExpanded) {
        scope.launch { sheetState.collapse() }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetShape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp
        ),
        sheetContent = {
            PlaylistBottomSheetContent(
                statusBarHeight = statusBarHeight,
                navigationBarHeight = navigationBarHeight,
                sheetState = sheetState,
                playlist = musicPlayerViewState.playlist,
                currentSong = musicPlayerViewState.currentPlaylistSong,
                bottomSheetProgressFractionProvider = { scaffoldState.currentFraction },
                sendUiEvent = { eventChannel.trySend(it) }
            )
        },
        sheetBackgroundColor = Color.Transparent,
        sheetPeekHeight = 48.dp + navigationBarHeight
    ) {
        MusicPlayerScreenContent(
            modifier = modifier.fillMaxSize(),
            state = musicPlayerViewState,
            statusBarHeight = statusBarHeight,
            navigationBarHeight = navigationBarHeight,
            bottomSheetProgressFractionProvider = { scaffoldState.currentFraction },
            sendUiEvent = { eventChannel.trySend(it) }
        )
    }
}


@Composable
fun MusicPlayerScreenContent(
    modifier: Modifier = Modifier,
    state: MusicPlayerViewState,
    statusBarHeight: Dp = 0.dp,
    navigationBarHeight: Dp = 0.dp,
    bottomSheetProgressFractionProvider: () -> Float = { 1f },
    sendUiEvent: (MusicPlayerEvent) -> Unit,
) {
    val controlEventsProvider = remember { ControlEventsProvider(sendUiEvent) }
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
                        horizontal = 16.dp,
                    )
                    .padding(
                        bottom = 8.dp,
                        top = 8.dp + statusBarHeight
                    ),
                albumRes = state.albumArt,
                bottomSheetProgressFractionProvider = bottomSheetProgressFractionProvider,
            )
            Spacer(modifier = Modifier.height(32.dp))
            SongTitle(
                modifier = Modifier.graphicsLayer {
                    alpha = max(
                        0f,
                        1f - bottomSheetProgressFractionProvider() * 2
                    )
                },
                title = state.songTitle,
                subTitle = state.songInfoLabel
            )
            Spacer(modifier = Modifier.height(32.dp))
            SongSeekBar(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = max(
                            0f,
                            1f - bottomSheetProgressFractionProvider() * 2
                        )
                    },
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
                    .padding(horizontal = 32.dp)
                    .graphicsLayer {
                        alpha = max(
                            0f,
                            1f - bottomSheetProgressFractionProvider() * 2
                        )
                    },
                showPlayButton = state.playing.not(),
                controlEventsProvider,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .height(48.dp + navigationBarHeight)
                    .fillMaxWidth()
            )
        }
        TopSlidingInHeader(
            modifier = Modifier.padding(top = statusBarHeight),
            controlEventsProvider = controlEventsProvider,
            showPlayButton = state.playing.not(),
            bottomSheetProgressFractionProvider = bottomSheetProgressFractionProvider
        )
    }
}

@Composable
fun TopSlidingInHeader(
    modifier: Modifier = Modifier,
    controlEventsProvider: ControlEventsProvider,
    bottomSheetProgressFractionProvider: () -> Float = { 1f },
    showPlayButton: Boolean,
) {
    SlidingHeaderLayout(
        modifier = modifier,
        bottomSheetProgressFractionProvider = bottomSheetProgressFractionProvider
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(100.dp)
        ) {
            Controls(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                showPlayButton = showPlayButton,
                controlEventsProvider = controlEventsProvider,
            )
        }
    }
}



@Composable
fun AlbumArt(
    modifier: Modifier = Modifier,
    albumRes: Int,
    bottomSheetProgressFractionProvider: () -> Float = { 1f },
) {
    Box(
        modifier = modifier
    ) {
        CollapsingImageLayout(
            bottomSheetProgressFractionProvider = bottomSheetProgressFractionProvider
        ) {
            Image(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                painter = painterResource(id = albumRes),
                contentDescription = "",
            )
        }
    }
}

@Composable
fun SongTitle(modifier: Modifier = Modifier, title: String, subTitle: String) {
    Column(
        modifier = modifier,
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
    controlEventsProvider: ControlEventsProvider,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ControlImage(
            id = R.drawable.ic_seek_backward,
            onClick = controlEventsProvider.onSeekBackward,
            contentScale = ContentScale.Inside
        )
        ControlImage(
            modifier = Modifier.padding(end = 8.dp),
            id = R.drawable.ic_previous,
            onClick = controlEventsProvider.onPrevious,
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
                    onClick = controlEventsProvider.onPlay,
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
                    onClick = controlEventsProvider.onPause,
                    contentScale = ContentScale.Fit
                )
            }
        }
        ControlImage(
            modifier = Modifier.padding(start = 8.dp),
            id = R.drawable.ic_next,
            onClick = controlEventsProvider.onNext,
            contentScale = ContentScale.Inside
        )
        ControlImage(
            id = R.drawable.ic_seek_forward,
            onClick = controlEventsProvider.onSeekForward,
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

val BottomSheetScaffoldState.currentFraction: Float
    get() {
        val fraction = bottomSheetState.progress.fraction
        val from = bottomSheetState.progress.from
        val to = bottomSheetState.progress.to
        return when {
            from == BottomSheetValue.Collapsed && to == BottomSheetValue.Collapsed -> 0f
            from == BottomSheetValue.Expanded && to == BottomSheetValue.Expanded -> 1f
            from == BottomSheetValue.Collapsed && to == BottomSheetValue.Expanded -> fraction
            else -> 1f - fraction
        }
    }
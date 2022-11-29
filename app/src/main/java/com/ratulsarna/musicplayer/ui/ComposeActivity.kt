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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
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
import com.ratulsarna.musicplayer.ui.ui.theme.*
import com.ratulsarna.musicplayer.utils.viewModelProvider
import dagger.android.support.DaggerAppCompatActivity
import fr.swarmlab.beta.ui.screens.components.material3.*
import kotlinx.coroutines.launch
import timber.log.Timber
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

private val ExpandedImageSize = 300.dp
private val CollapsedImageSize = 80.dp

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
    val navigationBarHeight = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
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
                    .background(LightGrayTransparent)
                    .navigationBarsPadding()
            ) {
                Text(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
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
                NavigationBarPaddingBox(
                    modifier = Modifier
                        .height(navigationBarHeight)
                        .fillMaxWidth()
                        .background(Color.Black),
                    scaffoldState.currentFraction
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
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(6.dp)),
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
                                    Text(
                                        text = playlistSong.infoLabel,
                                        style = MaterialTheme.typography.bodyMedium,
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
            navigationBarHeight = navigationBarHeight,
            bottomSheetProgressFractionProvider = { scaffoldState.currentFraction },
            sendUiEvent = {
                eventChannel.trySend(it)
            }
        )
    }
}

@Composable
fun NavigationBarPaddingBox(
    modifier: Modifier = Modifier,
    bottomSheetProgressFraction: Float = 1f
) {
    Box(
        modifier = modifier
            .alpha(1f - bottomSheetProgressFraction)
    )
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
                    .padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )
                    .statusBarsPadding(),
                albumRes = state.albumArt,
                bottomSheetProgressFractionProvider = bottomSheetProgressFractionProvider,
            )
            Spacer(modifier = Modifier.height(32.dp))
            SongTitle(
                modifier = Modifier.alpha(
                    max(
                        0f,
                        1f - bottomSheetProgressFractionProvider() * 2
                    )
                ),
                title = state.songTitle,
                subTitle = state.songInfoLabel
            )
            Spacer(modifier = Modifier.height(32.dp))
            SongSeekBar(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .alpha(
                        max(
                            0f,
                            1f - bottomSheetProgressFractionProvider() * 2
                        )
                    ),
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
                    .alpha(
                        max(
                            0f,
                            1f - bottomSheetProgressFractionProvider() * 2
                        )
                    ),
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
                    .height(48.dp + navigationBarHeight)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
fun AlbumArt(
    modifier: Modifier,
    @DrawableRes albumRes: Int,
    bottomSheetProgressFractionProvider: () -> Float
) {
    CollapsingImageLayout(
        modifier = modifier,
        bottomSheetProgressFractionProvider = bottomSheetProgressFractionProvider
    ) {
        Image(
            painter = painterResource(id = albumRes),
            contentDescription = "",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
        )
    }
}

@Composable
private fun CollapsingImageLayout(
    bottomSheetProgressFractionProvider: () -> Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        check(measurables.size == 1)

        val collapseFraction = bottomSheetProgressFractionProvider()

        val imageMaxSize =
            min(
                ExpandedImageSize.roundToPx(),
                constraints.maxWidth
            )
        val imageMinSize =
            max(
                CollapsedImageSize.roundToPx(),
                constraints.minWidth
            )
        val imageWidth =
            androidx.compose.ui.util.lerp(
                imageMaxSize,
                imageMinSize,
                collapseFraction
            )
        val imagePlaceable =
            measurables[0].measure(
                Constraints.fixed(
                    imageWidth,
                    imageWidth
                )
            )
        
        val imageY =
            lerp(
                constraints.maxHeight.toDp().div(8f),
                0.dp,
                collapseFraction
            ).roundToPx()
        val imageX = androidx.compose.ui.util.lerp(
            (constraints.maxWidth - imageWidth) / 2, // centered when expanded
            0, // right aligned when collapsed
            collapseFraction
        )
        layout(
            width = constraints.maxWidth,
            height = imageY + imageWidth
        ) {
            imagePlaceable.placeRelative(
                imageX,
                imageY
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
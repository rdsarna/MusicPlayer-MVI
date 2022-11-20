package com.ratulsarna.musicplayer.ui

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.ratulsarna.musicplayer.R
import com.ratulsarna.musicplayer.ui.ui.theme.BottomBlue
import com.ratulsarna.musicplayer.ui.ui.theme.MidBlue
import com.ratulsarna.musicplayer.ui.ui.theme.MusicPlayerTheme
import com.ratulsarna.musicplayer.ui.ui.theme.TopBlue

@OptIn(ExperimentalMaterial3Api::class)
class ComposeActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(
            window,
            false
        )
        setContent {
            MusicPlayerTheme {
                MusicPlayerScreen(
                    Modifier
                )
            }
        }
    }
}

@Composable
fun MusicPlayerScreen(modifier: Modifier) {
    Column(
        modifier = modifier
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
            R.drawable.levitating_album_art,
            modifier = Modifier
                .fillMaxHeight(.55f)
                .fillMaxWidth()
                .padding(
                    horizontal = 48.dp,
                    vertical = 16.dp
                )
        )
        Spacer(modifier = Modifier.height(32.dp))
        SongTitle(
            title = "Levitating",
            subTitle = "Dua Lipa feat. DaBaby | 2020"
        )
        Spacer(modifier = Modifier.height(32.dp))
        var sliderValue by remember { mutableStateOf(0f) }
        SongSeekBar(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth(),
            currentTimeLabel = "1:22",
            totalDurationTimeLabel = "3:43",
            totalDuration = 223f,
            onSliderValueChange = { sliderValue = it },
            sliderValue = sliderValue
        )
        Spacer(modifier = Modifier.height(8.dp))
        Controls(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            onPlayPause = {},
            onNext = {},
            onPrevious = {},
            onSeekForward = {},
            onSeekBackward = {},
        )
        Spacer(modifier = Modifier.height(32.dp))
        Box(
            modifier = Modifier
                .height(
                    32.dp + WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding()
                )
                .fillMaxWidth()
        )
    }
}

@Composable
fun AlbumArt(@DrawableRes albumRes: Int, modifier: Modifier) {
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
fun SongTitle(title: String, subTitle: String, modifier: Modifier = Modifier) {
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
            value = sliderValue,
            onValueChange = onSliderValueChange,
            valueRange = 0f..totalDuration,
            colors = SliderDefaults.colors(
                thumbColor = Color.White
            ),
            modifier = Modifier
                .padding(
                    horizontal = 16.dp,
                )
                .padding(top = 8.dp)
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
    onPlayPause: () -> Unit,
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
            onClick = onSeekBackward,
            contentScale = ContentScale.Inside
        )
        ControlImage(
            modifier = Modifier.padding(end = 8.dp),
            id = R.drawable.ic_previous,
            onClick = onPrevious,
            contentScale = ContentScale.Inside
        )
        ControlImage(
            id = R.drawable.ic_play,
            onClick = onPlayPause,
            contentScale = ContentScale.Fit
        )
        ControlImage(
            modifier = Modifier.padding(start = 8.dp),
            id = R.drawable.ic_next,
            onClick = onNext,
            contentScale = ContentScale.Inside
        )
        ControlImage(
            id = R.drawable.ic_seek_forward,
            onClick = onSeekForward,
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
        onClick = { /*TODO*/ },
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
        MusicPlayerScreen(
            Modifier
        )
    }
}
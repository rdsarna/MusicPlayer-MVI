package com.ratulsarna.musicplayer.ui

import android.graphics.fonts.FontStyle
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

class ComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(
            window,
            false
        )
        setContent {
            MusicPlayerTheme {
                MusicPlayerScreen()
            }
        }
    }
}

@Composable
fun MusicPlayerScreen() {
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
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxHeight(.7f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(fraction = .85f)
            ) {
                Card(
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .align(Alignment.BottomCenter)
                        .padding(48.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.levitating_album_art),
                        contentDescription = "",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = "Levitating",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Text(
                    text = "Dua Lipa feat. DaBaby | 2020",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3A,
    showSystemUi = true
)
@Composable
fun DefaultPreview() {
    MusicPlayerTheme {
        MusicPlayerScreen()
    }
}
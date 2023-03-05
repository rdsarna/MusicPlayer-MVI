package com.ratulsarna.musicplayer.ui.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ratulsarna.musicplayer.ui.MusicPlayerIntent
import com.ratulsarna.musicplayer.ui.model.PlaylistViewSong
import com.ratulsarna.musicplayer.ui.ui.theme.LightGrayDarkerTransparent
import com.ratulsarna.musicplayer.ui.ui.theme.LightGrayTransparent
import fr.swarmlab.beta.ui.screens.components.material3.BottomSheetState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

@Composable
fun PlaylistBottomSheetContent(
    navigationBarHeight: Dp,
    sheetState: BottomSheetState,
    playlist: ImmutableList<PlaylistViewSong>,
    currentSong: PlaylistViewSong?,
    bottomSheetProgressFractionProvider: () -> Float,
    sendUiEvent: (MusicPlayerIntent) -> Unit,
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .height(
                LocalConfiguration.current.screenHeightDp.dp + navigationBarHeight - 100.dp
            )
            .background(LightGrayTransparent)
    ) {
        BottomSheetHeader(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            onClick = {
                if (sheetState.isCollapsed) {
                    scope.launch { sheetState.expand() }
                } else {
                    scope.launch { sheetState.collapse() }
                }
            }
        )
        Spacer(
            modifier = Modifier
                .height(navigationBarHeight.times(1f - bottomSheetProgressFractionProvider()))
                .fillMaxWidth()
        )
        Playlist(
            modifier = Modifier
                .fillMaxSize(),
            playlist = playlist,
            currentSong = currentSong,
            sendUiEvent = sendUiEvent,
            navigationBarHeight = navigationBarHeight,
        )
    }
}

@Composable
private fun BottomSheetHeader(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Text(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onClick()
            },
        text = "Up Next",
        style = MaterialTheme.typography.titleMedium,
        color = Color.White
    )
}

@Composable
fun Playlist(
    modifier: Modifier = Modifier,
    playlist: ImmutableList<PlaylistViewSong>,
    currentSong: PlaylistViewSong?,
    sendUiEvent: (MusicPlayerIntent) -> Unit,
    navigationBarHeight: Dp,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = navigationBarHeight),
        content = {
            items(
                playlist,
                key = { it.id }
            ) { playlistSong ->
                PlaylistSongItem(
                    modifier = Modifier.padding(
                        horizontal = 32.dp,
                        vertical = 16.dp
                    ),
                    playlistSong = playlistSong,
                    currentSong = currentSong,
                    sendUiEvent = sendUiEvent
                )
            }
        }
    )
}

@Composable
private fun PlaylistSongItem(
    modifier: Modifier = Modifier,
    playlistSong: PlaylistViewSong,
    currentSong: PlaylistViewSong?,
    sendUiEvent: (MusicPlayerIntent) -> Unit
) {
    val backgroundColor: Color by animateColorAsState(
        targetValue = if (playlistSong.id == currentSong?.id) {
            LightGrayDarkerTransparent
        } else {
            Color.Transparent
        },
        animationSpec = tween(450)
    )
    Row(
        modifier = Modifier
            .clickable {
                sendUiEvent(MusicPlayerIntent.NewSongIntent(playlistSong.id))
            }
            .drawBehind {
                drawRect(backgroundColor)
            }
            .then(modifier)
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
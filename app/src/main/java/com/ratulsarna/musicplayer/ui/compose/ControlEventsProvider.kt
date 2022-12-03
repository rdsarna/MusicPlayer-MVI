package com.ratulsarna.musicplayer.ui.compose

import com.ratulsarna.musicplayer.ui.MusicPlayerIntent

class ControlEventsProvider(sendUiEvent: (MusicPlayerIntent) -> Unit) {
    val onPlay = { sendUiEvent(MusicPlayerIntent.PlayIntent) }
    val onPause = { sendUiEvent(MusicPlayerIntent.PauseIntent) }
    val onNext = { sendUiEvent(MusicPlayerIntent.NextSongIntent) }
    val onPrevious = { sendUiEvent(MusicPlayerIntent.PreviousSongIntent) }
}
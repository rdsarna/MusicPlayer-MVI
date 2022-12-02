package com.ratulsarna.musicplayer.ui.compose

import com.ratulsarna.musicplayer.ui.MusicPlayerEvent

class ControlEventsProvider(sendUiEvent: (MusicPlayerEvent) -> Unit) {
    val onPlay = { sendUiEvent(MusicPlayerEvent.PlayEvent) }
    val onPause = { sendUiEvent(MusicPlayerEvent.PauseEvent) }
    val onNext = { sendUiEvent(MusicPlayerEvent.NextSongEvent) }
    val onPrevious = { sendUiEvent(MusicPlayerEvent.PreviousSongEvent) }
    val onSeekForward = { sendUiEvent(MusicPlayerEvent.SeekForwardEvent) }
    val onSeekBackward = { sendUiEvent(MusicPlayerEvent.SeekBackwardEvent) }
}
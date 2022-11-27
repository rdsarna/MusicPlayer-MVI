package com.ratulsarna.musicplayer.ui

import com.ratulsarna.musicplayer.repository.model.Song
import com.ratulsarna.musicplayer.ui.model.PlaylistViewSong

sealed class MusicPlayerEvent {
    object UiCreateEvent : MusicPlayerEvent()
    object UiStartEvent : MusicPlayerEvent()
    object UiStopEvent : MusicPlayerEvent()
    object PlayEvent : MusicPlayerEvent()
    object PauseEvent : MusicPlayerEvent()
    data class NewSongEvent(val songId: Int) : MusicPlayerEvent()
    object SongCompletedEvent : MusicPlayerEvent()
    object NextSongEvent : MusicPlayerEvent()
    object PreviousSongEvent : MusicPlayerEvent()
    object SeekForwardEvent : MusicPlayerEvent()
    object SeekBackwardEvent : MusicPlayerEvent()
    data class SeekToEvent(val position: Int) : MusicPlayerEvent()
    data class CurrentPositionEvent(val position: Int): MusicPlayerEvent()
}

sealed class MusicPlayerResult {
    data class UiCreateResult(val playlist: List<PlaylistViewSong>) : MusicPlayerResult()
    data class UiStartResult(
        val song: Song?,
        val duration: Int,
        val playing: Boolean?,
        val errorLoadingSong: Boolean,
    ) : MusicPlayerResult()
    object UiStopResult : MusicPlayerResult()
    data class PlayResult(val playing: Boolean) : MusicPlayerResult()
    data class PauseResult(val playing: Boolean) : MusicPlayerResult()
    data class NewSongResult(
        val song: Song?,
        val duration: Int,
        val playing: Boolean,
        val errorLoading: Boolean,
    ) : MusicPlayerResult()
    data class SeekToResult(val position: Int) : MusicPlayerResult()
    data class CurrentPositionResult(val position: Int) : MusicPlayerResult()
}

sealed class MusicPlayerEffect {
    data class ForceScreenOnEffect(val on: Boolean) : MusicPlayerEffect()
    data class ShowErrorEffect(val errorMessage: String) : MusicPlayerEffect()
    object NoOpEffect : MusicPlayerEffect()
}
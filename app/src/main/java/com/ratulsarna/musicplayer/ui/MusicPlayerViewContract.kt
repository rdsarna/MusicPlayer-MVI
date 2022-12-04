package com.ratulsarna.musicplayer.ui

import androidx.annotation.DrawableRes
import com.ratulsarna.musicplayer.R
import com.ratulsarna.musicplayer.repository.model.Song
import com.ratulsarna.musicplayer.ui.model.PlaylistViewSong
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class MusicPlayerViewState(
    val loading: Boolean,
    val playing: Boolean,
    val songTitle: String,
    val songInfoLabel: String,
    @DrawableRes val albumArt: Int,
    val totalDuration: Float,
    val elapsedTime: Int,
    val currentPlaylistSong: PlaylistViewSong?,
    val playlist: ImmutableList<PlaylistViewSong>,
) {
    companion object {
        val INITIAL = MusicPlayerViewState(
            loading = true,
            playing = false,
            songTitle = "Loading...",
            songInfoLabel = "",
            albumArt = R.drawable.placeholder,
            totalDuration = 1f,
            elapsedTime = 0,
            currentPlaylistSong = null,
            playlist = persistentListOf(),
        )
    }
}

sealed class MusicPlayerIntent {
    object UiCreateIntent : MusicPlayerIntent()
    object UiStartIntent : MusicPlayerIntent()
    object UiStopIntent : MusicPlayerIntent()
    object PlayIntent : MusicPlayerIntent()
    object PauseIntent : MusicPlayerIntent()
    data class NewSongIntent(val songId: Int) : MusicPlayerIntent()
    object NextSongIntent : MusicPlayerIntent()
    object PreviousSongIntent : MusicPlayerIntent()
    object SeekForwardIntent : MusicPlayerIntent()
    object SeekBackwardIntent : MusicPlayerIntent()
    data class SeekToIntent(val position: Int) : MusicPlayerIntent()
    data class SongTickerIntent(val position: Int): MusicPlayerIntent()
}

sealed class MusicPlayerPartialStateChange {
    data class UiCreatePartialStateChange(val playlist: List<PlaylistViewSong>) : MusicPlayerPartialStateChange()
    data class UiStartPartialStateChange(
        val song: Song?,
        val duration: Int,
        val playing: Boolean?,
        val errorLoadingSong: Boolean,
    ) : MusicPlayerPartialStateChange()
    object UiStopPartialStateChange : MusicPlayerPartialStateChange()
    data class PlayPartialStateChange(val playing: Boolean) : MusicPlayerPartialStateChange()
    data class PausePartialStateChange(val playing: Boolean) : MusicPlayerPartialStateChange()
    data class NewSongPartialStateChange(
        val song: Song?,
        val duration: Int,
        val playing: Boolean,
        val errorLoading: Boolean,
    ) : MusicPlayerPartialStateChange()
    data class SeekToPartialStateChange(val position: Int) : MusicPlayerPartialStateChange()
    data class CurrentPositionPartialStateChange(val position: Int) : MusicPlayerPartialStateChange()
}

sealed class MusicPlayerSideEffect {
    data class ShowErrorSideEffect(val errorMessage: String) : MusicPlayerSideEffect()
}
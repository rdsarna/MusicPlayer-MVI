package com.ratulsarna.musicplayer.ui

import androidx.annotation.DrawableRes
import com.ratulsarna.musicplayer.R
import com.ratulsarna.shared.repository.model.Song
import com.ratulsarna.musicplayer.ui.model.PlaylistViewSong
import com.ratulsarna.shared.resources.ImageResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class MusicPlayerViewState(
    val loading: Boolean,
    val playing: Boolean,
    val songTitle: String,
    val songInfoLabel: String,
    val albumArt: ImageResource,
    val totalDuration: Long,
    val elapsedTime: Long,
    val elapsedTimeLabel: String,
    val totalTimeLabel: String,
    val currentPlaylistSong: PlaylistViewSong?,
    val playlist: ImmutableList<PlaylistViewSong>,
) {
    companion object {
        val INITIAL = MusicPlayerViewState(
            loading = true,
            playing = false,
            songTitle = "Loading...",
            songInfoLabel = "",
            albumArt = ImageResource.PLACEHOLDER_ALBUM_ART,
            totalDuration = 1,
            elapsedTime = 0,
            elapsedTimeLabel = "0:00",
            totalTimeLabel = "0:00",
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
    data class SeekToIntent(val position: Float) : MusicPlayerIntent()
    data class SongTickerIntent(val position: Long): MusicPlayerIntent()
}

sealed class MusicPlayerPartialStateChange {
    data class UiCreatePartialStateChange(val playlist: List<PlaylistViewSong>) : MusicPlayerPartialStateChange()
    data class UiStartPartialStateChange(
        val song: Song?,
        val duration: Long,
        val playing: Boolean?,
        val errorLoadingSong: Boolean,
    ) : MusicPlayerPartialStateChange()
    object UiStopPartialStateChange : MusicPlayerPartialStateChange()
    data class PlayPartialStateChange(val playing: Boolean) : MusicPlayerPartialStateChange()
    data class PausePartialStateChange(val playing: Boolean) : MusicPlayerPartialStateChange()
    data class NewSongPartialStateChange(
        val song: Song?,
        val duration: Long,
        val playing: Boolean,
        val errorLoading: Boolean,
    ) : MusicPlayerPartialStateChange()
    data class SeekToPartialStateChange(val position: Long) : MusicPlayerPartialStateChange()
    data class SongTickerPartialStateChange(val position: Long) : MusicPlayerPartialStateChange()
}

sealed class MusicPlayerSideEffect {
    data class ShowErrorSideEffect(val errorMessage: String) : MusicPlayerSideEffect()
}
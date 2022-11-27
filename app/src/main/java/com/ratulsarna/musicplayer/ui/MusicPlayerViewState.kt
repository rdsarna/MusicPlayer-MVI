package com.ratulsarna.musicplayer.ui

import androidx.annotation.DrawableRes
import androidx.lifecycle.LiveData
import com.ratulsarna.musicplayer.R
import com.ratulsarna.musicplayer.ui.model.PlaylistViewSong

data class MusicPlayerViewState(
    val loading: Boolean,
    val playing: Boolean,
    val songTitle: String,
    val songInfoLabel: String,
    @DrawableRes val albumArt: Int,
    val totalDuration: Float,
    val elapsedTime: Int,
    val currentPlaylistSong: PlaylistViewSong?,
    val playlist: List<PlaylistViewSong>,
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
            playlist = emptyList(),
        )
    }
}

data class MusicPlayerViewBindingState(
    val loading: LiveData<Boolean>,
    val playing: LiveData<Boolean>,
    val songTitle: LiveData<String>,
    val songInfoLabel: LiveData<String>,
    val albumArt: LiveData<Int>,
    val totalDuration: LiveData<Float>,
    val elapsedTime: LiveData<Int>,
    val totalDurationLabel: LiveData<String>,
    val elapsedTimeLabel: LiveData<String>,
    val nextSongLabel: LiveData<String>,
    val upNextSongsList: LiveData<List<PlaylistViewSong>>,
)

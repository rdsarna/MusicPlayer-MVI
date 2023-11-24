package com.ratulsarna.musicplayer.ui.model

import androidx.annotation.DrawableRes
import com.ratulsarna.musicplayer.repository.model.Song

data class PlaylistViewSong(
    val id: Int,
    val title: String,
    val infoLabel: String,
    @DrawableRes val albumArt: Int,
)

fun Song.toPlaylistViewSong(): PlaylistViewSong =
    PlaylistViewSong(id, title, "$artistName | $year", albumArtResId)

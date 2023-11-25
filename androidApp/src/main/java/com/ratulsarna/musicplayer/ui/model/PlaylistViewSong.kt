package com.ratulsarna.musicplayer.ui.model

import com.ratulsarna.shared.repository.model.Song
import com.ratulsarna.shared.resources.ImageResource

data class PlaylistViewSong(
    val id: Int,
    val title: String,
    val infoLabel: String,
    val albumArtResource: ImageResource,
)

fun Song.toPlaylistViewSong(): PlaylistViewSong =
    PlaylistViewSong(id, title, "$artistName | $year", albumArtResource)

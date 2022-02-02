package com.ratulsarna.musicplayer.repository.model

data class Playlist(
    val songs: List<PlaylistSongWrapper>,
    val createdAt: Long,
    val updatedAt: Long,
)

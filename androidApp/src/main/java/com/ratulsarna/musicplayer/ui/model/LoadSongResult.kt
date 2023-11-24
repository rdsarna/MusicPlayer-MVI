package com.ratulsarna.musicplayer.ui.model

data class LoadSongResult(
    val duration: Int?,
    val loadSuccessful: Boolean,
    val playing: Boolean,
)

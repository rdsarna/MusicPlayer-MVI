package com.ratulsarna.musicplayer.repository.model

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes

data class Song(
    val id: Int,
    val title: String,
    val artistName: String,
    val year: Int,
    @DrawableRes val albumArtResId: Int,
    @RawRes val rawResourceId: Int,
)
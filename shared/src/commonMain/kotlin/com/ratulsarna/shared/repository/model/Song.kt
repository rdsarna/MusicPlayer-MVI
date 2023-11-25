package com.ratulsarna.shared.repository.model

import com.ratulsarna.shared.BundledSongFileName
import com.ratulsarna.shared.resources.ImageResource

data class Song(
    val id: Int,
    val title: String,
    val artistName: String,
    val year: Int,
    val albumArtResource: ImageResource,
    val songFileName: BundledSongFileName
)
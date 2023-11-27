package com.ratulsarna.shared.repository

import com.ratulsarna.shared.repository.model.Song
import com.ratulsarna.shared.BundledSongFileName
import com.ratulsarna.shared.resources.ImageResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

interface SongsRepository {

    fun getSong(id: Int): Song?

    fun allSongs(): List<Song>
}

class SongsRepositoryDefault : SongsRepository {
    private val dummySongsList = listOf(
        Song(
            0,
            "Levitating",
            "Dua Lipa feat. DaBaby",
            2020,
            ImageResource.LEVITATING_ALBUM_ART,
            BundledSongFileName.LEVITATING
        ),
        Song(
            1,
            "Drinkee",
            "Sofi Tukker",
            2016,
            ImageResource.DRINKEE_ALBUM_ART,
            BundledSongFileName.DRINKEE
        ),
        Song(
            2,
            "Fireflies",
            "Owl City",
            2009,
            ImageResource.FIREFLIES_ALBUM_ART,
            BundledSongFileName.FIREFLIES
        ),
        Song(
            3,
            "Despacito",
            "Luis Fonsi ft. Daddy Yankee",
            2017,
            ImageResource.DESPACITO_ALBUM_ART,
            BundledSongFileName.DESPACITO
        )
    )
    private val allSongs = mutableListOf<Song>().apply {
        repeat(12) {
            add(dummySongsList[it % dummySongsList.size].copy(id = it))
        }
    }

    override fun getSong(id: Int): Song? =
        allSongs.find { it.id == id }

    override fun allSongs(): List<Song> = allSongs

}
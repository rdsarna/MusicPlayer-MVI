package com.ratulsarna.musicplayer.repository

import com.ratulsarna.musicplayer.R
import com.ratulsarna.musicplayer.repository.model.Song

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
            R.drawable.levitating_album_art,
            R.raw.dua_lipa_levitating,
        ),
        Song(
            1,
            "Drinkee",
            "Sofi Tukker",
            2016,
            R.drawable.drinkee_album_art,
            R.raw.sofi_tukker_drinkee,
        ),
        Song(
            2,
            "Fireflies",
            "Owl City",
            2009,
            R.drawable.fireflies_album_art,
            R.raw.owl_city_fireflies,
        ),
        Song(
            3,
            "Despacito",
            "Luis Fonsi ft. Daddy Yankee",
            2017,
            R.drawable.despacito_album_art,
            R.raw.luis_fonsi_despacito,
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
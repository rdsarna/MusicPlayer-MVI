package com.ratulsarna.musicplayer.repository

import com.ratulsarna.musicplayer.R
import com.ratulsarna.musicplayer.repository.model.Song
import javax.inject.Inject

interface SongsRepository {

    fun getSong(id: Int): Song?

    fun allSongs(): List<Song>
}

class SongsRepositoryDefault @Inject constructor() : SongsRepository {

    private val allSongs = listOf(
        Song(
            "Levitating",
            "Dua Lipa feat. DaBaby",
            2020,
            R.drawable.levitating_album_art,
            R.raw.dua_lipa_levitating,
        ),
        Song(
            "Drinkee",
            "Sofi Tukker",
            2016,
            R.drawable.drinkee_album_art,
            R.raw.sofi_tukker_drinkee,
        ),
        Song(
            "Fireflies",
            "Owl City",
            2009,
            R.drawable.fireflies_album_art,
            R.raw.owl_city_fireflies,
        ),
        Song(
            "Despacito",
            "Luis Fonsi ft. Daddy Yankee",
            2017,
            R.drawable.despacito_album_art,
            R.raw.luis_fonsi_despacito,
        )
    )

    override fun getSong(id: Int): Song? =
        allSongs.find { it.id == id }

    override fun allSongs(): List<Song> = allSongs

}
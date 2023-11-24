package com.ratulsarna.musicplayer.controllers

import com.ratulsarna.musicplayer.repository.model.Song

interface PlaylistSongsController {
    fun loadDefaultPlaylistSongs(): List<Song>
    fun currentPlaylist(): List<Song>
    fun currentSong(): Song?
    fun nextSong(): Song?
    fun previousSong(): Song?
    fun peekNextSong(): Song?
    fun newSong(id: Int): Song?
    fun playlist(): List<Song>
}
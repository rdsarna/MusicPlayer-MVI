package com.ratulsarna.musicplayer.ui.controllers

import com.ratulsarna.musicplayer.repository.model.Playlist
import com.ratulsarna.musicplayer.repository.PlaylistsRepository
import com.ratulsarna.musicplayer.repository.model.Song
import javax.inject.Inject

class UpNextSongsController @Inject constructor(
    private val playlistsRepository: PlaylistsRepository
) {

    private var _currentPlaylist: Playlist? = null
    private var _currentSongPosition = 0

    fun loadDefaultPlaylistSongs(): List<Song> {
        _currentPlaylist = playlistsRepository.getDefaultPlaylist()
        require(_currentPlaylist?.songs?.isNotEmpty() == true)
        return playlist()
    }

    fun currentPlaylist(): List<Song> = playlist()

    fun currentSong(): Song? = _currentPlaylist?.let {
        it.songs[_currentSongPosition].song
    }

    fun nextSong(): Song? = _currentPlaylist?.let {
        _currentSongPosition = (_currentSongPosition + 1) % it.songs.size
        it.songs[_currentSongPosition].song
    }

    fun previousSong(): Song? = _currentPlaylist?.let {
        _currentSongPosition = if ((_currentSongPosition-1) < 0) {
            it.songs.size - 1
        } else {
            _currentSongPosition - 1
        }
        it.songs[_currentSongPosition].song
    }

    fun peekNextSong(): Song? = _currentPlaylist?.let {
        it.songs[(_currentSongPosition + 1) % it.songs.size].song
    }

    fun newSong(id: Int): Song? {
        val playlist = _currentPlaylist ?: return null
        val newIndex = playlist.songs.indexOfFirst { it.song.id == id }
        if (newIndex != -1) {
            _currentSongPosition = newIndex
        } else {
            return null
        }
        return currentSong()
    }

    private fun playlist() =
        _currentPlaylist?.songs?.map { it.song } ?: emptyList()
}
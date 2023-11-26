package com.ratulsarna.shared.controllers

import com.ratulsarna.shared.repository.model.Playlist
import com.ratulsarna.shared.repository.PlaylistsRepository
import com.ratulsarna.shared.repository.model.Song

class PlaylistSongsControllerDefault(
    private val playlistsRepository: PlaylistsRepository
) : PlaylistSongsController {

    private var _currentPlaylist: Playlist? = null
    private var _currentSongPosition = 0

    override fun loadDefaultPlaylistSongs(): List<Song> {
        _currentPlaylist = playlistsRepository.getDefaultPlaylist()
        require(_currentPlaylist?.songs?.isNotEmpty() == true)
        return playlist()
    }

    override fun currentPlaylist(): List<Song> = playlist()

    override fun currentSong(): Song? = _currentPlaylist?.let {
        it.songs[_currentSongPosition].song
    }

    override fun nextSong(): Song? = _currentPlaylist?.let {
        _currentSongPosition = (_currentSongPosition + 1) % it.songs.size
        it.songs[_currentSongPosition].song
    }

    override fun previousSong(): Song? = _currentPlaylist?.let {
        _currentSongPosition = if ((_currentSongPosition-1) < 0) {
            it.songs.size - 1
        } else {
            _currentSongPosition - 1
        }
        it.songs[_currentSongPosition].song
    }

    override fun peekNextSong(): Song? = _currentPlaylist?.let {
        it.songs[(_currentSongPosition + 1) % it.songs.size].song
    }

    override fun newSong(id: Int): Song? {
        val playlist = _currentPlaylist ?: return null
        val newIndex = playlist.songs.indexOfFirst { it.song.id == id }
        if (newIndex != -1) {
            _currentSongPosition = newIndex
        } else {
            return null
        }
        return currentSong()
    }

    override fun playlist() =
        _currentPlaylist?.songs?.map { it.song } ?: emptyList()
}
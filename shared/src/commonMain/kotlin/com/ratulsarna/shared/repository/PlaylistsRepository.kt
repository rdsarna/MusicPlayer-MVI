package com.ratulsarna.shared.repository

import com.ratulsarna.shared.repository.model.Playlist
import com.ratulsarna.shared.repository.model.PlaylistSongWrapper

interface PlaylistsRepository {

    fun getDefaultPlaylist(): Playlist
}

class PlaylistsRepositoryDefault(
    songsRepository: SongsRepository,
) : PlaylistsRepository {

    private val _defaultPlaylist = Playlist(
        songs = songsRepository.allSongs().mapIndexed { i, song ->
            PlaylistSongWrapper(i, song)
        }
    )

    override fun getDefaultPlaylist(): Playlist = _defaultPlaylist
}

class PlaylistsRepositoryMock(var _mockPlaylist: Playlist) : PlaylistsRepository {

    override fun getDefaultPlaylist(): Playlist {
        return _mockPlaylist
    }
}
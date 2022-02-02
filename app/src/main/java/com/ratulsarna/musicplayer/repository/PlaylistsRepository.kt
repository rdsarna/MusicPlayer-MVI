package com.ratulsarna.musicplayer.repository

import com.ratulsarna.musicplayer.repository.model.Playlist
import com.ratulsarna.musicplayer.repository.model.PlaylistSongWrapper
import javax.inject.Inject

interface PlaylistsRepository {

    fun getDefaultPlaylist(): Playlist
}

class PlaylistsRepositoryDefault @Inject constructor(
    songsRepository: SongsRepository,
) : PlaylistsRepository {

    private val _defaultPlaylist = Playlist(
        songs = songsRepository.allSongs().mapIndexed { i, song ->
            PlaylistSongWrapper(i, song)
        },
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )

    override fun getDefaultPlaylist(): Playlist = _defaultPlaylist
}

class PlaylistsRepositoryMock(var _mockPlaylist: Playlist) : PlaylistsRepository {

    override fun getDefaultPlaylist(): Playlist {
        return _mockPlaylist
    }
}
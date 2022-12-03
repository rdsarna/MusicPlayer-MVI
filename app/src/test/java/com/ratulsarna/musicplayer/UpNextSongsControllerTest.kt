package com.ratulsarna.musicplayer

import com.ratulsarna.musicplayer.repository.model.Playlist
import com.ratulsarna.musicplayer.repository.model.PlaylistSongWrapper
import com.ratulsarna.musicplayer.repository.PlaylistsRepositoryMock
import com.ratulsarna.musicplayer.repository.model.Song
import com.ratulsarna.musicplayer.controllers.UpNextSongsController
import org.junit.Test

import org.junit.Assert.*

class UpNextSongsControllerTest {

    private val testSongs = listOf(
        PlaylistSongWrapper(0, Song(
            0,
            "Levitating",
            "Dua Lipa feat. DaBaby",
            2020,
            R.drawable.levitating_album_art,
            R.raw.dua_lipa_levitating,
        )
        ),
        PlaylistSongWrapper(1, Song(
            1,
            "Drinkee",
            "Sofi Tukker",
            2016,
            R.drawable.drinkee_album_art,
            R.raw.sofi_tukker_drinkee,
        )
        ),
        PlaylistSongWrapper(2, Song(
            2,
            "Fireflies",
            "Owl City",
            2009,
            R.drawable.fireflies_album_art,
            R.raw.owl_city_fireflies,
        )
        ),
    )

    @Test
    fun `all results are null or empty if playlist is not loaded`() {
        val playlistsRepository = PlaylistsRepositoryMock(
            Playlist(testSongs, 0, 0)
        )
        UpNextSongsController(playlistsRepository).apply {
            assertNull(currentSong())
            assertNull(nextSong())
            assertNull(previousSong())
            assertNull(peekNextSong())
            assertTrue(currentPlaylist().isEmpty())
        }
    }

    @Test
    fun `default up next list is loaded after filtering current song`() {
        val playlistsRepository = PlaylistsRepositoryMock(
            Playlist(testSongs, 0, 0)
        )
        val controller = UpNextSongsController(playlistsRepository)

        val expectedList = listOf(
            Song(
                0,
                "Drinkee",
                "Sofi Tukker",
                2016,
                R.drawable.drinkee_album_art,
                R.raw.sofi_tukker_drinkee,
            ),
            Song(
                1,
                "Fireflies",
                "Owl City",
                2009,
                R.drawable.fireflies_album_art,
                R.raw.owl_city_fireflies,
            ),
        )
        assertEquals(expectedList, controller.loadDefaultPlaylistSongs())
    }

    @Test
    fun `currentSong() works as expected after loading playlist`() {
        val playlistsRepository = PlaylistsRepositoryMock(
            Playlist(testSongs, 0, 0)
        )
        UpNextSongsController(playlistsRepository).apply {
            loadDefaultPlaylistSongs()
            assertEquals(testSongs[0].song, currentSong())
        }
    }

    @Test
    fun `peekNextSong() works as expected and does not change current song`() {
        val playlistsRepository = PlaylistsRepositoryMock(
            Playlist(testSongs, 0, 0)
        )
        UpNextSongsController(playlistsRepository).apply {
            loadDefaultPlaylistSongs()
            assertEquals(testSongs[1].song, peekNextSong())
            assertEquals(testSongs[0].song, currentSong())
        }
    }

    @Test
    fun `nextSong() changes song to next one`() {
        val playlistsRepository = PlaylistsRepositoryMock(
            Playlist(testSongs, 0, 0)
        )
        UpNextSongsController(playlistsRepository).apply {
            loadDefaultPlaylistSongs()
            assertEquals(testSongs[1].song, nextSong())
            assertEquals(testSongs[1].song, currentSong())
        }
    }

    @Test
    fun `previousSong() changes song to previous one`() {
        val playlistsRepository = PlaylistsRepositoryMock(
            Playlist(testSongs, 0, 0)
        )
        UpNextSongsController(playlistsRepository).apply {
            loadDefaultPlaylistSongs()
            assertEquals(testSongs[1].song, nextSong())
            assertEquals(testSongs[1].song, currentSong())
            assertEquals(testSongs[0].song, previousSong())
            assertEquals(testSongs[0].song, currentSong())
        }
    }

    @Test
    fun `newSong() changes song to the one that is the param`() {
        val playlistsRepository = PlaylistsRepositoryMock(
            Playlist(testSongs, 0, 0)
        )
        UpNextSongsController(playlistsRepository).apply {
            loadDefaultPlaylistSongs()
            assertEquals(testSongs[2].song, newSong(R.raw.owl_city_fireflies))
            assertEquals(testSongs[2].song, currentSong())
        }
    }

    @Test
    fun `newSong() returns null for an unknown id`() {
        val playlistsRepository = PlaylistsRepositoryMock(
            Playlist(testSongs, 0, 0)
        )
        UpNextSongsController(playlistsRepository).apply {
            loadDefaultPlaylistSongs()
            assertNull(newSong(0))
            assertEquals(testSongs[0].song, currentSong())
        }
    }

    @Test
    fun `nextSong() called at the end of the list should return first song`() {
        val playlistsRepository = PlaylistsRepositoryMock(
            Playlist(testSongs, 0, 0)
        )
        UpNextSongsController(playlistsRepository).apply {
            loadDefaultPlaylistSongs()
            assertEquals(testSongs[1].song, nextSong())
            assertEquals(testSongs[2].song, nextSong())
            assertEquals(testSongs[0].song, nextSong())
            assertEquals(testSongs[0].song, currentSong())
        }
    }

    @Test
    fun `previousSong() called at the start of the list should return last song`() {
        val playlistsRepository = PlaylistsRepositoryMock(
            Playlist(testSongs, 0, 0)
        )
        UpNextSongsController(playlistsRepository).apply {
            loadDefaultPlaylistSongs()
            assertEquals(testSongs[2].song, previousSong())
            assertEquals(testSongs[2].song, currentSong())
        }
    }

    @Test
    fun `peekNextSong() called at the end of the list should return first song`() {
        val playlistsRepository = PlaylistsRepositoryMock(
            Playlist(testSongs, 0, 0)
        )
        UpNextSongsController(playlistsRepository).apply {
            loadDefaultPlaylistSongs()
            assertEquals(testSongs[1].song, nextSong())
            assertEquals(testSongs[2].song, nextSong())
            assertEquals(testSongs[0].song, peekNextSong())
            assertEquals(testSongs[2].song, currentSong())
        }
    }
}
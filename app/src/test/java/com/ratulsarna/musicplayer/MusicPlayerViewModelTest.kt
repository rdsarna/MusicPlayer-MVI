package com.ratulsarna.musicplayer

import com.ratulsarna.musicplayer.controllers.MediaPlayerCommand
import com.ratulsarna.musicplayer.repository.PlaylistsRepositoryMock
import com.ratulsarna.musicplayer.repository.model.Playlist
import com.ratulsarna.musicplayer.repository.model.PlaylistSongWrapper
import com.ratulsarna.musicplayer.repository.model.Song
import com.ratulsarna.musicplayer.ui.MusicPlayerIntent.*
import com.ratulsarna.musicplayer.ui.vm.MusicPlayerViewModel
import com.ratulsarna.musicplayer.ui.MusicPlayerViewState
import com.ratulsarna.musicplayer.controllers.MediaPlayerControllerMock
import com.ratulsarna.musicplayer.controllers.PlaylistSongsController
import com.ratulsarna.musicplayer.controllers.PlaylistSongsControllerDefault
import com.ratulsarna.musicplayer.ui.model.toPlaylistViewSong
import com.ratulsarna.musicplayer.utils.CoroutineContextProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.CoroutineContext
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MusicPlayerViewModelTest {

    private val testSongs = listOf(
        PlaylistSongWrapper(0, Song(
            0,
            "Levitating",
            "Dua Lipa feat. DaBaby",
            2020,
            R.drawable.levitating_album_art,
            R.raw.dua_lipa_levitating,
        )),
        PlaylistSongWrapper(1, Song(
            1,
            "Drinkee",
            "Sofi Tukker",
            2016,
            R.drawable.drinkee_album_art,
            R.raw.sofi_tukker_drinkee,
        )),
        PlaylistSongWrapper(2, Song(
            2,
            "Fireflies",
            "Owl City",
            2009,
            R.drawable.fireflies_album_art,
            R.raw.owl_city_fireflies,
        )),
    )

    private val testDispatcher = ImmediateTestDispatcher()
    private lateinit var viewModel: MusicPlayerViewModel
    private val playlistSongsController = PlaylistSongsControllerDefault(
        PlaylistsRepositoryMock(Playlist(testSongs, 0, 0))
    )
    private lateinit var mediaPlayerController: MediaPlayerControllerMock

    private lateinit var stateJob: Job

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mediaPlayerController = MediaPlayerControllerMock()
        val coroutineContextProvider = object : CoroutineContextProvider {
            override val main: CoroutineContext = testDispatcher
            override val io: CoroutineContext = testDispatcher
        }
        viewModel = MusicPlayerViewModel(playlistSongsController, mediaPlayerController, coroutineContextProvider)
    }
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.collectStateAsList(): List<MusicPlayerViewState> {
        val actualStates = mutableListOf<MusicPlayerViewState>()
        stateJob = launch(testDispatcher) {
            viewModel.viewState.collect(actualStates::add)
        }
        return actualStates
    }

    private fun endCollection() {
        stateJob.cancel()
    }


    @Test
    fun `onSubscribing, should receive starting ViewState`() = runTest {
        val states = collectStateAsList()
        endCollection()

        assertEquals(1, states.size)
        assertEquals(MusicPlayerViewState.INITIAL, states[0])
    }

    @Test
    fun `UiStartIntent, loads song and sets song details to ViewState`() = runTest {
        val states = collectStateAsList()

        viewModel.processInput(UiStartIntent)

        endCollection()

        val expectedSong = testSongs[0].song.toPlaylistViewSong()

        assertEquals(2, states.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                totalDuration = TEST_SONG_DURATION,
                elapsedTime = 0,
                playing = false
            ),
            states[1]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
    }

    @Test
    fun `PlayIntent, plays song via media player and sets playing true in ViewState`() = runTest {
        val states = collectStateAsList()

        viewModel.processInput(UiStartIntent)
        viewModel.processInput(PlayIntent)

        endCollection()

        val expectedSong = testSongs[0].song.toPlaylistViewSong()

        assertEquals(3, states.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                totalDuration = TEST_SONG_DURATION,
                elapsedTime = 0,
                playing = true,
            ),
            states[2]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
    }

    @Test
    fun `PauseIntent, pauses song via media player and sets playing false in ViewState`() = runTest {
        val states = collectStateAsList()

        viewModel.apply {
            processInput(UiStartIntent)
            processInput(PlayIntent)
            processInput(PauseIntent)
        }

        endCollection()

        val expectedSong = testSongs[0].song.toPlaylistViewSong()

        assertEquals(4, states.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                totalDuration = TEST_SONG_DURATION,
                elapsedTime = 0,
                playing = false,
            ),
            states[3]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.Pause, mediaPlayerController.commands[3])
    }

    @Test
    fun `NextSongIntent, loads and plays next song from beginning`() = runTest {
        val states = collectStateAsList()

        viewModel.apply {
            processInput(UiStartIntent)
            processInput(PlayIntent)
            processInput(NextSongIntent)
        }

        endCollection()

        // expect second song
        val expectedSong = testSongs[1].song.toPlaylistViewSong()

        assertEquals(5, states.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                totalDuration = TEST_SONG_DURATION,
                elapsedTime = 0,
                playing = true,
            ),
            states[4]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[3])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[4])
    }

    @Test
    fun `PreviousSongIntent, loads and plays previous song from beginning`() = runTest {
        val states = collectStateAsList()

        viewModel.apply {
            processInput(UiStartIntent)
            processInput(PlayIntent)
            processInput(PreviousSongIntent)
        }

        endCollection()

        // expect previous/last song
        val expectedSong = testSongs[2].song.toPlaylistViewSong()

        assertEquals(5, states.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                totalDuration = TEST_SONG_DURATION,
                elapsedTime = 0,
                playing = true,
            ),
            states[4]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[3])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[4])
    }

    @Test
    fun `SeekToIntent, seeks to specified position and continues playing`() = runTest {
        val states = collectStateAsList()

        viewModel.apply {
            processInput(UiStartIntent)
            processInput(PlayIntent)
            processInput(SeekToIntent((TEST_SONG_DURATION/2).toInt()))
        }

        endCollection()

        val expectedSong = testSongs[0].song.toPlaylistViewSong()

        assertEquals(4, states.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                totalDuration = TEST_SONG_DURATION,
                elapsedTime = (TEST_SONG_DURATION/2).toInt(),
                playing = true,
            ),
            states[3]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.SeekTo, mediaPlayerController.commands[3])
    }

    @Test
    fun `SeekToIntent, currently not playing so only seeks to specified position`() = runTest {
        val states = collectStateAsList()

        viewModel.apply {
            processInput(UiStartIntent)
            processInput(SeekToIntent((TEST_SONG_DURATION/2).toInt()))
        }

        endCollection()

        val expectedSong = testSongs[0].song.toPlaylistViewSong()

        assertEquals(3, states.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                totalDuration = TEST_SONG_DURATION,
                elapsedTime = (TEST_SONG_DURATION/2).toInt(),
                playing = false,
            ),
            states[2]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.SeekTo, mediaPlayerController.commands[2])
    }

    @Test
    fun `SongTickerIntent, ticker increments and updates the elapsed time in ViewState`() = runTest {
        val states = collectStateAsList()

        viewModel.apply {
            processInput(UiStartIntent)
            processInput(PlayIntent)
            processInput(SongTickerIntent(1000))
            processInput(SongTickerIntent(2000))
            processInput(SongTickerIntent(3000))
            processInput(SongTickerIntent(4000))
            processInput(SongTickerIntent(5000))
            processInput(SongTickerIntent(6000))
            processInput(SongTickerIntent(7000))
        }

        endCollection()

        val expectedSong = testSongs[0].song.toPlaylistViewSong()

        assertEquals(10, states.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                totalDuration = TEST_SONG_DURATION,
                elapsedTime = 1000,
                playing = true,
            ),
            states[3]
        )
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                totalDuration = TEST_SONG_DURATION,
                elapsedTime = 7000,
                playing = true,
            ),
            states[9]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
    }

    @Test
    fun `NextSongIntent, chain of multiple next songs, should land on expected song`() = runTest {
        val states = collectStateAsList()

        viewModel.apply {
            processInput(UiStartIntent)
            processInput(PlayIntent)
            processInput(NextSongIntent)
            processInput(NextSongIntent)
            processInput(NextSongIntent)
            processInput(NextSongIntent)
            processInput(NextSongIntent)
            processInput(NextSongIntent)
            processInput(NextSongIntent)
            processInput(NextSongIntent)
        }

        endCollection()

        val expectedSong = testSongs[2].song.toPlaylistViewSong()

        assertEquals(19, states.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                totalDuration = TEST_SONG_DURATION,
                elapsedTime = 0,
                playing = true,
            ),
            states[18]
        )
    }

    companion object {
        private const val TEST_SONG_DURATION = 60 * 1000f
    }
}
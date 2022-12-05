package com.ratulsarna.musicplayer

import com.ratulsarna.musicplayer.controllers.MediaPlayerCommand
import com.ratulsarna.musicplayer.controllers.MediaPlayerControllerMock
import com.ratulsarna.musicplayer.controllers.MediaPlayerControllerMock.Companion.TEST_SONG_DURATION
import com.ratulsarna.musicplayer.controllers.PlaylistSongsControllerDefault
import com.ratulsarna.musicplayer.repository.PlaylistsRepositoryMock
import com.ratulsarna.musicplayer.repository.model.Playlist
import com.ratulsarna.musicplayer.repository.model.PlaylistSongWrapper
import com.ratulsarna.musicplayer.repository.model.Song
import com.ratulsarna.musicplayer.ui.MusicPlayerIntent.*
import com.ratulsarna.musicplayer.ui.MusicPlayerSideEffect
import com.ratulsarna.musicplayer.ui.MusicPlayerViewState
import com.ratulsarna.musicplayer.ui.model.toPlaylistViewSong
import com.ratulsarna.musicplayer.ui.vm.MusicPlayerViewModel
import com.ratulsarna.musicplayer.utils.CoroutineContextProvider
import com.ratulsarna.musicplayer.utils.MINIMUM_DURATION
import kotlinx.collections.immutable.toImmutableList
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
import kotlin.math.roundToInt
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
    private val playlist = testSongs.map { it.song.toPlaylistViewSong() }.toImmutableList()

    private lateinit var stateJob: Job
    private lateinit var effectsJob: Job

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

    private fun TestScope.collectStateAndSideEffectsAsLists(): Pair<List<MusicPlayerViewState>, List<MusicPlayerSideEffect>> {
        val actualStates = mutableListOf<MusicPlayerViewState>()
        stateJob = launch(testDispatcher) {
            viewModel.viewState.collect(actualStates::add)
        }
        val actualEffects = mutableListOf<MusicPlayerSideEffect>()
        effectsJob = launch(testDispatcher) {
            viewModel.sideEffects.collect(actualEffects::add)
        }
        return actualStates to actualEffects
    }

    private fun endCollection() {
        stateJob.cancel()
        effectsJob.cancel()
    }

    @Test
    fun `onSubscribing, should receive starting ViewState`() = runTest {
        val (states, sideEffects) = collectStateAndSideEffectsAsLists()
        endCollection()

        assertEquals(1, states.size)
        assertEquals(0, sideEffects.size)
        assertEquals(MusicPlayerViewState.INITIAL, states[0])
    }

    @Test
    fun `UiCreateIntent, loads playlist`() = runTest {
        val (states, sideEffects) = collectStateAndSideEffectsAsLists()

        viewModel.processInput(UiCreateIntent)

        endCollection()

        assertEquals(2, states.size)
        assertEquals(0, sideEffects.size)
        assertEquals(
            MusicPlayerViewState.INITIAL.copy(
                playlist = playlist
            ),
            states[1]
        )
    }

    @Test
    fun `calling UiStartIntent before UiCreateIntent results in initial ViewState only`() = runTest {
        val (states, sideEffects) = collectStateAndSideEffectsAsLists()

        viewModel.processInput(UiStartIntent)

        endCollection()

        assertEquals(1, states.size)
        assertEquals(0, sideEffects.size)
        assertEquals(MusicPlayerViewState.INITIAL, states[0])
    }

    @Test
    fun `UiStartIntent, loads song and sets song details to ViewState`() = runTest {
        val (states, sideEffects) = collectStateAndSideEffectsAsLists()

        viewModel.processInput(UiCreateIntent)
        viewModel.processInput(UiStartIntent)

        endCollection()

        val expectedSong = testSongs[0].song.toPlaylistViewSong()

        assertEquals(3, states.size)
        assertEquals(0, sideEffects.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                playing = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                totalDuration = TEST_SONG_DURATION,
                elapsedTime = 0,
                playlist = playlist,
                currentPlaylistSong = expectedSong,
                elapsedTimeLabel = "0:00",
                totalTimeLabel = "1:00"
            ),
            states[2]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
    }

    @Test
    fun `PlayIntent, plays song via media player and sets playing true in ViewState`() = runTest {
        val (states, sideEffects) = collectStateAndSideEffectsAsLists()

        viewModel.apply {
            processInput(UiCreateIntent)
            processInput(UiStartIntent)
            processInput(PlayIntent)
        }

        endCollection()

        val expectedSong = testSongs[0].song.toPlaylistViewSong()

        assertEquals(4, states.size)
        assertEquals(0, sideEffects.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                elapsedTime = 0,
                totalDuration = TEST_SONG_DURATION,
                playlist = playlist,
                currentPlaylistSong = expectedSong,
                playing = true,
                elapsedTimeLabel = "0:00",
                totalTimeLabel = "1:00"
            ),
            states[3]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
    }

    @Test
    fun `PauseIntent, pauses song via media player and sets playing false in ViewState`() = runTest {
        val (states, sideEffects) = collectStateAndSideEffectsAsLists()

        viewModel.apply {
            processInput(UiCreateIntent)
            processInput(UiStartIntent)
            processInput(PlayIntent)
            processInput(PauseIntent)
        }

        endCollection()

        val expectedSong = testSongs[0].song.toPlaylistViewSong()

        assertEquals(5, states.size)
        assertEquals(0, sideEffects.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                elapsedTime = 0,
                totalDuration = TEST_SONG_DURATION,
                playlist = playlist,
                currentPlaylistSong = expectedSong,
                playing = false,
                elapsedTimeLabel = "0:00",
                totalTimeLabel = "1:00"
            ),
            states[4]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.Pause, mediaPlayerController.commands[3])
    }

    @Test
    fun `NextSongIntent, loads and plays next song from beginning`() = runTest {
        val (states, sideEffects) = collectStateAndSideEffectsAsLists()

        viewModel.apply {
            processInput(UiCreateIntent)
            processInput(UiStartIntent)
            processInput(PlayIntent)
            processInput(NextSongIntent)
        }

        endCollection()

        val expectedSong = testSongs[1].song.toPlaylistViewSong()

        assertEquals(6, states.size)
        assertEquals(0, sideEffects.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                elapsedTime = 0,
                totalDuration = TEST_SONG_DURATION,
                playlist = playlist,
                currentPlaylistSong = expectedSong,
                playing = true,
                elapsedTimeLabel = "0:00",
                totalTimeLabel = "1:00"
            ),
            states[5]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[3])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[4])
    }

    @Test
    fun `PreviousSongIntent, loads and plays previous song from beginning`() = runTest {
        val (states, sideEffects) = collectStateAndSideEffectsAsLists()

        viewModel.apply {
            processInput(UiCreateIntent)
            processInput(UiStartIntent)
            processInput(PlayIntent)
            processInput(PreviousSongIntent)
        }

        endCollection()

        val expectedSong = testSongs[2].song.toPlaylistViewSong()

        assertEquals(6, states.size)
        assertEquals(0, sideEffects.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                elapsedTime = 0,
                totalDuration = TEST_SONG_DURATION,
                playlist = playlist,
                currentPlaylistSong = expectedSong,
                playing = true,
                elapsedTimeLabel = "0:00",
                totalTimeLabel = "1:00"
            ),
            states[5]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[3])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[4])
    }

    @Test
    fun `NewSongIntent, loads and plays new song from beginning`() = runTest {
        val (states, sideEffects) = collectStateAndSideEffectsAsLists()

        viewModel.apply {
            processInput(UiCreateIntent)
            processInput(UiStartIntent)
            processInput(PlayIntent)
            processInput(NewSongIntent(2))
        }

        endCollection()

        val expectedSong = testSongs[2].song.toPlaylistViewSong()

        assertEquals(6, states.size)
        assertEquals(0, sideEffects.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                elapsedTime = 0,
                totalDuration = TEST_SONG_DURATION,
                playlist = playlist,
                currentPlaylistSong = expectedSong,
                playing = true,
                elapsedTimeLabel = "0:00",
                totalTimeLabel = "1:00"
            ),
            states[5]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[3])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[4])
    }

    @Test
    fun `SeekForwardIntent, seeks forward by SEEK_DURATION and continues playing`() = runTest {
        val (states, sideEffects) = collectStateAndSideEffectsAsLists()

        viewModel.apply {
            processInput(UiCreateIntent)
            processInput(UiStartIntent)
            processInput(PlayIntent)
            processInput(SeekForwardIntent)
        }

        endCollection()

        val expectedSong = testSongs[0].song.toPlaylistViewSong()

        assertEquals(5, states.size)
        assertEquals(0, sideEffects.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                elapsedTime = 5000,
                totalDuration = TEST_SONG_DURATION,
                playlist = playlist,
                currentPlaylistSong = expectedSong,
                playing = true,
                elapsedTimeLabel = "0:05",
                totalTimeLabel = "1:00"
            ),
            states[4]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.SeekBy, mediaPlayerController.commands[3])
    }

    @Test
    fun `SeekBackwardIntent, seeks backward by SEEK_DURATION and continues playing`() = runTest {
        val (states, sideEffects) = collectStateAndSideEffectsAsLists()

        mediaPlayerController._currentPosition = 10000
        viewModel.apply {
            processInput(UiCreateIntent)
            processInput(UiStartIntent)
            processInput(PlayIntent)
            processInput(SongTickerIntent(10000))
            processInput(SeekBackwardIntent)
        }

        endCollection()

        val expectedSong = testSongs[0].song.toPlaylistViewSong()

        assertEquals(6, states.size)
        assertEquals(0, sideEffects.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                elapsedTime = 5000,
                totalDuration = TEST_SONG_DURATION,
                playlist = playlist,
                currentPlaylistSong = expectedSong,
                playing = true,
                elapsedTimeLabel = "0:05",
                totalTimeLabel = "1:00"
            ),
            states[5]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.SeekBy, mediaPlayerController.commands[3])
    }

    @Test
    fun `SeekToIntent, seeks to specified position and continues playing`() = runTest {
        val (states, sideEffects) = collectStateAndSideEffectsAsLists()

        viewModel.apply {
            processInput(UiCreateIntent)
            processInput(UiStartIntent)
            processInput(PlayIntent)
            processInput(SeekToIntent(TEST_SONG_DURATION/2f))
        }

        endCollection()

        val expectedSong = testSongs[0].song.toPlaylistViewSong()

        assertEquals(5, states.size)
        assertEquals(0, sideEffects.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                elapsedTime = TEST_SONG_DURATION / 2,
                totalDuration = TEST_SONG_DURATION,
                playlist = playlist,
                currentPlaylistSong = expectedSong,
                playing = true,
                elapsedTimeLabel = "0:30",
                totalTimeLabel = "1:00"
            ),
            states[4]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.SeekTo, mediaPlayerController.commands[3])
    }

    @Test
    fun `SeekForwardIntent, currently not playing so only seeks forward by SEEK_DURATION`() = runTest {
        val (states, sideEffects) = collectStateAndSideEffectsAsLists()

        viewModel.apply {
            processInput(UiCreateIntent)
            processInput(UiStartIntent)
            processInput(SeekForwardIntent)
        }

        endCollection()

        val expectedSong = testSongs[0].song.toPlaylistViewSong()

        assertEquals(4, states.size)
        assertEquals(0, sideEffects.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                elapsedTime = 5000,
                totalDuration = TEST_SONG_DURATION,
                playlist = playlist,
                currentPlaylistSong = expectedSong,
                playing = false,
                elapsedTimeLabel = "0:05",
                totalTimeLabel = "1:00"
            ),
            states[3]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.SeekBy, mediaPlayerController.commands[2])
    }

    @Test
    fun `SeekBackwardIntent, currently not playing so only seeks backward by SEEK_DURATION`() = runTest {
        val (states, sideEffects) = collectStateAndSideEffectsAsLists()

        mediaPlayerController._currentPosition = 10000
        viewModel.apply {
            processInput(UiCreateIntent)
            processInput(UiStartIntent)
            processInput(SeekBackwardIntent)
        }

        endCollection()

        val expectedSong = testSongs[0].song.toPlaylistViewSong()

        assertEquals(4, states.size)
        assertEquals(0, sideEffects.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                elapsedTime = 5000,
                totalDuration = TEST_SONG_DURATION,
                playlist = playlist,
                currentPlaylistSong = expectedSong,
                playing = false,
                elapsedTimeLabel = "0:05",
                totalTimeLabel = "1:00"
            ),
            states[3]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.SeekBy, mediaPlayerController.commands[2])
    }

    @Test
    fun `SeekToIntent, currently not playing so only seeks to specified position`() = runTest {
        val (states, sideEffects) = collectStateAndSideEffectsAsLists()

        viewModel.apply {
            processInput(UiCreateIntent)
            processInput(UiStartIntent)
            processInput(SeekToIntent(TEST_SONG_DURATION/2f))
        }

        endCollection()

        val expectedSong = testSongs[0].song.toPlaylistViewSong()

        assertEquals(4, states.size)
        assertEquals(0, sideEffects.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                elapsedTime = TEST_SONG_DURATION / 2,
                totalDuration = TEST_SONG_DURATION,
                playlist = playlist,
                currentPlaylistSong = expectedSong,
                playing = false,
                elapsedTimeLabel = "0:30",
                totalTimeLabel = "1:00"
            ),
            states[3]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.SeekTo, mediaPlayerController.commands[2])
    }

    @Test
    fun `UiStopIntent, song is playing, should release media player but state should remain playing`() = runTest {
        val (states, sideEffects) = collectStateAndSideEffectsAsLists()

        viewModel.apply {
            processInput(UiCreateIntent)
            processInput(UiStartIntent)
            processInput(PlayIntent)
            processInput(UiStopIntent)
        }

        endCollection()

        val expectedSong = testSongs[0].song.toPlaylistViewSong()

        assertEquals(4, states.size)
        assertEquals(0, sideEffects.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                elapsedTime = 0,
                totalDuration = TEST_SONG_DURATION,
                playlist = playlist,
                currentPlaylistSong = expectedSong,
                playing = true,
                elapsedTimeLabel = "0:00",
                totalTimeLabel = "1:00"
            ),
            states[3]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.Pause, mediaPlayerController.commands[3])
        assertEquals(MediaPlayerCommand.Release, mediaPlayerController.commands[4])
    }

    @Test
    fun `UiStopIntent, song is not playing, should release media player`() = runTest {
        val (states, sideEffects) = collectStateAndSideEffectsAsLists()

        viewModel.apply {
            processInput(UiCreateIntent)
            processInput(UiStartIntent)
            processInput(UiStopIntent)
        }

        endCollection()

        val expectedSong = testSongs[0].song.toPlaylistViewSong()

        assertEquals(3, states.size)
        assertEquals(0, sideEffects.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                elapsedTime = 0,
                totalDuration = TEST_SONG_DURATION,
                playlist = playlist,
                currentPlaylistSong = expectedSong,
                playing = false,
                elapsedTimeLabel = "0:00",
                totalTimeLabel = "1:00"
            ),
            states[2]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Pause, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.Release, mediaPlayerController.commands[3])
    }

    @Test
    fun `UiStartIntent after UiStopIntent while player was playing, should load same song and continue playing from same time`() = runTest {
        val (states, sideEffects) = collectStateAndSideEffectsAsLists()

        mediaPlayerController._currentPosition = 10000
        viewModel.apply {
            processInput(UiCreateIntent)
            processInput(UiStartIntent)
            processInput(PlayIntent)
            processInput(SongTickerIntent(10000))
            processInput(UiStopIntent)
            processInput(UiStartIntent)
        }

        endCollection()

        val expectedSong = testSongs[0].song.toPlaylistViewSong()

        assertEquals(5, states.size)
        assertEquals(0, sideEffects.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                elapsedTime = 10000,
                totalDuration = TEST_SONG_DURATION,
                playlist = playlist,
                currentPlaylistSong = expectedSong,
                playing = true,
                elapsedTimeLabel = "0:10",
                totalTimeLabel = "1:00"
            ),
            states[4]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.Pause, mediaPlayerController.commands[3])
        assertEquals(MediaPlayerCommand.Release, mediaPlayerController.commands[4])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[5])
        assertEquals(MediaPlayerCommand.SeekToAndStart, mediaPlayerController.commands[6])
        assertEquals(MediaPlayerCommand.SeekTo, mediaPlayerController.commands[7])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[8])
    }

    @Test
    fun `UiStartIntent after UiStopIntent while player was paused, should load same song from same elapsed time`() = runTest {
        val (states, sideEffects) = collectStateAndSideEffectsAsLists()

        mediaPlayerController._currentPosition = 10000
        viewModel.apply {
            processInput(UiCreateIntent)
            processInput(UiStartIntent)
            processInput(SongTickerIntent(10000))
            processInput(UiStopIntent)
            processInput(UiStartIntent)
        }

        endCollection()

        val expectedSong = testSongs[0].song.toPlaylistViewSong()

        assertEquals(4, states.size)
        assertEquals(0, sideEffects.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                elapsedTime = 10000,
                totalDuration = TEST_SONG_DURATION,
                playlist = playlist,
                currentPlaylistSong = expectedSong,
                playing = false,
                elapsedTimeLabel = "0:10",
                totalTimeLabel = "1:00"
            ),
            states[3]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Pause, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.Release, mediaPlayerController.commands[3])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[4])
        assertEquals(MediaPlayerCommand.SeekTo, mediaPlayerController.commands[5])
    }

    @Test
    fun `NextSongIntent, chain of multiple next songs, should land on expected song`() = runTest {
        val (states, sideEffects) = collectStateAndSideEffectsAsLists()

        viewModel.apply {
            processInput(UiCreateIntent)
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

        assertEquals(20, states.size)
        assertEquals(0, sideEffects.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                elapsedTime = 0,
                totalDuration = TEST_SONG_DURATION,
                playlist = playlist,
                currentPlaylistSong = expectedSong,
                playing = true,
                elapsedTimeLabel = "0:00",
                totalTimeLabel = "1:00"
            ),
            states[19]
        )
    }

    @Test
    fun `UiStartIntent, loading song fails, should show toast`() = runTest {
        val (states, sideEffects) = collectStateAndSideEffectsAsLists()

        mediaPlayerController._loadNewSongResultsInError = true
        viewModel.processInput(UiCreateIntent)
        viewModel.processInput(UiStartIntent)

        endCollection()

        val expectedSong = testSongs[0].song.toPlaylistViewSong()

        assertEquals(3, states.size)
        assertEquals(1, sideEffects.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                elapsedTime = 0,
                totalDuration = MINIMUM_DURATION.toLong(),
                playlist = playlist,
                currentPlaylistSong = expectedSong,
                playing = false,
                elapsedTimeLabel = "0:00",
                totalTimeLabel = "0:00"
            ),
            states[2]
        )
        assertEquals(
            MusicPlayerSideEffect.ShowErrorSideEffect("Error loading song. Try next song."),
            sideEffects[0]
        )
    }

    @Test
    fun `NewSongIntent, loading song fails, should show toast`() = runTest {
        val (states, sideEffects) = collectStateAndSideEffectsAsLists()

        mediaPlayerController._loadNewSongResultsInError = true
        viewModel.processInput(UiCreateIntent)
        viewModel.processInput(NewSongIntent(2))

        endCollection()

        val expectedSong = testSongs[2].song.toPlaylistViewSong()

        assertEquals(3, states.size)
        assertEquals(1, sideEffects.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                elapsedTime = 0,
                totalDuration = MINIMUM_DURATION.toLong(),
                playlist = playlist,
                currentPlaylistSong = expectedSong,
                playing = false,
                elapsedTimeLabel = "0:00",
                totalTimeLabel = "0:00"
            ),
            states[2]
        )
        assertEquals(
            MusicPlayerSideEffect.ShowErrorSideEffect("Error loading song. Try next song."),
            sideEffects[0]
        )
    }

    @Test
    fun `UiStartIntent, loading song fails, should show toast, NextSongIntent succeeds, should update ViewState`() = runTest {
        val (states, sideEffects) = collectStateAndSideEffectsAsLists()

        mediaPlayerController._loadNewSongResultsInError = true
        viewModel.processInput(UiCreateIntent)
        viewModel.processInput(UiStartIntent)
        mediaPlayerController._loadNewSongResultsInError = false
        viewModel.processInput(NextSongIntent)


        endCollection()

        val expectedSong1 = testSongs[0].song.toPlaylistViewSong()

        assertEquals(5, states.size)
        assertEquals(1, sideEffects.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong1.title,
                songInfoLabel = expectedSong1.infoLabel,
                albumArt = expectedSong1.albumArt,
                elapsedTime = 0,
                totalDuration = MINIMUM_DURATION.toLong(),
                playlist = playlist,
                currentPlaylistSong = expectedSong1,
                playing = false,
                elapsedTimeLabel = "0:00",
                totalTimeLabel = "0:00"
            ),
            states[2]
        )

        val expectedSong2 = testSongs[1].song.toPlaylistViewSong()
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong2.title,
                songInfoLabel = expectedSong2.infoLabel,
                albumArt = expectedSong2.albumArt,
                elapsedTime = 0,
                totalDuration = TEST_SONG_DURATION,
                playlist = playlist,
                currentPlaylistSong = expectedSong2,
                playing = true,
                elapsedTimeLabel = "0:00",
                totalTimeLabel = "1:00"
            ),
            states[4]
        )
        assertEquals(
            MusicPlayerSideEffect.ShowErrorSideEffect("Error loading song. Try next song."),
            sideEffects[0]
        )
    }

    @Test
    fun `SongTickerIntent, ticker increments and updates the elapsed time in ViewState`() = runTest {
        val (states, sideEffects) = collectStateAndSideEffectsAsLists()

        viewModel.apply {
            processInput(UiCreateIntent)
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

        assertEquals(11, states.size)
        assertEquals(0, sideEffects.size)
        assertEquals(
            MusicPlayerViewState(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                totalDuration = TEST_SONG_DURATION,
                elapsedTime = 1000,
                playing = true,
                currentPlaylistSong = expectedSong,
                playlist = playlist,
                elapsedTimeLabel = "0:01",
                totalTimeLabel = "1:00"
            ),
            states[4]
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
                currentPlaylistSong = expectedSong,
                playlist = playlist,
                elapsedTimeLabel = "0:07",
                totalTimeLabel = "1:00"
            ),
            states[10]
        )

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
    }

    companion object {
        private const val TEST_SONG_DURATION = 60 * 1000L
    }
}
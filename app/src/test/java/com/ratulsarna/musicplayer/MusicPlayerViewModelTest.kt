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
import com.ratulsarna.musicplayer.controllers.MediaPlayerControllerMock.Companion.TEST_SONG_DURATION
import com.ratulsarna.musicplayer.controllers.UpNextSongsController
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
    private val upNextSongsController = UpNextSongsController(
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
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, coroutineContextProvider)
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
            MusicPlayerViewState.INITIAL.copy(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                totalDuration = TEST_SONG_DURATION,
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
            MusicPlayerViewState.INITIAL.copy(
                loading = false,
                songTitle = expectedSong.title,
                songInfoLabel = expectedSong.infoLabel,
                albumArt = expectedSong.albumArt,
                totalDuration = TEST_SONG_DURATION,
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
            MusicPlayerViewState.INITIAL.copy(
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
            MusicPlayerViewState.INITIAL.copy(
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
            MusicPlayerViewState.INITIAL.copy(
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

//    @Test
//    fun `SongCompletedIntent, loads and plays next song from beginning since current song is playing`() {
//
//
//        val viewStateTester = viewModel.viewState.test()
//        val viewEffectTester = viewModel.viewEffects.test()
//
//        viewModel.apply {
//            processInput(UiCreateIntent)
//            processInput(UiStartIntent)
//            processInput(PlayIntent)
//            processInput(CurrentPositionIntent(TEST_SONG_DURATION.toInt()))
//            processInput(SongCompletedIntent)
//        }
//
//        viewStateTester.assertValueAt(7) { vs ->
//            val song = testSongs[1].song.toPlaylistViewSong()
//            assertEquals(
//                MusicPlayerViewState.INITIAL.copy(
//                    loading = false,
//                    songTitle = song.title,
//                    songInfoLabel = song.infoLabel,
//                    albumArt = song.albumArt,
//                    totalDuration = TEST_SONG_DURATION,
//                    elapsedTime = 0,
//                    playing = true,
//                    nextSongLabel = "Up Next: ${testSongs[2].song.title}",
//                    upNextSongs = upNextSongsController.currentPlaylist()
//                        .map { it.toPlaylistViewSong() }
//                ),
//                vs
//            )
//            true
//        }
//
//        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
//        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
//        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
//        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[3])
//        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[4])
//
//        assertEquals(ForceScreenOnSideEffect(true), viewEffectTester.values().last())
//    }
//
//    @Test
//    fun `SeekForwardIntent, seeks forward by SEEK_DURATION and continues playing`() {
//
//
//        val viewStateTester = viewModel.viewState.test()
//
//        mediaPlayerController._currentPosition = 2000
//        viewModel.apply {
//            processInput(UiCreateIntent)
//            processInput(UiStartIntent)
//            processInput(PlayIntent)
//            processInput(CurrentPositionIntent(2000))
//            processInput(SeekForwardIntent)
//        }
//
//        viewStateTester.assertValueAt(6) { vs ->
//            val song = testSongs[0].song.toPlaylistViewSong()
//            assertEquals(
//                MusicPlayerViewState.INITIAL.copy(
//                    loading = false,
//                    songTitle = song.title,
//                    songInfoLabel = song.infoLabel,
//                    albumArt = song.albumArt,
//                    totalDuration = TEST_SONG_DURATION,
//                    elapsedTime = 7000,
//                    playing = true,
//                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
//                    upNextSongs = upNextSongsController.currentPlaylist()
//                        .map { it.toPlaylistViewSong() }
//                ),
//                vs
//            )
//            true
//        }
//
//        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
//        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
//        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
//        assertEquals(MediaPlayerCommand.SeekBy, mediaPlayerController.commands[3])
//    }
//
//    @Test
//    fun `SeekBackwardIntent, seeks backward by SEEK_DURATION and continues playing`() {
//
//
//        val viewStateTester = viewModel.viewState.test()
//
//        mediaPlayerController._currentPosition = 10000
//        viewModel.apply {
//            processInput(UiCreateIntent)
//            processInput(UiStartIntent)
//            processInput(PlayIntent)
//            processInput(CurrentPositionIntent(10000))
//            processInput(SeekBackwardIntent)
//        }
//
//        viewStateTester.assertValueAt(6) { vs ->
//            val song = testSongs[0].song.toPlaylistViewSong()
//            assertEquals(
//                MusicPlayerViewState.INITIAL.copy(
//                    loading = false,
//                    songTitle = song.title,
//                    songInfoLabel = song.infoLabel,
//                    albumArt = song.albumArt,
//                    totalDuration = TEST_SONG_DURATION,
//                    elapsedTime = 5000,
//                    playing = true,
//                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
//                    upNextSongs = upNextSongsController.currentPlaylist()
//                        .map { it.toPlaylistViewSong() }
//                ),
//                vs
//            )
//            true
//        }
//
//        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
//        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
//        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
//        assertEquals(MediaPlayerCommand.SeekBy, mediaPlayerController.commands[3])
//    }
//
//    @Test
//    fun `SeekToIntent, seeks to specified position and continues playing`() {
//
//
//        val viewStateTester = viewModel.viewState.test()
//
//        viewModel.apply {
//            processInput(UiCreateIntent)
//            processInput(UiStartIntent)
//            processInput(PlayIntent)
//            processInput(SeekToIntent((TEST_SONG_DURATION/2).toInt()))
//        }
//
//        viewStateTester.assertValueAt(5) { vs ->
//            val song = testSongs[0].song.toPlaylistViewSong()
//            assertEquals(
//                MusicPlayerViewState.INITIAL.copy(
//                    loading = false,
//                    songTitle = song.title,
//                    songInfoLabel = song.infoLabel,
//                    albumArt = song.albumArt,
//                    totalDuration = TEST_SONG_DURATION,
//                    elapsedTime = (TEST_SONG_DURATION/2).toInt(),
//                    playing = true,
//                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
//                    upNextSongs = upNextSongsController.currentPlaylist()
//                        .map { it.toPlaylistViewSong() }
//                ),
//                vs
//            )
//            true
//        }
//
//        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
//        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
//        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
//        assertEquals(MediaPlayerCommand.SeekTo, mediaPlayerController.commands[3])
//    }
//
//    @Test
//    fun `SeekForwardIntent, currently not playing so only seeks forward by SEEK_DURATION`() {
//
//
//        val viewStateTester = viewModel.viewState.test()
//
//        mediaPlayerController._currentPosition = 2000
//        viewModel.apply {
//            processInput(UiCreateIntent)
//            processInput(UiStartIntent)
//            processInput(CurrentPositionIntent(2000))
//            processInput(SeekForwardIntent)
//        }
//
//        viewStateTester.assertValueAt(5) { vs ->
//            val song = testSongs[0].song.toPlaylistViewSong()
//            assertEquals(
//                MusicPlayerViewState.INITIAL.copy(
//                    loading = false,
//                    songTitle = song.title,
//                    songInfoLabel = song.infoLabel,
//                    albumArt = song.albumArt,
//                    totalDuration = TEST_SONG_DURATION,
//                    elapsedTime = 7000,
//                    playing = false,
//                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
//                    upNextSongs = upNextSongsController.currentPlaylist()
//                        .map { it.toPlaylistViewSong() }
//                ),
//                vs
//            )
//            true
//        }
//
//        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
//        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
//        assertEquals(MediaPlayerCommand.SeekBy, mediaPlayerController.commands[2])
//    }
//
//    @Test
//    fun `SeekBackwardIntent, currently not playing so only seeks backward by SEEK_DURATION`() {
//
//
//        val viewStateTester = viewModel.viewState.test()
//
//        mediaPlayerController._currentPosition = 10000
//        viewModel.apply {
//            processInput(UiCreateIntent)
//            processInput(UiStartIntent)
//            processInput(CurrentPositionIntent(10000))
//            processInput(SeekBackwardIntent)
//        }
//
//        viewStateTester.assertValueAt(5) { vs ->
//            val song = testSongs[0].song.toPlaylistViewSong()
//            assertEquals(
//                MusicPlayerViewState.INITIAL.copy(
//                    loading = false,
//                    songTitle = song.title,
//                    songInfoLabel = song.infoLabel,
//                    albumArt = song.albumArt,
//                    totalDuration = TEST_SONG_DURATION,
//                    elapsedTime = 5000,
//                    playing = false,
//                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
//                    upNextSongs = upNextSongsController.currentPlaylist()
//                        .map { it.toPlaylistViewSong() }
//                ),
//                vs
//            )
//            true
//        }
//
//        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
//        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
//        assertEquals(MediaPlayerCommand.SeekBy, mediaPlayerController.commands[2])
//    }
//
//    @Test
//    fun `SeekToIntent, currently not playing so only seeks to specified position`() {
//
//
//        val viewStateTester = viewModel.viewState.test()
//
//        viewModel.apply {
//            processInput(UiCreateIntent)
//            processInput(UiStartIntent)
//            processInput(SeekToIntent((TEST_SONG_DURATION/2).toInt()))
//        }
//
//        viewStateTester.assertValueAt(4) { vs ->
//            val song = testSongs[0].song.toPlaylistViewSong()
//            assertEquals(
//                MusicPlayerViewState.INITIAL.copy(
//                    loading = false,
//                    songTitle = song.title,
//                    songInfoLabel = song.infoLabel,
//                    albumArt = song.albumArt,
//                    totalDuration = TEST_SONG_DURATION,
//                    elapsedTime = (TEST_SONG_DURATION/2).toInt(),
//                    playing = false,
//                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
//                    upNextSongs = upNextSongsController.currentPlaylist()
//                        .map { it.toPlaylistViewSong() }
//                ),
//                vs
//            )
//            true
//        }
//
//        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
//        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
//        assertEquals(MediaPlayerCommand.SeekTo, mediaPlayerController.commands[2])
//    }
//
//    @Test
//    fun `UiStopIntent, song is playing, should release media player but state should remain playing`() {
//
//
//        val viewStateTester = viewModel.viewState.test()
//
//        viewModel.apply {
//            processInput(UiCreateIntent)
//            processInput(UiStartIntent)
//            processInput(PlayIntent)
//            processInput(UiStopIntent)
//        }
//
//        viewStateTester.assertValueAt(4) { vs ->
//            val song = testSongs[0].song.toPlaylistViewSong()
//            assertEquals(
//                MusicPlayerViewState.INITIAL.copy(
//                    loading = false,
//                    songTitle = song.title,
//                    songInfoLabel = song.infoLabel,
//                    albumArt = song.albumArt,
//                    totalDuration = TEST_SONG_DURATION,
//                    playing = true,
//                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
//                    upNextSongs = upNextSongsController.currentPlaylist()
//                        .map { it.toPlaylistViewSong() }
//                ),
//                vs
//            )
//            true
//        }
//
//        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
//        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
//        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
//        assertEquals(MediaPlayerCommand.Pause, mediaPlayerController.commands[3])
//        assertEquals(MediaPlayerCommand.Release, mediaPlayerController.commands[4])
//    }
//
//    @Test
//    fun `UiStopIntent, song is not playing, should release media player`() {
//
//
//        val viewStateTester = viewModel.viewState.test()
//
//        viewModel.apply {
//            processInput(UiCreateIntent)
//            processInput(UiStartIntent)
//            processInput(UiStopIntent)
//        }
//
//        viewStateTester.assertValueAt(3) { vs ->
//            val song = testSongs[0].song.toPlaylistViewSong()
//            assertEquals(
//                MusicPlayerViewState.INITIAL.copy(
//                    loading = false,
//                    songTitle = song.title,
//                    songInfoLabel = song.infoLabel,
//                    albumArt = song.albumArt,
//                    totalDuration = TEST_SONG_DURATION,
//                    playing = false,
//                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
//                    upNextSongs = upNextSongsController.currentPlaylist()
//                        .map { it.toPlaylistViewSong() }
//                ),
//                vs
//            )
//            true
//        }
//
//        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
//        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
//        assertEquals(MediaPlayerCommand.Pause, mediaPlayerController.commands[2])
//        assertEquals(MediaPlayerCommand.Release, mediaPlayerController.commands[3])
//    }
//
//    @Test
//    fun `UiStartIntent after UiStopIntent while player was playing, should load same song and continue playing from same time`() {
//
//
//        val viewStateTester = viewModel.viewState.test()
//
//        mediaPlayerController._currentPosition = 10000
//        viewModel.apply {
//            processInput(UiCreateIntent)
//            processInput(UiStartIntent)
//            processInput(PlayIntent)
//            processInput(CurrentPositionIntent(10000))
//            processInput(UiStopIntent)
//            processInput(UiStartIntent)
//        }
//
//        viewStateTester.assertValueAt(7) { vs ->
//            val song = testSongs[0].song.toPlaylistViewSong()
//            assertEquals(
//                MusicPlayerViewState.INITIAL.copy(
//                    loading = false,
//                    songTitle = song.title,
//                    songInfoLabel = song.infoLabel,
//                    albumArt = song.albumArt,
//                    totalDuration = TEST_SONG_DURATION,
//                    elapsedTime = 10000,
//                    playing = true,
//                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
//                    upNextSongs = upNextSongsController.currentPlaylist()
//                        .map { it.toPlaylistViewSong() }
//                ),
//                vs
//            )
//            true
//        }
//
//        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
//        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
//        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
//        assertEquals(MediaPlayerCommand.Pause, mediaPlayerController.commands[3])
//        assertEquals(MediaPlayerCommand.Release, mediaPlayerController.commands[4])
//        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[5])
//        assertEquals(MediaPlayerCommand.SeekToAndStart, mediaPlayerController.commands[6])
//        assertEquals(MediaPlayerCommand.SeekTo, mediaPlayerController.commands[7])
//        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[8])
//    }
//
//    @Test
//    fun `UiStartIntent after UiStopIntent while player was paused, should load same song from same elapsed time`() {
//
//
//        val viewStateTester = viewModel.viewState.test()
//
//        mediaPlayerController._currentPosition = 10000
//        viewModel.apply {
//            processInput(UiCreateIntent)
//            processInput(UiStartIntent)
//            processInput(CurrentPositionIntent(10000))
//            processInput(UiStopIntent)
//            processInput(UiStartIntent)
//        }
//
//        viewStateTester.assertValueAt(6) { vs ->
//            val song = testSongs[0].song.toPlaylistViewSong()
//            assertEquals(
//                MusicPlayerViewState.INITIAL.copy(
//                    loading = false,
//                    songTitle = song.title,
//                    songInfoLabel = song.infoLabel,
//                    albumArt = song.albumArt,
//                    totalDuration = TEST_SONG_DURATION,
//                    elapsedTime = 10000,
//                    playing = false,
//                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
//                    upNextSongs = upNextSongsController.currentPlaylist()
//                        .map { it.toPlaylistViewSong() }
//                ),
//                vs
//            )
//            true
//        }
//
//        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
//        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
//        assertEquals(MediaPlayerCommand.Pause, mediaPlayerController.commands[2])
//        assertEquals(MediaPlayerCommand.Release, mediaPlayerController.commands[3])
//        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[4])
//        assertEquals(MediaPlayerCommand.SeekTo, mediaPlayerController.commands[5])
//    }
//
//    @Test
//    fun `NextSongIntent, chain of multiple next songs, should land on expected song`() {
//
//
//        val viewStateTester = viewModel.viewState.test()
//
//        viewModel.apply {
//            processInput(UiCreateIntent)
//            processInput(UiStartIntent)
//            processInput(PlayIntent)
//            processInput(NextSongIntent)
//            processInput(NextSongIntent)
//            processInput(NextSongIntent)
//            processInput(NextSongIntent)
//            processInput(NextSongIntent)
//            processInput(NextSongIntent)
//            processInput(NextSongIntent)
//            processInput(NextSongIntent)
//        }
//
//        viewStateTester.assertValueAt(20) { vs ->
//            val song = testSongs[2].song.toPlaylistViewSong()
//            assertEquals(
//                MusicPlayerViewState.INITIAL.copy(
//                    loading = false,
//                    songTitle = song.title,
//                    songInfoLabel = song.infoLabel,
//                    albumArt = song.albumArt,
//                    totalDuration = TEST_SONG_DURATION,
//                    elapsedTime = 0,
//                    playing = true,
//                    nextSongLabel = "Up Next: ${testSongs[0].song.title}",
//                    upNextSongs = upNextSongsController.currentPlaylist()
//                        .map { it.toPlaylistViewSong() }
//                ),
//                vs
//            )
//            true
//        }
//    }
//
//    @Test
//    fun `UiStartIntent, loading song fails, should show toast`() {
//
//
//        mediaPlayerController._loadNewSongResultsInError = true
//
//        val viewStateTester = viewModel.viewState.test()
//        val viewEffectTester = viewModel.viewEffects.test()
//
//        viewModel.processInput(UiCreateIntent)
//        viewModel.processInput(UiStartIntent)
//
//        viewStateTester.assertValueAt(1) { vs ->
//            assertEquals(
//                MusicPlayerViewState.INITIAL.copy(
//                    upNextSongs = upNextSongsController.currentPlaylist()
//                        .map { it.toPlaylistViewSong() }
//                ),
//                vs
//            )
//            true
//        }
//
//        assertTrue { viewEffectTester.values().last() is ShowErrorSideEffect }
//    }
//
//    @Test
//    fun `NewSongIntent, loading song fails, should show toast`() {
//
//
//        mediaPlayerController._loadNewSongResultsInError = true
//
//        val viewStateTester = viewModel.viewState.test()
//        val viewEffectTester = viewModel.viewEffects.test()
//
//        viewModel.processInput(UiCreateIntent)
//        viewModel.processInput(NewSongIntent(R.raw.dua_lipa_levitating))
//
//        viewStateTester.assertValueAt(1) { vs ->
//            assertEquals(
//                MusicPlayerViewState.INITIAL.copy(
//                    upNextSongs = upNextSongsController.currentPlaylist()
//                        .map { it.toPlaylistViewSong() }
//                ),
//                vs
//            )
//            true
//        }
//
//        assertTrue { viewEffectTester.values().last() is ShowErrorSideEffect }
//    }
//
//    @Test
//    fun `UiStartIntent, loading song fails, should show toast, NextSongIntent succeeds, should update ViewState`() {
//
//
//        mediaPlayerController._loadNewSongResultsInError = true
//
//        val viewStateTester = viewModel.viewState.test()
//        val viewEffectTester = viewModel.viewEffects.test()
//
//        viewModel.processInput(UiCreateIntent)
//        viewModel.processInput(UiStartIntent)
//
//        viewStateTester.assertValueAt(2) { vs ->
//            val song = testSongs[0].song.toPlaylistViewSong()
//            assertEquals(
//                MusicPlayerViewState.INITIAL.copy(
//                    loading = false,
//                    songTitle = song.title,
//                    songInfoLabel = song.infoLabel,
//                    albumArt = song.albumArt,
//                    totalDuration = 1f,
//                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
//                    upNextSongs = upNextSongsController.currentPlaylist()
//                        .map { it.toPlaylistViewSong() }
//                ),
//                vs
//            )
//            true
//        }
//
//        assertTrue { viewEffectTester.values().last() is ShowErrorSideEffect }
//
//        mediaPlayerController._loadNewSongResultsInError = false
//        viewModel.processInput(NextSongIntent)
//
//        viewStateTester.assertValueAt(4) { vs ->
//            val song = testSongs[1].song.toPlaylistViewSong()
//            assertEquals(
//                MusicPlayerViewState.INITIAL.copy(
//                    loading = false,
//                    songTitle = song.title,
//                    songInfoLabel = song.infoLabel,
//                    albumArt = song.albumArt,
//                    totalDuration = TEST_SONG_DURATION,
//                    elapsedTime = 0,
//                    playing = true,
//                    nextSongLabel = "Up Next: ${testSongs[2].song.title}",
//                    upNextSongs = upNextSongsController.currentPlaylist()
//                        .map { it.toPlaylistViewSong() }
//                ),
//                vs
//            )
//            true
//        }
//    }

    companion object {
        private const val TEST_SONG_DURATION = 60 * 1000f
    }
}
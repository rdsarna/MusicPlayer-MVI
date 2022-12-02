package com.ratulsarna.musicplayer

import com.ratulsarna.musicplayer.repository.PlaylistsRepositoryMock
import com.ratulsarna.musicplayer.repository.model.Playlist
import com.ratulsarna.musicplayer.repository.model.PlaylistSongWrapper
import com.ratulsarna.musicplayer.repository.model.Song
import com.ratulsarna.musicplayer.ui.MusicPlayerEffect.ForceScreenOnEffect
import com.ratulsarna.musicplayer.ui.MusicPlayerEffect.ShowErrorEffect
import com.ratulsarna.musicplayer.ui.MusicPlayerEvent.*
import com.ratulsarna.musicplayer.ui.MusicPlayerViewModel
import com.ratulsarna.musicplayer.ui.MusicPlayerViewState
import com.ratulsarna.musicplayer.controllers.MediaPlayerCommand
import com.ratulsarna.musicplayer.controllers.MediaPlayerControllerMock
import com.ratulsarna.musicplayer.controllers.UpNextSongsController
import com.ratulsarna.musicplayer.ui.model.toPlaylistViewSong
import com.ratulsarna.musicplayer.utils.SchedulerProviderTrampoline
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MusicPlayerViewModelTest {

    private val testSongs = listOf(
        PlaylistSongWrapper(0, Song(
            "Levitating",
            "Dua Lipa feat. DaBaby",
            2020,
            R.drawable.levitating_album_art,
            R.raw.dua_lipa_levitating,
        )),
        PlaylistSongWrapper(1, Song(
            "Drinkee",
            "Sofi Tukker",
            2016,
            R.drawable.drinkee_album_art,
            R.raw.sofi_tukker_drinkee,
        )),
        PlaylistSongWrapper(2, Song(
            "Fireflies",
            "Owl City",
            2009,
            R.drawable.fireflies_album_art,
            R.raw.owl_city_fireflies,
        )),
    )

    private lateinit var viewModel: MusicPlayerViewModel
    private val upNextSongsController = UpNextSongsController(
        PlaylistsRepositoryMock(Playlist(testSongs, 0, 0))
    )
    private lateinit var mediaPlayerController: MediaPlayerControllerMock
    private val schedulerProvider = SchedulerProviderTrampoline()

    @Before
    fun setup() {
        mediaPlayerController = MediaPlayerControllerMock()
    }

    @Test
    fun `onSubscribing, should receive starting ViewState`() {
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, schedulerProvider)

        val viewStateTester = viewModel.viewState.test()
        viewStateTester.assertValueCount(1)
    }

    @Test
    fun `UiCreateEvent, loads playlist`() {
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, schedulerProvider)

        val viewStateTester = viewModel.viewState.test()

        viewModel.processInput(UiCreateEvent)

        viewStateTester.assertValueAt(1) { vs ->
            assertEquals(
                MusicPlayerViewState.INITIAL.copy(
                    upNextSongs = upNextSongsController.currentPlaylist()
                        .map { it.toPlaylistViewSong() }
                ),
                vs
            )
            true
        }
    }

    @Test
    fun `calling UiStartEvent before UiCreateEvent results in initial ViewState only`() {
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, schedulerProvider)

        val viewStateTester = viewModel.viewState.test()

        viewModel.processInput(UiStartEvent)

        viewStateTester.assertValueAt(0) { vs ->
            assertEquals(
                MusicPlayerViewState.INITIAL,
                vs
            )
            true
        }
    }

    @Test
    fun `UiStartEvent, loads song and sets song details to ViewState`() {
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, schedulerProvider)

        val viewStateTester = viewModel.viewState.test()

        viewModel.processInput(UiCreateEvent)
        viewModel.processInput(UiStartEvent)

        viewStateTester.assertValueAt(3) { vs ->
            val song = testSongs[0].song.toPlaylistViewSong()
            assertEquals(
                MusicPlayerViewState.INITIAL.copy(
                    loading = false,
                    songTitle = song.title,
                    songInfoLabel = song.infoLabel,
                    albumArt = song.albumArt,
                    totalDuration = TEST_SONG_DURATION,
                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
                    upNextSongs = upNextSongsController.currentPlaylist()
                        .map { it.toPlaylistViewSong() }
                ),
                vs
            )
            true
        }

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
    }

    @Test
    fun `PlayEvent, plays song via media player and sets playing true in ViewState`() {
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, schedulerProvider)

        val viewStateTester = viewModel.viewState.test()
        val viewEffectTester = viewModel.viewEffects.test()

        viewModel.processInput(UiCreateEvent)
        viewModel.processInput(UiStartEvent)
        viewModel.processInput(PlayEvent)

        viewStateTester.assertValueAt(4) { vs ->
            val song = testSongs[0].song.toPlaylistViewSong()
            assertEquals(
                MusicPlayerViewState.INITIAL.copy(
                    loading = false,
                    songTitle = song.title,
                    songInfoLabel = song.infoLabel,
                    albumArt = song.albumArt,
                    totalDuration = TEST_SONG_DURATION,
                    playing = true,
                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
                    upNextSongs = upNextSongsController.currentPlaylist()
                        .map { it.toPlaylistViewSong() }
                ),
                vs
            )
            true
        }

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])

        assertEquals(ForceScreenOnEffect(true), viewEffectTester.values().last())
    }

    @Test
    fun `PauseEvent, pauses song via media player and sets playing false in ViewState`() {
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, schedulerProvider)

        val viewStateTester = viewModel.viewState.test()
        val viewEffectTester = viewModel.viewEffects.test()

        viewModel.apply {
            processInput(UiCreateEvent)
            processInput(UiStartEvent)
            processInput(PlayEvent)
            processInput(CurrentPositionEvent(5))
            processInput(PauseEvent)
        }

        viewStateTester.assertValueAt(6) { vs ->
            val song = testSongs[0].song.toPlaylistViewSong()
            assertEquals(
                MusicPlayerViewState.INITIAL.copy(
                    loading = false,
                    songTitle = song.title,
                    songInfoLabel = song.infoLabel,
                    albumArt = song.albumArt,
                    totalDuration = TEST_SONG_DURATION,
                    elapsedTime = 5,
                    playing = false,
                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
                    upNextSongs = upNextSongsController.currentPlaylist()
                        .map { it.toPlaylistViewSong() }
                ),
                vs
            )
            true
        }

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.Pause, mediaPlayerController.commands[3])

        assertEquals(ForceScreenOnEffect(false), viewEffectTester.values().last())
    }

    @Test
    fun `NextSongEvent, loads and plays next song from beginning`() {
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, schedulerProvider)

        val viewStateTester = viewModel.viewState.test()
        val viewEffectTester = viewModel.viewEffects.test()

        viewModel.apply {
            processInput(UiCreateEvent)
            processInput(UiStartEvent)
            processInput(PlayEvent)
            processInput(CurrentPositionEvent(5))
            processInput(NextSongEvent)
        }

        viewStateTester.assertValueAt(7) { vs ->
            val song = testSongs[1].song.toPlaylistViewSong()
            assertEquals(
                MusicPlayerViewState.INITIAL.copy(
                    loading = false,
                    songTitle = song.title,
                    songInfoLabel = song.infoLabel,
                    albumArt = song.albumArt,
                    totalDuration = TEST_SONG_DURATION,
                    elapsedTime = 0,
                    playing = true,
                    nextSongLabel = "Up Next: ${testSongs[2].song.title}",
                    upNextSongs = upNextSongsController.currentPlaylist()
                        .map { it.toPlaylistViewSong() }
                ),
                vs
            )
            true
        }

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[3])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[4])

        assertEquals(ForceScreenOnEffect(true), viewEffectTester.values().last())
    }

    @Test
    fun `PreviousSongEvent, loads and plays previous song from beginning`() {
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, schedulerProvider)

        val viewStateTester = viewModel.viewState.test()
        val viewEffectTester = viewModel.viewEffects.test()

        viewModel.apply {
            processInput(UiCreateEvent)
            processInput(UiStartEvent)
            processInput(PlayEvent)
            processInput(CurrentPositionEvent(5))
            processInput(PreviousSongEvent)
        }

        viewStateTester.assertValueAt(7) { vs ->
            val song = testSongs[2].song.toPlaylistViewSong()
            assertEquals(
                MusicPlayerViewState.INITIAL.copy(
                    loading = false,
                    songTitle = song.title,
                    songInfoLabel = song.infoLabel,
                    albumArt = song.albumArt,
                    totalDuration = TEST_SONG_DURATION,
                    elapsedTime = 0,
                    playing = true,
                    nextSongLabel = "Up Next: ${testSongs[0].song.title}",
                    upNextSongs = upNextSongsController.currentPlaylist()
                        .map { it.toPlaylistViewSong() }
                ),
                vs
            )
            true
        }

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[3])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[4])

        assertEquals(ForceScreenOnEffect(true), viewEffectTester.values().last())
    }

    @Test
    fun `NewSongEvent, loads and plays new song from beginning`() {
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, schedulerProvider)

        val viewStateTester = viewModel.viewState.test()
        val viewEffectTester = viewModel.viewEffects.test()

        viewModel.apply {
            processInput(UiCreateEvent)
            processInput(UiStartEvent)
            processInput(PlayEvent)
            processInput(CurrentPositionEvent(5))
            processInput(NewSongEvent(R.raw.owl_city_fireflies))
        }

        viewStateTester.assertValueAt(7) { vs ->
            val song = testSongs[2].song.toPlaylistViewSong()
            assertEquals(
                MusicPlayerViewState.INITIAL.copy(
                    loading = false,
                    songTitle = song.title,
                    songInfoLabel = song.infoLabel,
                    albumArt = song.albumArt,
                    totalDuration = TEST_SONG_DURATION,
                    elapsedTime = 0,
                    playing = true,
                    nextSongLabel = "Up Next: ${testSongs[0].song.title}",
                    upNextSongs = upNextSongsController.currentPlaylist()
                        .map { it.toPlaylistViewSong() }
                ),
                vs
            )
            true
        }

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[3])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[4])

        assertEquals(ForceScreenOnEffect(true), viewEffectTester.values().last())
    }

    @Test
    fun `SongCompletedEvent, loads and plays next song from beginning since current song is playing`() {
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, schedulerProvider)

        val viewStateTester = viewModel.viewState.test()
        val viewEffectTester = viewModel.viewEffects.test()

        viewModel.apply {
            processInput(UiCreateEvent)
            processInput(UiStartEvent)
            processInput(PlayEvent)
            processInput(CurrentPositionEvent(TEST_SONG_DURATION.toInt()))
            processInput(SongCompletedEvent)
        }

        viewStateTester.assertValueAt(7) { vs ->
            val song = testSongs[1].song.toPlaylistViewSong()
            assertEquals(
                MusicPlayerViewState.INITIAL.copy(
                    loading = false,
                    songTitle = song.title,
                    songInfoLabel = song.infoLabel,
                    albumArt = song.albumArt,
                    totalDuration = TEST_SONG_DURATION,
                    elapsedTime = 0,
                    playing = true,
                    nextSongLabel = "Up Next: ${testSongs[2].song.title}",
                    upNextSongs = upNextSongsController.currentPlaylist()
                        .map { it.toPlaylistViewSong() }
                ),
                vs
            )
            true
        }

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[3])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[4])

        assertEquals(ForceScreenOnEffect(true), viewEffectTester.values().last())
    }

    @Test
    fun `SeekForwardEvent, seeks forward by SEEK_DURATION and continues playing`() {
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, schedulerProvider)

        val viewStateTester = viewModel.viewState.test()

        mediaPlayerController._currentPosition = 2000
        viewModel.apply {
            processInput(UiCreateEvent)
            processInput(UiStartEvent)
            processInput(PlayEvent)
            processInput(CurrentPositionEvent(2000))
            processInput(SeekForwardEvent)
        }

        viewStateTester.assertValueAt(6) { vs ->
            val song = testSongs[0].song.toPlaylistViewSong()
            assertEquals(
                MusicPlayerViewState.INITIAL.copy(
                    loading = false,
                    songTitle = song.title,
                    songInfoLabel = song.infoLabel,
                    albumArt = song.albumArt,
                    totalDuration = TEST_SONG_DURATION,
                    elapsedTime = 7000,
                    playing = true,
                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
                    upNextSongs = upNextSongsController.currentPlaylist()
                        .map { it.toPlaylistViewSong() }
                ),
                vs
            )
            true
        }

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.SeekBy, mediaPlayerController.commands[3])
    }

    @Test
    fun `SeekBackwardEvent, seeks backward by SEEK_DURATION and continues playing`() {
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, schedulerProvider)

        val viewStateTester = viewModel.viewState.test()

        mediaPlayerController._currentPosition = 10000
        viewModel.apply {
            processInput(UiCreateEvent)
            processInput(UiStartEvent)
            processInput(PlayEvent)
            processInput(CurrentPositionEvent(10000))
            processInput(SeekBackwardEvent)
        }

        viewStateTester.assertValueAt(6) { vs ->
            val song = testSongs[0].song.toPlaylistViewSong()
            assertEquals(
                MusicPlayerViewState.INITIAL.copy(
                    loading = false,
                    songTitle = song.title,
                    songInfoLabel = song.infoLabel,
                    albumArt = song.albumArt,
                    totalDuration = TEST_SONG_DURATION,
                    elapsedTime = 5000,
                    playing = true,
                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
                    upNextSongs = upNextSongsController.currentPlaylist()
                        .map { it.toPlaylistViewSong() }
                ),
                vs
            )
            true
        }

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.SeekBy, mediaPlayerController.commands[3])
    }

    @Test
    fun `SeekToEvent, seeks to specified position and continues playing`() {
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, schedulerProvider)

        val viewStateTester = viewModel.viewState.test()

        viewModel.apply {
            processInput(UiCreateEvent)
            processInput(UiStartEvent)
            processInput(PlayEvent)
            processInput(SeekToEvent((TEST_SONG_DURATION/2).toInt()))
        }

        viewStateTester.assertValueAt(5) { vs ->
            val song = testSongs[0].song.toPlaylistViewSong()
            assertEquals(
                MusicPlayerViewState.INITIAL.copy(
                    loading = false,
                    songTitle = song.title,
                    songInfoLabel = song.infoLabel,
                    albumArt = song.albumArt,
                    totalDuration = TEST_SONG_DURATION,
                    elapsedTime = (TEST_SONG_DURATION/2).toInt(),
                    playing = true,
                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
                    upNextSongs = upNextSongsController.currentPlaylist()
                        .map { it.toPlaylistViewSong() }
                ),
                vs
            )
            true
        }

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.SeekTo, mediaPlayerController.commands[3])
    }

    @Test
    fun `SeekForwardEvent, currently not playing so only seeks forward by SEEK_DURATION`() {
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, schedulerProvider)

        val viewStateTester = viewModel.viewState.test()

        mediaPlayerController._currentPosition = 2000
        viewModel.apply {
            processInput(UiCreateEvent)
            processInput(UiStartEvent)
            processInput(CurrentPositionEvent(2000))
            processInput(SeekForwardEvent)
        }

        viewStateTester.assertValueAt(5) { vs ->
            val song = testSongs[0].song.toPlaylistViewSong()
            assertEquals(
                MusicPlayerViewState.INITIAL.copy(
                    loading = false,
                    songTitle = song.title,
                    songInfoLabel = song.infoLabel,
                    albumArt = song.albumArt,
                    totalDuration = TEST_SONG_DURATION,
                    elapsedTime = 7000,
                    playing = false,
                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
                    upNextSongs = upNextSongsController.currentPlaylist()
                        .map { it.toPlaylistViewSong() }
                ),
                vs
            )
            true
        }

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.SeekBy, mediaPlayerController.commands[2])
    }

    @Test
    fun `SeekBackwardEvent, currently not playing so only seeks backward by SEEK_DURATION`() {
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, schedulerProvider)

        val viewStateTester = viewModel.viewState.test()

        mediaPlayerController._currentPosition = 10000
        viewModel.apply {
            processInput(UiCreateEvent)
            processInput(UiStartEvent)
            processInput(CurrentPositionEvent(10000))
            processInput(SeekBackwardEvent)
        }

        viewStateTester.assertValueAt(5) { vs ->
            val song = testSongs[0].song.toPlaylistViewSong()
            assertEquals(
                MusicPlayerViewState.INITIAL.copy(
                    loading = false,
                    songTitle = song.title,
                    songInfoLabel = song.infoLabel,
                    albumArt = song.albumArt,
                    totalDuration = TEST_SONG_DURATION,
                    elapsedTime = 5000,
                    playing = false,
                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
                    upNextSongs = upNextSongsController.currentPlaylist()
                        .map { it.toPlaylistViewSong() }
                ),
                vs
            )
            true
        }

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.SeekBy, mediaPlayerController.commands[2])
    }

    @Test
    fun `SeekToEvent, currently not playing so only seeks to specified position`() {
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, schedulerProvider)

        val viewStateTester = viewModel.viewState.test()

        viewModel.apply {
            processInput(UiCreateEvent)
            processInput(UiStartEvent)
            processInput(SeekToEvent((TEST_SONG_DURATION/2).toInt()))
        }

        viewStateTester.assertValueAt(4) { vs ->
            val song = testSongs[0].song.toPlaylistViewSong()
            assertEquals(
                MusicPlayerViewState.INITIAL.copy(
                    loading = false,
                    songTitle = song.title,
                    songInfoLabel = song.infoLabel,
                    albumArt = song.albumArt,
                    totalDuration = TEST_SONG_DURATION,
                    elapsedTime = (TEST_SONG_DURATION/2).toInt(),
                    playing = false,
                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
                    upNextSongs = upNextSongsController.currentPlaylist()
                        .map { it.toPlaylistViewSong() }
                ),
                vs
            )
            true
        }

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.SeekTo, mediaPlayerController.commands[2])
    }

    @Test
    fun `UiStopEvent, song is playing, should release media player but state should remain playing`() {
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, schedulerProvider)

        val viewStateTester = viewModel.viewState.test()

        viewModel.apply {
            processInput(UiCreateEvent)
            processInput(UiStartEvent)
            processInput(PlayEvent)
            processInput(UiStopEvent)
        }

        viewStateTester.assertValueAt(4) { vs ->
            val song = testSongs[0].song.toPlaylistViewSong()
            assertEquals(
                MusicPlayerViewState.INITIAL.copy(
                    loading = false,
                    songTitle = song.title,
                    songInfoLabel = song.infoLabel,
                    albumArt = song.albumArt,
                    totalDuration = TEST_SONG_DURATION,
                    playing = true,
                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
                    upNextSongs = upNextSongsController.currentPlaylist()
                        .map { it.toPlaylistViewSong() }
                ),
                vs
            )
            true
        }

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Start, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.Pause, mediaPlayerController.commands[3])
        assertEquals(MediaPlayerCommand.Release, mediaPlayerController.commands[4])
    }

    @Test
    fun `UiStopEvent, song is not playing, should release media player`() {
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, schedulerProvider)

        val viewStateTester = viewModel.viewState.test()

        viewModel.apply {
            processInput(UiCreateEvent)
            processInput(UiStartEvent)
            processInput(UiStopEvent)
        }

        viewStateTester.assertValueAt(3) { vs ->
            val song = testSongs[0].song.toPlaylistViewSong()
            assertEquals(
                MusicPlayerViewState.INITIAL.copy(
                    loading = false,
                    songTitle = song.title,
                    songInfoLabel = song.infoLabel,
                    albumArt = song.albumArt,
                    totalDuration = TEST_SONG_DURATION,
                    playing = false,
                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
                    upNextSongs = upNextSongsController.currentPlaylist()
                        .map { it.toPlaylistViewSong() }
                ),
                vs
            )
            true
        }

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Pause, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.Release, mediaPlayerController.commands[3])
    }

    @Test
    fun `UiStartEvent after UiStopEvent while player was playing, should load same song and continue playing from same time`() {
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, schedulerProvider)

        val viewStateTester = viewModel.viewState.test()

        mediaPlayerController._currentPosition = 10000
        viewModel.apply {
            processInput(UiCreateEvent)
            processInput(UiStartEvent)
            processInput(PlayEvent)
            processInput(CurrentPositionEvent(10000))
            processInput(UiStopEvent)
            processInput(UiStartEvent)
        }

        viewStateTester.assertValueAt(7) { vs ->
            val song = testSongs[0].song.toPlaylistViewSong()
            assertEquals(
                MusicPlayerViewState.INITIAL.copy(
                    loading = false,
                    songTitle = song.title,
                    songInfoLabel = song.infoLabel,
                    albumArt = song.albumArt,
                    totalDuration = TEST_SONG_DURATION,
                    elapsedTime = 10000,
                    playing = true,
                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
                    upNextSongs = upNextSongsController.currentPlaylist()
                        .map { it.toPlaylistViewSong() }
                ),
                vs
            )
            true
        }

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
    fun `UiStartEvent after UiStopEvent while player was paused, should load same song from same elapsed time`() {
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, schedulerProvider)

        val viewStateTester = viewModel.viewState.test()

        mediaPlayerController._currentPosition = 10000
        viewModel.apply {
            processInput(UiCreateEvent)
            processInput(UiStartEvent)
            processInput(CurrentPositionEvent(10000))
            processInput(UiStopEvent)
            processInput(UiStartEvent)
        }

        viewStateTester.assertValueAt(6) { vs ->
            val song = testSongs[0].song.toPlaylistViewSong()
            assertEquals(
                MusicPlayerViewState.INITIAL.copy(
                    loading = false,
                    songTitle = song.title,
                    songInfoLabel = song.infoLabel,
                    albumArt = song.albumArt,
                    totalDuration = TEST_SONG_DURATION,
                    elapsedTime = 10000,
                    playing = false,
                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
                    upNextSongs = upNextSongsController.currentPlaylist()
                        .map { it.toPlaylistViewSong() }
                ),
                vs
            )
            true
        }

        assertEquals(MediaPlayerCommand.Init, mediaPlayerController.commands[0])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[1])
        assertEquals(MediaPlayerCommand.Pause, mediaPlayerController.commands[2])
        assertEquals(MediaPlayerCommand.Release, mediaPlayerController.commands[3])
        assertEquals(MediaPlayerCommand.LoadSong, mediaPlayerController.commands[4])
        assertEquals(MediaPlayerCommand.SeekTo, mediaPlayerController.commands[5])
    }

    @Test
    fun `NextSongEvent, chain of multiple next songs, should land on expected song`() {
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, schedulerProvider)

        val viewStateTester = viewModel.viewState.test()

        viewModel.apply {
            processInput(UiCreateEvent)
            processInput(UiStartEvent)
            processInput(PlayEvent)
            processInput(NextSongEvent)
            processInput(NextSongEvent)
            processInput(NextSongEvent)
            processInput(NextSongEvent)
            processInput(NextSongEvent)
            processInput(NextSongEvent)
            processInput(NextSongEvent)
            processInput(NextSongEvent)
        }

        viewStateTester.assertValueAt(20) { vs ->
            val song = testSongs[2].song.toPlaylistViewSong()
            assertEquals(
                MusicPlayerViewState.INITIAL.copy(
                    loading = false,
                    songTitle = song.title,
                    songInfoLabel = song.infoLabel,
                    albumArt = song.albumArt,
                    totalDuration = TEST_SONG_DURATION,
                    elapsedTime = 0,
                    playing = true,
                    nextSongLabel = "Up Next: ${testSongs[0].song.title}",
                    upNextSongs = upNextSongsController.currentPlaylist()
                        .map { it.toPlaylistViewSong() }
                ),
                vs
            )
            true
        }
    }

    @Test
    fun `UiStartEvent, loading song fails, should show toast`() {
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, schedulerProvider)

        mediaPlayerController._loadNewSongResultsInError = true

        val viewStateTester = viewModel.viewState.test()
        val viewEffectTester = viewModel.viewEffects.test()

        viewModel.processInput(UiCreateEvent)
        viewModel.processInput(UiStartEvent)

        viewStateTester.assertValueAt(1) { vs ->
            assertEquals(
                MusicPlayerViewState.INITIAL.copy(
                    upNextSongs = upNextSongsController.currentPlaylist()
                        .map { it.toPlaylistViewSong() }
                ),
                vs
            )
            true
        }

        assertTrue { viewEffectTester.values().last() is ShowErrorEffect }
    }

    @Test
    fun `NewSongEvent, loading song fails, should show toast`() {
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, schedulerProvider)

        mediaPlayerController._loadNewSongResultsInError = true

        val viewStateTester = viewModel.viewState.test()
        val viewEffectTester = viewModel.viewEffects.test()

        viewModel.processInput(UiCreateEvent)
        viewModel.processInput(NewSongEvent(R.raw.dua_lipa_levitating))

        viewStateTester.assertValueAt(1) { vs ->
            assertEquals(
                MusicPlayerViewState.INITIAL.copy(
                    upNextSongs = upNextSongsController.currentPlaylist()
                        .map { it.toPlaylistViewSong() }
                ),
                vs
            )
            true
        }

        assertTrue { viewEffectTester.values().last() is ShowErrorEffect }
    }

    @Test
    fun `UiStartEvent, loading song fails, should show toast, NextSongEvent succeeds, should update ViewState`() {
        viewModel = MusicPlayerViewModel(upNextSongsController, mediaPlayerController, schedulerProvider)

        mediaPlayerController._loadNewSongResultsInError = true

        val viewStateTester = viewModel.viewState.test()
        val viewEffectTester = viewModel.viewEffects.test()

        viewModel.processInput(UiCreateEvent)
        viewModel.processInput(UiStartEvent)

        viewStateTester.assertValueAt(2) { vs ->
            val song = testSongs[0].song.toPlaylistViewSong()
            assertEquals(
                MusicPlayerViewState.INITIAL.copy(
                    loading = false,
                    songTitle = song.title,
                    songInfoLabel = song.infoLabel,
                    albumArt = song.albumArt,
                    totalDuration = 1f,
                    nextSongLabel = "Up Next: ${testSongs[1].song.title}",
                    upNextSongs = upNextSongsController.currentPlaylist()
                        .map { it.toPlaylistViewSong() }
                ),
                vs
            )
            true
        }

        assertTrue { viewEffectTester.values().last() is ShowErrorEffect }

        mediaPlayerController._loadNewSongResultsInError = false
        viewModel.processInput(NextSongEvent)

        viewStateTester.assertValueAt(4) { vs ->
            val song = testSongs[1].song.toPlaylistViewSong()
            assertEquals(
                MusicPlayerViewState.INITIAL.copy(
                    loading = false,
                    songTitle = song.title,
                    songInfoLabel = song.infoLabel,
                    albumArt = song.albumArt,
                    totalDuration = TEST_SONG_DURATION,
                    elapsedTime = 0,
                    playing = true,
                    nextSongLabel = "Up Next: ${testSongs[2].song.title}",
                    upNextSongs = upNextSongsController.currentPlaylist()
                        .map { it.toPlaylistViewSong() }
                ),
                vs
            )
            true
        }
    }

    companion object {
        private const val TEST_SONG_DURATION = 60 * 1000f
    }
}
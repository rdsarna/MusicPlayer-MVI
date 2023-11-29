package com.ratulsarna.shared.vm

import com.ratulsarna.shared.CoroutineContextProvider
import com.ratulsarna.shared.MINIMUM_DURATION
import com.ratulsarna.shared.controllers.MediaPlayerController
import com.ratulsarna.shared.controllers.PlaylistSongsController
import com.ratulsarna.shared.interval
import com.ratulsarna.shared.repository.model.Song
import com.ratulsarna.shared.vm.MusicPlayerIntent.*
import com.ratulsarna.shared.vm.MusicPlayerPartialStateChange.*
import com.ratulsarna.shared.vm.MusicPlayerSideEffect.*
import com.ratulsarna.shared.vm.model.toPlaylistViewSong
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.MutableStateFlow
import com.rickclephas.kmm.viewmodel.coroutineScope
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("INLINE_FROM_HIGHER_PLATFORM")
class MusicPlayerViewModel : KoinComponent, KMMViewModel() {

    private val playlistSongsController: PlaylistSongsController by inject()
    private val mediaPlayerController: MediaPlayerController by inject()
    private val coroutineContextProvider: CoroutineContextProvider by inject()

    private var oneSecondIntervalJob: Job? = null
    private val sideEffectChannel = Channel<MusicPlayerSideEffect>(Channel.BUFFERED)

    private val _viewState = MutableStateFlow(viewModelScope, MusicPlayerViewState.INITIAL)

    @NativeCoroutinesState
    val viewState: StateFlow<MusicPlayerViewState> = _viewState.asStateFlow()
    @NativeCoroutines
    val sideEffects: Flow<MusicPlayerSideEffect> = sideEffectChannel.receiveAsFlow()

    init {
        mediaPlayerController.init(
            startedListener = {
                oneSecondIntervalJob = interval(1000).map {
                    mediaPlayerController.getCurrentPosition()
                }.onEach {
                    processInput(SongTickerIntent(it.toLong()))
                }.launchIn(viewModelScope.coroutineScope)
            },
            pausedStoppedListener = {
                oneSecondIntervalJob?.cancel()
            },
            songCompletedListener = {
                viewModelScope.coroutineScope.launch {
                    processInput(NextSongIntent)
                }
            }
        )
    }

    override fun onCleared() {
        oneSecondIntervalJob?.cancel()
        super.onCleared()
    }

    fun processInput(intent: MusicPlayerIntent) {
        println("Event = $intent")
        when (intent) {
            UiStartIntent -> handleUiStartIntent()
            UiStopIntent -> handleUiStopIntent()
            PlayIntent -> handlePlayIntent()
            PauseIntent -> handlePauseIntent()
            NextSongIntent -> handleNextSongIntent()
            PreviousSongIntent -> handlePreviousSongIntent()
            SeekForwardIntent -> handleSeekForwardIntent()
            SeekBackwardIntent -> handleSeekBackwardIntent()
            is SeekToIntent -> handleSeekToIntent(intent)
            is NewSongIntent -> handleNewSongIntent(intent)
            is SongTickerIntent -> handleSongTickerIntent(intent)
        }
    }

    private fun handleUiStartIntent() {
        viewModelScope.coroutineScope.launch {
            println("inside UiStartIntent")
            val playlist = playlistSongsController
                .loadDefaultPlaylistSongs()
                .map { it.toPlaylistViewSong() }
                .toImmutableList()

            // emit a loading state if new song load takes time
            val loadSuccess = withContext(coroutineContextProvider.io) {
                mediaPlayerController.loadNewSong(playlistSongsController.currentSong()?.songFileName)
            }
            val duration =
                if (loadSuccess) mediaPlayerController.getDuration() else MINIMUM_DURATION
            val currentViewState = viewState.value
            var playing = false
            if (loadSuccess) {
                when {
                    currentViewState.playing && currentViewState.elapsedTime > 0 ->
                        playing =
                            mediaPlayerController.seekToAndStart(currentViewState.elapsedTime.toInt())
                    currentViewState.elapsedTime > 0 ->
                        mediaPlayerController.seekTo(currentViewState.elapsedTime.toInt())
                    currentViewState.playing ->
                        playing = mediaPlayerController.start()
                }
            }
            val song = playlistSongsController.currentSong()
            _viewState.update { vs ->
                song?.let {
                    vs.copy(
                        playlist = playlist,
                        loading = false,
                        songTitle = song.title,
                        songInfoLabel = "${song.artistName} | ${song.year}",
                        albumArt = song.albumArtResource,
                        totalDuration = duration.toLong(),
                        playing = playing,
                        currentPlaylistSong = song.toPlaylistViewSong(),
                        elapsedTimeLabel = vs.elapsedTime.getTimeLabel(),
                        totalTimeLabel = duration.toLong().getTimeLabel(),
                    )
                } ?: vs
            }
            if (!loadSuccess) {
                sideEffectChannel.send(ShowErrorSideEffect("Error loading song. Try next song."))
            }
        }
    }

    private fun handleUiStopIntent() {
        viewModelScope.coroutineScope.launch {
            // Important note: We are pausing here (if player is playing) to stop playback
            // immediately but this pause is not propagated to the Ui because we want to
            // continue playback on the next UiStartEvent
            mediaPlayerController.pause()
            mediaPlayerController.release()
        }
    }

    private fun handlePlayIntent() {
        val playing = mediaPlayerController.start()
        _viewState.update { vs ->
            vs.copy(playing = playing)
        }
    }

    private fun handlePauseIntent() {
        val playing = mediaPlayerController.pause().not()
        _viewState.update { vs ->
            vs.copy(playing = playing)
        }
    }

    private fun handleNextSongIntent() {
        val nextSong = playlistSongsController.nextSong()
        processNewSong(nextSong)
    }

    private fun handlePreviousSongIntent() {
        val previousSong = playlistSongsController.previousSong()
        processNewSong(previousSong)
    }

    private fun handleSeekForwardIntent() {
        val position = mediaPlayerController.seekBy(SEEK_DURATION).toLong()
        _viewState.update { vs ->
            vs.copy(
                elapsedTime = position,
                elapsedTimeLabel = position.getTimeLabel()
            )
        }
    }

    private fun handleSeekBackwardIntent() {
        val position = mediaPlayerController.seekBy(-SEEK_DURATION).toLong()
        _viewState.update { vs ->
            vs.copy(
                elapsedTime = position,
                elapsedTimeLabel = position.getTimeLabel()
            )
        }
    }

    private fun handleSeekToIntent(intent: SeekToIntent) {
        val position = mediaPlayerController.seekTo(intent.position.toInt()).toLong()
        _viewState.update { vs ->
            vs.copy(
                elapsedTime = position,
                elapsedTimeLabel = position.getTimeLabel()
            )
        }
    }

    private fun handleNewSongIntent(intent: NewSongIntent) {
        val newSong = if (intent.songId != viewState.value.currentPlaylistSong?.id) {
            playlistSongsController.newSong(intent .songId)
        } else {
            null
        }
        processNewSong(newSong)
    }

    private fun handleSongTickerIntent(intent: SongTickerIntent) {
        _viewState.update { vs ->
            vs.copy(
                elapsedTime = intent.position,
                elapsedTimeLabel = intent.position.getTimeLabel()
            )
        }
    }

    private fun processNewSong(song: Song?) {
        // initial update while song is loaded
        _viewState.update { vs ->
            song?.let {
                val duration = vs.totalDuration
                vs.copy(
                    loading = false,
                    songTitle = song.title,
                    songInfoLabel = "${song.artistName} | ${song.year}",
                    albumArt = song.albumArtResource,
                    elapsedTime = 0,
                    totalDuration = duration,
                    playing = false,
                    currentPlaylistSong = song.toPlaylistViewSong(),
                    elapsedTimeLabel = 0L.getTimeLabel(),
                    totalTimeLabel = duration.getTimeLabel(),
                )
            } ?: vs
        }
        viewModelScope.coroutineScope.launch {
            val loadSuccess = withContext(coroutineContextProvider.io) {
                val loadSuccess = mediaPlayerController.loadNewSong(song?.songFileName)
                if (!loadSuccess) mediaPlayerController.release()
                return@withContext loadSuccess
            }
            val (duration, playing) = if (loadSuccess) {
                mediaPlayerController.getDuration() to mediaPlayerController.start()
            } else {
                MINIMUM_DURATION to false
            }
            _viewState.update { vs ->
                song?.let {
                    vs.copy(
                        loading = false,
                        songTitle = song.title,
                        songInfoLabel = "${song.artistName} | ${song.year}",
                        albumArt = song.albumArtResource,
                        elapsedTime = 0,
                        totalDuration = duration.toLong(),
                        playing = playing,
                        currentPlaylistSong = song.toPlaylistViewSong(),
                        elapsedTimeLabel = 0L.getTimeLabel(),
                        totalTimeLabel = duration.toLong().getTimeLabel(),
                    )
                } ?: vs
            }
            if (!loadSuccess) {
                sideEffectChannel.send(ShowErrorSideEffect("Error loading song. Try next song."))
            }
        }
    }

    private fun Long.getTimeLabel(): String {
        val minutes = this / (1000 * 60)
        val seconds = this / 1000 % 60
        return "$minutes:${if (seconds < 10) "0$seconds" else seconds}"
    }

    companion object {
        const val SEEK_DURATION = 5000 // ms
    }
}

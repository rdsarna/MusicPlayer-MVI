package com.ratulsarna.musicplayer.ui

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ratulsarna.musicplayer.repository.model.Song
import com.ratulsarna.musicplayer.ui.MusicPlayerEffect.ForceScreenOnEffect
import com.ratulsarna.musicplayer.ui.MusicPlayerEffect.ShowErrorEffect
import com.ratulsarna.musicplayer.ui.MusicPlayerEvent.*
import com.ratulsarna.musicplayer.ui.MusicPlayerResult.*
import com.ratulsarna.musicplayer.ui.controllers.MediaPlayerController
import com.ratulsarna.musicplayer.ui.controllers.UpNextSongsController
import com.ratulsarna.musicplayer.ui.model.PlaylistViewSong
import com.ratulsarna.musicplayer.ui.model.toPlaylistViewSong
import com.ratulsarna.musicplayer.utils.CoroutineContextProvider
import com.ratulsarna.musicplayer.utils.MINIMUM_DURATION
import com.ratulsarna.musicplayer.utils.interval
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MusicPlayerViewModel @Inject constructor(
    private val upNextSongsController: UpNextSongsController,
    private val mediaPlayerController: MediaPlayerController,
    private val coroutineContextProvider: CoroutineContextProvider,
) : ViewModel() {

    private val _viewState = MutableStateFlow(MusicPlayerViewState.INITIAL)
    private val _viewEffects = MutableSharedFlow<MusicPlayerEffect>()

    val viewState = _viewState.asStateFlow()
    val viewEffects = _viewEffects.asSharedFlow()

    private var oneSecondIntervalJob: Job? = null
    private var uiStartJob: Job? = null
    private var newSongJob: Job? = null

    init {
        mediaPlayerController.init({
            oneSecondIntervalJob = interval(1000, TimeUnit.MILLISECONDS).map {
                mediaPlayerController.getCurrentPosition()
            }.onEach {
                processInput(CurrentPositionEvent(it))
            }.launchIn(viewModelScope)
        }, {
            oneSecondIntervalJob?.cancel()
        }) { processInput(SongCompletedEvent) }
    }

    fun processInput(event: MusicPlayerEvent) {
        handleEvents(event)
    }

    private fun handleEvents(event: MusicPlayerEvent) {
        when (event) {
            UiCreateEvent -> handleUiCreate()
            UiStartEvent -> handleUiStart()
            UiStopEvent -> handleUiStop()
            PlayEvent -> handlePlay()
            PauseEvent -> handlePause()
            NextSongEvent -> handleNewSong(upNextSongsController.nextSong())
            PreviousSongEvent -> handleNewSong(upNextSongsController.previousSong())
            SeekForwardEvent -> handleSeekBy(SEEK_DURATION)
            SeekBackwardEvent -> handleSeekBy(-SEEK_DURATION)
            is SeekToEvent -> handleSeekTo(event.position)
            SongCompletedEvent -> handleNewSong(upNextSongsController.nextSong())
            is CurrentPositionEvent -> processResult(CurrentPositionResult(event.position))
            is NewSongEvent -> handleNewSong(upNextSongsController.newSong(event.songId))
        }
    }

    private fun handleUiCreate() {
        val result = UiCreateResult(
            upNextSongsController.loadDefaultPlaylistSongs()
                .map { it.toPlaylistViewSong() }
        )
        processResult(result)
    }

    private fun handleUiStart() {
        processResult(
            UiStartResult(
                upNextSongsController.currentSong(),
                upNextSongsController.peekNextSong(),
                1,
                playing = null,
                errorLoadingSong = false,
            )
        )
        uiStartJob = viewModelScope.launch {
            val loadSuccess = withContext(coroutineContextProvider.io) {
                mediaPlayerController.loadNewSong(upNextSongsController.currentSong())
            }
            val duration = if (loadSuccess) mediaPlayerController.getDuration() else MINIMUM_DURATION
            val currentViewState = viewState.value
            var playing = false
            when {
                currentViewState.playing && currentViewState.elapsedTime > 0 -> {
                    playing = mediaPlayerController.seekToAndStart(currentViewState.elapsedTime)
                }
                currentViewState.elapsedTime > 0 -> {
                    mediaPlayerController.seekTo(currentViewState.elapsedTime)
                }
                currentViewState.playing -> {
                    playing = mediaPlayerController.start()
                }
            }
            processResult(
                UiStartResult(
                    upNextSongsController.currentSong(),
                    upNextSongsController.peekNextSong(),
                    duration,
                    playing = playing,
                    errorLoadingSong = !loadSuccess,
                )
            )
        }
    }

    private fun handleUiStop() {
        // Important note: We are pausing here (if player is playing) to stop playback
        // immediately but this pause is not propagated to the Ui because we want to
        // continue playback on the next UiStartEvent
        mediaPlayerController.pause()
        mediaPlayerController.release()
        processResult(UiStopResult)
    }

    private fun handlePlay() {
        val startSuccess = mediaPlayerController.start()
        processResult(PlayResult(startSuccess))
    }

    private fun handlePause() {
        val pauseSuccess = mediaPlayerController.pause()
        processResult(PauseResult(!pauseSuccess))
    }

    private fun handleSeekBy(duration: Int) {
        val newPosition = mediaPlayerController.seekBy(duration)
        processResult(SeekToResult(newPosition))
    }

    private fun handleSeekTo(position: Int) {
        val newPosition = mediaPlayerController.seekTo(position)
        processResult(SeekToResult(newPosition))
    }

    private fun handleNewSong(song: Song?) {
        processResult(
            NewSongResult(
                song,
                upNextSongsController.peekNextSong(),
                -1,
                upNextSongList,
                playing = false,
                errorLoading = false,
            )
        )
        newSongJob?.cancel()
        newSongJob = viewModelScope.launch {
            val loadSuccess = withContext(coroutineContextProvider.io) {
                val loadSuccess = mediaPlayerController.loadNewSong(song)
                if (!loadSuccess) mediaPlayerController.release()
                return@withContext loadSuccess
            }
            val (duration, playing) = if (loadSuccess) {
                mediaPlayerController.getDuration() to mediaPlayerController.start()
            } else {
                MINIMUM_DURATION to false
            }
            processResult(
                NewSongResult(
                    song,
                    upNextSongsController.peekNextSong(),
                    duration,
                    upNextSongList,
                    playing = playing,
                    errorLoading = !loadSuccess
                )
            )
        }
    }

    private fun processResult(result: MusicPlayerResult) {
        updateState {
            result.toViewState(it)
        }
        result.toViewEffect(viewState.value)?.let {
            emitSideEffect(it)
        }
    }

    private fun MusicPlayerResult.toViewState(oldState: MusicPlayerViewState): MusicPlayerViewState =
        when (this) {
            is UiCreateResult -> oldState.copy(upNextSongs = this.upNextSongList)
            is UiStartResult -> this.song?.let { song ->
                oldState.copy(
                    loading = false,
                    songTitle = song.title,
                    songInfoLabel = "${song.artistName} | ${song.year}",
                    albumArt = song.albumArtResId,
                    totalDuration = this.duration.toFloat(),
                    playing = this.playing ?: oldState.playing,
                    nextSongLabel = "Up Next: ${this.nextSong?.title}",
                )
            } ?: oldState
            is NewSongResult -> this.song?.let { song ->
                oldState.copy(
                    loading = false,
                    songTitle = song.title,
                    songInfoLabel = "${song.artistName} | ${song.year}",
                    albumArt = song.albumArtResId,
                    nextSongLabel = "Up Next: ${this.nextSong?.title}",
                    elapsedTime = 0,
                    totalDuration = (if (this.duration == -1) oldState.totalDuration else this.duration).toFloat(),
                    playing = this.playing,
                    upNextSongs = this.upNextSongList
                )
            } ?: oldState
            is SeekToResult -> oldState.copy(elapsedTime = this.position)
            UiStopResult -> oldState
            is PauseResult -> oldState.copy(playing = this.playing)
            is PlayResult -> oldState.copy(playing = this.playing)
            is CurrentPositionResult -> oldState.copy(elapsedTime = this.position)
        }

    private fun MusicPlayerResult.toViewEffect(oldState: MusicPlayerViewState): MusicPlayerEffect? =
        when (this) {
            is PlayResult -> ForceScreenOnEffect(true)
            is PauseResult -> ForceScreenOnEffect(false)
            is UiStartResult -> if (this.errorLoadingSong) {
                ShowErrorEffect("Error loading song. Try next song.")
            } else {
                null
            }
            is NewSongResult -> when {
                this.errorLoading -> ShowErrorEffect("Error loading song. Try next song.")
                oldState.playing -> ForceScreenOnEffect(true)
                else -> null
            }
            else -> null
        }

    private val upNextSongList: List<PlaylistViewSong>
        get() = upNextSongsController.currentUpNextSongList().map { it.toPlaylistViewSong() }

    private fun updateState(updateBlock: (MusicPlayerViewState) -> MusicPlayerViewState) {
        viewModelScope.launch {
            _viewState.update {
                updateBlock(it)
            }
        }
    }

    private fun emitSideEffect(sideEffect: MusicPlayerEffect) {
        viewModelScope.launch {
            _viewEffects.emit(sideEffect)
        }
    }

    companion object {
        @VisibleForTesting const val SEEK_DURATION = 5000 // ms
    }
}

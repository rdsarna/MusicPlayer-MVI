package com.ratulsarna.musicplayer.ui

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ratulsarna.musicplayer.repository.model.Song
import com.ratulsarna.musicplayer.ui.MusicPlayerEffect.*
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MusicPlayerViewModel @Inject constructor(
    private val upNextSongsController: UpNextSongsController,
    private val mediaPlayerController: MediaPlayerController,
    private val coroutineContextProvider: CoroutineContextProvider
) : ViewModel() {

    private var oneSecondIntervalJob: Job? = null
    private val _eventFlow = MutableSharedFlow<MusicPlayerEvent>()
    private val viewEffectChannel = Channel<MusicPlayerEffect>(Channel.BUFFERED)

    val viewState: StateFlow<MusicPlayerViewState>
    val viewEffects: Flow<MusicPlayerEffect> = viewEffectChannel.receiveAsFlow()

    init {
        val initialVS = MusicPlayerViewState.INITIAL
        viewState = _eventFlow
            .onEach { Timber.d("Event = $it") }
            .eventToResult()
            .onEach { Timber.d("Result = $it") }
            .resultToViewEffect()
            .resultToViewState()
            .onEach { Timber.d("ViewState = $it") }
            .catch {
                it.printStackTrace()
                Timber.e(it, "Something has gone horribly wrong")
            }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                initialVS
            )

        mediaPlayerController.init(
            startedListener = {
                oneSecondIntervalJob = interval(1000, TimeUnit.MILLISECONDS).map {
                    mediaPlayerController.getCurrentPosition()
                }.onEach {
                    processInput(CurrentPositionEvent(it))
                }.launchIn(viewModelScope)
            },
            pausedStoppedListener = {
                oneSecondIntervalJob?.cancel()
            },
            songCompletedListener = {
                viewModelScope.launch {
                    processInput(SongCompletedEvent)
                }
            }
        )
    }

    override fun onCleared() {
        oneSecondIntervalJob?.cancel()
        super.onCleared()
    }

    suspend fun processInput(event: MusicPlayerEvent) {
        Timber.d("-----------here2")
        _eventFlow.emit(event)
    }

    private fun Flow<MusicPlayerEvent>.eventToResult(): Flow<MusicPlayerResult> {
        return merge(
            onUiCreate(filterIsInstance()),
            onUiStart(filterIsInstance()),
            onUiStop(filterIsInstance()),
            onPlay(filterIsInstance()),
            onPause(filterIsInstance()),
            onNextSong(filterIsInstance()),
            onPreviousSong(filterIsInstance()),
            onSeekForward(filterIsInstance()),
            onSeekBackward(filterIsInstance()),
            onSeekTo(filterIsInstance()),
            onSongCompleted(filterIsInstance()),
            onCurrentPosition(filterIsInstance()),
            onNewSong(filterIsInstance()),
        )
    }

    private fun Flow<MusicPlayerResult>.resultToViewState(): Flow<MusicPlayerViewState> {
        return scan(MusicPlayerViewState.INITIAL) { vs, result ->
            when (result) {
                is UiCreateResult -> vs.copy(upNextSongs = result.upNextSongList)
                is UiStartResult -> result.song?.let { song ->
                    vs.copy(
                        loading = false,
                        songTitle = song.title,
                        songInfoLabel = "${song.artistName} | ${song.year}",
                        albumArt = song.albumArtResId,
                        totalDuration = result.duration.toFloat(),
                        playing = result.playing ?: vs.playing,
                        nextSongLabel = "Up Next: ${result.nextSong?.title}",
                    )
                } ?: vs
                is NewSongResult -> {
                    Timber.d("-----------here4")
                    result.song?.let { song ->
                        vs.copy(
                            loading = false,
                            songTitle = song.title,
                            songInfoLabel = "${song.artistName} | ${song.year}",
                            albumArt = song.albumArtResId,
                            nextSongLabel = "Up Next: ${result.nextSong?.title}",
                            elapsedTime = 0,
                            totalDuration = (if (result.duration == -1) vs.totalDuration else result.duration).toFloat(),
                            playing = result.playing,
                            upNextSongs = result.upNextSongList
                        )
                    } ?: vs
                }
                is SeekToResult -> {
                    Timber.d("-----------here5")
                    vs.copy(elapsedTime = result.position)
                }
                UiStopResult -> vs
                is PauseResult -> vs.copy(playing = result.playing)
                is PlayResult -> vs.copy(playing = result.playing)
                is CurrentPositionResult -> vs.copy(elapsedTime = result.position)
            }
        }
            .distinctUntilChanged()
    }

    private fun Flow<MusicPlayerResult>.resultToViewEffect(): Flow<MusicPlayerResult> {
        return onEach { result ->
            val effect = when (result) {
                is PlayResult -> ForceScreenOnEffect(true)
                is PauseResult -> ForceScreenOnEffect(false)
                is UiStartResult -> when {
                    result.errorLoadingSong -> ShowErrorEffect("Error loading song. Try next song.")
                    else -> NoOpEffect
                }
                is NewSongResult -> when {
                    result.errorLoading -> ShowErrorEffect("Error loading song. Try next song.")
                    viewState.value.playing -> ForceScreenOnEffect(true)
                    else -> NoOpEffect
                }
                else -> NoOpEffect
            }
            Timber.d("SideEffect = $effect")
            viewEffectChannel.send(effect)
        }
    }

    private val upNextSongList: List<PlaylistViewSong>
        get() = upNextSongsController.currentUpNextSongList().map { it.toPlaylistViewSong() }

    private fun onUiCreate(flow: Flow<UiCreateEvent>): Flow<UiCreateResult> =
        flow.map {
            UiCreateResult(
                upNextSongsController.loadDefaultPlaylistSongs()
                    .map { it.toPlaylistViewSong() }
            )
        }

    private fun onUiStart(flow: Flow<UiStartEvent>): Flow<UiStartResult> =
        flow.map { upNextSongsController.currentSong() }
            .transformLatest {
                emit(
                    UiStartResult(
                        upNextSongsController.currentSong(),
                        upNextSongsController.peekNextSong(),
                        1,
                        playing = null,
                        errorLoadingSong = false,
                    )
                )
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
                emit(
                    UiStartResult(
                        upNextSongsController.currentSong(),
                        upNextSongsController.peekNextSong(),
                        duration,
                        playing = playing,
                        errorLoadingSong = !loadSuccess,
                    )
                )
            }
    private fun onUiStop(flow: Flow<UiStopEvent>): Flow<UiStopResult> =
        flow.map {
            // Important note: We are pausing here (if player is playing) to stop playback
            // immediately but this pause is not propagated to the Ui because we want to
            // continue playback on the next UiStartEvent
            mediaPlayerController.pause()
            mediaPlayerController.release()
            UiStopResult
        }
    private fun onPlay(flow: Flow<PlayEvent>): Flow<PlayResult> =
        flow.map {
            PlayResult(mediaPlayerController.start())
        }
    private fun onPause(flow: Flow<PauseEvent>): Flow<PauseResult> =
        flow.map {
            PauseResult(mediaPlayerController.pause().not())
        }

    private fun onNextSong(flow: Flow<NextSongEvent>): Flow<NewSongResult> =
        flow.map {
            Timber.d("-----------here3-2")
            upNextSongsController.nextSong()
        }.newSongResultFromSong()

    private fun onPreviousSong(flow: Flow<PreviousSongEvent>): Flow<NewSongResult> =
        flow.map {
            upNextSongsController.previousSong()
        }.newSongResultFromSong()

    private fun onSeekForward(flow: Flow<SeekForwardEvent>): Flow<SeekToResult> =
       flow.map {
           SeekToResult(
               mediaPlayerController.seekBy(SEEK_DURATION)
           )
       }
    private fun onSeekBackward(flow: Flow<SeekBackwardEvent>): Flow<SeekToResult> =
        flow.map {
            SeekToResult(
                mediaPlayerController.seekBy(-SEEK_DURATION)
            )
        }
    private fun onSeekTo(flow: Flow<SeekToEvent>): Flow<SeekToResult> =
        flow.map {
            Timber.d("-----------here3")
            SeekToResult(
                mediaPlayerController.seekTo(it.position)
            )
        }

    private fun onSongCompleted(flow: Flow<SongCompletedEvent>): Flow<NewSongResult> =
        flow.map {
            upNextSongsController.nextSong()
        }.newSongResultFromSong()

    private fun onCurrentPosition(flow: Flow<CurrentPositionEvent>): Flow<CurrentPositionResult> =
        flow.map { CurrentPositionResult(it.position) }

    private fun onNewSong(flow: Flow<NewSongEvent>): Flow<NewSongResult> =
        flow.map {
            upNextSongsController.newSong(it.songId)
        }.newSongResultFromSong()

    private fun Flow<Song?>.newSongResultFromSong(): Flow<NewSongResult> =
        transformLatest { song ->
            emit(
                NewSongResult(
                    song,
                    upNextSongsController.peekNextSong(),
                    -1,
                    upNextSongList,
                    playing = false,
                    errorLoading = false,
                )
            )
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
            emit(
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

    companion object {
        @VisibleForTesting const val SEEK_DURATION = 5000 // ms
    }
}

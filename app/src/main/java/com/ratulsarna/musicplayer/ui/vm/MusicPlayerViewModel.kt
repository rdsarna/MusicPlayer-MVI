package com.ratulsarna.musicplayer.ui.vm

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ratulsarna.musicplayer.controllers.MediaPlayerController
import com.ratulsarna.musicplayer.controllers.PlaylistSongsController
import com.ratulsarna.musicplayer.repository.model.Song
import com.ratulsarna.musicplayer.ui.MusicPlayerIntent
import com.ratulsarna.musicplayer.ui.MusicPlayerIntent.*
import com.ratulsarna.musicplayer.ui.MusicPlayerPartialStateChange
import com.ratulsarna.musicplayer.ui.MusicPlayerPartialStateChange.*
import com.ratulsarna.musicplayer.ui.MusicPlayerViewState
import com.ratulsarna.musicplayer.utils.CoroutineContextProvider
import com.ratulsarna.musicplayer.utils.MINIMUM_DURATION
import com.ratulsarna.musicplayer.utils.interval
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
class MusicPlayerViewModel @Inject constructor(
    private val playlistSongsController: PlaylistSongsController,
    private val mediaPlayerController: MediaPlayerController,
    private val coroutineContextProvider: CoroutineContextProvider,
) : ViewModel() {

    private var oneSecondIntervalJob: Job? = null
    private val _eventFlow = MutableSharedFlow<MusicPlayerIntent>()

    val viewState: StateFlow<MusicPlayerViewState>

    init {
        val initialVS = MusicPlayerViewState.INITIAL
        viewState = _eventFlow
            .onEach { Timber.d("Event = $it") }
            .intentToPartialStateChange()
            .onEach { Timber.d("Result = $it") }
            .partialChangeToViewState()
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
                    mediaPlayerController.getCurrentPosition().toLong()
                }.onEach {
                    processInput(SongTickerIntent(it))
                }.launchIn(viewModelScope)
            },
            pausedStoppedListener = {
                oneSecondIntervalJob?.cancel()
            },
            songCompletedListener = {
                viewModelScope.launch {
                    processInput(NextSongIntent)
                }
            }
        )
    }

    override fun onCleared() {
        oneSecondIntervalJob?.cancel()
        super.onCleared()
    }

    suspend fun processInput(event: MusicPlayerIntent) {
        _eventFlow.emit(event)
    }

    private fun Flow<MusicPlayerIntent>.intentToPartialStateChange(): Flow<MusicPlayerPartialStateChange> {
        return merge(
            onUiStart(filterIsInstance()),
            onUiStop(filterIsInstance()),
            onPlay(filterIsInstance()),
            onPause(filterIsInstance()),
            onNextSong(filterIsInstance()),
            onPreviousSong(filterIsInstance()),
            onSeekTo(filterIsInstance()),
            onCurrentPosition(filterIsInstance()),
        )
    }

    private fun Flow<MusicPlayerPartialStateChange>.partialChangeToViewState(): Flow<MusicPlayerViewState> {
        return scan(MusicPlayerViewState.INITIAL) { vs, partialStateChange ->
            when (partialStateChange) {
                is UiStartPartialStateChange -> partialStateChange.song?.let { song ->
                    val totalDuration = partialStateChange.duration
                    vs.copy(
                        loading = false,
                        songTitle = song.title,
                        songInfoLabel = "${song.artistName} | ${song.year}",
                        albumArt = song.albumArtResId,
                        totalDuration = totalDuration,
                        playing = partialStateChange.playing ?: vs.playing,
                        elapsedTimeLabel = vs.elapsedTime.getTimeLabel(),
                        totalTimeLabel = totalDuration.getTimeLabel(),
                    )
                } ?: vs
                is NewSongPartialStateChange -> {
                    partialStateChange.song?.let { song ->
                        val totalDuration = if (partialStateChange.duration == -1L) vs.totalDuration else partialStateChange.duration
                        vs.copy(
                            loading = false,
                            songTitle = song.title,
                            songInfoLabel = "${song.artistName} | ${song.year}",
                            albumArt = song.albumArtResId,
                            elapsedTime = 0L,
                            totalDuration = totalDuration,
                            playing = partialStateChange.playing,
                            elapsedTimeLabel = 0L.getTimeLabel(),
                            totalTimeLabel = totalDuration.getTimeLabel(),
                        )
                    } ?: vs
                }
                is SeekToPartialStateChange -> {
                    vs.copy(
                        elapsedTime = partialStateChange.position,
                        elapsedTimeLabel = partialStateChange.position.getTimeLabel()
                    )
                }
                UiStopPartialStateChange -> vs
                is PausePartialStateChange -> vs.copy(playing = partialStateChange.playing)
                is PlayPartialStateChange -> vs.copy(playing = partialStateChange.playing)
                is CurrentPositionPartialStateChange -> vs.copy(
                    elapsedTime = partialStateChange.position,
                    elapsedTimeLabel = partialStateChange.position.getTimeLabel()
                )
            }
        }
    }

    private fun onUiStart(flow: Flow<UiStartIntent>): Flow<UiStartPartialStateChange> =
        flow.transformLatest {
            // emit some loading state if loading playlist and song takes time

            playlistSongsController.loadDefaultPlaylistSongs()
            val currentSong = playlistSongsController.currentSong()

            val loadSuccess = withContext(coroutineContextProvider.io) {
                mediaPlayerController.loadNewSong(currentSong)
            }
            val duration =
                if (loadSuccess) mediaPlayerController.getDuration() else MINIMUM_DURATION
            val currentViewState = viewState.value
            val playing = if (currentViewState.playing) {
                mediaPlayerController.start()
            } else false
            emit(
                UiStartPartialStateChange(
                    playlistSongsController.currentSong(),
                    duration.toLong(),
                    playing = playing,
                    errorLoadingSong = !loadSuccess,
                )
            )
        }

    private fun onUiStop(flow: Flow<UiStopIntent>): Flow<UiStopPartialStateChange> =
        flow.map {
            // Important note: We are pausing here (if player is playing) to stop playback
            // immediately but this pause is not propagated to the Ui because we want to
            // continue playback on the next UiStartEvent
            mediaPlayerController.pause()
            mediaPlayerController.release()
            UiStopPartialStateChange
        }

    private fun onPlay(flow: Flow<PlayIntent>): Flow<PlayPartialStateChange> =
        flow.map {
            PlayPartialStateChange(mediaPlayerController.start())
        }

    private fun onPause(flow: Flow<PauseIntent>): Flow<PausePartialStateChange> =
        flow.map {
            PausePartialStateChange(mediaPlayerController.pause().not())
        }

    private fun onNextSong(flow: Flow<NextSongIntent>): Flow<NewSongPartialStateChange> =
        flow.map {
            playlistSongsController.nextSong()
        }.newSongResultFromSong()

    private fun onPreviousSong(flow: Flow<PreviousSongIntent>): Flow<NewSongPartialStateChange> =
        flow.map {
            playlistSongsController.previousSong()
        }.newSongResultFromSong()

    private fun onSeekTo(flow: Flow<SeekToIntent>): Flow<SeekToPartialStateChange> =
        flow.map {
            SeekToPartialStateChange(
                mediaPlayerController.seekTo(it.position.roundToInt()).toLong()
            )
        }

    private fun onCurrentPosition(flow: Flow<SongTickerIntent>): Flow<CurrentPositionPartialStateChange> =
        flow.map { CurrentPositionPartialStateChange(it.position) }

    private fun Flow<Song?>.newSongResultFromSong(): Flow<NewSongPartialStateChange> =
        transformLatest { song ->
            emit(
                NewSongPartialStateChange(
                    song,
                    -1,
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
                NewSongPartialStateChange(
                    song = song,
                    duration = duration.toLong(),
                    playing = playing,
                    errorLoading = !loadSuccess
                )
            )
        }

    private fun Long.getTimeLabel(): String {
        val minutes = this / (1000 * 60)
        val seconds = this / 1000 % 60
        return "$minutes:${if (seconds < 10) "0$seconds" else seconds}"
    }

    companion object {
        @VisibleForTesting
        const val SEEK_DURATION = 5000 // ms
    }
}

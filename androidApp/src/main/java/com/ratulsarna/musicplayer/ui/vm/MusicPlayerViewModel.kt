package com.ratulsarna.musicplayer.ui.vm

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ratulsarna.shared.repository.model.Song
import com.ratulsarna.musicplayer.ui.MusicPlayerSideEffect.*
import com.ratulsarna.musicplayer.ui.MusicPlayerIntent.*
import com.ratulsarna.musicplayer.ui.MusicPlayerPartialStateChange.*
import com.ratulsarna.shared.controllers.MediaPlayerController
import com.ratulsarna.shared.controllers.PlaylistSongsController
import com.ratulsarna.musicplayer.ui.MusicPlayerSideEffect
import com.ratulsarna.musicplayer.ui.MusicPlayerIntent
import com.ratulsarna.musicplayer.ui.MusicPlayerPartialStateChange
import com.ratulsarna.musicplayer.ui.MusicPlayerViewState
import com.ratulsarna.musicplayer.ui.model.toPlaylistViewSong
import com.ratulsarna.shared.CoroutineContextProvider
import com.ratulsarna.musicplayer.utils.MINIMUM_DURATION
import com.ratulsarna.musicplayer.utils.interval
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class MusicPlayerViewModel(
    private val playlistSongsController: PlaylistSongsController,
    private val mediaPlayerController: MediaPlayerController,
    private val coroutineContextProvider: CoroutineContextProvider,
) : ViewModel() {

    private var oneSecondIntervalJob: Job? = null
    private val _intentFlow = MutableSharedFlow<MusicPlayerIntent>()
    private val sideEffectChannel = Channel<MusicPlayerSideEffect>(Channel.BUFFERED)

    val viewState: StateFlow<MusicPlayerViewState>
    val sideEffects: Flow<MusicPlayerSideEffect> = sideEffectChannel.receiveAsFlow()

    init {
        val initialVS = MusicPlayerViewState.INITIAL
        viewState = _intentFlow
            .onEach { Timber.d("Event = $it") }
            .intentToPartialStateChange()
            .onEach { Timber.d("Result = $it") }
            .partialStateChangeToSideEffect()
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
                    mediaPlayerController.getCurrentPosition()
                }.onEach {
                    processInput(SongTickerIntent(it.toLong()))
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

    suspend fun processInput(intent: MusicPlayerIntent) {
        _intentFlow.emit(intent)
    }

    private fun Flow<MusicPlayerIntent>.intentToPartialStateChange(): Flow<MusicPlayerPartialStateChange> {
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
            onCurrentPosition(filterIsInstance()),
            onNewSong(filterIsInstance()),
        )
    }

    private fun Flow<MusicPlayerPartialStateChange>.partialChangeToViewState(): Flow<MusicPlayerViewState> {
        return scan(MusicPlayerViewState.INITIAL) { vs, result ->
            when (result) {
                is UiCreatePartialStateChange -> vs.copy(playlist = result.playlist.toImmutableList())
                is UiStartPartialStateChange -> result.song?.let { song ->
                    vs.copy(
                        loading = false,
                        songTitle = song.title,
                        songInfoLabel = "${song.artistName} | ${song.year}",
                        albumArt = song.albumArtResource,
                        totalDuration = result.duration,
                        playing = result.playing ?: vs.playing,
                        currentPlaylistSong = song.toPlaylistViewSong(),
                        elapsedTimeLabel = vs.elapsedTime.getTimeLabel(),
                        totalTimeLabel = result.duration.getTimeLabel(),
                    )
                } ?: vs
                is NewSongPartialStateChange -> {
                    result.song?.let { song ->
                        val duration = if (result.duration == -1L) vs.totalDuration else result.duration
                        vs.copy(
                            loading = false,
                            songTitle = song.title,
                            songInfoLabel = "${song.artistName} | ${song.year}",
                            albumArt = song.albumArtResource,
                            elapsedTime = 0,
                            totalDuration = (if (result.duration == -1L) vs.totalDuration else result.duration),
                            playing = result.playing,
                            currentPlaylistSong = song.toPlaylistViewSong(),
                            elapsedTimeLabel = 0L.getTimeLabel(),
                            totalTimeLabel = duration.getTimeLabel(),
                        )
                    } ?: vs
                }
                is SeekToPartialStateChange -> {
                    vs.copy(
                        elapsedTime = result.position,
                        elapsedTimeLabel = result.position.getTimeLabel()
                    )
                }
                UiStopPartialStateChange -> vs
                is PausePartialStateChange -> vs.copy(playing = result.playing)
                is PlayPartialStateChange -> vs.copy(playing = result.playing)
                is SongTickerPartialStateChange -> vs.copy(
                    elapsedTime = result.position,
                    elapsedTimeLabel = result.position.getTimeLabel()
                )
            }
        }
    }

    private fun Flow<MusicPlayerPartialStateChange>.partialStateChangeToSideEffect(): Flow<MusicPlayerPartialStateChange> {
        return onEach { result ->
            val effect = when {
                (result is UiStartPartialStateChange && result.errorLoadingSong) ||
                        (result is NewSongPartialStateChange && result.errorLoading) -> {
                    ShowErrorSideEffect("Error loading song. Try next song.")
                }
                else -> null
            }
            Timber.d("SideEffect = $effect")
            effect?.let { sideEffectChannel.send(it) }
        }
    }

    private fun onUiCreate(flow: Flow<UiCreateIntent>): Flow<UiCreatePartialStateChange> =
        flow.map {
            UiCreatePartialStateChange(
                playlistSongsController.loadDefaultPlaylistSongs()
                    .map { it.toPlaylistViewSong() }
            )
        }

    private fun onUiStart(flow: Flow<UiStartIntent>): Flow<UiStartPartialStateChange> =
        flow.transformLatest {
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
                    currentViewState.playing && currentViewState.elapsedTime > 0 -> {
                        playing = mediaPlayerController.seekToAndStart(currentViewState.elapsedTime.toInt())
                    }

                    currentViewState.elapsedTime > 0 -> {
                        mediaPlayerController.seekTo(currentViewState.elapsedTime.toInt())
                    }

                    currentViewState.playing -> {
                        playing = mediaPlayerController.start()
                    }
                }
            }
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

    private fun onSeekForward(flow: Flow<SeekForwardIntent>): Flow<SeekToPartialStateChange> =
        flow.map {
            SeekToPartialStateChange(
                mediaPlayerController.seekBy(SEEK_DURATION).toLong()
            )
        }

    private fun onSeekBackward(flow: Flow<SeekBackwardIntent>): Flow<SeekToPartialStateChange> =
        flow.map {
            SeekToPartialStateChange(
                mediaPlayerController.seekBy(-SEEK_DURATION).toLong()
            )
        }

    private fun onSeekTo(flow: Flow<SeekToIntent>): Flow<SeekToPartialStateChange> =
        flow.map {
            SeekToPartialStateChange(
                mediaPlayerController.seekTo(it.position.toInt()).toLong()
            )
        }

    private fun onCurrentPosition(flow: Flow<SongTickerIntent>): Flow<SongTickerPartialStateChange> =
        flow.map { SongTickerPartialStateChange(it.position) }

    private fun onNewSong(flow: Flow<NewSongIntent>): Flow<NewSongPartialStateChange> =
        flow.mapNotNull {
            if (it.songId != viewState.value.currentPlaylistSong?.id) {
                playlistSongsController.newSong(it.songId)
            } else {
                null
            }
        }.newSongResultFromSong()

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
                val loadSuccess = mediaPlayerController.loadNewSong(song?.songFileName)
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

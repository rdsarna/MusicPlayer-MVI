package com.ratulsarna.musicplayer.ui

import android.media.audiofx.Visualizer
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import com.ratulsarna.musicplayer.repository.model.Song
import com.ratulsarna.musicplayer.ui.MusicPlayerEffect.*
import com.ratulsarna.musicplayer.ui.MusicPlayerEvent.*
import com.ratulsarna.musicplayer.ui.MusicPlayerResult.*
import com.ratulsarna.musicplayer.ui.controllers.MediaPlayerController
import com.ratulsarna.musicplayer.ui.controllers.UpNextSongsController
import com.ratulsarna.musicplayer.ui.model.LoadSongResult
import com.ratulsarna.musicplayer.ui.model.PlaylistViewSong
import com.ratulsarna.musicplayer.ui.model.toPlaylistViewSong
import com.ratulsarna.musicplayer.utils.MINIMUM_DURATION
import com.ratulsarna.musicplayer.utils.SchedulerProvider
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MusicPlayerViewModel @Inject constructor(
    private val upNextSongsController: UpNextSongsController,
    private val mediaPlayerController: MediaPlayerController,
    private val schedulerProvider: SchedulerProvider,
) : ViewModel() {

    private val eventEmitter: PublishSubject<MusicPlayerEvent> = PublishSubject.create()

    private lateinit var disposable: Disposable

    private val oneSecondIntervalEmitter = Observable.interval(
        0,1000, TimeUnit.MILLISECONDS, schedulerProvider.computation())
    private var oneSecondIntervalDisposable: Disposable? = null

    private val waveFormEmitter: PublishSubject<ByteArray> = PublishSubject.create()
    private var visualizer: Visualizer? = null
    private val visualizerDataCaptureListener = object : Visualizer.OnDataCaptureListener {
        override fun onWaveFormDataCapture(
            visualizer: Visualizer?,
            waveform: ByteArray?,
            samplingRate: Int
        ) {
            if (waveform != null) waveFormEmitter.onNext(waveform)
        }

        override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
            // ignore
        }
    }

    val viewState: Observable<MusicPlayerViewState>
    val viewEffects: Observable<MusicPlayerEffect>

    init {
        mediaPlayerController.init({
            oneSecondIntervalDisposable = oneSecondIntervalEmitter.map {
                mediaPlayerController.getCurrentPosition()
            }.subscribe {
                processInput(CurrentPositionEvent(it))
            }
        }, {
            oneSecondIntervalDisposable?.dispose()
        }) { processInput(SongCompletedEvent) }
        eventEmitter
            .doOnNext { Timber.d("----- event $it") }
            .eventToResult()
            .doOnNext { Timber.d("----- result $it") }
            .share()
            .also { result ->
                viewState = result
                    .resultToViewState()
                    .doOnNext { Timber.d("----- vs $it") }
                    .replay(1)
                    .autoConnect(1) { disposable = it }

                viewEffects = result
                    .resultToViewEffect()
                    .doOnNext { Timber.d("----- ve $it") }
            }
    }

    override fun onCleared() {
        disposable.dispose()
        oneSecondIntervalDisposable?.dispose()
        super.onCleared()
    }

    fun processInput(event: MusicPlayerEvent) {
        eventEmitter.onNext(event)
    }

    private fun Observable<MusicPlayerEvent>.eventToResult(): Observable<MusicPlayerResult> {
        return publish { o -> Observable.merge(
            listOf(
                o.ofType(UiCreateEvent::class.java).onUiCreate(),
                o.ofType(UiStartEvent::class.java).onUiStart(),
                o.ofType(UiStopEvent::class.java).onUiStop(),
                o.ofType(PlayEvent::class.java).onPlay(),
                o.ofType(PauseEvent::class.java).onPause(),
                o.ofType(NextSongEvent::class.java).onNextSong(),
                o.ofType(PreviousSongEvent::class.java).onPreviousSong(),
                o.ofType(SeekForwardEvent::class.java).onSeekForward(),
                o.ofType(SeekBackwardEvent::class.java).onSeekBackward(),
                o.ofType(SeekToEvent::class.java).onSeekTo(),
                o.ofType(SongCompletedEvent::class.java).onSongCompleted(),
                o.ofType(CurrentPositionEvent::class.java).onCurrentPosition(),
                o.ofType(NewSongEvent::class.java).onNewSong(),
            ))
        }
    }

    private fun Observable<MusicPlayerResult>.resultToViewState(): Observable<MusicPlayerViewState> {
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
                is NewSongResult -> result.song?.let { song ->
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
                is SeekToResult -> vs.copy(elapsedTime = result.position)
                UiStopResult -> vs
                is PauseResult -> vs.copy(playing = result.playing)
                is PlayResult -> vs.copy(playing = result.playing)
                is CurrentPositionResult -> vs.copy(elapsedTime = result.position)
            }
        }
            .distinctUntilChanged()
    }

    private fun Observable<MusicPlayerResult>.resultToViewEffect(): Observable<MusicPlayerEffect> {
        return withLatestFrom(viewState) { result, vs ->
            when (result) {
                is PlayResult -> ForceScreenOnEffect(true)
                is PauseResult -> ForceScreenOnEffect(false)
                is UiStartResult -> when {
                    result.errorLoadingSong -> ShowErrorEffect("Error loading song. Try next song.")
                    else -> NoOpEffect
                }
                is NewSongResult -> when {
                    result.errorLoading -> ShowErrorEffect("Error loading song. Try next song.")
                    vs.playing -> ForceScreenOnEffect(true)
                    else -> NoOpEffect
                }
                else -> NoOpEffect
            }
        }
    }

    private val upNextSongList: List<PlaylistViewSong>
        get() = upNextSongsController.currentUpNextSongList().map { it.toPlaylistViewSong() }

    private fun Observable<UiCreateEvent>.onUiCreate(): Observable<UiCreateResult> =
        map { UiCreateResult(upNextSongsController.loadDefaultPlaylistSongs()
                    .map { it.toPlaylistViewSong() }) }

    private fun Observable<UiStartEvent>.onUiStart(): Observable<UiStartResult> =
        map { upNextSongsController.currentSong() }
            .switchMap {
                Observable.fromCallable { mediaPlayerController.loadNewSong(it) }
                    .subscribeOn(schedulerProvider.io())
                    .map { loadSuccessful ->
                        if (loadSuccessful) {
                            mediaPlayerController.getDuration() to loadSuccessful
                        } else {
                            null to loadSuccessful
                        }
                    }
                    .withLatestFrom(viewState) { (duration, loadSuccessful), vs ->
                        var playing = false
                        when {
                            vs.playing && vs.elapsedTime > 0 -> playing = mediaPlayerController.seekToAndStart(vs.elapsedTime)
                            vs.elapsedTime > 0 -> mediaPlayerController.seekTo(vs.elapsedTime)
                            vs.playing -> playing = mediaPlayerController.start()
                        }
                        LoadSongResult(duration, loadSuccessful, playing)
                    }
                    .map { loadSongResult ->
                        UiStartResult(
                            upNextSongsController.currentSong(),
                            upNextSongsController.peekNextSong(),
                            (loadSongResult.duration ?: 1),
                            playing = loadSongResult.playing,
                            errorLoadingSong = !loadSongResult.loadSuccessful,
                        )
                    }
                    .startWithItem(
                        UiStartResult(
                            upNextSongsController.currentSong(),
                            upNextSongsController.peekNextSong(),
                            1,
                            playing = null,
                            errorLoadingSong = false,
                        )
                    )
            }
private fun Observable<UiStopEvent>.onUiStop(): Observable<UiStopResult> =
    map {
        // Important note: We are pausing here (if player is playing) to stop playback
        // immediately but this pause is not propagated to the Ui because we want to
        // continue playback on the next UiStartEvent
        mediaPlayerController.pause()
        mediaPlayerController.release()
    }
        .doOnNext {
            visualizer?.enabled = false
            visualizer?.release()
            visualizer = null
        }
        .map { UiStopResult }
    private fun Observable<PlayEvent>.onPlay(): Observable<PlayResult> =
        map { mediaPlayerController.start() }
            .map { PlayResult(it) }
    private fun Observable<PauseEvent>.onPause(): Observable<PauseResult> =
        map { mediaPlayerController.pause() }
            .map { PauseResult(!it) }

    private fun Observable<NextSongEvent>.onNextSong(): Observable<NewSongResult> =
        map {
            upNextSongsController.nextSong()
        }.newSongResultFromSong()

    private fun Observable<PreviousSongEvent>.onPreviousSong(): Observable<NewSongResult> =
        map {
            upNextSongsController.previousSong()
        }.newSongResultFromSong()

    private fun Observable<SeekForwardEvent>.onSeekForward(): Observable<SeekToResult> =
        map { mediaPlayerController.seekBy(SEEK_DURATION) }
            .map { SeekToResult(it) }
    private fun Observable<SeekBackwardEvent>.onSeekBackward(): Observable<SeekToResult> =
        map { mediaPlayerController.seekBy(-SEEK_DURATION) }
            .map { SeekToResult(it) }
    private fun Observable<SeekToEvent>.onSeekTo(): Observable<SeekToResult> =
        map { mediaPlayerController.seekTo(it.position) }
            .map { SeekToResult(it) }

    private fun Observable<SongCompletedEvent>.onSongCompleted(): Observable<NewSongResult> =
        map {
            upNextSongsController.nextSong()
        }.newSongResultFromSong()

    private fun Observable<CurrentPositionEvent>.onCurrentPosition(): Observable<CurrentPositionResult> =
        map { CurrentPositionResult(it.position) }

    private fun Observable<NewSongEvent>.onNewSong(): Observable<NewSongResult> =
        map {
            upNextSongsController.newSong(it.songId)
        }.newSongResultFromSong()

    private fun Observable<Song?>.newSongResultFromSong(): Observable<NewSongResult> =
        switchMap { song ->
            Observable.fromCallable {
                mediaPlayerController.loadNewSong(song)
            }
                .subscribeOn(schedulerProvider.io())
                .doOnNext { loadSuccessful ->
                    if (!loadSuccessful) mediaPlayerController.release()
                }
                .map { loadSuccessful ->
                    if (loadSuccessful) {
                        mediaPlayerController.getDuration() to loadSuccessful
                    } else {
                        null to loadSuccessful
                    }
                }
                .map { (duration, loadSuccessful) ->
                    val playing = mediaPlayerController.start()
                    LoadSongResult(duration, loadSuccessful, playing)
                }
                .map { loadSongResult ->
                    NewSongResult(
                        song,
                        upNextSongsController.peekNextSong(),
                        loadSongResult?.duration ?: MINIMUM_DURATION,
                        upNextSongList,
                        playing = loadSongResult.playing,
                        errorLoading = !loadSongResult.loadSuccessful
                    )
                }
                .startWithItem(
                    NewSongResult(
                        song,
                        upNextSongsController.peekNextSong(),
                        -1,
                        upNextSongList,
                        playing = false,
                        errorLoading = false,
                    )
                )
        }

    companion object {
        @VisibleForTesting const val SEEK_DURATION = 5000 // ms
    }
}

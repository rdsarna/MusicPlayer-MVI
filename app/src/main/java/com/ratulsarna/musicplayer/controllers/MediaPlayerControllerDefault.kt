package com.ratulsarna.musicplayer.controllers

import android.content.Context
import android.media.MediaPlayer
import com.ratulsarna.musicplayer.repository.model.Song
import com.ratulsarna.musicplayer.utils.MINIMUM_DURATION
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class MediaPlayerControllerDefault(
    private val context: Context
) : MediaPlayerController {

    private var mediaPlayer: MediaPlayer? = null
    private var pausedStoppedListener: (() -> Unit)? = null
    private var startedListener: (() -> Unit)? = null
    private var songCompletedListener: (() -> Unit)? = null

    override fun init(
        startedListener: () -> Unit,
        pausedStoppedListener: () -> Unit,
        songCompletedListener: () -> Unit
    ) {
        this.startedListener = startedListener
        this.pausedStoppedListener = pausedStoppedListener
        this.songCompletedListener = songCompletedListener
    }

    override fun loadNewSong(song: Song?): Boolean {
        if (song == null) return false
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer().apply {
                setOnCompletionListener {
                    // The following check is to avoid some weird kind of bug with MediaPlayer
                    // where rapidly resetting and loading new songs sometimes triggers onCompletionListener
                    if (it.currentPosition.approxEquals(it.duration)) {
                        songCompletedListener?.invoke()
                    }
                }
            }
        }
        pausedStoppedListener?.invoke()
        return try {
            val assetFileDescriptor = context.resources.openRawResourceFd(song.rawResourceId)
                ?: throw RuntimeException("ErrorLoadingSong")
            mediaPlayer?.run {
                reset()
                setDataSource(
                    assetFileDescriptor.fileDescriptor, assetFileDescriptor.startOffset,
                    assetFileDescriptor.declaredLength
                )
                prepare()
            }
            true
        } catch (ex: Exception) {
            Timber.e(ex,  "Exception while loading song, %s", song)
            false
        }
    }

    override fun start(): Boolean {
        if (mediaPlayer == null) return false
        return tryAndCatch("Error while starting song", false) {
            mediaPlayer?.start()
            startedListener?.invoke()
            true
        }
    }

    override fun stop(): Boolean {
        if (mediaPlayer == null) return false
        return tryAndCatch("Error while stopping song", false) {
            mediaPlayer?.stop()
            pausedStoppedListener?.invoke()
            true
        }
    }

    override fun pause(): Boolean {
        if (mediaPlayer == null) return false
        return tryAndCatch("Error while pausing song", false) {
            mediaPlayer?.pause()
            pausedStoppedListener?.invoke()
            true
        }
    }

    override fun getDuration(): Int = mediaPlayer?.duration ?: MINIMUM_DURATION

    override fun seekTo(position: Int): Int {
        if (mediaPlayer == null) return 0
        return tryAndCatch("Error while seeking by delta", getCurrentPosition()) {
            if (position in 0..getDuration()) {
                position
            } else {
                0
            }.apply {
                mediaPlayer?.seekTo(this)
            }
        }
    }

    override fun seekBy(delta: Int): Int =
        tryAndCatch("Error while seeking by delta", getCurrentPosition()) {
            if (delta < 0) {
                seekTo(max(getCurrentPosition() + delta, 0))
            } else {
                seekTo(min(getCurrentPosition() + delta, getDuration()))
            }
        }

    override fun seekToAndStart(position: Int): Boolean =
        tryAndCatch("Error while seeking and playing song", false) {
            seekTo(position)
            start()
        }

    override fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0

    override fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    override fun release() {
        pausedStoppedListener?.invoke()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun <T> tryAndCatch(errorLog: String, errorReturn: T, block: () -> T): T =
        try {
            block()
        } catch (ex: java.lang.IllegalStateException) {
            Timber.e(ex, errorLog)
            errorReturn
        }

    private fun Int.approxEquals(duration: Int): Boolean =
        this in (.95 * duration).roundToInt()..(1.05 * duration).roundToInt()
}

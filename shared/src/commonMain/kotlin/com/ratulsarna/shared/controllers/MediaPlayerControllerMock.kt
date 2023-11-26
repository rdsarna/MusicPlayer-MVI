package com.ratulsarna.shared.controllers


import com.ratulsarna.shared.BundledSongFileName
import kotlin.math.max
import kotlin.math.min

class MediaPlayerControllerMock : MediaPlayerController {

    var commands = mutableListOf<MediaPlayerCommand>()
    var _currentPosition = 0
    var _loadNewSongResultsInError = false

    override fun init(
        startedListener: () -> Unit,
        pausedStoppedListener: () -> Unit,
        songCompletedListener: () -> Unit
    ) {
        commands.add(MediaPlayerCommand.Init)
    }

    override fun loadNewSong(bundledSongFileName: BundledSongFileName?): Boolean {
        commands.add(MediaPlayerCommand.LoadSong)
        return !_loadNewSongResultsInError
    }

    override fun start(): Boolean {
        commands.add(MediaPlayerCommand.Start)
        return true
    }

    override fun stop(): Boolean {
        commands.add(MediaPlayerCommand.Stop)
        return true
    }

    override fun pause(): Boolean {
        commands.add(MediaPlayerCommand.Pause)
        return true
    }

    override fun getDuration(): Int {
        return TEST_SONG_DURATION
    }

    override fun seekTo(position: Int): Int {
        commands.add(MediaPlayerCommand.SeekTo)
        return if (position in 0..getDuration()) {
            position
        } else {
            0
        }.apply {
            _currentPosition = position
        }
    }

    override fun seekBy(delta: Int): Int {
        commands.add(MediaPlayerCommand.SeekBy)
        return if (delta < 0) {
            seekTo(max(getCurrentPosition() + delta, 0))
        } else {
            seekTo(min(getCurrentPosition() + delta, getDuration()))
        }
    }

    override fun seekToAndStart(position: Int): Boolean {
        commands.add(MediaPlayerCommand.SeekToAndStart)
        seekTo(position)
        return start()
    }

    override fun getCurrentPosition(): Int {
        return _currentPosition
    }

    override fun isPlaying(): Boolean {
        return true
    }

    override fun release() {
        commands.add(MediaPlayerCommand.Release)
    }

    companion object {
        const val TEST_SONG_DURATION = 60 * 1000 // 1minute in millis
    }
}

enum class MediaPlayerCommand {
    Init,
    LoadSong,
    Start,
    Pause,
    Stop,
    SeekToAndStart,
    SeekTo,
    SeekBy,
    Release,
}
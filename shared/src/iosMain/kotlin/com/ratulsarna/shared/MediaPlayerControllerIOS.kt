package com.ratulsarna.shared

import com.ratulsarna.shared.controllers.MediaPlayerController

class MediaPlayerControllerIOS : MediaPlayerController {
    override fun init(
        startedListener: () -> Unit,
        pausedStoppedListener: () -> Unit,
        songCompletedListener: () -> Unit
    ) {

    }

    override fun loadNewSong(songFileName: BundledSongFileName?): Boolean {
        return true
    }

    override fun start(): Boolean {
        return true
    }

    override fun stop(): Boolean {
        return true
    }

    override fun pause(): Boolean {
        return true
    }

    override fun getDuration(): Int {
        return 0
    }

    override fun seekTo(position: Int): Int {
        return 0
    }

    override fun seekBy(delta: Int): Int {
        return 0
    }

    override fun seekToAndStart(position: Int): Boolean {
        return true
    }

    override fun getCurrentPosition(): Int {
        return 0
    }

    override fun isPlaying(): Boolean {
        return true
    }

    override fun release() {

    }

}
package com.ratulsarna.musicplayer.controllers

import android.media.MediaPlayer
import com.ratulsarna.shared.repository.model.Song
import com.ratulsarna.shared.BundledSongFileName

interface MediaPlayerController {

    fun init(
        startedListener: () -> Unit,
        pausedStoppedListener: () -> Unit,
        songCompletedListener: () -> Unit
    )
    /**
     * Synchronously loads song into [MediaPlayer] and returns true if successful loading else false
     */
    fun loadNewSong(songFileName: BundledSongFileName?): Boolean

    fun start(): Boolean
    fun stop(): Boolean
    fun pause(): Boolean
    fun getDuration(): Int

    /**
     * Returns the new currentPosition of MediaPlayer. The new position will be 0 if [position]
     * is out of bounds
     */
    fun seekTo(position: Int): Int

    /**
     * Returns the new currentPosition of MediaPlayer
     */
    fun seekBy(delta: Int): Int
    fun seekToAndStart(position: Int): Boolean
    fun getCurrentPosition(): Int
    fun isPlaying(): Boolean
    fun release()
}
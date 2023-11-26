package com.ratulsarna.musicplayer

import com.ratulsarna.shared.controllers.MediaPlayerControllerMock
import com.ratulsarna.shared.controllers.MediaPlayerControllerMock.Companion.TEST_SONG_DURATION
import org.junit.Test
import kotlin.test.assertEquals

class MediaPlayerControllerTest {

    // Note: test song duration is TEST_SONG_DURATION = 1 minute (60*1000ms)

    @Test
    fun `seekTo() test, position is within bounds`() {
        MediaPlayerControllerMock().apply {
            assertEquals(20000, seekTo(20000))
            assertEquals(0, seekTo(0))
            assertEquals(TEST_SONG_DURATION, seekTo(TEST_SONG_DURATION))
        }
    }

    @Test
    fun `seekTo() test, position is outside bounds, should return 0`() {
        MediaPlayerControllerMock().apply {
            assertEquals(0, seekTo(70000))
            assertEquals(0, seekTo(-1))
        }
    }

    @Test
    fun `seekBy() test, resulting position is within bounds`() {
        MediaPlayerControllerMock().apply {
            _currentPosition = 10000
            assertEquals(20000, seekBy(10000))
            assertEquals(15000, seekBy(-5000))
        }
    }

    @Test
    fun `seekBy() test, resulting position is outside bounds`() {
        MediaPlayerControllerMock().apply {
            _currentPosition = 10000
            assertEquals(TEST_SONG_DURATION, seekBy(TEST_SONG_DURATION))
            assertEquals(0, seekBy(-(TEST_SONG_DURATION+10)))
        }
    }
}
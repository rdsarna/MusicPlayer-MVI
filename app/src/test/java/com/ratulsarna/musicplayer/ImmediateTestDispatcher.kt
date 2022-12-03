package com.ratulsarna.musicplayer

import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.CoroutineContext

/**
 * This is a workaround for an open bug in kotlin test library -
 * https://github.com/Kotlin/kotlinx.coroutines/issues/3120
 */
class ImmediateTestDispatcher : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        block.run()
    }
}
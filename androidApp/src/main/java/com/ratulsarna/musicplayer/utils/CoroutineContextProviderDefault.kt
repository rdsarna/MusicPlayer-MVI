package com.ratulsarna.musicplayer.utils

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

open class CoroutineContextProviderDefault : CoroutineContextProvider {
    override val main: CoroutineContext by lazy { Dispatchers.Main }
    override val io: CoroutineContext by lazy { Dispatchers.IO }
}

interface CoroutineContextProvider {
    val main: CoroutineContext
    val io: CoroutineContext
}
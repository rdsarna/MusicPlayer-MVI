package com.ratulsarna.musicplayer.utils

import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
open class CoroutineContextProviderDefault @Inject constructor() : CoroutineContextProvider {
    override val main: CoroutineContext by lazy { Dispatchers.Main }
    override val io: CoroutineContext by lazy { Dispatchers.IO }
}

interface CoroutineContextProvider {
    val main: CoroutineContext
    val io: CoroutineContext
}
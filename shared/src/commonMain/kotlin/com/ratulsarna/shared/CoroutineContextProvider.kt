package com.ratulsarna.shared

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlin.coroutines.CoroutineContext

open class CoroutineContextProviderDefault : CoroutineContextProvider {
    override val main: CoroutineContext by lazy { Dispatchers.Main }
    override val io: CoroutineContext by lazy { Dispatchers.IO }
}

interface CoroutineContextProvider {
    val main: CoroutineContext
    val io: CoroutineContext
}
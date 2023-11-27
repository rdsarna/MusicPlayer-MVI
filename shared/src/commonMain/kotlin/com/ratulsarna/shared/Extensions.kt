package com.ratulsarna.shared

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

fun interval(timeInMillis: Long): Flow<Long> = flow {
    var counter: Long = 0

    while (currentCoroutineContext().isActive) {
        delay(timeInMillis)
        emit(counter++)
    }
}

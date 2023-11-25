package com.ratulsarna.musicplayer.utils

import android.os.Looper
import android.view.View
import androidx.annotation.CheckResult
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.util.concurrent.TimeUnit

fun interval(timeInMillis: Long, timeUnit: TimeUnit): Flow<Long> = flow {
    var counter: Long = 0
    val delayTime = when (timeUnit) {
        TimeUnit.MICROSECONDS -> timeInMillis / 1000
        TimeUnit.NANOSECONDS -> timeInMillis / 1_000_000
        TimeUnit.SECONDS -> timeInMillis * 1000
        TimeUnit.MINUTES -> 60 * timeInMillis * 1000
        TimeUnit.HOURS -> 60 * 60 * timeInMillis * 1000
        TimeUnit.DAYS -> 24 * 60 * 60 * timeInMillis * 1000
        else -> timeInMillis
    }

    while (currentCoroutineContext().isActive) {
        delay(delayTime)
        emit(counter++)
    }
}

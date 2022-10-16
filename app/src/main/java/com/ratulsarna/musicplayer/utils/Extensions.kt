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

/**
 * For Actvities, allows declarations like
 * ```
 * val myViewModel = viewModelProvider(myViewModelFactory)
 * ```
 */
inline fun <reified VM : ViewModel> FragmentActivity.viewModelProvider(
    provider: ViewModelProvider.Factory
) = ViewModelProvider(this, provider).get(VM::class.java)

/** Returns true if the calling thread is the main thread.  */
fun isMainThread(): Boolean {
    return Looper.getMainLooper().thread === Thread.currentThread()
}

fun <T> MutableLiveData<T>.setValueIfNew(newValue: T?) {
    if (this.value != newValue) value = newValue
}

fun <T> MutableLiveData<T>.postValueIfNew(newValue: T) {
    if (this.value != newValue) postValue(newValue)
}

/**
 * Handles thread safety. Calls [setValueIfNew] when on the main
 * thread and [postValueIfNew] when not.
 */
fun <T> MutableLiveData<T>.updateValueIfNew(newValue: T) {
    if (isMainThread()) {
        setValueIfNew(newValue)
    } else {
        postValueIfNew(newValue)
    }
}

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

internal fun checkMainThread() {
    check(Looper.myLooper() == Looper.getMainLooper()) {
        "Expected to be called on the main thread but was " + Thread.currentThread().name
    }
}

@CheckResult
fun View.clicks(): Flow<View> {
    return callbackFlow {
        checkMainThread()

        setOnClickListener { trySend(it) }
        awaitClose { setOnClickListener(null) }
    }
}

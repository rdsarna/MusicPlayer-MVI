package com.ratulsarna.musicplayer.utils

import android.os.Looper
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

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
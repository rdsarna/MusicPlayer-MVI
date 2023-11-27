package com.ratulsarna.shared.di

import com.ratulsarna.shared.MediaPlayerControllerAndroid
import com.ratulsarna.shared.controllers.MediaPlayerController
import org.koin.dsl.module

actual fun platformModule() = module {
    factory<MediaPlayerController> { MediaPlayerControllerAndroid(get()) }
}
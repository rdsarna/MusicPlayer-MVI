package com.ratulsarna.musicplayer.di

import com.ratulsarna.musicplayer.controllers.MediaPlayerController
import com.ratulsarna.musicplayer.controllers.MediaPlayerControllerDefault
import com.ratulsarna.musicplayer.controllers.PlaylistSongsController
import com.ratulsarna.musicplayer.controllers.PlaylistSongsControllerDefault
import com.ratulsarna.musicplayer.ui.vm.MusicPlayerViewModel
import com.ratulsarna.musicplayer.utils.CoroutineContextProvider
import com.ratulsarna.musicplayer.utils.CoroutineContextProviderDefault
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val androidAppModule = module {
    factory<MediaPlayerController> { MediaPlayerControllerDefault(get()) }
    single<PlaylistSongsController> { PlaylistSongsControllerDefault(get()) }
    single<CoroutineContextProvider> { CoroutineContextProviderDefault() }

    viewModel {
        MusicPlayerViewModel(
            playlistSongsController = get(),
            mediaPlayerController = get(),
            coroutineContextProvider = get()
        )
    }
}
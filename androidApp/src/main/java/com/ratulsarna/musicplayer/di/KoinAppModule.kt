package com.ratulsarna.musicplayer.di

import com.ratulsarna.musicplayer.ui.vm.MusicPlayerViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val androidAppModule = module {
    viewModel {
        MusicPlayerViewModel(
            playlistSongsController = get(),
            mediaPlayerController = get(),
            coroutineContextProvider = get()
        )
    }
}
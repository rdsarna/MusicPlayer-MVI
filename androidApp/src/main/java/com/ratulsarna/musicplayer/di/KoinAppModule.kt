package com.ratulsarna.musicplayer.di

import com.ratulsarna.shared.vm.MusicPlayerViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val androidAppModule = module {
    viewModel {
        MusicPlayerViewModel()
    }
}
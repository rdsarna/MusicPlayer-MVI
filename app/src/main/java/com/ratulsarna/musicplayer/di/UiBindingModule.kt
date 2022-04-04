package com.ratulsarna.musicplayer.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ratulsarna.musicplayer.ui.test.MainActivity
import com.ratulsarna.musicplayer.ui.MusicPlayerActivity
import com.ratulsarna.musicplayer.ui.MusicPlayerViewModel
import com.ratulsarna.musicplayer.ui.test.StatefulActionViewModel
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoMap

@Module
abstract class UiBindingModule {

    @Binds
    internal abstract fun bindViewModelFactory(factory: AppViewModelFactory): ViewModelProvider.Factory

    @ActivityScoped
    @ContributesAndroidInjector
    abstract fun musicPlayerActivity(): MusicPlayerActivity

    @Binds
    @IntoMap
    @ViewModelKey(MusicPlayerViewModel::class)
    abstract fun musicPlayerViewModel(viewModel: MusicPlayerViewModel): ViewModel

    @ActivityScoped
    @ContributesAndroidInjector
    abstract fun testActivity(): MainActivity

    @Binds
    @IntoMap
    @ViewModelKey(StatefulActionViewModel::class)
    abstract fun testViewModel(viewModel: StatefulActionViewModel): ViewModel
}
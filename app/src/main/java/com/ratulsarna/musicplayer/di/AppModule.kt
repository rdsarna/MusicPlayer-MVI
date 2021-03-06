package com.ratulsarna.musicplayer.di

import android.content.Context
import com.ratulsarna.musicplayer.MainApplication
import com.ratulsarna.musicplayer.repository.PlaylistsRepository
import com.ratulsarna.musicplayer.repository.PlaylistsRepositoryDefault
import com.ratulsarna.musicplayer.repository.SongsRepository
import com.ratulsarna.musicplayer.repository.SongsRepositoryDefault
import com.ratulsarna.musicplayer.ui.controllers.MediaPlayerController
import com.ratulsarna.musicplayer.ui.controllers.MediaPlayerControllerDefault
import com.ratulsarna.musicplayer.utils.SchedulerProvider
import com.ratulsarna.musicplayer.utils.SchedulerProviderDefault
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule {

    @Provides
    fun provideContext(application: MainApplication): Context = application.applicationContext

    @Singleton
    @Provides
    fun provideSongsRepository(impl: SongsRepositoryDefault): SongsRepository = impl

    @Singleton
    @Provides
    fun providePlaylistsRepository(impl: PlaylistsRepositoryDefault): PlaylistsRepository = impl

    @Provides
    fun provideMediaPlayerController(impl: MediaPlayerControllerDefault): MediaPlayerController = impl

    @Singleton
    @Provides
    fun provideSchedulerProvider(impl: SchedulerProviderDefault): SchedulerProvider = impl
}
package com.ratulsarna.shared.di

import com.ratulsarna.shared.CoroutineContextProvider
import com.ratulsarna.shared.CoroutineContextProviderDefault
import com.ratulsarna.shared.controllers.PlaylistSongsController
import com.ratulsarna.shared.controllers.PlaylistSongsControllerDefault
import com.ratulsarna.shared.repository.PlaylistsRepository
import com.ratulsarna.shared.repository.PlaylistsRepositoryDefault
import com.ratulsarna.shared.repository.SongsRepository
import com.ratulsarna.shared.repository.SongsRepositoryDefault
import org.koin.core.module.Module
import org.koin.dsl.module

fun commonModule() = module {
    single<SongsRepository> { SongsRepositoryDefault() }
    single<PlaylistsRepository> { PlaylistsRepositoryDefault(get()) }
    single<PlaylistSongsController> { PlaylistSongsControllerDefault(get()) }
    single<CoroutineContextProvider> { CoroutineContextProviderDefault() }
}

expect fun platformModule(): Module

fun sharedModules() = commonModule() + platformModule()
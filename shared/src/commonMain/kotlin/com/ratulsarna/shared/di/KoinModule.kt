package com.ratulsarna.shared.di

import com.ratulsarna.shared.repository.PlaylistsRepository
import com.ratulsarna.shared.repository.PlaylistsRepositoryDefault
import com.ratulsarna.shared.repository.SongsRepository
import com.ratulsarna.shared.repository.SongsRepositoryDefault
import org.koin.dsl.module

fun commonModule() = module {
    single<SongsRepository> { SongsRepositoryDefault() }
    single<PlaylistsRepository> { PlaylistsRepositoryDefault(get()) }
}
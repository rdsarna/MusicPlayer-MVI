package com.ratulsarna.musicplayer.di

import com.ratulsarna.musicplayer.MainApplication
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

/**
 * Main component of the app, created and persisted in the Application class.
 *
 * Whenever a new module is created, it should be added to the list of modules.
 * [AndroidSupportInjectionModule] is the module from Dagger.Android that helps with the
 * generation and location of subcomponents.
 */
@Singleton
@Component(modules = [
    AppModule::class,
    AndroidSupportInjectionModule::class,
    UiBindingModule::class,
])
interface AppComponent : AndroidInjector<MainApplication> {
    @Component.Factory
    abstract class Builder : AndroidInjector.Factory<MainApplication>
}

package com.ratulsarna.musicplayer

import com.ratulsarna.musicplayer.di.AppComponent
import com.ratulsarna.musicplayer.di.DaggerAppComponent
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import dagger.android.HasAndroidInjector
import timber.log.Timber

class MainApplication : DaggerApplication(), HasAndroidInjector {

    private var appComponent: AppComponent? = null

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return getComponent()
    }

    private fun createComponent() = DaggerAppComponent.factory().create(this) as AppComponent

    private fun getComponent(): AppComponent {
        return appComponent ?: createComponent().also { appComponent = it }
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
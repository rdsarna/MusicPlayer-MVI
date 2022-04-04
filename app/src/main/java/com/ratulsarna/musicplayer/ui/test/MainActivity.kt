package com.ratulsarna.musicplayer.ui.test

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ratulsarna.musicplayer.R
import com.ratulsarna.musicplayer.databinding.ActivityMusicPlayerBinding
import com.ratulsarna.musicplayer.ui.MusicPlayerViewModel
import com.ratulsarna.musicplayer.utils.viewModelProvider
import dagger.android.support.DaggerAppCompatActivity
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class MainActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory


    private val viewModel: StatefulActionViewModel by lazy {
        viewModelProvider(viewModelFactory)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch {
            viewModel.state.collect {
                Timber.d("SPECIAL-- $it")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.send(TestEvent.TestEvent1)
        viewModel.send(TestEvent.TestEvent2)
    }
}
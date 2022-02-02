package com.ratulsarna.musicplayer.ui

import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.slider.Slider
import com.jakewharton.rxbinding4.view.clicks
import com.ratulsarna.musicplayer.databinding.ActivityMusicPlayerBinding
import com.ratulsarna.musicplayer.ui.MusicPlayerEffect.*
import com.ratulsarna.musicplayer.ui.MusicPlayerEvent.*
import com.ratulsarna.musicplayer.ui.model.PlaylistViewSong
import com.ratulsarna.musicplayer.utils.updateValueIfNew
import com.ratulsarna.musicplayer.utils.viewModelProvider
import dagger.android.support.DaggerAppCompatActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.roundToInt


class MusicPlayerActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: ActivityMusicPlayerBinding

    private var uiDisposable: Disposable? = null
    private var disposables: CompositeDisposable = CompositeDisposable()

    private val viewModel: MusicPlayerViewModel by lazy {
        viewModelProvider(viewModelFactory)
    }

    private val _loading = MutableLiveData<Boolean>()
    private val _playing = MutableLiveData<Boolean>()
    private val _songTitle = MutableLiveData<String>()
    private val _songInfoLabel = MutableLiveData<String>()
    private val _albumArt = MutableLiveData<Int>()
    private val _totalDuration = MutableLiveData<Float>()
    private val _elapsedTime = MutableLiveData<Int>()
    private val _totalDurationLabel = MutableLiveData<String>()
    private val _elapsedTimeLabel = MutableLiveData<String>()
    private val _nextSongLabel = MutableLiveData<String>()
    private val _upNextSongsList = MutableLiveData<List<PlaylistViewSong>>()
    private val viewBindingState = MusicPlayerViewBindingState(
        _loading, _playing, _songTitle, _songInfoLabel, _albumArt, _totalDuration, _elapsedTime,
        _totalDurationLabel, _elapsedTimeLabel, _nextSongLabel, _upNextSongsList,
    )

    private val nextSongIdClick: PublishSubject<Int> = PublishSubject.create()

    private var basePeekHeight: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMusicPlayerBinding.inflate(layoutInflater).apply {
            lifecycleOwner = this@MusicPlayerActivity
            viewState = viewBindingState
        }
        setContentView(binding.root)

        binding.apply {
            setPlaylistViewsAlpha(0f)

            // setup bottom sheet animations and listeners
            val bottomSheetBehavior = BottomSheetBehavior.from(playlistView.root)
            bottomSheetBehavior.apply {
                basePeekHeight = peekHeight
                addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {}
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        playlistView.closePlaylistButton.visibility =
                            if (slideOffset == 0f) View.GONE else View.VISIBLE
                        val offsetInverse = 1 - slideOffset
                        setPlayerViewsAlphaAndScale(offsetInverse, 1 - (slideOffset * .5f))
                        setPlaylistViewsAlpha(slideOffset)
                        playlistView.peekView.alpha = offsetInverse
                        bottomControlsGuideline.setGuidelinePercent(.725f * offsetInverse)
                    }
                })
                playlistView.peekView.setOnClickListener {
                    if (state == STATE_COLLAPSED) {
                        state = STATE_EXPANDED
                    }
                }
                playlistView.closePlaylistButton.setOnClickListener {
                    state = STATE_COLLAPSED
                }
            }
            playlistView.root.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                height = (screenHeight() * .77).toInt()
            }

            setupPlaylist(playlistView.playlistRecyclerView)

            // Setup insets to avoid issues when switching between gesture and button navigation
            window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                val relevantInsets = screenInsets(insets)
                parentView.updatePadding(top = relevantInsets.top, bottom = relevantInsets.bottom)
                playlistView.playlistParent.updatePadding(bottom = relevantInsets.bottom)
                bottomSheetBehavior.peekHeight = basePeekHeight + relevantInsets.bottom
                view.onApplyWindowInsets(insets)
            }
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
                WindowInsetsCompat.Builder()
                    .setSystemWindowInsets(insets.systemWindowInsets)
                    .setSystemGestureInsets(insets.systemGestureInsets).build()
            }
        }

        disposables.add(
            viewModel
                .viewState
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { Timber.d("----- onNext VS $it") }
                .subscribe(::render) {
                    Timber.w(it, "something went terribly wrong processing view state")
                }
        )

        disposables.add(
            viewModel
                .viewEffects
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::trigger) {
                    Timber.w(it, "something went terribly wrong processing view effects")
                }
        )

        viewModel.processInput(UiCreateEvent)
    }

    private fun setupPlaylist(playlistRecyclerView: RecyclerView) {
        playlistRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MusicPlayerActivity,
                LinearLayoutManager.VERTICAL, false)
            adapter = PlaylistAdapter { nextSongIdClick.onNext(it) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    override fun onStart() {
        super.onStart()

        binding.apply {
            val uiStartEvents = Observable.just(UiStartEvent)
            val playEvents = playButton.clicks().map { PlayEvent }
            val pauseEvents = pauseButton.clicks().map { PauseEvent }
            val seekForwardEvents = seekForwardButton.clicks().map { SeekForwardEvent }
            val seekBackwardEvents = seekBackwardButton.clicks().map { SeekBackwardEvent }
            val nextSongEvents = nextButton.clicks().map { NextSongEvent }
            val previousSongEvents = previousButton.clicks().map { PreviousSongEvent }
            val newSongEvents: Observable<NewSongEvent> = nextSongIdClick.map { NewSongEvent(it) }

            uiDisposable =
                Observable.merge(listOf(
                    uiStartEvents,
                    playEvents,
                    pauseEvents,
                    seekForwardEvents,
                    seekBackwardEvents,
                    nextSongEvents,
                    previousSongEvents,
                    newSongEvents,
                )).subscribe(
                    { viewModel.processInput(it) },
                    { Timber.e(it, "error processing input ") }
                )

            seekBar.addOnChangeListener(Slider.OnChangeListener { _, value, fromUser ->
                if (fromUser) viewModel.processInput(SeekToEvent(value.roundToInt()))
            })
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.processInput(UiStopEvent)
        uiDisposable?.dispose()
    }

    override fun onBackPressed() {
        BottomSheetBehavior.from(binding.playlistView.root).apply {
            if (state == STATE_EXPANDED) {
                state = STATE_COLLAPSED
                return
            }
        }
        super.onBackPressed()
    }

    private fun trigger(effect: MusicPlayerEffect) {
        when (effect) {
            is ForceScreenOnEffect -> {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                if (effect.on) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            is ShowErrorEffect -> Toast.makeText(this, effect.errorMessage, Toast.LENGTH_LONG).show()
            NoOpEffect -> { /* no-op */ }
        }
    }

    private fun render(viewState: MusicPlayerViewState) {
        viewState.apply {
            _loading.updateValueIfNew(loading)
            _playing.updateValueIfNew(playing)
            _songTitle.updateValueIfNew(songTitle)
            _songInfoLabel.updateValueIfNew(songInfoLabel)
            _albumArt.updateValueIfNew(albumArt)
            _totalDuration.updateValueIfNew(totalDuration)
            _elapsedTime.updateValueIfNew(min(elapsedTime, totalDuration.toInt()))
            _totalDurationLabel.updateValueIfNew(totalDuration.toInt().getTimeLabel())
            _elapsedTimeLabel.updateValueIfNew(elapsedTime.getTimeLabel())
            _nextSongLabel.updateValueIfNew(nextSongLabel)
            _upNextSongsList.updateValueIfNew(upNextSongs)
        }
    }

    private val playerViews: List<View> by lazy {
        with(binding) {
            listOf(
                albumArtCard,
                songTitle,
                songInfo
            )
        }
    }
    private val playlistViews: List<View> by lazy {
        with(binding.playlistView) {
            listOf(
                upNextLabel,
                closePlaylistButton,
                playlistRecyclerView,
                nowPlayingLabel,
                nowPlayingAlbumArt,
                nowPlayingSongTitle,
                nowPlayingInfoLabel
            )
        }
    }
    private fun setPlayerViewsAlphaAndScale(alpha: Float, scale: Float) {
        playerViews.forEach {
            it.alpha = alpha
            it.scaleX = scale
            it.scaleY = scale
        }

    }
    private fun setPlaylistViewsAlpha(alpha: Float) {
        playlistViews.forEach { it.alpha = alpha }
    }

    @Suppress("DEPRECATION")
    private fun screenHeight(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.height()
        } else {
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            displayMetrics.heightPixels
        }
    @Suppress("DEPRECATION")
    private fun screenInsets(preRInsets: WindowInsets): Rect =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.windowInsets
                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()).let {
                    Rect(it.left, it.top, it.right, it.bottom)
                }
        } else {
            preRInsets.let {
                Rect(it.systemWindowInsetLeft, it.systemWindowInsetTop,
                    it.systemWindowInsetRight, it.systemWindowInsetBottom)
            }
        }

    private fun Int.getTimeLabel(): String {
        val minutes = this / (1000 * 60)
        val seconds = this / 1000 % 60
        return "$minutes:${if (seconds < 10) "0$seconds" else seconds}"
    }
}

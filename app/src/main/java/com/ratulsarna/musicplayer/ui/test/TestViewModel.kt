package com.ratulsarna.musicplayer.ui.test

import androidx.lifecycle.viewModelScope
import com.ratulsarna.musicplayer.ui.ChaloBaseMviViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import javax.inject.Inject

class TestViewModel @Inject constructor() : ChaloBaseMviViewModel<TestIntent, TestState, TestSideEffect, TestPartialChange>() {

    override fun initialViewState(): TestState {
        return TestState(0)
    }

    override fun changeToSideEffect(change: TestPartialChange): List<TestSideEffect> {
        return listOf()
    }

    override fun Flow<TestIntent>.toPartialChangeFlow(): Flow<TestPartialChange> {
        return merge(
            filterIsInstance<TestIntent.UiCreateIntent>().map {
                viewModelScope.launch {
                    while (true) {
                        processIntent(TestIntent.CountIntent)
                        delay(1000)
                    }
                }
                TestPartialChange.UiCreatePartialChange
            },
            filterIsInstance<TestIntent.UiStartIntent>().map { TestPartialChange.UiStartPartialChange },
            filterIsInstance<TestIntent.UiStopIntent>().map { TestPartialChange.UiStopPartialChange },
            filterIsInstance<TestIntent.CountIntent>().map { TestPartialChange.CountPartialChange }
        )
    }
}
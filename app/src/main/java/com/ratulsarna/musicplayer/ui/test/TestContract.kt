package com.ratulsarna.musicplayer.ui.test

import app.zophop.mvibase.ChaloBasePartialChange

data class TestState(
    val count: Int
)

sealed class TestIntent {
    object UiCreateIntent : TestIntent()
    object UiStartIntent : TestIntent()
    object UiStopIntent : TestIntent()
    object CountIntent : TestIntent()
}

object TestSideEffect

sealed class TestPartialChange: ChaloBasePartialChange<TestState> {
    object UiCreatePartialChange : TestPartialChange()
    object UiStartPartialChange : TestPartialChange()
    object UiStopPartialChange : TestPartialChange()
    object CountPartialChange : TestPartialChange() {
        override fun reduce(oldState: TestState): TestState {
            return oldState.copy(count = oldState.count + 1)
        }
    }
}
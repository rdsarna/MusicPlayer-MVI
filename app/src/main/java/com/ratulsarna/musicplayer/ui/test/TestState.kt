package com.ratulsarna.musicplayer.ui.test

data class TestState(
    val test: String = ""
)

sealed class TestEvent {
    object TestEvent1 : TestEvent()
    object TestEvent2 : TestEvent()
    object TestEvent3 : TestEvent()
}

sealed class TestEffect {
    object TestEffect1 : TestEffect()
}

data class TestResult(
    val test: String = ""
)

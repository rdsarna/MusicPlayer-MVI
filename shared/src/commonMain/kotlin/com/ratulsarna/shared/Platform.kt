package com.ratulsarna.shared

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
package com.art.ffmpeg.core

fun interface FFmpegCallback {
    fun onComplete(state: State)
}

sealed class State {
    object Success : State()
    object Cancel : State()
    object Fail : State()
}
package com.art.ffmpeg.widget

interface VideoCropViewListener {
    fun onLoadThumbList(totalThumbsCount: Int, srcVideoPath: String, startSec: Int, endSec: Int)
}
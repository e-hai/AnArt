package com.art.ffmpeg.extensions.crop

interface VideoCropViewListener {
    fun onLoadThumbList(totalThumbsCount: Int, startSec: Long, endSec: Long)
}
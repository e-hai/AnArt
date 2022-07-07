package com.an.ffmpeg.code

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class VideoCropViewModel(app: Application) : AndroidViewModel(app) {

    val thumbList = MutableStateFlow<List<VideoThumbItem>>(emptyList())
    val cropStatus = MutableStateFlow<VideoCropStatus>(VideoCropStatus.Idle)

    fun startCrop(
        srcVideoPath: String,
        startSec: Int,
        endSec: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val outputName = "videoCrop_$timeStamp.mp4"
            val destPath = File(getApplication<Application>().filesDir, outputName).apply { createNewFile() }.absolutePath
            val duration = endSec - startSec

            VideoCrop.cropVideo(getApplication(), srcVideoPath, destPath, startSec, duration)
                .onEach {
                    cropStatus.value = it
                }
                .catch { e -> e.printStackTrace() }
                .collect()
        }
    }

    fun getVideoThumbList(
        totalThumbsCount: Int,
        srcVideoPath: String,
        startSec: Int,
        endSec: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            flow<List<VideoThumbItem>> {
                val interval = (endSec - startSec) / (totalThumbsCount - 1)
                val frameList = mutableListOf<VideoThumbItem>()
                for (i in 0 until totalThumbsCount) {
                    var frameTime: Long = if (0 == interval) {
                        startSec.toLong() + i
                    } else {
                        startSec.toLong() + interval * i
                    }
                    frameTime *= 1000 * 1000
                    frameList.add(VideoThumbItem(srcVideoPath, frameTime))
                }
                emit(frameList)
            }
                .catch { e ->
                    e.printStackTrace()
                }
                .collectLatest {
                    thumbList.value = it
                }
        }
    }
}

data class VideoThumbItem(
    val srcVideoPath: String,
    val frameTimeMicros: Long
)
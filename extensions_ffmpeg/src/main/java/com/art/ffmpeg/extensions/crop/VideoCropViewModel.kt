package com.art.ffmpeg.extensions.crop

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.an.file.FileManager
import com.art.ffmpeg.core.FFmpegManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class VideoCropViewModel(app: Application) : AndroidViewModel(app) {

    val thumbsUpdate = MutableStateFlow<List<VideoThumbItem>>(emptyList())
    val cropStatus = MutableStateFlow<VideoCropStatus>(VideoCropStatus.Idle)
    val videoUpdate = MutableStateFlow<Uri>(Uri.EMPTY)

    private lateinit var inputVideoUri: Uri

    fun resetVideo(videoUri: Uri) {
        Log.d(TAG, videoUri.path ?: "")
        inputVideoUri = videoUri
        viewModelScope.launch {
            videoUpdate.emit(videoUri)
        }
    }

    fun cropVideo(
        startSec: Int,
        endSec: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val outputVideoName = "videoCrop_$timeStamp.mp4"
            val outputVideoUri =
                FileManager.sharedStorage(getApplication()).createMovie(outputVideoName)
            val duration = endSec - startSec

            FFmpegManager.cropVideo(
                getApplication(),
                inputVideoUri,
                outputVideoUri,
                startSec,
                duration
            ) {

            }
        }
    }

    fun getVideoThumbList(
        totalThumbsCount: Int,
        startSec: Long,
        endSec: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            flow<List<VideoThumbItem>> {
                val interval = (endSec - startSec) / (totalThumbsCount - 1)
                val frameList = mutableListOf<VideoThumbItem>()
                for (i in 0 until totalThumbsCount) {
                    var frameTime: Long = if (0L == interval) {
                        startSec + i
                    } else {
                        startSec + interval * i
                    }
                    frameTime *= 1000 * 1000
                    frameList.add(VideoThumbItem(inputVideoUri, frameTime))
                }
                emit(frameList)
            }
                .catch { e ->
                    e.printStackTrace()
                }
                .collectLatest {
                    thumbsUpdate.value = it
                }
        }
    }

    companion object {
        const val TAG = "VideoCropViewModel"
    }
}

data class VideoThumbItem(
    val inputVideoUri: Uri,
    val frameTimeMicros: Long
)
package com.art.ffmpeg.extensions.crop

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.an.file.FileManager
import com.art.ffmpeg.core.FFmpegManager
import com.art.ffmpeg.core.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


sealed class VideoCropStatus {
    object Idle : VideoCropStatus()
    data class Success(val videoUri: Uri) : VideoCropStatus()
    data class Fail(val msg: String) : VideoCropStatus()
    data class Loading(val progress: Double) : VideoCropStatus()
}

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
                viewModelScope.launch {
                    when (it) {
                        is State.Success -> cropStatus.emit(VideoCropStatus.Success(outputVideoUri))
                        is State.Cancel -> cropStatus.emit(VideoCropStatus.Fail("User cancel"))
                        is State.Fail -> cropStatus.emit(VideoCropStatus.Fail("ffmpeg run fail"))
                        is State.Load -> {
                            val progress = it.time.toDouble() / duration.toDouble()
                            cropStatus.emit(VideoCropStatus.Loading(progress))
                        }
                    }
                }
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
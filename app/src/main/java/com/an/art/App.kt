package com.an.art

import android.app.Application
import android.util.Log
import com.an.art.demo_ffmpeg.FFmpegActivity.Companion.initViewCrop
import com.an.art.demo_opencv.DemoOpencvActivity.Companion.initOpencv
import com.an.ffmpeg.code.VideoCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        application = this
//        initViewCrop(this)
        initOpencv()
    }


    companion object {
        lateinit var application: Application
    }
}
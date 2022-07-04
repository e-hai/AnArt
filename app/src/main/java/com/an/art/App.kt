package com.an.art

import android.app.Application
import android.content.Context
import android.util.Log
import com.an.ffmpeg.widget.Utils
import nl.bravobit.ffmpeg.FFmpeg

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        application = this
        Utils.init(this)
        initFFmpegBinary(this)
    }

    private fun initFFmpegBinary(context: Context) {
        if (!FFmpeg.getInstance(context).isSupported) {
            Log.e("owwo", "Android cup arch not supported!")
        }
    }
    companion object {
        lateinit var application: Application
    }
}
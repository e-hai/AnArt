package com.an.art

import android.app.Application
import android.content.Context
import android.util.Log
import com.an.ffmpeg.code.VideoFFCrop
import com.an.ffmpeg.widget.Utils

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        application = this
        Utils.init(this)
        VideoFFCrop.instance?.init(this)
    }


    companion object {
        lateinit var application: Application
    }
}
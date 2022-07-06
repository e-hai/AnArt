package com.an.art

import android.app.Application
import com.an.ffmpeg.code.VideoCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        application = this
        initViewCrop(this)
    }

    private fun initViewCrop(application: Application) {
        MainScope().launch(Dispatchers.IO) {
            VideoCrop.init(application)
        }
    }


    companion object {
        lateinit var application: Application
    }
}
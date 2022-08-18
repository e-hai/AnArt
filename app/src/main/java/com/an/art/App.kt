package com.an.art

import android.app.Application
import com.an.art.opencv.OpencvActivity.Companion.initOpencv

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        application = this
        initOpencv()
    }


    companion object {
        lateinit var application: Application
    }
}
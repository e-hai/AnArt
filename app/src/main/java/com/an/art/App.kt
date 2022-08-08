package com.an.art

import android.app.Application
import com.an.art.demo_opencv.DemoOpencvActivity.Companion.initOpencv

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
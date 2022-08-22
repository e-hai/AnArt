package com.an.art.opencv

import android.app.Application
import com.an.art.opencv.OpencvActivity.Companion.initOpencv

class OpencvApp : Application() {

    override fun onCreate() {
        super.onCreate()
        application = this
        initOpencv()
    }


    companion object {
        lateinit var application: Application
    }
}
package com.an.app.opengl

import android.app.Application

class OpenglApp : Application() {

    override fun onCreate() {
        super.onCreate()
        application = this
    }


    companion object {
        lateinit var application: Application
    }
}
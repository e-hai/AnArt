package com.an.art.opencv

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.an.art.R
import org.opencv.android.OpenCVLoader

class OpencvActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_opencv)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, OpencvFragment.newInstance())
                .commitNow()
        }
    }

    companion object{

         fun initOpencv() {
            val loadSuccess: Boolean = OpenCVLoader.initDebug()

            if (loadSuccess) {
                Log.e("DemoOpencvActivity", "Opencv load Success")
            } else {
                Log.e("DemoOpencvActivity", "Opencv load Fail")
            }
        }

    }
}
package com.an.art.demo_opencv

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.an.art.R
import org.opencv.android.OpenCVLoader

class DemoOpencvActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo_opencv)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, DemoOpencvFragment.newInstance())
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
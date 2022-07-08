package com.an.art

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.an.art.databinding.ActivityMainBinding
import com.an.art.demo_opencv.DemoOpencvActivity

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        binding.opencvBtn.setOnClickListener {
            gotoOpencv()
        }
    }

    private fun gotoOpencv() {
        startActivity(Intent(this, DemoOpencvActivity::class.java))
    }
}
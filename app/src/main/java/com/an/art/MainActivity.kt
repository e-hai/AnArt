package com.an.art

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.an.art.databinding.ActivityMainBinding
import com.an.art.opencv.OpencvActivity
import com.an.art.ffmpeg.FFmpegActivity

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
        binding.openglBtn.setOnClickListener {
            gotoOpengl()
        }
        binding.ffmpegBtn.setOnClickListener {
            gotoFFmpeg()
        }
    }

    private fun gotoFFmpeg() {
        startActivity(Intent(this, FFmpegActivity::class.java))
    }

    private fun gotoOpengl() {
        startActivity(Intent(this, OpencvActivity::class.java))
    }

    private fun gotoOpencv() {
        startActivity(Intent(this, OpencvActivity::class.java))
    }
}
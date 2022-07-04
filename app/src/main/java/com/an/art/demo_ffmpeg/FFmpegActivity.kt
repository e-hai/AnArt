package com.an.art.demo_ffmpeg

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.an.art.databinding.ActivityFfmpegBinding

class FFmpegActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFfmpegBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFfmpegBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    companion object {
         const val VIDEO_URI_KEY = "VIDEO_URI_KEY"
        fun call(from: FragmentActivity, videoUri: Uri) {
            val bundle = Bundle()
            bundle.putParcelable(VIDEO_URI_KEY, videoUri)
            val intent = Intent(from, FFmpegActivity::class.java)
            intent.putExtras(bundle)
            from.startActivity(intent)
        }
    }
}
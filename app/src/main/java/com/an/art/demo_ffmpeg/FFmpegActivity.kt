package com.an.art.demo_ffmpeg

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.an.art.databinding.ActivityFfmpegBinding
import com.art.ffmpeg.code.VideoCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

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


         fun initViewCrop(application: Application) {
            MainScope().launch(Dispatchers.IO) {
                VideoCrop.init(application)
            }
        }
    }
}
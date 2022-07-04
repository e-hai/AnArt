package com.an.art.demo_ffmpeg

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import androidx.fragment.app.FragmentActivity
import com.an.art.App
import com.an.art.databinding.ActivityFfmpegBinding
import com.an.file.FileManager
import java.io.File

class FFmpegActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFfmpegBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFfmpegBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    companion object {
        const val VIDEO_URI_KEY = "VIDEO_URI_KEY"
        const val VIDEO_FILE_KEY = "VIDEO_FILE_KEY"

        fun call(from: FragmentActivity, videoUri: File) {
            val outfile = FileManager.specificStorage(App.application).createMovie("testff").toFile()
            val bundle = Bundle()
            bundle.putSerializable(VIDEO_URI_KEY, videoUri)
            bundle.putSerializable(VIDEO_FILE_KEY, outfile)
            val intent = Intent(from, FFmpegActivity::class.java)
            intent.putExtras(bundle)
            from.startActivity(intent)
        }
    }
}
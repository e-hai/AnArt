package com.an.art.demo_ffmpeg

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.an.art.databinding.FragmentFfmpegBinding
import com.an.art.demo_ffmpeg.FFmpegActivity.Companion.VIDEO_FILE_KEY
import com.an.art.demo_ffmpeg.FFmpegActivity.Companion.VIDEO_URI_KEY
import com.an.ffmpeg.widget.VideoTrimListener
import java.io.File

class FFmpegFragment : Fragment() {
    private lateinit var binding: FragmentFfmpegBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFfmpegBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setVideUri(
            activity?.intent?.getSerializableExtra(VIDEO_URI_KEY) as File,
            activity?.intent?.getSerializableExtra(VIDEO_FILE_KEY) as File
        )

        binding.trimmerView.setOnTrimVideoListener(object : VideoTrimListener {
            override fun onStartTrim() {
                Log.d("owow", "onStartTrim")

            }

            override fun onFinishTrim(url: String?) {
                Log.d("owow", "onFinishTrim=$url")

            }


            override fun onCancel() {
                Log.d("owow", "onCancel")
            }
        })
    }

    private fun setVideUri(inFile: File, outFile: File) {
        Log.d("owow", "inFile=${inFile.absolutePath}  outFile=${outFile.absolutePath}")
        binding.trimmerView.initVideoByURI(inFile, outFile)
    }
}
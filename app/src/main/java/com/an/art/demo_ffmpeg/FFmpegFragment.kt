package com.an.art.demo_ffmpeg

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toFile
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.an.art.App
import com.an.art.databinding.FragmentFfmpegBinding
import com.an.art.demo_ffmpeg.FFmpegActivity.Companion.VIDEO_URI_KEY
import com.art.ffmpeg.code.VideoCropViewModel
import com.art.ffmpeg.widget.VideoCropViewListener
import com.an.file.FileManager
import kotlinx.coroutines.flow.collectLatest
import java.io.File

class FFmpegFragment : Fragment() {
    private lateinit var binding: FragmentFfmpegBinding
    private val viewModel: VideoCropViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFfmpegBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.intent?.getParcelableExtra<Uri>(VIDEO_URI_KEY)?.let {
            val srcVideo = FileManager.specificStorage(App.application)
                .saveMovie("srcVideo", activity?.contentResolver?.openInputStream(it) ?: return)
                .toFile()
            setVideUri(srcVideo)
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.cropStatus.collect {
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.thumbList.collectLatest {
                binding.trimmerView.updateThumbs(it)
            }
        }
    }

    private fun setVideUri(inFile: File) {
        Log.d(TAG, "inFile=${inFile.absolutePath}")

        binding.trimmerView.initVideoByUri(inFile, object : VideoCropViewListener {
            override fun onLoadThumbList(
                totalThumbsCount: Int,
                srcVideoPath: String,
                startSec: Int,
                endSec: Int
            ) {
                viewModel.getVideoThumbList(totalThumbsCount, srcVideoPath, startSec, endSec)
            }
        })
        binding.finishBtn.setOnClickListener {
            onCropClicked()
        }
    }


    private fun onCropClicked() {
        val srcVideoPath =binding.trimmerView.srcVideo.absolutePath

        binding.trimmerView.onPause()
        viewModel.startCrop(
            srcVideoPath,
            binding.trimmerView.getCropStartTimeSec(),
            binding.trimmerView.getCropEndTimeSec()
        )
    }

    companion object {
        const val TAG = "FFmpegFragment"
    }
}
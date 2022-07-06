package com.an.art.demo_ffmpeg

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.an.art.databinding.FragmentFfmpegBinding
import com.an.art.demo_ffmpeg.FFmpegActivity.Companion.VIDEO_URI_KEY
import com.an.ffmpeg.code.VideoCropViewModel
import com.an.ffmpeg.widget.VideoCropViewListener
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
        setVideUri(
            activity?.intent?.getSerializableExtra(VIDEO_URI_KEY) as File,
        )
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

            override fun onClickCrop(srcVideoPath: String, startSec: Int, endSec: Int) {
                viewModel.startCrop(srcVideoPath, startSec, endSec)
            }

            override fun onClickCancel() {
                activity?.finish()
            }
        })
    }

    companion object {
        const val TAG = "FFmpegFragment"
    }
}
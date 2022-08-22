package com.an.ffmpeg

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.an.app.ffmpeg.databinding.FragmentFfmpegCropBinding
import com.art.ffmpeg.extensions.crop.VideoCropViewModel
import com.art.ffmpeg.extensions.crop.VideoCropViewListener
import kotlinx.coroutines.flow.collectLatest

class FFmpegFragment : Fragment() {
    private lateinit var binding: FragmentFfmpegCropBinding
    private val viewModel: VideoCropViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFfmpegCropBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gotoAlbumSelectVideo()
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.cropStatus.collect {
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.thumbsUpdate.collectLatest {
                binding.trimmerView.updateThumbs(it)
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.videoUpdate.collectLatest {
                updateVideoView(it)
            }
        }
    }

    private fun gotoAlbumSelectVideo() {
        PermissionsFragment.load(this)
            .requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                if (it) gotoAlbum()
            }
    }

    private fun gotoAlbum() {
        val local = Intent()
        local.type = "video/*;image/*"
        local.action = Intent.ACTION_OPEN_DOCUMENT
        startActivityForResult(local, REQ_CODE_ALBUM)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CODE_ALBUM && resultCode == RESULT_OK) {
            val videoUri = data?.data ?: return
            viewModel.resetVideo(videoUri)
        }
    }

    private fun updateVideoView(videoUri: Uri) {
        Log.d(TAG, "${videoUri.path.isNullOrEmpty()}")
        if (videoUri.path.isNullOrEmpty()) return
        binding.trimmerView.initVideoView(videoUri, object : VideoCropViewListener {
            override fun onLoadThumbList(
                totalThumbsCount: Int,
                startSec: Long,
                endSec: Long
            ) {
                loadVideoThumbs(totalThumbsCount, startSec, endSec)
            }
        })
        binding.finishBtn.setOnClickListener {
            onClickCrop()
        }
    }

    private fun loadVideoThumbs(totalThumbsCount: Int, startSec: Long, endSec: Long) {
        viewModel.getVideoThumbList(totalThumbsCount, startSec, endSec)
    }

    private fun onClickCrop() {
        binding.trimmerView.onPause()
        viewModel.cropVideo(
            binding.trimmerView.getCropStartTimeSec(),
            binding.trimmerView.getCropEndTimeSec()
        )
    }


    companion object {
        const val TAG = "FFmpegFragment"
        const val REQ_CODE_ALBUM = 1
    }
}
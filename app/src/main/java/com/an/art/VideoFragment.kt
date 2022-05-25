package com.an.art

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.an.art.databinding.FragmentVideoBinding
import com.an.gl.util.FileUtil
import com.an.gl.usercase.video.VideoAddWatermarkManager
import java.io.File

class VideoFragment : Fragment() {

    private lateinit var binding: FragmentVideoBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentVideoBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Thread {
            val context = context ?: return@Thread
            val fromFile: File = FileUtil.createFileByAssets(context, "test.mp4", "123.mp4")
            val outFile: File = FileUtil.createFile(context, "456.mp4")
            VideoAddWatermarkManager(context, fromFile, outFile).start()
        }.start()
    }
}
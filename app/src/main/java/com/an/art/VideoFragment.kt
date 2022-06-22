package com.an.art

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toFile
import com.an.art.databinding.FragmentVideoBinding
import com.an.file.FileManager
import com.an.gl.usercase.WatermarkConfig
import com.an.gl.util.FileUtil
import com.an.gl.usercase.video.VideoAddWatermarkManager
import java.io.File
import java.lang.Exception

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
            try {
                val context = context ?: return@Thread
                val fromFile: File = FileUtil.createFileByAssets(context, "test.mp4", "123.mp4")
                val outFile: File =
                    FileManager.specificStorage(App.application).createMovie("temp.mp4").toFile()
                Log.d("videoFragment", outFile.absolutePath)
                VideoAddWatermarkManager(
                    context,
                    fromFile,
                    outFile,
                    WatermarkConfig(R.drawable.watermark)
                ).start()
                FileManager.sharedStorage(App.application).saveMovie("666", outFile.inputStream())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
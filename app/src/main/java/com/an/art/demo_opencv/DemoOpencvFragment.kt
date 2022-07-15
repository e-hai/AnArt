package com.an.art.demo_opencv

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import com.an.art.databinding.FragmentDemoOpencvBinding
import kotlinx.coroutines.flow.collectLatest
import org.opencv.android.Utils
import org.opencv.core.*


class DemoOpencvFragment : Fragment() {

    companion object {
        const val TAG = "DemoOpencvFragment"
        fun newInstance() = DemoOpencvFragment()
        const val REQ_A = 1
        const val REQ_B = 2
    }


    private lateinit var binding: FragmentDemoOpencvBinding
    private val faceDetectViewModel: FaceDetectViewModel by viewModels()
    private lateinit var albumRouter: ActivityResultLauncher<Int>
    private var selectFrom = 0


    override fun onAttach(context: Context) {
        super.onAttach(context)
        albumRouter = registerForActivityResult(AlbumRouter()) {
            it?.let {
                if (selectFrom == REQ_A) {
                    binding.aFaceView.load(it)
                } else if (selectFrom == REQ_B) {
                    binding.bFaceView.load(it)
                }
                faceDetectViewModel.detectorByML(it)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDemoOpencvBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.aFaceView.setOnClickListener {
            albumRouter.launch(REQ_A)
        }
        binding.bFaceView.setOnClickListener {
            albumRouter.launch(REQ_B)
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            faceDetectViewModel.drawFacePoint.collectLatest {
                when (selectFrom) {
                    REQ_A -> binding.aFaceView.updateFacePoints(it.points)
                    REQ_B -> binding.bFaceView.updateFacePoints(it.points)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            faceDetectViewModel.transformResult.collectLatest {
                it.bitmap?.let { bitmap ->
                    binding.resultFaceView.setImageBitmap(bitmap)
                }
            }
        }
    }


    inner class AlbumRouter : ActivityResultContract<Int, Uri?>() {

        override fun createIntent(context: Context, input: Int): Intent {
            selectFrom = input
            return Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return intent?.data
        }
    }
}
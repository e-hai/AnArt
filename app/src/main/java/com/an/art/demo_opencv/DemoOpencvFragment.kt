package com.an.art.demo_opencv

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.viewModels
import coil.load
import com.an.art.FaceDetectViewModel
import com.an.art.databinding.FragmentDemoOpencvBinding

class DemoOpencvFragment : Fragment() {

    companion object {
        fun newInstance() = DemoOpencvFragment()
        const val SELECT_FROM_A = 1
        const val SELECT_FROM_B = 2
    }


    private lateinit var binding: FragmentDemoOpencvBinding
    private val faceDetectViewModel: FaceDetectViewModel by viewModels()
    private val opencvViewModel: DemoOpencvViewModel by viewModels()
    private lateinit var albumRouter: ActivityResultLauncher<Int>
    private var selectFrom = SELECT_FROM_A


    override fun onAttach(context: Context) {
        super.onAttach(context)
        albumRouter = registerForActivityResult(AlbumRouter()) {
            it?.let {
                if (selectFrom == SELECT_FROM_A) {
                    binding.aFaceView.load(it)
                } else if (selectFrom == SELECT_FROM_B) {
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
            albumRouter.launch(SELECT_FROM_A)
        }
        binding.bFaceView.setOnClickListener {
            albumRouter.launch(SELECT_FROM_B)
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
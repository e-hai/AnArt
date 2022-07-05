package com.an.art

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toFile
import com.an.art.databinding.ActivityMainBinding
import com.an.art.demo_ffmpeg.FFmpegActivity
import com.an.file.FileManager

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
        const val REQ_CODE = 1;
    }

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        PermissionsFragment.load(this)
            .requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                object : PermissionListener {
                    override fun invoke(isGranted: Boolean) {
                        if (isGranted) {
                            gotoAlbum()
                        }
                    }
                })
    }

    private fun gotoAlbum() {
        val local = Intent()
        local.type = "video/*;image/*"
        local.action = Intent.ACTION_OPEN_DOCUMENT
        startActivityForResult(local, REQ_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CODE && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            val inFIle = FileManager.specificStorage(App.application)
                .saveMovie("inputFile", contentResolver.openInputStream(uri) ?: return)
                .toFile()
            FFmpegActivity.call(this, inFIle)
            finish()
        }
    }

}
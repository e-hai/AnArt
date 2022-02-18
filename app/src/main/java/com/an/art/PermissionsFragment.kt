package com.an.art

import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

typealias PermissionListener = (isGranted: Boolean) -> Unit

class PermissionsFragment : Fragment() {


    fun requestPermissions(permissions: Array<String>, listener: PermissionListener) {
        val context = context ?: return
        permissions
            .filter {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }.also {
                if (it.size == permissions.size) {
                    listener(true)
                } else {
                    requestPermissionLauncher(
                        permissions, listener
                    )
                }
            }
    }

    private fun requestPermissionLauncher(
        permissions: Array<String>,
        listener: PermissionListener
    ) {
        val resultLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map ->
                map.forEach {
                    if (it.value == false) {
                        listener(false)
                        return@registerForActivityResult
                    }
                }
                listener(true)
            }

        resultLauncher.launch(
            permissions
        )
    }

    companion object {

        private const val TAG = "PermissionsFragment"

        fun load(activity: FragmentActivity): PermissionsFragment {
            return load(activity.supportFragmentManager)
        }

        fun load(fragment: Fragment): PermissionsFragment {
            return load(fragment.childFragmentManager)
        }

        private fun load(fragmentManager: FragmentManager): PermissionsFragment {
            var fragment = fragmentManager.findFragmentByTag(TAG)
            if (null == fragment) {
                fragment = PermissionsFragment().also {
                    fragmentManager
                        .beginTransaction()
                        .add(it, TAG)
                        .commitNow()
                }
            }
            return fragment as PermissionsFragment
        }
    }
}
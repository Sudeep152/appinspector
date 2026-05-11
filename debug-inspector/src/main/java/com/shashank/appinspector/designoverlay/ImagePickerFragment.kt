package com.shashank.appinspector.designoverlay

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference

internal class ImagePickerFragment : Fragment() {

    private lateinit var launcher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        launcher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            try {
                pendingCallback?.get()?.invoke(uri)
            } catch (_: Exception) { }
            pendingCallback = null
            try {
                if (isAdded && !parentFragmentManager.isStateSaved) {
                    parentFragmentManager.beginTransaction()
                        .remove(this)
                        .commitAllowingStateLoss()
                }
            } catch (_: Exception) { }
        }

        launcher.launch("image/*")
    }

    override fun onDestroy() {
        super.onDestroy()
        pendingCallback = null
    }

    companion object {
        private const val TAG = "DebugInspectorImagePicker"
        private var pendingCallback: WeakReference<((Uri?) -> Unit)>? = null

        fun launch(activity: FragmentActivity, onResult: (Uri?) -> Unit) {
            pendingCallback = WeakReference(onResult)
            try {
                val fragment = ImagePickerFragment()
                activity.supportFragmentManager
                    .beginTransaction()
                    .add(fragment, TAG)
                    .commitAllowingStateLoss()
            } catch (_: Exception) {
                pendingCallback = null
            }
        }
    }
}

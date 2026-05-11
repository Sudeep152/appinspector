package com.shashank.appinspector.designoverlay

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.shashank.appinspector.DebugConfig
import com.shashank.appinspector.R
import com.shashank.appinspector.databinding.LayoutDesignOverlayPickerBinding
import java.io.InputStream
import java.lang.ref.WeakReference
import java.net.URL
import kotlin.concurrent.thread

internal class DesignOverlayManager(
    private val activity: Activity,
    private val config: DebugConfig
) {

    private var overlayView: DesignOverlayView? = null
    private var controlBar: DesignOverlayControlBar? = null
    private var pickerDialog: BottomSheetDialog? = null
    private var onOverlayShown: (() -> Unit)? = null

    fun showPicker(onShown: () -> Unit) {
        this.onOverlayShown = onShown
        if (activity.isDestroyed || activity.isFinishing) return

        pickerDialog = BottomSheetDialog(activity)
        val binding = LayoutDesignOverlayPickerBinding.inflate(activity.layoutInflater)

        binding.diOverlayPickGallery.setOnClickListener {
            pickerDialog?.dismiss()
            launchGalleryPicker()
        }

        binding.diOverlayBtnLoad.setOnClickListener {
            val url = binding.diOverlayUrlInput.text.toString().trim()
            if (url.isEmpty()) return@setOnClickListener

            binding.diOverlayLoading.visibility = View.VISIBLE
            val activityRef = WeakReference(activity)

            thread {
                val bitmap = loadBitmapFromUrl(url)
                activityRef.get()?.runOnUiThread {
                    binding.diOverlayLoading.visibility = View.GONE
                    if (bitmap != null) {
                        pickerDialog?.dismiss()
                        showOverlay(bitmap)
                    } else {
                        showError()
                    }
                }
            }
        }

        pickerDialog?.setContentView(binding.root)
        pickerDialog?.show()
    }

    private fun launchGalleryPicker() {
        val fragmentActivity = activity as? FragmentActivity
        if (fragmentActivity == null) {
            showError()
            return
        }

        ImagePickerFragment.launch(fragmentActivity) { uri ->
            if (uri != null) {
                try {
                    val inputStream = activity.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (bitmap != null) {
                        showOverlay(bitmap)
                    } else {
                        showError()
                    }
                } catch (_: Exception) {
                    showError()
                }
            }
        }
    }

    private fun showOverlay(bitmap: Bitmap) {
        dismiss()
        if (activity.isDestroyed || activity.isFinishing) {
            bitmap.recycle()
            return
        }

        val decorView = activity.window.decorView as? FrameLayout ?: return

        val overlay = DesignOverlayView(activity, bitmap)
        decorView.addView(overlay, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        overlayView = overlay

        controlBar = DesignOverlayControlBar(
            activity = activity,
            onOpacityChanged = { alpha -> overlay.setOverlayAlpha(alpha) },
            onModeChanged = { mode ->
                overlay.compareMode = mode
            },
            onGridToggle = { overlay.isGridEnabled = !overlay.isGridEnabled },
            onClose = { dismiss() }
        )
        controlBar?.attach()

        onOverlayShown?.invoke()
        onOverlayShown = null
    }

    fun dismiss() {
        controlBar?.detach()
        controlBar = null

        overlayView?.let { view ->
            view.recycleBitmap()
            (view.parent as? ViewGroup)?.removeView(view)
        }
        overlayView = null

        pickerDialog?.dismiss()
        pickerDialog = null

        config.isDesignOverlayActive = false
    }

    private fun loadBitmapFromUrl(url: String): Bitmap? {
        return try {
            val connection = URL(url).openConnection()
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            val inputStream: InputStream = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun showError() {
        if (!activity.isDestroyed && !activity.isFinishing) {
            Toast.makeText(activity, R.string.debug_inspector_overlay_error, Toast.LENGTH_SHORT).show()
        }
    }
}

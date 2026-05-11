package com.shashank.appinspector.editor

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.shashank.appinspector.R
import com.shashank.appinspector.utils.ResourceUtils
import java.lang.ref.WeakReference

internal class PropertyEditorSheet(private val activity: Activity) {

    private val density = activity.resources.displayMetrics.density
    private val debounceHandler = Handler(Looper.getMainLooper())

    fun showTextEditor(view: TextView, onApplied: () -> Unit) {
        val dialog = buildDialog()
        val root = LayoutInflater.from(activity).inflate(R.layout.layout_editor_text, null)

        root.findViewById<TextView>(R.id.di_editor_title).text =
            activity.getString(R.string.debug_inspector_edit_text_title)

        val editText = root.findViewById<EditText>(R.id.di_editor_input)
        val charCountView = root.findViewById<TextView>(R.id.di_editor_char_count)

        editText.setText(view.text)
        editText.setSelection(editText.text.length)
        updateCharCount(charCountView, view.text.length)

        editText.addTextChangedListener(object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                updateCharCount(charCountView, s?.length ?: 0)
            }
        })

        root.findViewById<View>(R.id.di_editor_btn_apply).setOnClickListener {
            view.text = editText.text
            view.requestLayout()
            dialog.dismiss()
            onApplied()
        }

        root.findViewById<View>(R.id.di_editor_btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        showDialog(dialog, root)
        editText.post { editText.requestFocus() }
    }

    fun showPaddingEditor(view: View, onApplied: () -> Unit) {
        val dialog = buildDialog()
        val root = LayoutInflater.from(activity).inflate(R.layout.layout_editor_padding, null)

        root.findViewById<TextView>(R.id.di_editor_title).text =
            activity.getString(R.string.debug_inspector_edit_padding_title)

        val inputStart = root.findViewById<EditText>(R.id.di_padding_start)
        val inputTop = root.findViewById<EditText>(R.id.di_padding_top)
        val inputEnd = root.findViewById<EditText>(R.id.di_padding_end)
        val inputBottom = root.findViewById<EditText>(R.id.di_padding_bottom)
        val previewHint = root.findViewById<View>(R.id.di_padding_preview_hint)

        inputStart.setText(ResourceUtils.pxToDp(view.paddingStart, density).toString())
        inputTop.setText(ResourceUtils.pxToDp(view.paddingTop, density).toString())
        inputEnd.setText(ResourceUtils.pxToDp(view.paddingEnd, density).toString())
        inputBottom.setText(ResourceUtils.pxToDp(view.paddingBottom, density).toString())

        val origStart = view.paddingStart
        val origTop = view.paddingTop
        val origEnd = view.paddingEnd
        val origBottom = view.paddingBottom

        val viewRef = WeakReference(view)

        val livePreviewWatcher = object : SimpleTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                debounceHandler.removeCallbacksAndMessages(DEBOUNCE_TOKEN)
                debounceHandler.postAtTime({
                    val v = viewRef.get() ?: return@postAtTime
                    previewHint.visibility = View.VISIBLE
                    applyPaddingToView(v, inputStart, inputTop, inputEnd, inputBottom)
                }, DEBOUNCE_TOKEN, android.os.SystemClock.uptimeMillis() + DEBOUNCE_MS)
            }
        }

        listOf(inputStart, inputTop, inputEnd, inputBottom).forEach {
            it.addTextChangedListener(livePreviewWatcher)
        }

        root.findViewById<View>(R.id.di_editor_btn_apply).setOnClickListener {
            debounceHandler.removeCallbacksAndMessages(DEBOUNCE_TOKEN)
            val v = viewRef.get() ?: return@setOnClickListener
            applyPaddingToView(v, inputStart, inputTop, inputEnd, inputBottom)
            dialog.dismiss()
            onApplied()
        }

        root.findViewById<View>(R.id.di_editor_btn_cancel).setOnClickListener {
            debounceHandler.removeCallbacksAndMessages(DEBOUNCE_TOKEN)
            viewRef.get()?.apply {
                setPaddingRelative(origStart, origTop, origEnd, origBottom)
                requestLayout()
            }
            dialog.dismiss()
        }

        showDialog(dialog, root)
        inputStart.post { inputStart.requestFocus() }
    }

    private fun buildDialog(): BottomSheetDialog {
        val dialog = BottomSheetDialog(activity)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        return dialog
    }

    private fun showDialog(dialog: BottomSheetDialog, root: View) {
        try {
            if (activity.isDestroyed || activity.isFinishing) return
            dialog.setContentView(root)
            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            dialog.behavior.skipCollapsed = true
            dialog.behavior.peekHeight = (activity.resources.displayMetrics.heightPixels * 0.6).toInt()
            if (!activity.isDestroyed && !activity.isFinishing) {
                dialog.show()
            }
        } catch (_: Exception) { }
    }

    private fun applyPaddingToView(view: View, start: EditText, top: EditText, end: EditText, bottom: EditText) {
        view.setPaddingRelative(
            parseDp(start, view.paddingStart),
            parseDp(top, view.paddingTop),
            parseDp(end, view.paddingEnd),
            parseDp(bottom, view.paddingBottom)
        )
        view.requestLayout()
    }

    private fun parseDp(input: EditText, fallbackPx: Int): Int {
        return try {
            ResourceUtils.dpToPx(input.text.toString().trim().toInt(), density)
        } catch (_: NumberFormatException) {
            fallbackPx
        }
    }

    private fun updateCharCount(view: TextView, count: Int) {
        view.text = activity.getString(R.string.debug_inspector_char_count, count)
    }

    private abstract class SimpleTextWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
    }

    companion object {
        private const val DEBOUNCE_MS = 300L
        private val DEBOUNCE_TOKEN = Any()
    }
}

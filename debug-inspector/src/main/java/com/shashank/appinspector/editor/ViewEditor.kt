package com.shashank.appinspector.editor

import android.app.Activity
import android.view.View
import android.widget.TextView

internal object ViewEditor {

    fun toggleVisibility(view: View): Int {
        view.visibility = when (view.visibility) {
            View.VISIBLE -> View.GONE
            else -> View.VISIBLE
        }
        view.requestLayout()
        return view.visibility
    }

    fun showEditTextDialog(activity: Activity, view: View, onEdited: () -> Unit) {
        if (view !is TextView) return
        PropertyEditorSheet(activity).showTextEditor(view, onEdited)
    }

    fun showEditPaddingDialog(activity: Activity, view: View, onEdited: () -> Unit) {
        PropertyEditorSheet(activity).showPaddingEditor(view, onEdited)
    }
}

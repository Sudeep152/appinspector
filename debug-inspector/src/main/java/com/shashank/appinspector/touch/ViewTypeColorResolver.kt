package com.shashank.appinspector.touch

import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.shashank.appinspector.R

internal object ViewTypeColorResolver {

    fun getColor(view: View): Int {
        val ctx = view.context
        return when {
            view is ImageView || view is ImageButton ->
                ContextCompat.getColor(ctx, R.color.debug_inspector_type_image)

            view is EditText || view is Button || view is CheckBox || view is TextView ->
                ContextCompat.getColor(ctx, R.color.debug_inspector_type_text)

            view is RecyclerView || view is ListView ||
            view is ScrollView || view is HorizontalScrollView ->
                ContextCompat.getColor(ctx, R.color.debug_inspector_type_list)

            view.javaClass.name.contains("ComposeView") ||
            view.javaClass.name.contains("AndroidComposeView") ->
                ContextCompat.getColor(ctx, R.color.debug_inspector_type_compose)

            view is LinearLayout || view is FrameLayout ||
            view is RelativeLayout ||
            view.javaClass.name.contains("ConstraintLayout") ||
            view.javaClass.name.contains("CoordinatorLayout") ||
            view.javaClass.name.contains("DrawerLayout") ->
                ContextCompat.getColor(ctx, R.color.debug_inspector_type_layout)

            else -> ContextCompat.getColor(ctx, R.color.debug_inspector_type_default)
        }
    }
}

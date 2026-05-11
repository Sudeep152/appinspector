package com.shashank.appinspector.touch

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shashank.appinspector.R
import com.shashank.appinspector.utils.FragmentOwnerResolver
import com.shashank.appinspector.utils.ResourceUtils

internal class ViewStackPickerPopup(
    private val activity: Activity,
    private val onViewSelected: (View) -> Unit
) {

    private var popup: PopupWindow? = null

    fun show(views: List<View>, screenX: Int, screenY: Int) {
        dismiss()
        if (views.isEmpty()) return

        val contentView = LayoutInflater.from(activity)
            .inflate(R.layout.layout_view_stack_picker, null)

        val recyclerView = contentView.findViewById<RecyclerView>(R.id.di_stack_picker_rv)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.addItemDecoration(
            DividerItemDecoration(activity, DividerItemDecoration.VERTICAL).apply {
                activity.getDrawable(android.R.drawable.divider_horizontal_bright)?.let { setDrawable(it) }
            }
        )
        recyclerView.adapter = ViewStackAdapter(activity, views.reversed()) { view ->
            dismiss()
            onViewSelected(view)
        }

        val density = activity.resources.displayMetrics.density
        val widthPx = (280 * density + 0.5f).toInt()

        popup = PopupWindow(contentView, widthPx, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
            isOutsideTouchable = true
            elevation = 12f * density
            setBackgroundDrawable(activity.getDrawable(R.drawable.bg_view_stack_picker))
            animationStyle = android.R.style.Animation_Dialog
        }

        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupHeight = contentView.measuredHeight
        val screenWidth = activity.resources.displayMetrics.widthPixels
        val screenHeight = activity.resources.displayMetrics.heightPixels
        val marginPx = (16 * density + 0.5f).toInt()

        val x = (screenX - widthPx / 2).coerceIn(marginPx, screenWidth - widthPx - marginPx)
        val y = if (screenY > screenHeight / 2) {
            (screenY - popupHeight - marginPx * 2).coerceAtLeast(marginPx)
        } else {
            (screenY + marginPx * 2).coerceAtMost(screenHeight - popupHeight - marginPx)
        }

        popup?.showAtLocation(activity.window.decorView, Gravity.NO_GRAVITY, x, y)
    }

    fun dismiss() {
        popup?.dismiss()
        popup = null
    }

    private class ViewStackAdapter(
        private val activity: Activity,
        private val views: List<View>,
        private val onClick: (View) -> Unit
    ) : RecyclerView.Adapter<ViewStackAdapter.VH>() {

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val chip: View = view.findViewById(R.id.di_stack_chip)
            val className: TextView = view.findViewById(R.id.di_stack_class)
            val idText: TextView = view.findViewById(R.id.di_stack_id)
            val sizeBadge: TextView = view.findViewById(R.id.di_stack_size)
            val properties: TextView = view.findViewById(R.id.di_stack_properties)
            val fragment: TextView = view.findViewById(R.id.di_stack_fragment)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_view_stack_item, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val view = views[position]
            val density = holder.itemView.resources.displayMetrics.density

            (holder.chip.background as? GradientDrawable)?.setColor(ViewTypeColorResolver.getColor(view))

            holder.className.text = view.javaClass.simpleName

            val id = ResourceUtils.getResourceEntryName(view)
            if (id != null) {
                holder.idText.text = "#$id"
                holder.idText.visibility = View.VISIBLE
            } else {
                holder.idText.visibility = View.GONE
            }

            if (view.width > 0 && view.height > 0) {
                val w = ResourceUtils.pxToDp(view.width, density)
                val h = ResourceUtils.pxToDp(view.height, density)
                holder.sizeBadge.text = "${w}×${h}"
                holder.sizeBadge.visibility = View.VISIBLE
            } else {
                holder.sizeBadge.visibility = View.GONE
            }

            val props = buildPropertyLine(view, density)
            if (props.isNotEmpty()) {
                holder.properties.text = props
                holder.properties.visibility = View.VISIBLE
            } else {
                holder.properties.visibility = View.GONE
            }

            val fragmentName = FragmentOwnerResolver.getOwnerName(activity, view)
            if (fragmentName != null) {
                holder.fragment.text = fragmentName
                holder.fragment.visibility = View.VISIBLE
            } else {
                holder.fragment.visibility = View.GONE
            }

            holder.itemView.setOnClickListener { onClick(view) }
        }

        private fun buildPropertyLine(view: View, density: Float): String {
            val parts = mutableListOf<String>()
            if (view is android.widget.TextView) {
                val text = view.text?.toString()?.take(20)
                if (!text.isNullOrBlank()) parts.add("\"$text\"")
            }
            val elevDp = ResourceUtils.pxToDp(view.elevation.toInt(), density)
            if (elevDp > 0) parts.add("elev: ${elevDp}dp")
            if (view.alpha < 1f) parts.add("alpha: ${"%.1f".format(view.alpha)}")
            if (view.visibility != View.VISIBLE) {
                parts.add(ResourceUtils.getVisibilityString(view.visibility))
            }
            return parts.joinToString(" | ")
        }

        override fun getItemCount() = views.size
    }
}

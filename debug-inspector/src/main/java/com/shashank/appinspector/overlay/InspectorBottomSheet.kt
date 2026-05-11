package com.shashank.appinspector.overlay

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.shashank.appinspector.R
import com.shashank.appinspector.compose.ComposeDebugInfo
import com.shashank.appinspector.context.ActivityContextResolver
import com.shashank.appinspector.editor.ViewEditor
import com.shashank.appinspector.hierarchy.FlatViewNode
import com.shashank.appinspector.hierarchy.HierarchyTreeController
import com.shashank.appinspector.hierarchy.ViewHierarchyBuilder
import com.shashank.appinspector.touch.ViewTypeColorResolver
import com.shashank.appinspector.utils.FragmentOwnerResolver
import com.shashank.appinspector.utils.ResourceUtils
import com.shashank.appinspector.utils.ZoomableFrameLayout
import java.lang.ref.WeakReference
import kotlin.math.abs

internal class InspectorBottomSheet(
    private val activity: Activity,
    private val onViewSelected: (View) -> Unit
) {

    private var currentDialog: BottomSheetDialog? = null

    fun show(view: View, composeInfo: ComposeDebugInfo? = null) {
        dismiss()
        if (activity.isDestroyed || activity.isFinishing) return

        val dialog = BottomSheetDialog(activity)
        val contentView = LayoutInflater.from(activity)
            .inflate(R.layout.layout_inspector_bottom_sheet, null)

        val density = activity.resources.displayMetrics.density

        populateContextBanner(contentView)
        populateViewInfo(contentView, view, density, composeInfo)
        setupActions(contentView, view, density)

        dialog.setContentView(contentView)
        dialog.behavior.peekHeight = (activity.resources.displayMetrics.heightPixels * 0.48).toInt()
        dialog.show()
        currentDialog = dialog
    }

    fun dismiss() {
        currentDialog?.dismiss()
        currentDialog = null
    }

    private fun populateContextBanner(contentView: View) {
        try {
            val info = ActivityContextResolver.resolve(activity)

            contentView.findViewById<TextView>(R.id.di_context_path).text = info.fragmentPath

            val backStackView = contentView.findViewById<TextView>(R.id.di_context_backstack)
            if (info.backStackDepth > 0) {
                backStackView.text = activity.getString(R.string.debug_inspector_back_stack, info.backStackDepth)
                backStackView.visibility = View.VISIBLE
            } else {
                backStackView.visibility = View.GONE
            }
        } catch (_: Exception) {
            contentView.findViewById<View>(R.id.di_context_banner).visibility = View.GONE
        }
    }

    private fun populateViewInfo(
        contentView: View,
        targetView: View,
        density: Float,
        composeInfo: ComposeDebugInfo?
    ) {
        val chip = contentView.findViewById<View>(R.id.di_title_type_chip)
        (chip.background as? GradientDrawable)?.setColor(ViewTypeColorResolver.getColor(targetView))

        contentView.findViewById<TextView>(R.id.di_class_value).text =
            targetView.javaClass.simpleName

        val resName = ResourceUtils.getResourceName(targetView)
        val idValue = contentView.findViewById<TextView>(R.id.di_id_value)
        idValue.text = resName ?: activity.getString(R.string.debug_inspector_no_id)
        makeCopyable(idValue)

        val widthDp = ResourceUtils.pxToDp(targetView.width, density)
        val heightDp = ResourceUtils.pxToDp(targetView.height, density)
        val sizeValue = contentView.findViewById<TextView>(R.id.di_size_value)
        sizeValue.text = "${widthDp}dp x ${heightDp}dp  (${targetView.width}x${targetView.height}px)"
        makeCopyable(sizeValue)

        val location = IntArray(2)
        targetView.getLocationOnScreen(location)
        val posValue = contentView.findViewById<TextView>(R.id.di_position_value)
        posValue.text = "(${location[0]}, ${location[1]})"
        makeCopyable(posValue)

        val visValue = contentView.findViewById<TextView>(R.id.di_visibility_value)
        visValue.text = ResourceUtils.getVisibilityString(targetView.visibility)
        makeCopyable(visValue)

        val marginsStr = ResourceUtils.getMarginsString(targetView, density)
        val marginsRow = contentView.findViewById<View>(R.id.di_margins_row)
        if (marginsStr != null) {
            marginsRow.visibility = View.VISIBLE
            val marginsValue = contentView.findViewById<TextView>(R.id.di_margins_value)
            marginsValue.text = marginsStr
            makeCopyable(marginsValue)
        } else {
            marginsRow.visibility = View.GONE
        }

        val elevValue = contentView.findViewById<TextView>(R.id.di_elevation_value)
        elevValue.text = ResourceUtils.getElevationDp(targetView, density)
        makeCopyable(elevValue)

        val alphaValue = contentView.findViewById<TextView>(R.id.di_alpha_value)
        alphaValue.text = ResourceUtils.getAlphaString(targetView)
        makeCopyable(alphaValue)

        val bgStr = ResourceUtils.getBackgroundColorHex(targetView)
        val bgRow = contentView.findViewById<View>(R.id.di_bg_row)
        if (bgStr != null) {
            bgRow.visibility = View.VISIBLE
            val bgValue = contentView.findViewById<TextView>(R.id.di_bg_value)
            bgValue.text = bgStr
            makeCopyable(bgValue)
        } else {
            bgRow.visibility = View.GONE
        }

        val rotStr = ResourceUtils.getRotationString(targetView)
        val rotRow = contentView.findViewById<View>(R.id.di_rotation_row)
        if (rotStr != null) {
            rotRow.visibility = View.VISIBLE
            contentView.findViewById<TextView>(R.id.di_rotation_value).text = rotStr
        } else {
            rotRow.visibility = View.GONE
        }

        val scaleStr = ResourceUtils.getScaleString(targetView)
        val scaleRow = contentView.findViewById<View>(R.id.di_scale_row)
        if (scaleStr != null) {
            scaleRow.visibility = View.VISIBLE
            contentView.findViewById<TextView>(R.id.di_scale_value).text = scaleStr
        } else {
            scaleRow.visibility = View.GONE
        }

        val stateValue = contentView.findViewById<TextView>(R.id.di_state_flags_value)
        stateValue.text = ResourceUtils.getStateFlags(targetView)
        makeCopyable(stateValue)

        val fragmentName = FragmentOwnerResolver.getOwnerName(activity, targetView)
        val fragmentRow = contentView.findViewById<View>(R.id.di_fragment_row)
        if (fragmentName != null) {
            fragmentRow.visibility = View.VISIBLE
            val fragValue = contentView.findViewById<TextView>(R.id.di_fragment_value)
            fragValue.text = fragmentName
            makeCopyable(fragValue)
        } else {
            fragmentRow.visibility = View.GONE
        }

        if (targetView is TextView) {
            contentView.findViewById<View>(R.id.di_text_row).visibility = View.VISIBLE
            val textValue = contentView.findViewById<TextView>(R.id.di_text_value)
            textValue.text = targetView.text
            makeCopyable(textValue)
        } else {
            contentView.findViewById<View>(R.id.di_text_row).visibility = View.GONE
        }

        val pL = ResourceUtils.pxToDp(targetView.paddingLeft, density)
        val pT = ResourceUtils.pxToDp(targetView.paddingTop, density)
        val pR = ResourceUtils.pxToDp(targetView.paddingRight, density)
        val pB = ResourceUtils.pxToDp(targetView.paddingBottom, density)
        val padValue = contentView.findViewById<TextView>(R.id.di_padding_value)
        padValue.text = "$pL, $pT, $pR, $pB dp"
        makeCopyable(padValue)

        if (composeInfo != null) {
            contentView.findViewById<View>(R.id.di_compose_row).visibility = View.VISIBLE
            contentView.findViewById<TextView>(R.id.di_compose_tag_value).text = composeInfo.tag
        } else {
            contentView.findViewById<View>(R.id.di_compose_row).visibility = View.GONE
        }

        contentView.findViewById<TextView>(R.id.di_parent_chain).text =
            ResourceUtils.buildParentChain(targetView)
    }

    private fun makeCopyable(textView: TextView) {
        textView.setOnLongClickListener {
            val text = textView.text?.toString() ?: return@setOnLongClickListener false
            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("DebugInspector", text))
            Toast.makeText(activity, activity.getString(R.string.debug_inspector_copied, text), Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun setupActions(contentView: View, targetView: View, density: Float) {
        val viewRef = WeakReference(targetView)

        contentView.findViewById<View>(R.id.di_btn_toggle_visibility).setOnClickListener {
            val v = viewRef.get() ?: return@setOnClickListener
            val newVis = ViewEditor.toggleVisibility(v)
            contentView.findViewById<TextView>(R.id.di_visibility_value).text =
                ResourceUtils.getVisibilityString(newVis)
        }

        val textRow = contentView.findViewById<View>(R.id.di_text_row)
        if (textRow.visibility == View.VISIBLE) {
            contentView.findViewById<View>(R.id.di_btn_edit_text).setOnClickListener {
                val v = viewRef.get() ?: return@setOnClickListener
                ViewEditor.showEditTextDialog(activity, v) {
                    if (v is TextView) {
                        contentView.findViewById<TextView>(R.id.di_text_value).text = v.text
                    }
                }
            }
        }

        contentView.findViewById<View>(R.id.di_btn_edit_padding).setOnClickListener {
            val v = viewRef.get() ?: return@setOnClickListener
            ViewEditor.showEditPaddingDialog(activity, v) {
                val pL = ResourceUtils.pxToDp(v.paddingLeft, density)
                val pT = ResourceUtils.pxToDp(v.paddingTop, density)
                val pR = ResourceUtils.pxToDp(v.paddingRight, density)
                val pB = ResourceUtils.pxToDp(v.paddingBottom, density)
                contentView.findViewById<TextView>(R.id.di_padding_value).text =
                    "$pL, $pT, $pR, $pB dp"
            }
        }

        contentView.findViewById<View>(R.id.di_btn_hierarchy).setOnClickListener {
            showHierarchyDialog(targetView)
        }

        val parentView = targetView.parent as? View
        val decorRoot = activity.window.decorView
        val btnParentNav = contentView.findViewById<View>(R.id.di_btn_parent_nav)
        if (parentView != null && parentView !== decorRoot) {
            btnParentNav.visibility = View.VISIBLE
            btnParentNav.contentDescription = activity.getString(R.string.debug_inspector_cd_parent_nav)
            btnParentNav.setOnClickListener {
                dismiss()
                onViewSelected(parentView)
            }
        } else {
            btnParentNav.visibility = View.GONE
        }
    }

    private fun showHierarchyDialog(selectedView: View) {
        val decorView = activity.window.decorView
        val root = (decorView as? ViewGroup)?.getChildAt(0) ?: decorView
        val tree = ViewHierarchyBuilder.buildTree(root) ?: return
        val controller = HierarchyTreeController(tree, selectedView)

        val dialog = BottomSheetDialog(activity)
        val sheetView = LayoutInflater.from(activity)
            .inflate(R.layout.layout_hierarchy_sheet, null)

        val recyclerView = sheetView.findViewById<RecyclerView>(R.id.di_hierarchy_rv)
        val zoomContainer = sheetView.findViewById<ZoomableFrameLayout>(R.id.di_zoom_container)
        val zoomLabel = sheetView.findViewById<TextView>(R.id.di_zoom_label)

        sheetView.findViewById<View>(R.id.di_zoom_in).apply {
            contentDescription = activity.getString(R.string.debug_inspector_cd_zoom_in)
            setOnClickListener {
                zoomContainer.zoomIn()
                updateZoomLabel(zoomLabel, zoomContainer.getCurrentScale())
            }
        }
        sheetView.findViewById<View>(R.id.di_zoom_out).apply {
            contentDescription = activity.getString(R.string.debug_inspector_cd_zoom_out)
            setOnClickListener {
                zoomContainer.zoomOut()
                updateZoomLabel(zoomLabel, zoomContainer.getCurrentScale())
            }
        }
        sheetView.findViewById<View>(R.id.di_zoom_fit).apply {
            contentDescription = activity.getString(R.string.debug_inspector_cd_zoom_fit)
            setOnClickListener {
                zoomContainer.post {
                    zoomContainer.fitToScreen()
                    updateZoomLabel(zoomLabel, zoomContainer.getCurrentScale())
                }
            }
        }

        var hierarchyAdapter: HierarchyAdapter? = null
        hierarchyAdapter = HierarchyAdapter(
            controller = controller,
            onToggleCollapse = { node ->
                controller.toggleCollapse(node)
                hierarchyAdapter?.refresh()
            },
            onNodeClick = { node ->
                val v = node.viewRef.get() ?: return@HierarchyAdapter
                dialog.dismiss()
                onViewSelected(v)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = hierarchyAdapter

        val searchInput = sheetView.findViewById<android.widget.EditText>(R.id.di_hierarchy_search)
        val searchCount = sheetView.findViewById<TextView>(R.id.di_hierarchy_search_count)
        val searchHandler = android.os.Handler(android.os.Looper.getMainLooper())

        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                searchHandler.removeCallbacksAndMessages(null)
                searchHandler.postDelayed({
                    val query = s?.toString()?.trim() ?: ""
                    hierarchyAdapter?.filter(query)
                    val count = hierarchyAdapter?.itemCount ?: 0
                    if (query.isNotEmpty()) {
                        searchCount.text = activity.getString(R.string.debug_inspector_search_results, count)
                        searchCount.visibility = View.VISIBLE
                    } else {
                        searchCount.visibility = View.GONE
                    }
                }, 300)
            }
        })

        dialog.setContentView(sheetView)
        val screenHeight = activity.resources.displayMetrics.heightPixels
        dialog.behavior.peekHeight = (screenHeight * 0.75).toInt()
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.show()
    }

    private fun updateZoomLabel(label: TextView, scale: Float) {
        val text = "%.1fx".format(scale)
        label.text = text
        label.visibility = if (abs(scale - 1f) > 0.05f) View.VISIBLE else View.GONE
    }

    private inner class HierarchyAdapter(
        private val controller: HierarchyTreeController,
        private val onToggleCollapse: (FlatViewNode) -> Unit,
        private val onNodeClick: (FlatViewNode) -> Unit
    ) : RecyclerView.Adapter<HierarchyAdapter.VH>() {

        private var allItems: List<FlatViewNode> = controller.getVisibleNodes()
        private var items: List<FlatViewNode> = allItems
        private var searchQuery: String = ""

        fun refresh() {
            allItems = controller.getVisibleNodes()
            filter(searchQuery)
        }

        fun filter(query: String) {
            searchQuery = query
            items = if (query.isBlank()) {
                allItems
            } else {
                val lower = query.lowercase()
                allItems.filter { node ->
                    node.className.lowercase().contains(lower) ||
                    node.idName?.lowercase()?.contains(lower) == true ||
                    (node.viewRef.get() as? android.widget.TextView)?.text?.toString()?.lowercase()?.contains(lower) == true
                }
            }
            notifyDataSetChanged()
        }

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val indentSpacer: View = view.findViewById(R.id.di_hierarchy_indent)
            val expandArrow: ImageView = view.findViewById(R.id.di_hierarchy_arrow)
            val colorChip: View = view.findViewById(R.id.di_hierarchy_chip)
            val className: TextView = view.findViewById(R.id.di_hierarchy_class)
            val idText: TextView = view.findViewById(R.id.di_hierarchy_id)
            val countBadge: TextView = view.findViewById(R.id.di_hierarchy_count)
            val goneBadge: TextView = view.findViewById(R.id.di_hierarchy_gone)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.layout_hierarchy_item, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val node = items[position]
            val ctx = holder.itemView.context
            val indentUnit = ctx.resources.getDimensionPixelSize(R.dimen.debug_inspector_hierarchy_indent)

            holder.indentSpacer.layoutParams =
                holder.indentSpacer.layoutParams.also { it.width = node.depth * indentUnit }

            if (node.hasChildren) {
                holder.expandArrow.visibility = View.VISIBLE
                if (node.isCollapsed) {
                    holder.expandArrow.setImageResource(R.drawable.ic_chevron_right)
                    holder.expandArrow.contentDescription = ctx.getString(R.string.debug_inspector_cd_expand)
                } else {
                    holder.expandArrow.setImageResource(R.drawable.ic_chevron_down)
                    holder.expandArrow.contentDescription = ctx.getString(R.string.debug_inspector_cd_collapse)
                }
                holder.expandArrow.setOnClickListener { onToggleCollapse(node) }
            } else {
                holder.expandArrow.visibility = View.INVISIBLE
                holder.expandArrow.setOnClickListener(null)
            }

            (holder.colorChip.background as? GradientDrawable)?.setColor(node.viewTypeColor)

            holder.className.text = node.className
            holder.className.setTypeface(null, if (node.hasChildren) Typeface.BOLD else Typeface.NORMAL)
            if (node.isGone) {
                holder.className.paintFlags =
                    holder.className.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                holder.className.alpha = 0.45f
            } else {
                holder.className.paintFlags =
                    holder.className.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                holder.className.alpha = 1f
            }

            if (node.idName != null) {
                holder.idText.text = "#${node.idName}"
                holder.idText.visibility = View.VISIBLE
            } else {
                holder.idText.visibility = View.GONE
            }

            if (node.childCountLabel.isNotEmpty()) {
                holder.countBadge.text = node.childCountLabel
                holder.countBadge.visibility = View.VISIBLE
            } else {
                holder.countBadge.visibility = View.GONE
            }

            holder.goneBadge.visibility = if (node.isGone) View.VISIBLE else View.GONE

            if (node.isSelected) {
                holder.itemView.setBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.debug_inspector_hierarchy_selected_bg)
                )
            } else {
                holder.itemView.setBackgroundColor(
                    ContextCompat.getColor(ctx, R.color.debug_inspector_transparent)
                )
            }

            holder.itemView.setOnClickListener { onNodeClick(node) }
        }

        override fun getItemCount() = items.size
    }
}

package com.github.rooneyandshadows.lightbulb.recycleradapters.implementation.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class StaticViewsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val views = mutableListOf<FixedViewInfo>()
    private var isStaggeredGrid = false

    val headersCount: Int
        get() = views.size

    override fun getItemCount(): Int = views.size

    override fun getItemViewType(position: Int): Int {
        return views[position].viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val index = views.indexOfFirst { it.viewType == viewType }
        val info = views[index]
        return createViewHolder(info.view, info.viewListeners)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // nothing to bind (static views)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
    }

    fun addView(view: View, listeners: ViewListeners? = null) {
        val info = FixedViewInfo(
            viewType = BASE_VIEW_TYPE + views.size,
            view = view,
            localPosition = views.size,
            viewListeners = listeners
        )
        views.add(info)
        notifyItemInserted(views.lastIndex)
    }

    fun removeView(view: View): Boolean {
        val index = views.indexOfFirst { it.view === view }
        if (index == -1) return false

        views.removeAt(index)
        refreshPositions()
        notifyItemRemoved(index)
        return true
    }

    fun removeAllViews() {
        val count = views.size
        if (count == 0) return

        views.clear()
        notifyItemRangeRemoved(0, count)
    }

    fun containsView(view: View): Boolean {
        return views.any { it.view === view }
    }

    fun setViewVisibility(visible: Boolean) {
        views.forEach {
            it.view.visibility = if (visible) View.VISIBLE else View.GONE
        }
        notifyItemRangeChanged(0, views.size)
    }

    fun adjustSpanSize(recyclerView: RecyclerView) {
        val lm = recyclerView.layoutManager

        if (lm is GridLayoutManager) {
            lm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int = lm.spanCount
            }
        }

        if (lm is StaggeredGridLayoutManager) {
            isStaggeredGrid = true
        }
    }

    private fun refreshPositions() {
        views.forEachIndexed { index, info ->
            info.localPosition = index
        }
    }

    private fun createViewHolder(
        view: View,
        listeners: ViewListeners?
    ): RecyclerView.ViewHolder {

        if (isStaggeredGrid) {
            val params = StaggeredGridLayoutManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.isFullSpan = true
            view.layoutParams = params
        } else {
            view.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        listeners?.onCreated(view)

        return object : RecyclerView.ViewHolder(view) {}
    }

    class FixedViewInfo(
        val viewType: Int,
        val view: View,
        var localPosition: Int,
        val viewListeners: ViewListeners?
    )

    interface ViewListeners {
        fun onCreated(view: View?)
    }

    companion object {
        private const val BASE_VIEW_TYPE = -1 shl 10
    }
}
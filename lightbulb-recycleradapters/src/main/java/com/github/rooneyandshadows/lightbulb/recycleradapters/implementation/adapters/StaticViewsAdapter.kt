package com.github.rooneyandshadows.lightbulb.recycleradapters.implementation.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class StaticViewsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val views = mutableListOf<FixedViewInfo>()
    private var isStaggeredGrid = false

    fun addView(
        id: String,
        viewFactory: (ViewGroup) -> View,
        viewBinder: ((View, Int) -> Unit)? = null,
        listeners: ViewListeners? = null
    ) {
        if (views.any { it.id == id }) return

        val viewType = BASE_VIEW_TYPE + views.size
        val info = FixedViewInfo(
            id = id,
            viewType = viewType,
            localPosition = views.size,
            viewFactory = viewFactory,
            viewBinder = viewBinder,
            viewListeners = listeners
        )
        views.add(info)
        notifyItemInserted(views.lastIndex)
    }

    fun removeViewById(id: String) {
        val index = views.indexOfFirst { it.id == id }
        if (index == -1) return
        views.removeAt(index)
        refreshPositions()
        notifyItemRemoved(index)
    }

    fun updateViewById(id: String) {
        val index = views.indexOfFirst { it.id == id }
        if (index == -1) return

        notifyItemChanged(index)
    }

    fun containsView(id: String): Boolean {
        return views.any { it.id == id }
    }

    fun removeAllViews() {
        val count = views.size
        if (count == 0) return

        views.clear()
        notifyItemRangeRemoved(0, count)
    }

    override fun getItemCount(): Int = views.size

    override fun getItemViewType(position: Int): Int {
        return views[position].viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val info = views.first { it.viewType == viewType }
        val view = info.viewFactory(parent)

        return createViewHolder(view, info.viewListeners)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val info = views[position]

        info.viewBinder?.invoke(holder.itemView, position)
    }

    private fun createViewHolder(view: View, listeners: ViewListeners?): RecyclerView.ViewHolder {
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

    data class FixedViewInfo(
        val id: String,
        val viewType: Int,
        var localPosition: Int,
        val viewFactory: (ViewGroup) -> View,
        val viewBinder: ((View, Int) -> Unit)?,
        val viewListeners: ViewListeners?
    )

    interface ViewListeners {
        fun onCreated(view: View)
    }

    companion object {
        private const val BASE_VIEW_TYPE = -1 shl 10
    }
}
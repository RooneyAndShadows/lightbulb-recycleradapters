package com.github.rooneyandshadows.lightbulb.recycleradapters.implementation.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import kotlin.math.abs

@Suppress("unused")
class HeaderViewRecyclerAdapter @JvmOverloads constructor(
    private val recyclerView: RecyclerView,
    headerViewInfoList: MutableList<FixedViewInfo>? = null,
    footerViewInfoList: MutableList<FixedViewInfo>? = null,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val mHeaderViewInfoList: MutableList<FixedViewInfo> = headerViewInfoList ?: mutableListOf()
    private val mFooterViewInfoList: MutableList<FixedViewInfo> = footerViewInfoList ?: mutableListOf()
    private var adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>? = null
    private var mIsStaggeredGrid = false

    fun setDataAdapter(dataAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>) {
        adapter = dataAdapter
        setHasStableIds(dataAdapter.hasStableIds())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (isHeaderViewType(viewType)) {
            val headerIndex = abs(viewType - BASE_HEADER_VIEW_TYPE)
            val info = mHeaderViewInfoList[headerIndex]
            createHeaderFooterViewHolder(info.view, info.viewListeners)
        } else if (isFooterViewType(viewType)) {
            val footerIndex = abs(viewType - BASE_FOOTER_VIEW_TYPE)
            val info = mFooterViewInfoList[footerIndex]
            createHeaderFooterViewHolder(info.view, info.viewListeners)
        } else {
            adapter!!.onCreateViewHolder(parent, viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position < headersCount || position >= headersCount + adapter!!.itemCount)
            return
        adapter!!.onBindViewHolder(holder, position - headersCount)
    }

    override fun onViewRecycled(viewHolder: RecyclerView.ViewHolder) {
        val vtype = viewHolder.itemViewType
        if (isHeaderPosition(vtype) || isFooterPosition(vtype)) {
            super.onViewRecycled(viewHolder)
        } else {
            adapter!!.onViewRecycled(viewHolder)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isHeaderPosition(position)) {
            mHeaderViewInfoList[position].viewType
        } else if (isFooterPosition(position)) {
            mFooterViewInfoList[position - adapter!!.itemCount - headersCount].viewType
        } else {
            adapter!!.getItemViewType(position - headersCount)
        }
    }

    override fun getItemCount(): Int {
        return footersCount + headersCount + adapter!!.itemCount
    }

    override fun getItemId(position: Int): Long {
        return if (isFooterPosition(position) || isHeaderPosition(position)) position.toLong()
        else adapter!!.getItemId(position)
    }

    override fun registerAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        super.registerAdapterDataObserver(observer)
        adapter!!.registerAdapterDataObserver(observer)
    }

    override fun unregisterAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) {
        super.unregisterAdapterDataObserver(observer)
        adapter!!.unregisterAdapterDataObserver(observer)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        adapter!!.onAttachedToRecyclerView(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        adapter!!.onDetachedFromRecyclerView(recyclerView)
    }

    class FixedViewInfo(
        val viewType: Int,
        val view: View,
        var localPosition: Int,
        var viewListeners: ViewListeners?,
    )

    interface ViewListeners {
        fun onCreated(view: View?)
    }

    val headersCount: Int
        get() = mHeaderViewInfoList.size
    val footersCount: Int
        get() = mFooterViewInfoList.size
    val isEmpty: Boolean
        get() = adapter == null || adapter!!.itemCount == 0

    fun removeAllHeaderView() {
        if (mHeaderViewInfoList.isNotEmpty()) {
            val positionsToRemove = findHeaderPositions()
            mHeaderViewInfoList.clear()
            if (positionsToRemove.isNotEmpty()) notifyItemRangeRemoved(
                positionsToRemove[0],
                positionsToRemove.size
            )
        }
    }

    fun removeAllFooterView() {
        if (mFooterViewInfoList.isNotEmpty()) {
            val positionsToRemove = findFooterPositions()
            mFooterViewInfoList.clear()
            if (positionsToRemove.isNotEmpty())
                notifyItemRangeRemoved(
                    positionsToRemove[0],
                    positionsToRemove.size
                )
        }
    }

    @JvmOverloads
    fun addHeaderView(view: View?, viewListeners: ViewListeners? = null) {
        requireNotNull(view) { "the view to add must not be null" }
        val positionToAdd = headersCount
        val info = FixedViewInfo(
            BASE_HEADER_VIEW_TYPE + mHeaderViewInfoList.size,
            view,
            positionToAdd,
            viewListeners
        )
        mHeaderViewInfoList.add(info)
        refreshHeaderPositions()
        notifyItemInserted(positionToAdd)
    }

    fun removeHeaderView(v: View): Boolean {
        for (i in mHeaderViewInfoList.indices) {
            val info = mHeaderViewInfoList[i]
            if (info.view === v) {
                val positionToRemove = info.localPosition
                mHeaderViewInfoList.removeAt(i)
                refreshHeaderPositions()
                if (positionToRemove != -1)
                    notifyItemRemoved(positionToRemove)
                return true
            }
        }
        return false
    }

    @JvmOverloads
    fun addFooterView(view: View?, viewListeners: ViewListeners? = null) {
        requireNotNull(view) { "the view to add must not be null!" }
        val positionToAdd = headersCount + adapter!!.itemCount + footersCount
        val localPosition = footersCount
        val info = FixedViewInfo(
            BASE_FOOTER_VIEW_TYPE + mFooterViewInfoList.size,
            view,
            localPosition,
            viewListeners
        )
        mFooterViewInfoList.add(info)
        refreshFooterPositions()
        notifyItemInserted(positionToAdd)
    }

    fun removeFooterView(v: View): Boolean {
        for (i in mFooterViewInfoList.indices) {
            val info = mFooterViewInfoList[i]
            if (info.view === v) {
                val positionToRemove = headersCount + adapter!!.itemCount + info.localPosition
                mFooterViewInfoList.removeAt(i)
                refreshFooterPositions()
                if (positionToRemove != -1) notifyItemRemoved(positionToRemove)
                return true
            }
        }
        return false
    }

    fun containsFooterView(v: View): Boolean {
        for (i in mFooterViewInfoList.indices) {
            val info = mFooterViewInfoList[i]
            if (info.view === v) return true
        }
        return false
    }

    fun containsHeaderView(v: View): Boolean {
        for (i in mHeaderViewInfoList.indices) {
            val info = mHeaderViewInfoList[i]
            if (info.view === v) return true
        }
        return false
    }

    fun setHeaderVisibility(shouldShow: Boolean) {
        for (fixedViewInfo in mHeaderViewInfoList) fixedViewInfo.view.visibility =
            if (shouldShow) View.VISIBLE else View.GONE
        val positionsToChange = findHeaderPositions()
        if (positionsToChange.isNotEmpty()) notifyItemRangeChanged(
            positionsToChange[0],
            positionsToChange.size
        )
    }

    fun setFooterVisibility(shouldShow: Boolean) {
        for (fixedViewInfo in mFooterViewInfoList) fixedViewInfo.view.visibility =
            if (shouldShow) View.VISIBLE else View.GONE
        val positionsToChange = findFooterPositions()
        if (positionsToChange.isNotEmpty()) notifyItemRangeChanged(
            positionsToChange[0],
            positionsToChange.size
        )
    }

    private fun refreshFooterPositions() {
        for (position in mFooterViewInfoList.indices) {
            val info = mFooterViewInfoList[position]
            info.localPosition = position
        }
    }

    private fun refreshHeaderPositions() {
        for (position in mHeaderViewInfoList.indices) {
            val info = mHeaderViewInfoList[position]
            info.localPosition = position
        }
    }

    private fun isHeaderViewType(viewType: Int): Boolean {
        return (viewType >= BASE_HEADER_VIEW_TYPE
                && viewType < BASE_HEADER_VIEW_TYPE + mHeaderViewInfoList.size)
    }

    private fun isFooterViewType(viewType: Int): Boolean {
        return (viewType >= BASE_FOOTER_VIEW_TYPE
                && viewType < BASE_FOOTER_VIEW_TYPE + mFooterViewInfoList.size)
    }

    private fun isHeaderPosition(position: Int): Boolean {
        return position < mHeaderViewInfoList.size
    }

    private fun isFooterPosition(position: Int): Boolean {
        return position >= mHeaderViewInfoList.size + adapter!!.itemCount
    }

    private fun findAdapterPositionByView(view: View): Int {
        val holder = recyclerView.findContainingViewHolder(view)
        return holder?.absoluteAdapterPosition ?: -1
    }

    private fun findHeaderPositions(): List<Int> {
        val headerPositions: MutableList<Int> = ArrayList()
        for (i in 0 until itemCount) {
            if (isHeaderPosition(i)) headerPositions.add(i)
        }
        return headerPositions
    }

    private fun findFooterPositions(): List<Int> {
        val footerPositions: MutableList<Int> = ArrayList()
        for (i in 0 until itemCount) {
            if (isFooterPosition(i)) footerPositions.add(i)
        }
        return footerPositions
    }

    private fun createHeaderFooterViewHolder(
        view: View,
        listeners: ViewListeners?,
    ): RecyclerView.ViewHolder {
        if (mIsStaggeredGrid) {
            val layoutParams = StaggeredGridLayoutManager.LayoutParams(
                StaggeredGridLayoutManager.LayoutParams.MATCH_PARENT,
                StaggeredGridLayoutManager.LayoutParams.WRAP_CONTENT
            )
            layoutParams.isFullSpan = true
            view.layoutParams = layoutParams
        } else {
            val layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT
            )
            view.layoutParams = layoutParams
        }
        listeners?.onCreated(view)
        return object : RecyclerView.ViewHolder(view) {}
    }

    fun adjustSpanSize(recycler: RecyclerView) {
        if (recycler.layoutManager is GridLayoutManager) {
            val layoutManager = recycler.layoutManager as GridLayoutManager?
            layoutManager!!.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val isHeaderOrFooter = isHeaderPosition(position) || isFooterPosition(position)
                    return if (isHeaderOrFooter) layoutManager.spanCount else 1
                }
            }
        }
        if (recycler.layoutManager is StaggeredGridLayoutManager) {
            mIsStaggeredGrid = true
        }
    }

    companion object {
        private const val BASE_HEADER_VIEW_TYPE = -1 shl 10
        private const val BASE_FOOTER_VIEW_TYPE = -1 shl 11
    }
}
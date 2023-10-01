package com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.collection.EasyRecyclerAdapterCollection
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.data.EasyAdapterDataModel
import com.github.rooneyandshadows.lightbulb.recycleradapters.implementation.adapters.HeaderViewRecyclerAdapter
import com.github.rooneyandshadows.lightbulb.recycleradapters.implementation.collection.BasicCollection

//TODO fix to use ConcatAdapter instead of wrapping with HeaderViewRecyclerAdapter
@Suppress("MemberVisibilityCanBePrivate", "unused")
//@JvmSuppressWildcards
abstract class EasyRecyclerAdapter<ItemType : EasyAdapterDataModel>
    : Adapter<ViewHolder>() {
    private val items: EasyRecyclerAdapterCollection<ItemType> by lazy {
        return@lazy createCollection()
    }
    private var wrapperAdapter: HeaderViewRecyclerAdapter<ItemType>? = null
    var recyclerView: RecyclerView? = null
        private set
    open val footersCount: Int
        get() = if (wrapperAdapter == null) 0 else wrapperAdapter!!.footersCount
    open val headersCount: Int
        get() = if (wrapperAdapter == null) 0 else wrapperAdapter!!.headersCount
    open val collection: EasyRecyclerAdapterCollection<ItemType>
        get() = items


    /**
     * Used to create the collection that the adapter will use.
     *
     * @param outState state to save
     */
    protected open fun createCollection(): EasyRecyclerAdapterCollection<ItemType> {
        return BasicCollection(this)
    }

    /**
     * Used to add values to out state of the adapter during save state.
     *
     * @param outState state to save
     */
    protected open fun onSaveInstanceState(outState: Bundle) {
    }

    /**
     * Used to reuse values saved during previous save state.
     *
     * @param savedState The state that had previously been returned by onSaveInstanceState
     */
    protected open fun onRestoreInstanceState(savedState: Bundle) {
    }

    companion object {
        private const val COLLECTION_STATE_KEY = "COLLECTION_STATE_KEY"
    }

    @Override
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    @Override
    override fun getItemCount(): Int {
        return collection.size()
    }

    fun saveAdapterState(): Bundle {
        return Bundle().apply {
            putBundle(COLLECTION_STATE_KEY, collection.saveState())
            onSaveInstanceState(this)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun restoreAdapterState(savedState: Bundle) {
        savedState.apply {
            getBundle(COLLECTION_STATE_KEY)?.apply {
                collection.restoreState(this)
            }
            onRestoreInstanceState(savedState)
        }
    }

    internal fun wrap(headerAndFooterAdapter: HeaderViewRecyclerAdapter<ItemType>) {
        wrapperAdapter = headerAndFooterAdapter
    }
}
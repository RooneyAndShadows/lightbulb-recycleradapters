package com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterSelectableModes.*
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.collection.EasyRecyclerAdapterCollection
import com.github.rooneyandshadows.lightbulb.recycleradapters.implementation.HeaderViewRecyclerAdapter

//TODO fix to use ConcatAdapter instead of wrapping with HeaderViewRecyclerAdapter
@Suppress("MemberVisibilityCanBePrivate", "unused", "UNCHECKED_CAST")
@JvmSuppressWildcards
abstract class EasyRecyclerAdapter<CollectionType : EasyRecyclerAdapterCollection<out EasyAdapterDataModel>>
    : Adapter<ViewHolder>() {
    val collection: CollectionType by lazy { return@lazy createCollection() }
    var wrapperAdapter: HeaderViewRecyclerAdapter? = null
    var recyclerView: RecyclerView? = null
        private set
    val headersCount: Int
        get() = if (wrapperAdapter == null) 0 else wrapperAdapter!!.headersCount
    val footersCount: Int
        get() = if (wrapperAdapter == null) 0 else wrapperAdapter!!.footersCount

    abstract fun createCollection(): CollectionType

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
}
package com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction

import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.collection.AdapterCollection
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.data.EasyAdapterDataModel

@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class EasyRecyclerAdapter<ItemType : EasyAdapterDataModel> : Adapter<ViewHolder>() {
    private val _collection: AdapterCollection<ItemType> by lazy {
        return@lazy createCollection()
    }

    var recyclerView: RecyclerView? = null
        private set

    open val collection: AdapterCollection<ItemType>
        get() = _collection

    abstract fun createCollection(): AdapterCollection<ItemType>

    open fun onSaveInstanceState(): Bundle {
        return Bundle().apply {
            putBundle(COLLECTION_STATE_KEY, collection.onSaveInstanceState())
        }
    }

    open fun onRestoreInstanceState(savedState: Bundle) {
        savedState.getBundle(COLLECTION_STATE_KEY)?.apply {
            collection.onRestoreInstanceState(this)
        }
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
}
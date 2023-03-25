package com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.DiffUtil

@Suppress("UNCHECKED_CAST")
abstract class EasyRecyclerFilterableAdapter<ItemType : EasyAdapterDataModel> @JvmOverloads constructor(
    selectableMode: EasyAdapterSelectableModes = EasyAdapterSelectableModes.SELECT_NONE,
) : EasyRecyclerAdapter<ItemType>(selectableMode), Filterable {
    private var filteredPositions: MutableList<Int> = mutableListOf()
    var currentFilterQuery: String = ""
        private set

    companion object {
        private const val ADAPTER_FILTER_QUERY = "ADAPTER_FILTER_QUERY"
        private const val ADAPTER_FILTERED_POSITIONS = "ADAPTER_FILTERED_POSITIONS"
    }

    protected abstract fun filterItem(item: ItemType, filterQuery: String): Boolean

    @Override
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putString(ADAPTER_FILTER_QUERY, currentFilterQuery)
            putIntegerArrayList(ADAPTER_FILTERED_POSITIONS, ArrayList(filteredPositions))
        }

    }

    @Override
    override fun onRestoreInstanceState(savedState: Bundle) {
        super.onRestoreInstanceState(savedState)
        savedState.apply {
            currentFilterQuery = getString(ADAPTER_FILTER_QUERY) ?: ""
            filteredPositions = getIntegerArrayList(ADAPTER_FILTERED_POSITIONS)!!
        }
    }

    @Override
    override fun getItemCount(): Int {
        return getFilteredItems().size
    }

    @Override
    override fun onCollectionChanged() {
        super.onCollectionChanged()
        filter.filter(currentFilterQuery)
    }

    @Override
    final override fun getFilter(): Filter {
        return object : Filter() {
            @Override
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val items = getItems()
                currentFilterQuery = charSequence.toString()
                val filteredPositions: MutableList<Int> = mutableListOf()
                if (currentFilterQuery.isBlank()) {
                    for (position in items.indices) filteredPositions.add(position)
                } else {
                    getItems().forEachIndexed { index, item ->
                        if (filterItem(item, currentFilterQuery))
                            filteredPositions.add(index)
                    }
                }
                val filterResults = FilterResults()
                filterResults.values = filteredPositions
                return filterResults
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {

                val adapter = this@EasyRecyclerFilterableAdapter
                val newPositions = (filterResults.values as List<Int>).toMutableList()
                val oldPositions = filteredPositions.toMutableList()
                filteredPositions.apply {
                    clear()
                    addAll(newPositions)
                }
                val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                    override fun getOldListSize(): Int {
                        return oldPositions.size
                    }

                    override fun getNewListSize(): Int {
                        return newPositions.size
                    }

                    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        return oldPositions[oldItemPosition] == newPositions[newItemPosition]
                    }

                    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                        return true
                    }
                }, true)
                diff.dispatchUpdatesTo(EasyRecyclerAdapterUpdateCallback(adapter, headersCount))
                /*val toRemove = oldPositions.filter { return@filter !newPositions.contains(it) }.toMutableList()
                val toAdd = newPositions.filter { return@filter !oldPositions.contains(it) }
                toRemove.forEach { position ->
                    val posToRemove = oldPositions.indexOf(position)
                    oldPositions.removeAt(posToRemove)
                    notifyItemRemoved(posToRemove)
                }
                toAdd.forEach {
                    val posToAdd = newPositions.indexOf(it)
                    notifyItemInserted(posToAdd)
                }*/
            }
        }
    }

    fun getFilteredItems(): List<ItemType> {
        return mutableListOf<ItemType>().apply {
            for (filteredPosition in filteredPositions)
                add(getItems()[filteredPosition])
        }
    }
}
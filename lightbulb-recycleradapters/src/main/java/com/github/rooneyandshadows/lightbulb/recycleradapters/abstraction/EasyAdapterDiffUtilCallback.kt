package com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction

import androidx.recyclerview.widget.DiffUtil
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.collection.EasyRecyclerAdapterItemsComparator

@JvmSuppressWildcards
internal class EasyAdapterDiffUtilCallback<T : EasyAdapterDataModel>(
    private val oldData: List<SelectableItem<T>>?,
    private val newData: List<SelectableItem<T>>,
    private val compareCallbacks: EasyRecyclerAdapterItemsComparator<T>,
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldData!!.size
    }

    override fun getNewListSize(): Int {
        return newData.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return compareCallbacks.compareItems(oldData!![oldItemPosition].item, newData[newItemPosition].item)
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldData!![oldItemPosition]
        val newItem = newData[newItemPosition]
        return oldItem.isSelected == newItem.isSelected && compareCallbacks.compareItemsContent(
            oldItem.item,
            newItem.item
        )
    }
}
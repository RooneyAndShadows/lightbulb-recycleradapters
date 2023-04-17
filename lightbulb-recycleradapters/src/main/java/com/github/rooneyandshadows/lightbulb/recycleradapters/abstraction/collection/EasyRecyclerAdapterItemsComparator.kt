package com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.collection

import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterDataModel

@JvmSuppressWildcards
interface EasyRecyclerAdapterItemsComparator<T : EasyAdapterDataModel?> {
    fun compareItems(oldItem: T, newItem: T): Boolean
    fun compareItemsContent(oldItem: T, newItem: T): Boolean
}
package com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction

abstract class EasyAdapterItemsComparator<T : EasyAdapterDataModel?> {
    abstract fun compareItems(oldItem: T, newItem: T): Boolean
    abstract fun compareItemsContent(oldItem: T, newItem: T): Boolean
}
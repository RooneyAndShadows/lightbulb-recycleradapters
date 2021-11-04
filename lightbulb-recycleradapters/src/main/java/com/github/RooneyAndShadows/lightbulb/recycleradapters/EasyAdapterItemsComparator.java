package com.github.rooneyandshadows.lightbulb.recycleradapters;

public abstract class EasyAdapterItemsComparator<T extends EasyAdapterDataModel> {
    public abstract boolean compareItems(T oldItem, T newItem);

    public abstract boolean compareItemsContent(T oldItem, T newItem);

}

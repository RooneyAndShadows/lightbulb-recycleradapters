package com.github.rooneyandshadows.lightbulb.recycleradapters;

public abstract class LightBulbAdapterItemsComparator<T extends LightBulbAdapterDataModel> {
    public abstract boolean compareItems(T oldItem, T newItem);

    public abstract boolean compareItemsContent(T oldItem, T newItem);

}

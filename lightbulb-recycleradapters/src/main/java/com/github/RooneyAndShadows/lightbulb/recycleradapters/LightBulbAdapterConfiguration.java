package com.github.rooneyandshadows.lightbulb.recycleradapters;

import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.LifecycleOwner;

public class LightBulbAdapterConfiguration<T extends LightBulbAdapterDataModel> {
    private LifecycleOwner lifecycleOwner = null;
    private final List<T> items = new ArrayList<>();
    private LightBulbAdapterSelectableModes selectableMode = LightBulbAdapterSelectableModes.SELECT_NONE;
    private boolean useStableIds = false;
    private LightBulbAdapterItemsComparator<T> comparator = null;

    public LightBulbAdapterConfiguration<T> withLifecycleOwner(LifecycleOwner owner) {
        lifecycleOwner = owner;
        return this;
    }

    public LightBulbAdapterConfiguration<T> withItems(List<T> initialCollection) {
        items.clear();
        items.addAll(initialCollection);
        return this;
    }

    public LightBulbAdapterConfiguration<T> withSelectMode(LightBulbAdapterSelectableModes selectMode) {
        selectableMode = selectMode;
        return this;
    }

    public LightBulbAdapterConfiguration<T> withStableIds(boolean useStableIds) {
        this.useStableIds = useStableIds;
        return this;
    }

    public LightBulbAdapterConfiguration<T> withItemsComparator(LightBulbAdapterItemsComparator<T> comparator) {
        this.comparator = comparator;
        return this;
    }

    public LifecycleOwner getLifecycleOwner() {
        return lifecycleOwner;
    }

    public List<T> getItems() {
        return items;
    }

    public LightBulbAdapterSelectableModes getSelectableMode() {
        return selectableMode;
    }

    public boolean isUseStableIds() {
        return useStableIds;
    }

    public LightBulbAdapterItemsComparator<T> getComparator() {
        return comparator;
    }
}
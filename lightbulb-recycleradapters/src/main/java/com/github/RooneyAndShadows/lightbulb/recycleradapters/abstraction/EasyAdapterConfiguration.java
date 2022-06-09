package com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction;

import java.util.ArrayList;

import androidx.lifecycle.LifecycleOwner;

public class EasyAdapterConfiguration<T extends EasyAdapterDataModel> {
    private LifecycleOwner lifecycleOwner = null;
    private final ArrayList<T> items = new ArrayList<>();
    private EasyAdapterSelectableModes selectableMode = EasyAdapterSelectableModes.SELECT_NONE;
    private boolean useStableIds = false;
    private EasyAdapterItemsComparator<T> comparator = null;

    public EasyAdapterConfiguration<T> withLifecycleOwner(LifecycleOwner owner) {
        lifecycleOwner = owner;
        return this;
    }

    public EasyAdapterConfiguration<T> withItems(ArrayList<T> initialCollection) {
        items.clear();
        items.addAll(initialCollection);
        return this;
    }

    public EasyAdapterConfiguration<T> withSelectMode(EasyAdapterSelectableModes selectMode) {
        selectableMode = selectMode;
        return this;
    }

    public EasyAdapterConfiguration<T> withStableIds(boolean useStableIds) {
        this.useStableIds = useStableIds;
        return this;
    }

    public EasyAdapterConfiguration<T> withItemsComparator(EasyAdapterItemsComparator<T> comparator) {
        this.comparator = comparator;
        return this;
    }

    public LifecycleOwner getLifecycleOwner() {
        return lifecycleOwner;
    }

    public ArrayList<T> getItems() {
        return items;
    }

    public EasyAdapterSelectableModes getSelectableMode() {
        return selectableMode;
    }

    public boolean isUseStableIds() {
        return useStableIds;
    }

    public EasyAdapterItemsComparator<T> getComparator() {
        return comparator;
    }
}
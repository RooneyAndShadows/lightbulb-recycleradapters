package com.rands.lightbulb.recycleradapters;

import java.util.List;

import androidx.recyclerview.widget.DiffUtil;

final class EasyAdapterDiffUtilCallback<T extends EasyAdapterDataModel> extends DiffUtil.Callback {
    private final List<T> oldData;
    private final List<T> newData;
    private final EasyAdapterItemsComparator<T> compareCallbacks;

    public EasyAdapterDiffUtilCallback(List<T> oldData, List<T> newData, EasyAdapterItemsComparator<T> compareCallbacks) {
        this.oldData = oldData;
        this.newData = newData;
        this.compareCallbacks = compareCallbacks;
    }

    @Override
    public final int getOldListSize() {
        return oldData.size();
    }

    @Override
    public final int getNewListSize() {
        return newData.size();
    }

    @Override
    public final boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return compareCallbacks.compareItems(oldData.get(oldItemPosition), newData.get(newItemPosition));
    }

    @Override
    public final boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        T oldItem = oldData.get(oldItemPosition);
        T newItem = newData.get(newItemPosition);
        return oldItem.isSelected() == newItem.isSelected() && compareCallbacks.compareItemsContent(oldItem, newItem);
    }
}

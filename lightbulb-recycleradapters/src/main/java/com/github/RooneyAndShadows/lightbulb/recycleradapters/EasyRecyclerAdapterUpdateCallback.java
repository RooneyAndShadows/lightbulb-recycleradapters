package com.github.rooneyandshadows.lightbulb.recycleradapters;

import androidx.recyclerview.widget.ListUpdateCallback;
import androidx.recyclerview.widget.RecyclerView;

class EasyRecyclerAdapterUpdateCallback implements ListUpdateCallback {
        private final RecyclerView.Adapter adapter;
        private final int offset;

        public EasyRecyclerAdapterUpdateCallback(RecyclerView.Adapter adapter, int offset) {
            this.adapter = adapter;
            this.offset = offset;
        }

        @Override
        public void onInserted(int position, int count) {
            adapter.notifyItemRangeInserted(position + offset, count);
        }

        @Override
        public void onRemoved(int position, int count) {
            adapter.notifyItemRangeRemoved(position + offset, count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            adapter.notifyItemMoved(fromPosition + offset, toPosition);
        }

        @Override
        public void onChanged(int position, int count, Object payload) {
            adapter.notifyItemRangeChanged(position + offset, count, payload);
        }
    }
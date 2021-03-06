package com.github.rooneyandshadows.lightbulb.recycleradapters.implementation;

import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

@SuppressWarnings({"rawtypes", "unused", "unchecked"})
public class HeaderViewRecyclerAdapter extends RecyclerView.Adapter {
    private static final int BASE_HEADER_VIEW_TYPE = -1 << 10;
    private static final int BASE_FOOTER_VIEW_TYPE = -1 << 11;
    private final List<FixedViewInfo> mHeaderViewInfoList;
    private final List<FixedViewInfo> mFooterViewInfoList;
    private RecyclerView.Adapter dataAdapter;
    private final RecyclerView recyclerView;
    private boolean mIsStaggeredGrid;

    public HeaderViewRecyclerAdapter(RecyclerView recyclerView) {
        this(recyclerView, null, null);
    }

    public HeaderViewRecyclerAdapter(RecyclerView recyclerView, List<FixedViewInfo> headerViewInfoList, List<FixedViewInfo> footerViewInfoList) {
        this.recyclerView = recyclerView;
        if (headerViewInfoList == null) mHeaderViewInfoList = new ArrayList<>();
        else mHeaderViewInfoList = headerViewInfoList;
        if (footerViewInfoList == null) mFooterViewInfoList = new ArrayList<>();
        else mFooterViewInfoList = footerViewInfoList;
    }

    public void setDataAdapter(RecyclerView.Adapter dataAdapter) {
        this.dataAdapter = dataAdapter;
        setHasStableIds(dataAdapter.hasStableIds());
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (isHeaderViewType(viewType)) {
            int headerIndex = Math.abs(viewType - BASE_HEADER_VIEW_TYPE);
            FixedViewInfo info = mHeaderViewInfoList.get(headerIndex);
            return createHeaderFooterViewHolder(info.view, info.viewListeners);
        } else if (isFooterViewType(viewType)) {
            int footerIndex = Math.abs(viewType - BASE_FOOTER_VIEW_TYPE);
            FixedViewInfo info = mFooterViewInfoList.get(footerIndex);
            return createHeaderFooterViewHolder(info.view, info.viewListeners);
        } else {
            return dataAdapter.onCreateViewHolder(parent, viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position < getHeadersCount() || position >= getHeadersCount() + dataAdapter.getItemCount()) {
            return;
        }
        dataAdapter.onBindViewHolder(holder, position - getHeadersCount());
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder viewHolder) {
        int vtype = viewHolder.getItemViewType();
        if (isHeaderPosition(vtype) || isFooterPosition(vtype)) {
            super.onViewRecycled(viewHolder);
        } else {
            dataAdapter.onViewRecycled(viewHolder);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isHeaderPosition(position)) {
            return mHeaderViewInfoList.get(position).viewType;
        } else if (isFooterPosition(position)) {
            return mFooterViewInfoList.get(position - dataAdapter.getItemCount() - getHeadersCount()).viewType;
        } else {
            return dataAdapter.getItemViewType(position - getHeadersCount());
        }
    }

    @Override
    public int getItemCount() {
        return getFootersCount() + getHeadersCount() + dataAdapter.getItemCount();
    }

    @Override
    public long getItemId(int position) {
        if (isFooterPosition(position) || isHeaderPosition(position))
            return position;
        return dataAdapter.getItemId(position);
    }

    @Override
    public void registerAdapterDataObserver(@NonNull RecyclerView.AdapterDataObserver observer) {
        super.registerAdapterDataObserver(observer);
        dataAdapter.registerAdapterDataObserver(observer);
    }

    @Override
    public void unregisterAdapterDataObserver(@NonNull RecyclerView.AdapterDataObserver observer) {
        super.unregisterAdapterDataObserver(observer);
        dataAdapter.unregisterAdapterDataObserver(observer);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        dataAdapter.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        dataAdapter.onDetachedFromRecyclerView(recyclerView);
    }

    public static class FixedViewInfo {
        public final int viewType;
        public final View view;
        public int localPosition;
        public ViewListeners viewListeners;

        public FixedViewInfo(int viewType, View view, int localPosition, ViewListeners viewListeners) {
            this.viewType = viewType;
            this.view = view;
            this.localPosition = localPosition;
            this.viewListeners = viewListeners;
        }
    }

    public interface ViewListeners {
        void onCreated(View view);
    }

    public int getHeadersCount() {
        return mHeaderViewInfoList.size();
    }

    public int getFootersCount() {
        return mFooterViewInfoList.size();
    }

    public boolean isEmpty() {
        return dataAdapter == null || dataAdapter.getItemCount() == 0;
    }


    public void removeAllHeaderView() {
        if (!mHeaderViewInfoList.isEmpty()) {
            List<Integer> positionsToRemove = findHeaderPositions();
            mHeaderViewInfoList.clear();
            if (positionsToRemove.size() > 0)
                notifyItemRangeRemoved(positionsToRemove.get(0), positionsToRemove.size());
        }
    }

    public void removeAllFooterView() {
        if (!mFooterViewInfoList.isEmpty()) {
            List<Integer> positionsToRemove = findFooterPositions();
            mFooterViewInfoList.clear();
            if (positionsToRemove.size() > 0)
                notifyItemRangeRemoved(positionsToRemove.get(0), positionsToRemove.size());
        }
    }

    public void addHeaderView(View view) {
        addHeaderView(view, null);
    }

    public void addHeaderView(View view, ViewListeners viewListeners) {
        if (null == view) throw new IllegalArgumentException("the view to add must not be null");
        int positionToAdd = getHeadersCount();
        final FixedViewInfo info = new FixedViewInfo(BASE_HEADER_VIEW_TYPE + mHeaderViewInfoList.size(), view, positionToAdd, viewListeners);
        mHeaderViewInfoList.add(info);
        refreshHeaderPositions();
        notifyItemInserted(positionToAdd);
    }

    public boolean removeHeaderView(View v) {
        for (int i = 0; i < mHeaderViewInfoList.size(); i++) {
            FixedViewInfo info = mHeaderViewInfoList.get(i);
            if (info.view == v) {
                int positionToRemove = info.localPosition;
                mHeaderViewInfoList.remove(i);
                refreshHeaderPositions();
                if (positionToRemove != -1)
                    notifyItemRemoved(positionToRemove);
                return true;
            }
        }
        return false;
    }

    public void addFooterView(View view) {
        addFooterView(view, null);
    }

    public void addFooterView(View view, ViewListeners viewListeners) {
        if (null == view) throw new IllegalArgumentException("the view to add must not be null!");
        int positionToAdd = getHeadersCount() + dataAdapter.getItemCount() + getFootersCount();
        int localPosition = getFootersCount();
        final FixedViewInfo info = new FixedViewInfo(BASE_FOOTER_VIEW_TYPE + mFooterViewInfoList.size(), view, localPosition, viewListeners);
        mFooterViewInfoList.add(info);
        refreshFooterPositions();
        notifyItemInserted(positionToAdd);
    }

    public boolean removeFooterView(View v) {
        for (int i = 0; i < mFooterViewInfoList.size(); i++) {
            FixedViewInfo info = mFooterViewInfoList.get(i);
            if (info.view == v) {
                int positionToRemove = getHeadersCount() + dataAdapter.getItemCount() + info.localPosition;
                mFooterViewInfoList.remove(i);
                refreshFooterPositions();
                if (positionToRemove != -1)
                    notifyItemRemoved(positionToRemove);
                return true;
            }
        }
        return false;
    }

    public boolean containsFooterView(View v) {
        for (int i = 0; i < mFooterViewInfoList.size(); i++) {
            FixedViewInfo info = mFooterViewInfoList.get(i);
            if (info.view == v) return true;
        }
        return false;
    }

    public boolean containsHeaderView(View v) {
        for (int i = 0; i < mHeaderViewInfoList.size(); i++) {
            FixedViewInfo info = mHeaderViewInfoList.get(i);
            if (info.view == v) return true;
        }
        return false;
    }

    public void setHeaderVisibility(boolean shouldShow) {
        for (FixedViewInfo fixedViewInfo : mHeaderViewInfoList)
            fixedViewInfo.view.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
        List<Integer> positionsToChange = findHeaderPositions();
        if (positionsToChange.size() > 0)
            notifyItemRangeChanged(positionsToChange.get(0), positionsToChange.size());
    }

    public void setFooterVisibility(boolean shouldShow) {
        for (FixedViewInfo fixedViewInfo : mFooterViewInfoList)
            fixedViewInfo.view.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
        List<Integer> positionsToChange = findFooterPositions();
        if (positionsToChange.size() > 0)
            notifyItemRangeChanged(positionsToChange.get(0), positionsToChange.size());
    }

    private void refreshFooterPositions() {
        for (int position = 0; position < mFooterViewInfoList.size(); position++) {
            FixedViewInfo info = mFooterViewInfoList.get(position);
            info.localPosition = position;
        }
    }

    private void refreshHeaderPositions() {
        for (int position = 0; position < mHeaderViewInfoList.size(); position++) {
            FixedViewInfo info = mHeaderViewInfoList.get(position);
            info.localPosition = position;
        }
    }

    private boolean isHeaderViewType(int viewType) {
        return viewType >= BASE_HEADER_VIEW_TYPE
                && viewType < (BASE_HEADER_VIEW_TYPE + mHeaderViewInfoList.size());
    }

    private boolean isFooterViewType(int viewType) {
        return viewType >= BASE_FOOTER_VIEW_TYPE
                && viewType < (BASE_FOOTER_VIEW_TYPE + mFooterViewInfoList.size());
    }

    private boolean isHeaderPosition(int position) {
        return position < mHeaderViewInfoList.size();
    }

    private boolean isFooterPosition(int position) {
        return position >= mHeaderViewInfoList.size() + dataAdapter.getItemCount();
    }

    private int findAdapterPositionByView(View view) {
        RecyclerView.ViewHolder holder = recyclerView.findContainingViewHolder(view);
        return holder == null ? -1 : holder.getAbsoluteAdapterPosition();
    }

    private List<Integer> findHeaderPositions() {
        List<Integer> headerPositions = new ArrayList<>();
        for (int i = 0; i < getItemCount(); i++) {
            if (isHeaderPosition(i))
                headerPositions.add(i);
        }
        return headerPositions;
    }

    private List<Integer> findFooterPositions() {
        List<Integer> footerPositions = new ArrayList<>();
        for (int i = 0; i < getItemCount(); i++) {
            if (isFooterPosition(i))
                footerPositions.add(i);
        }
        return footerPositions;
    }

    private RecyclerView.ViewHolder createHeaderFooterViewHolder(View view, ViewListeners listeners) {
        if (mIsStaggeredGrid) {
            StaggeredGridLayoutManager.LayoutParams layoutParams = new StaggeredGridLayoutManager.LayoutParams(
                    StaggeredGridLayoutManager.LayoutParams.MATCH_PARENT, StaggeredGridLayoutManager.LayoutParams.WRAP_CONTENT);
            layoutParams.setFullSpan(true);
            view.setLayoutParams(layoutParams);
        } else {
            RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT);
            view.setLayoutParams(layoutParams);
        }
        if (listeners != null)
            listeners.onCreated(view);
        return new RecyclerView.ViewHolder(view) {
        };
    }

    public void adjustSpanSize(RecyclerView recycler) {
        if (recycler.getLayoutManager() instanceof GridLayoutManager) {
            final GridLayoutManager layoutManager = (GridLayoutManager) recycler.getLayoutManager();
            layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {

                @Override
                public int getSpanSize(int position) {
                    boolean isHeaderOrFooter =
                            isHeaderPosition(position) || isFooterPosition(position);
                    return isHeaderOrFooter ? layoutManager.getSpanCount() : 1;
                }

            });
        }

        if (recycler.getLayoutManager() instanceof StaggeredGridLayoutManager) {
            this.mIsStaggeredGrid = true;
        }
    }

    public RecyclerView.Adapter getAdapter() {
        return dataAdapter;
    }
}
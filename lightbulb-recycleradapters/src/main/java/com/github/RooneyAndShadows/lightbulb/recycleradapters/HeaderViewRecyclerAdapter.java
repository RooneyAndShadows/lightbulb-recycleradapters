package com.github.rooneyandshadows.lightbulb.recycleradapters;

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
            View headerView = mHeaderViewInfoList.get(headerIndex).view;
            return createHeaderFooterViewHolder(headerView);
        } else if (isFooterViewType(viewType)) {
            int footerIndex = Math.abs(viewType - BASE_FOOTER_VIEW_TYPE);
            View footerView = mFooterViewInfoList.get(footerIndex).view;
            return createHeaderFooterViewHolder(footerView);
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
        public int viewType;
        public View view;
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

    public boolean removeHeaderView(View v) {
        for (int i = 0; i < mHeaderViewInfoList.size(); i++) {
            FixedViewInfo info = mHeaderViewInfoList.get(i);
            if (info.view == v) {
                int positionToRemove = findAdapterPositionByView(v);
                mHeaderViewInfoList.remove(i);
                if (positionToRemove != -1)
                    notifyItemRemoved(positionToRemove);
                return true;
            }
        }
        return false;
    }

    public boolean removeFooterView(View v) {
        for (int i = 0; i < mFooterViewInfoList.size(); i++) {
            FixedViewInfo info = mFooterViewInfoList.get(i);
            if (info.view == v) {
                int positionToRemove = findAdapterPositionByView(v);
                mFooterViewInfoList.remove(i);
                if (positionToRemove != -1)
                    notifyItemRemoved(positionToRemove);
                return true;
            }
        }
        return false;
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
        if (null == view) {
            throw new IllegalArgumentException("the view to add must not be null");
        }
        final FixedViewInfo info = new FixedViewInfo();
        info.view = view;
        info.viewType = BASE_HEADER_VIEW_TYPE + mHeaderViewInfoList.size();
        mHeaderViewInfoList.add(info);
        notifyItemInserted(getHeadersCount());
    }

    public void addFooterView(View view) {
        if (null == view) {
            throw new IllegalArgumentException("the view to add must not be null!");
        }
        final FixedViewInfo info = new FixedViewInfo();
        info.view = view;
        info.viewType = BASE_FOOTER_VIEW_TYPE + mFooterViewInfoList.size();
        mFooterViewInfoList.add(info);
        notifyItemInserted(getHeadersCount() + dataAdapter.getItemCount() + getFootersCount());
    }

    public boolean containsFooterView(View v) {
        for (int i = 0; i < mFooterViewInfoList.size(); i++) {
            FixedViewInfo info = mFooterViewInfoList.get(i);
            if (info.view == v) {
                return true;
            }
        }
        return false;
    }

    public boolean containsHeaderView(View v) {
        for (int i = 0; i < mHeaderViewInfoList.size(); i++) {
            FixedViewInfo info = mHeaderViewInfoList.get(i);
            if (info.view == v) {
                return true;
            }
        }
        return false;
    }

    public void setHeaderVisibility(boolean shouldShow) {
        for (FixedViewInfo fixedViewInfo : mHeaderViewInfoList) {
            fixedViewInfo.view.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
        }
        List<Integer> positionsToChange = findHeaderPositions();
        if (positionsToChange.size() > 0)
            notifyItemRangeChanged(positionsToChange.get(0), positionsToChange.size());
    }

    public void setFooterVisibility(boolean shouldShow) {
        for (FixedViewInfo fixedViewInfo : mFooterViewInfoList) {
            fixedViewInfo.view.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
        }
        List<Integer> positionsToChange = findFooterPositions();
        if (positionsToChange.size() > 0)
            notifyItemRangeChanged(positionsToChange.get(0), positionsToChange.size());
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

    private RecyclerView.ViewHolder createHeaderFooterViewHolder(View view) {
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
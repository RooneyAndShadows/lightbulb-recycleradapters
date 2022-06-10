package com.github.rooneyandshadows.lightbulb.recycleradapters.implementation;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.github.rooneyandshadows.lightbulb.recycleradapters.R;
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterConfiguration;
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterDataModel;
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterSelectableModes;
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyRecyclerAdapter;
import com.github.rooneyandshadows.lightbulb.selectableview.RadioButtonView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

@SuppressWarnings({"unchecked"})
public class RadioButtonSelectableAdapter<ItemType extends EasyAdapterDataModel> extends EasyRecyclerAdapter<ItemType> {

    public RadioButtonSelectableAdapter() {
        super(new EasyAdapterConfiguration<ItemType>().withSelectMode(EasyAdapterSelectableModes.SELECT_SINGLE));
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RadioButtonView v = (RadioButtonView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_radio_button, parent, false);
        int[] padding = getItemPadding();
        if (padding != null && padding.length == 4)
            v.setPadding(padding[0], padding[1], padding[2], padding[3]);
        return new RadioButtonViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RadioButtonViewHolder vHolder = (RadioButtonViewHolder) holder;
        vHolder.bindItem();
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        RadioButtonViewHolder vh = (RadioButtonViewHolder) holder;
        vh.recycle();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public String getItemName(ItemType item) {
        return item.getItemName();
    }

    protected Drawable getItemIcon(ItemType item) {
        return null;
    }

    protected Drawable getItemIconBackground(ItemType item) {
        return null;
    }

    /**
     * Defines padding for the item view.
     *
     * @return int[] {left,top,right,bottom}
     */
    protected int[] getItemPadding() {
        return null;
    }

    public class RadioButtonViewHolder extends RecyclerView.ViewHolder {
        protected RadioButtonView selectableView;
        protected ItemType item;

        public RadioButtonViewHolder(RadioButtonView categoryItemBinding) {
            super(categoryItemBinding);
            this.selectableView = (RadioButtonView) itemView;
            selectableView.setOnCheckedListener((view, isChecked) -> selectableView.post(() -> selectItem(item, isChecked)));
        }

        public void bindItem() {
            this.item = getItem(getBindingAdapterPosition());
            boolean isSelectedInAdapter = isItemSelected(item);
            if (selectableView.isChecked() != isSelectedInAdapter)
                selectableView.setChecked(isSelectedInAdapter);
            selectableView.setText(getItemName(item));
            selectableView.setIcon(getItemIcon(item), getItemIconBackground(item));
        }

        private void recycle() {
            selectableView.setIcon(null, null);
        }
    }
}
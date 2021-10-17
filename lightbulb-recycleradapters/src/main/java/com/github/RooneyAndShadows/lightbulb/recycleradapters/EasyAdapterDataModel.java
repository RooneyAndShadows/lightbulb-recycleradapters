package com.github.RooneyAndShadows.lightbulb.recycleradapters;

import android.os.Parcel;
import android.os.Parcelable;

public abstract class EasyAdapterDataModel implements Parcelable {
    private boolean selected;

    public abstract String getItemName();

    public EasyAdapterDataModel(boolean selected) {
        this.selected = selected;
    }

    protected EasyAdapterDataModel(Parcel in) {
        selected = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (selected ? 1 : 0));
    }

    public boolean isSelected() {
        return selected;
    }

    void setSelected(boolean selected) {
        this.selected = selected;
    }
}
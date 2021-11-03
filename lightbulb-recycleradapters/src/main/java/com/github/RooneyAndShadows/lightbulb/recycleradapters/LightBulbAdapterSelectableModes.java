package com.github.rooneyandshadows.lightbulb.recycleradapters;

import android.util.SparseArray;

public enum LightBulbAdapterSelectableModes {
    SELECT_NONE(1),
    SELECT_SINGLE(2),
    SELECT_MULTIPLE(3);

    private final int value;
    private static final SparseArray<LightBulbAdapterSelectableModes> values = new SparseArray<>();

    LightBulbAdapterSelectableModes(int value) {
        this.value = value;
    }

    static {
        for (LightBulbAdapterSelectableModes selectMode : LightBulbAdapterSelectableModes.values()) {
            values.put(selectMode.value, selectMode);
        }
    }

    public static LightBulbAdapterSelectableModes valueOf(int selectMode) {
        return values.get(selectMode);
    }

    public int getValue() {
        return value;
    }
}
package com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction;

import android.util.SparseArray;

public enum EasyAdapterSelectableModes {
    SELECT_NONE(1),
    SELECT_SINGLE(2),
    SELECT_MULTIPLE(3);

    private final int value;
    private static final SparseArray<EasyAdapterSelectableModes> values = new SparseArray<>();

    EasyAdapterSelectableModes(int value) {
        this.value = value;
    }

    static {
        for (EasyAdapterSelectableModes selectMode : EasyAdapterSelectableModes.values()) {
            values.put(selectMode.value, selectMode);
        }
    }

    public static EasyAdapterSelectableModes valueOf(int selectMode) {
        return values.get(selectMode);
    }

    public int getValue() {
        return value;
    }
}
package com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.data

import android.os.Parcel
import android.os.Parcelable

abstract class EasyAdapterDataModel : Parcelable {
    abstract val itemName: String

    override fun describeContents(): Int {
        return 0;
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
    }
}
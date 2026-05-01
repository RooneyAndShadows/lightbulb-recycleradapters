package com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.data

import android.os.Parcel
import android.os.Parcelable

abstract class EasyAdapterDataModel() : Parcelable {
    abstract val itemName: String

    override fun writeToParcel(p0: Parcel, p1: Int) {

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<EasyAdapterDataModel> {
        override fun createFromParcel(parcel: Parcel): EasyAdapterDataModel? {
            return null
        }

        override fun newArray(size: Int): Array<EasyAdapterDataModel?> {
            return arrayOfNulls(size)
        }
    }
}
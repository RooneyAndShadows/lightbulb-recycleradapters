package com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction

import android.os.Parcel
import android.os.Parcelable
import com.github.rooneyandshadows.lightbulb.commons.utils.ParcelUtils

internal class SelectableItem<ItemType : EasyAdapterDataModel> : Parcelable {
    var isSelected: Boolean
    var item: ItemType

    constructor(isSelected: Boolean, item: ItemType) {
        this.isSelected = isSelected
        this.item = item
    }

    @Suppress("UNCHECKED_CAST")
    constructor(parcel: Parcel) {
        val className: String = parcel.readString()!!
        isSelected = parcel.readByte() != 0.toByte()
        val clazz = Class.forName(className) as Class<ItemType>
        item = ParcelUtils.readParcelable(parcel, clazz) as ItemType
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(item.javaClass.name)
        parcel.writeByte(if (isSelected) 1 else 0)
        parcel.writeParcelable(item, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SelectableItem<EasyAdapterDataModel>> {
        override fun createFromParcel(parcel: Parcel): SelectableItem<EasyAdapterDataModel> {
            return SelectableItem(parcel)
        }

        override fun newArray(size: Int): Array<SelectableItem<EasyAdapterDataModel>?> {
            return arrayOfNulls(size)
        }
    }
}
package com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.data

import android.os.Parcelable

abstract class EasyAdapterDataModel : Parcelable {
    abstract val itemName: String
}
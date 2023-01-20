package com.github.rooneyandshadowss.lightbulb.recycleradapters.abstraction

import android.os.Parcelable

abstract class EasyAdapterDataModel : Parcelable {
    abstract val itemName: String
}
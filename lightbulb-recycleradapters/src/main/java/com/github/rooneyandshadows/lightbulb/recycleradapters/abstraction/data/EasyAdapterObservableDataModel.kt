package com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.data

import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.databinding.PropertyChangeRegistry
import com.github.rooneyandshadows.lightbulb.commons.databinding.ObservableWithNotify

@Suppress("unused")
abstract class EasyAdapterObservableDataModel : EasyAdapterDataModel(), ObservableWithNotify {
    private val callbacks = PropertyChangeRegistry()

    override fun addOnPropertyChangedCallback(callback: OnPropertyChangedCallback) {
        callbacks.add(callback)
    }

    override fun removeOnPropertyChangedCallback(callback: OnPropertyChangedCallback) {
        callbacks.remove(callback)
    }

    override fun clearObservableCallbacks() {
        callbacks.clear()
    }

    override fun notifyChange() {
        callbacks.notifyCallbacks(this, 0, null)
    }

    override fun notifyPropertyChanged(fieldId: Int) {
        callbacks.notifyCallbacks(this, fieldId, null)
    }
}
package com.github.rooneyandshadows.lightbulb.recycleradapters;

import android.os.Parcel;


import androidx.databinding.Bindable;
import androidx.databinding.Observable;
import androidx.databinding.PropertyChangeRegistry;


public abstract class LightBulbAdapterObservableDataModel extends LightBulbAdapterDataModel implements Observable {
    private final PropertyChangeRegistry callbacks = new PropertyChangeRegistry();

    public LightBulbAdapterObservableDataModel(boolean selected) {
        super(selected);
    }

    protected LightBulbAdapterObservableDataModel(Parcel in) {
        super(in);
    }

    @Override
    @Bindable
    public boolean isSelected() {
        return super.isSelected();
    }

    @Override
    void setSelected(boolean selected) {
        super.setSelected(selected);
        notifyPropertyChanged(BR.selected);
    }

    public PropertyChangeRegistry getCallbacks() {
        return callbacks;
    }

    @Override
    public void addOnPropertyChangedCallback(
            Observable.OnPropertyChangedCallback callback) {
        callbacks.add(callback);
    }

    @Override
    public void removeOnPropertyChangedCallback(
            Observable.OnPropertyChangedCallback callback) {
        callbacks.remove(callback);
    }

    public void clearObservableCallbacks() {
        callbacks.clear();
    }

    /**
     * Notifies observers that all properties of this instance have changed.
     */
    public void notifyChange() {
        callbacks.notifyCallbacks(this, 0, null);
    }

    /**
     * Notifies observers that a specific property has changed. The getter for the
     * property that changes should be marked with the @Bindable annotation to
     * generate a field in the BR class to be used as the fieldId parameter.
     *
     * @param fieldId The generated BR id for the Bindable field.
     */
    public void notifyPropertyChanged(int fieldId) {
        callbacks.notifyCallbacks(this, fieldId, null);
    }
}
package com.github.rooneyandshadows.lightbulb.recycleradapters.implementation

import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterDataModel
import kotlin.jvm.JvmOverloads
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterConfiguration
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterSelectableModes
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterItemsComparator
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.callbacks.EasyAdapterSelectionChangedListener
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.callbacks.EasyAdapterCollectionChangedListener
import com.github.rooneyandshadows.lightbulb.recycleradapters.implementation.HeaderViewRecyclerAdapter
import android.os.Bundle
import android.annotation.SuppressLint
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterObservableDataModel
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterDiffUtilCallback
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyRecyclerAdapterUpdateCallback
import android.os.Parcelable
import android.os.Parcel
import android.util.SparseArray
import androidx.databinding.PropertyChangeRegistry
import androidx.databinding.Observable.OnPropertyChangedCallback
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyRecyclerAdapter
import android.view.ViewGroup
import com.github.rooneyandshadows.lightbulb.selectableview.CheckBoxView
import android.view.LayoutInflater
import com.github.rooneyandshadows.lightbulb.recycleradapters.R
import com.github.rooneyandshadows.lightbulb.recycleradapters.implementation.CheckBoxSelectableAdapter.CheckBoxViewHolder
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.RecyclerView
import com.github.rooneyandshadows.lightbulb.recycleradapters.implementation.HeaderViewRecyclerAdapter.ViewListeners
import com.github.rooneyandshadows.lightbulb.selectableview.RadioButtonView
import com.github.rooneyandshadows.lightbulb.recycleradapters.implementation.RadioButtonSelectableAdapter.RadioButtonViewHolder

class RadioButtonSelectableAdapter<ItemType : EasyAdapterDataModel?> :
    EasyRecyclerAdapter<ItemType?>(
        EasyAdapterConfiguration<ItemType>().withSelectMode(EasyAdapterSelectableModes.SELECT_SINGLE)
    ) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_radio_button, parent, false) as RadioButtonView
        val padding = itemPadding
        if (padding != null && padding.size == 4) v.setPadding(
            padding[0],
            padding[1],
            padding[2],
            padding[3]
        )
        return RadioButtonViewHolder(v)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val vHolder: RadioButtonViewHolder = holder as RadioButtonViewHolder
        vHolder.bindItem()
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        val vh: RadioButtonViewHolder = holder as RadioButtonViewHolder
        vh.recycle()
    }

    override fun getItemCount(): Int {
        return items!!.size
    }

    override fun getItemName(item: ItemType?): String? {
        return item.getItemName()
    }

    protected fun getItemIcon(item: ItemType?): Drawable? {
        return null
    }

    protected fun getItemIconBackground(item: ItemType?): Drawable? {
        return null
    }

    /**
     * Defines padding for the item view.
     *
     * @return int[] {left,top,right,bottom}
     */
    protected val itemPadding: IntArray?
        protected get() = null

    inner class RadioButtonViewHolder(categoryItemBinding: RadioButtonView?) :
        RecyclerView.ViewHolder(
            categoryItemBinding!!
        ) {
        protected var selectableView: RadioButtonView
        protected var item: ItemType? = null
        fun bindItem() {
            item = getItem(bindingAdapterPosition)
            val isSelectedInAdapter = isItemSelected(item)
            if (selectableView.isChecked != isSelectedInAdapter) selectableView.isChecked =
                isSelectedInAdapter
            selectableView.text = getItemName(item)
            selectableView.setIcon(getItemIcon(item), getItemIconBackground(item))
        }

        fun recycle() {
            selectableView.setIcon(null, null)
        }

        init {
            selectableView = itemView as RadioButtonView
            selectableView.setOnCheckedListener { view: RadioButtonView?, isChecked: Boolean ->
                selectableView.post {
                    selectItem(
                        item,
                        isChecked
                    )
                }
            }
        }
    }
}
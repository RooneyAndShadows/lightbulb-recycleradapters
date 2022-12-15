package com.github.rooneyandshadows.lightbulb.recycleradapters.implementation

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.rooneyandshadows.lightbulb.recycleradapters.R
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterDataModel
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterSelectableModes
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyRecyclerAdapter
import com.github.rooneyandshadows.lightbulb.selectableview.RadioButtonView

@Suppress("UNUSED_PARAMETER", "unused", "MemberVisibilityCanBePrivate")
class RadioButtonSelectableAdapter<ItemType : EasyAdapterDataModel> :
    EasyRecyclerAdapter<ItemType>(EasyAdapterSelectableModes.SELECT_SINGLE) {
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

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val vHolder: RadioButtonViewHolder = holder as RadioButtonSelectableAdapter<ItemType>.RadioButtonViewHolder
        vHolder.bindItem()
    }

    @Suppress("UNCHECKED_CAST")
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        val vh: RadioButtonViewHolder = holder as RadioButtonSelectableAdapter<ItemType>.RadioButtonViewHolder
        vh.recycle()
    }

    override fun getItemCount(): Int {
        return getItems().size
    }

    override fun getItemName(item: ItemType): String {
        return item.itemName
    }

    protected fun getItemIcon(item: ItemType): Drawable? {
        return null
    }

    protected fun getItemIconBackground(item: ItemType): Drawable? {
        return null
    }

    /**
     * Defines padding for the item view.
     *
     * @return int[] {left,top,right,bottom}
     */
    protected val itemPadding: IntArray?
        get() = null

    inner class RadioButtonViewHolder(categoryItemBinding: RadioButtonView?) :
        RecyclerView.ViewHolder(
            categoryItemBinding!!
        ) {
        private var selectableView: RadioButtonView = itemView as RadioButtonView
        private var item: ItemType? = null
        fun bindItem() {
            item = getItem(bindingAdapterPosition) ?: return
            val isSelectedInAdapter = isItemSelected(item!!)
            if (selectableView.isChecked != isSelectedInAdapter) selectableView.isChecked =
                isSelectedInAdapter
            selectableView.text = getItemName(item!!)
            selectableView.setIcon(getItemIcon(item!!), getItemIconBackground(item!!))
        }

        fun recycle() {
            selectableView.setIcon(null, null)
        }

        init {
            selectableView.setOnCheckedListener { _: RadioButtonView?, isChecked: Boolean ->
                selectableView.post {
                    selectItem(
                        item!!,
                        isChecked
                    )
                }
            }
        }
    }
}
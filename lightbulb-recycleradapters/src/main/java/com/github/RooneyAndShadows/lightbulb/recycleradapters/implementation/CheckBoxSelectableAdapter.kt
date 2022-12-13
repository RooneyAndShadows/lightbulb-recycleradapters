package com.github.rooneyandshadows.lightbulb.recycleradapters.implementation

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.rooneyandshadows.lightbulb.recycleradapters.R
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterDataModel
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterSelectableModes.*
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyRecyclerAdapter
import com.github.rooneyandshadows.lightbulb.selectableview.CheckBoxView

@Suppress("UNUSED_PARAMETER", "unused", "MemberVisibilityCanBePrivate")
abstract class CheckBoxSelectableAdapter<ItemType : EasyAdapterDataModel> : EasyRecyclerAdapter<ItemType>(SELECT_MULTIPLE) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_checkbox_button, parent, false) as CheckBoxView
        val padding = itemPadding
        if (padding != null && padding.size == 4) v.setPadding(
            padding[0],
            padding[1],
            padding[2],
            padding[3]
        )
        return CheckBoxViewHolder(v, this)
    }

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val vHolder: CheckBoxViewHolder = holder as CheckBoxSelectableAdapter<ItemType>.CheckBoxViewHolder
        vHolder.bindItem()
    }

    @Suppress("UNCHECKED_CAST")
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        val vh: CheckBoxViewHolder = holder as CheckBoxSelectableAdapter<ItemType>.CheckBoxViewHolder
        vh.recycle()
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemName(item: ItemType): String? {
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

    inner class CheckBoxViewHolder internal constructor(
        categoryItemBinding: CheckBoxView?,
        adapter: EasyRecyclerAdapter<ItemType>
    ) : RecyclerView.ViewHolder(
        categoryItemBinding!!
    ) {
        private var selectableView: CheckBoxView = itemView as CheckBoxView
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
            selectableView.setOnCheckedListener { _: CheckBoxView?, isChecked: Boolean ->
                selectableView.post {
                    adapter.selectItemAt(
                        bindingAdapterPosition, isChecked
                    )
                }
            }
        }
    }
}
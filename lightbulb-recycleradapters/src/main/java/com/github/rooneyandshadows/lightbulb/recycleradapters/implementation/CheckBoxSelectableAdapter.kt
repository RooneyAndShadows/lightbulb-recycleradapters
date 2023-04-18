package com.github.rooneyandshadows.lightbulb.recycleradapters.implementation

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.*
import com.github.rooneyandshadows.lightbulb.recycleradapters.R
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterDataModel
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterSelectableModes.*
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyRecyclerAdapter
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.collection.BasicCollection
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.collection.ExtendedCollection
import com.github.rooneyandshadows.lightbulb.selectableview.CheckBoxView

@Suppress("UNUSED_PARAMETER", "unused", "MemberVisibilityCanBePrivate")
@JvmSuppressWildcards
open class CheckBoxSelectableAdapter<ItemType : EasyAdapterDataModel> : EasyRecyclerAdapter<ExtendedCollection<ItemType>>() {

    override fun createCollection(): ExtendedCollection<ItemType> {
        return ExtendedCollection(this, SELECT_MULTIPLE)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val checkBoxView = LayoutInflater.from(parent.context).inflate(
            R.layout.layout_checkbox_button,
            parent, false
        ) as CheckBoxView
        val padding = getItemPadding(context)
        if (padding != null && padding.size == 4) checkBoxView.setPadding(
            padding[0],
            padding[1],
            padding[2],
            padding[3]
        )
        return CheckBoxViewHolder(checkBoxView)
    }

    @Suppress("UNCHECKED_CAST")
    @Override
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val vHolder: CheckBoxViewHolder = holder as CheckBoxSelectableAdapter<ItemType>.CheckBoxViewHolder
        vHolder.bindItem()
    }

    @Suppress("UNCHECKED_CAST")
    @Override
    override fun onViewRecycled(holder: ViewHolder) {
        val vh: CheckBoxViewHolder = holder as CheckBoxSelectableAdapter<ItemType>.CheckBoxViewHolder
        vh.recycle()
    }

    @Override
    override fun getItemCount(): Int {
        return collection.size()
    }

    protected open fun getItemIcon(context: Context, item: ItemType): Drawable? {
        return null
    }

    protected open fun getItemIconBackground(context: Context, item: ItemType): Drawable? {
        return null
    }

    protected open fun getItemPadding(context: Context): IntArray? {
        return null
    }

    inner class CheckBoxViewHolder internal constructor(checkBoxView: CheckBoxView) : ViewHolder(checkBoxView) {
        private var selectableView: CheckBoxView = itemView as CheckBoxView

        fun bindItem() {
            selectableView.apply {
                val item = collection.getItem(bindingAdapterPosition) ?: return
                val isSelectedInAdapter = collection.isItemSelected(item)
                val itemText = collection.getItemName(item)
                val itemIcon = getItemIcon(context, item)
                val itemIconBackground = getItemIconBackground(context, item)
                if (isChecked != isSelectedInAdapter) isChecked = isSelectedInAdapter
                text = itemText
                setIcon(itemIcon, itemIconBackground)
            }
        }

        fun recycle() {
            selectableView.setIcon(null, null)
        }

        init {
            with(selectableView) {
                setOnCheckedListener { _: CheckBoxView?, isChecked: Boolean ->
                    post {
                        collection.selectItemAt(bindingAdapterPosition, isChecked)
                    }
                }
            }
        }
    }
}
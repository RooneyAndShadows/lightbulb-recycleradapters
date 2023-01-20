package com.github.rooneyandshadowss.lightbulb.recycleradapters.implementation

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.rooneyandshadows.lightbulb.recycleradapters.R
import com.github.rooneyandshadowss.lightbulb.recycleradapters.abstraction.EasyAdapterDataModel
import com.github.rooneyandshadowss.lightbulb.recycleradapters.abstraction.EasyAdapterSelectableModes
import com.github.rooneyandshadowss.lightbulb.recycleradapters.abstraction.EasyRecyclerAdapter
import com.github.rooneyandshadows.lightbulb.selectableview.RadioButtonView

@Suppress("UNUSED_PARAMETER", "unused", "MemberVisibilityCanBePrivate")
open class RadioButtonSelectableAdapter<ItemType : EasyAdapterDataModel> :
    EasyRecyclerAdapter<ItemType>(EasyAdapterSelectableModes.SELECT_SINGLE) {
    @Override
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context = parent.context
        val radioButtonView = LayoutInflater.from(parent.context).inflate(
            R.layout.layout_radio_button,
            parent,
            false
        ) as RadioButtonView
        val padding = getItemPadding(context)
        if (padding != null && padding.size == 4) radioButtonView.setPadding(
            padding[0],
            padding[1],
            padding[2],
            padding[3]
        )
        return RadioButtonViewHolder(radioButtonView)
    }

    @Suppress("UNCHECKED_CAST")
    @Override
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val vHolder: RadioButtonViewHolder = holder as RadioButtonViewHolder
        vHolder.bindItem()
    }

    @Suppress("UNCHECKED_CAST")
    @Override
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        val vh: RadioButtonViewHolder = holder as RadioButtonViewHolder
        vh.recycle()
    }

    @Override
    override fun getItemCount(): Int {
        return getItems().size
    }

    @Override
    override fun getItemName(item: ItemType): String {
        return item.itemName
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

    inner class RadioButtonViewHolder(radioButtonView: RadioButtonView) : RecyclerView.ViewHolder(radioButtonView) {
        private var selectableView: RadioButtonView = itemView as RadioButtonView

        fun bindItem() {
            selectableView.apply {
                val item = getItem(bindingAdapterPosition) ?: return
                val isSelectedInAdapter = isItemSelected(item)
                val itemText = getItemName(item)
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
            selectableView.setOnCheckedListener { _: RadioButtonView?, isChecked: Boolean ->
                selectableView.post {
                    selectItemAt(bindingAdapterPosition, isChecked)
                }
            }
        }
    }
}
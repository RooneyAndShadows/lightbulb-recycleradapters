package com.github.rooneyandshadows.lightbulb.recycleradapters.implementation.collection

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.github.rooneyandshadows.lightbulb.commons.utils.BundleUtils
import com.github.rooneyandshadows.lightbulb.commons.utils.ParcelUtils
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.data.EasyAdapterDataModel
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyRecyclerAdapter
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.collection.EasyRecyclerAdapterCollection
import java.util.function.Predicate
import java.util.stream.Collectors

@Suppress("unused")
class BasicCollection<ItemType : EasyAdapterDataModel> @JvmOverloads constructor(
    adapter: EasyRecyclerAdapter<ItemType>,
    private val itemsComparator: ItemsComparator<ItemType>? = null,
) : EasyRecyclerAdapterCollection<ItemType>(adapter) {
    private val items: MutableList<ItemType> = mutableListOf()

    companion object {
        private const val ADAPTER_ITEMS = "ADAPTER_ITEMS"
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun setInternally(collection: List<ItemType>): Boolean {
        val hasStableIds = adapter.hasStableIds()
        val diffResult = if (itemsComparator != null && !hasStableIds) DiffUtil.calculateDiff(
            DiffUtilCallback(items, collection, itemsComparator), true
        ) else null
        items.clear()
        items.addAll(collection)
        adapter.apply {
            if (diffResult != null) {
                diffResult.dispatchUpdatesTo(AdapterUpdateCallback(this, headersCount))
                recyclerView?.invalidateItemDecorations()
            } else notifyDataSetChanged()
        }
        return true
    }

    override fun addInternally(item: ItemType): Boolean {
        val recyclerView = adapter.recyclerView
        val headersCount = adapter.headersCount
        val needToUpdatePreviousLastItem = items.size > 0 && recyclerView!!.itemDecorationCount > 0
        val previousLastPosition = items.size + headersCount - 1
        items.add(item)
        adapter.apply {
            if (needToUpdatePreviousLastItem) notifyItemChanged(
                previousLastPosition,
                false
            ) // update last item decoration without animation
            notifyItemInserted(items.size + headersCount - 1)
        }
        return true
    }

    override fun addAllInternally(collection: List<ItemType>): Boolean {
        if (collection.isEmpty()) return false
        val recyclerView = adapter.recyclerView
        val headersCount = adapter.headersCount
        val previousLastItem = items.size + headersCount - 1
        val needToUpdatePreviousLastItem = items.size > 0 && recyclerView!!.itemDecorationCount > 0
        val positionStart = items.size + 1
        val newItemsCount = collection.size
        items.addAll(collection)
        adapter.apply {
            if (needToUpdatePreviousLastItem) notifyItemChanged(
                previousLastItem,
                false
            )// update last item decoration without animation
            notifyItemRangeInserted(positionStart + headersCount, newItemsCount + headersCount)
        }
        return true
    }

    override fun removeInternally(targetPosition: Int): Boolean {
        if (!positionExists(targetPosition)) return false
        items.removeAt(targetPosition)
        adapter.apply {
            notifyItemRemoved(targetPosition + headersCount)
        }
        return true
    }

    override fun removeAllInternally(targets: List<ItemType>): Boolean {
        val positionsToRemove = getPositions(targets).sortedDescending()
        if (positionsToRemove.isEmpty()) return false
        items.removeIf { return@removeIf targets.contains(it) }
        adapter.apply {
            for (position in positionsToRemove)
                notifyItemRemoved(position + headersCount)
        }
        return true
    }

    override fun moveInternally(fromPosition: Int, toPosition: Int): Boolean {
        if (!positionExists(fromPosition) || !positionExists(toPosition)) return false
        val movingItem = items[fromPosition]
        items.removeAt(fromPosition)
        items.add(toPosition, movingItem)
        adapter.apply {
            notifyItemMoved(fromPosition + headersCount, toPosition + headersCount)
        }
        return true
    }

    override fun clearInternally(): Boolean {
        if (items.isEmpty()) return false
        val notifyRangeStart = 0
        val notifyRangeEnd = items.size - 1
        items.clear()
        adapter.apply {
            notifyItemRangeRemoved(notifyRangeStart + headersCount, notifyRangeEnd + headersCount)
        }
        return true
    }

    override fun size(): Int {
        return items.size
    }

    override fun isEmpty(): Boolean {
        return items.size <= 0
    }

    override fun positionExists(position: Int): Boolean {
        return items.isNotEmpty() && position >= 0 && position < items.size
    }

    override fun getItems(): List<ItemType> {
        return items.toList()
    }

    override fun getPosition(target: ItemType): Int {
        return items.indexOfFirst { return@indexOfFirst it == target }
    }

    override fun getItem(position: Int): ItemType? {
        return if (positionExists(position)) items[position] else null
    }

    override fun getItems(criteria: Predicate<ItemType>): List<ItemType> {
        return items.stream()
            .filter { return@filter criteria.test(it) }
            .collect(Collectors.toList())
    }

    override fun getItems(positions: IntArray): List<ItemType> {
        return mutableListOf<ItemType>().apply {
            positions.forEach { position ->
                if (!positionExists(position)) return@forEach
                add(items[position])
            }
        }
    }

    override fun getItems(positions: List<Int>): List<ItemType> {
        return mutableListOf<ItemType>().apply {
            positions.forEach { position ->
                if (!positionExists(position)) return@forEach
                add(items[position])
            }
        }
    }

    override fun getPositions(criteria: Predicate<ItemType>): IntArray {
        return getPositions(getItems(criteria))
    }

    override fun getPositions(targets: List<ItemType>): IntArray {
        if (targets.isEmpty()) return IntArray(0)
        return mutableListOf<Int>().let { positions ->
            targets.forEach {
                val targetPos = items.indexOf(it)
                if (targetPos != -1) positions.add(targetPos)
            }
            return@let positions.toIntArray()
        }
    }

    override fun getPositions(targets: Array<ItemType>): IntArray {
        if (targets.isEmpty()) return IntArray(0)
        return mutableListOf<Int>().let { positions ->
            targets.forEach {
                val targetPos = items.indexOf(it)
                if (targetPos != -1) positions.add(targetPos)
            }
            return@let positions.toIntArray()
        }
    }

    override fun getPositionsByItemNames(targetNames: List<String>): IntArray {
        if (targetNames.isEmpty()) return IntArray(0)
        return mutableListOf<Int>().let { positions ->
            items.forEachIndexed { position, item ->
                if (!targetNames.contains(item.itemName)) return@forEachIndexed
                positions.add(position)
            }
            return@let positions.toIntArray()
        }
    }

    override fun getPositionStrings(positions: IntArray): String {
        return items.filterIndexed { index, _ ->
            return@filterIndexed positions.contains(index)
        }.joinToString(", ")
    }

    override fun saveState(): Bundle {
        return Bundle().apply {
            BundleUtils.putParcelableList(ADAPTER_ITEMS, this, wrapToBasic(items))
            onSaveInstanceState(this)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Suppress("UNCHECKED_CAST")
    override fun restoreState(savedState: Bundle) {
        items.apply {
            val clz = Class.forName(BasicItem::class.java.name) as Class<BasicItem<ItemType>>
            BundleUtils.getParcelableList(ADAPTER_ITEMS, savedState, clz)?.apply {
                val rawItems = map { return@map it.item }
                clear()
                addAll(rawItems)
            }
            adapter.notifyDataSetChanged()
        }
        onRestoreInstanceState(savedState)
    }

    private fun wrapToBasic(target: ItemType): BasicItem<ItemType> {
        return BasicItem(target)
    }

    private fun wrapToBasic(target: List<ItemType>): List<BasicItem<ItemType>> {
        return target.map { return@map wrapToBasic(it) }.toMutableList()
    }

    private class DiffUtilCallback<T : EasyAdapterDataModel>(
        private val oldData: List<T>,
        private val newData: List<T>,
        private val compareCallbacks: ItemsComparator<T>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldData.size
        }

        override fun getNewListSize(): Int {
            return newData.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return compareCallbacks.compareItems(oldData[oldItemPosition], newData[newItemPosition])
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldData[oldItemPosition]
            val newItem = newData[newItemPosition]
            return compareCallbacks.compareItemsContent(oldItem, newItem)
        }
    }

    private class AdapterUpdateCallback(
        private val adapter: RecyclerView.Adapter<*>,
        private val offset: Int,
    ) : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            val start = position + offset
            adapter.notifyItemRangeInserted(start, count)
        }

        override fun onRemoved(position: Int, count: Int) {
            val start = position + offset
            adapter.notifyItemRangeRemoved(start, count)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            val from = fromPosition + offset
            adapter.notifyItemMoved(from, toPosition)
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            val start = position + offset
            adapter.notifyItemRangeChanged(start, count, payload)
        }
    }

    private class BasicItem<ItemType : EasyAdapterDataModel> : Parcelable {
        var item: ItemType

        constructor(item: ItemType) {
            this.item = item
        }

        @Suppress("UNCHECKED_CAST")
        constructor(parcel: Parcel) {
            val className: String = parcel.readString()!!
            val clazz = Class.forName(className, false, BasicItem::class.java.classLoader) as Class<ItemType>
            item = ParcelUtils.readParcelable(parcel, clazz) as ItemType
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(item.javaClass.name)
            parcel.writeParcelable(item, flags)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<BasicItem<EasyAdapterDataModel>> {
            override fun createFromParcel(parcel: Parcel): BasicItem<EasyAdapterDataModel> {
                return BasicItem(parcel)
            }

            override fun newArray(size: Int): Array<BasicItem<EasyAdapterDataModel>?> {
                return arrayOfNulls(size)
            }
        }
    }
}
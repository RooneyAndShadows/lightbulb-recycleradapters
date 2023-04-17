package com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.collection

import android.annotation.SuppressLint
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterDataModel
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyRecyclerAdapter
import java.util.function.Predicate
import java.util.stream.Collectors

@JvmSuppressWildcards
class BasicCollection<ItemType : EasyAdapterDataModel> @JvmOverloads constructor(
    private val itemsComparator: EasyRecyclerAdapterItemsComparator<ItemType>? = null,
) : EasyRecyclerAdapterCollection<ItemType>() {
    private var items: MutableList<ItemType> = mutableListOf()

    @SuppressLint("NotifyDataSetChanged")
    override fun setInternally(
        collection: List<ItemType>,
        adapter: EasyRecyclerAdapter<ItemType>,
        recyclerView: RecyclerView?,
    ): Boolean {
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

    override fun addInternally(
        item: ItemType,
        adapter: EasyRecyclerAdapter<ItemType>,
        recyclerView: RecyclerView?,
    ): Boolean {
        val headersCount = adapter.headersCount
        val needToUpdatePreviousLastItem = items.size > 0 && recyclerView!!.itemDecorationCount > 0
        val previousLastPosition = items.size + headersCount - 1
        items.add(item)
        adapter.apply {
            if (needToUpdatePreviousLastItem)
                notifyItemChanged(previousLastPosition, false) // update last item decoration without animation
            notifyItemInserted(items.size + headersCount - 1)
        }
        return true
    }

    override fun addAllInternally(
        collection: List<ItemType>,
        adapter: EasyRecyclerAdapter<ItemType>,
        recyclerView: RecyclerView?,
    ): Boolean {
        if (collection.isEmpty()) return false
        val headersCount = adapter.headersCount
        val needToUpdatePreviousLastItem = items.size > 0 && recyclerView!!.itemDecorationCount > 0
        val positionStart = items.size + 1
        val newItemsCount = collection.size
        items.addAll(collection)
        adapter.apply {
            if (needToUpdatePreviousLastItem) {
                val previousLastItem = items.size + headersCount - 1
                notifyItemChanged(previousLastItem, false)
            }// update last item decoration without animation
            notifyItemRangeInserted(positionStart + headersCount, newItemsCount + headersCount)
        }
        return true
    }

    override fun removeInternally(
        targetPosition: Int,
        adapter: EasyRecyclerAdapter<ItemType>,
        recyclerView: RecyclerView?,
    ): Boolean {
        if (!positionExists(targetPosition)) return false
        items.removeAt(targetPosition)
        adapter.apply {
            notifyItemRemoved(targetPosition + headersCount)
        }
        return true
    }

    override fun removeAllInternally(
        targets: List<ItemType>,
        adapter: EasyRecyclerAdapter<ItemType>,
        recyclerView: RecyclerView?,
    ): Boolean {
        val positionsToRemove = getPositions(targets).sortedDescending()
        if (positionsToRemove.isEmpty()) return false
        items.removeIf { return@removeIf targets.contains(it) }
        adapter.apply {
            for (position in positionsToRemove)
                notifyItemRemoved(position + headersCount)
        }
        return true
    }

    override fun moveInternally(
        fromPosition: Int,
        toPosition: Int,
        adapter: EasyRecyclerAdapter<ItemType>,
        recyclerView: RecyclerView?,
    ): Boolean {
        if (!positionExists(fromPosition) || !positionExists(toPosition)) return false
        val movingItem = items[fromPosition]
        items.removeAt(fromPosition)
        items.add(toPosition, movingItem)
        adapter.apply {
            notifyItemMoved(fromPosition + headersCount, toPosition + headersCount)
        }
        return true
    }

    override fun clearInternally(adapter: EasyRecyclerAdapter<ItemType>, recyclerView: RecyclerView?): Boolean {
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

    private class DiffUtilCallback<T : EasyAdapterDataModel>(
        private val oldData: List<T>,
        private val newData: List<T>,
        private val compareCallbacks: EasyRecyclerAdapterItemsComparator<T>,
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
}
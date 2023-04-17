package com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.collection

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.github.rooneyandshadows.lightbulb.commons.utils.ParcelUtils
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterDataModel
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterSelectableModes
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterSelectableModes.*
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyRecyclerAdapter
import java.util.function.Predicate
import java.util.stream.Collectors

@Suppress("unused", "MemberVisibilityCanBePrivate")
@JvmSuppressWildcards
class ExtendedCollection<ItemType : EasyAdapterDataModel> @JvmOverloads constructor(
    private val selectableMode: EasyAdapterSelectableModes = SELECT_NONE,
    private val itemsComparator: EasyRecyclerAdapterItemsComparator<ItemType>? = null,
) : EasyRecyclerAdapterCollection<ItemType>() {
    private val selectionChangeListeners: MutableList<SelectionChangeListener> = mutableListOf()
    private val items: MutableList<SelectableItem<ItemType>> = mutableListOf()
    val hasSelection: Boolean
        get() = items.any { return@any it.isSelected }
    val selectedItems: List<ItemType>
        get() = items.filter { return@filter it.isSelected }.map { return@map it.item }
    val selectedPositions: List<Int>
        get() = mutableListOf<Int>().apply {
            items.forEachIndexed { position, item ->
                if (item.isSelected) add(position)
            }
        }
    val selectedPositionsAsArray: IntArray
        get() = selectedPositions.toIntArray()
    val selectionString: String
        get() = selectedItems.joinToString(transform = { item -> item.itemName }, separator = ", ")

    fun addOnSelectionChangeListener(listener: SelectionChangeListener) {
        if (selectionChangeListeners.contains(listener)) return
        selectionChangeListeners.add(listener)
    }

    fun removeOnSelectionChangeListener(listener: SelectionChangeListener) {
        selectionChangeListeners.remove(listener)
    }

    fun isItemSelected(targetPosition: Int): Boolean {
        if (selectableMode == SELECT_NONE || !positionExists(targetPosition)) return false
        return items[targetPosition].isSelected
    }

    fun isItemSelected(targetItem: ItemType): Boolean {
        val targetPosition = getPosition(targetItem)
        return if (targetPosition == -1) false else isItemSelected(targetPosition)
    }

    fun clearSelection(adapter: EasyRecyclerAdapter<ItemType>) {
        if (selectableMode == SELECT_NONE) return
        if (clearSelectionInternally(adapter, true))
            dispatchSelectionChangeEvent()
    }

    fun selectAll(adapter: EasyRecyclerAdapter<ItemType>, newState: Boolean) {
        if (selectableMode == SELECT_NONE) return
        var changed = false
        for (positionToSelect in items.indices) {
            val isItemSelected = items[positionToSelect].isSelected
            if (newState == isItemSelected) continue
            changed = true
            selectInternally(adapter, positionToSelect, newState, true)
        }
        if (changed) dispatchSelectionChangeEvent()
    }

    /**
     * Used to un/select item corresponding to particular position in the adapter.
     *
     * @param adapter adapter to notify in case of change
     * @param targetPosition position to be un/selected
     * @param newState true -> select | false -> unselect.
     * @param notifyChange whether to notify registered observers in case of change.
     */
    @JvmOverloads
    fun selectItemAt(
        adapter: EasyRecyclerAdapter<ItemType>,
        targetPosition: Int,
        newState: Boolean,
        notifyChange: Boolean = true,
    ) {
        if (selectableMode == SELECT_NONE || newState == isItemSelected(targetPosition)) return
        if (selectableMode == SELECT_SINGLE) clearSelectionInternally(adapter, true)
        selectInternally(adapter, targetPosition, newState, notifyChange)
        dispatchSelectionChangeEvent()
    }

    /**
     * Used to select/select particular item in the adapter.
     *
     * @param adapter adapter to notify in case of change
     * @param targetItem item to be selected/unselected
     * @param newState true -> select | false -> unselect.
     * @param notifyChange whether to notify registered observers in case of change.
     */
    fun selectItem(
        adapter: EasyRecyclerAdapter<ItemType>,
        targetItem: ItemType,
        newState: Boolean,
        notifyChange: Boolean = true,
    ) {
        val index = getPosition(targetItem)
        if (index == -1) return
        selectItemAt(adapter, index, newState, notifyChange)
    }

    /**
     * * Used to select items corresponding to array of positions in the adapter.
     *
     * @param adapter adapter to notify in case of change
     * @param positions positions to be selected
     * @param newState new selected state for the positions
     * @param incremental Indicates whether the selection is applied incremental or initial.
     * Important: Parameter is taken in account only when using multiple selection [EasyAdapterSelectableModes].
     */
    fun selectPositions(
        adapter: EasyRecyclerAdapter<ItemType>,
        positions: IntArray,
        newState: Boolean,
        incremental: Boolean,
    ) {
        val targetPositions = positions.filter { return@filter positionExists(it) }
        if (selectableMode == SELECT_NONE) return
        if (targetPositions.isEmpty()) {
            if (!hasSelection) return
            if (clearSelectionInternally(adapter, true))
                dispatchSelectionChangeEvent()
            return
        }
        if (selectableMode == SELECT_SINGLE) {
            val targetPosition = targetPositions[0]
            if (!positionExists(targetPosition) || newState == isItemSelected(targetPosition)) return
            clearSelectionInternally(adapter, true)
            val selectionChanged = selectInternally(adapter, targetPosition, newState, true)
            if (selectionChanged) dispatchSelectionChangeEvent()
        }
        if (selectableMode == SELECT_MULTIPLE) {
            var selectionChanged = false
            if (incremental) {
                targetPositions.forEach { position ->
                    selectionChanged = selectionChanged || selectInternally(adapter, position, newState, true)
                }
            } else {
                items.forEachIndexed { position, _ ->
                    val stateToSet = if (targetPositions.contains(position)) newState else false
                    selectionChanged = selectionChanged || selectInternally(adapter, position, stateToSet, true)
                }
            }
            if (selectionChanged) dispatchSelectionChangeEvent()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun setInternally(
        collection: List<ItemType>,
        adapter: EasyRecyclerAdapter<ItemType>,
        recyclerView: RecyclerView?,
    ): Boolean {
        val selectionChanged = clearSelectionInternally(adapter, false)
        val selectableCollection = wrapToSelectable(collection)
        val hasStableIds = adapter.hasStableIds()
        val diffResult = if (itemsComparator != null && !hasStableIds) DiffUtil.calculateDiff(
            DiffUtilCallback(items, selectableCollection, itemsComparator), true
        ) else null
        items.clear()
        items.addAll(selectableCollection)
        adapter.apply {
            if (diffResult != null) {
                diffResult.dispatchUpdatesTo(AdapterUpdateCallback(this, headersCount))
                recyclerView?.invalidateItemDecorations()
            } else notifyDataSetChanged()
        }
        if (selectionChanged) dispatchSelectionChangeEvent()
        return true
    }

    override fun addInternally(
        item: ItemType,
        adapter: EasyRecyclerAdapter<ItemType>,
        recyclerView: RecyclerView?,
    ): Boolean {
        val selectableItem = wrapToSelectable(item)
        val headersCount = adapter.headersCount
        val needToUpdatePreviousLastItem = items.size > 0 && recyclerView!!.itemDecorationCount > 0
        val previousLastPosition = items.size + headersCount - 1
        items.add(selectableItem)
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
        val selectableCollection = wrapToSelectable(collection)
        val headersCount = adapter.headersCount
        val needToUpdatePreviousLastItem = items.size > 0 && recyclerView!!.itemDecorationCount > 0
        val positionStart = items.size + 1
        val newItemsCount = collection.size
        items.addAll(selectableCollection)
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
        val selectionChanged = items[targetPosition].isSelected
        items.removeAt(targetPosition)
        adapter.apply {
            notifyItemRemoved(targetPosition + headersCount)
        }
        if (selectionChanged) dispatchSelectionChangeEvent()
        return true
    }

    override fun removeAllInternally(
        targets: List<ItemType>,
        adapter: EasyRecyclerAdapter<ItemType>,
        recyclerView: RecyclerView?,
    ): Boolean {
        val positionsToRemove = getPositions(targets).sortedDescending()
        if (positionsToRemove.isEmpty()) return false
        val selectionChanged = isAtLeastOneSelected(positionsToRemove)
        items.removeIf { return@removeIf targets.contains(it.item) }
        adapter.apply {
            for (position in positionsToRemove)
                notifyItemRemoved(position + headersCount)
        }
        if (selectionChanged) dispatchSelectionChangeEvent()
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
        val selectionChanged = clearSelectionInternally(adapter, false)
        val notifyRangeStart = 0
        val notifyRangeEnd = items.size - 1
        items.clear()
        adapter.apply {
            notifyItemRangeRemoved(notifyRangeStart + headersCount, notifyRangeEnd + headersCount)
        }
        if (selectionChanged) dispatchSelectionChangeEvent()
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
        return items.map { return@map it.item }.toList()
    }

    override fun getPosition(target: ItemType): Int {
        val rawItems = items.map { return@map it.item }
        return rawItems.indexOf(target)
    }

    override fun getItem(position: Int): ItemType? {
        return if (positionExists(position)) items[position].item else null
    }

    override fun getItems(criteria: Predicate<ItemType>): List<ItemType> {
        return items.stream()
            .map { return@map it.item }
            .filter { return@filter criteria.test(it) }
            .collect(Collectors.toList())
    }

    override fun getItems(positions: IntArray): List<ItemType> {
        return mutableListOf<ItemType>().apply {
            positions.forEach { position ->
                if (!positionExists(position)) return@forEach
                add(items[position].item)
            }
        }
    }

    override fun getItems(positions: List<Int>): List<ItemType> {
        return mutableListOf<ItemType>().apply {
            positions.forEach { position ->
                if (!positionExists(position)) return@forEach
                add(items[position].item)
            }
        }
    }

    override fun getPositions(criteria: Predicate<ItemType>): IntArray {
        return getPositions(getItems(criteria))
    }

    override fun getPositions(targets: List<ItemType>): IntArray {
        if (targets.isEmpty()) return IntArray(0)
        return mutableListOf<Int>().let { positions ->
            val rawItems = items.map { return@map it.item }
            targets.forEach {
                val targetPos = rawItems.indexOf(it)
                if (targetPos != -1) positions.add(targetPos)
            }
            return@let positions.toIntArray()
        }
    }

    override fun getPositions(targets: Array<ItemType>): IntArray {
        if (targets.isEmpty()) return IntArray(0)
        return mutableListOf<Int>().let { positions ->
            val rawItems = items.map { return@map it.item }
            targets.forEach {
                val targetPos = rawItems.indexOf(it)
                if (targetPos != -1) positions.add(targetPos)
            }
            return@let positions.toIntArray()
        }
    }

    override fun getPositionsByItemNames(targetNames: List<String>): IntArray {
        if (targetNames.isEmpty()) return IntArray(0)
        return mutableListOf<Int>().let { positions ->
            items.forEachIndexed { position, selectableItem ->
                if (!targetNames.contains(selectableItem.item.itemName)) return@forEachIndexed
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

    private fun wrapToSelectable(target: ItemType): SelectableItem<ItemType> {
        return SelectableItem(false, target)
    }

    private fun wrapToSelectable(target: List<ItemType>): MutableList<SelectableItem<ItemType>> {
        return target.map { return@map wrapToSelectable(it) }.toMutableList()
    }

    private fun clearSelectionInternally(
        adapter: EasyRecyclerAdapter<ItemType>,
        notifyForSelectionChange: Boolean,
    ): Boolean {
        if (!hasSelection) return false
        items.forEachIndexed { position, selectableItem ->
            val isSelected = selectableItem.isSelected
            if (!isSelected) return@forEachIndexed
            adapter.apply {
                if (notifyForSelectionChange) {
                    val positionToNotify = position + headersCount
                    adapter.notifyItemChanged(positionToNotify, false)
                }
            }
        }
        return true
    }

    private fun selectInternally(
        adapter: EasyRecyclerAdapter<ItemType>,
        position: Int,
        newState: Boolean,
        notifyForSelectionChange: Boolean,
    ): Boolean {
        if (!positionExists(position) || isItemSelected(position) == newState) return false
        items[position].isSelected = newState
        if (notifyForSelectionChange) {
            adapter.apply {
                val positionToNotify = position + headersCount
                notifyItemChanged(positionToNotify, false)
            }
        }
        return true
    }

    private fun isAtLeastOneSelected(positions: List<Int>): Boolean {
        var hasSelectionInPositions = false
        for (positionToRemove in positions) {
            if (items[positionToRemove].isSelected) {
                hasSelectionInPositions = true
                break
            }
        }
        return hasSelectionInPositions
    }

    private fun dispatchSelectionChangeEvent() {
        selectionChangeListeners.forEach {
            it.onChanged(selectedPositionsAsArray)
        }
    }

    private class DiffUtilCallback<T : EasyAdapterDataModel>(
        private val oldData: List<SelectableItem<T>>,
        private val newData: List<SelectableItem<T>>,
        private val compareCallbacks: EasyRecyclerAdapterItemsComparator<T>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldData.size
        }

        override fun getNewListSize(): Int {
            return newData.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return compareCallbacks.compareItems(oldData[oldItemPosition].item, newData[newItemPosition].item)
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldData[oldItemPosition]
            val newItem = newData[newItemPosition]
            return oldItem.isSelected == newItem.isSelected && compareCallbacks.compareItemsContent(
                oldItem.item,
                newItem.item
            )
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

    private class SelectableItem<ItemType : EasyAdapterDataModel> : Parcelable {
        var isSelected: Boolean
        var item: ItemType

        constructor(isSelected: Boolean, item: ItemType) {
            this.isSelected = isSelected
            this.item = item
        }

        @Suppress("UNCHECKED_CAST")
        constructor(parcel: Parcel) {
            val className: String = parcel.readString()!!
            isSelected = parcel.readByte() != 0.toByte()
            val clazz = Class.forName(className) as Class<ItemType>
            item = ParcelUtils.readParcelable(parcel, clazz) as ItemType
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(item.javaClass.name)
            parcel.writeByte(if (isSelected) 1 else 0)
            parcel.writeParcelable(item, flags)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<SelectableItem<EasyAdapterDataModel>> {
            override fun createFromParcel(parcel: Parcel): SelectableItem<EasyAdapterDataModel> {
                return SelectableItem(parcel)
            }

            override fun newArray(size: Int): Array<SelectableItem<EasyAdapterDataModel>?> {
                return arrayOfNulls(size)
            }
        }
    }

    interface SelectionChangeListener {
        fun onChanged(newSelection: IntArray?)
    }
}
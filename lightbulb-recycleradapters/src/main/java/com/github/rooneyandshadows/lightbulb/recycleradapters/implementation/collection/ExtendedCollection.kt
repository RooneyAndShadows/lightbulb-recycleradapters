package com.github.rooneyandshadows.lightbulb.recycleradapters.implementation.collection

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcel
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.github.rooneyandshadows.lightbulb.commons.utils.BundleUtils
import com.github.rooneyandshadows.lightbulb.commons.utils.ParcelUtils
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyRecyclerAdapter
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.collection.AdapterCollection
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.data.EasyAdapterDataModel
import com.github.rooneyandshadows.lightbulb.recycleradapters.implementation.collection.ExtendedCollection.Item
import java.util.function.Predicate
import java.util.stream.Collectors

@Suppress("unused", "MemberVisibilityCanBePrivate")
open class ExtendedCollection<ItemType : Item> @JvmOverloads constructor(
    adapter: EasyRecyclerAdapter<ItemType>,
    private var selectableMode: SelectableModes = SelectableModes.SELECT_NONE,
    private val itemsComparator: ItemsComparator<ItemType>? = null,
) : AdapterCollection<ItemType>(adapter), Filterable {
    private val selectionChangeListeners: MutableList<SelectionChangeListener> = mutableListOf()
    private val items: MutableList<ItemType> = mutableListOf()
    private val internalFilter: Filter by lazy {
        return@lazy createFilter()
    }
    val hasSelection: Boolean
        get() = items.any { return@any it.isSelected }
    val selectedItems: List<ItemType>
        get() = items.filter { return@filter it.isSelected }
    val filteredItems: List<ItemType>
        get() = items.filter { return@filter it.isVisible }
    val selectedPositions: List<Int>
        get() = mutableListOf<Int>().apply {
            items.forEachIndexed { position, item ->
                if (item.isSelected) add(position)
            }
        }
    val filteredPositions: List<Int>
        get() = mutableListOf<Int>().apply {
            items.forEachIndexed { position, item ->
                if (item.isVisible) add(position)
            }
        }
    val selectedPositionsAsArray: IntArray
        get() = selectedPositions.toIntArray()
    val filteredPositionsAsArray: IntArray
        get() = filteredPositions.toIntArray()
    val selectionString: String
        get() = selectedItems.joinToString(
            transform = { item -> item.itemName },
            separator = ", "
        )
    var currentFilterQuery: String = ""
        private set

    companion object {
        private const val ADAPTER_SELECTION_MODE = "ADAPTER_SELECTION_MODE"
        private const val CURRENT_FILTER_QUERY = "CURRENT_FILTER_QUERY"
    }

    fun addOnSelectionChangeListener(listener: SelectionChangeListener) {
        if (selectionChangeListeners.contains(listener)) return
        selectionChangeListeners.add(listener)
    }

    fun removeOnSelectionChangeListener(listener: SelectionChangeListener) {
        selectionChangeListeners.remove(listener)
    }

    fun isItemSelected(targetPosition: Int): Boolean {
        if (selectableMode == SelectableModes.SELECT_NONE || !positionExists(targetPosition)) return false
        return items[targetPosition].isSelected
    }

    fun isItemSelected(targetItem: ItemType): Boolean {
        val targetPosition = getPosition(targetItem)
        return if (targetPosition == -1) false else isItemSelected(targetPosition)
    }

    fun clearSelection() {
        if (selectableMode == SelectableModes.SELECT_NONE) return
        if (clearSelectionInternally(true))
            dispatchSelectionChangeEvent()
    }

    fun selectAll(newState: Boolean) {
        if (selectableMode == SelectableModes.SELECT_NONE) return
        var selectionChanged = false
        for (positionToSelect in items.indices)
            selectionChanged =
                selectionChanged || selectInternally(positionToSelect, newState, true)
        if (selectionChanged) dispatchSelectionChangeEvent()
    }

    /**
     * Used to un/select item corresponding to particular position in the adapter.
     *
     * @param targetPosition position to be un/selected
     * @param newState true -> select | false -> unselect.
     * @param notifyChange whether to notify registered observers in case of change.
     */
    @JvmOverloads
    fun selectItemAt(
        targetPosition: Int,
        newState: Boolean,
        notifyChange: Boolean = true,
    ) {
        if (selectableMode == SelectableModes.SELECT_NONE) return
        if (selectableMode == SelectableModes.SELECT_SINGLE && newState) clearSelectionInternally(
            true
        )
        val selectionChanged = selectInternally(targetPosition, newState, notifyChange)
        if (selectionChanged) dispatchSelectionChangeEvent()
    }

    /**
     * Used to select/select particular item in the adapter.
     *
     * @param targetItem item to be selected/unselected
     * @param newState true -> select | false -> unselect.
     * @param notifyChange whether to notify registered observers in case of change.
     */
    fun selectItem(
        targetItem: ItemType,
        newState: Boolean,
        notifyChange: Boolean = true,
    ) {
        val index = getPosition(targetItem)
        if (index == -1) return
        selectItemAt(index, newState, notifyChange)
    }

    /**
     * * Used to select items corresponding to array of positions in the adapter.
     *
     * @param positions positions to be selected
     * @param newState new selected state for the positions
     * @param incremental Indicates whether the selection is applied incremental or initial.
     * Important: Parameter is taken in account only when using multiple selection - [SelectableModes.SELECT_MULTIPLE].
     */
    fun selectPositions(
        positions: IntArray,
        newState: Boolean,
        incremental: Boolean,
    ) {
        val targetPositions = positions.filter { return@filter positionExists(it) }
        if (selectableMode == SelectableModes.SELECT_NONE) return
        if (targetPositions.isEmpty()) {
            if (!hasSelection) return
            if (clearSelectionInternally(true))
                dispatchSelectionChangeEvent()
            return
        }
        if (selectableMode == SelectableModes.SELECT_SINGLE) {
            val targetPosition = targetPositions[0]
            if (!positionExists(targetPosition) || newState == isItemSelected(targetPosition)) return
            clearSelectionInternally(true)
            val selectionChanged =
                selectInternally(targetPosition, newState, notifyForSelectionChange = true)
            if (selectionChanged) dispatchSelectionChangeEvent()
        }
        if (selectableMode == SelectableModes.SELECT_MULTIPLE) {
            var selectionChanged = false
            if (incremental) {
                targetPositions.forEach { position ->
                    selectionChanged = selectionChanged || selectInternally(
                        position,
                        newState,
                        notifyForSelectionChange = true
                    )
                }
            } else {
                items.forEachIndexed { position, _ ->
                    val stateToSet = if (targetPositions.contains(position)) newState else false
                    selectionChanged = selectionChanged || selectInternally(
                        position,
                        stateToSet,
                        notifyForSelectionChange = true
                    )
                }
            }
            if (selectionChanged) dispatchSelectionChangeEvent()
        }
    }

    fun getFilteredItem(position: Int): ItemType? {
        val items = filteredItems
        if (!positionExists(items, position)) return null
        return items[position]
    }

    protected open fun filterItem(item: ItemType, filterQuery: String): Boolean {
        return true
    }

    @Override
    final override fun getFilter(): Filter {
        return internalFilter
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun setInternally(collection: List<ItemType>): Boolean {
        val selectionChanged = clearSelectionInternally(false)

        applyFilterToItems(collection)

        val hasStableIds = adapter.hasStableIds()
        val diffResult = if (itemsComparator != null && !hasStableIds) DiffUtil.calculateDiff(
            DiffUtilCallback(items, collection, itemsComparator), true
        ) else null
        items.clear()
        items.addAll(collection)
        adapter.apply {
            if (diffResult != null) {
                diffResult.dispatchUpdatesTo(AdapterUpdateCallback(this))
                recyclerView?.invalidateItemDecorations()
            } else notifyDataSetChanged()
        }
        if (selectionChanged) dispatchSelectionChangeEvent()
        return true
    }

    override fun addInternally(item: ItemType): Boolean {
        applyFilterToItem(item)
        items.add(item)
        if (!item.isVisible) return true
        adapter.apply {
            val currentlyVisible = filteredItems
            val recyclerView = adapter.recyclerView
            notifyItemInserted(currentlyVisible.size - 1)
            val hasItemDecorations = (recyclerView?.itemDecorationCount ?: 0) > 0
            val previousLastItem = currentlyVisible.size - 2
            val needToUpdatePreviousLastItem = previousLastItem >= 0 && hasItemDecorations
            // update last item decoration without animation
            if (needToUpdatePreviousLastItem) notifyItemChanged(previousLastItem, false)
        }
        return true
    }

    override fun addAllInternally(collection: List<ItemType>): Boolean {
        if (collection.isEmpty()) return false
        applyFilterToItems(collection)
        val currentlyVisible = filteredItems
        items.addAll(collection)
        val filtered = collection.filter { return@filter it.isVisible }
        if (filtered.isEmpty()) return true
        adapter.apply {
            val recyclerView = adapter.recyclerView
            val positionStart = currentlyVisible.size
            val newItemsCount = filtered.size
            notifyItemRangeInserted(positionStart, newItemsCount)
            val hasItemDecorations = (recyclerView?.itemDecorationCount ?: 0) > 0
            val previousLastItem = currentlyVisible.size - 1
            val needToUpdatePreviousLastItem = previousLastItem >= 0 && hasItemDecorations
            // update last item decoration without animation
            if (needToUpdatePreviousLastItem) notifyItemChanged(previousLastItem, false)
        }
        return true
    }

    override fun removeInternally(targetPosition: Int): Boolean {
        if (!positionExists(targetPosition)) return false
        val itemToRemove = items[targetPosition]
        val selectionChanged =
            selectInternally(targetPosition, newState = false, notifyForSelectionChange = false)
        val visibleItems = filteredItems
        items.removeAt(targetPosition)
        if (itemToRemove.isVisible) adapter.apply {
            val posToRemove = visibleItems.indexOf(itemToRemove)
            notifyItemRemoved(posToRemove)
        }
        if (selectionChanged) dispatchSelectionChangeEvent()
        return true
    }

    override fun removeAllInternally(targets: List<ItemType>): Boolean {
        val positions = getPositions(targets)
        if (positions.isEmpty()) return false
        val selectionChanged = isAtLeastOneSelected(positions)
        val positionsToRemove = getFilteredPositions(targets).sortedDescending()
        val removedItems = items.removeIf { return@removeIf targets.contains(it) }
        if (!removedItems) return false

        for (position in positionsToRemove)
            adapter.notifyItemRemoved(position)

        if (selectionChanged) dispatchSelectionChangeEvent()
        return true
    }

    override fun moveInternally(fromPosition: Int, toPosition: Int): Boolean {
        if (!positionExists(fromPosition) || !positionExists(toPosition)) return false
        val movingItem = items[fromPosition]
        items.removeAt(fromPosition)
        items.add(toPosition, movingItem)
        val needsNotify = movingItem.isVisible
        if (needsNotify) internalFilter.filter(currentFilterQuery)
        return true
    }

    override fun clearInternally(): Boolean {
        if (items.isEmpty()) return false
        val selectionChanged = clearSelectionInternally(false)
        val visibleItems = filteredItems
        items.clear()
        adapter.apply {
            val notifyRangeStart = 0
            val notifyRangeEnd = visibleItems.size - 1
            notifyItemRangeRemoved(notifyRangeStart, notifyRangeEnd)
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
        return positionExists(items, position)
    }

    override fun getItems(): List<ItemType> {
        return items.toList()
    }

    override fun getPosition(target: ItemType): Int {
        return items.indexOf(target)
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
            items.forEachIndexed { position, selectableItem ->
                if (!targetNames.contains(selectableItem.itemName)) return@forEachIndexed
                positions.add(position)
            }
            return@let positions.toIntArray()
        }
    }

    override fun getPositionStrings(positions: IntArray): String {
        return items.filterIndexed { index, _ ->
            return@filterIndexed positions.contains(index)
        }.joinToString(
            transform = { item -> item.itemName },
            separator = ", "
        )
    }

    override fun onSaveInstanceState(): Bundle {
        return super.onSaveInstanceState().apply {
            BundleUtils.putInt(ADAPTER_SELECTION_MODE, this, selectableMode.value)
            BundleUtils.putString(CURRENT_FILTER_QUERY, this, currentFilterQuery)
        }
    }

    override fun onRestoreInstanceState(state: Bundle) {
        super.onRestoreInstanceState(state)

        selectableMode = SelectableModes.valueOf(state.getInt(ADAPTER_SELECTION_MODE))
        currentFilterQuery = BundleUtils.getString(CURRENT_FILTER_QUERY, state) ?: ""
        internalFilter.filter(currentFilterQuery)
    }

    fun getFilteredPositions(targets: List<ItemType>): IntArray {
        if (targets.isEmpty()) return IntArray(0)
        return mutableListOf<Int>().let { positions ->
            val rawItems = filteredItems
            targets.forEach {
                val targetPos = rawItems.indexOf(it)
                if (targetPos != -1) positions.add(targetPos)
            }
            return@let positions.toIntArray()
        }
    }

    fun getFilteredPositions(targets: Array<ItemType>): IntArray {
        if (targets.isEmpty()) return IntArray(0)
        return mutableListOf<Int>().let { positions ->
            val rawItems = filteredItems
            targets.forEach {
                val targetPos = rawItems.indexOf(it)
                if (targetPos != -1) positions.add(targetPos)
            }
            return@let positions.toIntArray()
        }
    }


    private fun applyFilterToItem(item: ItemType) {
        item.isVisible = filterItem(item, currentFilterQuery)
    }

    private fun applyFilterToItems(items: List<ItemType>) {
        items.forEach {
            applyFilterToItem(it)
        }
    }

    private fun clearSelectionInternally(notifyForSelectionChange: Boolean): Boolean {
        if (!hasSelection) return false
        items.forEachIndexed { position, item ->
            val isSelected = item.isSelected
            if (!isSelected) return@forEachIndexed
            item.isSelected = false
            adapter.apply {
                if (notifyForSelectionChange && item.isVisible) {
                    val positionToNotify = position
                    adapter.notifyItemChanged(positionToNotify, false)
                }
            }
        }
        return true
    }

    private fun selectInternally(
        position: Int,
        newState: Boolean,
        notifyForSelectionChange: Boolean
    ): Boolean {
        if (!positionExists(position) || items[position].isSelected == newState) return false
        val item = items[position]
        item.isSelected = newState
        if (notifyForSelectionChange && item.isVisible) {
            adapter.apply {
                val positionToNotify = position
                notifyItemChanged(positionToNotify, false)
            }
        }
        return true
    }

    private fun isAtLeastOneSelected(positions: IntArray): Boolean {
        var hasSelectionInPositions = false
        for (positionToRemove in positions) {
            if (items[positionToRemove].isSelected) {
                hasSelectionInPositions = true
                break
            }
        }
        return hasSelectionInPositions
    }

    private fun positionExists(items: List<*>, position: Int): Boolean {
        return items.isNotEmpty() && position >= 0 && position < items.size
    }

    private fun dispatchSelectionChangeEvent() {
        selectionChangeListeners.forEach {
            it.onChanged(selectedPositionsAsArray)
        }
    }

    private fun createFilter(): Filter {
        return object : Filter() {
            @Override
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                currentFilterQuery = charSequence.toString()
                val result: MutableList<Int> = mutableListOf()
                if (currentFilterQuery.isBlank()) {
                    result.addAll(items.indices)
                } else {
                    items.forEachIndexed { index, extendedItem ->
                        if (filterItem(extendedItem, currentFilterQuery))
                            result.add(index)
                    }
                }
                val filterResults = FilterResults()
                filterResults.values = result
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            @SuppressLint("NotifyDataSetChanged")
            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                if (filterResults.values == null) return
                val newPositions = (filterResults.values as List<Int>).toMutableList()
                val oldPositions = filteredPositions
                items.forEachIndexed { position, extendedItem ->
                    extendedItem.isVisible = newPositions.contains(position)
                }

                val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                    override fun getOldListSize(): Int {
                        return oldPositions.size
                    }

                    override fun getNewListSize(): Int {
                        return newPositions.size
                    }

                    override fun areItemsTheSame(
                        oldItemPosition: Int,
                        newItemPosition: Int
                    ): Boolean {
                        return oldPositions[oldItemPosition] == newPositions[newItemPosition]
                    }

                    override fun areContentsTheSame(
                        oldItemPosition: Int,
                        newItemPosition: Int
                    ): Boolean {
                        return true
                    }
                }, true)
                diff.dispatchUpdatesTo(UpdateCallback(adapter))
            }
        }
    }

    private class DiffUtilCallback<T : Item>(
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
            return compareCallbacks.compareItems(
                oldData[oldItemPosition],
                newData[newItemPosition],
            )
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldData[oldItemPosition]
            val newItem = newData[newItemPosition]
            val selectedStateEquals = oldItem.isSelected == newItem.isSelected
            val visibleStateEquals = oldItem.isVisible == newItem.isVisible
            return selectedStateEquals && visibleStateEquals && compareCallbacks.compareItemsContent(
                oldItem,
                newItem
            )
        }
    }

    private class AdapterUpdateCallback(
        private val adapter: RecyclerView.Adapter<*>,
    ) : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            adapter.notifyItemRangeInserted(position, count)
        }

        override fun onRemoved(position: Int, count: Int) {
            adapter.notifyItemRangeRemoved(position, count)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            adapter.notifyItemMoved(fromPosition, toPosition)
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            adapter.notifyItemRangeChanged(position, count, payload)
        }
    }

    private class UpdateCallback(
        private val adapter: RecyclerView.Adapter<*>,
    ) : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            adapter.notifyItemRangeInserted(position, count)
        }

        override fun onRemoved(position: Int, count: Int) {
            adapter.notifyItemRangeRemoved(position, count)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            adapter.notifyItemMoved(fromPosition, toPosition)
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            adapter.notifyItemRangeChanged(position, count)
        }
    }

    enum class SelectableModes(val value: Int) {
        SELECT_NONE(1),
        SELECT_SINGLE(2),
        SELECT_MULTIPLE(3);

        companion object {
            @JvmStatic
            fun valueOf(value: Int) = entries.first { it.value == value }
        }
    }

    abstract class Item() : EasyAdapterDataModel() {
        var isSelected: Boolean = false
            internal set
        var isVisible: Boolean = true
            internal set

        protected constructor(parcel: Parcel) : this() {
            isSelected = ParcelUtils.readBoolean(parcel)!!
            isVisible = ParcelUtils.readBoolean(parcel)!!
        }

        override fun writeToParcel(p0: Parcel, p1: Int) {
            ParcelUtils.writeBoolean(p0, isSelected)
            ParcelUtils.writeBoolean(p0, isVisible)
        }

        override fun describeContents(): Int {
            return 0
        }
    }

    interface SelectionChangeListener {
        fun onChanged(newSelection: IntArray?)
    }
}
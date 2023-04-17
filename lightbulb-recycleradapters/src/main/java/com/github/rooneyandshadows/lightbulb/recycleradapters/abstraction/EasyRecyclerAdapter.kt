package com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import com.github.rooneyandshadows.lightbulb.commons.utils.BundleUtils
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterSelectableModes.*
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.callbacks.EasyAdapterCollectionChangedListener
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.callbacks.EasyAdapterSelectionChangedListener
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.collection.EasyRecyclerAdapterItemsComparator
import com.github.rooneyandshadows.lightbulb.recycleradapters.implementation.HeaderViewRecyclerAdapter
import java.util.function.Predicate
import java.util.stream.Collectors

//TODO fix to use ConcatAdapter instead of wrapping with HeaderViewRecyclerAdapter
@Suppress("MemberVisibilityCanBePrivate", "unused", "UNCHECKED_CAST")
@JvmSuppressWildcards
abstract class EasyRecyclerAdapter<ItemType : EasyAdapterDataModel> @JvmOverloads constructor(
    selectableMode: EasyAdapterSelectableModes = SELECT_NONE,
) : Adapter<ViewHolder>(), DefaultLifecycleObserver {
    private var recyclerView: RecyclerView? = null
    private var items: MutableList<SelectableItem<ItemType>> = mutableListOf()
    private var itemsSelection: MutableList<Int> = mutableListOf()
    private val onSelectionChangedListeners: MutableList<EasyAdapterSelectionChangedListener> = mutableListOf()
    private val onCollectionChangedListeners: MutableList<EasyAdapterCollectionChangedListener> = mutableListOf()
    protected var itemsComparator: EasyRecyclerAdapterItemsComparator<ItemType>? = null
    protected var lifecycleOwner: LifecycleOwner? = null
        set(value) {
            field = value
            if (value != null)
                lifecycleOwner!!.lifecycle.addObserver(this)
        }
    var wrapperAdapter: HeaderViewRecyclerAdapter? = null
    var selectableMode: EasyAdapterSelectableModes = selectableMode
        private set
    val headersCount: Int
        get() = if (wrapperAdapter == null) 0 else wrapperAdapter!!.headersCount
    val footersCount: Int
        get() = if (wrapperAdapter == null) 0 else wrapperAdapter!!.footersCount
    val selectedItems: List<ItemType>
        get() {
            return mutableListOf<ItemType>().apply {
                for (selectedPosition in itemsSelection)
                    add(items[selectedPosition].item)
            }
        }
    val selectedPositionsAsList: List<Int>
        get() = itemsSelection.toList()
    val selectedPositionsAsArray: IntArray
        get() {
            return IntArray(if (itemsSelection.isEmpty()) 0 else itemsSelection.size).apply {
                for (i in itemsSelection.indices)
                    this[i] = itemsSelection[i]
            }
        }
    val selectionString: String
        get() {
            var string = ""
            for (i in selectedItems.indices) {
                string += getItemName(selectedItems[i])
                if (i < selectedItems.size - 1)
                    string = "$string, "
            }
            return string
        }

    protected open fun onCollectionChanged() {
    }

    /**
     * Used to add values to out state of the adapter during save state.
     *
     * @param outState state to save
     */
    protected open fun onSaveInstanceState(outState: Bundle) {
    }

    /**
     * Used to reuse values saved during previous save state.
     *
     * @param savedState The state that had previously been returned by onSaveInstanceState
     */
    protected open fun onRestoreInstanceState(savedState: Bundle) {
    }

    companion object {
        private const val ADAPTER_ITEMS = "ADAPTER_ITEMS"
        private const val ADAPTER_SELECTION = "ADAPTER_SELECTION"
        private const val ADAPTER_SELECTION_MODE = "ADAPTER_SELECTION_MODE"
    }

    @Override
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    @Override
    override fun onDestroy(owner: LifecycleOwner) {
        clearObservableCallbacksOnCollection()
    }

    @Override
    override fun getItemCount(): Int {
        return items.size
    }

    @Override
    open fun getItemName(item: ItemType): String? {
        return item.itemName
    }

    fun saveAdapterState(): Bundle {
        return Bundle().apply {
            putParcelableArrayList(ADAPTER_ITEMS, ArrayList(items))
            putIntegerArrayList(ADAPTER_SELECTION, ArrayList(itemsSelection))
            putInt(ADAPTER_SELECTION_MODE, selectableMode.value)
            onSaveInstanceState(this)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun restoreAdapterState(savedState: Bundle) {
        savedState.apply {
            val clz = Class.forName(SelectableItem::class.java.name) as Class<SelectableItem<ItemType>>
            items = BundleUtils.getParcelableList(ADAPTER_ITEMS, this, clz) as MutableList<SelectableItem<ItemType>>
            itemsSelection = savedState.getIntegerArrayList(ADAPTER_SELECTION)!!
            selectableMode = EasyAdapterSelectableModes.valueOf(savedState.getInt(ADAPTER_SELECTION_MODE))
            onRestoreInstanceState(savedState)
            notifyDataSetChanged()
        }
    }

    fun addOnSelectionChangedListener(onSelectionChangedListener: EasyAdapterSelectionChangedListener) {
        onSelectionChangedListeners.add(onSelectionChangedListener)
    }

    fun removeOnSelectionChangedListener(onSelectionChangedListener: EasyAdapterSelectionChangedListener) {
        onSelectionChangedListeners.remove(onSelectionChangedListener)
    }

    fun addOrReplaceSelectionChangedListener(onSelectionChangedListener: EasyAdapterSelectionChangedListener) {
        onSelectionChangedListeners.remove(onSelectionChangedListener)
        onSelectionChangedListeners.add(onSelectionChangedListener)
    }

    fun addOnCollectionChangedListener(onCollectionChangedListener: EasyAdapterCollectionChangedListener) {
        onCollectionChangedListeners.add(onCollectionChangedListener)
    }

    protected fun clearObservableCallbacks(position: Int) {
        if (positionExists(position)) clearObservableCallbacks(getItem(position))
    }

    protected fun clearObservableCallbacks(item: ItemType?) {
        if (item is EasyAdapterObservableDataModel) item.clearObservableCallbacks()
    }

    protected fun clearObservableCallbacksOnCollection() {
        for (item in items) clearObservableCallbacks(item.item)
    }

    @SuppressLint("NotifyDataSetChanged")
    open fun setCollection(collection: List<ItemType>) {
        val selectableCollection = wrapToSelectable(collection)
        val selectionChanged: Boolean
        if (itemsComparator == null || hasStableIds()) {
            selectionChanged = clearSelectionInternally(false).isNotEmpty()
            clearObservableCallbacksOnCollection()
            items.clear()
            items.addAll(selectableCollection)
            notifyDataSetChanged()
        } else {
            val diffResult = DiffUtil.calculateDiff(
                EasyAdapterDiffUtilCallback(
                    items,
                    selectableCollection,
                    itemsComparator!!
                ), true
            )
            selectionChanged = clearSelectionInternally(false).isNotEmpty()
            clearObservableCallbacksOnCollection()
            items.clear()
            items.addAll(selectableCollection)
            diffResult.dispatchUpdatesTo(EasyRecyclerAdapterUpdateCallback(this, headersCount))
            recyclerView!!.invalidateItemDecorations()
        }
        if (selectionChanged) dispatchSelectionChangedEvent()
        dispatchCollectionChangedEvent()
    }



    /**
     * Used to add new items in the adapter.
     *
     * @param collection - items to be added.
     */
    open fun appendCollection(collection: List<ItemType>) {
        if (collection.isEmpty()) return
        val selectableCollection = wrapToSelectable(collection)
        val needToUpdatePreviousLastItem = items.size > 0 && recyclerView!!.itemDecorationCount > 0
        val previousLastItem = items.size + headersCount - 1
        val positionStart = items.size + 1
        val newItemsCount = collection.size
        items.addAll(selectableCollection)
        if (needToUpdatePreviousLastItem) notifyItemChanged(
            previousLastItem,
            false
        ) // update last item decoration without animation
        notifyItemRangeInserted(positionStart + headersCount, newItemsCount + headersCount)
        dispatchCollectionChangedEvent()
    }

    /**
     * Used to add new item in the adapter.
     *
     * @param item - item to be added.
     */
    open fun addItem(item: ItemType) {
        val needToUpdatePreviousLastItem =
            items.size > 0 && recyclerView!!.itemDecorationCount > 0
        val previousLastPosition = items.size + headersCount - 1
        items.add(wrapToSelectable(item))
        if (needToUpdatePreviousLastItem) notifyItemChanged(
            previousLastPosition,
            false
        ) // update last item decoration without animation
        notifyItemInserted(items.size + headersCount - 1)
        dispatchCollectionChangedEvent()
    }

    /**
     * Used to remove all items in the adapter.
     */
    open fun clearCollection() {
        if (!hasItems()) return
        val selectionChanged = hasSelection()
        clearSelectionInternally(false)
        clearObservableCallbacksOnCollection()
        val notifyRangeStart = 0
        val notifyRangeEnd = items.size - 1
        items.clear()
        notifyItemRangeRemoved(notifyRangeStart + headersCount, notifyRangeEnd + headersCount)
        if (selectionChanged) dispatchSelectionChangedEvent()
        dispatchCollectionChangedEvent()
    }

    /**
     * Used to move item in the adapter.
     *
     * @param fromPosition - from which position to move.
     * @param toPosition   - new position.
     */
    open fun moveItem(fromPosition: Int, toPosition: Int) {
        if (!positionExists(fromPosition) || !positionExists(toPosition)) return
        val movingItem = getSelectableItem(fromPosition)!!
        items.removeAt(fromPosition)
        items.add(toPosition, movingItem)
        notifyItemMoved(fromPosition + headersCount, toPosition + headersCount)
    }

    /**
     * Used to remove item corresponding to particular position in the adapter.
     *
     * @param targetPosition - position to be removed.
     */
    open fun removeItem(targetPosition: Int) {
        if (!positionExists(targetPosition)) return
        clearObservableCallbacks(targetPosition)
        val selectionChanged = isItemSelected(targetPosition)
        selectInternally(targetPosition, newState = false, notifyForSelectionChange = false)
        items.removeAt(targetPosition)
        notifyItemRemoved(targetPosition + headersCount)
        if (selectionChanged) dispatchSelectionChangedEvent()
        dispatchCollectionChangedEvent()
    }

    /**
     * Used to remove items from the adapter.
     *
     * @param collection - items to be removed.
     */
    open fun removeItems(collection: List<ItemType>?) {
        val positionsToRemove = getPositions(collection)
        if (positionsToRemove.isEmpty()) return
        var selectionChanged = false
        for (position in positionsToRemove) {
            if (!positionExists(position)) continue
            clearObservableCallbacks(position)
            if (isItemSelected(position)) selectionChanged = true
        }
        for (position in positionsToRemove)
            selectInternally(
                position,
                newState = false,
                notifyForSelectionChange = false
            )
        items.removeIf { return@removeIf collection!!.contains(it.item) }
        for (position in positionsToRemove) notifyItemRemoved(position + headersCount)
        if (selectionChanged) dispatchSelectionChangedEvent()
        dispatchCollectionChangedEvent()
    }

    /**
     * Used to clear deselect all elements in adapter.
     */
    open fun clearSelection() {
        if (selectableMode == SELECT_NONE || !hasSelection()) return
        if (clearSelectionInternally(true).isNotEmpty()) dispatchSelectionChangedEvent()
    }

    /**
     * Used to  un/select all elements in adapter.
     *
     * @param newState - true -> select | false -> unselect.
     */
    open fun selectAll(newState: Boolean) {
        if (selectableMode == SELECT_NONE) return
        var changed = false
        for (positionToSelect in items.indices) {
            val isItemSelected = isItemSelected(positionToSelect)
            if (newState == isItemSelected) continue
            changed = true
            selectInternally(positionToSelect, newState, true)
        }
        if (changed) dispatchSelectionChangedEvent()
    }

    /**
     * Used to un/select particular item in the adapter.
     *
     * @param targetItem   - item to be un/selected
     * @param newState     - true -> select | false -> unselect.
     * @param notifyChange - whether to notify registered observers in case of change.
     */
    open fun selectItem(targetItem: ItemType, newState: Boolean, notifyChange: Boolean) {
        val index = getPosition(targetItem)
        if (index == -1) return
        selectItemAt(index, newState, notifyChange)
    }

    /**
     * Used to un/select item corresponding to particular position in the adapter.
     *
     * @param targetPosition - position to be un/selected
     * @param newState       - true -> select | false -> unselect.
     * @param notifyChange   - whether to notify registered observers in case of change.
     */
    @JvmOverloads
    open fun selectItemAt(targetPosition: Int, newState: Boolean, notifyChange: Boolean = true) {
        if (selectableMode == SELECT_NONE || !positionExists(targetPosition) || newState == isItemSelected(
                targetPosition
            )
        ) return
        if (selectableMode == SELECT_SINGLE) clearSelectionInternally(
            true
        )
        selectInternally(targetPosition, newState, notifyChange)
        dispatchSelectionChangedEvent()
    }

    /**
     * Used to un/select particular item in the adapter.
     * Registered observers are notified automatically in case of change.
     *
     * @param targetItem - item to be un/selected
     * @param newState   - true -> select | false -> unselect.
     */
    open fun selectItem(targetItem: ItemType, newState: Boolean) {
        val index = getPosition(targetItem)
        if (index == -1) return
        selectItemAt(index, newState, true)
    }

    /**
     * * Used to select items corresponding to array of positions in the adapter.
     *
     * @param positions   - positions to be selected
     * @param newState    - new selected state for the positions
     * @param incremental - Indicates whether the selection is applied incremental or initial.
     * Important: Parameter is taken in account only when using multiple selection [EasyAdapterSelectableModes].
     */
    open fun selectPositions(positions: IntArray?, newState: Boolean, incremental: Boolean) {
        if (selectableMode == SELECT_NONE) return
        if (positions == null || positions.isEmpty()) {
            if (!hasSelection()) return
            if (clearSelectionInternally(true).isNotEmpty()) dispatchSelectionChangedEvent()
            return
        }
        if (selectableMode == SELECT_SINGLE) {
            val targetPosition = positions[0]
            if (!positionExists(targetPosition) || newState == isItemSelected(targetPosition)) return
            clearSelectionInternally(true)
            selectInternally(targetPosition, newState, true)
            dispatchSelectionChangedEvent()
        }
        if (selectableMode == SELECT_MULTIPLE) {
            var selectionChanged = false
            if (incremental) {
                for (targetPosition in positions) {
                    if (!positionExists(targetPosition) || newState == isItemSelected(targetPosition)) continue
                    selectionChanged = true
                    selectInternally(targetPosition, newState, true)
                }
            } else {
                for (currentItemPosition in items.indices) {
                    var newStateForPosition = isItemSelected(currentItemPosition)
                    for (targetPosition in positions) {
                        if (currentItemPosition == targetPosition) {
                            newStateForPosition = newState
                            break
                        }
                    }
                    if (!positionExists(currentItemPosition) || newStateForPosition == isItemSelected(
                            currentItemPosition
                        )
                    ) continue
                    selectInternally(currentItemPosition, newStateForPosition, true)
                    selectionChanged = true
                }
            }
            if (selectionChanged) dispatchSelectionChangedEvent()
        }
    }


    fun hasItems(): Boolean {
        return items.size > 0
    }

    fun hasSelection(): Boolean {
        return itemsSelection.size > 0
    }

    fun positionExists(position: Int): Boolean {
        return items.isNotEmpty() && position >= 0 && position < items.size
    }

    fun getItems(): List<ItemType> {
        return items.map { return@map it.item }.toList()
    }

    fun getPosition(target: ItemType): Int {
        return items.indexOfFirst { return@indexOfFirst it.item == target }
    }

    fun getItem(position: Int): ItemType? {
        return getSelectableItem(position)?.item
    }

    fun getItems(criteria: Predicate<ItemType>): List<ItemType> {
        return items.stream()
            .filter { return@filter criteria.test(it.item) }
            .map { return@map it.item }
            .collect(Collectors.toList())
    }

    fun getItems(positions: IntArray?): List<ItemType> {
        val itemsFromPositions: MutableList<ItemType> = mutableListOf()
        if (positions == null) return itemsFromPositions
        for (position in positions) itemsFromPositions.add(items[position].item)
        return itemsFromPositions
    }

    fun getItems(positions: List<Int>?): List<ItemType> {
        val itemsFromPositions: MutableList<ItemType> = mutableListOf()
        if (positions == null) return itemsFromPositions
        for (position in positions) itemsFromPositions.add(items[position].item)
        return itemsFromPositions
    }

    fun getPositionsByItemNames(targetNames: List<String>?): IntArray {
        if (targetNames == null || targetNames.isEmpty()) return IntArray(0)
        val matchedPositions: MutableList<Int> = mutableListOf()
        for (i in items.indices)
            for (targetName in targetNames)
                if (items[i].item.itemName == targetName)
                    matchedPositions.add(i)
        val positions = IntArray(matchedPositions.size)
        for (i in matchedPositions.indices) positions[i] = matchedPositions[i]
        return positions
    }

    fun getPositions(criteria: Predicate<ItemType>): IntArray {
        return getPositions(getItems(criteria))
    }

    fun getPositions(targets: List<ItemType>?): IntArray {
        if (targets == null || targets.isEmpty()) return IntArray(0)
        val matchedPositions: MutableList<Int> = mutableListOf()
        for (i in items.indices)
            for (target in targets)
                if (items[i].item == target)
                    matchedPositions.add(i)
        val positions = IntArray(matchedPositions.size)
        for (i in matchedPositions.indices) positions[i] = matchedPositions[i]
        return positions
    }

    fun getPositions(targets: Array<ItemType>?): IntArray {
        if (targets == null || targets.isEmpty()) return IntArray(0)
        val matchedPositions: MutableList<Int> = mutableListOf()
        for (i in items.indices)
            for (target in targets)
                if (items[i].item == target)
                    matchedPositions.add(i)
        val positions = IntArray(matchedPositions.size)
        for (i in matchedPositions.indices) positions[i] = matchedPositions[i]
        return positions
    }

    fun getPositionStrings(positions: IntArray?): String {
        var valuesString = ""
        if (positions == null) return valuesString
        for (i in positions.indices) {
            val item = getItem(positions[i]) ?: continue
            valuesString += getItemName(item)
            if (i < positions.size - 1)
                valuesString = "$valuesString, "
        }
        return valuesString
    }

    fun isItemSelected(targetPosition: Int): Boolean {
        if (selectableMode == SELECT_NONE || itemsSelection.size <= 0) return false
        for (checkedPosition in itemsSelection) {
            if (checkedPosition == targetPosition) return true
        }
        return false
    }

    fun isItemSelected(targetItem: ItemType): Boolean {
        val targetPosition = getPosition(targetItem)
        return if (targetPosition == -1) false else isItemSelected(targetPosition)
    }

    private fun clearSelectionInternally(notifyForSelectionChange: Boolean): List<Int> {
        if (!hasSelection()) return itemsSelection
        for (posToUnselect in itemsSelection)
            if (positionExists(posToUnselect))
                items[posToUnselect].isSelected = false
        if (notifyForSelectionChange) for (position in itemsSelection) notifyItemChanged(
            position + headersCount,
            false
        )
        val affectedPositions: List<Int> = itemsSelection.toList()
        itemsSelection.clear()
        return affectedPositions
    }

    private fun selectInternally(
        position: Int,
        newState: Boolean,
        notifyForSelectionChange: Boolean,
    ) {
        if (!positionExists(position)) return
        val selectedState = isItemSelected(position)
        if (selectedState == newState) return
        if (newState) {
            if (itemsSelection.contains(position)) return
            itemsSelection.add(position)
        } else {
            val positionToRemove = itemsSelection.indexOf(position)
            if (positionToRemove == -1) return
            itemsSelection.removeAt(positionToRemove)
        }
        items[position].isSelected = newState
        if (notifyForSelectionChange) notifyItemChanged(position + headersCount, false)
    }

    private fun getSelectableItem(position: Int): SelectableItem<ItemType>? {
        return if (positionExists(position)) items[position] else null
    }

    private fun wrapToSelectable(target: ItemType): SelectableItem<ItemType> {
        return SelectableItem(false, target)
    }

    private fun wrapToSelectable(target: List<ItemType>): MutableList<SelectableItem<ItemType>> {
        return target.map { return@map wrapToSelectable(it) }.toMutableList()
    }

    private fun dispatchSelectionChangedEvent() {
        for (onSelectionChangedListener in onSelectionChangedListeners)
            onSelectionChangedListener.onChanged(selectedPositionsAsArray)
    }

    private fun dispatchCollectionChangedEvent() {
        for (listener in onCollectionChangedListeners) listener.onChanged()
        onCollectionChanged()
    }
}
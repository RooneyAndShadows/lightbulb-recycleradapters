package com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.rooneyandshadows.lightbulb.commons.utils.BundleUtils
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.callbacks.EasyAdapterCollectionChangedListener
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.callbacks.EasyAdapterSelectionChangedListener
import com.github.rooneyandshadows.lightbulb.recycleradapters.implementation.HeaderViewRecyclerAdapter
import java.util.function.Predicate
import java.util.stream.Collectors

@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class EasyRecyclerAdapter<ItemType : EasyAdapterDataModel>(
    protected var selectableMode: EasyAdapterSelectableModes
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), DefaultLifecycleObserver {
    abstract val payloadClass: Class<SelectableItem<ItemType>>
    protected var items: MutableList<SelectableItem<ItemType>> = mutableListOf()
    protected var selectedPositions: MutableList<Int> = mutableListOf()
    protected var itemsComparator: EasyAdapterItemsComparator<ItemType>? = null
    protected var lifecycleOwner: LifecycleOwner? = null
        set(value) {
            field = value
            if (value != null)
                lifecycleOwner!!.lifecycle.addObserver(this)
        }
    private var recyclerView: RecyclerView? = null
    private val onSelectionChangedListeners: MutableList<EasyAdapterSelectionChangedListener> = mutableListOf()
    private val onCollectionChangedListeners: MutableList<EasyAdapterCollectionChangedListener> = mutableListOf()
    private var wrapperAdapter: HeaderViewRecyclerAdapter? = null
    val headersCount: Int
        get() = if (wrapperAdapter == null) 0 else wrapperAdapter!!.headersCount
    val footersCount: Int
        get() = if (wrapperAdapter == null) 0 else wrapperAdapter!!.footersCount
    val selectedItems: List<ItemType>
        get() {
            val selected: MutableList<ItemType> = mutableListOf()
            for (selectedPosition in selectedPositions) selected.add(items[selectedPosition].item)
            return selected
        }
    val selectedPositionsAsArray: IntArray
        get() {
            if (selectedPositions.isEmpty()) return IntArray(0)
            val selection = IntArray(selectedPositions.size)
            for (i in selectedPositions.indices)
                selection[i] = selectedPositions[i]
            return selection
        }
    val selectionString: String
        get() {
            var selectionString = ""
            val selection = selectedItems
            for (i in selection.indices) {
                selectionString += getItemName(selection[i])
                if (i < selection.size - 1)
                    selectionString = "$selectionString, "
            }
            return selectionString
        }

    fun saveAdapterState(): Bundle {
        val savedState = Bundle()
        savedState.putParcelableArrayList("ADAPTER_ITEMS", ArrayList(items))
        savedState.putIntegerArrayList("ADAPTER_SELECTION", ArrayList(selectedPositions))
        savedState.putInt("ADAPTER_SELECTION_MODE", selectableMode.value)
        return savedState
    }

    @SuppressLint("NotifyDataSetChanged")
    fun restoreAdapterState(savedState: Bundle) {
        items = BundleUtils.getParcelableArrayList("ADAPTER_ITEMS", savedState, payloadClass)!!
            .toMutableList()
        selectedPositions = savedState.getIntegerArrayList("ADAPTER_SELECTION")!!
        selectableMode = EasyAdapterSelectableModes.valueOf(savedState.getInt("ADAPTER_SELECTION_MODE"))
        notifyDataSetChanged()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDestroy(owner: LifecycleOwner) {
        clearObservableCallbacksOnCollection()
    }

    override fun getItemCount(): Int {
        return items.size
    }

    open fun getItemName(item: ItemType): String? {
        return item.itemName
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

    fun setWrapperAdapter(wrapperAdapter: HeaderViewRecyclerAdapter?) {
        this.wrapperAdapter = wrapperAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setCollection(collection: List<ItemType>) {
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
    fun appendCollection(collection: List<ItemType>?) {
        if (collection == null || collection.isEmpty()) return
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
    fun addItem(item: ItemType?) {
        if (item == null) return
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
    fun clearCollection() {
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
    fun moveItem(fromPosition: Int, toPosition: Int) {
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
    fun removeItem(targetPosition: Int) {
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
    fun removeItems(collection: List<ItemType>?) {
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
    fun clearSelection() {
        if (selectableMode == EasyAdapterSelectableModes.SELECT_NONE || !hasSelection()) return
        if (clearSelectionInternally(true).isNotEmpty()) dispatchSelectionChangedEvent()
    }

    /**
     * Used to  un/select all elements in adapter.
     *
     * @param newState - true -> select | false -> unselect.
     */
    fun selectAll(newState: Boolean) {
        if (selectableMode == EasyAdapterSelectableModes.SELECT_NONE) return
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
    fun selectItem(targetItem: ItemType, newState: Boolean, notifyChange: Boolean) {
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
    fun selectItemAt(targetPosition: Int, newState: Boolean, notifyChange: Boolean = true) {
        if (selectableMode == EasyAdapterSelectableModes.SELECT_NONE || !positionExists(targetPosition) || newState == isItemSelected(
                targetPosition
            )
        ) return
        if (selectableMode == EasyAdapterSelectableModes.SELECT_SINGLE) clearSelectionInternally(
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
    fun selectItem(targetItem: ItemType, newState: Boolean) {
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
    fun selectPositions(positions: IntArray?, newState: Boolean, incremental: Boolean) {
        if (selectableMode == EasyAdapterSelectableModes.SELECT_NONE) return
        if (positions == null || positions.isEmpty()) {
            if (!hasSelection()) return
            if (clearSelectionInternally(true).isNotEmpty()) dispatchSelectionChangedEvent()
            return
        }
        if (selectableMode == EasyAdapterSelectableModes.SELECT_SINGLE) {
            val targetPosition = positions[0]
            if (!positionExists(targetPosition) || newState == isItemSelected(targetPosition)) return
            clearSelectionInternally(true)
            selectInternally(targetPosition, newState, true)
            dispatchSelectionChangedEvent()
        }
        if (selectableMode == EasyAdapterSelectableModes.SELECT_MULTIPLE) {
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
        return selectedPositions.size > 0
    }

    fun positionExists(position: Int): Boolean {
        return items.isNotEmpty() && position >= 0 && position < items.size
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
                if (items[i] == target)
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
                if (items[i] == target)
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
        if (selectableMode == EasyAdapterSelectableModes.SELECT_NONE || selectedPositions.size <= 0) return false
        for (checkedPosition in selectedPositions) {
            if (checkedPosition == targetPosition) return true
        }
        return false
    }

    fun isItemSelected(targetItem: ItemType): Boolean {
        val targetPosition = getPosition(targetItem)
        return if (targetPosition == -1) false else isItemSelected(targetPosition)
    }

    private fun clearSelectionInternally(notifyForSelectionChange: Boolean): List<Int> {
        if (!hasSelection()) return selectedPositions
        for (posToUnselect in selectedPositions)
            if (positionExists(posToUnselect))
                items[posToUnselect].isSelected = false
        if (notifyForSelectionChange) for (position in selectedPositions) notifyItemChanged(
            position + headersCount,
            false
        )
        val affectedPositions: List<Int> = selectedPositions.toList()
        selectedPositions.clear()
        return affectedPositions
    }

    private fun selectInternally(
        position: Int,
        newState: Boolean,
        notifyForSelectionChange: Boolean
    ) {
        if (!positionExists(position)) return
        val selectedState = isItemSelected(position)
        if (selectedState == newState) return
        if (newState) {
            if (selectedPositions.contains(position)) return
            selectedPositions.add(position)
        } else {
            val positionToRemove = selectedPositions.indexOf(position)
            if (positionToRemove == -1) return
            selectedPositions.removeAt(positionToRemove)
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
        for (onSelectionChangedListener in onSelectionChangedListeners) onSelectionChangedListener.onChanged(
            selectedPositionsAsArray
        )
    }

    private fun dispatchCollectionChangedEvent() {
        for (listener in onCollectionChangedListeners) listener.onChanged()
    }
}
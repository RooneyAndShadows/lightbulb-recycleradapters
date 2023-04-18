package com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.collection

import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.*
import java.util.function.Predicate

@Suppress("unused")
@JvmSuppressWildcards
abstract class EasyRecyclerAdapterCollection<ItemType : EasyAdapterDataModel>(
    val adapter: EasyRecyclerAdapter<out EasyRecyclerAdapterCollection<ItemType>>
) : DefaultLifecycleObserver {
    private val onCollectionChangedListeners: MutableList<CollectionChangeListener> = mutableListOf()
    var lifecycleOwner: LifecycleOwner? = null
        set(value) {
            val observer = this
            field?.apply { lifecycle.removeObserver(observer) }
            field = value
            value?.apply { lifecycle.addObserver(observer) }
        }

    @Override
    override fun onDestroy(owner: LifecycleOwner) {
        clearObservableCallbacks()
    }

    fun addOnCollectionChangedListener(listener: CollectionChangeListener) {
        if (onCollectionChangedListeners.contains(listener)) return
        onCollectionChangedListeners.add(listener)
    }

    fun removeOnCollectionChangedListener(listener: CollectionChangeListener) {
        onCollectionChangedListeners.remove(listener)
    }

    /**
     * Sets collection
     *
     * @param items New collection to set
     */
    fun set(items: List<ItemType>) {
        clearObservableCallbacks()
        if (setInternally(items)) dispatchCollectionChangedEvent()
    }

    /**
     * Adds item at the end of the current collection
     *
     * @param item item to add
     */
    fun add(item: ItemType) {
        if (addInternally(item)) dispatchCollectionChangedEvent()
    }

    /**
     * Adds items at the end of the current collection
     *
     * @param items items to add
     */
    fun addAll(items: List<ItemType>) {
        if (addAllInternally(items)) dispatchCollectionChangedEvent()
    }

    /**
     * Removes item at desired position.
     *
     * @param targetPosition Desired position to be removed from the collection.
     */
    fun remove(targetPosition: Int) {
        if (!positionExists(targetPosition)) return
        clearObservableCallbacks(targetPosition)
        if (removeInternally(targetPosition)) dispatchCollectionChangedEvent()
    }

    /**
     * Removes items from the collection.
     *
     * @param targets Items to remove.
     */
    fun removeAll(targets: List<ItemType>) {
        val positionsToRemove = getPositions(targets)
        if (positionsToRemove.isEmpty()) return
        clearObservableCallbacks(positionsToRemove)
        if (removeAllInternally(targets)) dispatchCollectionChangedEvent()
    }

    /**
     * Moves item from it's position to another position.
     *
     * @param fromPosition Position of the target item
     * @param toPosition Target position
     */
    fun move(fromPosition: Int, toPosition: Int) {
        if (!positionExists(fromPosition) || !positionExists(toPosition)) return
        if (moveInternally(fromPosition, toPosition)) dispatchCollectionChangedEvent()
    }

    /**
     * Clears the collection
     */
    fun clear() {
        clearObservableCallbacks()
        if (clearInternally()) dispatchCollectionChangedEvent()
    }

    fun getItemsString(positions: IntArray): String {
        return getItems(positions).joinToString(
            transform = { item -> item.itemName },
            separator = ", "
        )
    }

    fun getItemsString(predicate: Predicate<ItemType>): String {
        return getItems(predicate).joinToString(
            transform = { item -> item.itemName },
            separator = ", "
        )
    }

    open fun getItemName(item: ItemType): String {
        return item.itemName
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

    /**
     * Sets collection
     *
     * @param collection new collection to set
     * @return 'true' if the collection has been changed
     */
    protected abstract fun setInternally(collection: List<ItemType>): Boolean

    /**
     * Adds item at the end of the current collection
     *
     * @param item item to add
     * @return 'true' if the collection has been changed
     */
    protected abstract fun addInternally(item: ItemType): Boolean

    /**
     * Adds items at the end of the current collection
     *
     * @param collection items to add
     * @return 'true' if the collection has been changed
     */
    protected abstract fun addAllInternally(collection: List<ItemType>): Boolean

    /**
     * Removes item at desired position.
     *
     * @param targetPosition Desired position to be removed from the collection.
     * @return 'true' if the collection has been changed
     */
    protected abstract fun removeInternally(targetPosition: Int): Boolean

    /**
     * Removes items from the collection.
     *
     * @param targets Items to remove.
     * @return 'true' if the collection has been changed
     */
    protected abstract fun removeAllInternally(targets: List<ItemType>): Boolean

    /**
     * Moves item from it's position to another position.
     *
     * @param fromPosition Position of the target item
     * @param toPosition Target position
     * @return 'true' if the collection has been changed
     */
    protected abstract fun moveInternally(fromPosition: Int, toPosition: Int): Boolean

    /**
     * Clears the collection
     *
     * @return 'true' if the collection has been changed
     */
    protected abstract fun clearInternally(): Boolean

    abstract fun size(): Int

    abstract fun isEmpty(): Boolean

    abstract fun positionExists(position: Int): Boolean

    abstract fun getItems(): List<ItemType>

    abstract fun getPosition(target: ItemType): Int

    abstract fun getItem(position: Int): ItemType?

    abstract fun getItems(criteria: Predicate<ItemType>): List<ItemType>

    abstract fun getItems(positions: IntArray): List<ItemType>

    abstract fun getItems(positions: List<Int>): List<ItemType>

    abstract fun getPositions(criteria: Predicate<ItemType>): IntArray

    abstract fun getPositions(targets: List<ItemType>): IntArray

    abstract fun getPositions(targets: Array<ItemType>): IntArray

    abstract fun getPositionsByItemNames(targetNames: List<String>): IntArray

    abstract fun getPositionStrings(positions: IntArray): String

    abstract fun saveState(): Bundle

    abstract fun restoreState(savedState: Bundle)

    private fun clearObservableCallbacks(position: Int) {
        getItem(position)?.apply {
            clearObservableCallbacks(this)
        }
    }

    private fun clearObservableCallbacks(item: ItemType?) {
        if (item is EasyAdapterObservableDataModel) item.clearObservableCallbacks()
    }

    private fun clearObservableCallbacks(positions: IntArray) {
        getItems().forEachIndexed { position, item ->
            if (item !is EasyAdapterObservableDataModel || !positions.contains(position)) return@forEachIndexed
            item.clearObservableCallbacks()
        }
    }

    private fun clearObservableCallbacks() {
        getItems().forEach {
            clearObservableCallbacks(it)
        }
    }

    private fun dispatchCollectionChangedEvent() {
        onCollectionChanged()
        onCollectionChangedListeners.forEach {
            it.onChanged()
        }
    }

    interface CollectionChangeListener {
        fun onChanged()
    }

    interface ItemsComparator<T : EasyAdapterDataModel?> {
        fun compareItems(oldItem: T, newItem: T): Boolean
        fun compareItemsContent(oldItem: T, newItem: T): Boolean
    }
}
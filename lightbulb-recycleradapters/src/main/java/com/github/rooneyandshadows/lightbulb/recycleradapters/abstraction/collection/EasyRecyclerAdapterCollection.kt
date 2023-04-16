package com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.collection

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterDataModel
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterObservableDataModel
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyRecyclerAdapter
import java.util.function.Predicate

@Suppress("unused")
@JvmSuppressWildcards
abstract class EasyRecyclerAdapterCollection<ItemType : EasyAdapterDataModel> : DefaultLifecycleObserver {
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

    fun addOnCollectionChangedListener(onCollectionChangedListener: CollectionChangeListener) {
        onCollectionChangedListeners.add(onCollectionChangedListener)
    }

    /**
     * Sets collection
     *
     * @param items new collection to set
     * @param adapter adapter to notify for changes
     */
    fun set(items: List<ItemType>, adapter: EasyRecyclerAdapter<ItemType>) {
        clearObservableCallbacks()
        if (setInternally(items, adapter))
            dispatchCollectionChangedEvent()
    }

    /**
     * Adds item at the end of the current collection
     *
     * @param item item to add
     * @param adapter adapter to notify for changes
     */
    fun add(item: ItemType, adapter: EasyRecyclerAdapter<ItemType>) {
        if (addInternally(item, adapter))
            dispatchCollectionChangedEvent()
    }

    /**
     * Adds items at the end of the current collection
     *
     * @param items items to add
     * @param adapter adapter to notify for changes
     */
    fun addAll(items: List<ItemType>, adapter: EasyRecyclerAdapter<ItemType>) {
        if (addAllInternally(items, adapter))
            dispatchCollectionChangedEvent()
    }

    /**
     * Removes item at desired position.
     *
     * @param targetPosition Desired position to be removed from the collection.
     * @param adapter adapter to notify for changes
     */
    fun remove(targetPosition: Int, adapter: EasyRecyclerAdapter<ItemType>) {
        if (!positionExists(targetPosition)) return
        clearObservableCallbacks(targetPosition)
        if (removeInternally(targetPosition, adapter))
            dispatchCollectionChangedEvent()
    }

    /**
     * Removes items from the collection.
     *
     * @param targets Items to remove.
     * @param adapter adapter to notify for changes
     */
    fun removeAll(targets: List<ItemType>, adapter: EasyRecyclerAdapter<ItemType>) {
        val positionsToRemove = getPositions(targets)
        if (positionsToRemove.isEmpty()) return
        clearObservableCallbacks(positionsToRemove)
        if (removeAllInternally(targets, adapter))
            dispatchCollectionChangedEvent()
    }

    /**
     * Moves item from it's position to another position.
     *
     * @param fromPosition Position of the target item
     * @param toPosition Target position
     * @param adapter adapter to notify for changes
     */
    fun move(fromPosition: Int, toPosition: Int, adapter: EasyRecyclerAdapter<ItemType>) {
        if (!positionExists(fromPosition) || !positionExists(toPosition)) return
        if (moveInternally(fromPosition, toPosition, adapter))
            dispatchCollectionChangedEvent()
    }

    /**
     * Clears the collection
     *
     * @param adapter adapter to notify for changes
     */
    fun clear(adapter: EasyRecyclerAdapter<ItemType>) {
        clearObservableCallbacks()
        if (clearInternally(adapter))
            dispatchCollectionChangedEvent()
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

    protected open fun onCollectionChanged() {
    }

    /**
     * Sets collection
     *
     * @param items new collection to set
     * @param adapter adapter to notify for changes
     * @return 'true' if the collection has been changed
     */
    protected abstract fun setInternally(
        items: List<ItemType>,
        adapter: EasyRecyclerAdapter<ItemType>,
    ): Boolean

    /**
     * Adds item at the end of the current collection
     *
     * @param item item to add
     * @param adapter adapter to notify for changes
     * @return 'true' if the collection has been changed
     */
    protected abstract fun addInternally(
        item: ItemType,
        adapter: EasyRecyclerAdapter<ItemType>,
    ): Boolean

    /**
     * Adds items at the end of the current collection
     *
     * @param items items to add
     * @param adapter adapter to notify for changes
     * @return 'true' if the collection has been changed
     */
    protected abstract fun addAllInternally(
        items: List<ItemType>,
        adapter: EasyRecyclerAdapter<ItemType>,
    ): Boolean

    /**
     * Removes item at desired position.
     *
     * @param targetPosition Desired position to be removed from the collection.
     * @param adapter adapter to notify for changes
     * @return 'true' if the collection has been changed
     */
    protected abstract fun removeInternally(
        targetPosition: Int,
        adapter: EasyRecyclerAdapter<ItemType>,
    ): Boolean

    /**
     * Removes items from the collection.
     *
     * @param targets Items to remove.
     * @param adapter adapter to notify for changes
     * @return 'true' if the collection has been changed
     */
    protected abstract fun removeAllInternally(
        targets: List<ItemType>,
        adapter: EasyRecyclerAdapter<ItemType>,
    ): Boolean

    /**
     * Moves item from it's position to another position.
     *
     * @param fromPosition Position of the target item
     * @param toPosition Target position
     * @param adapter adapter to notify for changes
     * @return 'true' if the collection has been changed
     */
    protected abstract fun moveInternally(
        fromPosition: Int,
        toPosition: Int,
        adapter: EasyRecyclerAdapter<ItemType>,
    ): Boolean

    /**
     * Clears the collection
     *
     * @param adapter adapter to notify for changes
     * @return 'true' if the collection has been changed
     */
    protected abstract fun clearInternally(
        adapter: EasyRecyclerAdapter<ItemType>,
    ): Boolean

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
}
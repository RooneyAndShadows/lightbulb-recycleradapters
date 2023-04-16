package com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.collection

import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterDataModel
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyAdapterItemsComparator
import com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction.EasyRecyclerAdapter
import java.util.function.Predicate
import java.util.stream.Collectors

@JvmSuppressWildcards
class BasicCollection<ItemType : EasyAdapterDataModel> @JvmOverloads constructor(
    private val itemsComparator: EasyAdapterItemsComparator<ItemType>? = null,
) : EasyRecyclerAdapterCollection<ItemType>() {
    private var items: MutableList<ItemType> = mutableListOf()

    override fun setInternally(items: List<ItemType>, adapter: EasyRecyclerAdapter<ItemType>): Boolean {
        TODO("Not yet implemented")
    }

    override fun addInternally(item: ItemType, adapter: EasyRecyclerAdapter<ItemType>): Boolean {
        TODO("Not yet implemented")
    }

    override fun addAllInternally(items: List<ItemType>, adapter: EasyRecyclerAdapter<ItemType>): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeInternally(targetPosition: Int, adapter: EasyRecyclerAdapter<ItemType>): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeAllInternally(targets: List<ItemType>, adapter: EasyRecyclerAdapter<ItemType>): Boolean {
        TODO("Not yet implemented")
    }

    override fun moveInternally(fromPosition: Int, toPosition: Int, adapter: EasyRecyclerAdapter<ItemType>): Boolean {
        TODO("Not yet implemented")
    }

    override fun clearInternally(adapter: EasyRecyclerAdapter<ItemType>): Boolean {
        TODO("Not yet implemented")
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
}
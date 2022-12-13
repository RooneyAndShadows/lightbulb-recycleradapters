package com.github.rooneyandshadows.lightbulb.recycleradapters.abstraction

enum class EasyAdapterSelectableModes(val value: Int) {
    SELECT_NONE(1),
    SELECT_SINGLE(2),
    SELECT_MULTIPLE(3);

    companion object {
        fun valueOf(value: Int) = values().first { it.value == value }
    }
}
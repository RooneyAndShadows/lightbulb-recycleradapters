package com.github.rooneyandshadows.lightbulb.recycleradapters;

import android.annotation.SuppressLint;
import android.os.Bundle;

import com.github.rooneyandshadows.lightbulb.recycleradapters.callbacks.EasyAdapterCollectionChangedListener;
import com.github.rooneyandshadows.lightbulb.recycleradapters.callbacks.EasyAdapterSelectionChangedListener;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import static com.github.rooneyandshadows.lightbulb.recycleradapters.EasyAdapterSelectableModes.*;

@SuppressWarnings("unused")
public abstract class EasyRecyclerAdapter<ItemType extends EasyAdapterDataModel> extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements DefaultLifecycleObserver {
    protected LifecycleOwner lifecycleOwner;
    protected ArrayList<ItemType> items = new ArrayList<>();
    protected ArrayList<Integer> selectedPositions = new ArrayList<>();
    private RecyclerView recyclerView;
    private EasyAdapterSelectableModes selectableMode;
    private final EasyAdapterItemsComparator<ItemType> itemsComparator;
    private final ArrayList<EasyAdapterSelectionChangedListener> onSelectionChangedListeners = new ArrayList<>();
    private final ArrayList<EasyAdapterCollectionChangedListener> onCollectionChangedListeners = new ArrayList<>();
    private HeaderViewRecyclerAdapter wrapperAdapter;


    public EasyRecyclerAdapter(EasyAdapterConfiguration<ItemType> adapterConfig) {
        if (adapterConfig == null)
            adapterConfig = new EasyAdapterConfiguration<>();
        setHasStableIds(adapterConfig.isUseStableIds());
        this.selectableMode = adapterConfig.getSelectableMode();
        this.itemsComparator = adapterConfig.getComparator();
        if (adapterConfig.getLifecycleOwner() != null) {
            lifecycleOwner = adapterConfig.getLifecycleOwner();
            lifecycleOwner.getLifecycle().addObserver(this);
        }
    }

    public EasyRecyclerAdapter() {
        this(new EasyAdapterConfiguration<>());
    }

    public Bundle saveAdapterState() {
        Bundle savedState = new Bundle();
        savedState.putParcelableArrayList("ADAPTER_ITEMS", items);
        savedState.putIntegerArrayList("ADAPTER_SELECTION", selectedPositions);
        savedState.putInt("ADAPTER_SELECTION_MODE", selectableMode.getValue());
        return savedState;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void restoreAdapterState(Bundle savedState) {
        items = savedState.getParcelableArrayList("ADAPTER_ITEMS");
        selectedPositions = savedState.getIntegerArrayList("ADAPTER_SELECTION");
        selectableMode = valueOf(savedState.getInt("ADAPTER_SELECTION_MODE"));
        notifyDataSetChanged();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        clearObservableCallbacksOnCollection();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public String getItemName(ItemType item) {
        return item.getItemName();
    }

    public void addOnSelectionChangedListener(EasyAdapterSelectionChangedListener onSelectionChangedListener) {
        this.onSelectionChangedListeners.add(onSelectionChangedListener);
    }

    public void removeOnSelectionChangedListener(EasyAdapterSelectionChangedListener onSelectionChangedListener) {
        this.onSelectionChangedListeners.remove(onSelectionChangedListener);
    }

    public void addOrReplaceSelectionChangedListener(EasyAdapterSelectionChangedListener onSelectionChangedListener) {
        this.onSelectionChangedListeners.remove(onSelectionChangedListener);
        this.onSelectionChangedListeners.add(onSelectionChangedListener);
    }

    public void addOnCollectionChangedListener(EasyAdapterCollectionChangedListener onCollectionChangedListener) {
        this.onCollectionChangedListeners.add(onCollectionChangedListener);
    }

    protected final void clearObservableCallbacks(int position) {
        if (positionExists(position))
            clearObservableCallbacks(getItem(position));
    }

    protected final void clearObservableCallbacks(ItemType item) {
        if (item instanceof EasyAdapterObservableDataModel)
            ((EasyAdapterObservableDataModel) item).clearObservableCallbacks();
    }

    protected final void clearObservableCallbacksOnCollection() {
        for (ItemType item : items) clearObservableCallbacks(item);
    }

    public void setWrapperAdapter(HeaderViewRecyclerAdapter wrapperAdapter) {
        this.wrapperAdapter = wrapperAdapter;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setCollection(List<ItemType> collection) {
        boolean selectionChanged;
        if (hasStableIds() || itemsComparator == null) {
            selectionChanged = clearSelectionInternally(false).size() > 0;
            clearObservableCallbacksOnCollection();
            items.clear();
            items.addAll(collection);
            notifyDataSetChanged();
        } else {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new EasyAdapterDiffUtilCallback<>(items, collection, itemsComparator), true);
            selectionChanged = clearSelectionInternally(false).size() > 0;
            clearObservableCallbacksOnCollection();
            items.clear();
            items.addAll(collection);
            diffResult.dispatchUpdatesTo(this);
            recyclerView.invalidateItemDecorations();
        }
        if (selectionChanged)
            dispatchSelectionChangedEvent();
        dispatchCollectionChangedEvent();
    }

    /**
     * Used to add new items in the adapter.
     *
     * @param collectionToAdd - items to be added.
     */
    public void appendCollection(List<ItemType> collectionToAdd) {
        if (collectionToAdd == null || collectionToAdd.size() <= 0)
            return;
        int positonStart = items.size() + 1;
        int newItemsCount = collectionToAdd.size();
        items.addAll(collectionToAdd);
        notifyItemRangeInserted(positonStart, newItemsCount);
        dispatchCollectionChangedEvent();
    }

    /**
     * Used to add new item in the adapter.
     *
     * @param item - item to be added.
     */
    public void addItem(ItemType item) {
        if (item == null)
            return;
        items.add(item);
        notifyItemInserted(items.size() - 1);
        dispatchCollectionChangedEvent();
    }

    /**
     * Used to remove all items in the adapter.
     */
    public void clearCollection() {
        if (!hasItems())
            return;
        boolean selectionChanged = hasSelection();
        clearSelectionInternally(false);
        clearObservableCallbacksOnCollection();
        int notifyRangeStart = 0;
        int notifyRangeEnd = items.size() - 1;
        items.clear();
        notifyItemRangeRemoved(notifyRangeStart, notifyRangeEnd);
        if (selectionChanged)
            dispatchSelectionChangedEvent();
        dispatchCollectionChangedEvent();
    }

    /**
     * Used to move item in the adapter.
     *
     * @param fromPosition - from which position to move.
     * @param toPosition   - new position.
     */
    public void moveItem(int fromPosition, int toPosition) {
        if (!positionExists(fromPosition) || !positionExists(toPosition))
            return;
        ItemType movingItem = getItem(fromPosition);
        items.remove(fromPosition);
        items.add(toPosition, movingItem);
        notifyItemMoved(fromPosition, toPosition);
    }

    /**
     * Used to remove item corresponding to particular position in the adapter.
     *
     * @param targetPosition - position to be removed.
     */
    public void removeItem(int targetPosition) {
        if (!positionExists(targetPosition))
            return;
        clearObservableCallbacks(targetPosition);
        boolean selectionChanged = isItemSelected(targetPosition);
        selectInternally(targetPosition, false, false);
        items.remove(targetPosition);
        notifyItemRemoved(targetPosition);
        if (selectionChanged)
            dispatchSelectionChangedEvent();
        dispatchCollectionChangedEvent();
    }

    /**
     * Used to remove items from the adapter.
     *
     * @param collection - items to be removed.
     */
    public void removeItems(List<ItemType> collection) {
        int[] positionsToRemove = getPositions(collection);
        if (positionsToRemove.length <= 0)
            return;
        boolean selectionChanged = false;
        for (int position : positionsToRemove) {
            if (!positionExists(position))
                continue;
            clearObservableCallbacks(position);
            if (isItemSelected(position))
                selectionChanged = true;
        }
        for (int position : positionsToRemove)
            selectInternally(position, false, false);
        items.removeAll(collection);
        for (int position : positionsToRemove)
            notifyItemRemoved(position);
        if (selectionChanged)
            dispatchSelectionChangedEvent();
        dispatchCollectionChangedEvent();
    }

    /**
     * Used to clear deselect all elements in adapter.
     */
    public void clearSelection() {
        if (selectableMode.equals(SELECT_NONE) || !hasSelection())
            return;
        if (clearSelectionInternally(true).size() > 0)
            dispatchSelectionChangedEvent();
    }

    /**
     * Used to  un/select all elements in adapter.
     *
     * @param newState - true -> select | false -> unselect.
     */
    public void selectAll(boolean newState) {
        if (selectableMode.equals(SELECT_NONE))
            return;
        boolean changed = false;
        for (int positionToSelect = 0; positionToSelect < items.size(); positionToSelect++) {
            boolean isItemSelected = isItemSelected(positionToSelect);
            if (newState == isItemSelected)
                continue;
            changed = true;
            selectInternally(positionToSelect, newState, true);
        }
        if (changed)
            dispatchSelectionChangedEvent();
    }


    /**
     * Used to un/select particular item in the adapter.
     *
     * @param targetItem   - item to be un/selected
     * @param newState     - true -> select | false -> unselect.
     * @param notifyChange - whether to notify registered observers in case of change.
     */
    public void selectItem(ItemType targetItem, boolean newState, boolean notifyChange) {
        //int index = getPosition(item);
        int index = items.indexOf(targetItem);
        if (index == -1)
            return;
        selectItemAt(index, newState, notifyChange);
    }

    /**
     * Used to un/select item corresponding to particular position in the adapter.
     *
     * @param targetPosition - position to be un/selected
     * @param newState       - true -> select | false -> unselect.
     * @param notifyChange   - whether to notify registered observers in case of change.
     */
    public void selectItemAt(int targetPosition, boolean newState, boolean notifyChange) {
        if (selectableMode.equals(SELECT_NONE) || !positionExists(targetPosition) || newState == isItemSelected(targetPosition))
            return;
        if (selectableMode.equals(SELECT_SINGLE))
            clearSelectionInternally(true);
        selectInternally(targetPosition, newState, notifyChange);
        dispatchSelectionChangedEvent();
    }

    /**
     * Used to un/select particular item in the adapter.
     * Registered observers are notified automatically in case of change.
     *
     * @param targetItem - item to be un/selected
     * @param newState   - true -> select | false -> unselect.
     */
    public void selectItem(ItemType targetItem, boolean newState) {
        //int index = getPosition(item);
        int index = items.indexOf(targetItem);
        if (index == -1)
            return;
        selectItemAt(index, newState, true);
    }

    /**
     * Used to un/select item corresponding to particular position in the adapter.
     * Registered observers are notified automatically in case of change.
     *
     * @param targetPosition - position to be un/selected
     * @param newState       - true -> select | false -> unselect.
     */
    public void selectItemAt(int targetPosition, boolean newState) {
        selectItemAt(targetPosition, newState, true);
    }

    /**
     * Used to select items corresponding to array of positions in the adapter.
     *
     * @param positions - positions to be selected
     */
    public void selectPositions(int[] positions) {
        if (selectableMode.equals(SELECT_NONE))
            return;
        if (positions == null || positions.length <= 0) {
            if (!hasSelection())
                return;
            if (clearSelectionInternally(true).size() > 0)
                dispatchSelectionChangedEvent();
            return;
        }
        if (selectableMode.equals(SELECT_SINGLE)) {
            int targetPosition = positions[0];
            if (!positionExists(targetPosition) || isItemSelected(targetPosition))
                return;
            clearSelectionInternally(true);
            selectInternally(targetPosition, true, true);
            dispatchSelectionChangedEvent();
        }
        if (selectableMode.equals(SELECT_MULTIPLE)) {
            boolean selectionChanged = false;
            for (int targetPosition : positions) {
                if (!positionExists(targetPosition) || isItemSelected(targetPosition))
                    continue;
                selectionChanged = true;
                selectInternally(targetPosition, true, true);
            }
            if (selectionChanged)
                dispatchSelectionChangedEvent();
        }
    }

    public EasyAdapterSelectableModes getSelectableMode() {
        return selectableMode;
    }

    public int getHeadersCount() {
        return wrapperAdapter == null ? 0 : wrapperAdapter.getHeadersCount();
    }

    public int getFootersCount() {
        return wrapperAdapter == null ? 0 : wrapperAdapter.getFootersCount();
    }

    public boolean hasItems() {
        return items != null && items.size() > 0;
    }

    public boolean hasSelection() {
        return selectedPositions.size() > 0;
    }

    public boolean positionExists(int position) {
        return items != null && !items.isEmpty() && position >= 0 && position < items.size();
    }

    public int getPosition(ItemType target) {
        return items.indexOf(target);
    }

    public ItemType getItem(int position) {
        return items.get(position);
    }

    public List<ItemType> getItems() {
        return items;
    }

    public List<ItemType> getItems(Predicate<ItemType> criteria) {
        return items.stream()
                .filter(criteria).collect(Collectors.toList());
    }

    public List<ItemType> getItems(int[] positions) {
        List<ItemType> itemsFromPositions = new ArrayList<>();
        if (positions == null)
            return itemsFromPositions;
        for (int position : positions)
            itemsFromPositions.add(items.get(position));
        return itemsFromPositions;
    }

    public List<ItemType> getItems(List<Integer> positions) {
        List<ItemType> itemsFromPositions = new ArrayList<>();
        if (positions == null)
            return itemsFromPositions;
        for (int position : positions)
            itemsFromPositions.add(items.get(position));
        return itemsFromPositions;
    }

    public List<ItemType> getSelectedItems() {
        List<ItemType> selected = new ArrayList<>();
        for (Integer selectedPosition : selectedPositions)
            selected.add(items.get(selectedPosition));
        return selected;
    }

    public List<Integer> getSelectedPositions() {
        return selectedPositions;
    }

    public int[] getSelectedPositionsAsArray() {
        if (selectedPositions.isEmpty())
            return new int[0];
        int[] selection = new int[selectedPositions.size()];
        for (int i = 0; i < selectedPositions.size(); i++) {
            selection[i] = selectedPositions.get(i);
        }
        return selection;
    }

    public int[] getPositionsByItemNames(List<String> targetNames) {
        if (targetNames == null || targetNames.size() <= 0)
            return new int[0];
        List<Integer> matchedPositions = new ArrayList<>();
        for (int i = 0; i < items.size(); i++)
            for (String targetName : targetNames)
                if (items.get(i).getItemName().equals(targetName))
                    matchedPositions.add(i);
        int[] positions = new int[matchedPositions.size()];
        for (int i = 0; i < matchedPositions.size(); i++)
            positions[i] = matchedPositions.get(i);
        return positions;
    }

    public int[] getPositions(Predicate<ItemType> criteria) {
        return getPositions(getItems(criteria));
    }

    public int[] getPositions(List<ItemType> targets) {
        if (targets == null || targets.size() <= 0)
            return new int[0];
        List<Integer> matchedPositions = new ArrayList<>();
        for (int i = 0; i < items.size(); i++)
            for (ItemType target : targets)
                if (items.get(i).equals(target))
                    matchedPositions.add(i);
        int[] positions = new int[matchedPositions.size()];
        for (int i = 0; i < matchedPositions.size(); i++)
            positions[i] = matchedPositions.get(i);
        return positions;
    }

    public int[] getPositions(ItemType[] targets) {
        if (targets == null || targets.length <= 0)
            return new int[0];
        List<Integer> matchedPositions = new ArrayList<>();
        for (int i = 0; i < items.size(); i++)
            for (ItemType target : targets)
                if (items.get(i).equals(target))
                    matchedPositions.add(i);
        int[] positions = new int[matchedPositions.size()];
        for (int i = 0; i < matchedPositions.size(); i++)
            positions[i] = matchedPositions.get(i);
        return positions;
    }

    public String getPositionStrings(int[] positions) {
        String valuesString = "";
        if (positions == null)
            return valuesString;
        for (int i = 0; i < positions.length; i++) {
            ItemType item = getItem(positions[i]);
            valuesString = valuesString.concat(getItemName(item));
            if (i < positions.length - 1) valuesString = valuesString.concat(", ");
        }
        return valuesString;
    }

    public String getSelectionString() {
        String selectionString = "";
        List<ItemType> selection = getSelectedItems();
        for (int i = 0; i < selection.size(); i++) {
            selectionString = selectionString.concat(getItemName(selection.get(i)));
            if (i < selection.size() - 1) selectionString = selectionString.concat(", ");
        }
        return selectionString;
    }

    public boolean isItemSelected(int targetPosition) {
        if (selectableMode.equals(SELECT_NONE) || selectedPositions.size() <= 0)
            return false;
        for (Integer checkedPosition : selectedPositions) {
            if (checkedPosition == targetPosition)
                return true;
        }
        return false;
    }

    public boolean isItemSelected(ItemType targetItem) {
        int targetPosition = getPosition(targetItem);
        if (targetPosition == -1)
            return false;
        return isItemSelected(targetPosition);
    }

    private List<Integer> clearSelectionInternally(boolean notifyForSelectionChange) {
        if (!hasSelection())
            return new ArrayList<>();
        for (int posToUnselect : selectedPositions)
            if (positionExists(posToUnselect))
                items.get(posToUnselect).setSelected(false);
        if (notifyForSelectionChange)
            for (int position : selectedPositions)
                notifyItemChanged(position + getHeadersCount());
        List<Integer> affectedPositions = new ArrayList<>(selectedPositions);
        selectedPositions.clear();
        return affectedPositions;
    }

    private void selectInternally(int position, boolean newState, boolean notifyForSelectionChange) {
        if (!positionExists(position))
            return;
        boolean selectedState = isItemSelected(position);
        if (selectedState == newState)
            return;
        if (newState) {
            if (selectedPositions.contains(position))
                return;
            selectedPositions.add(position);
        } else {
            int positionToRemove = selectedPositions.indexOf(position);
            if (positionToRemove == -1)
                return;
            selectedPositions.remove(positionToRemove);
        }
        items.get(position).setSelected(newState);
        if (notifyForSelectionChange)
            notifyItemChanged(position + getHeadersCount());
    }

    private void dispatchSelectionChangedEvent() {
        for (EasyAdapterSelectionChangedListener onSelectionChangedListener : onSelectionChangedListeners)
            onSelectionChangedListener.onChanged(getSelectedPositionsAsArray());
    }

    private void dispatchCollectionChangedEvent() {
        for (EasyAdapterCollectionChangedListener listener : onCollectionChangedListeners)
            listener.onChanged();
    }
}
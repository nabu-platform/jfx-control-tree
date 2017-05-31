package be.nabu.jfx.control.tree;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;

public class TreeSelectionModel<T> extends MultipleSelectionModel<TreeCell<T>> {

	private ObservableList<TreeCell<T>> selectedItems = FXCollections.observableArrayList();
	
	@Override
	public ObservableList<Integer> getSelectedIndices() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ObservableList<TreeCell<T>> getSelectedItems() {
		return selectedItems;
	}

	@Override
	public void selectAll() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void selectFirst() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void selectIndices(int arg0, int... arg1) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void selectLast() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearAndSelect(int arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clearSelection() {
		selectedItems.clear();
	}

	@Override
	public void clearSelection(int arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty() {
		return selectedItems.isEmpty();
	}

	@Override
	public boolean isSelected(int arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void select(int arg0) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void select(TreeCell<T> item) {
		// always remove the item first to make sure we trigger any listeners
		selectedItems.remove(item);
		if (getSelectionMode() == SelectionMode.SINGLE)
			clearSelection();
		selectedItems.add(item);
		// make sure we unset the parent first to trigger listeners
		super.setSelectedItem(null);
		// then reset
		super.setSelectedItem(item);
	}

	@Override
	public void selectNext() {
		if (selectedItems.size() == 1) {
			TreeCell<T> next = getNext(selectedItems.get(0), false);
			if (next != null) {
				TreeCell<?> previousSelection = selectedItems.get(0);
				clearSelection();
				select(next);
				if (next.getTree().isAutoscrollOnSelect()) {
					JFXUtils.scrollDown(previousSelection);
				}
			}
		}
	}
	@Override
	public void selectPrevious() {
		if (selectedItems.size() == 1) {
			TreeCell<T> previous = getPrevious(selectedItems.get(0), false);
			if (previous != null) {
				TreeCell<?> previousSelection = selectedItems.get(0);
				clearSelection();
				select(previous);
				if (previous.getTree().isAutoscrollOnSelect()) {
					JFXUtils.scrollUp(previousSelection);
				}
			}
		}
	}
	
	/**
	 * If the forceDepth boolean is set the code will forcefully remain at the same depth instead of switching depth
	 * @param current
	 * @param forceDepth
	 * @return
	 */
	private TreeCell<T> getNext(TreeCell<T> current, boolean forceDepth) {
		// not a leaf and it's expanded, go inside it
		if (!forceDepth && !current.getItem().leafProperty().getValue() && current.getItem().getChildren().size() > 0 && current.expandedProperty().getValue())
			return current.getCell(current.getItem().getChildren().get(0));
		else {
			TreeCell<T> parent = current.getParent();
			if (parent != null) {
				TreeItem<T> parentItem = parent.getItem();
				int index = parentItem.getChildren().indexOf(current.getItem());
				if (index < 0)
					throw new IllegalStateException("Could not find the item in its parent");
				// you have already selected the last element in the list, select a level deeper
				if (index >= parentItem.getChildren().size() - 1)
					return getNext(parent, true);
				else
					return parent.getCell(parentItem.getChildren().get(index + 1));
			}
			else
				return null;
		}
	}
	
	private TreeCell<T> getPrevious(TreeCell<T> current, boolean forceDepth) {
		TreeCell<T> parent = current.getParent();
		if (parent != null) {
			TreeItem<T> parentItem = parent.getItem();
			int index = parentItem.getChildren().indexOf(current.getItem());
			if (index < 0)
				throw new IllegalStateException("Could not find the item in its parent");
			// you have already selected the last element in the list, select a level deeper
			if (index == 0)
				return parent;
			else {
				TreeCell<T> previous = parent.getCell(parentItem.getChildren().get(index - 1));
				if (!forceDepth && !previous.getItem().leafProperty().getValue() && previous.getItem().getChildren().size() > 0 && previous.expandedProperty().getValue())
					return previous.getCell(previous.getItem().getChildren().get(previous.getItem().getChildren().size() - 1));
				else
					return previous;
			}
		}
		else
			return null;
	}
}

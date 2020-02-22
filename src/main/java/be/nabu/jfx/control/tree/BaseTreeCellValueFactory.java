package be.nabu.jfx.control.tree;

import be.nabu.jfx.control.tree.Tree.CellDescriptor;
import javafx.util.Callback;

public class BaseTreeCellValueFactory<T> implements Callback<TreeItem<T>, TreeCellValue<T>> {

	private Marshallable<T> marshallable;
	private Updateable<T> updateable;
	private CellDescriptor cellDescriptor;
	
	public BaseTreeCellValueFactory(Marshallable<T> marshallable, Updateable<T> updateable, CellDescriptor cellDescriptor) {
		this.marshallable = marshallable;
		this.updateable = updateable;
		this.cellDescriptor = cellDescriptor;
	}
	
	@Override
	public TreeCellValue<T> call(TreeItem<T> item) {
		return new TreeCellValueLabel<T>(item, marshallable, updateable, cellDescriptor);
	}
	
}

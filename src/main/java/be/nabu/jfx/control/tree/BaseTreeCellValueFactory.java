package be.nabu.jfx.control.tree;

import javafx.util.Callback;

public class BaseTreeCellValueFactory<T> implements Callback<TreeItem<T>, TreeCellValue<T>> {

	private Marshallable<T> marshallable;
	private Updateable<T> updateable;
	
	public BaseTreeCellValueFactory(Marshallable<T> marshallable) {
		this(marshallable, null);
	}
	
	public BaseTreeCellValueFactory(Marshallable<T> marshallable, Updateable<T> updateable) {
		this.marshallable = marshallable;
		this.updateable = updateable;
	}
	
	@Override
	public TreeCellValue<T> call(TreeItem<T> item) {
		return new TreeCellValueLabel<T>(item, marshallable, updateable);
	}
	
}

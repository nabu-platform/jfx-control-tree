package be.nabu.jfx.control.tree;

import javafx.beans.property.BooleanProperty;

public interface DisablableTreeItem<T> extends TreeItem<T> {
	public BooleanProperty disableProperty();
}

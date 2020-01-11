package be.nabu.jfx.control.tree;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;

public interface TreeItem<T> extends Refreshable {
	public BooleanProperty editableProperty();
	public BooleanProperty leafProperty();
	public ObjectProperty<T> itemProperty();
	public ObjectProperty<Node> graphicProperty();
	public ObservableList<TreeItem<T>> getChildren();
	public TreeItem<T> getParent();
	
	/**
	 * This allows you to use paths to get items
	 */
	public String getName();
	
	public default ReadOnlyBooleanProperty renameableProperty() {
		return editableProperty();
	}
}

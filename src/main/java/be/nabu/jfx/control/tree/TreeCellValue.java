package be.nabu.jfx.control.tree;

import javafx.beans.property.ObjectProperty;
import javafx.scene.layout.Region;

public interface TreeCellValue<T> {
	public Region getNode();
	public ObjectProperty<TreeCell<T>> cellProperty();
	public void refresh();
}

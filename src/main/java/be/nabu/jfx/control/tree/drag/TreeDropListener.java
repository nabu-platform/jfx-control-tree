package be.nabu.jfx.control.tree.drag;

import javafx.scene.input.TransferMode;
import be.nabu.jfx.control.tree.TreeCell;

public interface TreeDropListener<T> {
	public boolean canDrop(String dataType, TreeCell<T> target, TreeCell<?> dragged, TransferMode transferMode);
	public void drop(String dataType, TreeCell<T> target, TreeCell<?> dragged, TransferMode transferMode);
}

package be.nabu.jfx.control.tree;

import java.util.List;

import javafx.scene.control.ContextMenu;

public interface ContextMenuProvider<T> {
	public ContextMenu getMenu(List<TreeCell<T>> selectedItems);
}

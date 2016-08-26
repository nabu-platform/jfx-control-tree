package be.nabu.jfx.control.tree.drag;

import javafx.scene.input.TransferMode;
import be.nabu.jfx.control.tree.TreeCell;

public interface TreeDragListener<T> {
	public TransferMode getTransferMode();
	public boolean canDrag(TreeCell<T> item);
	public String getDataType(TreeCell<T> item);
	public void drag(TreeCell<T> cell);
	public void stopDrag(TreeCell<T> cell, boolean droppedSuccessfully);
	public default String getAsText(TreeCell<T> cell) { return null; }
}

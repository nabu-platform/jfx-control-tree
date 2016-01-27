package be.nabu.jfx.control.tree;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

public class JFXUtils {

	public static void focusInScroll(ScrollPane pane, TreeCell<?> cell, boolean horizontal) {
		double width = pane.getContent().getBoundsInLocal().getWidth();
		double height = pane.getContent().getBoundsInLocal().getHeight();

		double x = cell.treeLayoutXProperty().get() + (pane.getHvalue() * width / 2);
		double y = cell.treeLayoutYProperty().get() + (pane.getVvalue() * height / 2);
		
		pane.setVvalue(y / height);
		if (horizontal) {
			pane.setHvalue(width / x);
		}
	}
	
	public static void scrollDown(TreeCell<?> belowCell) {
		ScrollPane parent = getScrollParent(belowCell.getNode());
		if (parent != null) {
			double height = parent.getContent().getBoundsInLocal().getHeight();	
			parent.setVvalue(parent.getVvalue() + (belowCell.getNode().getHeight() / height));
		}
	}
	
	public static void scrollUp(TreeCell<?> aboveCell) {
		ScrollPane parent = getScrollParent(aboveCell.getNode());
		if (parent != null) {
			double height = parent.getContent().getBoundsInLocal().getHeight();	
			parent.setVvalue(parent.getVvalue() - (aboveCell.getNode().getHeight() / height));
		}
	}
	
	public static ScrollPane getScrollParent(Node node) {
		while (node.getParent() != null && !(node.getParent() instanceof ScrollPane)) {
			node = node.getParent();
		}
		return (ScrollPane) node.getParent();
	}
	
}

/*
* Copyright (C) 2015 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.jfx.control.tree;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;

public class JFXUtils {

	public static void focusInScroll(ScrollPane pane, TreeCell<?> cell, boolean horizontal, boolean focus) {
		// if we need to expand to focus, we want the full expansion (and laying out of it) to take place before we start calculating positions
		// otherwise we get the "old" pre-expansion positions
		// we force a layout to make sure the positioning is ok!
		// we fixed vertical scrolling but not yet horizontal scrolling...
		pane.layout();
		Platform.runLater(new Runnable() {
			public void run() {
				double width = pane.getContent().getBoundsInLocal().getWidth();
				double height = pane.getContent().getBoundsInLocal().getHeight();
				
				double x = cell.treeLayoutXProperty().get() + (pane.getHvalue() * width / 2);
				double y = cell.treeLayoutYProperty().get() + (pane.getVvalue() * height / 2);
				
//				double contentHeight = cell.getTree().getHeight();
				double contentHeight = pane.getContent().getBoundsInLocal().getHeight();
				double scrollHeight = pane.getHeight();
				// we don't want to jump _exactly_ to the item, as it would be at the very edge
				// instead we want to leave some room above and below it, we try to center it (more or less)
				double offsetWithinPane = scrollHeight / 2;
				double cellY = cell.treeLayoutYProperty().get();
				
				// when the scroll is at max (1), the top y position is treeHeight - scrollHeight
				if (contentHeight <= scrollHeight) {
					pane.setVvalue(0);
				}
				else {
					pane.setVvalue(Math.max(0, (cellY - offsetWithinPane) / (contentHeight - scrollHeight)));
				}
				
				if (horizontal) {
					pane.setHvalue(width / x);
				}
				// we have to focus _after_ the vvalue is set
				// if you put a focus on the tree before we update the vvalue, the setVvalue does not appear to do anything...everything else works
				if (focus) {
					// focus on the stage
					cell.getTree().getScene().getWindow().requestFocus();
					// focus on the tree
					cell.getTree().requestFocus();
				}
			}
		});
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

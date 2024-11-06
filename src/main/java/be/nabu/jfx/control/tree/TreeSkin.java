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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.SkinBase;

public class TreeSkin<T> extends SkinBase<Tree<T>> {

	public TreeSkin(Tree<T> tree) {
		super(tree);
		initialize();
	}

	private void initialize() {
		// initial
		refresh();
		// listen for updates
		getSkinnable().rootProperty().addListener(new ChangeListener<TreeItem<?>>() {
			@Override
			public void changed(ObservableValue<? extends TreeItem<?>> arg0, TreeItem<?> oldRoot, TreeItem<?> newRoot) {
				refresh();
			}
		});
	}
	
	private void refresh() {
		if (getSkinnable() != null && getSkinnable().getRootCell() != null && getSkinnable().getRootCell().getNode() != null) {
			getChildren().clear();
			getChildren().add(getSkinnable().getRootCell().getNode());
		}
	}
}

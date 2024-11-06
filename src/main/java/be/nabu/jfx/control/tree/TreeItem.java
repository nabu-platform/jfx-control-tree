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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
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
	
	// A tooltip for the item
	public default ReadOnlyStringProperty tooltipProperty() {
		return null;
	}
}

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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.util.Callback;

public class NameTreeCellValueFactory<T> implements Callback<TreeItem<T>, TreeCellValue<T>> {
	
	@Override
	public TreeCellValue<T> call(TreeItem<T> arg0) {
		return new TreeCellValue<T>() {
			private ObjectProperty<TreeCell<T>> cell = new SimpleObjectProperty<TreeCell<T>>(); {
				cell.addListener(new ChangeListener<TreeCell<T>>() {
					@Override
					public void changed(ObservableValue<? extends TreeCell<T>> arg0, TreeCell<T> arg1, TreeCell<T> cell) {
						label.setText(cell.getItem().getName());
					}
				});
			}
			private Label label = new Label();
														
			@Override
			public Region getNode() {
				return label;
			}
			@Override
			public ObjectProperty<TreeCell<T>> cellProperty() {
				return cell;
			}
			@Override
			public void refresh() {
				// do nothing
			}
		};
	}
}

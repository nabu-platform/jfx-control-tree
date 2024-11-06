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

import be.nabu.jfx.control.tree.Tree.CellDescriptor;
import javafx.util.Callback;

public class BaseTreeCellValueFactory<T> implements Callback<TreeItem<T>, TreeCellValue<T>> {

	private Marshallable<T> marshallable;
	private Updateable<T> updateable;
	private CellDescriptor cellDescriptor;
	
	public BaseTreeCellValueFactory(Marshallable<T> marshallable, Updateable<T> updateable, CellDescriptor cellDescriptor) {
		this.marshallable = marshallable;
		this.updateable = updateable;
		this.cellDescriptor = cellDescriptor;
	}
	
	@Override
	public TreeCellValue<T> call(TreeItem<T> item) {
		return new TreeCellValueLabel<T>(item, marshallable, updateable, cellDescriptor);
	}
	
}

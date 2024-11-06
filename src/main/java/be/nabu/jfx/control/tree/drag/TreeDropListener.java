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

package be.nabu.jfx.control.tree.drag;

import javafx.scene.input.TransferMode;
import be.nabu.jfx.control.tree.TreeCell;

public interface TreeDropListener<T> {
	public boolean canDrop(String dataType, TreeCell<T> target, TreeCell<?> dragged, TransferMode transferMode);
	public void drop(String dataType, TreeCell<T> target, TreeCell<?> dragged, TransferMode transferMode);
}

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
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

public class TreeCellValueLabel<T> implements TreeCellValue<T> {

	private Marshallable<T> marshallable;
	private Updateable<T> updateable;
	
	private TreeItem<T> item;
	
	private HBox node = new HBox();
	private Label label = new Label(), descriptionLabel = new Label();	// if we put a graphic on the original label, it sometimes gets messed up for reasons unknown
	private TextField textField = new TextField();
	private boolean isEditing = false;
	private ObjectProperty<TreeCell<T>> cell = new SimpleObjectProperty<TreeCell<T>>();
	private CellDescriptor cellDescriptor;
	
	TreeCellValueLabel(final TreeItem<T> item, Marshallable<T> marshallable, Updateable<T> updateable, CellDescriptor cellDescriptor) {
		this.item = item;
		this.marshallable = marshallable;
		this.updateable = updateable;
		this.cellDescriptor = cellDescriptor;
		
		this.item.itemProperty().addListener(new ChangeListener<T>() {
			@Override
			public void changed(ObservableValue<? extends T> arg0, T oldValue, T newValue) {
				label.setText(TreeCellValueLabel.this.marshallable.marshal(newValue));
			}
		});
		// initial load for label
		refresh();
		
		// add the label to the hbox
		node.getChildren().addAll(label, descriptionLabel);
		// added after the fact
		enrich();

		// if the property can be updated, enable F2 on label
		if (updateable != null) {
			node.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
				@Override
				public void handle(KeyEvent event) {
					// check the editable property every time, someone might turn it off
					if (item.renameableProperty().get() && !isEditing && event.getCode() == KeyCode.F2 && !event.isMetaDown()) {
						edit();
						event.consume();
					}
				}
			});
		}
		
		textField.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (isEditing && event.getCode() == KeyCode.ENTER) {
					commitEdit();
					event.consume();
				}
				else if (isEditing && event.getCode() == KeyCode.ESCAPE) {
					undoEdit();
					event.consume();
				}
			}
		});
		textField.getStyleClass().add("editableTextfield");
	}
	
	public void edit() {
		if (updateable != null) {
			textField.textProperty().setValue(marshallable.marshal(item.itemProperty().get()));
			node.getChildren().clear();
			node.getChildren().add(textField);
			textField.requestFocus();
			isEditing = true;
			textField.selectAll();
		}
	}
	
	public void commitEdit() {
		if (updateable != null) {
			try {
				item.itemProperty().set(
					updateable.update(cell.get(), textField.getText().isEmpty() ? null : textField.getText())
				);
				refresh();
				node.getChildren().clear();
				node.getChildren().addAll(label, descriptionLabel);
				enrich();
				isEditing = false;
				cellProperty().getValue().select();
				cellProperty().get().refresh();
			}
			catch (RuntimeException e) {
				undoEdit();
			}
		}
	}
	
	public void undoEdit() {
		node.getChildren().clear();
		node.getChildren().addAll(label, descriptionLabel);
		enrich();
		cellProperty().getValue().select();
		isEditing = false;
	}
	
	@Override
	public Region getNode() {
		return node;
	}

	@Override
	public ObjectProperty<TreeCell<T>> cellProperty() {
		return cell;
	}

	private void enrich() {
		if (cellDescriptor != null) {
			Node suffix = cellDescriptor.suffix(this.item.itemProperty().get());
			if (suffix != null) {
				node.getChildren().add(suffix);
			}
		}
	}
	
	@Override
	public void refresh() {
		if (this.item.itemProperty().isNotNull().getValue()) {
			label.setText(this.marshallable.marshal(item.itemProperty().getValue()));
			if (marshallable instanceof MarshallableWithDescription) {
				String description = ((MarshallableWithDescription<T>) marshallable).getDescription(item.itemProperty().getValue());
				if (description != null && !description.trim().isEmpty()) {
					if (cellDescriptor != null) {
						cellDescriptor.describe(descriptionLabel, description);
					}
					else {
						label.setTooltip(new Tooltip(description));
					}
//					label.setText(label.getText() + " (" + description + ")");
				}
			}
		}
		else {
			label.setText("");
		}
	}
}

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

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

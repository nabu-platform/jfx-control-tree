package be.nabu.jfx.control.tree.helper;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import be.nabu.jfx.control.tree.Marshallable;
import be.nabu.jfx.control.tree.TreeItem;

public class CategoryTreeItem<T> implements TreeItem<T> {

	private Item<T> item;
	private ObjectProperty<T> itemProperty = new SimpleObjectProperty<T>();
	private BooleanProperty editable = new SimpleBooleanProperty(false);
	private BooleanProperty leaf = new SimpleBooleanProperty();
	private Marshallable<T> marshallable;
	private ObjectProperty<Node> graphic = new SimpleObjectProperty<Node>();
	private ObservableList<TreeItem<T>> children;
	private CategoryTreeItem<T> parent;

	public CategoryTreeItem(Category<T> item, Marshallable<T> marshallable) {
		this(null, item, marshallable);
	}
	
	private CategoryTreeItem(CategoryTreeItem<T> parent, Item<T> item, Marshallable<T> marshallable) {
		this.parent = parent;
		this.marshallable = marshallable;
		this.item = item;
		this.itemProperty.set(item.getItem());
		this.leaf.set(!(item instanceof Category));
	}
	
	@Override
	public void refresh() {
		// do nothing
	}

	@Override
	public BooleanProperty editableProperty() {
		return editable;
	}

	@Override
	public BooleanProperty leafProperty() {
		return leaf;
	}

	@Override
	public ObjectProperty<T> itemProperty() {
		return itemProperty;
	}

	@Override
	public ObjectProperty<Node> graphicProperty() {
		return graphic;
	}

	@Override
	public String getName() {
		return marshallable.marshal(itemProperty.get());
	}

	@Override
	public ObservableList<TreeItem<T>> getChildren() {
		if (children == null) {
			children = FXCollections.observableArrayList();
			if (item instanceof Category) {
				for (Item<T> child : ((Category<T>) item).getChildren()) {
					children.add(new CategoryTreeItem<T>(this, child, marshallable));
				}
			}
		}
		return children;
	}

	@Override
	public TreeItem<T> getParent() {
		return parent;
	}

}

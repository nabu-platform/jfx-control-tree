package be.nabu.jfx.control.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.collections.ObservableList;

public class TreeUtils {
	
	public interface TreeItemCreator<T> {
		public TreeItem<T> create(TreeItem<T> parent, T item);
	}
	
	public static String getPath(TreeItem<?> item) {
		List<String> parts = new ArrayList<String>();
		while (item != null) {
			parts.add(item.getName());
			item = item.getParent();
		}
		Collections.reverse(parts);
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < parts.size(); i++) {
			if (i > 0) {
				builder.append("/");
			}
			builder.append(parts.get(i));
		}
		return builder.toString();
	}
	
	public static <T> List<TreeItem<T>> refreshChildren(TreeItemCreator<T> creator, TreeItem<T> item, Collection<T> children) {
		List<TreeItem<T>> result = new ArrayList<TreeItem<T>>();
		// first we build a map of current children
		Map<T, TreeItem<T>> currentChildren = new HashMap<T, TreeItem<T>>();
		for (TreeItem<T> child : item.getChildren()) {
			currentChildren.put(child.itemProperty().get(), child);
		}
		// then we loop over the children to make the new list
		for (T child : children) {
			if (currentChildren.containsKey(child)) {
				result.add(currentChildren.get(child));
			}
			else {
				result.add(creator.create(item, child));
			}
		}
		item.getChildren().setAll(result);
		return result;
	}
	
	public static <T> List<TreeItem<T>> refreshChildren(TreeItem<T> item, Collection<TreeItem<T>> children) {
		List<TreeItem<T>> result = new ArrayList<TreeItem<T>>();
		ObservableList<TreeItem<T>> currentChildren = item.getChildren();
		// then we loop over the children to make the new list
		for (TreeItem<T> child : children) {
			int index = currentChildren.indexOf(child);
			if (index >= 0) {
				result.add(currentChildren.get(index));
			}
			else {
				result.add(child);
			}
		}
		item.getChildren().setAll(result);
		return result;
	}
	
	public static <T> TreeItem<T> find(TreeItem<T> item, T value) {
		for (TreeItem<T> child : item.getChildren()) {
			if (child.itemProperty().get() != null && child.itemProperty().get().equals(value)) {
				return child;
			}
			else if (!child.leafProperty().get()) {
				TreeItem<T> found = find(child, value);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}
}

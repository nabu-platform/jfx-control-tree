package be.nabu.jfx.control.tree.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimpleCategory<T> extends SimpleItem<T> implements Category<T> {

	private List<Item<T>> children = new ArrayList<Item<T>>();

	public SimpleCategory(T item) {
		super(item);
	}

	@Override
	public List<Item<T>> getChildren() {
		return children;
	}

	@SafeVarargs
	final public SimpleCategory<T> add(Item<T>...children) {
		this.children.addAll(Arrays.asList(children));
		return this;
	}
	
	@Override
	public String toString() {
		return getItem() + children.toString();
	}
}

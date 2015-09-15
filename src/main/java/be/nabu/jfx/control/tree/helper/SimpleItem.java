package be.nabu.jfx.control.tree.helper;

public class SimpleItem<T> implements Item<T> {

	private T item;

	public SimpleItem(T item) {
		this.item = item;
	}
	
	@Override
	public T getItem() {
		return item;
	}

	@Override
	public String toString() {
		return item.toString();
	}
}

package be.nabu.jfx.control.tree;

public interface Updateable<T> {
	public T update(TreeCell<T> instance, String text);
}

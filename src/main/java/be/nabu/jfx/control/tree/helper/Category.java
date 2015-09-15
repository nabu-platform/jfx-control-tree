package be.nabu.jfx.control.tree.helper;

import java.util.List;

public interface Category<T> extends Item<T> {
	public List<Item<T>> getChildren();
}

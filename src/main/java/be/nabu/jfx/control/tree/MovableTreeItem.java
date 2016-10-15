package be.nabu.jfx.control.tree;


public interface MovableTreeItem<T> extends TreeItem<T> {
	public enum Direction {
		UP,
		DOWN,
		LEFT,
		RIGHT
	}
	
	public TreeItem<T> move(Direction direction);
}

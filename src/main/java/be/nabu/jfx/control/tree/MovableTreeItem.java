package be.nabu.jfx.control.tree;


public interface MovableTreeItem<T> extends TreeItem<T> {
	public enum Direction {
		UP,
		DOWN,
		LEFT,
		RIGHT
	}
	
	public void move(Direction direction);
}

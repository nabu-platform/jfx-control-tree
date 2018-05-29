package be.nabu.jfx.control.tree;

public interface Refreshable {
	public void refresh();
	public default void refresh(boolean hard) {
		refresh();
	}
}

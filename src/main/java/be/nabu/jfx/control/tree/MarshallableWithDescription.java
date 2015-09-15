package be.nabu.jfx.control.tree;

public interface MarshallableWithDescription<T> extends Marshallable<T> {
	public String getDescription(T instance);
}

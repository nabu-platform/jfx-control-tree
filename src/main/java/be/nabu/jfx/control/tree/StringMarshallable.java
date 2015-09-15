package be.nabu.jfx.control.tree;

public class StringMarshallable implements Marshallable<String> {

	@Override
	public String marshal(String instance) {
		return instance;
	}

}

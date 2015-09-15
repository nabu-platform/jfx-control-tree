package be.nabu.jfx.control.tree.clipboard;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

public interface ClipboardHandler {
	public ClipboardContent getContent();
	public void setClipboard(Clipboard content);
}

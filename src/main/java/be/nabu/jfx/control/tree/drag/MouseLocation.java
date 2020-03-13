package be.nabu.jfx.control.tree.drag;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;

public class MouseLocation {

	private static Map<Scene, MouseLocation> instances = new HashMap<Scene, MouseLocation>();
	
	private DoubleProperty x = new SimpleDoubleProperty(), y = new SimpleDoubleProperty();
	
	private EventHandler<MouseEvent> mouseHandler = new EventHandler<MouseEvent>() {
		@Override
		public void handle(MouseEvent event) {
			x.set(event.getSceneX());
			y.set(event.getSceneY());
		}
	};
	
	private EventHandler<DragEvent> dragHandler = new EventHandler<DragEvent>() {
		@Override
		public void handle(DragEvent event) {
			// at the end, when you release drag mode, it suddenly sends out events with position 0,0
			if (event.getSceneX() > 0 || event.getSceneY() > 0) {
				x.set(event.getSceneX());
				y.set(event.getSceneY());
			}
		}
	};
	
	public MouseLocation(Scene scene) {
		scene.addEventHandler(MouseEvent.ANY, getMouseHandler());
		scene.addEventHandler(DragEvent.ANY, getDragHandler());
	}
	
	public static MouseLocation getInstance(Scene scene) {
		if (!instances.containsKey(scene))
			instances.put(scene, new MouseLocation(scene));
		return instances.get(scene);
	}

	public ReadOnlyDoubleProperty xProperty() {
		return x;
	}

	public ReadOnlyDoubleProperty yProperty() {
		return y;
	}
	
	public EventHandler<MouseEvent> getMouseHandler() {
		return mouseHandler;
	}
	
	public EventHandler<DragEvent> getDragHandler() {
		return dragHandler;
	}
}
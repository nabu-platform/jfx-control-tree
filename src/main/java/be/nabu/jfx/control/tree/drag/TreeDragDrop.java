package be.nabu.jfx.control.tree.drag;

import java.util.ArrayList;

import be.nabu.jfx.control.tree.Tree;
import be.nabu.jfx.control.tree.TreeCell;
import be.nabu.jfx.control.tree.TreeItem;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;

public class TreeDragDrop {
	
	public static final String DATA_TYPE_TREE = "tree";
	
	private Dragboard dragboard;
	private ClipboardContent clipboard;
	
	@SuppressWarnings("rawtypes")
	private TreeCell dragSource;
	
	private EndHandler endHandler;
	
	
	private static TreeDragDrop instance = new TreeDragDrop();
	
	public static <T> void makeDraggable(Tree<T> tree, TreeDragListener<T> listener) {
		if (tree.getDragListener() == null) {
//		if (!instance.dragListeners.containsKey(tree)) {
//			instance.dragListeners.put(tree, listener);
			tree.setDragListener(listener);
			instance.makeDraggable(tree);
		}
	}
	
	public static <T> void makeDroppable(Tree<T> tree, TreeDropListener<T> listener) {
		if (tree.getDropListeners() == null) {
			tree.setDropListeners(new ArrayList<TreeDropListener<T>>());
			instance.makeDroppable(tree);
		}
		if (!tree.getDropListeners().contains(listener)) {
			tree.getDropListeners().add(listener);
		}
	}
	
	public static MouseLocation getMouseLocation(Tree<?> tree) {
		return MouseLocation.getInstance(tree.getScene());
	}
	
	private TreeDragDrop() {}
	
	private void makeDraggable(final Tree<?> tree) {
		// this starts a new drag (if allowed)
		tree.addEventHandler(MouseEvent.DRAG_DETECTED, new EventHandler<MouseEvent>() {			
			@SuppressWarnings("unchecked")
			@Override
			public void handle(MouseEvent event) {
				dragSource = (TreeCell<?>) ((Node) event.getTarget()).getUserData();
				if (dragSource.getTree().getDragListener().canDrag(dragSource)) {
					clipboard = new ClipboardContent();
					dragboard = dragSource.getTree().startDragAndDrop(dragSource.getTree().getDragListener().getTransferMode());
					DataFormat format = getDataFormat(dragSource.getTree().getDragListener().getDataType(dragSource));
					clipboard.put(format, getPath(dragSource.getItem()));
					if (dragSource.getTree().getId() != null) {
						clipboard.put(getDataFormat(DATA_TYPE_TREE), dragSource.getTree().getId());
					}
					dragboard.setContent(clipboard);
					String asText = dragSource.getTree().getDragListener().getAsText(dragSource);
					if (asText != null) {
						clipboard.put(DataFormat.PLAIN_TEXT, asText);
					}
					event.consume();
					// whether or not you drop the drag event onto a valid target, you want the dragging to stop
					// we subscribe to this event do reset the state of the ongoing drag
					endHandler = new EndHandler();
					dragSource.getTree().getScene().addEventHandler(DragEvent.DRAG_DONE, endHandler);
					dragSource.getTree().getDragListener().drag(dragSource);
				}
				else {
					dragSource = null;
				}
			}
		});
		if (tree.getScene() != null) {
			// make sure we still get mouse updates when we move in the tree, the events are NOT bubbled up to the parent so don't arrive in the scene
			tree.addEventHandler(MouseEvent.ANY, MouseLocation.getInstance(tree.getScene()).getMouseHandler());
		}
		// if the scene is updated, add a new listener
		// this also takes care of trees that are not added to a scene before they are made draggable
		// the problem here being the mouse location needs a scene to work on but the scene might not be set when you make it draggable
		tree.sceneProperty().addListener(new ChangeListener<Scene>() {
			@Override
			public void changed(ObservableValue<? extends Scene> arg0, Scene arg1, Scene newScene) {
				if (newScene != null) {
					tree.addEventHandler(MouseEvent.ANY, MouseLocation.getInstance(newScene).getMouseHandler());
				}
			}
		});
	}
	
	private void makeDroppable(final Tree<?> tree) {
		if (tree.getScene() != null) {
			// make sure we still get mouse updates when we move in the tree, the events are NOT bubbled up to the parent so don't arrive in the scene
			tree.addEventHandler(MouseEvent.ANY, MouseLocation.getInstance(tree.getScene()).getMouseHandler());
			// make sure drag events are also pushed so the mouse position is updated when we are dragging stuff
			tree.addEventHandler(DragEvent.ANY, MouseLocation.getInstance(tree.getScene()).getDragHandler());
		}
		// delayed initialization
		tree.sceneProperty().addListener(new ChangeListener<Scene>() {
			@Override
			public void changed(ObservableValue<? extends Scene> arg0, Scene arg1, Scene newScene) {
				if (newScene != null) {
					tree.addEventHandler(MouseEvent.ANY, MouseLocation.getInstance(tree.getScene()).getMouseHandler());
					tree.addEventHandler(DragEvent.ANY, MouseLocation.getInstance(tree.getScene()).getDragHandler());
				}
			}
		});
		// check if you are allowed to drop it here
		tree.addEventHandler(DragEvent.DRAG_OVER, new EventHandler<DragEvent>() {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void handle(DragEvent event) {
				TreeCell<?> target = (TreeCell<?>) ((Node) event.getTarget()).getUserData();
				if (dragSource != null) {
					for (TreeDropListener listener : target.getTree().getDropListeners()) {
						if (listener.canDrop(dragSource.getTree().getDragListener().getDataType(dragSource), target, dragSource, dragSource.getTree().getDragListener().getTransferMode())) {
							event.acceptTransferModes(dragSource.getTree().getDragListener().getTransferMode());
							event.consume();
							break;
						}
					}
				}
			}
		});
		// this handler is _ONLY_ triggered if you drop it on something that is allowed by the drag over handler
		tree.addEventHandler(DragEvent.DRAG_DROPPED, new EventHandler<DragEvent>() {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void handle(DragEvent event) {
				if (!event.isConsumed() && !event.isDropCompleted() && dragSource != null && dragSource.getTree() != null && dragSource.getTree().getDragListener() != null) {
					TreeCell<?> target = (TreeCell<?>) ((Node) event.getTarget()).getUserData();
					// drop it on the first one that accepts
					for (TreeDropListener listener : target.getTree().getDropListeners()) {
						if (listener.canDrop(dragSource.getTree().getDragListener().getDataType(dragSource), target, dragSource, dragSource.getTree().getDragListener().getTransferMode())) {
							listener.drop(dragSource.getTree().getDragListener().getDataType(dragSource), target, dragSource, dragSource.getTree().getDragListener().getTransferMode());
							break;
						}
					}
					dragSource.getTree().getDragListener().stopDrag(dragSource, true);
					stopDrag(true);
					event.setDropCompleted(true);
				}
			}
		});
	}
	
	public static String getPath(TreeItem<?> item) {
		String name = item.getName();
		while (item.getParent() != null) {
			item = item.getParent();
			name = item.getName() + "/" + name;
		}
		return name;
	}

	public static DataFormat getDataFormat(String name) {
		DataFormat format = DataFormat.lookupMimeType(name);
		if (format == null) {
			format = new DataFormat(name);
		}
		return format;
	}
	
	@SuppressWarnings("unchecked")
	private void stopDrag(boolean successful) {
		if (dragSource != null) {
			if (endHandler != null && dragSource.getTree() != null && dragSource.getTree().getScene() != null) {
				dragSource.getTree().getScene().removeEventHandler(DragEvent.DRAG_DONE, endHandler);
			}
			if (!successful) {
				dragSource.getTree().getDragListener().stopDrag(dragSource, false);
			}
			endHandler = null;
			dragSource = null;
		}
	}
	
	public class EndHandler implements EventHandler<DragEvent> {
		@Override
		public void handle(DragEvent event) {
			stopDrag(false);
		}
	}
}

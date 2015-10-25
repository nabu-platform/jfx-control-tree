package be.nabu.jfx.control.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.nabu.jfx.control.tree.clipboard.ClipboardHandler;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

public class Tree<T> extends Control {
	
	private ObjectProperty<TreeItem<T>> root = new SimpleObjectProperty<TreeItem<T>>(this, "root");
	private TreeCell<T> rootCell;
	private Marshallable<T> stringConverter;
	private DoubleProperty spacing = new SimpleDoubleProperty(15);
	private MultipleSelectionModel<TreeCell<T>> selectionModel = new TreeSelectionModel<T>();
	private Map<String, Object> properties = new HashMap<String, Object>();
	private ContextMenuProvider<T> contextMenuProvider;
	private ClipboardHandler clipboardHandler;
	private boolean autoscrollOnSelect = true;
	private boolean autodetectDirty = true;
	
	private Callback<TreeItem<T>, TreeCellValue<T>> cellValueFactory;
	
	private List<Refreshable> linkedRefreshables = new ArrayList<Refreshable>();
	
	private EventHandler<MouseEvent> clickHandler;

	@SuppressWarnings("unchecked")
	public Tree(Marshallable<T> marshallable) {
		this(marshallable, marshallable instanceof Updateable ? (Updateable<T>) marshallable : null);
	}
	
	public Tree(Marshallable<T> marshallable, Updateable<T> updateable) {
		this(new BaseTreeCellValueFactory<T>(marshallable, updateable));
	}

	public Tree(Callback<TreeItem<T>, TreeCellValue<T>> cellValueFactory) {
		minWidthProperty().bind(prefWidthProperty());
		this.cellValueFactory = cellValueFactory;
		getStyleClass().add("jfx-tree");
		// only change selection in the treecell depending on the actual selection model
		// this way you can guarantee that external meddling with the selection is displayed properly in the tree
		selectionModel.getSelectedItems().addListener(new ListChangeListener<TreeCell<T>>() {
			@Override
			public void onChanged(ListChangeListener.Change<? extends TreeCell<T>> change) {
				while (change.next()) {
					if (change.wasRemoved()) {
						for (TreeCell<T> removed : change.getRemoved())
							removed.selected.setValue(false);
					}
					if (change.wasAdded()) {
						for (TreeCell<T> added : change.getAddedSubList())
							added.selected.setValue(true);
					}
				}
			}
		});
		root.addListener(new ChangeListener<TreeItem<?>>() {
			@Override
			public void changed(ObservableValue<? extends TreeItem<?>> arg0, TreeItem<?> oldRoot, TreeItem<?> newRoot) {
				setRootCell(new TreeCell<T>(Tree.this, rootProperty().getValue()));
			}
		});
		this.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode() == KeyCode.DELETE) {
					for (TreeCell<T> selected : getSelectionModel().getSelectedItems()) {
						if (selected.getItem() instanceof RemovableTreeItem) {
							if (((RemovableTreeItem<T>) selected.getItem()).remove()) {
								selected.getParent().refresh();
							}
							event.consume();
						}
					}
				}
				else if (event.getCode() == KeyCode.F5) {
					for (TreeCell<T> selected : getSelectionModel().getSelectedItems()) {
						selected.refresh();
					}
				}
				// copy
				else if (event.getCode() == KeyCode.C && event.isControlDown()) {
					if (clipboardHandler != null) {
						ClipboardContent content = clipboardHandler.getContent();
						if (content != null) {
							Clipboard.getSystemClipboard().setContent(content);
						}
						event.consume();
					}
				}
				// paste
				else if (event.getCode() == KeyCode.V && event.isControlDown()) {
					if (clipboardHandler != null) {
						clipboardHandler.setClipboard(Clipboard.getSystemClipboard());
						event.consume();
					}
				}
			}
		});
	}
	
	public void autoscroll(TreeCell<T> to) {
		ScrollPane parent = JFXUtils.getScrollParent(getRootCell().getNode());
		if (parent != null) {
			JFXUtils.focusInScroll(parent, to);
		}
	}
	
	public void autoscroll() {
		if (getSelectionModel().getSelectedItem() != null) {
			autoscroll(getSelectionModel().getSelectedItem());
		}
	}
	
	public ClipboardHandler getClipboardHandler() {
		return clipboardHandler;
	}

	public void setClipboardHandler(ClipboardHandler clipboardHandler) {
		this.clipboardHandler = clipboardHandler;
	}

	/**
	 * Returns path [root, item]
	 * @param item
	 * @return
	 */
	private List<TreeItem<T>> getPath(TreeItem<T> item) {
		List<TreeItem<T>> path = new ArrayList<TreeItem<T>>();
		path.add(item);
		while(item.getParent() != null) {
			item = item.getParent();
			path.add(item);
		}
		Collections.reverse(path);
		return path;
	}
	
	public TreeCell<T> getTreeCell(TreeItem<T> item) {
		List<TreeItem<T>> path = getPath(item);
		// not the same root?
		if (!path.get(0).equals(rootCell.getItem()))
			return null;
		TreeCell<T> cell = rootCell;
		for (int i = 1; i < path.size(); i++) {
			cell = cell.getCell(path.get(i));
			if (cell == null)
				break;
		}
		return cell;
	}
	
	public ObjectProperty<TreeItem<T>> rootProperty() {
		return root;
	}

	public Marshallable<T> getStringConverter() {
		return stringConverter;
	}

	public TreeCell<T> getRootCell() {
		return rootCell;
	}

	void setRootCell(TreeCell<T> rootCell) {
		this.rootCell = rootCell;
	}
	
	@Override
	public String getUserAgentStylesheet() {
		return Tree.class.getClassLoader().getResource("jfx-tree.css").toExternalForm();
	}
	
	public DoubleProperty spacingProperty() {
		return spacing;
	}
	
	public MultipleSelectionModel<TreeCell<T>> getSelectionModel() {
		return selectionModel;
	}

	public Callback<TreeItem<T>, TreeCellValue<T>> getCellValueFactory() {
		return cellValueFactory;
	}
	
	public TreeItem<T> resolve(String path) {
		return resolve(rootProperty().get(), path.replaceAll("^[/]*(.*?)[/]*$", "$1").split("/"), 0);
	}
	
	private TreeItem<T> resolve(TreeItem<T> against, String [] path, int counter) {
		if (against.leafProperty().get())
			throw new IllegalArgumentException("Can't resolve against a leaf");
		TreeItem<T> target = null;
		for (TreeItem<T> child : against.getChildren()) {
			if (child.getName().equals(path[counter])) {
				target = child;
				break;
			}
		}
		if (target == null) {
			StringBuilder builder = new StringBuilder();
			for (String part : path) {
				if (!builder.toString().isEmpty()) {
					builder.append("/");
				}
				builder.append(part);
			}
			throw new IllegalArgumentException("The path does not exist: " + builder.toString());
		}
		
		if (counter == path.length - 1)
			return target;
		else
			return resolve(target, path, counter + 1);
	}

	public void addRefreshListener(Refreshable...refreshables) {
		linkedRefreshables.addAll(Arrays.asList(refreshables));
	}
	
	public void removeRefreshListener(Refreshable...refreshables) {
		linkedRefreshables.removeAll(Arrays.asList(refreshables));
	}

	void pushRefresh() {
		// refresh linked items
		for (Refreshable linked : linkedRefreshables) {
			linked.refresh();
		}
	}
	
	public void set(String name, Object value) {
		properties.put(name, value);
	}
	
	public Object get(String name) {
		return properties.get(name);
	}
	
	public void resize() {
		// update size of tree on expanded toggling
		double rootWidth = 50 + getRootCell().getLocalWidth();
		if (!prefWidthProperty().isBound()) {
			prefWidthProperty().set(rootWidth);
		}
		if (!minWidthProperty().isBound()) {
			minWidthProperty().set(rootWidth);
		}
	}

	public ContextMenuProvider<T> getContextMenuProvider() {
		return contextMenuProvider;
	}

	public void setContextMenuProvider(final ContextMenuProvider<T> contextMenuProvider) {
		// if no context menu provider is set yet, add some listeners
		if (this.contextMenuProvider == null) {
			// we need to make sure that the context menu is fully gone from the tree after you use it
			// this listener will remove the context menu when it is hidden
			contextMenuProperty().addListener(new ChangeListener<ContextMenu>() {
				@Override
				public void changed(ObservableValue<? extends ContextMenu> arg0, ContextMenu arg1, ContextMenu newContextMenu) {
					if (newContextMenu != null) {
						newContextMenu.showingProperty().addListener(new ChangeListener<Boolean>() {
							@Override
							public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean arg2) {
								if (arg1 != null && arg1) {
									setContextMenu(null);
								}
							}
						});
					}
				}
			});
			addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					List<TreeCell<T>> selected = getSelectionModel().getSelectedItems();
					if (contextMenuProvider != null && event.getButton().equals(MouseButton.SECONDARY)) {
						ContextMenu menu = Tree.this.contextMenuProvider.getMenu(selected);
						setContextMenu(menu);
						if (menu != null) {
							getContextMenu().show(getScene().getWindow(), event.getScreenX(), event.getScreenY());
							// need to actually _remove_ the context menu on action
							// otherwise by default (even if not in this if), the context menu will be shown if you right click
							getContextMenu().addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
								@Override
								public void handle(ActionEvent arg0) {
									setContextMenu(null);
								}
							});
						}
					}
				}
			});
		}
		this.contextMenuProvider = contextMenuProvider;
	}
	
	public void refresh() {
		if (root.isNotNull().get()) {
			getTreeCell(root.get()).refresh();
		}
	}

	public EventHandler<MouseEvent> getClickHandler() {
		return clickHandler;
	}

	public void setClickHandler(EventHandler<MouseEvent> clickHandler) {
		this.clickHandler = clickHandler;
	}

	public boolean isAutoscrollOnSelect() {
		return autoscrollOnSelect;
	}

	public void setAutoscrollOnSelect(boolean autoscrollOnSelect) {
		this.autoscrollOnSelect = autoscrollOnSelect;
	}
	
	public void forceLoad() {
		if (root.get() != null) {
			forceLoad(root.get(), true);
		}
	}
	
	public void forceLoad(TreeItem<T> item, boolean recursive) {
		for (TreeItem<T> child : item.getChildren()) {
			TreeCell<T> cell = getTreeCell(child);
			cell.initialize();
			if (recursive) {
				forceLoad(child, recursive);
			}
		}
	}

	/**
	 * The cells will try their best to detect when they are dirty which means the children have changed
	 * This functionality should only be used if the TreeItem.getChildren() is fast (or cached)
	 * If the getChildren() in the treeitem is slow, disable autodetect and manually set the isdirty on the cell to trigger refreshes
	 */
	public boolean isAutodetectDirty() {
		return autodetectDirty;
	}
	public void setAutodetectDirty(boolean autodetectDirty) {
		this.autodetectDirty = autodetectDirty;
	}
	
}

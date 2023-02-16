package be.nabu.jfx.control.tree;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import be.nabu.jfx.control.tree.MovableTreeItem.Direction;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Much care has been given into the ability to retain the same cell/item combination at all times
 * If you hit a refresh, all unchanged children should have the same treeitem
 * All treeitems should have the same cell
 * This means you can for example draw lines from a cell and they won't break on a refresh
 * 
 * The "isLoaded" state is used to indicate whether or not a child has been loaded
 * The child loading is postponed as long as possible because of circular references
 * 		> suppose child A loads B, B loads C and C tries to load A again
 * 		> without lazy loading, this will always end up badly
 */
public class TreeCell<T> implements Refreshable, Focusable {
	
	private Tree<T> tree;
	private TreeItem<T> item;
	private BooleanProperty expanded = new SimpleBooleanProperty(false);
	BooleanProperty selected = new SimpleBooleanProperty(false);
	private static Map<String, Image> images = new HashMap<String, Image>();
	private BooleanProperty hideSelf = new SimpleBooleanProperty(false);
	
	/**
	 * Indicates whether or not this component should be refreshed
	 */
	private boolean isDirty = true;
	
	private DoubleProperty leftAnchorX = new SimpleDoubleProperty(),
			leftAnchorY = new SimpleDoubleProperty(),
			rightAnchorX = new SimpleDoubleProperty(),
			rightAnchorY = new SimpleDoubleProperty(),
			treeLayoutX = new SimpleDoubleProperty(),
			treeLayoutY = new SimpleDoubleProperty();
	
	private ImageView expandedIcon = loadGraphic("minus.png"), 
			collapsedIcon = loadGraphic("plus.png"), 
			itemIcon = loadGraphic("item.png");
	
	private boolean isLoaded = false;
	
	/**
	 * The actual node consists of a spacer and the item container
	 */
	private HBox node;
	
	/**
	 * The itemContainer consists of the item display and the nodes of all the children (if any)
	 */
	private VBox itemContainer;
	
	/**
	 * The itemDisplay contains the graphic and/or the label for the item
	 */
	private HBox itemDisplay;
	
	private HBox displayIcon;
	
	private TreeCellValue<T> cellValue;
	
	private Map<TreeItem<T>, TreeCell<T>> children = new LinkedHashMap<TreeItem<T>, TreeCell<T>>();
	
	private TreeCell<T> parent;
	
	private boolean isRefreshing = false;
	
	/**
	 * This keeps track for each treecell which cell is actually visible
	 * By default this is the treecell itself of course but if you start collapsing random parents, you need to keep track of the first actually visible parent
	 * You can't do this with the visibleProperty() because if you first collapse parent1, you will trigger a change, but if you then collapse the parents parent, you will not be triggered and still reference the parent
	 */
	private ObjectProperty<TreeCell<T>> visibleCell = new SimpleObjectProperty<TreeCell<T>>(this);
	
	TreeCell(Tree<T> tree, TreeItem<T> item) {
		this(tree, item, null);
	}

	TreeCell(final Tree<T> tree, TreeItem<T> item, final TreeCell<T> parent) {
		this.tree = tree;
		this.item = item;
		this.parent = parent;
		
		// if the expanded status changes, the visibility of the direct child nodes must be updated
		expanded.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean newValue) {
				if (newValue && !isInitialized) {
					initialize();
					internalRefresh(true, false);
				}
				for (final TreeCell<T> child : children.values()) {
					if (newValue) {
						if (!child.isLoaded) {
							itemContainer.getChildren().add(child.getNode());
						}
					}
					if (child.isLoaded) {
						child.getNode().visibleProperty().setValue(newValue);
					}
					// if you have collapsed, set the visible cell of the child to this
					child.visibleCell.setValue(newValue ? child : TreeCell.this);
				}
				Platform.runLater(resizer);
			}
		});
		
		// monitor the "visible cell" of the parent
		if (parent != null) {
			parent.visibleCell.addListener(new ChangeListener<TreeCell<T>>() {
				@Override
				public void changed(ObservableValue<? extends TreeCell<T>> arg0, TreeCell<T> arg1, TreeCell<T> newCell) {
					// if the parent "visible cell" is updated to the parent itself, that means it has been made visible, depending on its expanded property the children are visible or not
					if (parent.equals(newCell))
						visibleCell.set(parent.expandedProperty().getValue() ? TreeCell.this : parent);
					// the parent is updated to something else so it has become invisible, update this as well
					else
						visibleCell.set(newCell);
				}
			});
		}
		
		// if you update the leafiness of an item, we probably need to update its icon
		item.leafProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean arg2) {
				refreshItemDisplayIcon();
			}
		});
		
		// change style on selection
		selected.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean newValue) {
				if (newValue) {
					getCellValue().getNode().requestFocus();
					getCellValue().getNode().getStyleClass().remove("deselected");
					getCellValue().getNode().getStyleClass().add("selected");
				}
				else {
					getCellValue().getNode().getStyleClass().remove("selected");
					getCellValue().getNode().getStyleClass().add("deselected");
				}
			}
		});
	
		visibleCell.addListener(new ChangeListener<TreeCell<T>>() {
			@Override
			public void changed(ObservableValue<? extends TreeCell<T>> arg0, TreeCell<T> arg1, TreeCell<T> arg2) {
				bindAnchors();
			}
		});
		
		item.getChildren().addListener(new ListChangeListener<TreeItem<T>>() {
			@Override
			public void onChanged(javafx.collections.ListChangeListener.Change<? extends TreeItem<T>> arg0) {
				// if you are in the middle of a refresh, ignore changes to children
				// most refreshing will remove all children and re-add them
				// if we trigger on every change, we will throw away all the mapped cells
				if (!isRefreshing) {
					isDirty = true;
					refreshItemContainer(false);
				}
			}
		});
	}
	
	Resizer resizer = new Resizer();
	private class Resizer implements Runnable {
		@Override
		public void run() {
			tree.resize();
		}
	}
	
	private boolean isInitialized = false;
	
	void initialize() {
		if (!isInitialized) {
			isInitialized = true;
			if (parent == null) {
				treeLayoutX.bind(tree.layoutXProperty().add(getItemContainer().layoutXProperty()));
				treeLayoutY.bind(tree.layoutYProperty().add(getNode().layoutYProperty()));
			}
			else {
				treeLayoutX.bind(parent.treeLayoutXProperty().add(getItemContainer().layoutXProperty()));
				// there is a gradual decay in Y-positioning the deeper you go into the tree (level-wise)
				// adding the layoutYProperty of the item container fixes this (not entirely sure why...)
				treeLayoutY.bind(parent.treeLayoutYProperty().add(getNode().layoutYProperty()).add(getItemContainer().layoutYProperty()));
			}
	
			bindAnchors();
			isDirty = true;
		}
	}
	
	public List<TreeCell<T>> getChildren() {
		return new ArrayList<TreeCell<T>>(children.values());
	}
	
	public void expandAll() {
		initialize();
		expandedProperty().set(true);
		for (TreeCell<T> child : children.values()) {
			child.expandAll();
		}
	}
	
	public void expandAll(int count) {
		initialize();
		expandedProperty().set(true);
		if (count > 0) {
			for (TreeCell<T> child : children.values()) {
				child.expandAll(count - 1);
			}
		}
	}
	
	public void collapseAll() {
		expandedProperty().set(false);
		for (TreeCell<T> child : children.values()) {
			if (child.isLoaded) {
				child.collapseAll();
			}
		}
	}
	
	private void bindAnchors() {
		leftAnchorX.bind(visibleCell.getValue().treeLayoutX);
		// should divide height by 2 to get centered but this looks good, same for below
		leftAnchorY.bind(visibleCell.getValue().treeLayoutY.add(visibleCell.get().getCellValue().getNode().layoutYProperty().add(visibleCell.getValue().getCellValue().getNode().heightProperty().divide(1.4))));
		
		rightAnchorX.bind(visibleCell.getValue().treeLayoutX.add(visibleCell.getValue().getCellValue().getNode().layoutXProperty().add(visibleCell.getValue().getCellValue().getNode().widthProperty())));
		rightAnchorY.bind(visibleCell.getValue().treeLayoutY.add(visibleCell.getValue().getCellValue().getNode().heightProperty().divide(1.4)));
	}
	
	public void show() {
		initialize();
		List<TreeCell<T>> parents = new ArrayList<TreeCell<T>>();
		TreeCell<T> parent = this.parent;
		while (parent != null) {
			parents.add(parent);
			parent = parent.getParent();
		}
		// we need to expand them from the root up
		Collections.reverse(parents);
		for (TreeCell<T> cell : parents) {
			cell.expandedProperty().set(true);
		}
	}
	
	public ReadOnlyObjectProperty<TreeCell<T>> visibleCellProperty() {
		return visibleCell;
	}	
	public ReadOnlyDoubleProperty leftAnchorXProperty() {
		initialize();
		return leftAnchorX;
	}
	public ReadOnlyDoubleProperty leftAnchorYProperty() {
		initialize();
		return leftAnchorY;
	}

	public ReadOnlyDoubleProperty rightAnchorXProperty() {
		initialize();
		return rightAnchorX;
	}
	public ReadOnlyDoubleProperty rightAnchorYProperty() {
		initialize();
		return rightAnchorY;
	}
	public ReadOnlyDoubleProperty treeLayoutXProperty() {
		initialize();
		return treeLayoutX;
	}
	public ReadOnlyDoubleProperty treeLayoutYProperty() {
		initialize();
		return treeLayoutY;
	}
	
	public Tree<T> getTree() {
		return tree;
	}

	private void select(TreeCell<T> cell, boolean appendSelection) {
		if (!appendSelection) {
			tree.getSelectionModel().clearSelection();
		}
		tree.getSelectionModel().select(cell);
	}
	
	public void select(boolean append) {
		select(this, append);
	}
	
	public void select() {
		select(this, false);
	}
	
	public TreeItem<T> getItem() {
		return item;
	}
	
	Map<TreeItem<T>, TreeCell<T>> getChildrenAsMap() {
		initialize();
		getNode();
		return children;
	}
	
	@Override
	public void refresh() {
		internalRefresh(true, false);
	}

	@Override
	public void refresh(boolean hard) {
		internalRefresh(true, hard);
	}
	
	private void internalRefresh(boolean isFirst, boolean hard) {
		isRefreshing = true;
		// first force a refresh on the current item
		item.refresh(hard);
		isRefreshing = false;
		// then refresh the child contents which will rebuild the treecells
		refreshItemContainer(isFirst);
		// refresh the icon (not really necessary i think but hey)
		refreshItemDisplayIcon();
		// refresh the cell value (this updates the label or whatever is being used)
		cellValue.refresh();
		// propagate the refresh to the children
		for (TreeCell<T> child : children.values()) {
			// only reload immediate children
			if (child.isLoaded || isFirst) {
				child.internalRefresh(false, hard);
			}
		}
		// first refresh all the children (where necessary)
		if (isFirst) {
			getTree().pushRefresh();
		}
	}
	
	public Region getNode() {
		if (!isLoaded && tree.isRefreshOnFirstOpen()) {
			refresh();
		}
		isLoaded = true;
		if (node == null) {
			node = new HBox();
			node.getStyleClass().add("tree-cell");
			// only add the spacer if there is a parent
			if (item.getParent() != null) {
				HBox spacer = new HBox();
				spacer.getStyleClass().add("jfx-tree-spacer");
				spacer.prefWidthProperty().bind(tree.spacingProperty());
				spacer.minWidthProperty().bind(tree.spacingProperty());
				spacer.maxWidthProperty().bind(tree.spacingProperty());
				// if we are the root and we are hidden, hide our spacer
				if (getParent() == null) {
					spacer.visibleProperty().bind(hideSelf.not());
					spacer.managedProperty().bind(hideSelf.not());
				}
				// otherwise, if our parent is hidden, we don't need an additional spacer, hide it
				else {
					spacer.visibleProperty().bind(getParent().hideSelf.not());
					spacer.managedProperty().bind(getParent().hideSelf.not());
				}
				node.getChildren().add(spacer);
			}
			
			// allow mouse selection and double-click expand functionality
			node.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent event) {
					if (!event.isConsumed() && event.getTarget().equals(node)) {
						if (tree.getClickHandler() != null) {
							tree.getClickHandler().handle(event);
						}
						// if you didn't consume it, do default stuff
						if (!event.isConsumed()) {
							// at the very least, request focus
							tree.getRootCell().focus();
							// single click = select
							if (event.getClickCount() == 1) {
								if ((event.isControlDown() ^ tree.isInvertSelection()) && tree.getSelectionModel().getSelectedItems().contains(TreeCell.this)) {
									tree.getSelectionModel().getSelectedItems().remove(TreeCell.this);
									event.consume();
								}
								// if we have shift down and don't have invert selection on (which is weird currently)
								// we do a multiselect from the last selected
								else if (event.isShiftDown() && !tree.isInvertSelection()) {
									ObservableList<TreeCell<T>> current = tree.getSelectionModel().getSelectedItems();
									TreeCell<T> last = current.get(current.size() - 1);
									// we keep stepping to the next item until we find the current one
									List<TreeCell<T>> siblings = last.getParent().getChildren();
									
									int lastIndex = siblings.indexOf(last);
									int myIndex = siblings.indexOf(TreeCell.this);
									
									// if we are not part of the same parent, we don't do anything atm. walking the tree can be difficult, especially for lazily loaded trees
									if (myIndex >= 0) {
										// we are further than the last index, select everything in between
										if (myIndex > lastIndex) {
											for (int i = lastIndex + 1; i <= myIndex; i++) {
												select(siblings.get(i), true);
											}
										}
										else if (myIndex < lastIndex) {
											for (int i = lastIndex - 1; i >= myIndex; i--) {
												select(siblings.get(i), true);	
											}
										}
									}
								}
								// if the item is not selected yet, select it first
								// otherwise trigger selection _only_ if the mouse button is primary, otherwise it has the effect that the right click context menu is disabled every time even if it is selected
								else if (!tree.getSelectionModel().getSelectedItems().contains(TreeCell.this) || event.getButton() == MouseButton.PRIMARY) {
									select(TreeCell.this, event.isControlDown() ^ tree.isInvertSelection());
									event.consume();
								}
							}
							// if double click, toggle expand
							else if (event.getClickCount() == 2 && !TreeCell.this.getItem().leafProperty().get()) {
								expanded.setValue(!expanded.getValue());
								event.consume();
							}
						}
					}
				}
			});
			
			// allow browsing with arrow keys
			node.addEventHandler(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
				@Override
				public void handle(KeyEvent event) {
					if (event.getCode() == KeyCode.DOWN) {
						if (event.isControlDown() && item instanceof MovableTreeItem && !tree.isReadOnly()) {
							TreeItem<T> move = ((MovableTreeItem<T>) item).move(Direction.DOWN);
							if (move != null) {
								TreeCell<T> treeCell = TreeCell.this.tree.getTreeCell(move);
								if (treeCell != null) {
									treeCell.select();
									treeCell.focus();
								}
							}
						}
						else {
							TreeCell.this.tree.getSelectionModel().selectNext();
						}
						event.consume();
					}
					else if (event.getCode() == KeyCode.UP) {
						if (event.isControlDown() && item instanceof MovableTreeItem && !tree.isReadOnly()) {
							TreeItem<T> move = ((MovableTreeItem<T>) item).move(Direction.UP);
							if (move != null) {
								TreeCell<T> treeCell = TreeCell.this.tree.getTreeCell(move);
								if (treeCell != null) {
									treeCell.select();
									treeCell.focus();
								}
							}
						}
						else {
							TreeCell.this.tree.getSelectionModel().selectPrevious();
						}
						event.consume();
					}
					// if you press right arrow, make sure it's expanded, if it is already open, select first child
					else if (event.getCode() == KeyCode.RIGHT) {
						if (event.isControlDown() && item instanceof MovableTreeItem && !tree.isReadOnly()) {
							TreeItem<T> move = ((MovableTreeItem<T>) item).move(Direction.RIGHT);
							if (move != null) {
								TreeCell<T> treeCell = TreeCell.this.tree.getTreeCell(move);
								if (treeCell != null) {
									treeCell.select();
									treeCell.focus();
								}
							}
							event.consume();
						}
						else if (!getItem().leafProperty().getValue()) {
							// expand this one if necessary
							if (!expanded.getValue()) {
								expanded.setValue(true);
							}
							else if (getChildrenAsMap().size() > 0) {
								select(getCell(getItem().getChildren().get(0)), false);
								if (tree.isAutoscrollOnSelect()) {
									tree.autoscroll();
								}
							}
							event.consume();
						}
					}
					else if (event.getCode() == KeyCode.LEFT) {
						if (event.isControlDown() && item instanceof MovableTreeItem && !tree.isReadOnly()) {
							TreeItem<T> move = ((MovableTreeItem<T>) item).move(Direction.LEFT);
							if (move != null) {
								TreeCell<T> treeCell = TreeCell.this.tree.getTreeCell(move);
								if (treeCell != null) {
									treeCell.select();
									treeCell.focus();
								}
							}
						}
						else if (!getItem().leafProperty().getValue() && expanded.getValue()) {
							expanded.setValue(false);
						}
						else {
							select(getParent(), false);
							if (tree.isAutoscrollOnSelect()) {
								tree.autoscroll();
							}
						}
						event.consume();
					}
				}
			});
			
			// if this node is hidden, it is enough to hide all the children visually (because it contains them)
			// however you can't listen for visibility changes on children then because they remain "visible" for all intents and purposes
			// so if visibility changes on this node, propagate it to the children! that way you can listen on all levels for visibility changes
			node.visibleProperty().addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean newValue) {
					if (expanded.getValue()) {
						for (TreeCell<T> child : children.values())
							child.getNode().visibleProperty().setValue(newValue);
					}
				}
			});

			node.addEventHandler(MouseEvent.ANY, new CloneEventHandler<MouseEvent>(node));
			node.addEventHandler(DragEvent.ANY, new CloneEventHandler<DragEvent>(node));
			node.addEventHandler(KeyEvent.ANY, new CloneEventHandler<KeyEvent>(node));
			
			// link the treecell to the root node so you can detect where an event has originated from
			node.setUserData(this);
			node.getChildren().add(refreshItemContainer(false));
			// bind the "managed" property to the "visibility" property so it does not take up space if it's invisible
			node.managedProperty().bind(node.visibleProperty());
			
			if (item instanceof DisablableTreeItem) {
				node.disableProperty().bind(((DisablableTreeItem<T>) item).disableProperty());
			}
			isDirty = true;
		}
		return node;
	}
		
	private VBox refreshItemContainer(boolean force) {
		if (itemContainer == null) {
			itemContainer = new VBox();
			itemContainer.getStyleClass().add("jfx-tree-cell");
			itemContainer.getChildren().add(buildItemDisplay());
			isDirty = true;
			HBox.setHgrow(itemContainer, Priority.SOMETIMES);
		}
		// try to auto-detect dirty nodes
		if (getTree().isAutodetectDirty() && !item.leafProperty().getValue()) {
			List<TreeItem<T>> itemChildren = item.getChildren();
			if (children.size() != itemChildren.size()) {
				isDirty = true;
			}
			else {
				for (TreeItem<T> item : itemChildren) {
					if (!children.containsKey(item)) {
						isDirty = true;
						break;
					}
				}
			}
		}
		// if it's not a leaf, manage the children
		if (isDirty && !item.leafProperty().getValue()) {
			List<TreeItem<T>> itemChildren = item.getChildren();
			// remove dead values from the children map
			// remove all values from the itemContainer so they can be readded later _in the correct order_
			// this should not be a problem because they remain the same cell (with the same node)
			// however because we disconnect it (for the briefest of moments) from the scene, this might give errors at some point, to be seen
			Iterator<TreeItem<T>> iterator = children.keySet().iterator();
			while (iterator.hasNext()) {
				TreeItem<T> next = iterator.next();
				if (children.get(next).isLoaded) {
					itemContainer.getChildren().remove(children.get(next).getNode());
				}
				if (!itemChildren.contains(next)) {
					iterator.remove();
				}
			}
			// we need to copy the remaining (existing) children to a temporary map
			Map<TreeItem<T>, TreeCell<T>> temporaryMap = new HashMap<TreeItem<T>, TreeCell<T>>(children);
			// they will be readded when we loop over the children
			children.clear();
			
			// now we loop over the children of the item and add them again in the correct order
			for (TreeItem<T> child : itemChildren) {
				TreeCell<T> cell = temporaryMap.containsKey(child) ? temporaryMap.get(child) : new TreeCell<T>(tree, child, this);
				children.put(child, cell);
				// because we are readding the node, we need to take into account the expanded setting
				if (expanded.get() || force) {
					itemContainer.getChildren().add(children.get(child).getNode());
					cell.getNode().visibleProperty().setValue(expanded.getValue());
				}
				// if we don't actually add the child node (so the parent is collapsed), its "isLoaded" must be set to false again
				// otherwise if we expand the parent again, it won't be loaded
				else {
					cell.isLoaded = false;
				}
				// for new cells (and it doesn't matter for old cells): also set the visible cell for expansion
				cell.visibleCell.setValue(expanded.getValue() ? cell : TreeCell.this);
				
				// if the child is currently selected, add it again to the selection
				// TODO: need to make sure this doesn't backfire with nested items that are identical
				if (expanded.get() || force) {
					for (TreeCell<T> selected : new ArrayList<TreeCell<T>>(getTree().getSelectionModel().getSelectedItems())) {
						if (selected.getItem().itemProperty().get().equals(cell.getItem().itemProperty().get())) {
							cell.select(true);
						}
					}
				}
			}
			isDirty = false;
		}
		return itemContainer;
	}
	
	private HBox buildItemDisplay() {
		if (itemDisplay == null) {
			itemDisplay = new HBox();
			displayIcon = new HBox();
			
			itemDisplay.visibleProperty().bind(hideSelf.not());
			itemDisplay.managedProperty().bind(hideSelf.not());
			displayIcon.visibleProperty().bind(hideSelf.not());
			displayIcon.managedProperty().bind(hideSelf.not());
			
			itemDisplay.getChildren().add(displayIcon);
			itemDisplay.setAlignment(Pos.CENTER_LEFT);
			displayIcon.setAlignment(Pos.CENTER);
			refreshItemDisplayIcon();
			// toggle expanded if you click on it
			displayIcon.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent arg0) {
					expanded.setValue(!expanded.getValue());
				}
			});
			displayIcon.getStyleClass().add("jfx-tree-icon");
			if (item.graphicProperty().isNotNull().getValue())
				itemDisplay.getChildren().add(item.graphicProperty().getValue());
			item.graphicProperty().addListener(new ChangeListener<Node>() {
				@Override
				public void changed(ObservableValue<? extends Node> arg0, Node oldNode, Node newNode) {
					// remove the old one
					if (oldNode != null)
						itemDisplay.getChildren().remove(oldNode);
					// add the new one
					if (newNode != null)
						itemDisplay.getChildren().add(1, newNode);
				}
			});
			itemDisplay.getChildren().add(getCellValue().getNode());
			
			ReadOnlyStringProperty tooltipProperty = item.tooltipProperty();
			if (tooltipProperty != null) {
				SimpleObjectProperty<Tooltip> tooltipObjectProperty = new SimpleObjectProperty<Tooltip>();
				// if we have an initial value, install it immediately
				if (tooltipProperty.get() != null) {
					tooltipObjectProperty.set(new Tooltip(tooltipProperty.get()));
					trySetDelay(tooltipObjectProperty.get());
					Tooltip.install(itemDisplay, tooltipObjectProperty.get());
				}
				// listen for changes
				tooltipProperty.addListener(new ChangeListener<String>() {
					@Override
					public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
						if (tooltipObjectProperty.get() != null) {
							Tooltip.uninstall(itemDisplay, tooltipObjectProperty.get());
							tooltipObjectProperty.set(null);
						}
						if (newValue != null && !newValue.trim().isEmpty()) {
							tooltipObjectProperty.set(new Tooltip(newValue));
							trySetDelay(tooltipObjectProperty.get());
							Tooltip.install(itemDisplay, tooltipObjectProperty.get());
						}
					}
				});
			}
		}
		return itemDisplay;
	}
	
	private void trySetDelay(Tooltip tooltip) {
		for (Method method : tooltip.getClass().getMethods()) {
			if (method.getName().equals("setShowDelay")) {
				try {
					method.invoke(tooltip, Duration.millis(50));
				}
				catch (Exception e) {
					// ignore
				}
			}
		}
	}
	
	private void refreshItemDisplayIcon() {
		// it may not have been instantiated yet, for example when changing the leaf property really quickly
		if (displayIcon != null) {
			displayIcon.getChildren().clear();
			if (!item.leafProperty().getValue()) {
				displayIcon.getChildren().add(expanded.get() ? expandedIcon : collapsedIcon);
				// listen to changes
				expanded.addListener(new ChangeListener<Boolean>() {
					@Override
					public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean newValue) {
						// expanded
						if (newValue)
							displayIcon.getChildren().set(0, expandedIcon);
						else
							displayIcon.getChildren().set(0, collapsedIcon);
					}
				});
			}
			else
				displayIcon.getChildren().add(itemIcon);
		}
	}

	public TreeCell<T> getCell(TreeItem<T> item) {
		initialize();
		if (this.item.getChildren().contains(item) && !children.containsKey(item)) {
			isDirty = true;
		}
		refreshItemContainer(true);
		getNode();
		return children.get(item);
	}
	
	public HBox getItemDisplay() {
		initialize();
		getNode();
		return itemDisplay;
	}
	
	public VBox getItemContainer() {
		initialize();
		getNode();
		return itemContainer;
	}
	
	public TreeCellValue<T> getCellValue() {
		if (cellValue == null) {
			cellValue = tree.getCellValueFactory().call(item);
			cellValue.cellProperty().setValue(this);
			cellValue.getNode().getStyleClass().add("treeCellValue");
		}
		return cellValue;
	}
	
	public BooleanProperty expandedProperty() {
		return expanded;
	}
	
	/**
	 * Exposed as read-only. Use the selection model to actually select something
	 * @return
	 */
	public ReadOnlyBooleanProperty selectedProperty() {
		return selected;
	}
	
	public TreeCell<T> getParent() {
		return parent;
	}
	
	static Image loadImage(String name) {
		if (!images.containsKey(name)) {
			synchronized(images) {
				if (!images.containsKey(name)) {
					InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
					try {
						images.put(name, new Image(input));
					}
					finally {
						try {
							input.close();
						}
						catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				}
			}
		}
		return images.get(name);
	}
	
	static ImageView loadGraphic(String name) {
		return new ImageView(loadImage(name));
	}
	
	@Override
	public String toString() {
		return "TreeCell: " + item; 
	}
	
	/**
	 * Each TreeCell node can consist of numerous child nodes, each of those child nodes can create an event
	 * The target of this class is to take events from any child node and refire it as an event from this node
	 * This node has a link to the treecell (using userdata) so you can catch them all on the root 
	 */
	private static class CloneEventHandler<T extends Event> implements EventHandler<T> {

		private HBox node;
		
		public CloneEventHandler(HBox node) {
			this.node = node;
		}
		
		@Override
		public void handle(T event) {
			// if the target is already this node, don't fire it again
			// otherwise if the target is already pointed at a specific treecell-linked node, don't fire it again, it will bubble up naturally
			if (!event.isConsumed() && !event.getTarget().equals(node) && (!(event.getTarget() instanceof Node && ((Node) event.getTarget()).getUserData() instanceof TreeCell))) {
				Event copy = event.copyFor(node, node);
				// consume the original event
				event.consume();
				node.fireEvent(copy);
			}
		}
	}
	
	public double getLocalWidth() {
		if (parent != null && (!isLoaded || !parent.expanded.get() || !getNode().visibleProperty().get())) {
			return 0;
		}
		getCellValue().getNode().autosize();
		double width = Math.max(50, getCellValue().getNode().widthProperty().get());
		double biggestChild = 0;
		if (expanded.get()) {
			for (final TreeCell<T> child : children.values()) {
				double childWidth = child.getLocalWidth();
				if (childWidth > biggestChild) {
					biggestChild = childWidth;
				}
			}
		}
		return width + biggestChild;
	}

	@Override
	public void focus() {
		if (getCellValue() instanceof Focusable) {
			((Focusable) getCellValue()).focus();
		}
		else {
			getNode().requestFocus();
		}
	}

	public void setDirty(boolean isDirty) {
		this.isDirty = isDirty;
	}
	
	public boolean isInitialized() {
		return isInitialized;
	}
	
	public boolean isLoaded() {
		return isInitialized && isLoaded;
	}
	
	public BooleanProperty hideSelfProperty() {
		return hideSelf;
	}
}

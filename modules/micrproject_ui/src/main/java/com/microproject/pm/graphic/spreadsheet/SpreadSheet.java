/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.pm.graphic.spreadsheet;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.netbeans.swing.outline.RenderDataProvider;

import com.microproject.dialog.ResourceAdditionDialog;
import com.microproject.help.HelpUtil;
import com.microproject.menu.MenuActionConstants;
import com.microproject.pm.graphic.IconManager;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.collaboration.CollaborationHelper;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetAction;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.common.transfer.NodeListTransferHandler;
import com.microproject.pm.graphic.spreadsheet.common.transfer.NodeListTransferable;
import com.microproject.pm.graphic.spreadsheet.editor.SimpleComboBoxEditor;
import com.microproject.pm.graphic.spreadsheet.renderer.NameCellComponent;
import com.microproject.pm.graphic.spreadsheet.selection.SpreadSheetListSelectionModel;
import com.microproject.pm.graphic.spreadsheet.selection.SpreadSheetSelectionModel;
import com.microproject.pm.graphic.spreadsheet.selection.event.HeaderMouseListener;
import com.microproject.datatype.Hyperlink;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.ActionList;
import com.microproject.graphic.configuration.CellStyle;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeBridge;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.job.Job;
import com.microproject.job.JobRunnable;
import com.microproject.options.GeneralOption;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.server.data.EnterpriseResourceData;
import com.microproject.server.data.Serializer;
import com.microproject.session.Session;
import com.microproject.session.SessionFactory;
import com.microproject.strings.Messages;
import com.microproject.util.Alert;
import com.microproject.util.BrowserControl;

/**
 * 
 */
@SuppressWarnings("unchecked")
public class SpreadSheet extends CommonSpreadSheet implements Cloneable {
	private static final Logger logger = Logger.getLogger(SpreadSheet.class.getName());
	private static final Map<NodeModel, Map<String, List<WeakReference<SpreadSheet>>>> ACTIVE_LAYOUT_TARGETS = new WeakHashMap<>();
	private NodeModel registeredLayoutModel;
	private String registeredLayoutCategory;
	private static final long serialVersionUID = 5958334223191182318L;
	public static final String NAME_COLUMN_INDENT_ACTION = "spreadsheet.nameColumnIndent";
	public static final String NAME_COLUMN_OUTDENT_ACTION = "spreadsheet.nameColumnOutdent";
	private static final String NAME_COLUMN_JUMP_PREVIOUS_ACTION = "spreadsheet.nameColumnJumpPrevious";
	private static final String NAME_COLUMN_JUMP_NEXT_ACTION = "spreadsheet.nameColumnJumpNext";
	private static final String CLIPBOARD_PASTE_VALUES_ACTION = "spreadsheet.clipboardPasteValues";
	public static final String MOVE_TASK_UP_ACTION = "spreadsheet.moveTaskUp";
	public static final String MOVE_TASK_DOWN_ACTION = "spreadsheet.moveTaskDown";
	private Object defaultTabActionKey;
	private Object defaultShiftTabActionKey;
	protected SpreadSheetPopupMenu popup=null;
	private boolean hierarchyActionInProgress;
	private boolean tableMouseHandlerInstalled;
	private String[] actionList = null;
	private Map<String, CommonSpreadSheetAction> actionMap = null;


	public SpreadSheet() {
		super();
		NodeListTransferHandler.registerWith(this);
		// Issue #47: prevent the TransferHandler from also binding ctrl C/X/V on the
		// component's WHEN_FOCUSED input map. Those keys are wired globally to
		// ACTION_COPY/CUT/PASTE on the root-pane WHEN_IN_FOCUSED_WINDOW by
		// GraphicManager.applyDocumentShortcuts. Leaving both layers bound makes
		// ctrl+V non-deterministic ("paste doesn't work well"). The real paste logic
		// (NodeListTransferHandler.importData) stays reachable via ACTION_PASTE, so we
		// only remove the duplicate keyboard bindings here.
		removeDuplicateClipboardKeyBindings(this);
		installClipboardPasteBindings();
		installTaskMoveBindings(this);

	}

	private static void removeDuplicateClipboardKeyBindings(JComponent c) {
		InputMap focused = c.getInputMap(JComponent.WHEN_FOCUSED);
		focused.remove(KeyStroke.getKeyStroke("ctrl V"));
		focused.remove(KeyStroke.getKeyStroke("ctrl C"));
		focused.remove(KeyStroke.getKeyStroke("ctrl X"));
	}

	public void installTaskMoveBindings(JComponent component) {
		if (component == null)
			return;
		InputMap inputMap = component.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap actionMap = component.getActionMap();
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK), MOVE_TASK_UP_ACTION);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK), MOVE_TASK_DOWN_ACTION);
		actionMap.put(MOVE_TASK_UP_ACTION, new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(ActionEvent event) { moveSelectedTaskRows(-1); }
		});
		actionMap.put(MOVE_TASK_DOWN_ACTION, new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override public void actionPerformed(ActionEvent event) { moveSelectedTaskRows(1); }
		});
	}

	public boolean moveSelectedTaskRows(int direction) {
		// Microsoft Project moves the selected task from any selected cell (a single
		// cell in the task row is enough), not only when the entire row is selected.
		// Requiring the whole row broke the keyboard move after a "select column then
		// click a cell" sequence, which collapses the selection to a single cell and
		// silently rejected the move. See issue #45.
		return moveSelectedTaskRows(direction, false);
	}

	public boolean moveSelectedTaskRowsFromCommand(int direction) {
		return moveSelectedTaskRows(direction, false);
	}

	public boolean canMoveSelectedTaskRows(int direction, boolean requireEntireRow) {
		if (requireEntireRow && !hasEntireRowSelection())
			return false;
		return hasOnlyTaskRowsSelected() && getCache().canMoveNodes(getSelectedGraphicNodes(), direction);
	}

	private boolean moveSelectedTaskRows(int direction, boolean requireEntireRow) {
		finishCurrentOperations();
		if (!canMoveSelectedTaskRows(direction, requireEntireRow))
			return false;
		List<Node> nodes = new ArrayList<Node>(getSelectedNodes());
		if (nodes.isEmpty() || !CollaborationHelper.tryLockNodes(null, nodes, this, "move task"))
			return false;
		boolean moved = getCache().moveNodes(getSelectedGraphicNodes(), direction);
		if (moved) {
			refreshTaskMoveViews();
			restoreTaskRowSelection(nodes);
		}
		return moved;
	}

	public boolean canMoveSelectedTaskRowsTo(int targetRow, boolean after) {
		// Microsoft Project lets you drag a selected task to a new position from any
		// selected cell, not only when the entire row is selected. Requiring the whole
		// row here (while the keyboard/ribbon move paths no longer do) was an extra
		// source of the "refresh sometimes does not work" symptom reported in issue
		// #45: after a "select column then click a cell" sequence the selection is a
		// single cell and the drop was silently rejected. See issue #45.
		if (!(getModel() instanceof SpreadSheetModel model))
			return false;
		if (targetRow < 0 || targetRow >= getRowCount() || !hasOnlyTaskRowsSelected())
			return false;
		GraphicNode target = model.getNode(targetRow);
		return target != null && target.getNode() != null
			&& getCache().canRelocateNodes(getSelectedGraphicNodes(), target.getNode(), after);
	}

	public boolean moveSelectedTaskRowsTo(int targetRow, boolean after) {
		finishCurrentOperations();
		if (!canMoveSelectedTaskRowsTo(targetRow, after) || !(getModel() instanceof SpreadSheetModel model))
			return false;
		GraphicNode target = model.getNode(targetRow);
		List<Node> nodes = new ArrayList<Node>(getSelectedNodes());
		List<Node> locks = new ArrayList<Node>(nodes);
		if (!locks.contains(target.getNode()))
			locks.add(target.getNode());
		if (nodes.isEmpty() || !CollaborationHelper.tryLockNodes(null, locks, this, "drag task"))
			return false;
		boolean moved = getCache().relocateNodes(getSelectedGraphicNodes(), target.getNode(), after);
		if (moved) {
			refreshTaskMoveViews();
			restoreTaskRowSelection(nodes);
		}
		return moved;
	}

	private boolean hasEntireRowSelection() {
		return getSelectedRowCount() > 0 && getColumnCount() > 0 && getSelectedColumnCount() == getColumnCount();
	}

	private boolean hasOnlyTaskRowsSelected() {
		List<Node> nodes = getSelectedNodes();
		if (nodes.isEmpty())
			return false;
		for (Node node : nodes)
			if (node == null || !(node.getImpl() instanceof com.microproject.pm.task.Task))
				return false;
		return true;
	}

	private void refreshTaskMoveViews() {
		getCache().getReference().update();
		if (getModel() instanceof CommonSpreadSheetModel model)
			model.fireUpdateAll();
		revalidate();
		repaint();
		if (getRowHeader() != null) {
			getRowHeader().revalidate();
			getRowHeader().repaint();
		}
		if (getParent() != null)
			getParent().repaint();
		// Moving rows posts a NodeRelocationEdit through the model.  Unlike field
		// edits, this route does not pass through DocumentFrame's edit listener, so
		// the root-pane Ctrl+Z action can remain disabled even though the edit is
		// undoable.  Refresh it after both command and drag moves (which share this
		// method) so the one global shortcut sees the new undo state immediately.
		GraphicManager graphicManager = GraphicManager.getInstance(this);
		if (graphicManager != null) {
			DocumentFrame documentFrame = graphicManager.getCurrentFrame();
			if (documentFrame != null)
				documentFrame.refreshUndoButtons();
		}
	}

	private void restoreTaskRowSelection(List<Node> nodes) {
		if (!(getModel() instanceof SpreadSheetModel model) || nodes == null || nodes.isEmpty())
			return;
		clearSelection();
		setRowHeaderSelectionActive(true);
		boolean first = true;
		int firstRow = -1;
		for (Node node : nodes) {
			GraphicNode graphicNode = (GraphicNode)getCache().getGraphicNode(node);
			int row = graphicNode == null ? -1 : model.findGraphicNodeRow(graphicNode);
			if (row < 0)
				continue;
			if (first) {
				getSelectionModel().setSelectionInterval(row, row);
				firstRow = row;
				first = false;
			} else {
				getSelectionModel().addSelectionInterval(row, row);
			}
		}
		if (!first && getColumnCount() > 0)
			getColumnModel().getSelectionModel().setSelectionInterval(0, getColumnCount() - 1);
		if (firstRow >= 0)
			scrollRectToVisible(getCellRect(firstRow, 0, true));
	}

	/** Copies each selected column's first selected value into the remaining selected rows. */
	public void fillDownSelection() {
		finishCurrentOperations();
		int[] rows = getSelectedRows();
		int[] columns = getSelectedColumns();
		if (rows.length < 2)
			return;
		if (columns.length == 0 && getSelectedColumn() >= 0)
			columns = new int[] { getSelectedColumn() };
		for (int column : columns) {
			Object value = getValueAt(rows[0], column);
			for (int index = 1; index < rows.length; index++) {
				if (getModel().isCellEditable(rows[index], column))
					setValueAt(value, rows[index], column);
			}
		}
	}

	private void installClipboardPasteBindings() {
		var inputMap = getInputMap(JComponent.WHEN_FOCUSED);
		// Ctrl+V (normal paste) is wired globally on the document root pane via
		// ACTION_PASTE (NodeListTransferHandler). Keep only the "paste values" variant
		// here so a single key resolves to a single action across the app.
		inputMap.put(KeyStroke.getKeyStroke("shift ctrl V"), CLIPBOARD_PASTE_VALUES_ACTION);

		var actionMap = getActionMap();
		actionMap.put(CLIPBOARD_PASTE_VALUES_ACTION, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				pasteClipboardAsValues();
			}
		});
	}

	public void pasteClipboardAsValues() {
		finishCurrentOperations();
		var transferable = getClipboardContents();
		if (transferable.isEmpty()) {
			return;
		}
		pasteClipboardAsValues(transferable.get());
	}

	void pasteClipboardAsValues(Transferable transferable) {
		if (transferable == null) {
			return;
		}
		var text = getClipboardText(transferable);
		if (text.isPresent()) {
			if (!prepareCellPaste()) {
				return;
			}
			if (!NodeListTransferable.pasteString(text.get(), this)) {
				Alert.error(Messages.getString("Message.invalidInput"));
			}
			return;
		}
		if (hasNodeListFlavor(transferable)) {
			pasteClipboardContents(transferable);
			return;
		}
		pasteClipboardContents(transferable);
	}

	public void insertClipboardContents() {
		pasteClipboardInsertedContents();
	}

	public void pasteClipboardContents() {
		getClipboardContents().ifPresent(this::pasteClipboardContents);
	}

	public void pasteClipboardInsertedContents() {
		getClipboardContents().ifPresent(this::pasteInsertedClipboardContents);
	}

	private void pasteClipboardContents(Transferable transferable) {
		if (transferable == null) {
			return;
		}
		if (getTransferHandler() instanceof NodeListTransferHandler transferHandler) {
			if (!transferHandler.importData(this, transferable)) {
				Alert.error(Messages.getString("Message.invalidInput"));
			}
			return;
		}
		if (NodeListTransferHandler.getPasteAction() != null) {
			NodeListTransferHandler.getPasteAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
		}
	}

	private void pasteInsertedClipboardContents(Transferable transferable) {
		if (transferable == null) {
			return;
		}
		if (hasNodeListFlavor(transferable)) {
			pasteClipboardContents(transferable);
			return;
		}
		var text = getClipboardText(transferable);
		if (text.isEmpty()) {
			Alert.error(Messages.getString("Message.invalidInput"));
			return;
		}
		NodeModel model = ((CommonSpreadSheetModel)getModel()).getCache().getModel();
		List<Field> fields = getSelectedFields();
		if (fields == null || fields.isEmpty()) {
			fields = getSelectableFields();
		}
		List<Node> nodes = NodeListTransferable.stringToNodeList(text.get(), this,
			fields, model.getDataFactory());
		if (nodes.isEmpty() || !pasteNodesFromClipboard(nodes)) {
			Alert.error(Messages.getString("Message.invalidInput"));
		}
	}

	private Optional<Transferable> getClipboardContents() {
		try {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			return Optional.ofNullable((clipboard == null) ? null : clipboard.getContents(null));
		} catch (IllegalStateException ignored) {
			return Optional.empty();
		}
	}

	private Optional<String> getClipboardText(Transferable transferable) {
		try {
			if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				return Optional.of((String) transferable.getTransferData(DataFlavor.stringFlavor));
			}
			if (transferable.isDataFlavorSupported(DataFlavor.getTextPlainUnicodeFlavor())) {
				Object data = transferable.getTransferData(DataFlavor.getTextPlainUnicodeFlavor());
				if (data instanceof Reader reader) {
					StringWriter writer = new StringWriter();
					reader.transferTo(writer);
					return Optional.of(writer.toString());
				}
				return Optional.of(data.toString());
			}
		} catch (UnsupportedFlavorException ignored) {
			return Optional.empty();
		} catch (IOException ignored) {
			return Optional.empty();
		}
		return Optional.empty();
	}

	private boolean hasNodeListFlavor(Transferable transferable) {
		for (DataFlavor flavor : transferable.getTransferDataFlavors()) {
			if (NodeListTransferable.isNodeListFlavor(flavor)) {
				return true;
			}
		}
		return false;
	}

	public boolean pasteNodesFromClipboard(List<Node> pastedNodes) {
		if (pastedNodes == null || pastedNodes.isEmpty() || isClipboardTargetReadOnly()) {
			return false;
		}
		finishCurrentOperations();
		List<Node> selectedNodes = getSelectedNodes();
		if (!CollaborationHelper.tryLockNodes(null, selectedNodes, this, "paste")) {
			return false;
		}
		Node parent = null;
		int position = 0;
		if (!selectedNodes.isEmpty()) {
			Node node = selectedNodes.get(0);
			parent = (Node)node.getParent();
			if (parent != null) {
				position = ((NodeBridge)parent).getIndex(node);
			}
		}
		clearSelection();
		return getCache().pasteNodes(parent, pastedNodes, position);
	}

	public boolean prepareCellPaste() {
		if (isClipboardTargetReadOnly()) {
			return false;
		}
		finishCurrentOperations();
		return CollaborationHelper.tryLockNodes(null, getSelectedNodes(), this, "paste");
	}

	public boolean cutSelectedCellValues(int[] rows, int[] columns) {
		if (rows == null || columns == null || rows.length == 0 || columns.length == 0 || isClipboardTargetReadOnly()) {
			return false;
		}
		List<Node> selectedNodes = getSelectedNodes();
		if (!CollaborationHelper.tryLockNodes(null, selectedNodes, this, "cut")) {
			return false;
		}
		boolean cleared = false;
		for (int row : rows) {
			for (int column : columns) {
				if (isCellEditable(row, column)) {
					setValueAt("", row, column);
					cleared = true;
				}
			}
		}
		return cleared;
	}

	public boolean commitTaskCut(List<Node> selectedNodes) {
		if (selectedNodes == null || selectedNodes.isEmpty() || isClipboardTargetReadOnly()) {
			return false;
		}
		finishCurrentOperations();
		List<Node> nodes = getSelectedCuttableRows(new ArrayList<>(selectedNodes));
		if (nodes.isEmpty() || !CollaborationHelper.tryLockNodes(null, nodes, this, "cut")) {
			return false;
		}
		getCache().deleteNodes(nodes);
		return true;
	}

	private boolean isClipboardTargetReadOnly() {
		if (isReadOnly()) return true;
		NodeModel model=((CommonSpreadSheetModel)getModel()).getCache().getModel();
		return model.getDataFactory() instanceof Project project && project.isReadOnly();
	}

	public void cleanUp() {
		unregisterLayoutTarget();
		if (getModel() instanceof CommonSpreadSheetModel commonModel) {
			NodeModelCache currentCache = commonModel.getCache();
			if (currentCache != null) {
				currentCache.removeNodeModelListener(this);
			}
		}
		super.cleanUp();
	}
	public void setCache(NodeModelCache cache, ArrayList fieldArray, CellStyle cellStyle, ActionList actionList) {
		unregisterLayoutTarget();
		// if (getCache()!=null) getCache().close();
		if (getCache() != null) {
			getCache().getReference().close(); // deepClose
		}
		
		var oldColModel = getColumnModel();
		var colModel = (oldColModel instanceof SpreadSheetColumnModel spreadSheetColumnModel
				&& spreadSheetColumnModel.getFieldArray() == fieldArray)
				? spreadSheetColumnModel
				: new SpreadSheetColumnModel(fieldArray);
		setModel(new SpreadSheetModel(cache, colModel, cellStyle, actionList), (colModel == oldColModel) ? null : colModel);
		configureOutlineRendering();
		registerLayoutTarget();

	}

	@Override
	public void setSpreadSheetCategory(String spreadSheetCategory) {
		unregisterLayoutTarget();
		super.setSpreadSheetCategory(spreadSheetCategory);
		registerLayoutTarget();
	}

	private void registerLayoutTarget() {
		NodeModelCache currentCache = getCache();
		NodeModel currentModel = currentCache == null ? null : currentCache.getModel();
		String category = getSpreadSheetCategory();
		if (currentModel == null || category == null)
			return;
		registerLayoutTarget(currentModel, category, this);
		registeredLayoutModel = currentModel;
		registeredLayoutCategory = category;
	}

	static void registerLayoutTarget(NodeModel model, String category, SpreadSheet sheet) {
		synchronized (ACTIVE_LAYOUT_TARGETS) {
			List<WeakReference<SpreadSheet>> targets = ACTIVE_LAYOUT_TARGETS
					.computeIfAbsent(model, ignored -> new HashMap<>())
					.computeIfAbsent(category, ignored -> new ArrayList<>());
			for (int index = targets.size() - 1; index >= 0; index--) {
				SpreadSheet target = targets.get(index).get();
				if (target == null || target == sheet)
					targets.remove(index);
			}
			targets.add(new WeakReference<>(sheet));
		}
	}

	private void unregisterLayoutTarget() {
		if (registeredLayoutModel == null || registeredLayoutCategory == null)
			return;
		unregisterLayoutTarget(registeredLayoutModel, registeredLayoutCategory, this);
		registeredLayoutModel = null;
		registeredLayoutCategory = null;
	}

	static void unregisterLayoutTarget(NodeModel model, String category, SpreadSheet sheet) {
		synchronized (ACTIVE_LAYOUT_TARGETS) {
			Map<String, List<WeakReference<SpreadSheet>>> targetsByCategory = ACTIVE_LAYOUT_TARGETS.get(model);
			if (targetsByCategory != null) {
				List<WeakReference<SpreadSheet>> targets = targetsByCategory.get(category);
				if (targets != null) {
					for (int index = targets.size() - 1; index >= 0; index--) {
						SpreadSheet target = targets.get(index).get();
						if (target == null || target == sheet)
							targets.remove(index);
					}
					if (targets.isEmpty())
						targetsByCategory.remove(category);
				}
				if (targetsByCategory.isEmpty())
					ACTIVE_LAYOUT_TARGETS.remove(model);
			}
		}
	}

	static SpreadSheet findLayoutTarget(NodeModel model, String category) {
		return findLayoutTarget(model, category, null);
	}

	static SpreadSheet findLayoutTarget(NodeModel model, String category, SpreadSheet preferred) {
		if (model == null || category == null)
			return null;
		synchronized (ACTIVE_LAYOUT_TARGETS) {
			Map<String, List<WeakReference<SpreadSheet>>> targetsByCategory = ACTIVE_LAYOUT_TARGETS.get(model);
			List<WeakReference<SpreadSheet>> targets = targetsByCategory == null ? null : targetsByCategory.get(category);
			if (targets == null)
				return null;
			SpreadSheet latest = null;
			for (int index = targets.size() - 1; index >= 0; index--) {
				SpreadSheet target = targets.get(index).get();
				if (target == null) {
					targets.remove(index);
					continue;
				}
				if (latest == null)
					latest = target;
				if (target == preferred)
					return target;
			}
			if (targets.isEmpty()) {
				targetsByCategory.remove(category);
				if (targetsByCategory.isEmpty())
					ACTIVE_LAYOUT_TARGETS.remove(model);
			}
			return latest;
		}
	}

	private void configureOutlineRendering() {
		if (!(getModel() instanceof SpreadSheetModel model)) {
			return;
		}
		setRenderDataProvider(new RenderDataProvider() {
			@Override
			public String getDisplayName(Object o) {
				return lookupDisplayName(model, o);
			}

			@Override
			public boolean isHtmlDisplayName(Object o) {
				return false;
			}

			@Override
			public java.awt.Color getBackground(Object o) {
				return null;
			}

			@Override
			public java.awt.Color getForeground(Object o) {
				return null;
			}

			@Override
			public javax.swing.Icon getIcon(Object o) {
				return lookupIcon(o);
			}

			@Override
			public String getTooltipText(Object o) {
				return lookupDisplayName(model, o);
			}
		});
	}

	private javax.swing.Icon lookupIcon(Object treeObject) {
		if (!(treeObject instanceof GraphicNode graphicNode)) {
			return null;
		}
		if (graphicNode.isLazyParent()) {
			if (!graphicNode.isFetched()) {
				return IconManager.getIcon("spreadsheet.unfetchedLazy.icon");
			}
			return graphicNode.isCollapsed()
				? IconManager.getIcon("spreadsheet.fetchedLazyCollapsed.icon")
				: IconManager.getIcon("spreadsheet.fetchedLazyExpanded.icon");
		}
		if (graphicNode.isComposite()) {
			return graphicNode.isCollapsed()
				? IconManager.getIcon("spreadsheet.collapsed.icon")
				: IconManager.getIcon("spreadsheet.expanded.icon");
		}
		return graphicNode.isVoid()
			? IconManager.getIcon("spreadsheet.emptyleaf.icon")
			: IconManager.getIcon("spreadsheet.leaf.icon");
	}

	private String lookupDisplayName(SpreadSheetModel model, Object treeObject) {
		if (treeObject instanceof GraphicNode graphicNode) {
			NodeModelCache currentCache = model.getCache();
			int row = currentCache == null ? -1 : currentCache.getRowAt(graphicNode);
			if (row >= 0) {
				Object value = model.getValueAt(row, 0);
				if (value != null) {
					return String.valueOf(value);
				}
			}
			var node = graphicNode.getNode();
			if (node != null && node.getImpl() != null) {
				return String.valueOf(node.getImpl());
			}
		}
		return treeObject == null ? "" : String.valueOf(treeObject);
	}

	public TableCellEditor getCellEditor(int row, int column) {
		if (!(getModel() instanceof SpreadSheetModel model)) {
			return super.getCellEditor(row, column);
		}
		var field = model.getFieldInViewColumn(column);
		var node = model.getNode(row);
		if (field != null && node != null && node.getNode() != null && (field.isDynamicOptions() || field.hasFilter())) {
			return new SimpleComboBoxEditor(new DefaultComboBoxModel(field.getOptions(node.getNode().getImpl())));
		} else {
			return super.getCellEditor(row, column);
		}
	}

	public boolean isNameFieldColumn(int column) {
		if (column < 0 || !(getModel() instanceof SpreadSheetModel))
			return false;
		Field field = ((SpreadSheetModel) getModel()).getFieldInViewColumn(column);
		return field != null && field.isNameField();
	}

	public boolean isNameCellTabActionEnabled() {
		int column = isEditing() ? getEditingColumn() : getSelectedColumn();
		return isNameFieldColumn(column);
	}

	public void executeNameCellTabAction(boolean outdent) {
		if (hierarchyActionInProgress || !isNameCellTabActionEnabled())
			return;
		var rowToFocus = getCurrentRow();
		if (rowToFocus < 0)
			rowToFocus = getSelectionModel().getAnchorSelectionIndex();
		GraphicNode focusNode = null;
		if (rowToFocus >= 0 && rowToFocus < getRowCount() && getModel() instanceof SpreadSheetModel) {
			focusNode = ((SpreadSheetModel) getModel()).getNode(rowToFocus);
		}
		hierarchyActionInProgress = true;
		try {
			finishCurrentOperations();
			focusSingleNameRow(rowToFocus);
			executeAction(outdent ? MenuActionConstants.ACTION_OUTDENT : MenuActionConstants.ACTION_INDENT);
			restoreNameColumnFocus(focusNode, rowToFocus);
		} finally {
			hierarchyActionInProgress = false;
		}
	}

	public boolean canIndentCurrentNameRow() {
		var row = getCurrentRow();
		if (row < 0 || row >= getRowCount())
			return false;
		if (!(getModel() instanceof SpreadSheetModel))
			return false;
		var model = (SpreadSheetModel) getModel();
		var graphicNode = model.getNode(row);
		if (graphicNode == null)
			return false;
		var node = graphicNode.getNode();
		if (node == null || node.isRoot() || !node.isIndentable(1))
			return false;
		var parent = (Node) node.getParent();
		if (parent == null)
			return false;
		var index = parent.getIndex(node);
		if (index <= 0)
			return false;
		for (var siblingIndex = index - 1; siblingIndex >= 0; siblingIndex--) {
			var sibling = (Node) parent.getChildAt(siblingIndex);
			if (node.canBeChildOf(sibling))
				return true;
			if (!sibling.isVoid())
				break;
		}
		return false;
	}

	public boolean canOutdentCurrentNameRow() {
		var row = getCurrentRow();
		if (row < 0 || row >= getRowCount())
			return false;
		if (!(getModel() instanceof SpreadSheetModel))
			return false;
		var model = (SpreadSheetModel) getModel();
		var graphicNode = model.getNode(row);
		if (graphicNode == null)
			return false;
		var node = graphicNode.getNode();
		if (node == null || node.isRoot() || !node.isIndentable(-1))
			return false;
		var parent = (Node) node.getParent();
		if (parent == null || parent.isRoot() || parent.isLazyParent())
			return false;
		return true;
	}

	public void executeNameCellCollapseExpand(boolean expand) {
		if (hierarchyActionInProgress)
			return;
		if (!(getModel() instanceof SpreadSheetModel model))
			return;
		var column = isEditing() ? getEditingColumn() : getSelectedColumn();
		if (!isNameFieldColumn(column))
			return;
		var rowToFocus = getCurrentRow();
		if (rowToFocus < 0)
			rowToFocus = getSelectionModel().getAnchorSelectionIndex();
		if (rowToFocus < 0)
			return;
		var focusNode = model.getNode(rowToFocus);
		if (focusNode == null)
			return;
		hierarchyActionInProgress = true;
		try {
			finishCurrentOperations();
			focusSingleNameRow(rowToFocus);
			executeAction(expand ? MenuActionConstants.ACTION_EXPAND : MenuActionConstants.ACTION_COLLAPSE);
			restoreNameColumnFocus(focusNode, rowToFocus);
		} finally {
			hierarchyActionInProgress = false;
		}
	}

	public void executeNameCellHierarchyJump(boolean forward) {
		if (hierarchyActionInProgress || !isNameCellTabActionEnabled())
			return;
		if (!(getModel() instanceof SpreadSheetModel model))
			return;
		var rowToFocus = getCurrentRow();
		if (rowToFocus < 0)
			rowToFocus = getSelectionModel().getAnchorSelectionIndex();
		if (rowToFocus < 0 || rowToFocus >= getRowCount())
			return;
		var sourceNode = model.getNode(rowToFocus);
		if (sourceNode == null)
			return;
		var targetRow = findSameLevelVisibleRow(model, rowToFocus, forward);
		if (targetRow < 0)
			return;
		var targetNode = model.getNode(targetRow);
		if (targetNode == null)
			return;
		hierarchyActionInProgress = true;
		try {
			finishCurrentOperations();
			focusSingleNameRow(targetRow);
			restoreNameColumnFocus(targetNode, targetRow);
		} finally {
			hierarchyActionInProgress = false;
		}
	}

	private void installNameColumnHierarchyNavigationActions() {
		var inputMap = getInputMap(JComponent.WHEN_FOCUSED);
		var actionMap = getActionMap();
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.CTRL_DOWN_MASK), "spreadsheet.nameColumnCollapseExpandLeft");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.CTRL_DOWN_MASK), "spreadsheet.nameColumnCollapseExpandRight");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.CTRL_DOWN_MASK), NAME_COLUMN_JUMP_PREVIOUS_ACTION);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.CTRL_DOWN_MASK), NAME_COLUMN_JUMP_NEXT_ACTION);
		actionMap.put("spreadsheet.nameColumnCollapseExpandLeft", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				executeNameCellCollapseExpand(false);
			}
		});
		actionMap.put("spreadsheet.nameColumnCollapseExpandRight", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				executeNameCellCollapseExpand(true);
			}
		});
		actionMap.put(NAME_COLUMN_JUMP_PREVIOUS_ACTION, new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				executeNameCellHierarchyJump(false);
			}
		});
		actionMap.put(NAME_COLUMN_JUMP_NEXT_ACTION, new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				executeNameCellHierarchyJump(true);
			}
		});
	}

	private void focusSingleNameRow(int row) {
		int nameColumn = findNameColumn();
		if (row < 0 || row >= getRowCount() || nameColumn < 0)
			return;
		getSelectionModel().setSelectionInterval(row, row);
		getColumnModel().getSelectionModel().setSelectionInterval(nameColumn, nameColumn);
	}

	private void restoreNameColumnFocus(GraphicNode preferredNode, int preferredRow) {
		int nameColumn = findNameColumn();
		if (nameColumn < 0)
			return;
		int row = findRowForGraphicNode(preferredNode);
		if (row < 0)
			row = preferredRow;
		if (row < 0 || row >= getRowCount())
			row = getCurrentRow();
		if (row < 0 || row >= getRowCount())
			row = Math.max(0, Math.min(getSelectedRow(), getRowCount() - 1));
		if (row < 0 || row >= getRowCount())
			return;
		requestFocusInWindow();
		if (getSelectedRowCount() == 0)
			getSelectionModel().setSelectionInterval(row, row);
		getColumnModel().getSelectionModel().setSelectionInterval(nameColumn, nameColumn);
		scrollRectToVisible(getCellRect(row, nameColumn, true));
	}

	private int findRowForGraphicNode(GraphicNode node) {
		if (node == null || !(getModel() instanceof SpreadSheetModel))
			return -1;
		return ((SpreadSheetModel)getModel()).findGraphicNodeRow(node);
	}

	private int findNameColumn() {
		if (!(getModel() instanceof SpreadSheetModel))
			return -1;
		SpreadSheetModel model = (SpreadSheetModel)getModel();
		for (int column = 0; column < getColumnCount(); column++) {
			Field field = model.getFieldInViewColumn(column);
			if (field != null && field.isNameField())
				return column;
		}
		return -1;
	}

	private int findSameLevelVisibleRow(SpreadSheetModel model, int startRow, boolean forward) {
		if (model == null || startRow < 0 || startRow >= getRowCount())
			return -1;
		var cache = model.getCache();
		if (cache == null)
			return -1;
		var sourceNode = model.getNode(startRow);
		if (sourceNode == null)
			return -1;
		var sourceLevel = cache.getLevel(sourceNode);
		int step = forward ? 1 : -1;
		for (int row = startRow + step; row >= 0 && row < getRowCount(); row += step) {
			var candidate = model.getNode(row);
			if (candidate == null || candidate.isVoid())
				continue;
			if (cache.getLevel(candidate) == sourceLevel)
				return row;
		}
		return -1;
	}

	private void installNameColumnTabActions() {
		var inputMap = getInputMap(JComponent.WHEN_FOCUSED);
		var actionMap = getActionMap();
		defaultTabActionKey = inputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
		defaultShiftTabActionKey = inputMap.get(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK));
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), NAME_COLUMN_INDENT_ACTION);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK), NAME_COLUMN_OUTDENT_ACTION);
		actionMap.put(NAME_COLUMN_INDENT_ACTION, new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				if (isNameCellTabActionEnabled()) {
					executeNameCellTabAction(false);
					return;
				}
				invokeBoundAction(defaultTabActionKey, e);
			}
		});
		actionMap.put(NAME_COLUMN_OUTDENT_ACTION, new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				if (isNameCellTabActionEnabled()) {
					executeNameCellTabAction(true);
					return;
				}
				invokeBoundAction(defaultShiftTabActionKey, e);
			}
		});
	}

	@Override
	protected boolean handleHierarchyNavigationKeyEvent(KeyEvent e) {
		if (e == null || e.getID() != KeyEvent.KEY_PRESSED)
			return false;
		var column = getSelectedColumn();
		if (!isNameFieldColumn(column))
			return false;
		if ((e.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK | KeyEvent.META_DOWN_MASK)) != KeyEvent.CTRL_DOWN_MASK)
			return false;
		return switch (e.getKeyCode()) {
			case KeyEvent.VK_LEFT -> {
				executeNameCellCollapseExpand(false);
				yield true;
			}
			case KeyEvent.VK_RIGHT -> {
				executeNameCellCollapseExpand(true);
				yield true;
			}
			case KeyEvent.VK_UP -> {
				executeNameCellHierarchyJump(false);
				yield true;
			}
			case KeyEvent.VK_DOWN -> {
				executeNameCellHierarchyJump(true);
				yield true;
			}
			default -> false;
		};
	}

	private void invokeBoundAction(Object actionKey, ActionEvent event) {
		if (actionKey == null)
			return;
		var action = getActionMap().get(actionKey);
		if (action != null)
			action.actionPerformed(new ActionEvent(this, event.getID(), String.valueOf(actionKey), event.getWhen(), event.getModifiers()));
	}

	public void setFieldArray(ArrayList fieldArray) {
		clearHeaderColumnSelectionState();
		((SpreadSheetColumnModel) getColumnModel()).setFieldArray(fieldArray);
		createDefaultColumnsFromModel(fieldArray);
		resizeAndRepaintHeader();

	}
	
	public void resizeAndRepaintHeader() {
		JTableHeader header = getTableHeader();
		SpreadSheetColumnModel tm = ((SpreadSheetColumnModel) getColumnModel());
		int colWidth = tm.getColWidth();// tm.getTotalColumnWidth(); //Hack,
										// colWidth isn't enough why?
		header.setPreferredSize(new Dimension(colWidth, header.getPreferredSize().height));
		header.resizeAndRepaint();
		
	}

	public void createDefaultColumnsFromModel(ArrayList fieldArray) {
			// Remove any current columns
			TableColumnModel cm = getColumnModel();
			while (cm.getColumnCount() > 0) {
				cm.removeColumn(cm.getColumn(0));
			}

			// Create new columns from the data model info
			int colCount=fieldArray.size();
			for (int i = 0; i < colCount; i++) {
				TableColumn newColumn = new TableColumn(i);
				addColumn(newColumn);
			}
			
//		TableModel m = getModel();
//		if (m != null) {
//			// Remove any current columns
//			TableColumnModel cm = getColumnModel();
//			while (cm.getColumnCount() > 0) {
//				cm.removeColumn(cm.getColumn(0));
//			}
//
//			// Create new columns from the data model info
//			for (int i = 0; i < m.getColumnCount(); i++) {
//				TableColumn newColumn = new TableColumn(i);
//				addColumn(newColumn);
//			}
//		}
	}

	private void makeCustomTableHeader(TableColumnModel columnModel) {
		JTableHeader h =new JTableHeader(columnModel) {

			public String getToolTipText(MouseEvent e) {
				if (isHasColumnHeaderPopup()) {
					int col = columnAtPoint(e.getPoint());
					Field f = ((SpreadSheetModel) getModel()).getFieldInViewColumn(col);
					if (f != null)
						return Messages.format("Format.htmlLines", f.getName(),
								Messages.getString("Text.rightClickToInsertRemoveColumns"));
				}
				return super.getToolTipText(e);
			}
			
		};
		setTableHeader(h);
		
	}
	public SpreadSheetPopupMenu getPopup(){
		if (popup == null && (hasRowPopup()
				|| SpreadSheetCategories.taskSpreadsheetCategory.equals(getSpreadSheetCategory()))) {
			popup = new SpreadSheetPopupMenu(this);
		}
		return popup;
	}
	
	
	public void setModel(SpreadSheetModel spreadSheetModel, SpreadSheetColumnModel spreadSheetColumnModel) {
		if (spreadSheetModel == null)
			return;
		makeCustomTableHeader(spreadSheetColumnModel);
		var oldModel = getModel();
		setModel(spreadSheetModel);
		
		if (spreadSheetColumnModel != null) {
			//System.out.println("creating new ColModel");
			setColumnModel(spreadSheetColumnModel);
	
			selection = new SpreadSheetSelectionModel(this);
			selection.setRowSelection(new SpreadSheetListSelectionModel(selection, true));
			selection.setColumnSelection(new SpreadSheetListSelectionModel(selection, false));
			setSelectionModel(selection.getRowSelection());
			createDefaultColumnsFromModel(spreadSheetModel.getFieldArray()); //Consume memory
			getColumnModel().setSelectionModel(selection.getColumnSelection());
		}
		
		registerEditors(); //Consume memory
		installNameColumnHierarchyNavigationActions();
		installNameColumnTabActions();
		initRowHeader(spreadSheetModel);
		initModel();
		if (SpreadSheetCategories.taskSpreadsheetCategory.equals(getSpreadSheetCategory())
				&& spreadSheetColumnModel != null) {
			spreadSheetColumnModel.autoSizeColumnsToContent(this);
			resizeAndRepaintHeader();
		}
		initListeners();

		var config = GraphicConfiguration.getInstance();
		//fix for substance
		setTableHeader(createDefaultTableHeader());
		JTableHeader header = getTableHeader();
		header.setPreferredSize(new Dimension((int) header.getPreferredSize().getWidth(), config.getColumnHeaderHeight()));
		header.addMouseListener(new HeaderMouseListener(this));

		installTableMouseHandler();

		if (oldModel != spreadSheetModel && oldModel instanceof CommonSpreadSheetModel commonModel) {
			NodeModelCache currentCache = commonModel.getCache();
			if (currentCache != null) {
				currentCache.removeNodeModelListener(this);
			}
		}
		if (spreadSheetModel.getCache() != null) {
			spreadSheetModel.getCache().addNodeModelListener(this);
		}

	}

	private void installTableMouseHandler() {
		if (tableMouseHandlerInstalled)
			return;
		tableMouseHandlerInstalled = true;
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				beginCellRangeSelection(e);
				handleTableMousePressed(e);
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				extendCellRangeSelection(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				endCellRangeSelection();
				handleTablePopupTrigger(e);
			}
		});
	}

	void handleTableMousePressed(MouseEvent e) {
		var p = e.getPoint();
		var row = rowAtPoint(p);
		var col = columnAtPoint(p);
		var popup = getPopup();
		if (row < 0 || col < 0) {
			return;
		}
		if (SwingUtilities.isLeftMouseButton(e)) {
			selectCellFromClick(row, col, e);
			if (!(getModel() instanceof SpreadSheetModel model))
				return;
			var field = model.getFieldInViewColumn(col);
			if (field != null && field.isNameField()) {
				// if (col == columnModel.getNameIndex()) {
				var node = model.getNode(row);
				if (node != null && isOnIcon(e)) {
					if (model.getCellProperties(node).isCompositeIcon()) {
						finishCurrentOperations();
						selection.getRowSelection().clearSelection();
						boolean change = true;
						if (!node.isFetched()) // for subprojects
							change = node.fetch();
						if (change)
							model.changeCollapsedState(row);
						e.consume(); // prevent dbl click treatment below

						// because editor may have already been
						// installed we
						// have to update its collapsed state
						// updateNameCellEditor(node);

						// editCellAt(row,model.findGraphicNodeRow(node));
					}
				}
			} else if (field != null && field.isHyperlink()) {
				int modelColumn = model.getModelColumnForViewColumn(col);
				Hyperlink link = modelColumn < 0 ? null : (Hyperlink) model.getValueAt(row, modelColumn);
				if (link != null) {
					BrowserControl.displayURL(link.getAddress());
					e.consume(); // prevent dbl click treatment below
				}
			}
			if (!e.isConsumed()) {
				if (e.getClickCount() == 2) {
					finishCurrentOperations();
					doDoubleClick(row, col);
					e.consume();
				} else {
					doClick(row,col);
				}
			}
		} else if (popup != null && e.isPopupTrigger()) {
			showPopupForCell(row, col, this, e);
		}
	}

	private void handleTablePopupTrigger(MouseEvent e) {
		if (!e.isPopupTrigger()) {
			return;
		}
		int row = rowAtPoint(e.getPoint());
		int col = columnAtPoint(e.getPoint());
		if (row >= 0 && col >= 0) {
			showPopupForCell(row, col, this, e);
		}
	}

	/**
	 * Shows the task popup after synchronizing table selection and popup context.
	 * Row headers delegate here so both hit areas keep identical popup behavior.
	 */
	public void showPopupForCell(int row, int col, Component invoker, MouseEvent e) {
		if (row < 0 || col < 0 || invoker == null || e == null)
			return;
		SpreadSheetPopupMenu popup = getPopup();
		if (popup == null)
			return;
			if (!isCellSelected(row, col)) {
				selectCellFromClick(row, col, null);
			}
			popup.setRow(row);
			popup.setCol(col);
			showPopupMenu(popup, invoker, e);
	}

	protected void showPopupMenu(SpreadSheetPopupMenu popup, MouseEvent e) {
		showPopupMenu(popup, this, e);
	}

	protected void showPopupMenu(SpreadSheetPopupMenu popup, Component invoker, MouseEvent e) {
		popup.show(invoker, e.getX(), e.getY());
	}

	/**
	 * A header click selects every row in one visible column.  Backspace must
	 * therefore remove that visible column instead of clearing JTable's lead
	 * cell.  The view change is recorded in the document undo history so the
	 * standard Ctrl+Z/Ctrl+Y commands restore it.
	 */
	@Override
	protected boolean handleClearCellKey(KeyEvent e) {
		int column = getSelectedColumn();
		if (!isColumnFullySelected(column)) {
			return false;
		}
		removeSelectedColumn(column);
		return true;
	}

	boolean removeSelectedColumn(int viewColumn) {
		if (!isCanModifyColumns()) {
			return false;
		}
		if (!(getFieldArray() instanceof SpreadSheetFieldArray fields) || fields.size() <= 2) {
			return false; // retain the hidden ID field and at least one visible field
		}
		int fieldIndex = viewColumn + 1; // field array includes the hidden ID field
		if (fieldIndex <= 0 || fieldIndex >= fields.size()) {
			return false;
		}

		SpreadSheetFieldArray before = (SpreadSheetFieldArray) fields.clone();
		SpreadSheetFieldArray after = fields.removeField(fieldIndex);
		applyColumnLayoutChange(before, after, viewColumn, Messages.getString("SpreadSheetColumnMenu.HideColumn"));
		return true;
	}

	private int rangeAnchorRow = -1;
	private int rangeAnchorColumn = -1;
	private boolean selectingCellRange;

	void beginCellRangeSelection(MouseEvent e) {
		if (!SwingUtilities.isLeftMouseButton(e))
			return;
		Point point = e.getPoint();
		rangeAnchorRow = rowAtPoint(point);
		rangeAnchorColumn = columnAtPoint(point);
		selectingCellRange = false;
	}

	void extendCellRangeSelection(MouseEvent e) {
		if (rangeAnchorRow < 0 || rangeAnchorColumn < 0
				|| (e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0) {
			return;
		}
		Point point = e.getPoint();
		int row = rowAtPoint(point);
		int column = columnAtPoint(point);
		if (row < 0 || column < 0
				|| (row == rangeAnchorRow && column == rangeAnchorColumn)) {
			return;
		}
		// A simple click keeps the Microsoft Project whole-row selection. Once
		// the pointer moves, restore the original cell as the anchor so JTable
		// can form a rectangular cell range for clipboard operations.
		if (!selectingCellRange) {
			changeSelection(rangeAnchorRow, rangeAnchorColumn, false, false);
			selectingCellRange = true;
		}
		changeSelection(row, column, false, true);
		scrollRectToVisible(getCellRect(row, column, true));
	}

	void endCellRangeSelection() {
		rangeAnchorRow = -1;
		rangeAnchorColumn = -1;
		selectingCellRange = false;
	}

	/** Inserts an available field at a field-array index (which includes the hidden ID field). */
	boolean insertColumn(int fieldIndex, Field field) {
		if (!isCanModifyColumns() || field == null || !(getFieldArray() instanceof SpreadSheetFieldArray fields)) {
			return false;
		}
		for (Field current : fields) {
			if (field.getId().equals(current.getId())) {
				return false;
			}
		}
		int insertionIndex = Math.max(1, Math.min(fieldIndex, fields.size()));
		SpreadSheetFieldArray before = (SpreadSheetFieldArray) fields.clone();
		SpreadSheetFieldArray after = fields.insertField(insertionIndex, field);
		applyColumnLayoutChange(before, after, insertionIndex - 1,
				Messages.getString("SpreadSheetColumnMenu.InsertColumn"));
		return true;
	}

	private void applyColumnLayoutChange(SpreadSheetFieldArray before, SpreadSheetFieldArray after,
			int preferredColumn, String presentationName) {
		Project project = getLayoutProject();
		if (project != null)
			project.setFieldArray(after);
		setFieldArray(after);
		selectColumnAfterColumnChange(Math.min(preferredColumn, getColumnCount() - 1));

		var cache = getCache();
		var undoController = cache == null || cache.getModel() == null ? null : cache.getModel().getUndoController();
		if (undoController != null) {
			undoController.getEditSupport().postEdit(new ColumnLayoutEdit(
					cache.getModel(), this, getSpreadSheetCategory(), project, before, after, preferredColumn, presentationName));
		}
	}

	private void selectColumnAfterColumnChange(int column) {
		if (column >= 0 && getColumnCount() > 0) {
			selectColumnAndAllRows(column);
		}
	}

	private Project getLayoutProject() {
		var cache = getCache();
		return cache == null ? null : projectLayoutOwner(cache.getModel(), getSpreadSheetCategory());
	}

	static Project projectLayoutOwner(NodeModel model, String category) {
		if (!SpreadSheetCategories.taskSpreadsheetCategory.equals(category)
				|| model == null
				|| !(model.getDataFactory() instanceof Project project))
			return null;
		return project;
	}

	private static final class ColumnLayoutEdit extends AbstractUndoableEdit {
		private static final long serialVersionUID = 1L;
		private final WeakReference<NodeModel> model;
		private final WeakReference<SpreadSheet> source;
		private final String category;
		private final WeakReference<Project> project;
		private final SpreadSheetFieldArray before;
		private final SpreadSheetFieldArray after;
		private final int selectedColumn;
		private final String presentationName;

		private ColumnLayoutEdit(NodeModel model, SpreadSheet source, String category, Project project,
				SpreadSheetFieldArray before, SpreadSheetFieldArray after, int selectedColumn, String presentationName) {
			this.model = new WeakReference<>(model);
			this.source = new WeakReference<>(source);
			this.category = category;
			this.project = new WeakReference<>(project);
			this.before = (SpreadSheetFieldArray) before.clone();
			this.after = (SpreadSheetFieldArray) after.clone();
			this.selectedColumn = selectedColumn;
			this.presentationName = presentationName;
		}

		@Override
		public void undo() throws CannotUndoException {
			super.undo();
			apply(before, selectedColumn);
		}

		@Override
		public void redo() throws CannotRedoException {
			super.redo();
			apply(after, selectedColumn);
		}

		private void apply(SpreadSheetFieldArray fields, int preferredColumn) {
			SpreadSheetFieldArray restoredFields = (SpreadSheetFieldArray) fields.clone();
			Project currentProject = project.get();
			if (currentProject != null)
				currentProject.setFieldArray(restoredFields);
			SpreadSheet target = findLayoutTarget(model.get(), category, source.get());
			if (target == null)
				return;
			target.setFieldArray(restoredFields);
			target.selectColumnAfterColumnChange(Math.min(preferredColumn, target.getColumnCount() - 1));
		}

		@Override
		public String getPresentationName() {
			return presentationName;
		}
	}

	private void selectCellFromClick(int row, int col, MouseEvent e) {
		if (row < 0 || col < 0)
			return;
		finishCurrentOperations();
		requestFocusInWindow();
		boolean extend = e != null && e.isShiftDown();
		boolean toggle = e != null && (e.isControlDown() || e.isMetaDown());
		if (!toggle && !extend) {
			// Microsoft Project selects the complete task row when any task-table
			// cell is clicked. The selection model retains the clicked cell so the
			// active-cell border stays on the actual field rather than column zero.
			selectRowAndAllColumns(row);
			// A task-table click is not a row-header click.  Keeping this state
			// separate prevents the ID column from being rendered as the selected
			// target and preserves cell clipboard semantics.
			setRowHeaderSelectionActive(false);
			getSelection().setActiveCell(row, col);
		} else {
			changeSelection(row, col, toggle, extend);
		}
		scrollRectToVisible(getCellRect(row, col, true));
	}
	
//    public void columnSelectionChanged(ListSelectionEvent e) {
//		System.out.println("JTable: "+((e.getValueIsAdjusting())?"lse=":"LSE=")+e.getFirstIndex()+", "+e.getLastIndex());
//    	super.columnSelectionChanged(e);
//    }
    

	public void doDoubleClick(int row, int col) {
		GraphicManager.getInstance(this).doInformationDialog(false);
	}
	public void doClick(int row, int col) {
		// override to treat cell clicks
	}
	/*
	 * public SpreadSheetPopupMenu getPopup() { return popup; } public void
	 * setPopup(SpreadSheetPopupMenu popup) { this.popup = popup; }
	 */

	public boolean isOnIcon(MouseEvent e) {
		var p = e.getPoint();
		var row = rowAtPoint(p);
		var col = columnAtPoint(p);
		var bounds = getCellRect(row, col, false);
		var model = (SpreadSheetModel) getModel();
		var node = model.getNode(row);
		return NameCellComponent.isOnIcon(new Point((int) (p.getX() - bounds.getX()), (int) (p.getY() - bounds.getY())), bounds.getSize(), model
				.getCache().getLevel(node));
	}

	public boolean isOnText(MouseEvent e) {
		var p = e.getPoint();
		var row = rowAtPoint(p);
		var col = columnAtPoint(p);
		var bounds = getCellRect(row, col, false);
		var model = (SpreadSheetModel) getModel();
		var node = model.getNode(row);
		return NameCellComponent.isOnText(new Point((int) (p.getX() - bounds.getX()), (int) (p.getY() - bounds.getY())), bounds.getSize(), model
				.getCache().getLevel(node));
	}

	public void updateNameCellEditor(GraphicNode node) {
		// if (isEditing() && getEditingColumn() == columnModel.getNameIndex()
		// && editorComp != null) {

		if (isEditing() && editorComp != null && ((SpreadSheetModel) getModel()).getFieldInViewColumn(getEditingColumn()).isNameField()) {
			var nameCellComponent = (NameCellComponent) editorComp;
			var model = (SpreadSheetModel) getModel();
			// GraphicNode node = model.getNode(row);
			if (model.getCellProperties(node).isCompositeIcon())
				nameCellComponent.setCollapsed(node.isCollapsed());
		}
	}

	protected void initListeners() {

	}

	// Actions on selected nodes
	public List<GraphicNode> getSelectedGraphicNodes() {
		return rowsToGraphicNodes(getSelectedRows());
	}

	public List<GraphicNode> rowsToGraphicNodes(int[] rows) {
		if (rows == null || rows.length == 0)
			return new LinkedList<>();
		NodeModelCache cache = ((SpreadSheetModel) getModel()).getCache();
		List<Object> elements = cache.getElementsAt(rows);
		List<GraphicNode> nodes = new LinkedList<>();
		for (Object element : elements) {
			nodes.add((GraphicNode) element);
		}
		return nodes;
	}
	
	
	
	
	


	// gui actions
	public void executeAction(String actionId) {
		performAction(actionId, null);
	}

	/** Executes a configured spreadsheet action without exposing nullable lookup results to callers. */
	public boolean performAction(String actionId, ActionEvent event) {
		var action = prepareAction(actionId);
		if (action == null) {
			logger.log(Level.FINE, "No action for {0}", actionId);
			return false;
		}
		action.actionPerformed(event == null
				? new ActionEvent(this, ActionEvent.ACTION_PERFORMED, actionId)
				: event);
		return true;
	}

	// init actions
	public CommonSpreadSheetAction prepareAction(String actionId) {
		var action = getAction(actionId);
		if (action == null) {
			return null;
		}
		action.setSpreadSheet(this);
		return action;
	}
	public String[] getActionList(){
		if (actionList == null) {
			var spreadSheetModel = getSpreadSheetModel();
			actionList = spreadSheetModel == null ? new String[0] : spreadSheetModel.getActionList();
		}
		return actionList;
	}

	private SpreadSheetModel getSpreadSheetModel() {
		var model = getModel();
		return model instanceof SpreadSheetModel spreadSheetModel ? spreadSheetModel : null;
	}

	private void removeExistingResources(ResourcePool resourcePool, HashMap<Long, EnterpriseResourceData> resourceMap) {
		List<com.microproject.pm.resource.Resource> currentResources = resourcePool.getResourceList();
		for (com.microproject.pm.resource.Resource resource : currentResources){
			Long key = Long.valueOf(resource.getUniqueId());
			if (resourceMap.containsKey(key)) {
				resourceMap.remove(key);
			}
		}
	}

	public CommonSpreadSheetAction getAction(String actionId) {
		if (actionId == null) {
			return null;
		}
		if (actionMap == null) {
			actionMap = new HashMap<>();
			addActions(getActionList());
		}
		return actionMap.get(actionId);
	}
	private void addAction(String action,String spreadSheetActionId,CommonSpreadSheetAction spreadSheetAction){
		if (spreadSheetActionId.equals(action)){
			actionMap.put(spreadSheetActionId,spreadSheetAction);
		}
	}
	private void addActions(String[] actions){
//		System.out.println("SpreadSheet "+spreadSheetCategory+", "+hashCode()+" addActions("+dumpActions(actions)+")");
		var handler = getTransferHandler() instanceof NodeListTransferHandler transferHandler ? transferHandler : null;
		if (actions != null) {
			for (int i = 0; i < actions.length; i++) {
				String action = actions[i];
				addAction(action,MenuActionConstants.ACTION_INDENT,indentAction);
				addAction(action,MenuActionConstants.ACTION_OUTDENT,outdentAction);
				addAction(action,MenuActionConstants.ACTION_NEW,newAction);
				addAction(action,MenuActionConstants.ACTION_DELETE,deleteAction);
				if (handler!=null){
					addAction(action,MenuActionConstants.ACTION_COPY,handler.getNodeListCopyAction());
					addAction(action,MenuActionConstants.ACTION_CUT,handler.getNodeListCutAction());
					addAction(action,MenuActionConstants.ACTION_PASTE,clipboardPasteAction);
					if (MenuActionConstants.ACTION_PASTE.equals(action)) {
						actionMap.put(MenuActionConstants.ACTION_PASTE_INSERT, pasteInsertAction);
					}
				}
				addAction(action,MenuActionConstants.ACTION_EXPAND,expandAction);
				addAction(action,MenuActionConstants.ACTION_COLLAPSE,collapseAction);
			}
		}
	}
	public void clearActions(){
		actionMap=null;
		actionList=null;
		popup=null;
		((CommonSpreadSheetModel)getModel()).clearActions();
	}
//	private static String dumpActions(String[] actions){
//		if (actions==null) return null;
//		StringBuffer sb=new StringBuffer();
//		for (int i=0;i<actions.length;i++){
//			sb.append(actions[i]).append(',');
//		}
//		return sb.toString();
//	}
	public void setActions(String[] actions){
		//replace default actions
		actionList=actions;
		if (actionMap==null) actionMap=new HashMap<>();
		else actionMap.clear();
		addActions(actions);
	}
	public void setActions(String actions){
		addActions(CommonSpreadSheetModel.convertActions(actions));
	}

	
//	public static final String INDENT = "Action.Indent";
//	public static final String OUTDENT = "Action.Outdent";
//	public static final String NEW = "Action.New";
//	public static final String DELETE = "Action.Delete";
//	public static final String CUT = "Action.Cut";
//	public static final String COPY = "Action.Copy";
//	public static final String PASTE = "Action.Paste";
	
//	public static final int INDENT = 0;
//
//	public static final int OUTDENT = 1;
//
//	public static final int NEW = 2;
//
//	public static final int DELETE = 3;
//
//	public static final int CUT = 4;
//
//
//	public static final int COPY = 5;
//
//	public static final int PASTE = 6;

	public static abstract class SpreadSheetAction extends AbstractAction implements CommonSpreadSheetAction {
		protected SpreadSheet spreadSheet;

		protected int[] rows;

		public SpreadSheetAction(String id,SpreadSheet spreadSheet) {
			super(Messages.getString(id));
			this.spreadSheet=spreadSheet;
		}

		public void actionPerformed(ActionEvent e) {
			execute();
		}

		public void executeFirst() {
			rows = spreadSheet.finishCurrentOperations();
		}

		public void execute(Object o) {
			executeFirst();
			execute();
		}

		public abstract void execute();

		public CommonSpreadSheet getSpreadSheet() {
			return spreadSheet;
		}

		public void setSpreadSheet(CommonSpreadSheet spreadSheet) {
			this.spreadSheet = (SpreadSheet)spreadSheet;
		}

		public NodeModelCache getCache() {
			return ((SpreadSheetModel) spreadSheet.getModel()).getCache();
		}

		public List<GraphicNode> getSelected() {
			return spreadSheet.rowsToGraphicNodes((rows == null) ? spreadSheet.getSelectedRows() : rows);
		}
	}


	protected SpreadSheetAction indentAction = new SpreadSheetAction("Spreadsheet.Action.indent",this) {
		public void execute() {
			finishCurrentOperations();
			getCache().indentNodes(getSelected());
		}
	};

	protected SpreadSheetAction outdentAction = new SpreadSheetAction("Spreadsheet.Action.outdent",this) {
		public void execute() {
			finishCurrentOperations();
			getCache().outdentNodes(getSelected());
		}
	};

	protected SpreadSheetAction newAction=new SpreadSheetAction("Spreadsheet.Action.new",this){
		public void execute(){
			finishCurrentOperations();
			List<GraphicNode> nodes = getSelected();
			if (nodes == null || nodes.isEmpty()) {
				int row = getCurrentRow();
				if (row == -1)
					return;
				getCache().newNode((GraphicNode) getCache().getElementAt(row));
			} else {
				getCache().newNode(nodes);
			}
		}
	};
	
	//will be used later
	protected SpreadSheetAction newResourceAction=new SpreadSheetAction("Spreadsheet.Action.new",this){
		public void execute(){
			List<GraphicNode> selectedNodes = getSelected();
			ResourcePool resourcePool = (ResourcePool) getCache().getModel().getDataFactory();
			Project project = (Project) resourcePool.getProjects().get(0);
			if (selectedNodes == null || selectedNodes.isEmpty()) return;
			final ArrayList descriptors = new ArrayList();
			Session session = SessionFactory.getInstance().getSession(false);
			Job job = (Job) SessionFactory.callNoEx(session, "getLoadProjectDescriptorsJob", new Class[]{boolean.class, java.util.List.class, boolean.class}, new Object[]{true, descriptors, true});
			job.addSwingRunnable(new JobRunnable("Local: addNodes"){
				public Object run() throws Exception{
					final Consumer<Object> setter = new Consumer<Object>() { public void accept(Object obj) {
						}
					};
					final Consumer<Object> getter = new Consumer<Object>() { public void accept(Object obj) {
							ResourceAdditionDialog.Form form = (ResourceAdditionDialog.Form) obj;
							List<Node> nodes = new ArrayList<>();
							for (Object selectedResource : form.getSelectedResources()){
								try {
									nodes.add(NodeFactory.getInstance().createNode(Serializer.deserializeResourceAndAddToPool((EnterpriseResourceData) selectedResource, resourcePool, null)));
								} catch (IOException e) {
									logger.log(Level.WARNING, "Failed to deserialize enterprise resource", e);
								} catch (ClassNotFoundException e) {
									logger.log(Level.WARNING, "Failed to deserialize enterprise resource class", e);
								}
							}
							getCache().addNodes(selectedNodes.get(0).getNode(), nodes);
							getCache().update();
						}
					};
					ResourceAdditionDialog.Form form = new ResourceAdditionDialog.Form();
					try{
						List<EnterpriseResourceData> resources = (List<EnterpriseResourceData>) SessionFactory.call(SessionFactory.getInstance().getSession(false), "retrieveResourceDescriptors", null, null);
						HashMap<Long, EnterpriseResourceData> resourceMap = new HashMap<>();
						for (EnterpriseResourceData data : resources){
							resourceMap.put(Long.valueOf(data.getUniqueId()), data);
						}
						removeExistingResources(resourcePool, resourceMap);
						form.getSelectedResources().addAll(resourceMap.values());
					}catch(Exception e){
						logger.log(java.util.logging.Level.WARNING, "Failed to process resource selection", e);
					}
					
					ResourceAdditionDialog.getInstance((JFrame)SwingUtilities.getRoot(SpreadSheet.this),form).execute(setter,getter);
					return null;
				}
			});
			session.schedule(job);
		}
	};

	protected SpreadSheetAction deleteAction = new SpreadSheetAction("Spreadsheet.Action.delete",this) {
		private static final long serialVersionUID = 1561847977122331970L;

		public void execute() {
			finishCurrentOperations();
			List l = getSelectedDeletableRows();
			if (l.isEmpty())
				return;
			if (!CollaborationHelper.tryLockNodes(null, l, SpreadSheet.this, "delete"))
				return;
			if (!GeneralOption.getInstance().isConfirmDeletes() || Alert.okCancel(Messages.getString("Message.confirmDeleteRows"))) {
				getCache().deleteNodes(l);
			}
		}
	};

	protected SpreadSheetAction cutAction = new SpreadSheetAction("Spreadsheet.Action.cut",this) {
		private static final long serialVersionUID = -7928292866527615772L;

		public void execute() {
			finishCurrentOperations();
			accept(getSelectedRows());
		}

		public void execute(Object object) {
			if (object instanceof List<?> selectedRows) {
				finishCurrentOperations();
				List<Node> nodes = getSelectedCuttableRows((List<Node>) selectedRows);
				if (nodes.isEmpty())
					return;
				if (!CollaborationHelper.tryLockNodes(null, nodes, SpreadSheet.this, "cut"))
					return;
				executeFirst();
				getCache().cutNodes(nodes);
			}
		}
	};

	protected SpreadSheetAction copyAction = new SpreadSheetAction("Spreadsheet.Action.copy",this) {
		/**
		 * 
		 */
		private static final long serialVersionUID = -7593036949653490043L;

		public void execute() {
			accept(getSelectedNodes());
		}

		public void execute(Object object) {
			if (object instanceof List<?> selectedNodes) {
				finishCurrentOperations();
				executeFirst();
				getCache().copyNodes(selectedNodes);
			}
		}
	};

	protected SpreadSheetAction pasteAction = new SpreadSheetAction("Spreadsheet.Action.paste",this) {
		private static final long serialVersionUID = 5904764895696983803L;

		public void execute() {
			accept(getSelectedNodes());
		}

		public void execute(Object object) {
			if (object instanceof List<?> pastedNodes) {
				finishCurrentOperations();
				List selectedNodes = getSelectedNodes();
				if (!CollaborationHelper.tryLockNodes(null, selectedNodes, SpreadSheet.this, "paste"))
					return;
				Node parent = null;
				int position = 0;
				if (selectedNodes.size() > 0) {
					Node node = (Node) selectedNodes.get(0);
					parent = (Node) node.getParent();
					position = ((NodeBridge) parent).getIndex(node);
				}
				executeFirst();
				spreadSheet.clearSelection();
				getCache().pasteNodes(parent, pastedNodes, position);
//				if (nodes.size() > 0) {
//					int row = ((SpreadSheetModel) spreadSheet.getModel()).findGraphicNodeRow(spreadSheet.getCache().getGraphicNode(nodes.get(0)));
//					changeSelection(row, 0, false, false);
//					if (nodes.size() > 1)
//						changeSelection(row + nodes.size() - 1, getColumnCount(), false, true);
//				}
			}
		}
	};
	protected SpreadSheetAction clipboardPasteAction = new SpreadSheetAction("Spreadsheet.Action.paste",this) {
		private static final long serialVersionUID = 1L;

		public void execute() {
			pasteClipboardContents();
		}
	};
	protected SpreadSheetAction pasteInsertAction = new SpreadSheetAction("Spreadsheet.Action.pasteInsert",this) {
		private static final long serialVersionUID = 1L;

		public void execute() {
			pasteClipboardInsertedContents();
		}
	};
	
	protected SpreadSheetAction expandAction = new SpreadSheetAction("Spreadsheet.Action.expand",this) {
		public void execute() {
			finishCurrentOperations();
			getCache().expandNodes(getSelected(),true);
		}
	};
	protected SpreadSheetAction collapseAction = new SpreadSheetAction("Spreadsheet.Action.collapse",this) {
		public void execute() {
			finishCurrentOperations();
			getCache().expandNodes(getSelected(),false);
		}
	};
	
	
	public boolean isReadOnly() {
		return ((SpreadSheetModel)getModel()).isReadOnly();
	}

	public void setReadOnly(boolean readOnly) {
		((SpreadSheetModel)getModel()).setReadOnly(readOnly);
	}


//	private static final int[] DEFAULT_POPUP_OPTIONS = new int[] {INDENT,OUTDENT,NEW,DELETE,CUT,COPY,PASTE};
//	private int[] popupActions = DEFAULT_POPUP_OPTIONS;
//	public final void setPopupActions(int[] popupActions) {
//		this.popupActions = popupActions;
//	}
	

//	public boolean supportsAction(int option) {
//		if (popupActions == null)
//			return false;
//		for (int i = 0; i<popupActions.length; i++) {
//			if (popupActions[i] == option)
//				return true;
//		}
//		return false;
//	}
//	public boolean hasRowPopup() {
//		return popupActions != null && popupActions.length > 0;
//	}
	public boolean hasRowPopup() {
		getAction(null);
		return actionMap != null && actionMap.size() > 0;
	}

	public SpreadSheetAction getCopyAction() {
		return copyAction;
	}

	public SpreadSheetAction getCutAction() {
		return cutAction;
	}

	public SpreadSheetAction getPasteAction() {
		return pasteAction;
	}

}


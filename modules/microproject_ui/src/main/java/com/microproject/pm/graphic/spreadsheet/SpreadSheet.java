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
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
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
import com.microproject.ui.input.PopupTriggerController;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.ProjectionRowKey;
import com.microproject.pm.graphic.collaboration.CollaborationHelper;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetAction;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.common.transfer.NodeListTransferHandler;
import com.microproject.pm.graphic.spreadsheet.common.transfer.NodeListTransferable;
import com.microproject.pm.graphic.spreadsheet.command.TaskCommandGateway;
import com.microproject.pm.graphic.spreadsheet.command.TaskCommandResult;
import com.microproject.pm.graphic.spreadsheet.command.TaskHierarchyEditIntent;
import com.microproject.pm.graphic.spreadsheet.command.TaskPasteIntent;
import com.microproject.pm.graphic.spreadsheet.editor.SimpleComboBoxEditor;
import com.microproject.pm.graphic.spreadsheet.renderer.NameCellComponent;
import com.microproject.pm.graphic.spreadsheet.selection.SpreadSheetListSelectionModel;
import com.microproject.pm.graphic.spreadsheet.selection.SpreadSheetSelectionModel;
import com.microproject.pm.graphic.spreadsheet.selection.event.HeaderMouseListener;
import com.microproject.datatype.Hyperlink;
import com.microproject.field.Field;
import com.microproject.field.FieldContext;
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
import com.microproject.pm.task.Task;
import com.microproject.server.data.EnterpriseResourceData;
import com.microproject.server.data.ProjectData;
import com.microproject.server.data.Serializer;
import com.microproject.session.Session;
import com.microproject.session.SessionFactory;
import com.microproject.strings.Messages;
import com.microproject.undo.UndoController;
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
	private static final String CLIPBOARD_PASTE_VALUES_ACTION = "spreadsheet.clipboardPasteValues";
	protected SpreadSheetPopupMenu popup=null;
	private boolean hierarchyActionInProgress;
	private boolean tableMouseHandlerInstalled;
	private final PopupTriggerController popupTriggerController = new PopupTriggerController();
	private transient IntConsumer headerClickHandler;

	public void setHeaderClickHandler(IntConsumer handler) {
		headerClickHandler = handler;
	}

	public boolean handleHeaderClick(int viewColumn) {
		if (headerClickHandler == null) return false;
		headerClickHandler.accept(viewColumn);
		return true;
	}
	private String[] actionList = null;
	private Map<String, CommonSpreadSheetAction> actionMap = null;


	public SpreadSheet() {
		super();
		NodeListTransferHandler.registerWith(this);
		// Issue #47: prevent the TransferHandler from also binding ctrl C/X/V on the
		// component's WHEN_FOCUSED input map. Those keys are wired globally to
		// ACTION_COPY/CUT/PASTE on the root-pane WHEN_IN_FOCUSED_WINDOW by
		// GraphicManager.applyMicrosoftShortcuts. Leaving both layers bound makes
		// ctrl+V non-deterministic ("paste doesn't work well"). The real paste logic
		// (NodeListTransferHandler.importData) stays reachable via ACTION_PASTE, so we
		// only remove the duplicate keyboard bindings here.
		removeDuplicateClipboardKeyBindings(this);
		installClipboardPasteBindings();
	}

	private static void removeDuplicateClipboardKeyBindings(JComponent c) {
		InputMap focused = c.getInputMap(JComponent.WHEN_FOCUSED);
		focused.remove(KeyStroke.getKeyStroke("ctrl V"));
		focused.remove(KeyStroke.getKeyStroke("ctrl C"));
		focused.remove(KeyStroke.getKeyStroke("ctrl X"));
	}

	/**
	 * Canonical task-row move command shared by the Alt+Shift+Up/Down shortcut
	 * and the ribbon/menu route.  Keeping this as the one entry point ensures
	 * that both routes apply the same whole-row precondition, collaboration
	 * lock, undoable relocation, selection restore, and view refresh.
	 */
	public boolean moveSelectedTaskRows(int direction) {
		return moveSelectedTaskRowsInternal(direction);
	}

	public boolean canMoveSelectedTaskRows(int direction, boolean requireEntireRow) {
		// Project Desktop documents Alt+Shift+Up/Down as moving the selected task
		// only when its entire row is selected.  All task move entry points share
		// this precondition so shortcut, ribbon/menu, and drag routes cannot diverge.
		if (!hasEntireRowSelection())
			return false;
		return hasOnlyTaskRowsSelected() && getCache().canMoveNodes(getSelectedGraphicNodes(), direction);
	}

	private boolean moveSelectedTaskRowsInternal(int direction) {
		finishCurrentOperations();
		if (!canMoveSelectedTaskRows(direction, true))
			return false;
		List<Node> nodes = new ArrayList<Node>(getSelectedNodes());
		TaskHierarchyEditIntent intent = captureMoveIntent(direction);
		if (nodes.isEmpty() || intent == null)
			return false;
		boolean moved = TaskCommandGateway.execute(this, intent).status() == TaskCommandResult.Status.CHANGED;
		if (moved) {
			refreshTaskMoveViews();
			restoreTaskRowSelection(nodes);
		}
		return moved;
	}

	public TaskHierarchyEditIntent.SelectionSnapshot captureHierarchySelection() {
		return captureTaskSelection(true);
	}

	/** Captures selected task rows for dependency commands without retaining mutable node references. */
	public TaskHierarchyEditIntent.SelectionSnapshot captureTaskDependencySelection() {
		return captureTaskSelection(false);
	}

	private TaskHierarchyEditIntent.SelectionSnapshot captureTaskSelection(boolean requireEntireRows) {
		if (!(getModel() instanceof SpreadSheetModel model) || model.getCache() == null || model.getRowMultiple() != 1
				|| getSelectedRowCount() == 0 || (requireEntireRows && !hasEntireRowSelection())
				|| !hasOnlyTaskRowsSelected())
			return null;
		var projection = model.getCache().getVisibleNodes().getProjectionIndex();
		List<ProjectionRowKey.TaskRow> taskRows = new ArrayList<>();
		for (int selectedRow : getSelectedRows()) {
			int modelRow = convertRowIndexToModel(selectedRow);
			if (modelRow < 0 || modelRow >= projection.size()
					|| !(projection.keyAt(modelRow) instanceof ProjectionRowKey.TaskRow taskRow))
				return null;
			taskRows.add(taskRow);
		}
		return taskRows.isEmpty() ? null
			: new TaskHierarchyEditIntent.SelectionSnapshot(taskRows, projection.topologyRevision());
	}

	public TaskHierarchyEditIntent createRelocationIntent(TaskHierarchyEditIntent.SelectionSnapshot selection,
			int targetRow, boolean after) {
		if (selection == null || !(getModel() instanceof SpreadSheetModel model))
			return null;
		var projection = model.getCache().getVisibleNodes().getProjectionIndex();
		int modelRow = convertRowIndexToModel(targetRow);
		if (modelRow < 0 || modelRow >= projection.size()
				|| !(projection.keyAt(modelRow) instanceof ProjectionRowKey.TaskRow anchor))
			return null;
		return new TaskHierarchyEditIntent(TaskHierarchyEditIntent.Operation.RELOCATE,
			selection.tasks(), selection.projectionRevision(), anchor, after, 0);
	}

	public boolean canMoveTaskRowsTo(TaskHierarchyEditIntent intent) {
		return intent != null && TaskCommandGateway.canExecute(this, intent);
	}

	public boolean moveTaskRowsTo(TaskHierarchyEditIntent intent) {
		finishCurrentOperations();
		if (intent == null)
			return false;
		boolean moved = TaskCommandGateway.execute(this, intent).status() == TaskCommandResult.Status.CHANGED;
		if (moved) {
			refreshTaskMoveViews();
			restoreTaskRowSelectionByKeys(intent.tasks());
		}
		return moved;
	}

	private TaskHierarchyEditIntent captureMoveIntent(int direction) {
		TaskHierarchyEditIntent.SelectionSnapshot selection = captureHierarchySelection();
		if (selection == null)
			return null;
		return new TaskHierarchyEditIntent(TaskHierarchyEditIntent.Operation.MOVE, selection.tasks(),
			selection.projectionRevision(), null, false, direction);
	}

	private void restoreTaskRowSelectionByKeys(List<ProjectionRowKey.TaskRow> taskRows) {
		if (!(getModel() instanceof SpreadSheetModel model) || taskRows == null || taskRows.isEmpty())
			return;
		var projection = model.getCache().getVisibleNodes().getProjectionIndex();
		List<Integer> rows = new ArrayList<>(taskRows.size());
		for (ProjectionRowKey.TaskRow key : taskRows) {
			int row = projection.rowForKey(key);
			if (row >= 0)
				rows.add(row);
		}
		if (rows.isEmpty())
			return;
		clearSelection();
		setRowHeaderSelectionActive(true);
		getSelectionModel().setSelectionInterval(rows.getFirst(), rows.getFirst());
		for (int i = 1; i < rows.size(); i++)
			getSelectionModel().addSelectionInterval(rows.get(i), rows.get(i));
		if (getColumnCount() > 0)
			getColumnModel().getSelectionModel().setSelectionInterval(0, getColumnCount() - 1);
		scrollRectToVisible(getCellRect(rows.getFirst(), 0, true));
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

	public void restoreTaskRowSelection(List<Node> nodes) {
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

	public List<Node> insertTaskRowsAfter(Node anchor, int count) {
		return ((CommonSpreadSheetModel) getModel()).getCache().newNodesAfter(anchor, count);
	}

	public Node insertOneTaskRowAtCurrentRow() {
		Node anchor = getCurrentRowNode();
		if (anchor == null || !(anchor.getImpl() instanceof Task)) {
			anchor = null;
			for (int row = 0; row < getRowCount(); row++) {
				Node candidate = getNodeAtRow(row);
				if (candidate != null && candidate.getImpl() instanceof Task) {
					anchor = candidate;
					break;
				}
			}
		}
		if (anchor == null)
			return null;
		finishCurrentOperations();
		Node inserted = ((CommonSpreadSheetModel) getModel()).getCache().newNodeBefore(anchor);
		if (inserted != null) restoreTaskRowSelection(List.of(inserted));
		return inserted;
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

	public boolean pasteClipboardAsValues() {
		finishCurrentOperations();
		var transferable = getClipboardContents();
		if (transferable.isEmpty()) {
			return false;
		}
		return pasteClipboardAsValues(transferable.get());
	}

	boolean pasteClipboardAsValues(Transferable transferable) {
		if (transferable == null) {
			return false;
		}
		var text = getClipboardText(transferable);
		if (text.isPresent()) {
			if (!prepareCellPaste()) {
				return false;
			}
			if (!NodeListTransferable.pasteString(text.get(), this)) {
				Alert.error(Messages.getString("Message.invalidInput"));
				return false;
			}
			return true;
		}
		if (hasNodeListFlavor(transferable)) {
			return pasteClipboardContents(transferable);
		}
		return pasteClipboardContents(transferable);
	}

	public boolean insertClipboardContents() {
		return pasteClipboardInsertedContents();
	}

	public boolean pasteClipboardContents() {
		return getClipboardContents().map(this::pasteClipboardContents).orElse(false);
	}

	public boolean pasteClipboardInsertedContents() {
		return getClipboardContents().map(this::pasteInsertedClipboardContents).orElse(false);
	}

	private boolean pasteClipboardContents(Transferable transferable) {
		if (transferable == null) {
			return false;
		}
		if (getTransferHandler() instanceof NodeListTransferHandler transferHandler) {
			if (!transferHandler.importData(this, transferable)) {
				Alert.error(Messages.getString("Message.invalidInput"));
				return false;
			}
			return true;
		}
		if (NodeListTransferHandler.getPasteAction() != null) {
			NodeListTransferHandler.getPasteAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
			return true;
		}
		return false;
	}

	private boolean pasteInsertedClipboardContents(Transferable transferable) {
		if (transferable == null) {
			return false;
		}
		if (hasNodeListFlavor(transferable)) {
			return pasteClipboardContents(transferable);
		}
		var text = getClipboardText(transferable);
		if (text.isEmpty()) {
			Alert.error(Messages.getString("Message.invalidInput"));
			return false;
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
			return false;
		}
		return true;
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
		Object factory = ((CommonSpreadSheetModel)getModel()).getCache().getModel().getDataFactory();
		boolean taskModel = factory instanceof Project;
		if (!taskModel && !isCompatibleResourcePasteBatch(pastedNodes)) {
			return false;
		}
		List<ProjectionRowKey> selectedRowKeys = captureSelectedProjectionRowKeys();
		int[] previousRows = getSelectedRows();
		boolean pasted;
		if (taskModel) {
			var projection = ((CommonSpreadSheetModel)getModel()).getCache().getVisibleNodes().getProjectionIndex();
			TaskPasteIntent intent = new TaskPasteIntent(pastedNodes, selectedRowKeys, projection.topologyRevision());
			pasted = TaskCommandGateway.execute(this, intent).status() == TaskCommandResult.Status.CHANGED;
		} else {
			List<Node> selectedNodes = getSelectedNodes();
			if (!CollaborationHelper.tryLockNodes(null, selectedNodes, this, "paste"))
				return false;
			Node parent = null;
			int position = 0;
			if (!selectedNodes.isEmpty()) {
				Node node = selectedNodes.getFirst();
				parent = (Node)node.getParent();
				if (parent != null)
					position = ((NodeBridge)parent).getIndex(node);
			}
			pasted = getCache().pasteNodes(parent, pastedNodes, position);
		}
		if (pasted) {
			clearSelection();
		} else if (!selectedRowKeys.isEmpty()) {
			restoreSelectionByProjectionRowKeys(selectedRowKeys);
		} else if (previousRows.length > 0) {
			clearSelection();
			for (int index = 0; index < previousRows.length; index++) {
				if (index == 0)
					getSelectionModel().setSelectionInterval(previousRows[index], previousRows[index]);
				else
					getSelectionModel().addSelectionInterval(previousRows[index], previousRows[index]);
			}
		}
		return pasted;
	}

	private List<ProjectionRowKey> captureSelectedProjectionRowKeys() {
		if (!(getModel() instanceof SpreadSheetModel model) || model.getCache() == null)
			return List.of();
		var projection = model.getCache().getVisibleNodes().getProjectionIndex();
		List<ProjectionRowKey> keys = new ArrayList<>();
		for (int selectedRow : getSelectedRows()) {
			int modelRow = convertRowIndexToModel(selectedRow);
			if (modelRow < 0 || modelRow >= projection.size())
				return List.of();
			keys.add(projection.keyAt(modelRow));
		}
		return List.copyOf(keys);
	}

	private void restoreSelectionByProjectionRowKeys(List<ProjectionRowKey> rowKeys) {
		if (!(getModel() instanceof SpreadSheetModel model) || model.getCache() == null)
			return;
		var projection = model.getCache().getVisibleNodes().getProjectionIndex();
		List<Integer> viewRows = new ArrayList<>(rowKeys.size());
		for (ProjectionRowKey key : rowKeys) {
			int modelRow = projection.rowForKey(key);
			if (modelRow >= 0) {
				int viewRow = convertRowIndexToView(modelRow);
				if (viewRow >= 0)
					viewRows.add(viewRow);
			}
		}
		clearSelection();
		for (int i = 0; i < viewRows.size(); i++) {
			if (i == 0)
				getSelectionModel().setSelectionInterval(viewRows.getFirst(), viewRows.getFirst());
			else
				getSelectionModel().addSelectionInterval(viewRows.get(i), viewRows.get(i));
		}
	}

	private boolean isCompatibleResourcePasteBatch(List<Node> pastedNodes) {
		if (!(getModel() instanceof CommonSpreadSheetModel spreadsheetModel)
				|| spreadsheetModel.getCache() == null) {
			return false;
		}
		Object factory = spreadsheetModel.getCache().getModel().getDataFactory();
		for (Node node : pastedNodes) {
			boolean compatible = node != null && (node.isVoid()
				|| factory instanceof ResourcePool && node.getImpl() instanceof ResourceImpl);
			if (!compatible) {
				return false;
			}
		}
		return factory instanceof ResourcePool;
	}

	public boolean prepareCellPaste() {
		if (isClipboardTargetReadOnly()) {
			return false;
		}
		finishCurrentOperations();
		return CollaborationHelper.tryLockNodes(null, getSelectedNodes(), this, "paste");
	}

	/**
	 * Canonical cell-value clear used by Microsoft Project Ctrl+Delete and cell Cut.
	 * It never creates a row, skips read-only cells, validates the whole writable range
	 * before mutation, and records the range as one Undo unit.
	 */
	public boolean clearSelectedCellValues() {
		int[] rows = finishCurrentOperations();
		return clearSelectedCellValues(rows == null ? getSelectedRows() : rows, getSelectedColumns());
	}

	public boolean clearSelectedCellValues(int[] rows, int[] columns) {
		if (rows == null || columns == null || rows.length == 0 || columns.length == 0 || isClipboardTargetReadOnly()) {
			return false;
		}
		if (!(getModel() instanceof SpreadSheetModel model)) {
			return false;
		}
		List<ClearCellTarget> targets = new ArrayList<>();
		FieldContext fieldContext = model.getFieldContext();
		for (int row : rows) {
			if (row < 0 || row >= model.getRowCount())
				continue;
			Node node = model.getNodeForDisplayRow(row);
			if (node == null || node.isVoid())
				continue;
			for (int column : columns) {
				int modelColumn = convertColumnIndexToModel(column);
				if (!model.isCellEditable(row, modelColumn))
					continue;
				Field field = model.getFieldInColumn(modelColumn);
				Object currentValue = field.getValue(node, model.getCache().getWalkersModel(), fieldContext);
				if (currentValue == null || "".equals(currentValue))
					continue;
				targets.add(new ClearCellTarget(row, modelColumn, node, field));
			}
		}
		if (targets.isEmpty() || !validateClearTargets(targets, model, fieldContext))
			return false;
		List<Node> targetNodes = targets.stream().map(ClearCellTarget::node).distinct().toList();
		if (!CollaborationHelper.tryLockNodes(null, targetNodes, this, "clearContents"))
			return false;
		UndoController undoController = model.getCache().getModel().getUndoController();
		if (undoController != null)
			undoController.beginUpdate();
		try {
			for (ClearCellTarget target : targets)
				model.setValueAt("", target.row(), target.column());
		} finally {
			if (undoController != null)
				undoController.endUpdate();
		}
		return true;
	}

	private boolean validateClearTargets(List<ClearCellTarget> targets, SpreadSheetModel model, FieldContext fieldContext) {
		boolean parseOnly = fieldContext.isParseOnly();
		fieldContext.setParseOnly(true);
		try {
			for (ClearCellTarget target : targets)
				target.field().setValue(target.node(), model.getCache().getWalkersModel(), this, "", fieldContext);
			return true;
		} catch (Exception exception) {
			logger.log(Level.FINE, "Unable to clear selected field values", exception);
			return false;
		} finally {
			fieldContext.setParseOnly(parseOnly);
		}
	}

	/** Compatibility entry point for the transfer handler's Cut completion callback. */
	public boolean cutSelectedCellValues(int[] rows, int[] columns) {
		return clearSelectedCellValues(rows, columns);
	}

	private record ClearCellTarget(int row, int column, Node node, Field field) { }

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

	private boolean isNameCellNavigationEnabled() {
		int column = getCurrentViewColumn();
		return isNameFieldColumn(column);
	}

	public void executeNameCellCollapseExpand(boolean expand) {
		if (hierarchyActionInProgress)
			return;
		if (!(getModel() instanceof SpreadSheetModel model))
			return;
		var column = getCurrentViewColumn();
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

	/** Moves the active name cell to the first or last visible task row, matching MSP sheet navigation. */
	public void executeNameCellBoundaryJump(boolean last) {
		if (hierarchyActionInProgress || !isNameCellNavigationEnabled())
			return;
		if (!(getModel() instanceof SpreadSheetModel model) || getRowCount() == 0)
			return;
		var targetRow = findBoundaryVisibleRow(model, last);
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

	private int findBoundaryVisibleRow(SpreadSheetModel model, boolean last) {
		if (model == null)
			return -1;
		int step = last ? -1 : 1;
		int start = last ? getRowCount() - 1 : 0;
		for (int row = start; row >= 0 && row < getRowCount(); row += step) {
			var candidate = model.getNode(row);
			if (candidate != null && !candidate.isVoid())
				return row;
		}
		return -1;
	}

	public void setFieldArray(ArrayList fieldArray) {
		if (!SwingUtilities.isEventDispatchThread()) {
			try {
				SwingUtilities.invokeAndWait(() -> setFieldArray(fieldArray));
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted while updating spreadsheet columns", exception);
			} catch (InvocationTargetException exception) {
				Throwable cause = exception.getCause();
				if (cause instanceof RuntimeException runtimeException)
					throw runtimeException;
				if (cause instanceof Error error)
					throw error;
				throw new IllegalStateException("Could not update spreadsheet columns", cause);
			}
			return;
		}
		clearHeaderColumnSelectionState();
		if (getSelection() != null)
			getSelection().clearActiveCell();
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
			setColumnModel(spreadSheetColumnModel);
	
			selection = new SpreadSheetSelectionModel(this);
			selection.setRowSelection(new SpreadSheetListSelectionModel(selection, true));
			selection.setColumnSelection(new SpreadSheetListSelectionModel(selection, false));
			setSelectionModel(selection.getRowSelection());
			createDefaultColumnsFromModel(spreadSheetModel.getFieldArray()); //Consume memory
			getColumnModel().setSelectionModel(selection.getColumnSelection());
		}
		
		registerEditors(); //Consume memory
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
	}

	@Override
	public boolean editCellAt(int row, int column, java.util.EventObject event) {
		// MSP opens Task Information for a double-click anywhere in a task row.
		// Reserve mouse double-clicks for that command; F2 and typed input still
		// use the normal editor path.
		if (isTaskTable() && event instanceof MouseEvent mouse
				&& mouse.getID() == MouseEvent.MOUSE_PRESSED && mouse.getClickCount() >= 2)
			return false;
		return super.editCellAt(row, column, event);
	}

	/**
	 * Own table gestures after the Swing UI delegate has processed the native
	 * event. ETable's own mouse listener may consume or replace a selection;
	 * installing a second listener made dispatch order determine whether task
	 * clicks selected a cell or a whole row. Processing here leaves one
	 * deterministic controller for the task-table surface.
	 */
	@Override
	protected void processMouseEvent(MouseEvent e) {
		int[] taskRowsBeforePress = e.getID() == MouseEvent.MOUSE_PRESSED && isTaskTable()
				&& SwingUtilities.isLeftMouseButton(e) ? getSelectedRows() : null;
		super.processMouseEvent(e);
		if (!tableMouseHandlerInstalled)
			return;
		if (e.getID() == MouseEvent.MOUSE_PRESSED) {
			suppressTaskClickRestore = false;
			taskRowsSelectedAfterPress = null;
			cellRangeRowsAfterDrag = null;
			cellRangeColumnsAfterDrag = null;
			restoreModifiedTaskSelection = false;
			beginCellRangeSelection(e);
			// BasicTableUI has already applied its own Ctrl/Shift selection by
			// this point. Restore the pre-press task rows before applying the
			// task-table gesture so the modifier transition runs exactly once.
			if (taskRowsBeforePress != null && (e.isControlDown() || e.isMetaDown() || e.isShiftDown()))
				restoreTaskRowSelection(taskRowsBeforePress, getSelection().getActiveRow(), getSelection().getActiveColumn());
			handleTableMousePressed(e);
			if (popupTriggerController.mousePressed(e))
				handleTablePopupTrigger(e);
			if (isTaskTable() && SwingUtilities.isLeftMouseButton(e)) {
				taskRowsSelectedAfterPress = getSelectedRows();
				restoreModifiedTaskSelection = e.isControlDown() || e.isMetaDown() || e.isShiftDown();
			}
		} else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
			int releasedRow = rowAtPoint(e.getPoint());
			int releasedColumn = columnAtPoint(e.getPoint());
			boolean completedCellRange = selectingCellRange;
			boolean taskRowDrag = isTaskTable() && selectingCellRange
					&& rangeAnchorRow >= 0 && rangeAnchorRow != releasedRow
					&& rangeAnchorColumn == releasedColumn;
			int taskRowAnchor = rangeAnchorRow;
			endCellRangeSelection();
			// BasicTableUI can update the lead selection while handling mouse
			// release, after the press route has selected the complete task row.
			// Restore the task-table selection contract here as well as on click so
			// the visible highlight, active-cell border and F2 target cannot diverge
			// depending on whether the user is selecting or editing a cell.
			if (taskRowDrag) {
				applyTaskRowSelection(releasedRow, false, true, false, false, taskRowAnchor, releasedColumn);
				suppressTaskClickRestore = true;
			} else if (completedCellRange) {
				restoreTaskCellRangeSelection(cellRangeRowsAfterDrag, cellRangeColumnsAfterDrag,
					cellRangeActiveRow, cellRangeActiveColumn);
				suppressTaskClickRestore = true;
			} else if (restoreModifiedTaskSelection) {
				restoreTaskRowSelection(taskRowsSelectedAfterPress, releasedRow, releasedColumn);
			} else if (SwingUtilities.isLeftMouseButton(e) && !e.isControlDown()
					&& !e.isMetaDown() && !e.isShiftDown())
				restoreTaskRowSelection(releasedRow, releasedColumn);
			if (popupTriggerController.mouseReleased(e) == PopupTriggerController.ReleaseOutcome.SHOW)
				handleTablePopupTrigger(e);
		} else if (e.getID() == MouseEvent.MOUSE_CLICKED && SwingUtilities.isLeftMouseButton(e)) {
			if (suppressTaskClickRestore) {
				suppressTaskClickRestore = false;
				taskRowsSelectedAfterPress = null;
				return;
			}
			if (restoreModifiedTaskSelection) {
				restoreTaskRowSelection(taskRowsSelectedAfterPress, rowAtPoint(e.getPoint()), columnAtPoint(e.getPoint()));
				taskRowsSelectedAfterPress = null;
				restoreModifiedTaskSelection = false;
			} else if (!e.isControlDown() && !e.isMetaDown() && !e.isShiftDown()) {
				// ETable also updates its lead selection from mouseClicked. Apply row
				// semantics after the final UI-delegate callback.
				restoreTaskRowSelection(rowAtPoint(e.getPoint()), columnAtPoint(e.getPoint()));
				taskRowsSelectedAfterPress = null;
			}
		}
	}

	/** Compatibility name for callers compiled against the earlier navigation helper. */
	@Deprecated
	public void executeNameCellHierarchyJump(boolean forward) {
		executeNameCellBoundaryJump(forward);
	}

	@Override
	protected void processMouseMotionEvent(MouseEvent e) {
		super.processMouseMotionEvent(e);
		if (tableMouseHandlerInstalled && e.getID() == MouseEvent.MOUSE_DRAGGED) {
			extendCellRangeSelection(e);
			if (isTaskTable() && selectingCellRange) {
				cellRangeRowsAfterDrag = getSelectedRows();
				cellRangeColumnsAfterDrag = getSelectedColumns();
				cellRangeActiveRow = getSelection().getActiveRow();
				cellRangeActiveColumn = getSelection().getActiveColumn();
			}
		}
	}

	void handleTableMousePressed(MouseEvent e) {
		var p = e.getPoint();
		var row = rowAtPoint(p);
		var col = columnAtPoint(p);
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
				} else {
					doClick(row,col);
				}
				// The BasicTableUI mouse listener otherwise applies a second,
				// cell-only selection after this controller has selected the complete
				// task row.  This table owns task-cell gestures, so consume the press
				// once its single selection route has run.
				e.consume();
			}
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
		int column = getCurrentViewColumn();
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
	private int[] taskRowsSelectedAfterPress;
	private int[] cellRangeRowsAfterDrag;
	private int[] cellRangeColumnsAfterDrag;
	private int cellRangeActiveRow = -1;
	private int cellRangeActiveColumn = -1;
	private boolean restoreModifiedTaskSelection;
	private boolean suppressTaskClickRestore;

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

	/**
	 * Applies a task-table column preset through the same project persistence and
	 * Undo path as header insert/hide.  Presets are view state, but for the task
	 * sheet that state belongs to the project and must survive save/reload.
	 */
	public boolean applyColumnLayoutPreset(SpreadSheetFieldArray fields, String presentationName) {
		if (!isCanModifyColumns() || fields == null || !(getFieldArray() instanceof SpreadSheetFieldArray current))
			return false;
		SpreadSheetFieldArray before = (SpreadSheetFieldArray) current.clone();
		SpreadSheetFieldArray after = (SpreadSheetFieldArray) fields.clone();
		applyColumnLayoutChange(before, after, 0, presentationName);
		return true;
	}

	private void applyColumnLayoutChange(SpreadSheetFieldArray before, SpreadSheetFieldArray after,
			int preferredColumn, String presentationName) {
		Project project = getLayoutProject();
		if (project != null) {
			project.setFieldArray(after);
			project.setDirty(true);
		}
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
			if (currentProject != null) {
				currentProject.setFieldArray(restoredFields);
				currentProject.setDirty(true);
			}
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
			applyTaskRowSelection(row, toggle, extend, false, false, -1, col);
		}
		scrollRectToVisible(getCellRect(row, col, true));
	}

	private void restoreTaskRowSelection(int[] selectedRows, int activeRow, int activeColumn) {
		if (selectedRows == null || getSelection() == null)
			return;
		getSelectionModel().clearSelection();
		for (int row : selectedRows) {
			if (row >= 0 && row < getRowCount())
				getSelectionModel().addSelectionInterval(row, row);
		}
		if (getSelectedRowCount() == 0) {
			clearSelection();
			return;
		}
		setHeaderColumnSelectionActive(false);
		setRowHeaderSelectionActive(false);
		getColumnModel().getSelectionModel().setSelectionInterval(0, getColumnCount() - 1);
		if (getSelectionModel().isSelectedIndex(activeRow) && activeColumn >= 0 && activeColumn < getColumnCount())
			getSelection().setActiveCell(activeRow, activeColumn);
		else
			getSelection().clearActiveCell();
	}

	private void restoreTaskCellRangeSelection(int[] selectedRows, int[] selectedColumns,
			int activeRow, int activeColumn) {
		if (selectedRows == null || selectedColumns == null || getSelection() == null)
			return;
		getSelectionModel().clearSelection();
		for (int row : selectedRows) {
			if (row >= 0 && row < getRowCount())
				getSelectionModel().addSelectionInterval(row, row);
		}
		getColumnModel().getSelectionModel().clearSelection();
		for (int column : selectedColumns) {
			if (column >= 0 && column < getColumnCount())
				getColumnModel().getSelectionModel().addSelectionInterval(column, column);
		}
		if (getSelectedRowCount() == 0 || getSelectedColumnCount() == 0) {
			clearSelection();
			return;
		}
		setHeaderColumnSelectionActive(false);
		setRowHeaderSelectionActive(false);
		if (getSelectionModel().isSelectedIndex(activeRow)
				&& getColumnModel().getSelectionModel().isSelectedIndex(activeColumn))
			getSelection().setActiveCell(activeRow, activeColumn);
		else
			getSelection().clearActiveCell();
	}

	/** Applies one row-selection transition for table cells, the row header, and the Gantt. */
	public void selectTaskRowsFromGesture(int row, boolean toggle, boolean extend,
			boolean rowHeaderSelection, boolean keepExisting) {
		applyTaskRowSelection(row, toggle, extend, rowHeaderSelection, keepExisting, -1, -1);
	}

	private void applyTaskRowSelection(int row, boolean toggle, boolean extend,
			boolean rowHeaderSelection, boolean keepExisting, int explicitAnchor, int activeColumn) {
		if (row < 0 || row >= getRowCount() || getColumnCount() == 0 || getSelection() == null)
			return;
		setHeaderColumnSelectionActive(false);
		if (extend) {
			int anchor = explicitAnchor >= 0 ? explicitAnchor : getSelectionModel().getAnchorSelectionIndex();
			if (anchor < 0)
				anchor = row;
			getSelectionModel().setSelectionInterval(Math.min(anchor, row), Math.max(anchor, row));
		} else if (toggle) {
			if (getSelectionModel().isSelectedIndex(row))
				getSelectionModel().removeSelectionInterval(row, row);
			else
				getSelectionModel().addSelectionInterval(row, row);
		} else if (!keepExisting) {
			getSelectionModel().setSelectionInterval(row, row);
		}
		if (getSelectedRowCount() == 0) {
			clearSelection();
			return;
		}
		getColumnModel().getSelectionModel().setSelectionInterval(0, getColumnCount() - 1);
		setRowHeaderSelectionActive(rowHeaderSelection);
		if (rowHeaderSelection || activeColumn < 0)
			getSelection().clearActiveCell();
		else
			getSelection().setActiveCell(row, activeColumn);
	}

	/**
	 * Applies the Microsoft Project whole-row selection used by the Gantt
	 * calendar.  Keeping this in the spreadsheet makes a chart click use the
	 * same row/column projection as a task-table click instead of leaving only
	 * one table cell selected.
	 */
	public void selectTaskRowFromGantt(int row, boolean toggle, boolean extend) {
		if (row < 0 || row >= getRowCount() || getColumnCount() == 0) {
			return;
		}
		finishCurrentOperations();
		selectTaskRowsFromGesture(row, toggle, extend, true, false);
		scrollRectToVisible(getCellRect(row, 0, true));
	}

	private void restoreTaskRowSelection(int row, int col) {
		if (row < 0 || row >= getRowCount() || col < 0 || col >= getColumnCount()
				|| getSelection() == null)
			return;
		selectRowAndAllColumns(row);
		setRowHeaderSelectionActive(false);
		getSelection().setActiveCell(row, col);
	}

//    public void columnSelectionChanged(ListSelectionEvent e) {
//    	super.columnSelectionChanged(e);
//    }
    

	public void doDoubleClick(int row, int col) {
		GraphicManager manager = GraphicManager.getInstance(this);
		if (manager == null)
			return;
		Task task = getTaskAtRow(row);
		if (task != null)
			manager.doInformationDialog(task, opensNotesTabOnDoubleClick(col));
		else
			manager.doInformationDialog(false);
	}

	/** MSP opens Task Information directly on Notes for a Notes-field double-click. */
	boolean opensNotesTabOnDoubleClick(int viewColumn) {
		if (!isTaskTable() || viewColumn < 0 || viewColumn >= getColumnCount()
				|| !(getModel() instanceof SpreadSheetModel model))
			return false;
		Field field = model.getFieldInViewColumn(viewColumn);
		// getName() is the localized/aliased display label (for example メモ in
		// Japanese), so it is not a compatibility-safe discriminator.
		return isNotesField(field);
	}

	static boolean isNotesField(Field field) {
		return field != null && "Field.notes".equalsIgnoreCase(field.getId());
	}

	private boolean isTaskTable() {
		return SpreadSheetCategories.taskSpreadsheetCategory.equals(getSpreadSheetCategory());
	}

	/**
	 * Resolves the task represented by a clicked view row.  Information dialogs
	 * opened from the spreadsheet must use this value rather than relying on the
	 * document frame's asynchronously updated selection.
	 */
	Task getTaskAtRow(int row) {
		if (row < 0 || row >= getRowCount() || !(getModel() instanceof SpreadSheetModel model))
			return null;
		GraphicNode graphicNode = model.getNode(row);
		Node node = graphicNode == null ? null : graphicNode.getNode();
		return node != null && node.getImpl() instanceof Task task ? task : null;
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
		if (!(getModel() instanceof SpreadSheetModel model) || model.getCache() == null)
			return new LinkedList<>();
		NodeModelCache cache = model.getCache();
		List<Object> elements = cache.getElementsAt(rows);
		List<GraphicNode> nodes = new LinkedList<>();
		for (Object element : elements) {
			if (element instanceof GraphicNode graphicNode)
				nodes.add(graphicNode);
		}
		return nodes;
	}

	/**
	 * Resolves model rows from a stable node selection.  Row indexes are a view
	 * detail and may change when an editor is committed or the outline is
	 * rebuilt; callers that already captured node identities must use this
	 * conversion immediately before dispatching a row-based action.
	 */
	public int[] nodesToRows(List<Node> nodes) {
		if (nodes == null || nodes.isEmpty() || !(getModel() instanceof SpreadSheetModel model)
				|| model.getCache() == null)
			return new int[0];
		List<Integer> rows = new ArrayList<>(nodes.size());
		for (Node node : nodes) {
			if (node == null)
				continue;
			Object cachedNode = model.getCache().getGraphicNode(node);
			GraphicNode graphicNode = cachedNode instanceof GraphicNode candidate ? candidate : null;
			int row = graphicNode == null ? -1 : model.findGraphicNodeRow(graphicNode);
			if (row >= 0)
				rows.add(row);
		}
		return rows.stream().mapToInt(Integer::intValue).toArray();
	}
	
	
	
	
	


	// gui actions
	public void executeAction(String actionId) {
		performAction(actionId, null);
	}

	public void executeAction(String actionId, int[] selectedRows) {
		performAction(actionId, null, selectedRows);
	}

	/** Executes a configured spreadsheet action without exposing nullable lookup results to callers. */
	public boolean performAction(String actionId, ActionEvent event) {
		return performAction(actionId, event, null);
	}

	private boolean performAction(String actionId, ActionEvent event, int[] selectedRows) {
		var action = prepareAction(actionId);
		if (action == null) {
			logger.log(Level.FINE, "No action for {0}", actionId);
			return false;
		}
		if (action instanceof SpreadSheetAction spreadSheetAction)
			spreadSheetAction.setCommandRows(selectedRows);
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
		var handler = getTransferHandler() instanceof NodeListTransferHandler transferHandler ? transferHandler : null;
		if (actions != null) {
			for (int i = 0; i < actions.length; i++) {
				String action = actions[i];
				addAction(action,MenuActionConstants.ACTION_INDENT,indentAction);
				addAction(action,MenuActionConstants.ACTION_OUTDENT,outdentAction);
				addAction(action,MenuActionConstants.ACTION_NEW,newAction);
				addAction(action,MenuActionConstants.ACTION_INSERT_RESOURCE,newAction);
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
		if (getModel() instanceof CommonSpreadSheetModel commonSpreadSheetModel)
			commonSpreadSheetModel.clearActions();
	}
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

		public void setCommandRows(int[] rows) {
			this.rows = rows;
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
			// Capture the node identity before finishing an editor.  Finishing the
			// editor and inserting a row can shift JTable row indices; using the
			// post-insert selected row makes repeated Insert commands drift.
			List<Node> selectedNodes = new ArrayList<>(getSelectedNodes());
			Node currentRowAnchor = selectedNodes.isEmpty() ? getCurrentRowNode() : null;
			finishCurrentOperations();
			for (int i = selectedNodes.size() - 1; i >= 0; i--) {
				Node anchor = selectedNodes.get(i);
				if (anchor == null)
					continue;
				Node inserted = getCache().newNodeBefore(anchor);
				if (inserted != null) {
					restoreTaskRowSelection(List.of(inserted));
					return;
				}
			}
			if (currentRowAnchor != null) {
				Node inserted = getCache().newNodeBefore(currentRowAnchor);
				if (inserted != null)
					restoreTaskRowSelection(List.of(inserted));
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
			final ArrayList<ProjectData> descriptors = new ArrayList<>();
			Session session = SessionFactory.getInstance().getSession(false);
			Job job = (Job) SessionFactory.callNoEx(session, "getLoadProjectDescriptorsJob", new Class<?>[]{boolean.class, java.util.List.class, boolean.class}, new Object[]{true, descriptors, true});
			job.addSwingRunnable(new JobRunnable("Local: addNodes"){
				public Object run() throws Exception{
					final Consumer<Object> setter = new Consumer<Object>() { public void accept(Object obj) {
						}
					};
					final Consumer<Object> getter = new Consumer<Object>() { public void accept(Object obj) {
							ResourceAdditionDialog.Form form = (ResourceAdditionDialog.Form) obj;
							List<Node> nodes = new ArrayList<>(form.getSelectedResources().size());
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
						HashMap<Long, EnterpriseResourceData> resourceMap = new HashMap<>(resources.size());
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
			if (object instanceof List<?> nodes && nodes.stream().allMatch(Node.class::isInstance)) {
				@SuppressWarnings("unchecked")
				List<Node> pastedNodes = (List<Node>) nodes;
				pasteNodesFromClipboard(pastedNodes);
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


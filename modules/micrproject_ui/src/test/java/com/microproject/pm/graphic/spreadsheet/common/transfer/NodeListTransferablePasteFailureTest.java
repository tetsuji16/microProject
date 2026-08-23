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
package com.microproject.pm.graphic.spreadsheet.common.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.StringReader;
import javax.swing.JComponent;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import org.junit.jupiter.api.Test;
import org.apache.commons.collections.Predicate;

import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.collaboration.CollaborationSession;
import com.microproject.menu.MenuActionConstants;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.graphic.spreadsheet.common.transfer.NodeListTransferHandler;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.model.NodeModelDataFactory;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.SubProj;
import com.microproject.pm.task.Task;
import com.microproject.undo.DataFactoryUndoController;

class NodeListTransferablePasteFailureTest {
	@Test
	void nodeListFlavorUsesMimeSemantics() throws Exception {
		Project project = createProject();
		createTask(project, "Source");
		SpreadSheet[] sheetRef = new SpreadSheet[1];
		NodeListTransferHandler[] handlerRef = new NodeListTransferHandler[1];

		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = createSheet(project, "flavor-test");
			sheet.selectRowAndAllColumns(0);
			handlerRef[0] = new NodeListTransferHandler(sheet);
			sheetRef[0] = sheet;
		});

		Clipboard clipboard = new Clipboard("flavor-test");
		SwingUtilities.invokeAndWait(() -> handlerRef[0].exportToClipboard(sheetRef[0], clipboard, TransferHandler.COPY));
		Transferable transferable = clipboard.getContents(null);

		assertTrue(handlerRef[0].canImport(sheetRef[0], transferable.getTransferDataFlavors()));
		assertTrue(Arrays.stream(transferable.getTransferDataFlavors()).anyMatch(NodeListTransferable::isNodeListFlavor));
	}

	@Test
	void copiedTaskRowsPasteAboveSelectionAndCanBePastedRepeatedly() throws Exception {
		Project sourceProject = createProject();
		createTask(sourceProject, "Copied");
		Project targetProject = createProject();
		NormalTask targetTask = createTask(targetProject, "Target");
		int targetTaskCount = targetProject.getTasks().size();
		SpreadSheet[] sourceSheetRef = new SpreadSheet[1];
		SpreadSheet[] targetSheetRef = new SpreadSheet[1];
		NodeListTransferHandler[] sourceHandlerRef = new NodeListTransferHandler[1];
		NodeListTransferHandler[] targetHandlerRef = new NodeListTransferHandler[1];

		SwingUtilities.invokeAndWait(() -> {
			sourceSheetRef[0] = createSheet(sourceProject, "copy-source");
			sourceSheetRef[0].selectRowAndAllColumns(0);
			sourceHandlerRef[0] = new NodeListTransferHandler(sourceSheetRef[0]);
			targetSheetRef[0] = createSheet(targetProject, "copy-target");
			targetSheetRef[0].selectRowAndAllColumns(0);
			targetHandlerRef[0] = new NodeListTransferHandler(targetSheetRef[0]);
		});

		Clipboard clipboard = new Clipboard("copy-paste-test");
		SwingUtilities.invokeAndWait(() -> sourceHandlerRef[0].exportToClipboard(
			sourceSheetRef[0], clipboard, TransferHandler.COPY));
		Transferable transferable = clipboard.getContents(null);
		boolean[] pasted = new boolean[2];
		SwingUtilities.invokeAndWait(() -> {
			pasted[0] = targetHandlerRef[0].importData(targetSheetRef[0], transferable);
			targetSheetRef[0].selectRowAndAllColumns(1);
			pasted[1] = targetHandlerRef[0].importData(targetSheetRef[0], transferable);
		});

		assertTrue(pasted[0]);
		assertTrue(pasted[1]);
		Node firstCopy = findNodeByName(targetProject, "Copied");
		assertNotNull(targetProject.getTaskModel().search(firstCopy.getImpl()));
		assertNotNull(targetProject.getTaskModel().search(targetTask));
		assertEquals(targetTaskCount + 2, targetProject.getTasks().size());
		assertEquals(Arrays.asList("Copied", "Copied", "Target"), rootTaskNames(targetProject));
	}

	@Test
	void pastedTaskRowsRemainIndexedAcrossUndoAndRedo() {
		Project sourceProject = createProject();
		NormalTask sourceTask = createTask(sourceProject, "Copied");
		Project targetProject = createProject();
		createTask(targetProject, "Target");
		ArrayList<Node> sourceNodes = new ArrayList<>();
		sourceNodes.add(sourceProject.getTaskModel().search(sourceTask));
		List<Node> copiedNodes = sourceProject.getTaskModel().copy(sourceNodes, NodeModel.SILENT);
		Node pastedNode = copiedNodes.get(0);
		Node root = (Node)targetProject.getTaskModel().getHierarchy().getRoot();
		targetProject.getUndoController().clear();

		targetProject.getTaskModel().paste(root, copiedNodes, 0, NodeModel.NORMAL);
		assertNotNull(targetProject.getTaskModel().search(pastedNode.getImpl()));

		targetProject.getUndoController().undo();
		assertNull(targetProject.getTaskModel().search(pastedNode.getImpl()));
		targetProject.getUndoController().redo();
		assertNotNull(targetProject.getTaskModel().search(pastedNode.getImpl()));
	}

	@Test
	void clipboardFailureDoesNotRemoveCutTask() throws Exception {
		Project project = createProject();
		NormalTask task = createTask(project, "Keep me");
		SpreadSheet[] sheetRef = new SpreadSheet[1];
		NodeListTransferHandler[] handlerRef = new NodeListTransferHandler[1];

		SwingUtilities.invokeAndWait(() -> {
			sheetRef[0] = createSheet(project, "failed-cut");
			sheetRef[0].selectRowAndAllColumns(0);
			handlerRef[0] = new NodeListTransferHandler(sheetRef[0]);
		});
		Clipboard busyClipboard = new Clipboard("busy") {
			@Override
			public synchronized void setContents(Transferable contents, java.awt.datatransfer.ClipboardOwner owner) {
				throw new IllegalStateException("busy");
			}
		};

		SwingUtilities.invokeAndWait(() -> handlerRef[0].exportToClipboard(
			sheetRef[0], busyClipboard, TransferHandler.MOVE));

		assertNotNull(project.getTaskModel().search(task));
		assertEquals("Keep me", task.getName());
	}

	@Test
	void acceptedTaskCutCanBePastedWithANewUniqueId() throws Exception {
		Project project = createProject();
		NormalTask sourceTask = createTask(project, "Moved");
		long sourceUniqueId = sourceTask.getUniqueId();
		createTask(project, "Target");
		SpreadSheet[] sheetRef = new SpreadSheet[1];
		NodeListTransferHandler[] handlerRef = new NodeListTransferHandler[1];
		Clipboard clipboard = new Clipboard("task-cut");

		SwingUtilities.invokeAndWait(() -> {
			sheetRef[0] = createSheet(project, "cut-paste");
			sheetRef[0].selectRowAndAllColumns(0);
			handlerRef[0] = new NodeListTransferHandler(sheetRef[0]);
			handlerRef[0].exportToClipboard(sheetRef[0], clipboard, TransferHandler.MOVE);
		});

		assertNull(project.getTaskModel().search(sourceTask));
		boolean[] pasted = new boolean[1];
		SwingUtilities.invokeAndWait(() -> {
			sheetRef[0].selectRowAndAllColumns(0);
			pasted[0] = handlerRef[0].importData(sheetRef[0], clipboard.getContents(null));
		});

		assertTrue(pasted[0]);
		Node movedNode = findNodeByName(project, "Moved");
		assertTrue(((NormalTask)movedNode.getImpl()).getUniqueId() != sourceUniqueId);
		assertEquals(Arrays.asList("Moved", "Target"), rootTaskNames(project));
	}

	@Test
	void cellCutClearsSourceOnlyAfterClipboardAcceptsSnapshot() throws Exception {
		Project project = createProject();
		NormalTask task = createTask(project, "Original");
		SpreadSheet[] sheetRef = new SpreadSheet[1];
		NodeListTransferHandler[] handlerRef = new NodeListTransferHandler[1];
		Clipboard clipboard = new Clipboard("cell-cut");

		SwingUtilities.invokeAndWait(() -> {
			sheetRef[0] = createSheet(project, "cell-cut");
			sheetRef[0].changeSelection(0, 1, false, false);
			handlerRef[0] = new NodeListTransferHandler(sheetRef[0]);
			handlerRef[0].exportToClipboard(sheetRef[0], clipboard, TransferHandler.MOVE);
		});

		assertEquals("Original\n", clipboard.getContents(null).getTransferData(DataFlavor.stringFlavor));
		assertTrue(task.getName() == null || task.getName().isEmpty());
	}

	@Test
	void clickingCellAfterRowHeaderSwitchesBackToCellCopy() throws Exception {
		Project project = createProject();
		createTask(project, "Task");
		SpreadSheet[] sheetRef = new SpreadSheet[1];

		SwingUtilities.invokeAndWait(() -> {
			sheetRef[0] = createSheet(project, "selection-mode");
			sheetRef[0].selectRowAndAllColumns(0);
			assertNull(sheetRef[0].getSelectedFields());
			sheetRef[0].changeSelection(0, 1, false, false);
		});

		assertNotNull(sheetRef[0].getSelectedFields());
		assertEquals(1, sheetRef[0].getSelectedFields().size());
	}

	@Test
	void pasteAsValuesReadsPlainUnicodeReader() throws Exception {
		Project project = createProject();
		NormalTask task = createTask(project, "Original");
		SpreadSheet[] sheetRef = new SpreadSheet[1];
		Method pasteAsValuesMethod = SpreadSheet.class.getDeclaredMethod(
			"pasteClipboardAsValues", Transferable.class);
		pasteAsValuesMethod.setAccessible(true);
		Transferable unicodeOnly = unicodeTransferable("Unicode value");

		SwingUtilities.invokeAndWait(() -> {
			sheetRef[0] = createSheet(project, "unicode-paste");
			sheetRef[0].changeSelection(0, 1, false, false);
			try {
				pasteAsValuesMethod.invoke(sheetRef[0], unicodeOnly);
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		});

		assertEquals("Unicode value", task.getName());
	}

	@Test
	void normalPasteReadsPlainUnicodeReader() throws Exception {
		Project project = createProject();
		NormalTask task = createTask(project, "Original");
		SpreadSheet[] sheetRef = new SpreadSheet[1];
		NodeListTransferHandler[] handlerRef = new NodeListTransferHandler[1];
		boolean[] pasted = new boolean[1];
		Transferable unicodeOnly = unicodeTransferable("Unicode value");

		SwingUtilities.invokeAndWait(() -> {
			sheetRef[0] = createSheet(project, "unicode-normal-paste");
			sheetRef[0].changeSelection(0, 1, false, false);
			handlerRef[0] = new NodeListTransferHandler(sheetRef[0]);
			pasted[0] = handlerRef[0].importData(sheetRef[0], unicodeOnly);
		});

		assertTrue(pasted[0]);
		assertEquals("Unicode value", task.getName());
	}

	@Test
	void readonlyProjectRejectsCellPaste() throws Exception {
		Project project = createProject();
		NormalTask task = createTask(project, "Original");
		SpreadSheet[] sheetRef = new SpreadSheet[1];
		NodeListTransferHandler[] handlerRef = new NodeListTransferHandler[1];
		boolean[] pasted = new boolean[1];

		SwingUtilities.invokeAndWait(() -> {
			sheetRef[0] = createSheet(project, "readonly-paste");
			sheetRef[0].changeSelection(0, 1, false, false);
			handlerRef[0] = new NodeListTransferHandler(sheetRef[0]);
			project.setReadOnly(true);
			pasted[0] = handlerRef[0].importData(sheetRef[0], new StringSelection("Changed"));
		});

		assertFalse(pasted[0]);
		assertEquals("Original", task.getName());
	}

	@Test
	void collaborationLockRejectionPreventsCellPaste() throws Exception {
		Project project = createProject();
		NormalTask task = createTask(project, "Original");
		project.setCollaborationSession(new RejectingCollaborationSession(project));
		SpreadSheet[] sheetRef = new SpreadSheet[1];
		NodeListTransferHandler[] handlerRef = new NodeListTransferHandler[1];
		boolean[] pasted = new boolean[1];

		SwingUtilities.invokeAndWait(() -> {
			sheetRef[0] = createSheet(project, "locked-paste");
			sheetRef[0].changeSelection(0, 1, false, false);
			handlerRef[0] = new NodeListTransferHandler(sheetRef[0]);
			pasted[0] = handlerRef[0].importData(sheetRef[0], new StringSelection("Changed"));
		});

		assertFalse(pasted[0]);
		assertEquals("Original", task.getName());
	}

	@Test
	void pasteInsertCreatesTextTasksAboveSelectedRow() throws Exception {
		Project project = createProject();
		createTask(project, "Target");
		SpreadSheet[] sheetRef = new SpreadSheet[1];
		Method pasteInsertedMethod = SpreadSheet.class.getDeclaredMethod(
			"pasteInsertedClipboardContents", Transferable.class);
		pasteInsertedMethod.setAccessible(true);

		SwingUtilities.invokeAndWait(() -> {
			sheetRef[0] = createSheet(project, "paste-insert");
			sheetRef[0].changeSelection(0, 1, false, false);
			try {
				pasteInsertedMethod.invoke(sheetRef[0], new StringSelection("Inserted"));
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		});

		assertEquals(Arrays.asList("Inserted", "Target"), rootTaskNames(project));
	}
	@Test
	void invalidMultiCellValuePasteDoesNotFallBackToLinePaste() throws Exception {
		final Project project = createProject();
		final NormalTask task = createTask(project, "Original");
		final SpreadSheet[] sheetRef = new SpreadSheet[1];

		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()),
				"paste-test",
				null);
			SpreadSheetUtils.setFieldsAndContext(sheet,
				cache,
				SpreadSheetCategories.taskSpreadsheetCategory,
				"Spreadsheet.Task.entry",
				true);
			sheet.setRowSelectionInterval(0, 0);
			selectNameThroughDuration(sheet);
			sheetRef[0] = sheet;
		});

		SwingUtilities.invokeAndWait(() -> NodeListTransferable.pasteString(invalidDurationClipboard(sheetRef[0]), sheetRef[0]));

		assertEquals("Original", task.getName());
	}

	@Test
	void pasteAsValuesUsesTextFlavorEvenWhenNodeFlavorExists() throws Exception {
		final Project sourceProject = createProject();
		final NormalTask sourceTask = createTask(sourceProject, "Copied");
		final Project targetProject = createProject();
		final NormalTask targetTask = createTask(targetProject, "Original");
		final SpreadSheet[] targetSheetRef = new SpreadSheet[1];
		final Method pasteAsValuesMethod = SpreadSheet.class.getDeclaredMethod("pasteClipboardAsValues", java.awt.datatransfer.Transferable.class);
		pasteAsValuesMethod.setAccessible(true);
		final NodeListTransferable[] transferableRef = new NodeListTransferable[1];

		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sourceSheet = new SpreadSheet();
			sourceSheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache sourceCache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(sourceProject, sourceProject.getTaskModel()),
				"source-paste-test",
				null);
			SpreadSheetUtils.setFieldsAndContext(sourceSheet,
				sourceCache,
				SpreadSheetCategories.taskSpreadsheetCategory,
				"Spreadsheet.Task.entry",
				true);
			int sourceRow = sourceSheet.getValueAt(0, 1) != null ? 0 : 1;
			sourceSheet.setRowSelectionInterval(sourceRow, sourceRow);
			sourceSheet.setColumnSelectionInterval(1, 1);

			SpreadSheet targetSheet = new SpreadSheet();
			targetSheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache targetCache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(targetProject, targetProject.getTaskModel()),
				"target-paste-test",
				null);
			SpreadSheetUtils.setFieldsAndContext(targetSheet,
				targetCache,
				SpreadSheetCategories.taskSpreadsheetCategory,
				"Spreadsheet.Task.entry",
				true);
			int targetRow = targetSheet.getValueAt(0, 1) != null ? 0 : 1;
			targetSheet.setRowSelectionInterval(targetRow, targetRow);
			targetSheet.setColumnSelectionInterval(1, 1);
			targetSheetRef[0] = targetSheet;

			ArrayList<Node> nodes = new ArrayList<>(sourceSheet.getSelectedNodes());
			transferableRef[0] = new NodeListTransferable(nodes, sourceSheet.getSelectedFields(),
				sourceSheet, sourceSheet.getSelectedRows(), sourceSheet.getSelectedColumns(), true);
		});

		SwingUtilities.invokeAndWait(() -> {
			try {
				pasteAsValuesMethod.invoke(targetSheetRef[0], transferableRef[0]);
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		});

		assertTrue(transferableRef[0].isDataFlavorSupported(DataFlavor.stringFlavor));
		assertEquals("Copied", targetTask.getName());
		assertEquals("Copied", sourceTask.getName());
	}

	@Test
	void clipboardTextIsSnapshotAtCopyTime() throws Exception {
		final Project project = createProject();
		final NormalTask task = createTask(project, "Copied");
		final NodeListTransferable[] transferableRef = new NodeListTransferable[1];

		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()),
				"clipboard-snapshot-test",
				null);
			SpreadSheetUtils.setFieldsAndContext(sheet,
				cache,
				SpreadSheetCategories.taskSpreadsheetCategory,
				"Spreadsheet.Task.entry",
				true);
			int row = sheet.getValueAt(0, 1) != null ? 0 : 1;
			sheet.setRowSelectionInterval(row, row);
			sheet.setColumnSelectionInterval(1, 1);
			transferableRef[0] = new NodeListTransferable(new ArrayList<>(sheet.getSelectedNodes()),
				sheet.getSelectedFields(), sheet, sheet.getSelectedRows(), sheet.getSelectedColumns(), false);
			task.setName("Changed after copy");
		});

		assertEquals("Copied\n", transferableRef[0].getTransferData(DataFlavor.stringFlavor));
	}

	@Test
	void invalidMultiCellValueImportDoesNotReportSuccess() throws Exception {
		final Project project = createProject();
		final NormalTask task = createTask(project, "Original");
		final SpreadSheet[] sheetRef = new SpreadSheet[1];
		final NodeListTransferHandler[] handlerRef = new NodeListTransferHandler[1];
		final boolean[] importedRef = new boolean[1];

		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()),
				"paste-test",
				null);
			SpreadSheetUtils.setFieldsAndContext(sheet,
				cache,
				SpreadSheetCategories.taskSpreadsheetCategory,
				"Spreadsheet.Task.entry",
				true);
			sheet.setRowSelectionInterval(0, 0);
			selectNameThroughDuration(sheet);
			sheetRef[0] = sheet;
			handlerRef[0] = new NodeListTransferHandler(sheet);
		});

		SwingUtilities.invokeAndWait(() -> importedRef[0] = handlerRef[0].importData(sheetRef[0], new StringSelection(invalidDurationClipboard(sheetRef[0]))));

		assertFalse(importedRef[0]);
		assertEquals("Original", task.getName());
	}

	@Test
	void invalidMultiCellValueFallbackPasteDoesNotPartiallyApply() throws Exception {
		final Project project = createProject();
		final NormalTask task = createTask(project, "Original");
		final SpreadSheet[] sheetRef = new SpreadSheet[1];
		final boolean[] pastedRef = new boolean[1];

		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()),
				"paste-test",
				null);
			SpreadSheetUtils.setFieldsAndContext(sheet,
				cache,
				SpreadSheetCategories.taskSpreadsheetCategory,
				"Spreadsheet.Task.entry",
				true);
			sheet.setRowSelectionInterval(0, 0);
			sheet.setColumnSelectionInterval(3, 3);
			sheetRef[0] = sheet;
		});

		SwingUtilities.invokeAndWait(() -> pastedRef[0] = NodeListTransferable.pasteString("New name\tnot-a-duration", sheetRef[0]));

		assertFalse(pastedRef[0]);
		assertEquals("Original", task.getName());
	}

	@Test
	void nodeListPasteIntoReadonlyParentReturnsFalse() throws Exception {
		final Project sourceProject = createProject();
		final NormalTask sourceTask = createTask(sourceProject, "Source");

		final Project targetProject = createProject();
		final NormalTask targetTask = createTask(targetProject, "Target");
		createTask(targetProject, "Other");
		targetTask.setExternal(true);
		targetProject.setReadOnly(true);
		int targetTaskCount = targetProject.getTasks().size();
		ArrayList<Node> nodes = new ArrayList<>();
		nodes.add((Node) sourceProject.getTaskModel().search(sourceTask));
		List copiedNodes = sourceProject.getTaskModel().copy(nodes, NodeModel.SILENT);

		NodeModelCache targetCache = NodeModelCacheFactory.getInstance().createFilteredCache(
			NodeModelCacheFactory.createTaskNodeModelCache(targetProject, targetProject.getTaskModel()),
			"paste-target",
			null);
		boolean pasted = targetCache.pasteNodes((Node) targetProject.getTaskModel().search(targetTask), copiedNodes, 0);

		assertFalse(pasted);
		assertTrue(targetProject.isReadOnly());
		assertEquals(targetTaskCount, targetProject.getTasks().size());
		assertEquals("Target", targetTask.getName());
	}

	@Test
	void newActionSkipsReadonlyLastSelectionAndUsesEarlierEditableNode() throws Exception {
		final Project project = createProject();
		project.getTaskModel().getHierarchy().setNbEndVoidNodes(0);
		createTask(project, "Editable");
		NormalTask readonlyTask = createTask(project, "Readonly");
		final SpreadSheet[] sheetRef = new SpreadSheet[1];
		final int[] childCountBefore = new int[1];
		final int[] childCountAfter = new int[1];

		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()),
				"new-action-test",
				null);
			SpreadSheetUtils.setFieldsAndContext(sheet,
				cache,
				SpreadSheetCategories.taskSpreadsheetCategory,
				"Spreadsheet.Task.entry",
				true);
			project.getTaskModel().getHierarchy().setNbEndVoidNodes(0);
			Node root = (Node) project.getTaskModel().getHierarchy().getRoot();
			childCountBefore[0] = project.getTaskModel().getHierarchy().getChildren(root).size();
			sheet.setRowSelectionInterval(0, 1);
			sheetRef[0] = sheet;
		});

		readonlyTask.setExternal(true);

		SwingUtilities.invokeAndWait(() -> sheetRef[0].executeAction(MenuActionConstants.ACTION_NEW));

		SwingUtilities.invokeAndWait(() -> {
			Node root = (Node) project.getTaskModel().getHierarchy().getRoot();
			childCountAfter[0] = project.getTaskModel().getHierarchy().getChildren(root).size();
		});

		assertEquals(childCountBefore[0] + 1, childCountAfter[0]);
	}

	@Test
	void subprojectPasteIsNormalizedToATask() throws Exception {
		final Project sourceProject = createProject();
		sourceProject.getTaskModel().getHierarchy().setNbEndVoidNodes(0);
		SubprojectTask sourceTask = createSubprojectTask(sourceProject, "Subproject");

		final Project targetProject = createProject();
		targetProject.getTaskModel().getHierarchy().setNbEndVoidNodes(0);
		createTask(targetProject, "Target");
		createTask(targetProject, "Other");

		ArrayList<Node> nodes = new ArrayList<>();
		nodes.add((Node) sourceProject.getTaskModel().search(sourceTask));
		List copiedNodes = sourceProject.getTaskModel().copy(nodes, NodeModel.SILENT);
		Node pastedNode = (Node) copiedNodes.get(0);

		NodeListTransferHandler handler = new NodeListTransferHandler(null);
		Method method = NodeListTransferHandler.class.getDeclaredMethod(
			"transformSubprojectBranches",
			Node.class,
			NodeModelDataFactory.class,
			Predicate.class);
		method.setAccessible(true);
		boolean transformed = (Boolean) method.invoke(handler, pastedNode, targetProject, new Predicate() {
			public boolean evaluate(Object object) {
				Node parent = (Node) object;
				NormalTask task = new NormalTask();
				((NormalTask) parent.getImpl()).cloneTo(task);
				parent.setImpl(task);
				return true;
			}
		});

		assertTrue(transformed);
		assertFalse(pastedNode.getImpl() instanceof SubProj);
		assertInstanceOf(NormalTask.class, pastedNode.getImpl());
		assertEquals(sourceTask.getName(), ((NormalTask) pastedNode.getImpl()).getName());
	}

	private Project createProject() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("paste-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		return project;
	}

	private NormalTask createTask(Project project, String name) {
		NormalTask task = project.createScriptedTask();
		task.setName(name);
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
		return task;
	}

	private static void selectNameThroughDuration(SpreadSheet sheet) {
		sheet.setColumnSelectionInterval(columnForField(sheet, "Field.name"), columnForField(sheet, "Field.duration"));
	}

	private static String invalidDurationClipboard(SpreadSheet sheet) {
		int nameColumn = columnForField(sheet, "Field.name");
		int durationColumn = columnForField(sheet, "Field.duration");
		StringBuilder clipboard = new StringBuilder();
		for (int column = nameColumn; column <= durationColumn; column++) {
			if (column > nameColumn)
				clipboard.append('\t');
			Field field = (Field) sheet.getColumnModel().getColumn(column).getIdentifier();
			if ("Field.name".equals(field.getId()))
				clipboard.append("New name");
			else if ("Field.duration".equals(field.getId()))
				clipboard.append("not-a-duration");
		}
		return clipboard.toString();
	}

	private static int columnForField(SpreadSheet sheet, String fieldId) {
		for (int column = 0; column < sheet.getColumnCount(); column++) {
			Field field = (Field) sheet.getColumnModel().getColumn(column).getIdentifier();
			if (fieldId.equals(field.getId()))
				return column;
		}
		throw new AssertionError("Missing column for " + fieldId);
	}

	private ReferenceNodeModelCache createTaskNodeModelCache(Project project) {
		return NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
	}

	private SpreadSheet createSheet(Project project, String viewName) {
		SpreadSheet sheet = new SpreadSheet();
		sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
		NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
			NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()),
			viewName,
			null);
		SpreadSheetUtils.setFieldsAndContext(sheet,
			cache,
			SpreadSheetCategories.taskSpreadsheetCategory,
			"Spreadsheet.Task.entry",
			true);
		return sheet;
	}

	private SubprojectTask createSubprojectTask(Project project, String name) {
		SubprojectTask task = new SubprojectTask(project);
		task.setName(name);
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
		return task;
	}

	private Node findNodeByName(Project project, String name) {
		Node root = (Node) project.getTaskModel().getHierarchy().getRoot();
		for (Object child : project.getTaskModel().getHierarchy().getChildren(root)) {
			Node node = (Node) child;
			Object impl = node.getImpl();
			if (impl instanceof NormalTask task && name.equals(task.getName())) {
				return node;
			}
		}
		throw new AssertionError("Could not find pasted node named " + name);
	}

	private List<String> rootTaskNames(Project project) {
		List<String> names = new ArrayList<>();
		Node root = (Node)project.getTaskModel().getHierarchy().getRoot();
		for (Object child : project.getTaskModel().getHierarchy().getChildren(root)) {
			Object impl = ((Node)child).getImpl();
			if (impl instanceof NormalTask task) names.add(task.getName());
		}
		return names;
	}

	private Transferable unicodeTransferable(String text) {
		return new Transferable() {
			private final DataFlavor flavor = DataFlavor.getTextPlainUnicodeFlavor();
			public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[] { flavor }; }
			public boolean isDataFlavorSupported(DataFlavor candidate) { return flavor.equals(candidate); }
			public Object getTransferData(DataFlavor candidate) throws UnsupportedFlavorException {
				if (!isDataFlavorSupported(candidate)) throw new UnsupportedFlavorException(candidate);
				return new StringReader(text);
			}
		};
	}

	private static final class SubprojectTask extends NormalTask implements SubProj {
		private Project subproject;
		private long subprojectUniqueId;
		private boolean fetching;

		private SubprojectTask(Project project) {
			super(project);
		}

		public Project getSubproject() {
			return subproject;
		}

		public boolean isSubprojectOpen() {
			return subproject != null;
		}

		public boolean isValidAndOpen() {
			return subproject != null;
		}

		public boolean isWritable() {
			return true;
		}

		public long getSubprojectUniqueId() {
			return subprojectUniqueId;
		}

		public void setFetching(boolean b) {
			fetching = b;
		}

		public boolean isValid() {
			return true;
		}

		public void setSubprojectFieldValues(java.util.Map subprojectFieldValues) {
		}

		public void setSubprojectUniqueId(long subprojectId) {
			this.subprojectUniqueId = subprojectId;
		}

		public void setSchedulesFromSubprojectFieldValues() {
		}
	}

	/**
	 * Regression for issue #47: the SpreadSheet must NOT bind ctrl C/X/V on its own
	 * WHEN_FOCUSED input map. Those keys are wired globally to ACTION_COPY/CUT/PASTE
	 * on the document root-pane (WHEN_IN_FOCUSED_WINDOW) by GraphicManager.applyMicrosoftShortcuts.
	 * If both layers answer ctrl+V the paste shortcut becomes non-deterministic
	 * ("paste doesn't work well"). The real paste logic (NodeListTransferHandler.importData)
	 * stays reachable through ACTION_PASTE, so only the duplicate keyboard bindings are removed.
	 */
	@Test
	void spreadSheetDoesNotDoubleBindClipboardShortcuts() throws Exception {
		Project project = createProject();
		final SpreadSheet[] sheetRef = new SpreadSheet[1];
		SwingUtilities.invokeAndWait(() -> sheetRef[0] = createSheet(project, "double-wire-regression"));
		SpreadSheet sheet = sheetRef[0];
		InputMap focused = sheet.getInputMap(JComponent.WHEN_FOCUSED);
		assertNull(focused.get(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK)),
				"ctrl+V must not be bound on WHEN_FOCUSED (root-pane ACTION_PASTE owns it)");
		assertNull(focused.get(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK)),
				"ctrl+C must not be bound on WHEN_FOCUSED (root-pane ACTION_COPY owns it)");
		assertNull(focused.get(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK)),
				"ctrl+X must not be bound on WHEN_FOCUSED (root-pane ACTION_CUT owns it)");
		// The real paste logic (NodeListTransferHandler.importData) is still reachable via
		// ACTION_PASTE on the root pane (wired by GraphicManager.applyMicrosoftShortcuts),
		// so removing these duplicate bindings does not disable paste.
	}

	private static final class RejectingCollaborationSession extends CollaborationSession {
		private RejectingCollaborationSession(Project project) {
			super(project, new java.io.File(System.getProperty("java.io.tmpdir"),
				"projectlibre-lock-test.pod").getAbsolutePath(), "test-user");
		}

		@Override
		public boolean tryLockTasks(Iterable<Task> tasks, java.awt.Component parent, String actionLabel) {
			return false;
		}
	}
}

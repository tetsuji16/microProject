/*******************************************************************************
 * MIT License
 *
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
package com.microproject.ui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.microproject.configuration.Dictionary;
import com.microproject.grouping.core.model.NodeModelDataFactory;
import com.microproject.interval.ValueObjectForIntervalTable;
import com.microproject.options.GeneralOption;
import com.microproject.ui.view.spreadsheet.DialogSpreadSheetShortcuts;
import com.microproject.ui.view.spreadsheet.SpreadSheet;
import com.microproject.ui.view.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.strings.Messages;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Alert;
import com.microproject.util.AlertPresenter;
import com.microproject.util.DateTime;
import com.microproject.util.Environment;

/**
 * Issue #997: the document shortcuts live on the document root pane, so a table inside a
 * dialog had no keyboard Delete/Cut at all. The dialog route must delete the dialog's own
 * row, keep the first interval protected, and report the refusal once.
 */
class DialogTableKeyboardRouteTest {
	private final List<String> errors = new ArrayList<>();
	private boolean confirmDeletes;
	private boolean previousClientSide;
	private AlertPresenter previousPresenter;

	private record Fixture(Project project, ValueObjectForIntervalTable table, SpreadSheet sheet, JRootPane root) {
	}

	@AfterEach
	void restoreState() {
		GeneralOption.getInstance().setConfirmDeletes(confirmDeletes);
		Environment.setClientSide(previousClientSide);
		Alert.setPresenter(previousPresenter);
	}

	private Fixture fixture() throws Exception {
		Dictionary.getInstance();
		confirmDeletes = GeneralOption.getInstance().isConfirmDeletes();
		GeneralOption.getInstance().setConfirmDeletes(false);
		previousClientSide = Environment.isClientSide();
		Environment.setClientSide(true);
		previousPresenter = alertPresenter();
		Alert.setPresenter(new AlertPresenter() {
			@Override
			public void warn(Object message, Object parent) {
			}

			@Override
			public void error(Object message, Object parent) {
				errors.add(String.valueOf(message));
			}

			@Override
			public int confirmYesNo(Object message) {
				return Alert.YES_OPTION;
			}

			@Override
			public int confirm(Object message) {
				return Alert.OK_OPTION;
			}

			@Override
			public boolean okCancel(Object message) {
				return true;
			}

			@Override
			public String renameProject(String name, Set<String> projectNames, boolean saveAs) {
				return null;
			}

			@Override
			public void warnWithOnceOption(Object message, String preference, Object parent) {
			}
		});
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("dialog-keys", undo), undo);
		project.initialize(false, false);
		project.getResourcePool().addProject(project);
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		resource.setName("Keyboard resource");
		ValueObjectForIntervalTable table = resource.getAvailabilityTable();
		table.newValueObject(DateTime.calendarInstance(2030, Calendar.JANUARY, 1).getTimeInMillis());
		assertEquals(2, table.getList().size());
		Fixture[] built = new Fixture[1];
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory("availabilitySpreadsheet");
			sheet.setCanSelectFieldArray(false);
			SpreadSheetUtils.createCollectionSpreadSheet(sheet, table.getList(), "View.ResourceInformation.Availability",
					"availabilitySpreadsheet", "Spreadsheet.availability", false, (NodeModelDataFactory) table, 1);
			ResourceInformationDialog.trackTableEdits(sheet, () -> resource);
			project.markSaved();
			JRootPane root = new JRootPane();
			DialogSpreadSheetShortcuts.install(root);
			root.setContentPane(sheet);
			built[0] = new Fixture(project, table, sheet, root);
		});
		assertFalse(project.needsSaving(), "opening the dialog table must not count as an edit");
		return built[0];
	}

	/** Alert stores its presenter privately; restore whatever the previous test installed. */
	private AlertPresenter alertPresenter() {
		try {
			var field = Alert.class.getDeclaredField("presenter");
			field.setAccessible(true);
			return (AlertPresenter) field.get(null);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	/** The dialog root pane owns the four editing keys Microsoft Project uses in its tables. */
	@Test
	void dialogRootPaneBindsDeleteCutCopyAndPaste() throws Exception {
		Fixture fixture = fixture();
		int shortcutMask;
		try {
			shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
		} catch (HeadlessException e) {
			shortcutMask = InputEvent.CTRL_DOWN_MASK;
		}
		final int menuMask = shortcutMask;
		SwingUtilities.invokeAndWait(() -> {
			for (KeyStroke key : List.of(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
					KeyStroke.getKeyStroke(KeyEvent.VK_X, menuMask),
					KeyStroke.getKeyStroke(KeyEvent.VK_C, menuMask),
					KeyStroke.getKeyStroke(KeyEvent.VK_V, menuMask))) {
				Object command = fixture.root().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(key);
				assertNotNull(command, () -> "dialog root pane must bind " + key);
				assertNotNull(fixture.root().getActionMap().get(command), () -> "bound action for " + key);
			}
		});
	}

	/** Delete removes the selected interval row of the dialog table and marks the project unsaved. */
	@Test
	void deleteKeyRemovesTheSelectedDialogRow() throws Exception {
		Fixture fixture = fixture();
		SwingUtilities.invokeAndWait(() -> {
			fixture.sheet().setRowSelectionInterval(1, 1);
			pressDelete(fixture.root());
			assertEquals(1, fixture.table().getList().size(), "keyboard Delete must remove the second interval");
			assertEquals(2, fixture.sheet().getRowCount(), "the table keeps one data row and the void row");
		});
		assertTrue(fixture.project().needsSaving(), "a keyboard row removal must prompt to save");
	}

	/** The first interval cannot be removed by the keyboard either, and nothing else changes. */
	@Test
	void deleteKeyCannotRemoveTheFirstDialogRow() throws Exception {
		Fixture fixture = fixture();
		AtomicInteger rowsBefore = new AtomicInteger();
		SwingUtilities.invokeAndWait(() -> {
			rowsBefore.set(fixture.sheet().getRowCount());
			fixture.sheet().setRowSelectionInterval(0, 0);
			int dataBefore = fixture.table().getList().size();
			pressDelete(fixture.root());
			assertEquals(dataBefore, fixture.table().getList().size(), "the first interval must stay in the data");
			assertEquals(rowsBefore.get(), fixture.sheet().getRowCount(), "the first interval must stay in the table");
		});
		assertFalse(fixture.project().needsSaving(), "a refused removal must not prompt to save");
		assertEquals(List.of(Messages.getString("ValueObjectForIntervalTable.YouCannotRemoveTheFirst")), errors,
				"the refusal must be reported once with the existing message");
	}

	private static void pressDelete(JRootPane root) {
		Object command = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.get(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
		Action action = command == null ? null : root.getActionMap().get(command);
		assertNotNull(action, "the dialog must bind the Delete key");
		action.actionPerformed(new ActionEvent(root, ActionEvent.ACTION_PERFORMED, "Delete"));
	}
}

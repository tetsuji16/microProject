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
package com.microproject.ui.dialog.assignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.configuration.Dictionary;
import com.microproject.ui.menu.MenuActionConstants;
import com.microproject.ui.view.spreadsheet.SpreadSheet;
import com.microproject.ui.view.spreadsheet.SpreadSheetPopupMenu;
import com.microproject.model.resource.Resource;
import com.microproject.model.resource.ResourcePool;
import com.microproject.model.task.Project;
import com.microproject.undo.DataFactoryUndoController;

/**
 * Issue #997: the resource tables shown in the assignment dialogs are not
 * {@code InformationDialog}s, so the dialog key route never reached them and the
 * assignment entry override hid its configured {@code Copy} command. The
 * assignment entry table must keep exactly its configured command, and both
 * assignment dialogs must own the editing keys for the table they show.
 */
class AssignmentDialogTableRouteTest {
	/** The assignment entry table offers the Copy command its configuration declares. */
	@Test
	void assignmentEntryPopupOffersItsConfiguredCopyCommand() throws Exception {
		Fixture fixture = assignmentFixture();
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = fixture.getSpreadSheet();
			assertEquals(List.of(MenuActionConstants.ACTION_COPY), List.of(sheet.getActionList()),
					"the assignment entry table must use ActionList.assignmentEntry");
			assertNotNull(sheet.getPopup(), "a table with a row command must open a row popup");
			assertEquals(List.of("popup." + MenuActionConstants.ACTION_COPY), names(sheet.getPopup()),
					"the assignment entry popup offers its configured commands only");
			assertNotNull(sheet.getAction(MenuActionConstants.ACTION_COPY),
					"the configured Copy command must have a live action");
			assertNull(sheet.getAction(MenuActionConstants.ACTION_DELETE),
					"the assignment entry table declares no Delete");
			assertNull(sheet.getAction(MenuActionConstants.ACTION_CUT),
					"the assignment entry table declares no Cut");
			assertFalse(sheet.performAction(MenuActionConstants.ACTION_DELETE, null),
					"Delete must not run without a configured command");
			assertFalse(sheet.performAction(MenuActionConstants.ACTION_CUT, null),
					"Cut must not run without a configured command");
		});
	}

	/** Both assignment dialogs export their root pane, so the shared table can install the dialog keys there. */
	@Test
	void assignmentAndTimesheetDialogsBindTheEditingKeys() throws Exception {
		Fixture assignment = assignmentFixture();
		Fixture timesheet = timesheetFixture();
		SwingUtilities.invokeAndWait(() -> {
			assertEditingKeysBound(assignment.root());
			assertEditingKeysBound(timesheet.root());
		});
	}

	/** The timesheet table declares no row commands, so it opens no popup and its keys stay inert. */
	@Test
	void timesheetTableKeepsNoRowCommandsAndNoPopup() throws Exception {
		Fixture fixture = timesheetFixture();
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = fixture.getSpreadSheet();
			assertEquals(0, sheet.getActionList().length, "the timesheet table declares no row commands");
			assertNull(sheet.getPopup(), "a table without row commands must not open a popup");
			assertNull(sheet.getAction(MenuActionConstants.ACTION_DELETE));
			assertFalse(sheet.performAction(MenuActionConstants.ACTION_DELETE, null),
					"the timesheet table's Delete key must stay inert");
			assertFalse(sheet.performAction(MenuActionConstants.ACTION_CUT, null),
					"the timesheet table's Cut key must stay inert");
		});
	}

	private static void assertEditingKeysBound(JRootPane root) {
		for (KeyStroke key : editingKeys()) {
			Object command = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(key);
			assertNotNull(command, () -> "dialog root pane must bind " + key);
			Action action = root.getActionMap().get(command);
			assertNotNull(action, () -> "bound action for " + key);
		}
	}

	private static List<KeyStroke> editingKeys() {
		int shortcutMask;
		try {
			shortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
		} catch (HeadlessException e) {
			shortcutMask = InputEvent.CTRL_DOWN_MASK;
		}
		return List.of(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
				KeyStroke.getKeyStroke(KeyEvent.VK_X, shortcutMask),
				KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcutMask),
				KeyStroke.getKeyStroke(KeyEvent.VK_V, shortcutMask));
	}

	private static Fixture assignmentFixture() throws Exception {
		Project project = project("assignment-entry-route");
		Fixture[] built = new Fixture[1];
		SwingUtilities.invokeAndWait(() -> {
			FakeDialog dialog = new FakeDialog();
			AssignmentEntryPane pane = new AssignmentEntryPane(dialog, null, null, false, null);
			pane.setProject(project);
			built[0] = new Fixture(pane, dialog.getRootPane());
		});
		return built[0];
	}

	private static Fixture timesheetFixture() throws Exception {
		Project project = project("timesheet-entry-route");
		Resource resource = project.getResourcePool().createScriptedResource();
		resource.setName("Timesheet route resource");
		Fixture[] built = new Fixture[1];
		SwingUtilities.invokeAndWait(() -> {
			FakeDialog dialog = new FakeDialog();
			TimesheetEntryPane pane = new TimesheetEntryPane(dialog, project);
			pane.setSelectedResources(List.of(resource));
			built[0] = new Fixture(pane, dialog.getRootPane());
		});
		return built[0];
	}

	private static Project project(String name) {
		Dictionary.getInstance();
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool(name, undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		pool.addProject(project);
		pool.setLocal(true);
		return project;
	}

	private static List<String> names(SpreadSheetPopupMenu popup) {
		List<String> names = new ArrayList<>();
		for (java.awt.Component component : popup.getComponents())
			if (component instanceof JMenuItem item && item.getName() != null
					&& item.getName().startsWith("popup."))
				names.add(item.getName());
		return names;
	}

	private record Fixture(JScrollPane pane, JRootPane root) {
		private SpreadSheet getSpreadSheet() {
			if (pane instanceof AssignmentEntryPane assignment)
				return assignment.getSpreadSheet();
			return ((TimesheetEntryPane) pane).getSpreadSheet();
		}
	}

	private static final class FakeDialog implements CommonAssignmentDialog {
		private final JRootPane root = new JRootPane();

		@Override
		public void setEditorButtonsVisible(boolean visible) {
		}

		@Override
		public JRootPane getRootPane() {
			return root;
		}
	}
}

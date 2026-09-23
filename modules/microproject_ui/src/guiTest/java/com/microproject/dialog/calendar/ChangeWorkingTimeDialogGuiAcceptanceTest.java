/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog.calendar;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Files;
import java.lang.reflect.Field;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JScrollPane;
import javax.swing.text.JTextComponent;
import javax.swing.SwingUtilities;
import javax.swing.JTabbedPane;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.dialog.AbstractDialog;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.calendar.CalendarService;
import com.microproject.pm.calendar.DayDescriptor;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.DialogLayoutAssertions;
import com.microproject.strings.Messages;
import com.microproject.exchange.LocalFileImporter;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.undo.DataFactoryUndoController;

/** GUI-NC-07: open and cancel the working-time dialog through real mouse input. */
class ChangeWorkingTimeDialogGuiAcceptanceTest {
	private JFrame frame;
	private ChangeWorkingTimeDialogBox dialog;
	private NewBaseCalendarDialog newBaseDialog;

	@AfterEach
	void closeWindows() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Window window : Window.getWindows())
				if (window instanceof ChangeWorkingTimeDialogBox || window instanceof NewBaseCalendarDialog) window.dispose();
			if (frame != null) frame.dispose();
		});
	}

	@Test
	void robotNewBaseCalendarDialogKeepsRadioLabelsInsideTheirLayoutCells() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("New calendar layout acceptance");
			frame.setSize(420, 240);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
			new GraphicManager(frame).getMenuManager();
			newBaseDialog = NewBaseCalendarDialog.getInstance(frame, null);
			SwingUtilities.invokeLater(newBaseDialog::doModal);
		});
		GuiAcceptanceSupport.await(() -> newBaseDialog != null && newBaseDialog.isVisible(), "new base calendar dialog did not open");
		assertTrue(newBaseDialog.createNewBase.getWidth() >= newBaseDialog.createNewBase.getPreferredSize().width,
				() -> "new-calendar radio label must not be clipped: actual=" + newBaseDialog.createNewBase.getWidth()
						+ " preferred=" + newBaseDialog.createNewBase.getPreferredSize().width);
		assertTrue(newBaseDialog.makeACopy.getWidth() >= newBaseDialog.makeACopy.getPreferredSize().width,
				() -> "copy-calendar radio label must not be clipped: actual=" + newBaseDialog.makeACopy.getWidth()
						+ " preferred=" + newBaseDialog.makeACopy.getPreferredSize().width);
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		click(robot, cancelButton(newBaseDialog));
		GuiAcceptanceSupport.await(() -> !newBaseDialog.isVisible(), "Cancel did not close new base calendar dialog");
	}

	@Test
	void robotOpensWorkingTimeDialogAndCancelsWithoutCommit() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("gui-working-time", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		WorkingCalendar calendar = WorkingCalendar.getStandardBasedInstance();
		calendar.setName("Visual Exception Calendar");
		project.setWorkCalendar(calendar);
		SwingUtilities.invokeAndWait(() -> {
			frame = new MainRibbonFrame("Working time GUI acceptance", null, null);
			frame.setPreferredSize(new Dimension(1000, 650));
			GraphicManager manager = new GraphicManager(frame);
			((MainRibbonFrame) frame).setGraphicManager(manager);
			manager.initView();
			manager.addProjectFrame(project);
			frame.pack();
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
			dialog = ChangeWorkingTimeDialogBox.getInstance(frame, project, calendar, null, false, undo);
			dialog.projectCalendars.add(calendar);
			SwingUtilities.invokeLater(dialog::doModal);
		});
		GuiAcceptanceSupport.await(() -> dialog != null && dialog.isVisible(), "working-time dialog did not open");
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(dialog, "Change Working Time dialog (#590 body image 1)");
		assertVisibleComponentsFit(dialog, "Change Working Time Calendar tab");
		Robot robot = new Robot();
		robot.setAutoDelay(50);
		clickTab(robot, dialog.calendarTabs, 1);
		assertVisibleComponentsFit(dialog, "Change Working Time Work Weeks tab");
		click(robot, findButton(dialog.workWeekEditor, "workWeekAddButton"));
		GuiAcceptanceSupport.await(() -> visibleChildDialog() != null, "Work Week Details visual editor did not open");
		JDialog workWeekDetails = visibleChildDialog();
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(workWeekDetails, "Work Week Details visual matrix");
		assertVisibleComponentsFit(workWeekDetails, "Work Week Details visual matrix");
		pressEscape(robot);
		GuiAcceptanceSupport.await(() -> visibleChildDialog() == null, "Escape did not close Work Week Details");
		clickTab(robot, dialog.calendarTabs, 2);
		GuiAcceptanceSupport.await(() -> dialog.calendarTabs.getSelectedIndex() == 2, "Exceptions tab did not become active");
		assertVisibleComponentsFit(dialog, "Change Working Time Exceptions tab");
		click(robot, findButton(dialog.exceptionEditor, "calendarExceptionAddButton"));
		GuiAcceptanceSupport.await(() -> visibleChildDialog() != null, "Add Exception editor did not open");
		JDialog addEditor = visibleChildDialog();
		JTextComponent exceptionName = findNamedText(addEditor, "calendarExceptionName");
		clickComponent(robot, exceptionName);
		for (int key : new int[] { KeyEvent.VK_L, KeyEvent.VK_A, KeyEvent.VK_Y, KeyEvent.VK_O, KeyEvent.VK_U, KeyEvent.VK_T }) {
			robot.keyPress(key); robot.keyRelease(key);
		}
		click(robot, buttonNamed(addEditor, Messages.getString("ButtonText.OK")));
		GuiAcceptanceSupport.await(() -> visibleChildDialog() == null, "Add Exception editor did not close");
		JList<?> exceptions = findNamedList(dialog.exceptionEditor, "calendarExceptionsList");
		Point exceptionCell = new Point();
		SwingUtilities.invokeAndWait(() -> {
			Rectangle cell = exceptions.getCellBounds(0, 0);
			Point location = exceptions.getLocationOnScreen();
			exceptionCell.setLocation(location.x + cell.x + cell.width / 2, location.y + cell.y + cell.height / 2);
		});
		robot.mouseMove(exceptionCell.x, exceptionCell.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK); robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		click(robot, findButton(dialog.exceptionEditor, "calendarExceptionDetailsButton"));
		GuiAcceptanceSupport.await(() -> visibleChildDialog() != null, "Exception Details editor did not open");
		JDialog exceptionDetails = visibleChildDialog();
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(exceptionDetails, "Exception Details visual matrix");
		assertVisibleComponentsFit(exceptionDetails, "Exception Details visual matrix");
		pressEscape(robot);
		GuiAcceptanceSupport.await(() -> visibleChildDialog() == null, "Escape did not close Exception Details");
		AbstractButton cancel = cancelButton(dialog);
		click(robot, cancel);
		GuiAcceptanceSupport.await(() -> !dialog.isVisible(), "Cancel did not close working-time dialog");
		assertFalse(dialog.isCalendarCommitted(), "Cancel must not commit calendar changes");
		assertTrue(frame.isVisible(), "Cancel must return to the project window");
	}

	@Test
	void robotAddsWorkWeekOnScratchAndCommitsAsOneUndoableEdit() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("gui-work-week", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		long projectStart = com.microproject.util.DateTime.dayFloor(System.currentTimeMillis());
		project.setStart(projectStart);
		com.microproject.pm.task.NormalTask task = (com.microproject.pm.task.NormalTask) project.createLocalTaskNode(null).getImpl();
		task.setName("Work-week persistence task");
		task.getCurrentSchedule().setStart(projectStart);
		task.setDuration(8L * 60L * 60L * 1000L);
		project.recalculate();
		WorkingCalendar calendar = WorkingCalendar.getStandardBasedInstance();
		calendar.setName("GUI Work Week Calendar");
		project.setWorkCalendar(calendar);
		SwingUtilities.invokeAndWait(() -> {
			frame = new MainRibbonFrame("Work week editor acceptance", null, null);
			GraphicManager manager = new GraphicManager(frame);
			((MainRibbonFrame) frame).setGraphicManager(manager);
			manager.initView();
			manager.addProjectFrame(project);
			frame.setSize(new Dimension(1000, 650));
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
			frame.toFront();
			frame.requestFocus();
			dialog = ChangeWorkingTimeDialogBox.getInstance(frame, project, calendar, null, false, undo);
			dialog.projectCalendars.add(calendar);
			SwingUtilities.invokeLater(dialog::doModal);
		});
		GuiAcceptanceSupport.await(() -> dialog != null && dialog.isVisible(), "working-time dialog did not open");
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		JTabbedPane tabs = dialog.calendarTabs;
		clickTab(robot, tabs, 1);
		GuiAcceptanceSupport.await(() -> tabs.getSelectedIndex() == 1, "Work Weeks tab did not become active");
		click(robot, findButton(dialog.workWeekEditor, "workWeekAddButton"));
		GuiAcceptanceSupport.await(() -> visibleChildDialog() != null, "Work Week Details editor did not open");
		JDialog details = visibleChildDialog();
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(details, "Work Week Details editor");
		assertVisibleComponentsFit(details, "Work Week Details editor");
		JTextComponent name = findNamedText(details, "workWeekName");
		clickComponent(robot, name);
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		for (int key : new int[] {KeyEvent.VK_S, KeyEvent.VK_U, KeyEvent.VK_M, KeyEvent.VK_M, KeyEvent.VK_E, KeyEvent.VK_R}) {
			robot.keyPress(key);
			robot.keyRelease(key);
		}
		click(robot, buttonNamed(details, Messages.getString("ButtonText.OK")));
		GuiAcceptanceSupport.await(() -> visibleChildDialog() == null, "Work Week Details did not close after OK");
		assertEquals(1, calendarWorkWeeks(dialog).size(), "detail edits affect only the parent dialog's scratch calendar");
		click(robot, okButton(dialog));
		GuiAcceptanceSupport.await(() -> !dialog.isVisible(), "parent OK did not close Change Working Time");
		assertTrue(dialog.isCalendarCommitted());
		assertEquals(1, calendar.getWorkWeekPeriods().size(), "parent OK commits the dated weekday pattern");
		var committedWeek = calendar.getWorkWeekPeriods().getFirst();
		assertEquals("summer", committedWeek.getName());
		for (int day = 0; day < 7; day++)
			assertEquals(day > 0 && day < 6, committedWeek.getWeekDay(day).isWorking(),
				"the seven day-level work/nonwork choices must commit independently");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_Z);
		robot.keyRelease(KeyEvent.VK_Z);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> calendar.getWorkWeekPeriods().isEmpty(),
			"physical Ctrl+Z did not undo the committed work week");
		assertTrue(calendar.getWorkWeekPeriods().isEmpty(), "one Undo restores the pre-dialog calendar");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> calendar.getWorkWeekPeriods().size() == 1,
			"physical Ctrl+Y did not redo the committed work week");
		assertEquals(1, calendar.getWorkWeekPeriods().size(), "one Redo reapplies the work week");
		File pod = Files.createTempFile("gui-work-week-roundtrip-", ".pod").toFile();
		try {
			LocalFileImporter exporter = new LocalFileImporter();
			exporter.setFileName(pod.getAbsolutePath());
			exporter.setProject(project);
			exporter.exportFile();
			LocalFileImporter importer = new LocalFileImporter();
			importer.setFileName(pod.getAbsolutePath());
			importer.setProjectFactory(ProjectFactory.getInstance());
			importer.importFile();
			assertEquals("summer", ((WorkingCalendar) importer.getProject().getWorkCalendar())
				.getWorkWeekPeriods().getFirst().getName(), "native POD reload must preserve the edited work week");
		} finally {
			Files.deleteIfExists(pod.toPath());
		}
	}

	@Test
	void robotAddsRecurringExceptionCommitsUndoesRedoesAndPersists() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("gui-calendar-exception", undo);
		pool.setLocal(true);
		Project project = Project.createProject(pool, undo);
		project.setMaster(true);
		project.initialize(false, false);
		WorkingCalendar projectCalendar = WorkingCalendar.getStandardBasedInstance();
		projectCalendar.setName("GUI Project Exception Calendar");
		project.setWorkCalendar(projectCalendar);
		com.microproject.pm.task.NormalTask task = (com.microproject.pm.task.NormalTask) project
			.createLocalTaskNode(null).getImpl();
		task.setName("Task-specific calendar fixture");
		WorkingCalendar taskCalendar = WorkingCalendar.getInstanceBasedOn(projectCalendar);
		taskCalendar.setName("GUI Task Exception Calendar");
		task.setTaskCalendar(taskCalendar);
		Resource resource = pool.createScriptedResource();
		resource.setName("Recurring exception resource");
		WorkingCalendar calendar = WorkingCalendar.getInstanceBasedOn(projectCalendar);
		calendar.setName("GUI Resource Exception Calendar");
		resource.setWorkCalendar(calendar);
		SwingUtilities.invokeAndWait(() -> {
			frame = new MainRibbonFrame("Calendar exception acceptance", null, null);
			GraphicManager manager = new GraphicManager(frame);
			((MainRibbonFrame) frame).setGraphicManager(manager);
			manager.initView();
			manager.addProjectFrame(project);
			frame.setSize(new Dimension(1000, 650));
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
			frame.toFront();
			frame.requestFocus();
			dialog = ChangeWorkingTimeDialogBox.getInstance(frame, project, calendar, pool.extractCalendars(), false, undo);
			dialog.projectCalendars.add(projectCalendar);
			dialog.projectCalendars.add(taskCalendar);
			SwingUtilities.invokeLater(dialog::doModal);
		});
		GuiAcceptanceSupport.await(() -> dialog != null && dialog.isVisible(), "working-time dialog did not open");
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		long selectedDate = selectDisplayedWorkingDate(robot, calendar);
		clickTab(robot, dialog.calendarTabs, 2);
		GuiAcceptanceSupport.await(() -> dialog.calendarTabs.getSelectedIndex() == 2, "Exceptions tab did not become active");
		assertVisibleComponentsFit(dialog, "Change Working Time Exceptions tab");
		click(robot, findButton(dialog.exceptionEditor, "calendarExceptionAddButton"));
		JDialog addDialog = visibleChildDialog();
		GuiAcceptanceSupport.await(() -> visibleChildDialog() != null, "Add Exception editor did not open");
		addDialog = visibleChildDialog();
		JSpinner start = findNamedSpinner(addDialog, "calendarExceptionStart");
		JSpinner finish = findNamedSpinner(addDialog, "calendarExceptionFinish");
		assertEquals(selectedDate, com.microproject.util.DateTime.dayFloor(((java.util.Date) start.getValue()).getTime()),
			"Add Exception must initialize Start to the date selected on the calendar");
		assertEquals(selectedDate, com.microproject.util.DateTime.dayFloor(((java.util.Date) finish.getValue()).getTime()),
			"Add Exception must initialize Finish to the date selected on the calendar");
		JTextComponent name = findNamedText(addDialog, "calendarExceptionName");
		clickComponent(robot, name);
		for (int key : new int[] { KeyEvent.VK_H, KeyEvent.VK_O, KeyEvent.VK_L, KeyEvent.VK_I, KeyEvent.VK_D, KeyEvent.VK_A, KeyEvent.VK_Y }) {
			robot.keyPress(key); robot.keyRelease(key);
		}
		click(robot, buttonNamed(addDialog, Messages.getString("ButtonText.OK")));
		GuiAcceptanceSupport.await(() -> visibleChildDialog() == null, "Add Exception editor did not close");
		JList<?> list = findNamedList(dialog.exceptionEditor, "calendarExceptionsList");
		Point listPoint = new Point();
		SwingUtilities.invokeAndWait(() -> {
			Rectangle cell = list.getCellBounds(0, 0);
			Point location = list.getLocationOnScreen();
			listPoint.setLocation(location.x + cell.x + cell.width / 2, location.y + cell.y + cell.height / 2);
		});
		robot.mouseMove(listPoint.x, listPoint.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK); robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		click(robot, findButton(dialog.exceptionEditor, "calendarExceptionDetailsButton"));
		GuiAcceptanceSupport.await(() -> visibleChildDialog() != null, "Exception Details editor did not open");
		JDialog details = visibleChildDialog();
		assertVisibleComponentsFit(details, "Exception Details editor");
		JCheckBox recurring = findNamedCheckBox(details, "calendarExceptionRecurring");
		showInScrollPane(recurring);
		click(robot, recurring);
		click(robot, buttonNamed(details, Messages.getString("ButtonText.OK")));
		GuiAcceptanceSupport.await(() -> visibleChildDialog() == null, "Exception Details editor did not close");
		assertEquals(1, dialog.getScratchCalendar().getRecurringExceptions().size(), "recurrence edit is staged until parent OK");
		assertTrue(calendar.getRecurringExceptions().isEmpty(), "the live calendar must remain unchanged before parent OK");
		selectCalendarWithRobot(robot, dialog, projectCalendar);
		click(robot, findButton(dialog.exceptionEditor, "calendarExceptionAddButton"));
		GuiAcceptanceSupport.await(() -> visibleChildDialog() != null, "project calendar Add Exception editor did not open");
		JDialog projectAddDialog = visibleChildDialog();
		JTextComponent projectExceptionName = findNamedText(projectAddDialog, "calendarExceptionName");
		clickComponent(robot, projectExceptionName);
		for (int key : new int[] { KeyEvent.VK_P, KeyEvent.VK_R, KeyEvent.VK_O, KeyEvent.VK_J, KeyEvent.VK_E, KeyEvent.VK_C, KeyEvent.VK_T }) {
			robot.keyPress(key); robot.keyRelease(key);
		}
		click(robot, buttonNamed(projectAddDialog, Messages.getString("ButtonText.OK")));
		GuiAcceptanceSupport.await(() -> visibleChildDialog() == null, "project calendar exception Add editor did not close");
		assertTrue(projectCalendar.getExceptionDays().length == 0, "project-calendar edits must remain staged before parent OK");
		selectCalendarWithRobot(robot, dialog, taskCalendar);
		click(robot, findButton(dialog.exceptionEditor, "calendarExceptionAddButton"));
		GuiAcceptanceSupport.await(() -> visibleChildDialog() != null, "task calendar Add Exception editor did not open");
		JDialog taskAddDialog = visibleChildDialog();
		JTextComponent taskExceptionName = findNamedText(taskAddDialog, "calendarExceptionName");
		clickComponent(robot, taskExceptionName);
		for (int key : new int[] { KeyEvent.VK_T, KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_K }) {
			robot.keyPress(key); robot.keyRelease(key);
		}
		click(robot, buttonNamed(taskAddDialog, Messages.getString("ButtonText.OK")));
		GuiAcceptanceSupport.await(() -> visibleChildDialog() == null, "task calendar exception Add editor did not close");
		assertTrue(taskCalendar.getExceptionDays().length == 0, "task-calendar edits must remain staged before parent OK");
		click(robot, okButton(dialog));
		GuiAcceptanceSupport.await(() -> !dialog.isVisible(), "parent OK did not close Change Working Time");
		assertTrue(dialog.isCalendarCommitted());
		assertTrue(projectCalendar.getExceptionDays().length > 0, "parent OK must commit the project calendar edit");
		assertTrue(taskCalendar.getExceptionDays().length > 0, "parent OK must commit the task calendar edit");
		var rule = calendar.getRecurringExceptions().getFirst().getRecurrence();
		assertEquals(10, rule.occurrenceDates().size(), "the default recurrence count must be represented by ten daily occurrences");
		assertTrue(calendar.getExceptionDays().length == 0, "generated recurrence dates must not be flattened into one-off exceptions");
		robot.keyPress(KeyEvent.VK_CONTROL); robot.keyPress(KeyEvent.VK_Z);
		robot.keyRelease(KeyEvent.VK_Z); robot.keyRelease(KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> calendar.getRecurringExceptions().isEmpty()
			&& projectCalendar.getExceptionDays().length == 0 && taskCalendar.getExceptionDays().length == 0,
			"one physical Ctrl+Z must undo all staged calendar edits");
		robot.keyPress(KeyEvent.VK_CONTROL); robot.keyPress(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_Y); robot.keyRelease(KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> calendar.getRecurringExceptions().size() == 1
			&& projectCalendar.getExceptionDays().length > 0 && taskCalendar.getExceptionDays().length > 0,
			"one physical Ctrl+Y must redo all staged calendar edits");
		File pod = Files.createTempFile("gui-calendar-exception-", ".pod").toFile();
		try {
			LocalFileImporter exporter = new LocalFileImporter();
			exporter.setFileName(pod.getAbsolutePath()); exporter.setProject(project); exporter.exportFile();
			LocalFileImporter importer = new LocalFileImporter();
			importer.setFileName(pod.getAbsolutePath()); importer.setProjectFactory(ProjectFactory.getInstance()); importer.importFile();
			Resource loadedResource = importer.getProject().getResourcePool().getResourceList().stream()
				.filter(candidate -> "Recurring exception resource".equals(candidate.getName())).findFirst().orElseThrow();
			WorkingCalendar loaded = (WorkingCalendar) loadedResource.getWorkCalendar();
			assertEquals(rule.occurrenceDates(), loaded.getRecurringExceptions().getFirst().getRecurrence().occurrenceDates(),
				"POD save/reload must preserve the recurrence rule without flattening");
			WorkingCalendar loadedProjectCalendar = (WorkingCalendar) importer.getProject().getWorkCalendar();
			assertTrue(loadedProjectCalendar.getExceptionDays().length > 0,
				"POD save/reload must preserve the project-calendar edit alongside the resource calendar edit");
			var loadedTask = (com.microproject.pm.task.NormalTask) importer.getProject().getTaskOutlineIterator().next();
			WorkingCalendar loadedTaskCalendar = (WorkingCalendar) loadedTask.getTaskCalendar();
			assertTrue(loadedTaskCalendar.getExceptionDays().length > 0,
				"POD save/reload must preserve the task-specific calendar edit");
		} finally { Files.deleteIfExists(pod.toPath()); }
	}

	private static void assertVisibleComponentsFit(JDialog value, String surface) throws Exception {
		final String[] violation = new String[1];
		SwingUtilities.invokeAndWait(() -> {
			Point dialogLocation = value.getLocationOnScreen();
			Rectangle dialogBounds = new Rectangle(dialogLocation.x, dialogLocation.y,
					value.getWidth(), value.getHeight());
			for (Component component : allComponents(value)) {
				if (!isLayoutProbe(component) || !component.isShowing() || component == value)
					continue;
				Rectangle bounds = component.getBounds();
				if (bounds.width <= 0 || bounds.height <= 0)
					continue;
				Point location = component.getLocationOnScreen();
				Rectangle screenBounds = new Rectangle(location.x, location.y, bounds.width, bounds.height);
				if (!dialogBounds.contains(new Point(screenBounds.x, screenBounds.y))
						|| !dialogBounds.contains(new Point(screenBounds.x + screenBounds.width - 1,
							screenBounds.y + screenBounds.height - 1))) {
					violation[0] = component.getClass().getSimpleName() + " bounds=" + screenBounds
							+ " dialog=" + dialogBounds;
						return;
				}
				for (Container parent = component.getParent(); parent != null && parent != value; parent = parent.getParent()) {
					if (parent instanceof JScrollPane)
						break;
					Point parentLocation = parent.getLocationOnScreen();
					Rectangle parentBounds = new Rectangle(parentLocation.x, parentLocation.y,
							parent.getWidth(), parent.getHeight());
					if (!parentBounds.contains(new Point(screenBounds.x, screenBounds.y))
							|| !parentBounds.contains(new Point(screenBounds.x + screenBounds.width - 1,
									screenBounds.y + screenBounds.height - 1))) {
						violation[0] = component.getClass().getSimpleName() + " escapes parent "
								+ parent.getClass().getSimpleName() + " bounds=" + screenBounds
								+ " parent=" + parentBounds;
						return;
					}
				}
			}
			for (Component parentComponent : allComponents(value)) {
				if (!(parentComponent instanceof Container parent) || parent instanceof JScrollPane)
					continue;
				java.util.List<Component> probes = java.util.Arrays.stream(parent.getComponents())
					.filter(ChangeWorkingTimeDialogGuiAcceptanceTest::isLayoutProbe)
					.filter(Component::isShowing).toList();
				for (int first = 0; first < probes.size(); first++) {
					for (int second = first + 1; second < probes.size(); second++) {
						Rectangle a = probes.get(first).getBounds();
						Rectangle b = probes.get(second).getBounds();
						if (a.intersects(b)) {
							violation[0] = parent.getClass().getSimpleName() + " children overlap: "
									+ probes.get(first).getClass().getSimpleName() + "=" + a + " and "
									+ probes.get(second).getClass().getSimpleName() + "=" + b;
							return;
						}
					}
				}
			}
		});
		assertTrue(violation[0] == null, () -> surface + " has an out-of-bounds or overlapping control: " + violation[0]);
	}

	private static boolean isLayoutProbe(Component component) {
		return component instanceof JLabel || component instanceof AbstractButton
				|| component instanceof JTextComponent || component instanceof JComboBox
				|| component instanceof JSpinner;
	}

	private static java.util.List<Component> allComponents(Container root) {
		java.util.ArrayList<Component> result = new java.util.ArrayList<>();
		for (Component child : root.getComponents()) {
			result.add(child);
			if (child instanceof Container container)
				result.addAll(allComponents(container));
		}
		return result;
	}

	@Test
	void robotSelectsDisplayedDateMarksNonWorkingAndCommits() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("gui-working-time-save", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		WorkingCalendar calendar = WorkingCalendar.getStandardBasedInstance();
		calendar.setName("GUI Save Calendar");
		project.setWorkCalendar(calendar);
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("Working time GUI save acceptance");
			frame.setPreferredSize(new Dimension(900, 420));
			frame.pack();
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
			new GraphicManager(frame).getMenuManager();
			dialog = ChangeWorkingTimeDialogBox.getInstance(frame, project, calendar, null, false, undo);
			dialog.projectCalendars.add(calendar);
			SwingUtilities.invokeLater(dialog::doModal);
		});
		GuiAcceptanceSupport.await(() -> dialog != null && dialog.isVisible(), "working-time dialog did not open");
		Robot robot = new Robot();
		robot.setAutoDelay(50);
		long selectedDate = selectDisplayedWorkingDate(robot, calendar);
		assertTrue(dialog.nonWorking.isEnabled(), "Non-working radio button must be enabled for an editable project calendar");
		click(robot, dialog.nonWorking);
		robot.delay(200);
		click(robot, okButton(dialog));
		GuiAcceptanceSupport.await(() -> !dialog.isVisible(), "OK did not close working-time dialog");
		assertTrue(dialog.isCalendarCommitted(), "OK must commit calendar changes");
		DayDescriptor changed = CalendarService.getInstance().getDay(calendar, selectedDate);
		assertFalse(changed.isWorking(), "Selected displayed date must be persisted as non-working");
	}

	@Test
	void switchingCalendarsDoesNotCommitEditsBeforeParentOk() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("gui-working-time-staged", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		WorkingCalendar first = WorkingCalendar.getStandardBasedInstance();
		first.setName("Staged first calendar");
		project.setWorkCalendar(first);
		WorkingCalendar second = WorkingCalendar.getStandardBasedInstance();
		second.setName("Staged second calendar");
		CalendarService service = CalendarService.getInstance();
		long[] editedDate = new long[1];
		DayDescriptor[] original = new DayDescriptor[1];
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("Working time staged edit acceptance");
			frame.setPreferredSize(new Dimension(1000, 600));
			frame.pack();
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
			new GraphicManager(frame).getMenuManager();
			dialog = ChangeWorkingTimeDialogBox.getInstance(frame, project, first, null, false, undo);
			dialog.projectCalendars.add(first);
			dialog.projectCalendars.add(second);
			SwingUtilities.invokeLater(dialog::doModal);
		});
		GuiAcceptanceSupport.await(() -> dialog != null && dialog.isVisible(), "working-time dialog did not open");
		Robot robot = new Robot();
		robot.setAutoDelay(45);
		editedDate[0] = selectDisplayedWorkingDate(robot, first);
		original[0] = service.getDay(first, editedDate[0]);
		click(robot, dialog.nonWorking);
		SwingUtilities.invokeAndWait(() -> dialog.calendarType.setSelectedItem(second));
		assertEquals(original[0].isWorking(), service.getDay(first, editedDate[0]).isWorking(),
			"switching the calendar must keep the first calendar edit in the dialog's scratch state");
		click(robot, cancelButton(dialog));
		GuiAcceptanceSupport.await(() -> !dialog.isVisible(), "Cancel did not close working-time dialog");
		assertEquals(original[0].isWorking(), service.getDay(first, editedDate[0]).isWorking(),
			"Cancel must discard edits made before switching calendars");
		assertFalse(dialog.isCalendarCommitted(), "Cancel must not commit any staged calendar");
	}

	private long selectDisplayedWorkingDate(Robot robot, WorkingCalendar calendar) throws Exception {
		CalendarService service = CalendarService.getInstance();
		Point[] point = new Point[1];
		long[] date = new long[1];
		SwingUtilities.invokeAndWait(() -> {
			for (int y = 0; y < dialog.sdCalendar.getHeight() && point[0] == null; y += 2) {
				for (int x = 0; x < dialog.sdCalendar.getWidth(); x += 2) {
					long candidate = dialog.sdCalendar.getDayAt(x, y);
					if (candidate > 0 && service.getDay(calendar, candidate).isWorking()) {
						Point location = dialog.sdCalendar.getLocationOnScreen();
						point[0] = new Point(location.x + x + 1, location.y + y + 1);
						date[0] = candidate;
						break;
					}
				}
			}
		});
		assertTrue(point[0] != null, "A working displayed date cell must be discoverable");
		robot.mouseMove(point[0].x, point[0].y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> !dialog.sdCalendar.getSelectedFixedIntervals().isEmpty(), "Date cell selection did not register");
		return date[0];
	}

	private static void selectCalendarWithRobot(Robot robot, ChangeWorkingTimeDialogBox value,
		WorkingCalendar calendar) throws Exception {
		int[] targetIndex = new int[1];
		SwingUtilities.invokeAndWait(() -> {
			targetIndex[0] = -1;
			for (int index = 0; index < value.calendarType.getItemCount(); index++) {
				if (value.calendarType.getItemAt(index) == calendar) {
					targetIndex[0] = index;
					break;
				}
			}
		});
		assertTrue(targetIndex[0] >= 0, "target calendar must be present in For calendar");
		clickComponent(robot, value.calendarType);
		robot.keyPress(KeyEvent.VK_HOME);
		robot.keyRelease(KeyEvent.VK_HOME);
		for (int index = 0; index < targetIndex[0]; index++) {
			robot.keyPress(KeyEvent.VK_DOWN);
			robot.keyRelease(KeyEvent.VK_DOWN);
		}
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> value.calendarType.getSelectedItem() == calendar,
			"physical For calendar selection did not choose the project calendar");
	}

	private static AbstractButton cancelButton(ChangeWorkingTimeDialogBox value) throws Exception {
		Field field = AbstractDialog.class.getDeclaredField("cancel");
		field.setAccessible(true);
		return (AbstractButton) field.get(value);
	}

	private static AbstractButton cancelButton(NewBaseCalendarDialog value) throws Exception {
		Field field = AbstractDialog.class.getDeclaredField("cancel");
		field.setAccessible(true);
		return (AbstractButton) field.get(value);
	}

	private static AbstractButton okButton(ChangeWorkingTimeDialogBox value) throws Exception {
		Field field = AbstractDialog.class.getDeclaredField("ok");
		field.setAccessible(true);
		return (AbstractButton) field.get(value);
	}

	private static void click(Robot robot, AbstractButton button) throws Exception {
		Point[] center = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			Point location = button.getLocationOnScreen();
			center[0] = new Point(location.x + button.getWidth() / 2, location.y + button.getHeight() / 2);
		});
		robot.mouseMove(center[0].x, center[0].y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	private static void clickComponent(Robot robot, Component component) throws Exception {
		Point[] center = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			Point location = component.getLocationOnScreen();
			center[0] = new Point(location.x + component.getWidth() / 2, location.y + component.getHeight() / 2);
		});
		robot.mouseMove(center[0].x, center[0].y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	private static void pressEscape(Robot robot) {
		robot.keyPress(KeyEvent.VK_ESCAPE);
		robot.keyRelease(KeyEvent.VK_ESCAPE);
	}

	private static void clickTab(Robot robot, JTabbedPane tabs, int index) throws Exception {
		Point[] center = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			Rectangle bounds = tabs.getBoundsAt(index);
			Point location = tabs.getLocationOnScreen();
			center[0] = new Point(location.x + bounds.x + bounds.width / 2, location.y + bounds.y + bounds.height / 2);
		});
		robot.mouseMove(center[0].x, center[0].y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	private static AbstractButton findButton(Container root, String name) {
		for (Component component : allComponents(root))
			if (component instanceof AbstractButton button && name.equals(button.getName())) return button;
		throw new AssertionError("button not found: " + name);
	}

	private static AbstractButton buttonNamed(Container root, String text) {
		for (Component component : allComponents(root))
			if (component instanceof AbstractButton button && text.equals(button.getText())) return button;
		throw new AssertionError("button not found: " + text);
	}

	private static JTextComponent findNamedText(Container root, String name) {
		for (Component component : allComponents(root))
			if (component instanceof JTextComponent text && name.equals(text.getName())) return text;
		throw new AssertionError("text field not found: " + name);
	}

	private static JSpinner findNamedSpinner(Container root, String name) {
		for (Component component : allComponents(root))
			if (component instanceof JSpinner spinner && name.equals(spinner.getName())) return spinner;
		throw new AssertionError("spinner not found: " + name);
	}

	private static JCheckBox findNamedCheckBox(Container root, String name) {
		for (Component component : allComponents(root))
			if (component instanceof JCheckBox checkbox && name.equals(checkbox.getName())) return checkbox;
		throw new AssertionError("checkbox not found: " + name);
	}

	private static JList<?> findNamedList(Container root, String name) {
		for (Component component : allComponents(root))
			if (component instanceof JList<?> list && name.equals(list.getName())) return list;
		throw new AssertionError("list not found: " + name);
	}

	private static void showInScrollPane(Component component) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Container parent = component.getParent(); parent != null; parent = parent.getParent())
				if (parent instanceof JScrollPane scroll) {
					Component view = scroll.getViewport().getView();
					if (view instanceof javax.swing.JComponent swingView) {
						Rectangle bounds = SwingUtilities.convertRectangle(component.getParent(), component.getBounds(), view);
						swingView.scrollRectToVisible(bounds);
					}
					return;
				}
		});
	}

	private JDialog visibleChildDialog() {
		for (Window window : Window.getWindows())
			if (window instanceof JDialog child && child.isVisible() && child != dialog) return child;
		return null;
	}

	private static java.util.List<com.microproject.pm.calendar.WorkWeekPeriod> calendarWorkWeeks(
			ChangeWorkingTimeDialogBox value) {
		return value.getScratchCalendar().getWorkWeekPeriods();
	}
}

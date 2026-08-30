/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog.calendar;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.lang.reflect.Field;

import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.dialog.AbstractDialog;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.calendar.CalendarService;
import com.microproject.pm.calendar.DayDescriptor;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;

/** GUI-NC-07: open and cancel the working-time dialog through real mouse input. */
class ChangeWorkingTimeDialogGuiAcceptanceTest {
	private JFrame frame;
	private ChangeWorkingTimeDialogBox dialog;

	@AfterEach
	void closeWindows() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Window window : Window.getWindows())
				if (window instanceof ChangeWorkingTimeDialogBox) window.dispose();
			if (frame != null) frame.dispose();
		});
	}

	@Test
	void robotOpensWorkingTimeDialogAndCancelsWithoutCommit() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("gui-working-time", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		WorkingCalendar calendar = (WorkingCalendar) project.getWorkCalendar();
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("Working time GUI acceptance");
			frame.setPreferredSize(new Dimension(900, 420));
			frame.pack();
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
			new GraphicManager(frame).getMenuManager();
			dialog = ChangeWorkingTimeDialogBox.getInstance(frame, project, calendar, null, false, undo);
			SwingUtilities.invokeLater(dialog::doModal);
		});
		GuiAcceptanceSupport.await(() -> dialog != null && dialog.isVisible(), "working-time dialog did not open");
		Robot robot = new Robot();
		robot.setAutoDelay(50);
		AbstractButton cancel = cancelButton(dialog);
		click(robot, cancel);
		GuiAcceptanceSupport.await(() -> !dialog.isVisible(), "Cancel did not close working-time dialog");
		assertFalse(dialog.isCalendarCommitted(), "Cancel must not commit calendar changes");
		assertTrue(frame.isVisible(), "Cancel must return to the project window");
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

	private static AbstractButton cancelButton(ChangeWorkingTimeDialogBox value) throws Exception {
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
}

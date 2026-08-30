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

	private static AbstractButton cancelButton(ChangeWorkingTimeDialogBox value) throws Exception {
		Field field = AbstractDialog.class.getDeclaredField("cancel");
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

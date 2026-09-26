/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.field.Field;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.pm.graphic.views.SearchContext;
import com.microproject.pm.graphic.views.Searchable;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.strings.Messages;
import com.microproject.testsupport.DialogLayoutAssertions;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.configuration.Configuration;

/** Verifies the real Find buttons' theme sizing, icons, and physical directions (#717). */
class FindDialogButtonsGuiAcceptanceTest {
	private MainRibbonFrame owner;
	private GraphicManager manager;
	private FindDialog dialog;

	@AfterEach
	void closeWindows() throws Exception {
		if (dialog != null) SwingUtilities.invokeAndWait(dialog::dispose);
		if (manager != null) SwingUtilities.invokeAndWait(manager::cleanUp);
		if (owner != null) SwingUtilities.invokeAndWait(owner::dispose);
	}

	@Test
	void directionButtonsRemainReadableAndNavigateInOppositeDirections() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Find button acceptance coverage.");
		Field nameField = Configuration.getFieldFromId("Field.name");
		boolean[] lastDirection = { true };
		Searchable searchable = new Searchable() {
			@Override public boolean findNext(SearchContext context) {
				lastDirection[0] = context.isForward();
				return true;
			}
			@Override public SearchContext createSearchContext() { return new SearchContext() { }; }
			@Override public List<Field> getAvailableFields() { return List.of(nameField); }
		};
		SwingUtilities.invokeAndWait(() -> {
			owner = new MainRibbonFrame("Find dialog button acceptance", null, null);
			manager = new GraphicManager(owner);
			owner.setGraphicManager(manager);
			owner.setSize(900, 600);
			owner.setLocationByPlatform(true);
			owner.setVisible(true);
			DataFactoryUndoController undo = new DataFactoryUndoController();
			Project project = Project.createProject(ResourcePool.createRourcePool("Find button pool", undo), undo);
			DocumentFrame frame = new DocumentFrame(manager, project, "find-buttons-gui");
			dialog = FindDialog.getInstance(frame, searchable, nameField);
			dialog.pack();
			dialog.setLocationRelativeTo(owner);
			dialog.setVisible(true);
		});
		GuiAcceptanceSupport.await(() -> dialog.isShowing(), "Find dialog did not open");

		assertNotNull(dialog.next.getIcon(), "Find Next must retain its down-direction icon");
		assertNotNull(dialog.previous.getIcon(), "Find Previous must retain its up-direction icon");
		assertTrue(dialog.next.getIcon().getIconWidth() > 0 && dialog.next.getIcon().getIconHeight() > 0,
			"Find Next icon must have drawable dimensions");
		assertEquals(dialog.next.getIcon().getIconWidth(), dialog.previous.getIcon().getIconWidth(),
			"direction icons must use a consistent size");
		assertEquals(dialog.next.getPreferredSize().height, dialog.previous.getPreferredSize().height,
			"direction buttons must share one preferred height");
		assertEquals(dialog.next.getFont(), dialog.previous.getFont(), "direction buttons must share the dialog font");
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(dialog.getContentPane(), "Find dialog (#717)");
		DialogLayoutAssertions.assertWithinUsableScreen(dialog, "Find dialog (#724)");
		DialogLayoutAssertions.assertResizeKeepsTextControls(dialog, dialog.getContentPane(), 80, 30,
			"Find dialog (#724)");

		Robot robot = new Robot();
		robot.setAutoDelay(35);
		click(robot, dialog.search);
		robot.keyPress(KeyEvent.VK_X);
		robot.keyRelease(KeyEvent.VK_X);
		GuiAcceptanceSupport.await(() -> dialog.next.isEnabled() && dialog.previous.isEnabled(),
			"Find buttons must enable for a non-empty query");
		click(robot, dialog.next);
		assertTrue(lastDirection[0], "Find Next physical route must search forward");
		click(robot, dialog.previous);
		assertTrue(!lastDirection[0], "Find Previous physical route must search backward");
	}

	private static void click(Robot robot, java.awt.Component component) throws Exception {
		java.awt.Point point = component.getLocationOnScreen();
		robot.mouseMove(point.x + component.getWidth() / 2, point.y + component.getHeight() / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}
}

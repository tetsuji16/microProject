/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;

import javax.swing.AbstractButton;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import com.jgoodies.forms.layout.FormLayout;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.DialogLayoutAssertions;
import com.microproject.undo.DataFactoryUndoController;

/** Physical layout regression for Project > Information. */
class ProjectInformationDialogGuiAcceptanceTest {
	private ProjectInformationDialog dialog;
	private boolean previousStandalone;

	@AfterEach
	void closeDialog() throws Exception {
		if (dialog != null) SwingUtilities.invokeAndWait(dialog::dispose);
		com.microproject.util.Environment.setStandAlone(previousStandalone);
	}

	@Test
	void projectInformationShowsAllTabsAndButtonsAfterResize() throws Exception {
		previousStandalone = com.microproject.util.Environment.getStandAlone();
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for layout acceptance coverage.");
		DataFactoryUndoController undo = new DataFactoryUndoController();
		com.microproject.util.Environment.setStandAlone(false);
		ResourcePool pool = ResourcePool.createRourcePool("project-information-gui", undo);
		pool.setLocal(true);
		Project project = Project.createProject(pool, undo);
		project.setName("Project information GUI");
		// Move Project is available only for forward-scheduled projects; keep the
		// fixture's physical route preconditions explicit.
		project.setForward(true);
		dialog = ProjectInformationDialog.getInstance(null, project);
		SwingUtilities.invokeLater(() -> { dialog.pack(); dialog.setVisible(true); });
		GuiAcceptanceSupport.await(() -> dialog.isShowing(), "Project information dialog did not open");
		DialogLayoutAssertions.assertWithinUsableScreen(dialog, "Project Information dialog (#724)");
		GuiAcceptanceSupport.await(dialog::isActive, "Project information dialog did not become active");
		SwingUtilities.invokeAndWait(() -> { dialog.setAlwaysOnTop(true); dialog.toFront(); dialog.requestFocus(); });
		JTabbedPane tabs = findTabs(dialog);
		assertTrue(tabs != null && tabs.getTabCount() == 3,
			"Project information must show General, Statistics, and Notes tabs");
		FormLayout generalLayout = findFormLayout((java.awt.Container) tabs.getComponentAt(0));
		assertTrue(generalLayout != null && generalLayout.getRowCount() >= 26,
			"General form must provide row 25 for custom fields after the access-control row");
		for (int index = 0; index < tabs.getTabCount(); index++) {
			final int tabIndex = index;
			SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(tabIndex));
			DialogLayoutAssertions.assertTextControlsAtPreferredHeight(tabs,
				"Project Information tab " + tabs.getTitleAt(index) + " (#714)");
		}
		SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(0));
		assertTrue(findButton(dialog, "Close") != null || findButton(dialog, "閉じる") != null,
			"Project information must expose a visible close button");
		AbstractButton move = findButton(dialog, "Move Project...");
		if (move == null) move = findButton(dialog, "プロジェクトの移動...");
		assertTrue(move != null, "Project information must expose the MSP Move Project route");

		Robot robot = new Robot();
		robot.setAutoDelay(30);
		robot.waitForIdle();
		java.awt.Point location = move.getLocationOnScreen();
		java.awt.Point local = new java.awt.Point(location);
		SwingUtilities.convertPointFromScreen(local, dialog);
		assertTrue(SwingUtilities.getDeepestComponentAt(dialog, local.x + move.getWidth() / 2, local.y + move.getHeight() / 2) == move,
			"Move Project button must be the physical hit target");
		robot.mouseMove(location.x + move.getWidth() / 2, location.y + move.getHeight() / 2);
		robot.delay(250);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> visibleDialog(MoveProjectDialog.class) != null,
			"Move Project dialog did not open from Project Information");
		SwingUtilities.invokeAndWait(() -> visibleDialog(MoveProjectDialog.class).dispose());
		DialogLayoutAssertions.assertResizeKeepsTextControls(dialog, tabs, 180, 80,
			"Project Information dialog (#724)");
		assertTrue(tabs.isShowing() && tabs.getWidth() > 0 && tabs.getHeight() > 0,
			"Project information content must remain visible after resize");
	}

	private static <T extends Window> T visibleDialog(Class<T> type) {
		for (Window window : Window.getWindows()) if (type.isInstance(window) && window.isShowing()) return type.cast(window);
		return null;
	}

	private static JTabbedPane findTabs(java.awt.Container root) {
		for (Component child : root.getComponents()) {
			if (child instanceof JTabbedPane tabs) return tabs;
			if (child instanceof java.awt.Container nested) {
				JTabbedPane tabs = findTabs(nested);
				if (tabs != null) return tabs;
			}
		}
		return null;
	}

	private static FormLayout findFormLayout(java.awt.Container root) {
		if (root.getLayout() instanceof FormLayout layout) return layout;
		for (Component child : root.getComponents()) {
			if (child instanceof java.awt.Container nested) {
				FormLayout layout = findFormLayout(nested);
				if (layout != null) return layout;
			}
		}
		return null;
	}

	private static AbstractButton findButton(java.awt.Container root, String text) {
		for (Component child : root.getComponents()) {
			if (child instanceof AbstractButton button && text.equals(button.getText()) && button.isShowing()) return button;
			if (child instanceof java.awt.Container nested) {
				AbstractButton button = findButton(nested, text);
				if (button != null) return button;
			}
		}
		return null;
	}
}

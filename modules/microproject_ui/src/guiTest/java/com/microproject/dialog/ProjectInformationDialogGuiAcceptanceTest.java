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
import com.microproject.testsupport.DialogScreenshotArtifacts;
import com.microproject.pm.graphic.frames.ApplicationStartupFactory;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.ui.ribbon.RibbonDisplayMode;
import com.microproject.ui.ribbon.RibbonDisplayPreferences;
import com.microproject.util.Environment;
import com.microproject.undo.DataFactoryUndoController;

/** Physical layout regression for Project > Information. */
class ProjectInformationDialogGuiAcceptanceTest {
	private ProjectInformationDialog dialog;
	private MainRibbonFrame window;
	private GraphicManager manager;
	private boolean previousStandalone;
	private boolean previousRibbonUi;
	private boolean previousNewLook;
	private boolean previousClientSide;
	private RibbonDisplayMode previousRibbonDisplayMode;

	@AfterEach
	void closeDialog() throws Exception {
		if (dialog != null) SwingUtilities.invokeAndWait(dialog::dispose);
		if (manager != null) SwingUtilities.invokeAndWait(manager::cleanUp);
		if (window != null) SwingUtilities.invokeAndWait(window::dispose);
		Environment.setStandAlone(previousStandalone);
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
		Environment.setClientSide(previousClientSide);
		if (previousRibbonDisplayMode != null) RibbonDisplayPreferences.save(previousRibbonDisplayMode);
	}

	@Test
	void projectInformationShowsAllTabsAndButtonsAfterResize() throws Exception {
		previousStandalone = com.microproject.util.Environment.getStandAlone();
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousClientSide = Environment.isClientSide();
		previousRibbonDisplayMode = RibbonDisplayPreferences.load();
		RibbonDisplayPreferences.save(RibbonDisplayMode.ALWAYS_SHOW);
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for layout acceptance coverage.");
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Environment.setStandAlone(true);
		Environment.setClientSide(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("microProject — Project Information layout acceptance", null);
			manager = new ApplicationStartupFactory(new java.util.HashMap<>()).instanceFromNewSession(window, false);
			window.setGraphicManager(manager);
			window.setAlwaysOnTop(true);
			if (!window.isShowing()) {
				window.setSize(1200, 700);
				window.setLocationByPlatform(true);
				window.setAlwaysOnTop(true);
				window.setVisible(true);
			}
		});
		GuiAcceptanceSupport.await(() -> window.isShowing(), "application ribbon window did not become visible");
		ResourcePool pool = ResourcePool.createRourcePool("project-information-gui", undo);
		pool.setLocal(true);
		Project project = Project.createProject(pool, undo);
		project.setName("Project information GUI");
		// Move Project is available only for forward-scheduled projects; keep the
		// fixture's physical route preconditions explicit.
		project.setForward(true);
		SwingUtilities.invokeAndWait(() -> manager.addProjectFrame(project));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
			&& manager.getCurrentFrame().getProject() == project, "Project Information fixture was not activated");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		robot.waitForIdle();
		click(robot, findTextButton(window.getRibbonPanel(), "File", "ファイル"));
		GuiAcceptanceSupport.await(() -> namedComponent(window.getRootPane(), "officeBackstageNav-info") != null,
			"File tab did not expose the Project Info destination");
		click(robot, (Component) namedComponent(window.getRootPane(), "officeBackstageNav-info"));
		GuiAcceptanceSupport.await(() -> namedComponent(window.getRootPane(), "officeBackstageCommand-RibbonBackstageProjectInformation") != null,
			"Project Info destination did not expose its command");
		click(robot, findCommandButton(window.getRootPane(), "RibbonBackstageProjectInformation"));
		GuiAcceptanceSupport.await(() -> visibleDialog(ProjectInformationDialog.class) != null,
			"Project > Info route did not open Project Information");
		dialog = visibleDialog(ProjectInformationDialog.class);
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
		DialogScreenshotArtifacts.capture(dialog, "issue-460-project-information");
		assertTrue(findButton(dialog, "Close") != null || findButton(dialog, "閉じる") != null,
			"Project information must expose a visible close button");
		AbstractButton move = findButton(dialog, "Move Project...");
		if (move == null) move = findButton(dialog, "プロジェクトの移動...");
		assertTrue(move != null, "Project information must expose the MSP Move Project route");

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

	private static AbstractButton findCommandButton(Component root, String command) {
		AbstractButton found = findCommandButtonOrNull(root, command);
		if (found == null) throw new AssertionError("Ribbon command is not physically available: " + command);
		return found;
	}

	private static AbstractButton findCommandButtonOrNull(Component root, String command) {
		if (root instanceof AbstractButton button && command.equals(button.getActionCommand()) && button.isShowing()) return button;
		if (root instanceof java.awt.Container container) for (Component child : container.getComponents()) {
			AbstractButton found = findCommandButtonOrNull(child, command);
			if (found != null) return found;
		}
		return null;
	}

	private static Component namedComponent(Component root, String name) {
		if (name.equals(root.getName())) return root;
		if (root instanceof java.awt.Container container) {
			for (Component child : container.getComponents()) {
				Component found = namedComponent(child, name);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static AbstractButton findTextButton(Component root, String... texts) {
		AbstractButton found = findTextButtonOrNull(root, texts);
		if (found == null) throw new AssertionError("Visible ribbon tab is missing: " + java.util.Arrays.toString(texts));
		return found;
	}

	private static AbstractButton findTextButtonOrNull(Component root, String... texts) {
		if (root instanceof AbstractButton button) {
			for (String text : texts) if (text.equals(button.getText()) && button.isShowing()) return button;
		}
		if (root instanceof java.awt.Container container) {
			for (Component child : container.getComponents()) {
				AbstractButton found = findTextButtonOrNull(child, texts);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static void click(Robot robot, Component target) throws Exception {
		java.awt.Point point = target.getLocationOnScreen();
		robot.mouseMove(point.x + target.getWidth() / 2, point.y + target.getHeight() / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}
}

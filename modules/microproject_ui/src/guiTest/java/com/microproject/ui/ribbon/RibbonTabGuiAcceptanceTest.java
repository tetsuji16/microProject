/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pushingpixels.flamingo.api.common.AbstractCommandButton;
import org.pushingpixels.flamingo.api.ribbon.JRibbon;

import com.microproject.menu.MenuManager;
import com.microproject.menu.ProjectMenuActionMap;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.util.Environment;

/** Physical GUI checks for the live Flamingo JRibbon path. */
class RibbonTabGuiAcceptanceTest {
	private JFrame frame;
	private boolean previousRibbonUi;
	private boolean previousNewLook;

	@BeforeEach void configureRibbonEnvironment() {
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
	}

	@AfterEach void closeWindow() throws Exception {
		if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
	}

	@Test
	void robotSelectsNativeTaskAndClicksCanonicalFlamingoCommandOnce() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		RecordingActionMap actions = new RecordingActionMap();
		MenuManager manager = MenuManager.getInstance(actions);
		JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
		JRibbon ribbon = findRibbon(host);
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("Flamingo ribbon acceptance");
			frame.add(host);
			frame.setSize(1440, 420);
			frame.setLocation(40, 40);
			frame.setVisible(true);
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(frame::isActive, "ribbon test window did not become active");

		Robot robot = new Robot(frame.getGraphicsConfiguration().getDevice());
		robot.setAutoDelay(35);
		var bundle = MenuDefinitionSupport.menuBundle(Locale.getDefault());
		AbstractCommandButton taskTab = findTab(host, bundle.getString("TaskRibbonTask.title"));
		java.util.concurrent.atomic.AtomicInteger physicalPresses = new java.util.concurrent.atomic.AtomicInteger();
		taskTab.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override public void mousePressed(java.awt.event.MouseEvent event) { physicalPresses.incrementAndGet(); }
		});
		int tabClickAttempts = 0;
		while (tabClickAttempts < 3 && !isTaskSelected(ribbon, taskTab.getText())) {
			if (tabClickAttempts > 0) frame.toFront();
			click(robot, taskTab);
			tabClickAttempts++;
			robot.delay(200);
		}
		GuiAcceptanceSupport.await(() -> ribbon.getSelectedTask() != null
			&& ribbon.getSelectedTask().getTitle().equals(taskTab.getText()),
			"physical task tab click did not select the Flamingo task after " + tabClickAttempts + " attempt(s); selected="
				+ (ribbon.getSelectedTask() == null ? "<none>" : ribbon.getSelectedTask().getTitle())
				+ ", clicked=" + taskTab.getText() + ", showing=" + taskTab.isShowing()
				+ ", enabled=" + taskTab.isEnabled() + ", bounds=" + taskTab.getBounds()
				+ ", screen=" + taskTab.getLocationOnScreen()
				+ ", receivedMousePresses=" + physicalPresses.get());
		assertTrue(physicalPresses.get() > 0, "Robot must physically reach the native Flamingo task tab");

		AbstractCommandButton save = findCommand(host, "RibbonSaveProject");
		String actionId = manager.getToolBarFactory().getActionStringFromId("RibbonSaveProject");
		assertNotNull(actionId);
		GuiAcceptanceSupport.await(save::isShowing, "native command button is not visible in the selected task");
		int before = actions.count(actionId);
		click(robot, save);
		GuiAcceptanceSupport.await(() -> actions.count(actionId) == before + 1,
			"physical JRibbon command click did not dispatch exactly once: " + actionId);
	}

	@Test
	void contextualTaskVisibilityAndTitlesAreOwnedByJRibbon() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		MenuManager manager = MenuManager.getInstance(new RecordingActionMap());
		JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
		JRibbon ribbon = findRibbon(host);
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("Flamingo contextual ribbon acceptance");
			frame.add(host);
			frame.setSize(1200, 420);
			frame.setLocationByPlatform(true);
			frame.setVisible(true);
		});
		RibbonController controller = (RibbonController) host.getClientProperty(RibbonController.CONTEXTUAL_TABS_PROPERTY);
		SwingUtilities.invokeAndWait(() -> {
			controller.setVisibleContextualTabs(Set.of("FormatRibbonTask"));
			controller.setContextualTabTitles(Map.of("FormatRibbonTask", "Gantt Chart Format"));
		});
		GuiAcceptanceSupport.await(() -> contextualGroupVisible(ribbon), "Flamingo contextual task group did not become visible");
		assertTrue(contextualGroupHasTitle(ribbon, "Gantt Chart Format"));
	}

	private static boolean contextualGroupHasTitle(JRibbon ribbon, String title) {
		for (int index = 0; index < ribbon.getContextualTaskGroupCount(); index++)
			if (title.equals(ribbon.getContextualTaskGroup(index).getTitle())) return true;
		return false;
	}

	private static boolean contextualGroupVisible(JRibbon ribbon) {
		for (int index = 0; index < ribbon.getContextualTaskGroupCount(); index++)
			if (ribbon.getContextualTaskGroup(index).getTitle().equals("Gantt Chart Format")
				&& ribbon.isVisible(ribbon.getContextualTaskGroup(index))) return true;
		return false;
	}

	private static boolean isTaskSelected(JRibbon ribbon, String title) {
		return ribbon.getSelectedTask() != null && title.equals(ribbon.getSelectedTask().getTitle());
	}

	private static JRibbon findRibbon(JComponent root) {
		return UiComponentWalker.flatten(root).stream().filter(JRibbon.class::isInstance)
			.map(JRibbon.class::cast).findFirst().orElseThrow();
	}

	private static AbstractCommandButton findTab(JComponent root, String title) {
		return UiComponentWalker.flatten(root).stream().filter(AbstractCommandButton.class::isInstance)
			.map(AbstractCommandButton.class::cast).filter(button -> title.equals(button.getText())).findFirst().orElseThrow();
	}

	private static AbstractCommandButton findCommand(JComponent root, String name) {
		return UiComponentWalker.flatten(root).stream().filter(AbstractCommandButton.class::isInstance)
			.map(AbstractCommandButton.class::cast).filter(button -> name.equals(button.getName())).findFirst().orElseThrow();
	}

	private static void click(Robot robot, java.awt.Component component) throws Exception {
		robot.waitForIdle();
		Point point = component.getLocationOnScreen();
		int x = point.x + component.getWidth() / 2;
		int y = point.y + component.getHeight() / 2;
		robot.mouseMove(x, y);
		robot.waitForIdle();
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private static final class RecordingActionMap implements ProjectMenuActionMap {
		private final Map<String, Action> actions = new HashMap<>();
		private final Map<String, Integer> counts = new HashMap<>();
		@Override public Action getAction(String key) {
			return actions.computeIfAbsent(key, id -> new AbstractAction(id) {
				@Override public void actionPerformed(java.awt.event.ActionEvent event) { counts.merge(id, 1, Integer::sum); }
			});
		}
		@Override public String getStringFromAction(Action action) {
			Object name = action.getValue(Action.NAME);
			return name == null ? "" : name.toString();
		}
		int count(String actionId) { return counts.getOrDefault(actionId, 0); }
	}
}

/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.ProjectMenuActionMap;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.util.Environment;
import com.microproject.util.FlatUiSupport;

/** Non-headless coverage for a real mouse click on a responsive ribbon tab. */
class RibbonTabGuiAcceptanceTest {
	private JFrame frame;
	private boolean previousRibbonUi;
	private boolean previousNewLook;

	@BeforeEach
	void configureRibbonEnvironment() {
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
	}

	@AfterEach
	void closeWindow() throws Exception {
		if (frame != null) {
			SwingUtilities.invokeAndWait(() -> {
				frame.dispose();
				frame = null;
			});
		}
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
	}

	@Test
	void mouseClickSelectsEveryRibbonTabExactlyOnceAndKeepsTheCommandSurfaceVisible() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
		ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
		ribbon.setVisibleContextualTabs(Set.of("FormatRibbonTask"));
		List<AbstractButton> tabs = new ArrayList<>();
		for (String tabId : MenuDefinitionSupport.ribbonTaskIds()) {
			String title = MenuDefinitionSupport.menuBundle(Locale.getDefault()).getString(tabId + ".title");
			tabs.add(findButton(host, title));
		}
		show(host);

		Robot robot = new Robot();
		robot.setAutoDelay(40);
		for (int index = 0; index < tabs.size(); index++) {
			AbstractButton tab = tabs.get(index);
			click(robot, tab);
			GuiAcceptanceSupport.await(tab::isSelected, "Robot click did not select ribbon tab " + tab.getText());
			captureVisibleRibbon(robot, "ribbon-tab-" + index + ".png");

			SwingUtilities.invokeAndWait(() -> {
				assertEquals(1, tabs.stream().filter(AbstractButton::isSelected).count(), "exactly one ribbon tab must be selected");
				assertTrue(tab.isSelected());
				assertEquals(FlatUiSupport.tabSelectedForeground(), tab.getForeground());
				assertTrue(host.isShowing() && host.getWidth() > 900 && host.getHeight() > 100 && host.getHeight() < 250,
					"ribbon command surface height is invalid after selecting " + tab.getText());
			});
		}
	}

	@Test
	void robotClicksEveryStandardRibbonCommandOnce() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Assumptions.assumeTrue(uiScale() <= 1.0d,
			"Direct command sweep requires a full-width desktop; high-DPI layout is covered by the dedicated visual matrix.");
		RecordingActionMap actions = new RecordingActionMap();
		MenuManager manager = MenuManager.getInstance(actions);
		JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
		ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
		ribbon.setVisibleContextualTabs(Set.of("FormatRibbonTask"));
		// Keep the fixture above the compact breakpoint so every command is a
		// direct hit target; the overflow path is covered separately by the
		// responsive ribbon tests.
		show(host, 1600, true);

		Robot robot = new Robot();
		robot.setAutoDelay(35);
		for (String tabId : MenuDefinitionSupport.ribbonTaskIds()) {
			String title = MenuDefinitionSupport.menuBundle(Locale.getDefault()).getString(tabId + ".title");
			AbstractButton tab = findButton(host, title);
			click(robot, tab);
			GuiAcceptanceSupport.await(tab::isSelected, "Robot click did not select ribbon tab " + title);
			// The selection model and the command-panel replacement are separate EDT
			// listeners; drain the queue before looking up the newly attached buttons.
			SwingUtilities.invokeAndWait(() -> { });
			for (String bandId : MenuDefinitionSupport.ribbonBandIds(tabId)) {
				for (String buttonId : MenuDefinitionSupport.ribbonButtonIds(bandId)) {
					AbstractButton button = findAttachedButtonByCommand(host, buttonId);
					assertTrue(button.isShowing(), () -> buttonId + " is not visible in " + tabId);
					assertTrue(button.isEnabled(), () -> buttonId + " is disabled in " + bandId);
					String actionId = manager.getToolBarFactory().getActionStringFromId(buttonId);
					int before = actions.count(actionId);
					Point clickPoint = clickCommand(robot, button);
					GuiAcceptanceSupport.await(() -> actions.count(actionId) == before + 1,
						"Robot click did not dispatch " + buttonId + " (" + actionId + ") at " + clickPoint
							+ " bounds=" + button.getBounds() + " screen=" + safeScreenBounds(button));
				}
			}
		}
	}

	@Test
	void narrowRibbonExposesCollapsedCommandsThroughMousePopup() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		RecordingActionMap actions = new RecordingActionMap();
		MenuManager manager = MenuManager.getInstance(actions);
		JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
		ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
		ribbon.setVisibleContextualTabs(Set.of("FormatRibbonTask"));
		show(host, 900, true);

		Robot robot = new Robot();
		robot.setAutoDelay(35);
		AbstractButton tab = findButton(host, MenuDefinitionSupport.menuBundle(Locale.getDefault())
				.getString("TaskRibbonTask.title"));
		click(robot, tab);
		GuiAcceptanceSupport.await(tab::isSelected, "Task ribbon tab was not selected at narrow width");
		SwingUtilities.invokeAndWait(() -> { });

		AbstractButton overflow = UiComponentWalker.flatten(host).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(AbstractButton::isShowing)
			.filter(button -> button.getClientProperty(ModernRibbonPanel.COLLAPSED_POPUP_PROPERTY) instanceof JPopupMenu popup
					&& popup.getComponentCount() > 0)
			.findFirst()
			.orElseThrow(() -> new AssertionError("narrow ribbon did not expose an overflow popup"));
		JPopupMenu popup = (JPopupMenu) overflow.getClientProperty(ModernRibbonPanel.COLLAPSED_POPUP_PROPERTY);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocusInWindow();
		});
		robot.delay(150);
		Point overflowScreen = overflow.getLocationOnScreen();
		Rectangle frameBounds = frame.getBounds();
		assertTrue(new Rectangle(frameBounds.x, frameBounds.y, frameBounds.width, frameBounds.height)
				.contains(overflowScreen.x + overflow.getWidth() / 2, overflowScreen.y + overflow.getHeight() / 2),
			"responsive overflow trigger must remain inside the host window");
		clickCommand(robot, overflow);
		GuiAcceptanceSupport.await(popup::isVisible, "overflow popup did not open by mouse click");
		JMenu bandMenu = UiComponentWalker.flatten(popup).stream()
			.filter(JMenu.class::isInstance).map(JMenu.class::cast)
			.filter(menu -> UiComponentWalker.flatten(menu.getPopupMenu()).stream()
					.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
					.anyMatch(button -> "RibbonHideSelectedTasks".equals(button.getActionCommand())))
			.findFirst()
			.orElseThrow(() -> new AssertionError("collapsed popup did not expose the Hide Selected Tasks band submenu"));
		clickCommand(robot, bandMenu);
		GuiAcceptanceSupport.await(() -> bandMenu.getPopupMenu().isVisible(),
			"collapsed popup band submenu did not open by mouse click");
		AbstractButton hiddenCommand = UiComponentWalker.flatten(bandMenu.getPopupMenu()).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(button -> "RibbonHideSelectedTasks".equals(button.getActionCommand()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("collapsed band popup did not retain RibbonHideSelectedTasks"));
		String actionId = manager.getToolBarFactory().getActionStringFromId(hiddenCommand.getActionCommand());
		int before = actions.count(actionId);
		clickCommand(robot, hiddenCommand);
		GuiAcceptanceSupport.await(() -> actions.count(actionId) == before + 1,
			"collapsed RibbonHideSelectedTasks did not dispatch");
	}

	@Test
	void viewRibbonKeepsTheLargeGanttButtonVisibleAndDispatchesItOnce() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		RecordingActionMap actions = new RecordingActionMap();
		MenuManager manager = MenuManager.getInstance(actions);
		JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
		ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
		ribbon.setVisibleContextualTabs(Set.of("FormatRibbonTask"));
		show(host, 1200, true);

		Robot robot = new Robot();
		robot.setAutoDelay(40);
		String viewTitle = MenuDefinitionSupport.menuBundle(Locale.getDefault()).getString("ViewRibbonTask.title");
		AbstractButton viewTab = findButton(host, viewTitle);
		click(robot, viewTab);
		GuiAcceptanceSupport.await(viewTab::isSelected, "Robot click did not select the View ribbon tab");
		SwingUtilities.invokeAndWait(() -> { });

		AbstractButton gantt = findAttachedButtonByCommand(host, "RibbonGantt");
		SwingUtilities.invokeAndWait(() -> assertContainedInRibbonBand(gantt));
		Rectangle ganttScreenBounds = safeScreenBounds(gantt);
		Rectangle windowBounds = frame.getBounds();
		assertTrue(windowBounds.contains(ganttScreenBounds),
			() -> "RibbonGantt must be fully visible in the Robot window: button=" + ganttScreenBounds + " window=" + windowBounds);
		captureVisibleRibbon(robot, "ribbon-view-gantt-layout.png");

		String actionId = manager.getToolBarFactory().getActionStringFromId("RibbonGantt");
		int before = actions.count(actionId);
		click(robot, gantt);
		GuiAcceptanceSupport.await(() -> actions.count(actionId) == before + 1,
			"Robot click did not dispatch RibbonGantt exactly once");
	}

	@Test
	void fullyCollapsedRibbonKeepsLauncherIconAndCommandsReachable() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
		ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
		ribbon.setVisibleContextualTabs(Set.of("FormatRibbonTask"));
		show(host, 620, true);

		Robot robot = new Robot();
		robot.setAutoDelay(35);
		AbstractButton tab = findButton(host, MenuDefinitionSupport.menuBundle(Locale.getDefault())
			.getString("FileRibbonTask.title"));
		click(robot, tab);
		GuiAcceptanceSupport.await(tab::isSelected, "File ribbon tab was not selected at collapsed width");
		AbstractButton launcher = UiComponentWalker.flatten(host).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(AbstractButton::isShowing)
			.filter(button -> button.getClientProperty(ModernRibbonPanel.COLLAPSED_POPUP_PROPERTY) instanceof JPopupMenu)
			.findFirst().orElseThrow(() -> new AssertionError("fully collapsed ribbon launcher is missing"));
		assertTrue(launcher.getIcon() != null, "fully collapsed ribbon launcher lost its identifying icon");
		JPopupMenu popup = (JPopupMenu)launcher.getClientProperty(ModernRibbonPanel.COLLAPSED_POPUP_PROPERTY);
		assertTrue(popup.getComponentCount() > 0, "fully collapsed ribbon launcher has no commands");
		clickCommand(robot, launcher);
		GuiAcceptanceSupport.await(popup::isVisible, "fully collapsed ribbon launcher did not open by mouse click");
	}

	@Test
	void defaultNarrowDesktopKeepsPrimaryFileCommandsVisible() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
		ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
		ribbon.setVisibleContextualTabs(Set.of("FormatRibbonTask"));
		// 672 logical px is approximately a 1008px physical client area at 150%
		// Windows scaling, matching the reported production screenshot.
		show(host, 672, true);

		Robot robot = new Robot();
		robot.setAutoDelay(35);
		AbstractButton tab = findButton(host, MenuDefinitionSupport.menuBundle(Locale.getDefault())
			.getString("FileRibbonTask.title"));
		click(robot, tab);
		GuiAcceptanceSupport.await(tab::isSelected, "File ribbon tab was not selected at the default narrow desktop width");
		SwingUtilities.invokeAndWait(() -> { });
		assertTrue(UiComponentWalker.flatten(host).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.noneMatch(button -> button.isShowing()
				&& Boolean.TRUE.equals(button.getClientProperty(ModernRibbonPanel.COLLAPSED_TAB_LAUNCHER_PROPERTY))),
			"default narrow desktop must show direct ribbon commands, not a single launcher popup");

		for (String commandId : List.of("RibbonNewProject", "RibbonOpenProject", "RibbonRecentProjects",
			"RibbonImportProject", "RibbonPrint")) {
			AbstractButton command = findAttachedButtonByCommand(host, commandId);
			assertTrue(command.isShowing(), () -> commandId + " must remain a visible primary File command at 672 logical px");
			assertTrue(command.getIcon() != null && command.getIcon().getIconWidth() > 0,
				() -> commandId + " must retain a visible icon at 672 logical px");
		}
		captureVisibleRibbon(robot, "ribbon-default-high-dpi-primary-commands.png", 600);
	}

	private void show(JPanel host) throws Exception {
		show(host, 1200, false);
	}

	private void show(JPanel host, int width, boolean center) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("Ribbon tab GUI acceptance");
			// MainRibbonFrame docks the production shell in BorderLayout.NORTH.  Keep
			// the acceptance fixture identical so the capture reflects the actual
			// ribbon height rather than stretching it through the whole test window.
			frame.add(host, center ? BorderLayout.CENTER : BorderLayout.NORTH);
			if (!center) {
				frame.add(new JPanel(), BorderLayout.CENTER);
			}
			frame.setPreferredSize(new Dimension(width, 360));
			frame.pack();
			frame.setLocationByPlatform(true);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
			frame.toFront();
			frame.requestFocus();
		});
	}

	private static AbstractButton findButton(JPanel host, String text) {
		return UiComponentWalker.flatten(host).stream()
			.filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast)
			.filter(button -> text.equals(button.getText()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("Ribbon tab not found: " + text));
	}

	private static AbstractButton findAttachedButtonByCommand(JPanel host, String command) {
		return UiComponentWalker.flatten(host).stream()
			.filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast)
			.filter(button -> command.equals(button.getActionCommand()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("Ribbon command not found: " + command));
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

	private static Point clickCommand(Robot robot, AbstractButton button) throws Exception {
		Point[] location = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			Point topLeft = button.getLocationOnScreen();
			// Split/dropdown buttons reserve their right edge for the arrow; the
			// left third is the command surface used by a normal mouse click.
			location[0] = new Point(topLeft.x + Math.max(2, button.getWidth() / 3),
				topLeft.y + button.getHeight() / 2);
		});
		robot.mouseMove(location[0].x, location[0].y);
		robot.waitForIdle();
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		return location[0];
	}

	private static double uiScale() {
		try {
			String configured = System.getProperty("sun.java2d.uiScale");
			if (configured != null)
				return Double.parseDouble(configured);
		} catch (NumberFormatException ignored) {
			// Fall through to the active device transform.
		}
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
			.getDefaultConfiguration().getDefaultTransform().getScaleX();
	}

	private static Rectangle safeScreenBounds(AbstractButton button) {
		try {
			Point point = button.getLocationOnScreen();
			return new Rectangle(point.x, point.y, button.getWidth(), button.getHeight());
		} catch (IllegalComponentStateException e) {
			return new Rectangle();
		}
	}

	private static void assertContainedInRibbonBand(AbstractButton button) {
		Component band = findRibbonBand(button);
		Rectangle buttonBounds = SwingUtilities.convertRectangle(button.getParent(), button.getBounds(), band);
		Insets insets = ((JComponent) band).getInsets();
		Rectangle contentBounds = new Rectangle(
			insets.left,
			insets.top,
			band.getWidth() - insets.left - insets.right,
			band.getHeight() - insets.top - insets.bottom);
		assertTrue(contentBounds.contains(buttonBounds),
			() -> "Ribbon button is clipped by its band: button=" + buttonBounds + " content=" + contentBounds);
	}

	private static Component findRibbonBand(Component component) {
		for (Component current = component; current != null; current = current.getParent()) {
			if ("projectLibreRibbonBand".equals(current.getName())) {
				return current;
			}
		}
		throw new AssertionError("Ribbon band not found for " + component);
	}

	private void captureVisibleRibbon(Robot robot, String fileName) throws Exception {
		captureVisibleRibbon(robot, fileName, 900);
	}

	private void captureVisibleRibbon(Robot robot, String fileName, int minimumWidth) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(frame.getRootPane().getLocationOnScreen(), frame.getRootPane().getSize()));
		BufferedImage screenshot = robot.createScreenCapture(bounds[0]);
		Path directory = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		ImageIO.write(screenshot, "png", directory.resolve(fileName).toFile());
		assertTrue(screenshot.getWidth() >= minimumWidth && screenshot.getHeight() > 120,
			"captured ribbon is unexpectedly small: " + screenshot.getWidth() + "x" + screenshot.getHeight());
	}

	private static final class RecordingActionMap implements ProjectMenuActionMap {
		private final Map<String, Integer> counts = new HashMap<>();
		private final Map<String, Action> actions = new HashMap<>();

		@Override
		public Action getAction(String key) {
			return actions.computeIfAbsent(key, actionId -> new AbstractAction(actionId) {
				@Override
				public void actionPerformed(java.awt.event.ActionEvent event) {
					counts.merge(actionId, 1, Integer::sum);
				}
			});
		}

		@Override
		public String getStringFromAction(Action action) {
			Object value = action.getValue(Action.NAME);
			return value == null ? "" : value.toString();
		}

		int count(String actionId) {
			return counts.getOrDefault(actionId, 0);
		}
	}
}

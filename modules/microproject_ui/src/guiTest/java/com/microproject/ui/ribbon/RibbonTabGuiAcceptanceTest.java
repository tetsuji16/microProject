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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.accessibility.AccessibleState;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.MenuElement;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.ProjectMenuActionMap;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.ribbon.CommandId;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.GuiPhysicalRouteAdapter;
import com.microproject.testsupport.RibbonGuiEnvironment;
import com.microproject.ui.shell.MicroProjectShell;
import com.microproject.util.Environment;
import com.microproject.util.FlatUiSupport;

/** Non-headless coverage for a real mouse click on a responsive ribbon tab. */
class RibbonTabGuiAcceptanceTest {
	private JFrame frame;
	private boolean previousRibbonUi;
	private boolean previousNewLook;
	private RibbonDisplayMode previousShellDisplayMode;

	@BeforeEach
	void configureRibbonEnvironment() {
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		RibbonGuiEnvironment.initialize();
	}

	@AfterEach
	void closeWindow() throws Exception {
		if (frame != null) {
			SwingUtilities.invokeAndWait(() -> {
				frame.dispose();
				frame = null;
			});
		}
		if (previousShellDisplayMode != null) {
			RibbonDisplayPreferences.save(previousShellDisplayMode);
			previousShellDisplayMode = null;
		}
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
	}

	@Test
	void rightClickTabCollapsesAndRestoresRibbonCommands() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
		ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
		show(host, 1200, false);
		AbstractButton taskTab = findButton(host,
			MenuDefinitionSupport.menuBundle(Locale.getDefault()).getString("TaskRibbonTask.title"));
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		rightClick(robot, taskTab);
		JPopupMenu firstPopup = awaitDisplayModePopup();
		clickCommand(robot, firstPopupMenuItem(firstPopup));
		GuiAcceptanceSupport.await(() -> ribbon.getRibbonDisplayMode() == RibbonDisplayMode.TABS_ONLY,
			"physical tab popup did not collapse the ribbon");
		assertTrue(taskTab.isShowing(), "tabs-only mode must leave the tab row reachable");
		assertTrue(!ribbon.isCommandSurfaceVisible(), "tabs-only mode left command bands visible");

		rightClick(robot, taskTab);
		JPopupMenu secondPopup = awaitDisplayModePopup();
		clickCommand(robot, firstPopupMenuItem(secondPopup));
		GuiAcceptanceSupport.await(() -> ribbon.getRibbonDisplayMode() == RibbonDisplayMode.ALWAYS_SHOW,
			"physical tab popup did not restore the ribbon");
		assertTrue(ribbon.isCommandSurfaceVisible());

		doubleClick(robot, taskTab);
		GuiAcceptanceSupport.await(() -> ribbon.getRibbonDisplayMode() == RibbonDisplayMode.TABS_ONLY,
			"physical double-click on a tab did not collapse the ribbon");
		assertTrue(taskTab.isShowing());
		assertTrue(!ribbon.isCommandSurfaceVisible());
		captureVisibleRibbon(robot, "ribbon-double-click-collapsed.png");

		doubleClick(robot, taskTab);
		GuiAcceptanceSupport.await(() -> ribbon.getRibbonDisplayMode() == RibbonDisplayMode.ALWAYS_SHOW,
			"physical double-click on a tab did not restore the ribbon");
		assertTrue(ribbon.isCommandSurfaceVisible());
	}

	@Test
	void mouseClickSelectsEveryRibbonTabExactlyOnceAndKeepsTheCommandSurfaceVisible() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
		ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
		ribbon.setVisibleContextualTabs(Set.of("FormatRibbonTask", "NetworkFormatRibbonTask", "CalendarFormatRibbonTask"));
		show(host);
		List<AbstractButton> tabs = new ArrayList<>();
		for (String tabId : MenuDefinitionSupport.ribbonTaskIds()) {
			String title = MenuDefinitionSupport.menuBundle(Locale.getDefault()).getString(tabId + ".title");
			tabs.add(findButton(host, title));
		}
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		for (int index = 0; index < MenuDefinitionSupport.ribbonTaskIds().size(); index++) {
			String tabId = MenuDefinitionSupport.ribbonTaskIds().get(index);
			AbstractButton tab = clickRibbonTab(robot, host, tabId);
			GuiAcceptanceSupport.await(tab::isSelected, "Robot click did not select ribbon tab " + tab.getText());
			captureVisibleRibbon(robot, "ribbon-tab-" + index + ".png");

			SwingUtilities.invokeAndWait(() -> {
				assertEquals(1, tabs.stream().filter(AbstractButton::isSelected).count(), "exactly one ribbon tab must be selected");
				assertTrue(tab.isSelected());
				assertEquals(FlatUiSupport.tabSelectedForeground(), tab.getForeground());
				Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
				Rectangle hostBounds = null;
				if (host.isShowing()) {
					try {
						hostBounds = new Rectangle(host.getLocationOnScreen(), host.getSize());
					} catch (IllegalComponentStateException ignored) {
						// A native host can stop showing between the visibility check and screen lookup.
					}
				}
				assertTrue(frame.isShowing() && usable.contains(frame.getBounds())
						&& host.isShowing() && hostBounds != null && usable.contains(hostBounds)
						&& host.getHeight() > 100 && host.getHeight() < 250,
					"ribbon must fit the usable viewport and retain its command-surface height after selecting "
						+ tab.getText() + ": frame=" + frame.getBounds()
						+ ", hostShowing=" + host.isShowing()
						+ (host.isShowing() ? ", host=" + hostBounds : ", host=not-showing")
						+ ", usable=" + usable + ", height=" + host.getHeight());
			});
		}
	}

	/**
	 * Wiring-only sweep.  Production command semantics are covered separately by
	 * RibbonExternalCommandGuiAcceptanceTest, which uses a real GraphicManager
	 * instead of this recording ActionMap.
	 */
	@Test
	void robotClicksEveryStandardRibbonCommandOnce() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Assumptions.assumeTrue(uiScale() <= 1.0d,
			"Direct command sweep requires a full-width desktop; high-DPI layout is covered by the dedicated visual matrix.");
		RecordingActionMap actions = new RecordingActionMap();
		MenuManager manager = MenuManager.getInstance(actions);
		MainRibbonFrame applicationFrame = new MainRibbonFrame("Ribbon command GUI acceptance", "");
		MicroProjectShell.installRibbonShell(applicationFrame, manager, null);
		frame = applicationFrame;
		JPanel host = applicationFrame.getRibbonPanel();
		ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
		previousShellDisplayMode = ribbon.getRibbonDisplayMode();
		ribbon.setRibbonDisplayMode(RibbonDisplayMode.ALWAYS_SHOW);
		assertTrue(ribbon.isCommandSurfaceVisible(),
			"the production shell fixture must expose command bands for direct physical routing");
		ribbon.setVisibleContextualTabs(Set.of("FormatRibbonTask"));
		// Use the production Office shell so File commands are exercised in their
		// Backstage host, while document commands remain attached to the ribbon.
		// Use the current desktop work area without changing its resolution. Commands
		// that collapse at this width are dispatched through their responsive group
		// proxies below, so the sweep remains complete on smaller CI desktops.
		SwingUtilities.invokeAndWait(() -> {
			Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
			frame.setSize(usable.width, Math.min(700, usable.height));
			frame.setLocation(usable.x, usable.y);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(frame::isShowing, "production ribbon shell did not become visible");

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(35);
		EnumSet<CommandId> physicalTaskCommands = EnumSet.noneOf(CommandId.class);
		for (String tabId : MenuDefinitionSupport.ribbonTaskIds().stream()
				.filter(tabId -> !Set.of("NetworkFormatRibbonTask", "CalendarFormatRibbonTask").contains(tabId))
				.toList()) {
			String title = MenuDefinitionSupport.menuBundle(Locale.getDefault()).getString(tabId + ".title");
			AbstractButton tab = findButton(host, title);
			click(robot, tab);
			GuiAcceptanceSupport.await(tab::isSelected, "Robot click did not select ribbon tab " + title);
			// The selection model and the command-panel replacement are separate EDT
			// listeners; drain the queue before looking up the newly attached buttons.
			SwingUtilities.invokeAndWait(() -> { });
			for (String bandId : MenuDefinitionSupport.ribbonBandIds(tabId)) {
				for (String buttonId : MenuDefinitionSupport.ribbonButtonIds(bandId)) {
					if ("FileRibbonTask".equals(tabId))
						openFileCommandDestination(robot, frame, tab, buttonId);
					try {
						GuiAcceptanceSupport.await(() -> hasPhysicalButtonForCommand(frame, buttonId),
							"Selected ribbon tab " + tabId + " did not expose command or responsive group proxy " + buttonId);
					} catch (AssertionError failure) {
						throw new AssertionError(failure.getMessage() + "; "
							+ ribbonCommandDiagnostic(frame, buttonId), failure);
					}
					AbstractButton button = findAttachedButtonByCommand(frame, buttonId);
					AbstractButton proxy = button == null ? findCollapsedProxyForCommand(frame, buttonId) : null;
					assertTrue(button != null || proxy != null, () -> buttonId + " has no physical route in " + tabId);
					assertTrue(button == null || button.isEnabled(), () -> buttonId + " is disabled in " + bandId);
					String actionId = manager.getToolBarFactory().getActionStringFromId(buttonId);
					try {
						physicalTaskCommands.add(CommandId.fromActionId(actionId));
					} catch (RuntimeException ignoredAction) {
						try {
							// Ribbon definitions may expose a legacy action alias. If
							// it is not one of the stable command ids, use the button
							// id (RibbonInsert -> Insert) as the route identifier.
							String buttonRoute = buttonId.replaceFirst("^Ribbon", "");
							// The ribbon calls the task insertion command Insert,
							// while the legacy menu contract calls it InsertTask.
							physicalTaskCommands.add(CommandId.fromActionId(
								"Insert".equals(buttonRoute) ? "InsertTask" : buttonRoute));
						} catch (RuntimeException ignoredButton) {
							// The standard ribbon also contains view/file/resource commands.
						}
					}
					int before = actions.count(actionId);
					Point clickPoint;
					if (button != null) {
						clickPoint = clickCommand(robot, button);
					} else {
						JPopupMenu popup = (JPopupMenu) proxy.getClientProperty(ModernRibbonPanel.COLLAPSED_POPUP_PROPERTY);
						clickPoint = clickCommand(robot, proxy);
						GuiAcceptanceSupport.await(popup::isVisible,
							"responsive group proxy did not open for " + buttonId);
						AbstractButton popupCommand = findPopupCommand(popup, buttonId);
						assertTrue(popupCommand.isEnabled(), () -> buttonId + " is disabled in responsive popup " + bandId);
						clickPoint = clickCommand(robot, popupCommand);
					}
					GuiAcceptanceSupport.await(() -> actions.count(actionId) == before + 1,
						"Robot click did not dispatch " + buttonId + " (" + actionId + ") at " + clickPoint
							+ (button == null ? " through responsive group popup" : " bounds=" + button.getBounds()
								+ " screen=" + safeScreenBounds(button)));
				}
			}
		}
		EnumSet<CommandId> expectedRibbonCommands = EnumSet.allOf(CommandId.class);
		expectedRibbonCommands.remove(CommandId.PASTE_INSERT);
		assertTrue(physicalTaskCommands.containsAll(expectedRibbonCommands),
				"every routed CommandId except popup-only PasteInsert must have a physical ribbon click: "
						+ physicalTaskCommands);
	}

	@Test
	void highDpiRibbonCommandFamiliesRemainContainedAndNonOverlapping() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for visual matrix coverage.");
		Assumptions.assumeTrue(uiScale() > 1.0d,
			"This visual-matrix case is the high-DPI counterpart of the 100% command sweep.");
		MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
		ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
		ribbon.setVisibleContextualTabs(Set.of("FormatRibbonTask", "NetworkFormatRibbonTask", "CalendarFormatRibbonTask"));
		// Keep the window within a normal desktop at 125/150%; commands that do
		// not fit are intentionally represented by the responsive popup route.
		show(host, 1000, false);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(35);
		for (String tabId : MenuDefinitionSupport.ribbonTaskIds()) {
			AbstractButton tab = clickRibbonTab(robot, host, tabId);
			GuiAcceptanceSupport.await(tab::isSelected, "high-DPI tab was not selected: " + tabId);
			SwingUtilities.invokeAndWait(() -> assertVisibleRibbonControlsFit(host, tab));
			captureVisibleRibbon(robot, "ribbon-high-dpi-" + tabId + ".png", 600);
		}
	}

	/**
	 * #561: Network and Calendar are view-contextual surfaces, not separate
	 * mutation command families.  The contextual tabs must be physically
	 * reachable after the view transition, while their unprovided popup and
	 * shortcut routes must remain absent (rather than silently dispatching a
	 * similarly named legacy action).
	 */
	@Test
	void networkAndCalendarContextualTabsHaveExplicitRouteMatrix() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for contextual route coverage.");
		MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
		ModernRibbonPanel ribbon = (ModernRibbonPanel) host
			.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
		show(host, 1200, false);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);

		List<String> contextualTabs = List.of("NetworkFormatRibbonTask", "CalendarFormatRibbonTask");
		for (String tabId : contextualTabs) {
			// Simulate the active-view transition: a contextual tab is hidden until
			// the corresponding view publishes its descriptor, then becomes visible.
			SwingUtilities.invokeAndWait(() -> ribbon.setVisibleContextualTabs(Set.of()));
			AbstractButton tab = findButton(host,
				MenuDefinitionSupport.menuBundle(Locale.getDefault()).getString(tabId + ".title"));
			assertTrue(!tab.isShowing(), () -> tabId + " must be absent outside its active view");

			SwingUtilities.invokeAndWait(() -> ribbon.setVisibleContextualTabs(Set.of(tabId)));
			GuiPhysicalRouteAdapter.awaitVisible(tab::isShowing,
				"contextual tab did not become visible after view transition: " + tabId);
			click(robot, tab);
			GuiAcceptanceSupport.await(tab::isSelected,
				"Robot click did not select contextual tab " + tabId);
			SwingUtilities.invokeAndWait(() -> {
				assertTrue(ribbon.isContextualTabVisible(tabId));
				assertTrue(host.isShowing());
			});

			Set<String> contextualButtons = MenuDefinitionSupport.ribbonButtonIdsForTask(tabId);
			assertTrue(!contextualButtons.isEmpty(), tabId + " must expose a non-empty format surface");
			for (String buttonId : contextualButtons) {
				AbstractButton button = GuiPhysicalRouteAdapter.visibleButton(host, buttonId);
				assertTrue(button.isEnabled(), () -> tabId + " command is unexpectedly disabled: " + buttonId);
				String actionId = manager.getToolBarFactory().getActionStringFromId(buttonId);
				assertTrue(actionId != null && !actionId.isBlank(),
					() -> tabId + " command has no legacy action mapping: " + buttonId);
				// NetworkAction and CalendarViewAction are view selectors on the
				// standard View tab.  They must not leak into contextual format tabs.
				assertTrue(!Set.of("NetworkAction", "CalendarViewAction").contains(actionId),
					() -> tabId + " contextual command leaked a view selector: " + actionId);
			}
			captureVisibleRibbon(robot, "ribbon-contextual-" + tabId + ".png");
		}

		// Explicit absence contract: no contextual tab registers a popup or
		// root-pane shortcut for the view selectors; the standard View route owns
		// those actions.  This prevents duplicate/ambiguous command ownership.
		SwingUtilities.invokeAndWait(() -> ribbon.setVisibleContextualTabs(Set.of()));
		for (String tabId : contextualTabs) {
			AbstractButton tab = findButton(host,
				MenuDefinitionSupport.menuBundle(Locale.getDefault()).getString(tabId + ".title"));
			assertTrue(!tab.isShowing(), () -> tabId + " remained visible after leaving its view");
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
		show(host, 320, true);

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(35);
		AbstractButton tab = findButton(host, MenuDefinitionSupport.menuBundle(Locale.getDefault())
				.getString("TaskRibbonTask.title"));
		click(robot, tab);
		GuiAcceptanceSupport.await(tab::isSelected, "Task ribbon tab was not selected at narrow width");
		SwingUtilities.invokeAndWait(() -> { });

		AbstractButton overflow = UiComponentWalker.flatten(host).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(this::isInsideTestWindow)
			.filter(button -> button.getClientProperty(ModernRibbonPanel.COLLAPSED_POPUP_PROPERTY) instanceof JPopupMenu popup
					&& popup.getComponentCount() > 0
					&& Boolean.TRUE.equals(button.getClientProperty(ModernRibbonPanel.BAND_PROXY_PROPERTY))
					&& UiComponentWalker.flatten(popup).stream()
						.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
						.anyMatch(command -> "RibbonPaste".equals(command.getActionCommand())))
			.findFirst()
			.orElseThrow(() -> new AssertionError("narrow ribbon did not expose an overflow popup"));
		JPopupMenu popup = (JPopupMenu) overflow.getClientProperty(ModernRibbonPanel.COLLAPSED_POPUP_PROPERTY);
		assertEquals("\u25BC", overflow.getText(), "collapsed group must visibly advertise its disclosure menu");
		assertTrue(overflow.getToolTipText() != null && !overflow.getToolTipText().isBlank(),
			"collapsed group must explain that it contains commands");
		assertEquals(overflow.getToolTipText(), overflow.getAccessibleContext().getAccessibleDescription(),
			"screen readers must receive the same group-menu instruction");
		assertTrue(overflow.isFocusable(), "collapsed group must be reachable by keyboard focus");
		assertTrue(overflow.isFocusPainted(), "keyboard focus must have a visible indication");
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocusInWindow();
			overflow.requestFocusInWindow();
		});
		robot.delay(150);
		GuiAcceptanceSupport.await(overflow::isFocusOwner, "collapsed group did not receive keyboard focus");
		captureVisibleRibbon(robot, "ribbon-task-narrow-focused-proxy.png", 0);
		robot.keyPress(KeyEvent.VK_SPACE);
		robot.keyRelease(KeyEvent.VK_SPACE);
		GuiAcceptanceSupport.await(popup::isVisible, "Space did not open the collapsed group menu");
		robot.keyPress(KeyEvent.VK_ESCAPE);
		robot.keyRelease(KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> !popup.isVisible(), "Escape did not close the collapsed group menu");
		SwingUtilities.invokeAndWait(() -> overflow.requestFocusInWindow());
		GuiAcceptanceSupport.await(overflow::isFocusOwner, "collapsed group did not regain keyboard focus");
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(popup::isVisible, "Enter did not open the collapsed group menu");
		robot.keyPress(KeyEvent.VK_ESCAPE);
		robot.keyRelease(KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> !popup.isVisible(), "Escape did not close the group menu opened by Enter");
		SwingUtilities.invokeAndWait(() -> overflow.requestFocusInWindow());
		Point overflowScreen = overflow.getLocationOnScreen();
		Rectangle frameBounds = frame.getBounds();
		assertTrue(new Rectangle(frameBounds.x, frameBounds.y, frameBounds.width, frameBounds.height)
				.contains(overflowScreen.x + overflow.getWidth() / 2, overflowScreen.y + overflow.getHeight() / 2),
			"responsive overflow trigger must remain inside the host window");
		clickCommand(robot, overflow);
		GuiAcceptanceSupport.await(popup::isVisible, "overflow popup did not open by mouse click");
		AbstractButton hiddenCommand = UiComponentWalker.flatten(popup).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(button -> "RibbonPaste".equals(button.getActionCommand()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("collapsed band popup did not retain RibbonPaste"));
		String actionId = manager.getToolBarFactory().getActionStringFromId(hiddenCommand.getActionCommand());
		int before = actions.count(actionId);
		clickCommand(robot, hiddenCommand);
		GuiAcceptanceSupport.await(() -> actions.count(actionId) == before + 1,
			"collapsed RibbonPaste did not dispatch");
	}

	@Test
	void resizingWindowCollapsesOnlyNecessaryCommandsAndKeepsThemReachable() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		RecordingActionMap actions = new RecordingActionMap();
		MenuManager manager = MenuManager.getInstance(actions);
		JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
		ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
		ribbon.setVisibleContextualTabs(Set.of("FormatRibbonTask"));
		show(host, 1200, true);

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(35);
		AbstractButton tab = findButton(host, MenuDefinitionSupport.menuBundle(Locale.getDefault())
			.getString("TaskRibbonTask.title"));
		click(robot, tab);
		GuiAcceptanceSupport.await(tab::isSelected, "Task ribbon tab was not selected at wide window width");
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> hasPhysicalButtonForCommand(host, "RibbonPaste"),
			"Task commands did not replace the previous tab after physical selection");
		AbstractButton widePaste = findAttachedButtonByCommand(host, "RibbonPaste");
		int initialRibbonWidth = ribbon.getWidth();
		boolean initialPasteVisible = widePaste != null && widePaste.isShowing();
		if (frame.getWidth() >= 1200) {
			assertTrue(initialPasteVisible,
				"Paste should be directly visible when the fixture has at least 1200 pixels: frame=" + frame.getWidth()
					+ " ribbon=" + initialRibbonWidth);
		} else {
			assertTrue(initialPasteVisible || findCollapsedProxyForCommand(host, "RibbonPaste") != null,
				"Paste must remain directly visible or reachable through a collapsed group at initial ribbon width "
					+ initialRibbonWidth);
		}
		int wideProxyCount = visibleGroupProxyCount(host);
		captureVisibleRibbon(robot, "ribbon-task-wide-1200.png", 0);

		resizeWindow(672);
		GuiAcceptanceSupport.await(() -> ribbon.getWidth() < 1000,
			"ribbon did not receive the medium window resize");
		int mediumProxyCount = visibleGroupProxyCount(host);
		assertTrue(mediumProxyCount >= wideProxyCount,
			"reducing window width must not expand command groups: wide=" + wideProxyCount
				+ " medium=" + mediumProxyCount);
		captureVisibleRibbon(robot, "ribbon-task-medium-672.png", 0);

		resizeWindow(320);
		GuiAcceptanceSupport.await(() -> ribbon.getWidth() < 400,
			"ribbon did not receive the narrow window resize");
		int narrowProxyCount = visibleGroupProxyCount(host);
		assertTrue((initialPasteVisible ? narrowProxyCount > wideProxyCount : narrowProxyCount >= mediumProxyCount)
				&& narrowProxyCount > 0,
			"narrowing the window must keep command groups collapsed and reachable: medium=" + mediumProxyCount
				+ " narrow=" + narrowProxyCount + " initialWidth=" + initialRibbonWidth);
		captureVisibleRibbon(robot, "ribbon-task-narrow-320.png", 0);

		AbstractButton pasteOverflow = UiComponentWalker.flatten(host).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(button -> button.isShowing()
				&& Boolean.TRUE.equals(button.getClientProperty(ModernRibbonPanel.BAND_PROXY_PROPERTY))
				&& button.getClientProperty(ModernRibbonPanel.COLLAPSED_POPUP_PROPERTY) instanceof JPopupMenu popup
				&& UiComponentWalker.flatten(popup).stream().filter(AbstractButton.class::isInstance)
					.map(AbstractButton.class::cast).anyMatch(command -> "RibbonPaste".equals(command.getActionCommand())))
			.findFirst().orElseThrow(() -> new AssertionError(
				"Paste must remain reachable through its collapsed group at narrow width"));
		JPopupMenu popup = (JPopupMenu) pasteOverflow.getClientProperty(ModernRibbonPanel.COLLAPSED_POPUP_PROPERTY);
		clickCommand(robot, pasteOverflow);
		GuiAcceptanceSupport.await(popup::isVisible, "narrow group overflow popup did not open");
		AbstractButton paste = UiComponentWalker.flatten(popup).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(button -> "RibbonPaste".equals(button.getActionCommand()))
			.findFirst().orElseThrow(() -> new AssertionError("Paste was lost during responsive collapse"));
		String actionId = manager.getToolBarFactory().getActionStringFromId("RibbonPaste");
		int before = actions.count(actionId);
		clickCommand(robot, paste);
		GuiAcceptanceSupport.await(() -> actions.count(actionId) == before + 1,
			"Paste did not dispatch after resizing into the collapsed layout");
	}

	@Test
	void fileTabOpensBackstageAboveTheWorkspaceAndRestoresTheRibbon() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for GUI coverage.");
		RecordingActionMap actions = new RecordingActionMap();
		MenuManager manager = MenuManager.getInstance(actions);
		MainRibbonFrame mainFrame = new MainRibbonFrame("Ribbon startup", null);
		frame = mainFrame;
		MicroProjectShell.installRibbonShell(mainFrame, manager, null);
		JPanel host = mainFrame.getRibbonPanel();
		SwingUtilities.invokeAndWait(() -> {
			frame.setSize(1100, 700);
			frame.setLocation(0, 0);
			frame.setVisible(true);
		});
		String taskTitle = MenuDefinitionSupport.menuBundle(Locale.getDefault()).getString("TaskRibbonTask.title");
		click(new com.microproject.testsupport.GuiRobot(), findButton(host, taskTitle));
		String fileTitle = MenuDefinitionSupport.menuBundle(Locale.getDefault()).getString("FileRibbonTask.title");
		AbstractButton fileTab = findButton(host, fileTitle);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		click(robot, fileTab);
		GuiAcceptanceSupport.await(() -> namedComponent(frame.getRootPane(), "officeBackstageOverlay") != null,
			"File tab did not open the full-window Backstage surface");
		SwingUtilities.invokeAndWait(() -> {
			JComponent backstage = (JComponent)namedComponent(frame.getRootPane(), "officeBackstageView");
			assertTrue(backstage != null && backstage.isShowing(), "Backstage view is not visible over the workspace");
			assertTrue(namedComponent(backstage, "officeBackstageNavigation") != null, "Backstage navigation pane is missing");
			assertTrue(namedComponent(backstage, "officeBackstageDetails") != null, "Backstage detail pane is missing");
			assertTrue(namedComponent(backstage, "officeBackstageNav-share") == null, "Unsupported Share destination must not be exposed");
			assertTrue(namedComponent(backstage, "officeBackstageNav-info") != null, "Project Info destination is missing");
			assertTrue(namedComponent(backstage, "officeBackstageNav-options") != null, "Options destination is missing");
			assertTrue(namedComponent(backstage, "officeBackstageNav-account") == null,
				"Account destination must be absent without an account/licensing service");
			AbstractButton newProject = (AbstractButton)namedComponent(backstage, "officeBackstageCommand-RibbonNewProject");
			assertTrue(newProject != null && newProject.isShowing(), "New must be available in Backstage details");
			assertTrue(newProject.isEnabled(), "New must be enabled on the File ribbon without a document");
		});
		captureVisibleRibbon(robot, "ribbon-file-backstage-open.png");
		robot.keyPress(KeyEvent.VK_ESCAPE);
		robot.keyRelease(KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> namedComponent(frame.getRootPane(), "officeBackstageOverlay") == null,
			"Escape did not dismiss Backstage");
		AbstractButton restoredTaskAfterEscape = findButton(host, taskTitle);
		assertTrue(restoredTaskAfterEscape.isSelected(), "Escape did not restore the previous ribbon tab");
		GuiAcceptanceSupport.await(restoredTaskAfterEscape::isFocusOwner,
			"Escape did not return keyboard focus to the previously active ribbon tab; focus owner="
				+ java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
		click(robot, findButton(host, fileTitle));
		GuiAcceptanceSupport.await(() -> namedComponent(frame.getRootPane(), "officeBackstageOverlay") != null,
			"File tab did not reopen Backstage after Escape");
		clickCommand(robot, (AbstractButton)namedComponent(frame.getRootPane(), "officeBackstageNav-open"));
		SwingUtilities.invokeAndWait(() -> {
			assertTrue(namedComponent(frame.getRootPane(), "officeBackstageCommand-RibbonOpenProject") != null,
				"Open destination omitted the existing Open command");
			assertTrue(namedComponent(frame.getRootPane(), "officeBackstageCommand-RibbonRecentProjects") != null,
				"Open destination omitted Recent Projects");
		});
		clickCommand(robot, (AbstractButton)namedComponent(frame.getRootPane(), "officeBackstageNav-info"));
		assertTrue(namedComponent(frame.getRootPane(), "officeBackstageCommand-RibbonBackstageProjectInformation") != null,
			"Info destination omitted the existing Project Information action");
		captureVisibleRibbon(robot, "ribbon-file-backstage-info.png");
		clickCommand(robot, (AbstractButton)namedComponent(frame.getRootPane(), "officeBackstageNav-options"));
		assertTrue(namedComponent(frame.getRootPane(), "officeBackstageCommand-RibbonBackstageOptions") != null,
			"Options destination omitted the existing General Options action");
		captureVisibleRibbon(robot, "ribbon-file-backstage-options.png");
		clickCommand(robot, (AbstractButton)namedComponent(frame.getRootPane(), "officeBackstageNav-new"));
		String actionId = manager.getToolBarFactory().getActionStringFromId("RibbonNewProject");
		int before = actions.count(actionId);
		clickCommand(robot, (AbstractButton)namedComponent(frame.getRootPane(), "officeBackstageCommand-RibbonNewProject"));
		GuiAcceptanceSupport.await(() -> actions.count(actionId) == before + 1,
			"Robot click did not dispatch RibbonNewProject from Backstage");
		GuiAcceptanceSupport.await(() -> namedComponent(frame.getRootPane(), "officeBackstageOverlay") == null,
			"Backstage remained open after invoking a command");
		AbstractButton restoredTask = findButton(host, taskTitle);
		assertTrue(restoredTask.isSelected(), "Closing Backstage did not restore the previously selected ribbon tab");
		captureVisibleRibbon(robot, "ribbon-file-backstage.png");
	}

	private static Component namedComponent(Component root, String name) {
		if (name.equals(root.getName())) return root;
		if (root instanceof Container container) {
			for (Component child : container.getComponents()) {
				Component found = namedComponent(child, name);
				if (found != null) return found;
			}
		}
		return null;
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

		Robot robot = new com.microproject.testsupport.GuiRobot();
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
	void narrowRibbonUsesReachableGroupProxiesInsteadOfATabLauncher() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
		ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
		ribbon.setVisibleContextualTabs(Set.of("FormatRibbonTask"));
		show(host, 320, true);

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(35);
		AbstractButton tab = findButton(host, MenuDefinitionSupport.menuBundle(Locale.getDefault())
			.getString("TaskRibbonTask.title"));
		click(robot, tab);
		GuiAcceptanceSupport.await(tab::isSelected, "Task ribbon tab was not selected at narrow width");
		assertTrue(UiComponentWalker.flatten(host).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.noneMatch(button -> button.isShowing()
				&& Boolean.TRUE.equals(button.getClientProperty(ModernRibbonPanel.COLLAPSED_TAB_LAUNCHER_PROPERTY))),
			"narrow ribbon must not replace the selected tab with one launcher");
		AbstractButton proxy = UiComponentWalker.flatten(host).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(this::isInsideTestWindow)
			.filter(button -> Boolean.TRUE.equals(button.getClientProperty(ModernRibbonPanel.BAND_PROXY_PROPERTY)))
			.findFirst().orElseThrow(() -> new AssertionError("narrow ribbon group proxy is missing"));
		JPopupMenu popup = (JPopupMenu)proxy.getClientProperty(ModernRibbonPanel.COLLAPSED_POPUP_PROPERTY);
		assertTrue(popup.getComponentCount() > 0, "group proxy has no commands");
		RibbonTail tail = findRibbonTail(host);
		assertTrue(onEdt(tail.nextButton()::isShowing), "narrow ribbon must expose its horizontal next control");
		Rectangle initialTitleBounds = ribbonBounds(tail.title());
		Rectangle initialViewportBounds = viewportBounds(tail.scrollPane());
		assertTrue(!initialViewportBounds.contains(initialTitleBounds),
			"the last group caption should require horizontal navigation at narrow width");
		int initialViewX = onEdt(() -> tail.scrollPane().getViewport().getViewPosition().x);
		for (int attempt = 0; attempt < 24; attempt++) {
			int beforeScroll = onEdt(() -> tail.scrollPane().getViewport().getViewPosition().x);
			click(robot, tail.nextButton());
			robot.waitForIdle();
			int afterScroll = onEdt(() -> tail.scrollPane().getViewport().getViewPosition().x);
			if (afterScroll == beforeScroll) break;
		}
		int finalViewX = onEdt(() -> tail.scrollPane().getViewport().getViewPosition().x);
		assertTrue(finalViewX > initialViewX, "physical next-arrow clicks did not scroll the ribbon groups");
		Rectangle finalViewportBounds = viewportBounds(tail.scrollPane());
		assertTrue(finalViewportBounds.contains(ribbonBounds(tail.title())),
			"horizontal navigation must expose the full last group caption");
		assertTrue(finalViewportBounds.contains(ribbonBounds(tail.command())),
			"horizontal navigation must expose the last group's command control");
		JPopupMenu tailPopup = onEdt(() -> (JPopupMenu) tail.command()
			.getClientProperty(ModernRibbonPanel.COLLAPSED_POPUP_PROPERTY));
		assertTrue(tailPopup != null, "last group's narrow proxy must retain its command menu");
		click(robot, tail.command());
		GuiAcceptanceSupport.await(tailPopup::isVisible,
			"last group's command menu was not reachable after horizontal navigation");
		assertTrue(onEdt(() -> tailPopup.getComponentCount() > 0),
			"last group's command menu must expose its commands after horizontal navigation");
	}

	@Test
	void narrowTabStripKeepsFileAndSelectedTabVisibleAndExposesHiddenTabs() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
		ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
		ribbon.setVisibleContextualTabs(Set.of("FormatRibbonTask"));
		show(host, 320, true);

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(35);
		AbstractButton taskTab = findTab(host, "TaskRibbonTask");
		AbstractButton fileTab = findTab(host, "FileRibbonTask");
		click(robot, taskTab);
		GuiAcceptanceSupport.await(taskTab::isSelected, "Task tab did not become active at narrow width");
		assertTrue(taskTab.isShowing(), "the selected tab must remain visible when other tabs overflow");
		assertTrue(fileTab.isShowing(), "the fixed File tab must remain directly available");
		String taskTabText = onEdt(taskTab::getText);
		String taskTabAccessibleName = onEdt(() -> taskTab.getAccessibleContext().getAccessibleName());
		boolean taskTabAccessibleSelected = onEdt(() -> taskTab.getAccessibleContext()
			.getAccessibleStateSet().contains(AccessibleState.CHECKED));
		assertEquals(taskTabText, taskTabAccessibleName,
			"the selected tab must expose its label to assistive technology");
		assertTrue(taskTabAccessibleSelected,
			"the selected tab must expose its selected state to assistive technology");
		for (Component component : UiComponentWalker.flatten(host)) {
			if (!(component instanceof AbstractButton button)
				|| !(button.getClientProperty(ModernRibbonPanel.TAB_ID_PROPERTY) instanceof String)
				|| !button.isShowing()) continue;
			assertTrue(button.getWidth() >= button.getPreferredSize().width,
				() -> "visible ribbon tab label is clipped: " + button.getName());
			assertTrue(button.getX() >= 0 && button.getX() + button.getWidth() <= button.getParent().getWidth(),
				() -> "visible ribbon tab escaped the tab strip: " + button.getName());
		}

		AbstractButton overflow = UiComponentWalker.flatten(host).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(button -> Boolean.TRUE.equals(button.getClientProperty(ModernRibbonPanel.TAB_OVERFLOW_PROPERTY)))
			.filter(AbstractButton::isShowing).findFirst()
			.orElseThrow(() -> new AssertionError("narrow tab strip clipped tabs instead of exposing an overflow menu"));
		assertTrue(overflow.getAccessibleContext().getAccessibleDescription() != null
			&& !overflow.getAccessibleContext().getAccessibleDescription().isBlank(),
			"tab overflow must explain its purpose to assistive technology");
		captureVisibleRibbon(robot, "ribbon-tabs-narrow-320.png", 0);
		assertTrue(overflow.isFocusable() && overflow.isFocusPainted(),
			"tab overflow must support keyboard access with a visible focus indication");
		String overflowAccessibleName = onEdt(() -> overflow.getAccessibleContext().getAccessibleName());
		assertTrue(overflowAccessibleName != null && !overflowAccessibleName.isBlank(),
			"tab overflow must expose its name to assistive technology");
		GuiAcceptanceSupport.await(taskTab::isFocusOwner, "physical tab click did not leave focus on the selected tab");
		robot.keyPress(KeyEvent.VK_TAB);
		robot.keyRelease(KeyEvent.VK_TAB);
		GuiAcceptanceSupport.await(overflow::isFocusOwner,
			"Tab traversal from the selected tab did not reach the tab overflow control");
		boolean overflowAccessibleFocused = onEdt(() -> overflow.getAccessibleContext()
			.getAccessibleStateSet().contains(AccessibleState.FOCUSED));
		assertTrue(overflowAccessibleFocused,
			"tab overflow must expose its focused state to assistive technology");
		captureVisibleRibbon(robot, "ribbon-tabs-overflow-focused-320.png", 0);
		robot.keyPress(KeyEvent.VK_SPACE);
		robot.keyRelease(KeyEvent.VK_SPACE);
		JPopupMenu keyboardPopup = (JPopupMenu) overflow.getClientProperty(ModernRibbonPanel.TAB_OVERFLOW_POPUP_PROPERTY);
		GuiAcceptanceSupport.await(() -> keyboardPopup != null && keyboardPopup.isVisible(),
			"Space did not open the tab overflow menu");
		robot.keyPress(KeyEvent.VK_DOWN);
		robot.keyRelease(KeyEvent.VK_DOWN);
		AbstractButton keyboardSelectedTab = selectedOverflowTabOnEdt();
		assertTrue(keyboardSelectedTab != null,
			"keyboard traversal did not select a hidden tab menu item after opening overflow");
		String hiddenTabId = onEdt(() -> (String) keyboardSelectedTab.getClientProperty(ModernRibbonPanel.TAB_ID_PROPERTY));
		String hiddenTabText = onEdt(keyboardSelectedTab::getText);
		String hiddenTabAccessibleName = onEdt(() -> keyboardSelectedTab.getAccessibleContext().getAccessibleName());
		assertEquals(hiddenTabText, hiddenTabAccessibleName,
			"keyboard-selected hidden tab must expose its label to assistive technology");
		Rectangle[] popupBounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> popupBounds[0] = new Rectangle(keyboardPopup.getLocationOnScreen(), keyboardPopup.getSize()));
		assertTrue(new Rectangle(frame.getLocationOnScreen(), frame.getSize()).contains(popupBounds[0]),
			() -> "tab overflow popup escaped the application window: popup=" + popupBounds[0] + " frame=" + frame.getBounds());
		captureVisibleRibbon(robot, "ribbon-tabs-overflow-popup-320.png", 0);
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		AbstractButton selectedTab = findTab(host, hiddenTabId);
		GuiAcceptanceSupport.await(() -> selectedTab.isSelected() && selectedTab.isShowing(),
			"keyboard selection from the overflow menu did not reveal the hidden tab: " + hiddenTabId);
		boolean selectedTabAccessibleSelected = onEdt(() -> selectedTab.getAccessibleContext()
			.getAccessibleStateSet().contains(AccessibleState.CHECKED));
		assertTrue(selectedTabAccessibleSelected,
			"the keyboard-selected tab must expose its selected state to assistive technology");
		assertTrue(fileTab.isShowing(), "selecting an overflow tab hid the fixed File tab");

		clickCommand(robot, overflow);
		JPopupMenu mousePopup = (JPopupMenu) overflow.getClientProperty(ModernRibbonPanel.TAB_OVERFLOW_POPUP_PROPERTY);
		GuiAcceptanceSupport.await(mousePopup::isVisible, "tab overflow menu did not open from its physical control");
		AbstractButton mouseSelectedTab = onEdt(() -> UiComponentWalker.flatten(mousePopup).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(button -> button.getClientProperty(ModernRibbonPanel.TAB_ID_PROPERTY) instanceof String)
			.findFirst().orElse(null));
		assertTrue(mouseSelectedTab != null, "mouse-opened tab overflow menu contains no hidden tabs");
		String mouseHiddenTabId = onEdt(() -> (String) mouseSelectedTab.getClientProperty(ModernRibbonPanel.TAB_ID_PROPERTY));
		robot.keyPress(KeyEvent.VK_ESCAPE);
		robot.keyRelease(KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> !mousePopup.isVisible(), "Escape did not close the preview tab overflow menu");
		clickRibbonTab(robot, host, mouseHiddenTabId);
		AbstractButton mouseRevealedTab = findTab(host, mouseHiddenTabId);
		GuiAcceptanceSupport.await(() -> mouseRevealedTab.isSelected() && mouseRevealedTab.isShowing(),
			"mouse selection from the overflow menu did not reveal the hidden tab: " + mouseHiddenTabId);

		resizeWindow(1200);
		GuiAcceptanceSupport.await(() -> UiComponentWalker.flatten(host).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(button -> button.getClientProperty(ModernRibbonPanel.TAB_ID_PROPERTY) instanceof String)
			.filter(button -> ribbon.isContextualTabVisible((String) button.getClientProperty(ModernRibbonPanel.TAB_ID_PROPERTY))
				|| !((String) button.getClientProperty(ModernRibbonPanel.TAB_ID_PROPERTY)).contains("Format"))
			.allMatch(AbstractButton::isShowing), "wide tab strip did not restore direct tab navigation");
		assertTrue(!overflow.isShowing(), "wide tab strip kept an unnecessary overflow control visible");
	}

	@Test
	void fileCommandsAreNotDuplicatedInTheDocumentRibbon() throws Exception {
		MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
		Set<String> fileCommands = Set.of("RibbonNewProject", "RibbonOpenProject", "RibbonSaveProject",
			"RibbonPrint", "RibbonExportProject", "RibbonLocale");
		assertTrue(UiComponentWalker.flatten(host).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.noneMatch(button -> button.getActionCommand() != null && fileCommands.contains(button.getActionCommand())),
			"File commands must not register a duplicate command surface in the document ribbon");
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
			Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
			int boundedWidth = Math.min(width, Math.max(320, usable.width - 32));
			int boundedHeight = Math.min(360, usable.height);
			frame.setPreferredSize(new Dimension(boundedWidth, boundedHeight));
			frame.pack();
			frame.setBounds(usable.x + Math.max(0, (usable.width - boundedWidth) / 2),
				usable.y + Math.max(0, (usable.height - boundedHeight) / 2), boundedWidth, boundedHeight);
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

	private static AbstractButton findTab(JPanel host, String tabId) {
		return UiComponentWalker.flatten(host).stream()
			.filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast)
			.filter(button -> tabId.equals(button.getClientProperty(ModernRibbonPanel.TAB_ID_PROPERTY)))
			.findFirst().orElseThrow(() -> new AssertionError("Ribbon tab not found: " + tabId));
	}

	private static AbstractButton clickRibbonTab(Robot robot, JPanel host, String tabId) throws Exception {
		AbstractButton tab = findTab(host, tabId);
		if (tab.isShowing()) {
			clickCommand(robot, tab);
		} else {
			AbstractButton overflow = UiComponentWalker.flatten(host).stream()
				.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
				.filter(button -> Boolean.TRUE.equals(button.getClientProperty(ModernRibbonPanel.TAB_OVERFLOW_PROPERTY)))
				.filter(AbstractButton::isShowing).findFirst()
				.orElseThrow(() -> new AssertionError("hidden tab has no visible overflow route: " + tabId));
			clickCommand(robot, overflow);
			GuiAcceptanceSupport.await(() -> overflow.getClientProperty(ModernRibbonPanel.TAB_OVERFLOW_POPUP_PROPERTY)
				instanceof JPopupMenu openedPopup && openedPopup.isVisible(), "tab overflow menu did not open for " + tabId);
			JPopupMenu popup = (JPopupMenu) overflow.getClientProperty(ModernRibbonPanel.TAB_OVERFLOW_POPUP_PROPERTY);
			AbstractButton menuTab = UiComponentWalker.flatten(popup).stream()
				.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
				.filter(button -> tabId.equals(button.getClientProperty(ModernRibbonPanel.TAB_ID_PROPERTY)))
				.findFirst().orElseThrow(() -> new AssertionError("hidden tab is missing from overflow menu: " + tabId));
			clickCommand(robot, menuTab);
		}
		GuiAcceptanceSupport.await(() -> {
			AbstractButton current = findTab(host, tabId);
			return current.isSelected() && current.isShowing();
		}, "physical ribbon tab route did not reveal and select " + tabId);
		return findTab(host, tabId);
	}

	private static <T> T onEdt(Callable<T> action) throws Exception {
		if (SwingUtilities.isEventDispatchThread()) return action.call();
		FutureTask<T> task = new FutureTask<>(action);
		SwingUtilities.invokeAndWait(task);
		return task.get();
	}

	private static AbstractButton selectedOverflowTabOnEdt() throws Exception {
		return onEdt(() -> {
			for (MenuElement element : MenuSelectionManager.defaultManager().getSelectedPath()) {
				Component component = element.getComponent();
				if (component instanceof AbstractButton button
					&& button.getClientProperty(ModernRibbonPanel.TAB_ID_PROPERTY) instanceof String) {
					return button;
				}
			}
			return null;
		});
	}

	private static void openFileCommandDestination(Robot robot, JFrame frame, AbstractButton fileTab,
		String commandId) throws Exception {
		if (findShowingComponent(frame, "officeBackstageOverlay") == null) {
			click(robot, fileTab);
			GuiAcceptanceSupport.await(() -> findShowingComponent(frame, "officeBackstageOverlay") != null,
				"File tab did not open Backstage before command " + commandId);
		}
		java.util.ResourceBundle metadata = java.util.ResourceBundle.getBundle(
			"com.microproject.menu.menuInternal", Locale.ROOT);
		String pageId = metadata.getString(commandId + ".backstagePage");
		Component destination = findShowingComponent(frame, "officeBackstageNav-" + pageId);
		if (!(destination instanceof AbstractButton pageButton))
			throw new AssertionError("Backstage page is not physically available for " + commandId + ": " + pageId);
		// Immediate Backstage destinations are themselves the command button.
		// Leave them for the shared physical click/count assertion below.
		if (metadata.containsKey(commandId + ".backstageImmediate")
			&& Boolean.parseBoolean(metadata.getString(commandId + ".backstageImmediate"))) return;
		if (pageButton instanceof javax.swing.JToggleButton toggle && !toggle.isSelected()) {
			click(robot, pageButton);
			GuiAcceptanceSupport.await(() -> {
				Component selected = findShowingComponent(frame, "officeBackstageNav-" + pageId);
				return selected instanceof javax.swing.JToggleButton current && current.isSelected();
			}, "Backstage did not select page " + pageId + " for " + commandId);
		}
		GuiAcceptanceSupport.await(
			() -> findShowingComponent(frame, "officeBackstageCommand-" + commandId) != null,
			"Backstage page " + pageId + " did not expose command " + commandId);
	}

	private static Component findShowingComponent(Component root, String name) {
		return UiComponentWalker.flatten(root).stream()
			.filter(component -> name.equals(component.getName()) && component.isShowing())
			.findFirst().orElse(null);
	}

	private static AbstractButton findAttachedButtonByCommand(Component root, String command) {
		return UiComponentWalker.flatten(root).stream()
			.filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast)
			.filter(AbstractButton::isShowing)
			.filter(button -> command.equals(button.getActionCommand())
				|| ("officeBackstageCommand-" + command).equals(button.getName()))
			.findFirst().orElse(null);
	}

	private static AbstractButton findCollapsedProxyForCommand(Component root, String command) {
		return UiComponentWalker.flatten(root).stream()
			.filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast)
			.filter(AbstractButton::isShowing)
			.filter(button -> button.getClientProperty(ModernRibbonPanel.COLLAPSED_POPUP_PROPERTY) instanceof JPopupMenu popup
				&& findPopupCommand(popup, command) != null)
			.findFirst().orElse(null);
	}

	private static AbstractButton findPopupCommand(JPopupMenu popup, String command) {
		return UiComponentWalker.flatten(popup).stream()
			.filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast)
			.filter(button -> command.equals(button.getActionCommand()))
			.findFirst().orElse(null);
	}

	private static boolean hasPhysicalButtonForCommand(Component root, String command) {
		return findAttachedButtonByCommand(root, command) != null
			|| findCollapsedProxyForCommand(root, command) != null;
	}

	private static boolean hasAttachedButtonByCommand(Component root, String command) {
		return UiComponentWalker.flatten(root).stream()
			.filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast)
			.filter(AbstractButton::isShowing)
			.anyMatch(button -> command.equals(button.getActionCommand())
				|| ("officeBackstageCommand-" + command).equals(button.getName()));
	}

	private static String ribbonCommandDiagnostic(Component root, String command) {
		List<String> selectedTabs = new ArrayList<>();
		List<String> matchingButtons = new ArrayList<>();
		List<String> visibleCommands = new ArrayList<>();
		for (Component component : UiComponentWalker.flatten(root)) {
			if (!(component instanceof AbstractButton button)) continue;
			String name = String.valueOf(button.getName());
			String actionCommand = String.valueOf(button.getActionCommand());
			if (button instanceof javax.swing.JToggleButton && button.isSelected())
				selectedTabs.add(name + "[" + button.getText() + "]");
			if (command.equals(button.getActionCommand())
				|| ("officeBackstageCommand-" + command).equals(button.getName()))
				matchingButtons.add(name + "[action=" + actionCommand + ",visible=" + button.isVisible()
					+ ",showing=" + button.isShowing() + ",bounds=" + button.getBounds() + "]");
			if (button.isShowing() && actionCommand.startsWith("Ribbon"))
				visibleCommands.add(name + "[action=" + actionCommand + "]");
		}
		List<String> ribbonSurfaces = UiComponentWalker.flatten(root).stream()
			.filter(JComponent.class::isInstance)
			.map(JComponent.class::cast)
			.map(component -> component.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY))
			.filter(ModernRibbonPanel.class::isInstance)
			.map(ModernRibbonPanel.class::cast)
			.map(ribbon -> ribbon.getRibbonDisplayMode() + "/commandSurfaceVisible="
				+ ribbon.isCommandSurfaceVisible())
			.toList();
		return "selectedTabs=" + selectedTabs + "; matchingButtons=" + matchingButtons
			+ "; visibleRibbonCommands=" + visibleCommands + "; ribbonSurfaces=" + ribbonSurfaces;
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

	private boolean isInsideTestWindow(AbstractButton button) {
		if (!button.isShowing()) return false;
		try {
			Point location = button.getLocationOnScreen();
			return frame != null && frame.getBounds().contains(
				location.x + button.getWidth() / 2,
				location.y + button.getHeight() / 2);
		} catch (IllegalComponentStateException ignored) {
			return false;
		}
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

	private static void rightClick(Robot robot, AbstractButton button) throws Exception {
		Point point = button.getLocationOnScreen();
		robot.mouseMove(point.x + button.getWidth() / 2, point.y + button.getHeight() / 2);
		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
	}

	private void resizeWindow(int width) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			frame.setSize(width, frame.getHeight());
			frame.validate();
		});
	}

	private static int visibleGroupProxyCount(JPanel host) {
		return (int) UiComponentWalker.flatten(host).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(button -> button.isShowing()
				&& Boolean.TRUE.equals(button.getClientProperty(ModernRibbonPanel.BAND_PROXY_PROPERTY)))
			.count();
	}

	private record RibbonTail(JScrollPane scrollPane, JButton nextButton, JLabel title, AbstractButton command) { }

	private static RibbonTail findRibbonTail(JPanel host) throws Exception {
		return onEdt(() -> {
			JButton next = UiComponentWalker.flatten(host).stream()
				.filter(JButton.class::isInstance).map(JButton.class::cast)
				.filter(button -> Boolean.TRUE.equals(button.getClientProperty(ModernRibbonPanel.SCROLL_NEXT_PROPERTY)))
				.findFirst().orElseThrow(() -> new AssertionError("narrow ribbon next control is missing"));
			Container viewportContainer = next.getParent();
			JScrollPane scrollPane = java.util.Arrays.stream(viewportContainer.getComponents())
				.filter(JScrollPane.class::isInstance).map(JScrollPane.class::cast)
				.findFirst().orElseThrow(() -> new AssertionError("ribbon next control has no viewport"));
			Component view = scrollPane.getViewport().getView();
			Component lastBand = UiComponentWalker.flatten(view).stream()
				.filter(JComponent.class::isInstance).map(JComponent.class::cast)
				.filter(component -> ModernRibbonPanel.RIBBON_BAND_COMPONENT_NAME.equals(component.getName()))
				.max(java.util.Comparator.comparingInt(component ->
					SwingUtilities.convertRectangle(component.getParent(), component.getBounds(), view).x))
				.orElseThrow(() -> new AssertionError("narrow ribbon has no group bands"));
			JLabel title = java.util.Arrays.stream(((Container) lastBand).getComponents())
				.filter(JLabel.class::isInstance).map(JLabel.class::cast).findFirst()
				.orElseThrow(() -> new AssertionError("last ribbon group has no caption"));
			AbstractButton command = UiComponentWalker.flatten(lastBand).stream()
				.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
				.filter(button -> Boolean.TRUE.equals(button.getClientProperty(ModernRibbonPanel.BAND_PROXY_PROPERTY)))
				.findFirst().orElseThrow(() -> new AssertionError("last narrow ribbon group has no command proxy"));
			return new RibbonTail(scrollPane, next, title, command);
		});
	}

	private static Rectangle ribbonBounds(Component component) throws Exception {
		return onEdt(() -> SwingUtilities.convertRectangle(component.getParent(), component.getBounds(),
			((JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, component))
				.getViewport().getView()));
	}

	private static Rectangle viewportBounds(JScrollPane scrollPane) throws Exception {
		return onEdt(() -> {
			Point position = scrollPane.getViewport().getViewPosition();
			Dimension extent = scrollPane.getViewport().getExtentSize();
			return new Rectangle(position.x, position.y, extent.width, extent.height);
		});
	}

	private static boolean containsCommand(JPanel host, String commandId) {
		return UiComponentWalker.flatten(host).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.anyMatch(button -> commandId.equals(button.getActionCommand()));
	}

	private static void doubleClick(Robot robot, AbstractButton button) throws Exception {
		// Separate successive double-click gestures by the operating system's
		// multi-click window so the final click from one gesture cannot combine
		// with the first click of the next gesture.
		robot.delay(600);
		Point point = button.getLocationOnScreen();
		robot.mouseMove(point.x + button.getWidth() / 2, point.y + button.getHeight() / 2);
		for (int click = 0; click < 2; click++) {
			robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		}
		robot.waitForIdle();
	}

	private static JPopupMenu awaitDisplayModePopup() throws Exception {
		GuiAcceptanceSupport.await(() -> java.util.Arrays.stream(MenuSelectionManager.defaultManager().getSelectedPath())
			.anyMatch(JPopupMenu.class::isInstance), "ribbon display-mode popup did not open");
		return java.util.Arrays.stream(MenuSelectionManager.defaultManager().getSelectedPath())
			.filter(JPopupMenu.class::isInstance).map(JPopupMenu.class::cast)
			.filter(popup -> ModernRibbonPanel.DISPLAY_MODE_POPUP_NAME.equals(popup.getName()))
			.findFirst().orElseThrow(() -> new AssertionError("unexpected popup opened from ribbon tab"));
	}

	private static AbstractButton firstPopupMenuItem(JPopupMenu popup) {
		return UiComponentWalker.flatten(popup).stream().filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast).findFirst().orElseThrow();
	}

	private static void assertVisibleRibbonControlsFit(JPanel host, AbstractButton selectedTab) {
		for (Component component : UiComponentWalker.flatten(host)) {
			if (!(component instanceof AbstractButton button) || !button.isShowing() || button == selectedTab)
				continue;
			if (button.getParent() != null && button.getParent().getName() != null
					&& button.getParent().getName().equals(ModernRibbonPanel.RIBBON_BAND_COMPONENT_NAME))
				assertContainedInRibbonBand(button);
		}
		for (Component parent : UiComponentWalker.flatten(host)) {
			if (!(parent instanceof Container container))
				continue;
			List<Component> children = java.util.Arrays.stream(container.getComponents())
				.filter(Component::isShowing).toList();
			for (int first = 0; first < children.size(); first++) {
				for (int second = first + 1; second < children.size(); second++) {
					Component a = children.get(first);
					Component b = children.get(second);
					if (!(a instanceof AbstractButton) || !(b instanceof AbstractButton))
						continue;
					assertTrue(!a.getBounds().intersects(b.getBounds()),
						() -> "high-DPI ribbon controls overlap in " + container.getClass().getSimpleName()
							+ ": " + a.getBounds() + " and " + b.getBounds());
				}
			}
		}
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
		Rectangle[] usableBounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			JComponent rootPane = frame.getRootPane();
			bounds[0] = new Rectangle(rootPane.getLocationOnScreen(), rootPane.getSize());
			Rectangle usable = new Rectangle(rootPane.getGraphicsConfiguration().getBounds());
			Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(rootPane.getGraphicsConfiguration());
			usable.x += screenInsets.left;
			usable.y += screenInsets.top;
			usable.width -= screenInsets.left + screenInsets.right;
			usable.height -= screenInsets.top + screenInsets.bottom;
			usableBounds[0] = usable;
		});
		BufferedImage screenshot = robot.createScreenCapture(bounds[0]);
		Path directory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		ImageIO.write(screenshot, "png", directory.resolve(fileName).toFile());
		int screenAwareMinimumWidth = Math.min(minimumWidth,
			Math.max(Math.min(640, usableBounds[0].width), usableBounds[0].width - 64));
		assertTrue(usableBounds[0].contains(bounds[0]),
			"captured ribbon root pane must fit the usable work area: bounds=" + bounds[0] + " usable=" + usableBounds[0]);
		assertTrue(screenshot.getWidth() == bounds[0].width && screenshot.getHeight() == bounds[0].height
			&& screenshot.getWidth() >= screenAwareMinimumWidth && screenshot.getHeight() > 120,
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

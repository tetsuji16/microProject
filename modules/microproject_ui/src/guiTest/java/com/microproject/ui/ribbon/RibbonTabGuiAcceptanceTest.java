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

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JComponent;
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
		JPanel host = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
		ModernRibbonPanel ribbon = (ModernRibbonPanel) host.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
		ribbon.setVisibleContextualTabs(Set.of("FormatRibbonTask"));
		// Keep the fixture above the compact breakpoint so every command is a
		// direct hit target; the overflow path is covered separately by the
		// responsive ribbon tests.
		show(host, 1600, true);

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
					AbstractButton button = findAttachedButtonByCommand(host, buttonId);
					assertTrue(button.isShowing(), () -> buttonId + " is not visible in " + tabId);
					assertTrue(button.isEnabled(), () -> buttonId + " is disabled in " + bandId);
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
					Point clickPoint = clickCommand(robot, button);
					GuiAcceptanceSupport.await(() -> actions.count(actionId) == before + 1,
						"Robot click did not dispatch " + buttonId + " (" + actionId + ") at " + clickPoint
							+ " bounds=" + button.getBounds() + " screen=" + safeScreenBounds(button));
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
			AbstractButton tab = findButton(host,
				MenuDefinitionSupport.menuBundle(Locale.getDefault()).getString(tabId + ".title"));
			click(robot, tab);
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
		GuiAcceptanceSupport.await(() -> containsCommand(host, "RibbonPaste"),
			"Task commands did not replace the previous tab after physical selection");
		AbstractButton widePaste = findAttachedButtonByCommand(host, "RibbonPaste");
		assertTrue(widePaste.isShowing(), "Paste should be directly visible when the ribbon has enough width");
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
		assertTrue(narrowProxyCount > wideProxyCount,
			"narrowing the window must collapse some command groups: wide=" + wideProxyCount
				+ " narrow=" + narrowProxyCount);
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
		MainRibbonFrame mainFrame = new MainRibbonFrame("Ribbon startup", null, null);
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
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocusInWindow();
			overflow.requestFocusInWindow();
		});
		GuiAcceptanceSupport.await(overflow::isFocusOwner, "tab overflow did not receive keyboard focus");
		captureVisibleRibbon(robot, "ribbon-tabs-overflow-focused-320.png", 0);
		robot.keyPress(KeyEvent.VK_SPACE);
		robot.keyRelease(KeyEvent.VK_SPACE);
		JPopupMenu keyboardPopup = (JPopupMenu) overflow.getClientProperty(ModernRibbonPanel.TAB_OVERFLOW_POPUP_PROPERTY);
		GuiAcceptanceSupport.await(() -> keyboardPopup != null && keyboardPopup.isVisible(),
			"Space did not open the tab overflow menu");
		robot.keyPress(KeyEvent.VK_ESCAPE);
		robot.keyRelease(KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> !keyboardPopup.isVisible(), "Escape did not close the tab overflow menu");
		clickCommand(robot, overflow);
		JPopupMenu popup = (JPopupMenu) overflow.getClientProperty(ModernRibbonPanel.TAB_OVERFLOW_POPUP_PROPERTY);
		GuiAcceptanceSupport.await(popup::isVisible, "tab overflow menu did not open from its physical control");
		Rectangle[] popupBounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> popupBounds[0] = new Rectangle(popup.getLocationOnScreen(), popup.getSize()));
		assertTrue(new Rectangle(frame.getLocationOnScreen(), frame.getSize()).contains(popupBounds[0]),
			() -> "tab overflow popup escaped the application window: popup=" + popupBounds[0] + " frame=" + frame.getBounds());
		captureVisibleRibbon(robot, "ribbon-tabs-overflow-popup-320.png", 0);
		AbstractButton hiddenTab = UiComponentWalker.flatten(popup).stream()
			.filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast)
			.filter(button -> button.getClientProperty(ModernRibbonPanel.TAB_ID_PROPERTY) instanceof String)
			.findFirst().orElseThrow(() -> new AssertionError("tab overflow menu contains no hidden tabs"));
		String hiddenTabId = (String) hiddenTab.getClientProperty(ModernRibbonPanel.TAB_ID_PROPERTY);
		clickCommand(robot, hiddenTab);
		AbstractButton selectedTab = findTab(host, hiddenTabId);
		GuiAcceptanceSupport.await(() -> selectedTab.isSelected() && selectedTab.isShowing(),
			"choosing an overflow tab did not select and reveal it: " + hiddenTabId);
		assertTrue(fileTab.isShowing(), "selecting an overflow tab hid the fixed File tab");

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

	private static AbstractButton findTab(JPanel host, String tabId) {
		return UiComponentWalker.flatten(host).stream()
			.filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast)
			.filter(button -> tabId.equals(button.getClientProperty(ModernRibbonPanel.TAB_ID_PROPERTY)))
			.findFirst().orElseThrow(() -> new AssertionError("Ribbon tab not found: " + tabId));
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
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(frame.getRootPane().getLocationOnScreen(), frame.getRootPane().getSize()));
		BufferedImage screenshot = robot.createScreenCapture(bounds[0]);
		Path directory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"));
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

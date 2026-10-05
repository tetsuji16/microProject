/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import javax.imageio.ImageIO;

import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.dialog.UsabilityStrings;
import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.RibbonGuiEnvironment;
import com.microproject.ui.ribbon.RibbonController;
import com.microproject.ui.ribbon.RibbonDisplayMode;
import com.microproject.ui.ribbon.RibbonDisplayPreferences;
import com.microproject.util.Environment;

class OfficeChromeRibbonDisplayGuiAcceptanceTest {
	private JFrame frame;
	private MainRibbonFrame persistedFrame;
	private boolean originalQuickAccessVisible;
	private RibbonDisplayMode originalRibbonDisplayMode;
	private boolean previousRibbonUi;
	private boolean previousNewLook;

	@org.junit.jupiter.api.BeforeEach
	void configureRibbonEnvironment() {
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		originalRibbonDisplayMode = RibbonDisplayPreferences.load();
		originalQuickAccessVisible = RibbonDisplayPreferences.loadQuickAccessVisible();
		RibbonGuiEnvironment.initialize();
	}

	@AfterEach
	void closeWindow() throws Exception {
		if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
		if (persistedFrame != null) SwingUtilities.invokeAndWait(() -> persistedFrame.dispose());
		com.microproject.ui.ribbon.RibbonDisplayPreferences.saveQuickAccessVisible(originalQuickAccessVisible);
		if (originalRibbonDisplayMode != null) RibbonDisplayPreferences.save(originalRibbonDisplayMode);
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
	}

	@Test
	void titleBarDisplayOptionsPhysicallySwitchBetweenAllRibbonModes() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		originalQuickAccessVisible = com.microproject.ui.ribbon.RibbonDisplayPreferences.loadQuickAccessVisible();
		com.microproject.ui.ribbon.RibbonDisplayPreferences.saveQuickAccessVisible(true);
		JPanel ribbonHost = manager.createRibbonPanel(MenuManager.STANDARD_RIBBON, null);
		RibbonController ribbon = (RibbonController) ribbonHost.getClientProperty(RibbonController.CONTEXTUAL_TABS_PROPERTY);
		JPanel documentSurface = new JPanel();
		ribbon.setRibbonDisplayMode(RibbonDisplayMode.ALWAYS_SHOW);
		OfficeChromePanel chrome = new OfficeChromePanel(manager, ribbonHost, () -> { });
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("Office chrome ribbon display acceptance");
			frame.add(chrome, BorderLayout.NORTH);
			frame.add(documentSurface, BorderLayout.CENTER);
			frame.setSize(1200, 500);
			frame.setLocationByPlatform(true);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
			frame.toFront();
			frame.requestFocus();
		});
		AbstractButton options = findShowingButton(chrome, OfficeChromePanel.RIBBON_DISPLAY_OPTIONS_NAME);
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		GuiAcceptanceSupport.await(frame::isActive, "office chrome test window did not become active");
		GuiAcceptanceSupport.await(() -> options.isShowing()
			&& options.getWidth() > 0 && options.getHeight() > 0,
			"title-bar display options button did not become laid out");
		assertTrue(OfficeChromePanel.RIBBON_SURFACE_NAME.equals(options.getParent().getParent().getName()),
			"ribbon display options should be placed at the ribbon surface's lower-right edge");
		Rectangle surfaceBounds = bounds(chrome, OfficeChromePanel.RIBBON_SURFACE_NAME);
		Rectangle buttonBounds = bounds(chrome, OfficeChromePanel.RIBBON_DISPLAY_OPTIONS_NAME);
		assertTrue(buttonBounds.getMaxX() >= surfaceBounds.getMaxX() - 80,
			"ribbon display options are not aligned with the window's right edge: surface="
				+ surfaceBounds + ", button=" + buttonBounds);
		assertTrue(buttonBounds.getY() >= surfaceBounds.getY(),
			"ribbon display options should appear at the lower-right of the ribbon surface: surface="
				+ surfaceBounds + ", button=" + buttonBounds);
		robot.waitForIdle();
		click(robot, options);
		JPopupMenu optionsPopup = displayOptionsPopup();
		assertEquals(6, optionsPopup.getComponentCount(),
			"display options should contain heading, three modes, separator, and QAT visibility command");
		assertEquals(UsabilityStrings.text("chrome.ribbonShow"), ((javax.swing.JLabel) optionsPopup.getComponent(0)).getText());
		assertEquals(UsabilityStrings.text("chrome.ribbonAutoHide"), ((AbstractButton) optionsPopup.getComponent(1)).getText());
		assertEquals(UsabilityStrings.text("chrome.ribbonTabsOnly"), ((AbstractButton) optionsPopup.getComponent(2)).getText());
		assertEquals(UsabilityStrings.text("chrome.ribbonAlwaysShow"), ((AbstractButton) optionsPopup.getComponent(3)).getText());
		assertTrue(((AbstractButton) optionsPopup.getComponent(3)).isSelected(),
			"Always show Ribbon should be selected in the default display state");
		assertTrue(optionsPopup.getComponent(4) instanceof JPopupMenu.Separator);
		assertEquals(UsabilityStrings.text("chrome.ribbonHideQuickAccess"), ((AbstractButton) optionsPopup.getComponent(5)).getText());
		assertPopupFitsWindow(optionsPopup, frame);
		capture(robot, "ribbon-display-options-popup");
		click(robot, popupItem(UsabilityStrings.text("chrome.ribbonTabsOnly")));
		GuiAcceptanceSupport.await(() -> ribbon.getRibbonDisplayMode() == RibbonDisplayMode.TABS_ONLY,
			"title-bar display options did not switch to tabs-only mode");
		assertTrue(!ribbon.isCommandSurfaceVisible());
		capture(robot, "ribbon-display-tabs-only");

		GuiAcceptanceSupport.await(options::isShowing,
			"title-bar display options became unreachable after collapsing the command bands");
		click(robot, options);
		click(robot, popupItem(UsabilityStrings.text("chrome.ribbonAlwaysShow")));
		GuiAcceptanceSupport.await(() -> ribbon.getRibbonDisplayMode() == RibbonDisplayMode.ALWAYS_SHOW,
			"title-bar display options did not restore the command surface");
		assertTrue(ribbon.isCommandSurfaceVisible());

		click(robot, findShowingButton(chrome, OfficeChromePanel.RIBBON_DISPLAY_OPTIONS_NAME));
		AbstractButton autoHide = popupItem(UsabilityStrings.text("chrome.ribbonAutoHide"));
		assertTrue(!autoHide.isSelected(), "auto-hide should not be selected before the user chooses it");
		click(robot, autoHide);
		GuiAcceptanceSupport.await(() -> ribbon.getRibbonDisplayMode() == RibbonDisplayMode.AUTO_HIDE,
			"title-bar display options did not select auto-hide");
		GuiAcceptanceSupport.await(() -> !((javax.swing.JComponent) ribbon).isVisible(),
			"auto-hide must hide the ribbon surface while leaving window chrome available");
		capture(robot, "ribbon-display-auto-hide");

		click(robot, findShowingButton(chrome, OfficeChromePanel.RIBBON_DISPLAY_OPTIONS_NAME));
		GuiAcceptanceSupport.await(() -> ((javax.swing.JComponent) ribbon).isVisible()
			&& ribbon.isCommandSurfaceVisible() && ribbon.getRibbonDisplayMode() == RibbonDisplayMode.AUTO_HIDE,
			"the Office More control must temporarily reveal the auto-hidden ribbon without changing its saved mode");
		capture(robot, "ribbon-display-auto-hide-temporary-reveal");
		AbstractButton taskTab = ribbonTab(ribbonHost, "TaskRibbonTask");
		click(robot, taskTab);
		GuiAcceptanceSupport.await(() -> taskTab.isSelected() && ribbon.isCommandSurfaceVisible(),
			"interacting with a ribbon tab must keep a temporary Auto-hide reveal open");
		click(robot, documentSurface);
		GuiAcceptanceSupport.await(() -> !((javax.swing.JComponent) ribbon).isVisible()
			&& ribbon.getRibbonDisplayMode() == RibbonDisplayMode.AUTO_HIDE,
			"returning to the document must dismiss the temporary reveal and preserve Auto-hide");
		capture(robot, "ribbon-display-auto-hide-return-to-document");
		pressAlt(robot);
		GuiAcceptanceSupport.await(() -> ((javax.swing.JComponent) ribbon).isVisible()
			&& ribbon.isCommandSurfaceVisible() && ribbon.getRibbonDisplayMode() == RibbonDisplayMode.AUTO_HIDE,
			"physical Alt key did not temporarily reveal the Auto-hide ribbon");
		capture(robot, "ribbon-display-auto-hide-alt-reveal");
		click(robot, documentSurface);
		GuiAcceptanceSupport.await(() -> !((javax.swing.JComponent) ribbon).isVisible()
			&& ribbon.getRibbonDisplayMode() == RibbonDisplayMode.AUTO_HIDE,
			"returning to the document after Alt reveal must restore Auto-hide");

		click(robot, findShowingButton(chrome, OfficeChromePanel.RIBBON_DISPLAY_OPTIONS_NAME));
		GuiAcceptanceSupport.await(() -> ((javax.swing.JComponent) ribbon).isVisible()
			&& ribbon.isCommandSurfaceVisible(), "the Office More control did not reveal the ribbon again");
		click(robot, findShowingButton(chrome, OfficeChromePanel.RIBBON_DISPLAY_OPTIONS_NAME));
		AbstractButton selectedAutoHide = popupItem(UsabilityStrings.text("chrome.ribbonAutoHide"));
		assertTrue(selectedAutoHide.isSelected(), "display options must identify the active auto-hide mode");
		capture(robot, "ribbon-display-options-auto-hide-selected");
		click(robot, popupItem(UsabilityStrings.text("chrome.ribbonAlwaysShow")));
		GuiAcceptanceSupport.await(() -> ribbon.getRibbonDisplayMode() == RibbonDisplayMode.ALWAYS_SHOW
			&& ((javax.swing.JComponent) ribbon).isVisible() && ribbon.isCommandSurfaceVisible(),
			"display options did not restore the auto-hidden ribbon");
		capture(robot, "ribbon-display-always-show-restored");

		click(robot, findShowingButton(chrome, OfficeChromePanel.RIBBON_DISPLAY_OPTIONS_NAME));
		click(robot, popupItem(UsabilityStrings.text("chrome.ribbonHideQuickAccess")));
		GuiAcceptanceSupport.await(() -> !findComponent(chrome, OfficeChromePanel.QUICK_ACCESS_COMMANDS_NAME)
			.isVisible(), "Quick Access Toolbar commands did not hide");
		assertTrue(!com.microproject.ui.ribbon.RibbonDisplayPreferences.loadQuickAccessVisible(),
			"Quick Access Toolbar visibility should persist as a user preference");
		click(robot, findShowingButton(chrome, OfficeChromePanel.RIBBON_DISPLAY_OPTIONS_NAME));
		click(robot, popupItem(UsabilityStrings.text("chrome.ribbonShowQuickAccess")));
		assertTrue(com.microproject.ui.ribbon.RibbonDisplayPreferences.loadQuickAccessVisible(),
			"Quick Access Toolbar should be restorable from display options");
	}

	@Test
	void productionShellRestoresTheSavedRibbonModeWhenCreatedAgain() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		MenuManager manager = MenuManager.getInstance(MenuActionMapSupport.noopActionMap());
		RibbonDisplayPreferences.save(RibbonDisplayMode.ALWAYS_SHOW);
		persistedFrame = createProductionRibbonFrame(manager, "Ribbon preference first shell");
		JPanel firstShell = persistedFrame.getRibbonPanel();
		AbstractButton options = findShowingButton(firstShell, OfficeChromePanel.RIBBON_DISPLAY_OPTIONS_NAME);
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		GuiAcceptanceSupport.await(persistedFrame::isActive, "first production ribbon window did not become active");
		click(robot, options);
		click(robot, popupItem(UsabilityStrings.text("chrome.ribbonTabsOnly")));
		GuiAcceptanceSupport.await(() -> RibbonDisplayPreferences.load() == RibbonDisplayMode.TABS_ONLY,
			"production shell did not persist the selected Tabs Only mode");
		RibbonController firstRibbon = ribbonController(firstShell);
		assertEquals(RibbonDisplayMode.TABS_ONLY, firstRibbon.getRibbonDisplayMode());
		assertFalse(firstRibbon.isCommandSurfaceVisible(), "Tabs Only must hide command bands in the first shell");

		SwingUtilities.invokeAndWait(() -> {
			persistedFrame.dispose();
			persistedFrame = null;
		});
		persistedFrame = createProductionRibbonFrame(manager, "Ribbon preference recreated shell");
		RibbonController restoredRibbon = ribbonController(persistedFrame.getRibbonPanel());
		GuiAcceptanceSupport.await(() -> restoredRibbon.getRibbonDisplayMode() == RibbonDisplayMode.TABS_ONLY,
			"new production shell did not load the saved ribbon display mode");
		assertFalse(restoredRibbon.isCommandSurfaceVisible(),
			"the recreated shell must render Tabs Only, not merely retain the preference value");
	}

	private MainRibbonFrame createProductionRibbonFrame(MenuManager manager, String title) throws Exception {
		MainRibbonFrame[] created = new MainRibbonFrame[1];
		SwingUtilities.invokeAndWait(() -> {
			MainRibbonFrame next = new MainRibbonFrame(title, "", "");
			persistedFrame = next;
			ProjectLibreShell.installRibbonShell(next, manager, () -> { });
			next.getContentPane().add(new JPanel(), java.awt.BorderLayout.CENTER);
			next.setSize(1200, 500);
			next.setLocationByPlatform(true);
			next.setAlwaysOnTop(true);
			next.setVisible(true);
			next.toFront();
			created[0] = next;
		});
		GuiAcceptanceSupport.await(created[0]::isShowing, "production ribbon shell did not become visible");
		return created[0];
	}

	private static RibbonController ribbonController(JPanel shell) {
		Object controller = shell.getClientProperty(RibbonController.CONTEXTUAL_TABS_PROPERTY);
		if (controller instanceof RibbonController ribbon) return ribbon;
		throw new AssertionError("production shell does not expose its ribbon controller");
	}

	private static AbstractButton popupItem(String text) throws Exception {
		JPopupMenu popup = displayOptionsPopup();
		AbstractButton item = UiComponentWalker.flatten(popup).stream().filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast).filter(button -> text.equals(button.getText())).findFirst().orElseThrow();
		GuiAcceptanceSupport.await(item::isShowing, "ribbon display menu item did not become visible: " + text);
		return item;
	}

	private static AbstractButton findShowingButton(JPanel root, String name) {
		return UiComponentWalker.flatten(root).stream().filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast).filter(button -> name.equals(button.getName()) && button.isShowing())
			.findFirst().orElseThrow();
	}

	private static AbstractButton ribbonTab(JPanel root, String tabId) {
		return UiComponentWalker.flatten(root).stream().filter(AbstractButton.class::isInstance)
			.map(AbstractButton.class::cast)
			.filter(button -> tabId.equals(((javax.swing.JComponent) button)
				.getClientProperty(com.microproject.ui.ribbon.ModernRibbonPanel.TAB_ID_PROPERTY)))
			.findFirst().orElseThrow();
	}

	private static java.awt.Component findComponent(JPanel root, String name) {
		return UiComponentWalker.flatten(root).stream().filter(candidate -> name.equals(candidate.getName()))
			.findFirst().orElseThrow();
	}

	private static JPopupMenu displayOptionsPopup() throws Exception {
		GuiAcceptanceSupport.await(() -> Arrays.stream(MenuSelectionManager.defaultManager().getSelectedPath())
			.anyMatch(JPopupMenu.class::isInstance), "ribbon display options popup did not open");
		JPopupMenu popup = Arrays.stream(MenuSelectionManager.defaultManager().getSelectedPath())
			.filter(JPopupMenu.class::isInstance).map(JPopupMenu.class::cast)
			.filter(candidate -> OfficeChromePanel.RIBBON_DISPLAY_OPTIONS_POPUP_NAME.equals(candidate.getName()))
			.findFirst().orElseThrow();
		return popup;
	}

	private static void assertPopupFitsWindow(JPopupMenu popup, JFrame frame) throws Exception {
		Rectangle[] popupBounds = new Rectangle[1];
		Rectangle[] frameBounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			popupBounds[0] = new Rectangle(popup.getLocationOnScreen(), popup.getSize());
			frameBounds[0] = new Rectangle(frame.getLocationOnScreen(), frame.getSize());
		});
		assertTrue(frameBounds[0].contains(popupBounds[0]),
			() -> "ribbon display options popup is clipped by its window: popup=" + popupBounds[0]
				+ " window=" + frameBounds[0]);
	}

	private static Rectangle bounds(JPanel root, String name) throws Exception {
		java.awt.Component component = UiComponentWalker.flatten(root).stream()
			.filter(candidate -> name.equals(candidate.getName()) && candidate.isShowing()).findFirst().orElseThrow();
		Rectangle[] result = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> result[0] = new Rectangle(component.getLocationOnScreen(), component.getSize()));
		return result[0];
	}

	private static void click(Robot robot, java.awt.Component button) {
		robot.waitForIdle();
		Point point = button.getLocationOnScreen();
		robot.mouseMove(point.x + button.getWidth() / 2, point.y + button.getHeight() / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	private static void pressAlt(Robot robot) {
		robot.keyPress(KeyEvent.VK_ALT);
		robot.keyRelease(KeyEvent.VK_ALT);
		robot.waitForIdle();
	}

	private void capture(Robot robot, String name) throws Exception {
		robot.waitForIdle();
		robot.delay(250);
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(frame.getLocationOnScreen(), frame.getSize()));
		BufferedImage image = robot.createScreenCapture(bounds[0]);
		Path directory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		String environment = (System.getProperty("user.language", "unknown") + "-"
			+ System.getProperty("sun.java2d.uiScale", "default")).replaceAll("[^A-Za-z0-9_.-]", "_");
		ImageIO.write(image, "png", directory.resolve(name + "-" + environment + ".png").toFile());
	}
}

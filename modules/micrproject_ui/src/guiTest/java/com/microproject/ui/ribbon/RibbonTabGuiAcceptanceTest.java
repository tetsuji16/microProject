/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.ribbon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.menu.MenuActionMapSupport;
import com.microproject.menu.MenuManager;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.util.FlatUiSupport;

/** Non-headless coverage for a real mouse click on a responsive ribbon tab. */
class RibbonTabGuiAcceptanceTest {
	private JFrame frame;

	@AfterEach
	void closeWindow() throws Exception {
		if (frame != null) {
			SwingUtilities.invokeAndWait(() -> {
				frame.dispose();
				frame = null;
			});
		}
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

	private void show(JPanel host) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("Ribbon tab GUI acceptance");
			// MainRibbonFrame docks the production shell in BorderLayout.NORTH.  Keep
			// the acceptance fixture identical so the capture reflects the actual
			// ribbon height rather than stretching it through the whole test window.
			frame.add(host, BorderLayout.NORTH);
			frame.add(new JPanel(), BorderLayout.CENTER);
			frame.setPreferredSize(new Dimension(1200, 360));
			frame.pack();
			frame.setLocationByPlatform(true);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
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

	private void captureVisibleRibbon(Robot robot, String fileName) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(frame.getRootPane().getLocationOnScreen(), frame.getRootPane().getSize()));
		BufferedImage screenshot = robot.createScreenCapture(bounds[0]);
		Path directory = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		ImageIO.write(screenshot, "png", directory.resolve(fileName).toFile());
		assertTrue(screenshot.getWidth() > 900 && screenshot.getHeight() > 200, "captured ribbon is unexpectedly small");
	}
}

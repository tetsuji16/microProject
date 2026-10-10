/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.menu.MenuManager;
import com.microproject.menu.ProjectMenuActionMap;
import com.microproject.menu.ExtToolBarFactory;
import com.microproject.menu.MenuRibbonCommandSource;
import com.microproject.menu.testsupport.MenuDefinitionSupport;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.ui.ribbon.SwingRibbonFactory;

/** Verifies the tab-row search hit area using the real ribbon inside a realized Swing window. */
class OfficeChromeSearchGuiAcceptanceTest {
	private MainRibbonFrame frame;

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
	void physicalTabRowSearchAndHeaderControlsRemainInteractiveAndRunOnce() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for this acceptance test.");
		Assumptions.assumeTrue(com.microproject.util.Environment.isWindows(), "The full-window-content caption integration is Windows-specific.");
		final OfficeChromePanel[] panel = new OfficeChromePanel[1];
		final JTextField[] field = new JTextField[1];
		final Component[] box = new Component[1];
		final Component[] save = new Component[1];
		final Component[] help = new Component[1];
		AtomicInteger searchCalls = new AtomicInteger();
		AtomicInteger saveCalls = new AtomicInteger();
		AtomicInteger helpCalls = new AtomicInteger();
		ProjectMenuActionMap actionMap = new ProjectMenuActionMap() {
			@Override public Action getAction(String key) {
				return new AbstractAction(key) {
					@Override public void actionPerformed(ActionEvent event) {
						// MenuManager resolves user-facing ids through menuInternal.properties
						// before asking this map for the canonical application action.
						if ("FindAction".equals(key)) searchCalls.incrementAndGet();
						if ("SaveProjectAction".equals(key)) saveCalls.incrementAndGet();
					}
				};
			}
			@Override public String getStringFromAction(Action action) {
				Object name = action.getValue(Action.NAME);
				return name == null ? "" : name.toString();
			}
			};
		SwingUtilities.invokeAndWait(() -> {
			MenuManager manager = MenuManager.getInstance(actionMap);
			var bundles = MenuDefinitionSupport.ribbonBundles(Locale.ROOT);
			ExtToolBarFactory buttonFactory = new ExtToolBarFactory(actionMap, bundles);
			JPanel ribbonPanel = new SwingRibbonFactory(
				new MenuRibbonCommandSource(buttonFactory), bundles).createPanel(MenuManager.STANDARD_RIBBON, () -> {});
			frame = new MainRibbonFrame("Office chrome search acceptance", "");
			panel[0] = new OfficeChromePanel(frame, manager, ribbonPanel, helpCalls::incrementAndGet,
				AutoSaveControl.DISABLED);
			field[0] = find(panel[0], OfficeChromePanel.SEARCH_FIELD_NAME, JTextField.class);
			box[0] = find(panel[0], OfficeChromePanel.SEARCH_BOX_NAME, Component.class);
			assertEquals("projectLibreRibbonTabRow", box[0].getParent().getName());
			save[0] = find(panel[0], "RibbonTopBarSaveProject", Component.class);
			help[0] = find(panel[0], OfficeChromePanel.HELP_BUTTON_NAME, Component.class);
			frame.setRibbonPanel(panel[0]);
			frame.setSize(1100, 260);
			frame.setLocation(80, 80);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(frame::isActive, "native Office header frame did not receive focus");
		GuiAcceptanceSupport.await(() -> box[0].isShowing() && save[0].isShowing() && help[0].isShowing(),
			"native Office header controls did not become visible");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(35);
		click(robot, field[0]);
		GuiAcceptanceSupport.await(field[0]::isFocusOwner, "physical click in the search field did not focus the text field");
		robot.keyPress(KeyEvent.VK_1);
		robot.keyRelease(KeyEvent.VK_1);
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(() -> searchCalls.get() == 1,
			"physical search submission did not invoke the canonical RibbonFind action exactly once");
		assertEquals("1", field[0].getText(), "physical keyboard input was not retained by the search field");
		click(robot, save[0]);
		GuiAcceptanceSupport.await(() -> saveCalls.get() == 1,
			"physical QAT Save click did not invoke its action exactly once");
		click(robot, help[0]);
		GuiAcceptanceSupport.await(() -> helpCalls.get() == 1,
			"physical Help click did not invoke its action exactly once");
	}

	private static void click(Robot robot, Component component) throws Exception {
		Point[] point = new Point[1];
		SwingUtilities.invokeAndWait(() -> point[0] = component.getLocationOnScreen());
		robot.waitForIdle();
		robot.mouseMove(point[0].x + component.getWidth() / 2, point[0].y + component.getHeight() / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private static <T extends Component> T find(Component root, String name, Class<T> type) {
		if (type.isInstance(root) && name.equals(root.getName()))
			return type.cast(root);
		if (root instanceof java.awt.Container container) {
			for (Component child : container.getComponents()) {
				try {
					return find(child, name, type);
				} catch (IllegalArgumentException ignored) {
					// Continue searching siblings.
				}
			}
		}
		throw new IllegalArgumentException("Component not found: " + name);
	}

}

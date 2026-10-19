/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.Point;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.testsupport.DialogLayoutAssertions;
import com.microproject.testsupport.DialogScreenshotArtifacts;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.RibbonGuiSupport;
import com.microproject.ui.ribbon.ModernRibbonPanel;
import com.microproject.ui.ribbon.RibbonDisplayMode;
import com.microproject.ui.ribbon.RibbonDisplayPreferences;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.pm.snapshot.Snapshottable;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.microproject.ui.theme.MicroProjectTheme;

/** GUI regression for the Clear Baseline controls reported in #715. */
class BaselineDialogGuiAcceptanceTest {
	private BaselineDialog dialog;
	private MainRibbonFrame window;
	private GraphicManager manager;
	private Font originalDefaultFont;
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
		SwingUtilities.invokeAndWait(() -> {
			FlatLightLaf.setup();
			MicroProjectTheme.installLight();
			if (originalDefaultFont != null) UIManager.put("defaultFont", originalDefaultFont);
		});
		Environment.setStandAlone(previousStandalone);
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
		Environment.setClientSide(previousClientSide);
		if (previousRibbonDisplayMode != null) RibbonDisplayPreferences.save(previousRibbonDisplayMode);
	}

	@Test
	void clearBaselineControlsRemainVisibleAndClickable() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for baseline dialog layout acceptance coverage.");
		originalDefaultFont = UIManager.getFont("defaultFont");
		previousStandalone = Environment.getStandAlone();
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousClientSide = Environment.isClientSide();
		previousRibbonDisplayMode = RibbonDisplayPreferences.load();
		RibbonDisplayPreferences.save(RibbonDisplayMode.TABS_ONLY);
		Environment.setStandAlone(true);
		Environment.setClientSide(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("microProject — Clear Baseline layout acceptance", null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			window.setAlwaysOnTop(true);
			if (!window.isShowing()) {
				window.setSize(1200, 700);
				window.setLocationByPlatform(true);
				window.setAlwaysOnTop(true);
				window.setVisible(true);
			}
			window.setBounds(java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
		});
		GuiAcceptanceSupport.await(() -> window.isShowing(), "application ribbon window did not become visible");
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("clear-baseline-gui", undo);
		pool.setLocal(true);
		Project project = Project.createProject(pool, undo);
		project.setName("Clear Baseline GUI");
		com.microproject.pm.task.NormalTask task = project.createScriptedTask();
		task.setName("Baseline fixture task");
		project.saveCurrentToSnapshot(Integer.valueOf(Snapshottable.BASELINE), false, java.util.List.of(task));
		SwingUtilities.invokeAndWait(() -> manager.addProjectFrame(project));
		GuiAcceptanceSupport.await(() -> manager.getCurrentFrame() != null
			&& manager.getCurrentFrame().getProject() == project, "Clear Baseline fixture was not activated");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(35);
		openClearBaseline(robot);
		SwingUtilities.invokeAndWait(() -> {
			for (java.awt.Window candidate : java.awt.Window.getWindows())
				if (candidate instanceof BaselineDialog found && found.isShowing()) dialog = found;
		});
		assertTrue(dialog != null, "Ribbon Clear Baseline command did not create its dialog");
		GuiAcceptanceSupport.await(() -> dialog.isShowing(), "Clear Baseline dialog did not open");
		SwingUtilities.invokeAndWait(() -> {
			dialog.setAlwaysOnTop(true);
			dialog.toFront();
			dialog.requestFocus();
		});
		GuiAcceptanceSupport.await(dialog::isActive, "Clear Baseline dialog did not become the foreground window");

		Container content = dialog.getContentPane();
		JComboBox<?> baseline = dialog.baseline;
		AbstractButton entire = dialog.entireProject;
		AbstractButton selected = dialog.selectedTasks;
		AbstractButton cancel = dialog.cancel;
		AbstractButton ok = dialog.ok;
		click(robot, entire);
		GuiAcceptanceSupport.await(dialog::isActive, "physical dialog activation did not make Clear Baseline foreground");
		assertControlInsideDialog(dialog, baseline, "baseline selector");
		assertControlInsideDialog(dialog, entire, "Entire Project option");
		assertControlInsideDialog(dialog, selected, "Selected Tasks option");
		assertControlInsideDialog(dialog, cancel, "Cancel button");
		assertControlInsideDialog(dialog, ok, "OK button");
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(content, "Clear Baseline dialog (#715)");
		DialogLayoutAssertions.assertWithinUsableScreen(dialog, "Clear Baseline dialog (#724)");
		DialogLayoutAssertions.assertResizeKeepsTextControls(dialog, content, 100, 40, "Clear Baseline dialog (#724)");
		DialogScreenshotArtifacts.capture(dialog, "issue-460-clear-baseline");
		assertControlInsideDialog(dialog, baseline, "resized baseline selector");
		assertControlInsideDialog(dialog, entire, "resized Entire Project option");
		assertControlInsideDialog(dialog, selected, "resized Selected Tasks option");
		assertControlInsideDialog(dialog, cancel, "resized Cancel button");
		assertControlInsideDialog(dialog, ok, "resized OK button");

		click(robot, selected);
		if (selected.isEnabled()) {
			assertTrue(selected.isSelected(), "Selected Tasks must respond to a physical click when tasks are selected");
		} else {
			assertTrue(!selected.isSelected(), "Selected Tasks must remain unselected while no tasks are selected");
		}
		click(robot, entire);
		assertTrue(entire.isSelected(), "Entire Project must respond to a physical click");
		click(robot, cancel);
		GuiAcceptanceSupport.await(() -> !dialog.isShowing(),
			"Clear Baseline dialog did not close through its visible Cancel button");

		Font largerFont = (originalDefaultFont == null ? new Font(Font.SANS_SERIF, Font.PLAIN, 12) : originalDefaultFont)
			.deriveFont(16f);
		SwingUtilities.invokeAndWait(() -> {
			FlatDarkLaf.setup();
			UIManager.put("defaultFont", largerFont);
			SwingUtilities.updateComponentTreeUI(window);
		});
		openClearBaseline(robot);
		SwingUtilities.invokeAndWait(() -> {
			for (java.awt.Window candidate : java.awt.Window.getWindows())
				if (candidate instanceof BaselineDialog found && found.isShowing()) dialog = found;
		});
		GuiAcceptanceSupport.await(() -> dialog != null && dialog.isShowing(), "Clear Baseline dialog did not reopen through the ribbon after theme/font change");
		assertControlInsideDialog(dialog, dialog.baseline, "reopened baseline selector");
		assertControlInsideDialog(dialog, dialog.entireProject, "reopened Entire Project option");
		assertControlInsideDialog(dialog, dialog.selectedTasks, "reopened Selected Tasks option");
		assertControlInsideDialog(dialog, dialog.cancel, "reopened Cancel button");
		assertControlInsideDialog(dialog, dialog.ok, "reopened OK button");
		DialogLayoutAssertions.assertTextControlsAtPreferredHeight(dialog.getContentPane(),
			"Clear Baseline dialog after reopen, FlatDarkLaf, and larger font (#723)");
		click(robot, dialog.cancel);
		GuiAcceptanceSupport.await(() -> !dialog.isShowing(), "reopened Clear Baseline dialog did not close");
	}

	private void openClearBaseline(Robot robot) throws Exception {
		Component projectTab = GuiAcceptanceSupport.findShowingButtonByText(window,
			java.util.ResourceBundle.getBundle("com.microproject.menu.menu").getString("ProjectRibbonTask.title"));
		ModernRibbonPanel ribbon = (ModernRibbonPanel) window.getRibbonPanel()
			.getClientProperty(ModernRibbonPanel.CONTEXTUAL_TABS_PROPERTY);
		if (!ribbon.isCommandSurfaceVisible()) {
			restoreRibbonCommandsThroughTabMenu(robot, (AbstractButton) projectTab, ribbon);
		}
		if (!((AbstractButton) projectTab).isSelected()) click(robot, projectTab);
		GuiAcceptanceSupport.await(() -> ((AbstractButton) projectTab).isSelected(), "Project ribbon tab did not become selected");
		click(robot, RibbonGuiSupport.findVisibleOrExpand(robot, window.getRibbonPanel(), "RibbonClearBaseline"));
		GuiAcceptanceSupport.await(() -> visibleBaselineDialog() != null, "Project > Clear Baseline did not open its dialog");
	}

	private static void restoreRibbonCommandsThroughTabMenu(Robot robot, AbstractButton tab,
			ModernRibbonPanel ribbon) throws Exception {
		SwingUtilities.invokeAndWait(() -> MenuSelectionManager.defaultManager().clearSelectedPath());
		Point point = tab.getLocationOnScreen();
		robot.mouseMove(point.x + tab.getWidth() / 2, point.y + tab.getHeight() / 2);
		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> findShowingShowRibbonItem() != null,
			"physical ribbon tab menu did not show its Show Ribbon command");
		AbstractButton showRibbon = findShowingShowRibbonItem();
		assertTrue(showRibbon != null && showRibbon.isShowing(),
			"Show Ribbon command must be visible in the active tab context menu");
		click(robot, showRibbon);
		GuiAcceptanceSupport.await(ribbon::isCommandSurfaceVisible,
			"physical Show Ribbon command did not restore the ribbon command surface");
	}

	private static AbstractButton findShowingShowRibbonItem() {
		AtomicReference<AbstractButton> result = new AtomicReference<>();
		try {
			SwingUtilities.invokeAndWait(() -> {
				for (MenuElement selected : MenuSelectionManager.defaultManager().getSelectedPath()) {
					if (!(selected.getComponent() instanceof JPopupMenu popup)
							|| !ModernRibbonPanel.DISPLAY_MODE_POPUP_NAME.equals(popup.getName())
							|| !popup.isShowing()) continue;
					for (Component item : popup.getComponents()) {
						if (item instanceof AbstractButton button && button.isShowing()
								&& UsabilityStrings.text("chrome.ribbonShow").equals(button.getText())) {
							result.set(button);
							return;
						}
					}
				}
			});
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new AssertionError("interrupted while checking ribbon tab popup visibility", interrupted);
		} catch (java.lang.reflect.InvocationTargetException failure) {
			throw new AssertionError("failed to inspect ribbon tab popup on the EDT", failure.getCause());
		}
		return result.get();
	}

	private BaselineDialog visibleBaselineDialog() {
		for (java.awt.Window candidate : java.awt.Window.getWindows())
			if (candidate instanceof BaselineDialog found && found.isShowing()) return found;
		return null;
	}

	private static void assertControlInsideDialog(BaselineDialog dialog, Component control, String name) {
		assertTrue(control != null && control.isShowing() && control.getWidth() > 0 && control.getHeight() > 0,
			name + " must be visible with a non-empty hit area");
		Rectangle dialogBounds = new Rectangle(0, 0, dialog.getRootPane().getWidth(), dialog.getRootPane().getHeight());
		Rectangle bounds = SwingUtilities.convertRectangle(control.getParent(), control.getBounds(), dialog.getRootPane());
		assertTrue(dialogBounds.contains(bounds), name + " is clipped by the dialog content: " + bounds);
	}

	private static void click(Robot robot, Component component) throws Exception {
		java.awt.Point point = component.getLocationOnScreen();
		robot.mouseMove(point.x + component.getWidth() / 2, point.y + component.getHeight() / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}
}

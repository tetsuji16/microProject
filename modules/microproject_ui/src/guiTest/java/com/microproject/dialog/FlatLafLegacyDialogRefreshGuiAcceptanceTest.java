/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.menu.testsupport.UiComponentWalker;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.ui.shell.WindowBoundsSupport;
import com.microproject.util.FlatLafSupport;
import com.microproject.util.FlatUiSupport;

/** Shared visual journey for legacy dialogs and theme reapplication. */
class FlatLafLegacyDialogRefreshGuiAcceptanceTest {
	private JFrame owner;
	private Project project;

	@AfterEach
	void disposeWindows() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Window window : Window.getWindows())
				if (window instanceof JDialog dialog && dialog.isDisplayable()) dialog.dispose();
			if (owner != null) owner.dispose();
		});
	}

	@Test
	void ownedLegacyDialogsRetainTheirFlatLafSurfacesAfterUiRefresh() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for dialog visual acceptance.");
		FlatLafSupport.ensureInitialized();
		project = project();
		NormalTask task = project.createScriptedTask();
		task.setName("Dialog style fixture task");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(25);
		SwingUtilities.invokeAndWait(() -> {
			owner = new JFrame("FlatLaf dialog visual fixture");
			owner.setSize(1000, 640);
			owner.setLocation(30, 30);
			owner.setVisible(true);
		});
		GuiAcceptanceSupport.await(() -> owner.isShowing(), "dialog fixture owner did not become visible");

		List<Supplier<JDialog>> dialogFactories = List.of(
			() -> new CalendarViewDialogBox(owner, project),
			() -> new TimelineDialogBox(owner, project),
			() -> new CustomFieldsDialogBox(owner, project, List.of(task)),
			() -> new CustomReportDialogBox(owner, project),
			() -> TeamPlannerDialogBox.getInstance(owner, project));
		List<String> artifactNames = List.of("calendar", "timeline", "custom-fields", "custom-report", "team-planner");
		Path artifactDirectory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/reports/guiTest-artifacts"));
		Files.createDirectories(artifactDirectory);

		for (int index = 0; index < dialogFactories.size(); index++) {
			int dialogIndex = index;
			JDialog[] current = new JDialog[1];
			SwingUtilities.invokeAndWait(() -> {
				current[0] = dialogFactories.get(dialogIndex).get();
				current[0].setAlwaysOnTop(true);
				current[0].setVisible(true);
				WindowBoundsSupport.fitWithinUsableScreen(current[0]);
				current[0].toFront();
				current[0].requestFocus();
			});
			JDialog dialog = current[0];
			GuiAcceptanceSupport.await(dialog::isShowing, artifactNames.get(dialogIndex) + " dialog did not become visible");
			GuiAcceptanceSupport.await(() -> dialog.getWidth() > 300 && dialog.getHeight() > 200,
				artifactNames.get(dialogIndex) + " dialog did not receive usable bounds");

			SwingUtilities.invokeAndWait(() -> FlatUiSupport.updateComponentTreeUI(owner));
			GuiAcceptanceSupport.await(() -> dialog.isShowing() && dialog.getWidth() > 300,
				artifactNames.get(dialogIndex) + " dialog did not survive owner theme refresh");
			assertDialogStyle(dialog, artifactNames.get(dialogIndex));
			Rectangle bounds = boundsOnScreen(dialog);
			robot.waitForIdle();
			var screenshot = robot.createScreenCapture(bounds);
			javax.imageio.ImageIO.write(screenshot, "png", artifactDirectory
				.resolve("flatlaf-" + artifactNames.get(dialogIndex) + "-after-ui-refresh.png").toFile());

			Action escapeAction = escapeAction(dialog);
			assertNotNull(escapeAction, artifactNames.get(dialogIndex) + " Escape action after theme refresh");
			SwingUtilities.invokeAndWait(() -> escapeAction.actionPerformed(
				new ActionEvent(dialog.getRootPane(), ActionEvent.ACTION_PERFORMED, "ESCAPE")));
			GuiAcceptanceSupport.await(() -> !dialog.isShowing(), artifactNames.get(dialogIndex) + " Escape action did not close the dialog");
			SwingUtilities.invokeAndWait(dialog::dispose);
		}
	}

	private static void assertDialogStyle(JDialog dialog, String name) {
		assertTrue(dialog.getRootPane().getBorder() instanceof LineBorder,
			name + " dialog must retain the shared root border");
		LineBorder border = (LineBorder) dialog.getRootPane().getBorder();
		assertEquals(FlatUiSupport.borderColor(), border.getLineColor(), name + " root border color");
		assertEquals(FlatUiSupport.panelBackground(), dialog.getContentPane().getBackground(),
			name + " content surface after theme refresh");
		KeyStroke escape = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0);
		Object escapeActionKey = dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(escape);
		assertNotNull(escapeActionKey, name + " Escape binding after theme refresh");
		assertNotNull(dialog.getRootPane().getActionMap().get(escapeActionKey), name + " Escape action after theme refresh");

		List<JScrollPane> scrollPanes = new ArrayList<>();
		List<JTable> tables = new ArrayList<>();
		List<AbstractButton> buttons = new ArrayList<>();
		for (java.awt.Component component : UiComponentWalker.flatten(dialog)) {
			if (component instanceof JScrollPane pane) scrollPanes.add(pane);
			if (component instanceof JTable table) tables.add(table);
			if (component instanceof AbstractButton button) buttons.add(button);
		}
		assertTrue(!scrollPanes.isEmpty(), name + " must expose its main scroll surface");
		for (JScrollPane pane : scrollPanes)
			assertEquals(FlatUiSupport.viewportBackground(), pane.getViewport().getBackground(), name + " viewport color");
		for (JTable table : tables) {
			assertEquals(FlatUiSupport.dataSurfaceBackground(), table.getBackground(), name + " table surface");
			assertEquals(FlatUiSupport.tableSelectionBackground(), table.getSelectionBackground(), name + " table selection");
		}
		assertTrue(!buttons.isEmpty(), name + " must expose styled controls");
		for (AbstractButton button : buttons)
			assertEquals(FlatUiSupport.uiFont(), button.getFont(), name + " button font after theme refresh");
	}

	private static Action escapeAction(JDialog dialog) {
		KeyStroke escape = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0);
		Object key = dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(escape);
		return key == null ? null : dialog.getRootPane().getActionMap().get(key);
	}

	private static Rectangle boundsOnScreen(Window window) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			java.awt.Point location = window.getLocationOnScreen();
			bounds[0] = new Rectangle(location.x, location.y, window.getWidth(), window.getHeight());
		});
		return bounds[0];
	}

	private static Project project() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("flatlaf-dialog-acceptance", undo), undo);
		project.initialize(false, false);
		project.setName("FlatLaf dialog acceptance");
		project.setFileName("C:/gui-fixtures/flatlaf-dialog-acceptance.mpo");
		return project;
	}
}

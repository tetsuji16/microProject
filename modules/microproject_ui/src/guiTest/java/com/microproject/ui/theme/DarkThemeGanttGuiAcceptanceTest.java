/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.options.CalendarOption;
import com.microproject.ui.frame.DocumentFrame;
import com.microproject.ui.frame.GraphicManager;
import com.microproject.ui.frame.MainRibbonFrame;
import com.microproject.ui.view.timescale.ScaledScrollPane;
import com.microproject.ui.view.views.GanttView;
import com.microproject.model.resource.ResourcePool;
import com.microproject.model.task.NormalTask;
import com.microproject.model.task.Project;
import com.microproject.ui.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.ui.util.FlatLafSupport;
import com.microproject.ui.util.FlatUiSupport;

/**
 * Keeps the dark-theme Gantt evidence attached to issue #483: the production
 * Gantt view must follow a run-time theme switch without a restart.
 *
 * <p>The chart colours carry business meaning (bar, progress, critical path) and
 * stay with the Microsoft Project palette. The surfaces that frame them, the
 * time-scale header, the table column headers and both viewports, must be
 * repainted from the active theme tokens; those are the ones asserted here and
 * captured as screenshots for review.
 */
class DarkThemeGanttGuiAcceptanceTest {
	private JFrame frame;
	private GraphicManager graphicManager;
	private boolean originalDark;

	@AfterEach
	void restoreThemeAndDisposeWindows() throws Exception {
		ThemeManager.applyTheme(originalDark);
		SwingUtilities.invokeAndWait(() -> {
			for (java.awt.Window window : java.awt.Window.getWindows())
				if (window.isDisplayable())
					window.dispose();
		});
		if (graphicManager != null) {
			SwingUtilities.invokeAndWait(() -> graphicManager.cleanUp());
			graphicManager = null;
		}
	}

	@Test
	void ganttTimeScaleAndTaskColumnsFollowARunTimeThemeSwitch() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for the dark-theme Gantt evidence.");
		FlatLafSupport.ensureInitialized();
		originalDark = FlatUiSupport.isDarkTheme();
		ThemeManager.applyTheme(false);

		Project project = project();
		GanttView[] view = new GanttView[1];
		SwingUtilities.invokeAndWait(() -> {
			MainRibbonFrame mainWindow = new MainRibbonFrame("Dark theme Gantt acceptance", null);
			frame = mainWindow;
			graphicManager = new GraphicManager(mainWindow);
			mainWindow.setGraphicManager(graphicManager);
			graphicManager.initView();
			DocumentFrame document = graphicManager.addProjectFrame(project);
			mainWindow.setBounds(40, 40, 1180, 720);
			mainWindow.setAlwaysOnTop(true);
			mainWindow.setVisible(true);
			document.activateGanttView();
			view[0] = (GanttView) document.getActiveTopView();
		});
		GuiAcceptanceSupport.await(() -> frame.isShowing() && view[0] != null && view[0].isShowing(),
			"the production Gantt view did not become visible");
		Robot robot = new com.microproject.ui.testsupport.GuiRobot();
		robot.setAutoDelay(30);
		GuiAcceptanceSupport.activateAndAwaitRenderedWindow(frame, frame.getContentPane(), robot, true,
			"dark theme Gantt");
		GuiAcceptanceSupport.await(() -> view[0].getGantt().isShowing() && view[0].getSpreadSheet().isShowing(),
			"the Gantt chart and the task table must both be visible");

		JComponent timeScale = timeScaleView(view[0]);
		assertNotNull(timeScale, "the Gantt view must host a time-scale header");

		SurfaceSnapshot light = snapshot(view[0], timeScale, robot);
		capture(robot, "theme-switch-gantt-light.png");

		ThemeManager.applyTheme(true);
		GuiAcceptanceSupport.await(() -> FlatUiSupport.isDarkTheme(), "the dark theme was not applied");
		GuiAcceptanceSupport.await(() -> frame.getContentPane().isValid(), "the frame did not validate after the dark switch");
		GuiAcceptanceSupport.awaitInputAndEventQueueIdle(robot, "the dark theme repaint");
		capture(robot, "theme-switch-gantt-dark.png");
		SurfaceSnapshot dark = snapshot(view[0], timeScale, robot);

		assertNotEquals(light.timeScale(), dark.timeScale(), "the time-scale header kept its light surface");
		assertNotEquals(light.tableHeader(), dark.tableHeader(), "the task-table column headers kept their light surface");
		assertNotEquals(light.ganttViewport(), dark.ganttViewport(), "the Gantt viewport kept its light surface");
		assertEquals(FlatUiSupport.spreadsheetHeaderBackground(), dark.timeScale(),
			"the dark time-scale header must use the active header token");
		assertEquals(FlatUiSupport.spreadsheetHeaderBackground(), dark.tableHeader(),
			"the dark task-table column headers must use the active header token");
		assertEquals(FlatUiSupport.viewportBackground(), dark.ganttViewport(),
			"the dark Gantt viewport must use the active viewport token");
		assertEquals(FlatUiSupport.viewportBackground(), dark.tableViewport(),
			"the dark task-table viewport must use the active viewport token");
		assertEquals(dark.timeScale(), dark.timeScaleRenderedBackground(),
			"the painted time-scale background must match the dark header token");
		assertTrue(luminance(dark.timeScaleRenderedBackground()) < 0.45d,
			"the painted time-scale header must actually be dark, was " + hex(dark.timeScaleRenderedBackground()));

		ThemeManager.applyTheme(false);
		GuiAcceptanceSupport.await(() -> !FlatUiSupport.isDarkTheme(), "the light theme was not restored");
		GuiAcceptanceSupport.awaitInputAndEventQueueIdle(robot, "the light theme repaint");
		SurfaceSnapshot restored = snapshot(view[0], timeScale, robot);
		assertEquals(light.timeScale(), restored.timeScale(), "the time-scale header did not return to its light token");
		assertEquals(light.tableHeader(), restored.tableHeader(), "the task-table headers did not return to their light token");
		assertEquals(light.ganttViewport(), restored.ganttViewport(), "the Gantt viewport did not return to its light token");
		capture(robot, "theme-switch-gantt-light-restored.png");

		Path directory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/reports/guiTest-artifacts"));
		Files.createDirectories(directory);
		Files.writeString(directory.resolve("theme-switch-gantt.txt"),
			"Issue #483 dark theme Gantt evidence\n"
				+ "lightTimeScale=" + hex(light.timeScale()) + " darkTimeScale=" + hex(dark.timeScale()) + '\n'
				+ "lightTableHeader=" + hex(light.tableHeader()) + " darkTableHeader=" + hex(dark.tableHeader()) + '\n'
				+ "lightGanttViewport=" + hex(light.ganttViewport()) + " darkGanttViewport=" + hex(dark.ganttViewport()) + '\n'
				+ "lightTableHeaderToken=" + hex(FlatUiSupport.spreadsheetHeaderBackground()) + '\n'
				+ "darkRenderedTimeScale=" + hex(dark.timeScaleRenderedBackground()) + '\n',
			StandardCharsets.UTF_8);
	}

	private static JComponent timeScaleView(GanttView view) {
		ScaledScrollPane pane = (ScaledScrollPane) SwingUtilities.getAncestorOfClass(ScaledScrollPane.class, view.getGantt());
		return pane == null ? null : (JComponent) pane.getTimeScaleComponent();
	}

	private static SurfaceSnapshot snapshot(GanttView view, JComponent timeScale, Robot robot) throws Exception {
		Color[] colors = new Color[5];
		SwingUtilities.invokeAndWait(() -> {
			JTableHeader header = view.getSpreadSheet().getTableHeader();
			JScrollPane tablePane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, view.getSpreadSheet());
			JScrollPane ganttPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, view.getGantt());
			colors[0] = timeScale.getBackground();
			colors[1] = header.getBackground();
			colors[2] = ganttPane.getViewport().getBackground();
			colors[3] = tablePane.getViewport().getBackground();
			colors[4] = renderedBackground(timeScale);
		});
		robot.waitForIdle();
		return new SurfaceSnapshot(colors[0], colors[1], colors[2], colors[3], colors[4]);
	}

	/** Most frequent pixel of a painted component, which is its background. */
	private static Color renderedBackground(JComponent component) {
		int width = Math.max(1, component.getWidth());
		int height = Math.max(1, component.getHeight());
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		try {
			component.printAll(graphics);
		} finally {
			graphics.dispose();
		}
		Map<Integer, Integer> counts = new HashMap<>();
		for (int y = 0; y < height; y++)
			for (int x = 0; x < width; x++)
				counts.merge(image.getRGB(x, y) & 0xFFFFFF, 1, Integer::sum);
		int dominant = counts.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
		return new Color(dominant);
	}

	private void capture(Robot robot, String fileName) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(frame.getRootPane().getLocationOnScreen(),
			frame.getRootPane().getSize()));
		BufferedImage screenshot = robot.createScreenCapture(bounds[0]);
		Path directory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/reports/guiTest-artifacts"));
		Files.createDirectories(directory);
		ImageIO.write(screenshot, "png", directory.resolve(fileName).toFile());
		assertTrue(screenshot.getWidth() > 400 && screenshot.getHeight() > 300,
			"the captured Gantt window is unexpectedly small");
	}

	private static double luminance(Color color) {
		return (0.2126d * color.getRed() + 0.7152d * color.getGreen() + 0.0722d * color.getBlue()) / 255.0d;
	}

	private static String hex(Color color) {
		return color == null ? "null" : String.format("#%06x", color.getRGB() & 0xFFFFFF);
	}

	private static Project project() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("dark-theme-gantt", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		long day = CalendarOption.getInstance().getMillisPerDay();
		for (int index = 1; index <= 5; index++) {
			NormalTask task = project.createScriptedTask();
			task.setName("Theme task " + index);
			task.getCurrentSchedule().setStart(project.getStart());
			task.setDuration(day * index);
		}
		project.recalculate();
		return project;
	}

	private record SurfaceSnapshot(Color timeScale, Color tableHeader, Color ganttViewport, Color tableViewport,
		Color timeScaleRenderedBackground) { }
}

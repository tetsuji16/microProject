/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.configuration.Dictionary;
import com.microproject.dialog.CriticalChainStatusDialogBox;
import com.microproject.dialog.UsabilityStrings;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.pm.ccpm.CriticalChainService;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.task.Task;
import com.microproject.testsupport.GuiAcceptanceSupport;

/** Robot acceptance coverage for the checked-in CCPM sample's visible progress state. */
class CcpmSampleProgressGuiAcceptanceTest {
	private JFrame frame;
	private Gantt gantt;
	private DialogObserver observer;

	@AfterEach
	void closeWindows() throws Exception {
		if (observer != null) observer.close();
		for (Window window : Window.getWindows()) {
			if (window instanceof CriticalChainStatusDialogBox && window.isDisplayable()) {
				SwingUtilities.invokeAndWait(window::dispose);
			}
		}
		if (frame != null) SwingUtilities.invokeAndWait(() -> { frame.dispose(); frame = null; });
		if (gantt != null) { gantt.cleanUp(); gantt = null; }
	}

	@Test
	void mixedProgressSampleRendersInGanttAndCcpmStatusDialogs() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Project project = loadSample();
		assertEquals(1D, task(project, "要件定義").getPercentComplete(), 0.00001D);
		assertEquals(0.75D, task(project, "基幹機能の実装").getPercentComplete(), 0.00001D);
		assertEquals(0D, task(project, "本番リリース").getPercentComplete(), 0.00001D);

		showGantt(project);
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		GuiAcceptanceSupport.await(() -> frame.isShowing() && gantt.isShowing(), "sample Gantt window did not become visible");
		assertTrue(hasRenderedGanttNode(), "the sample Gantt must render task bars");
		capture(robot, frame, "ccpm-sample-progress-gantt.png");

		CriticalChainService service = new CriticalChainService();
		CriticalChainService.Settings settings = service.settings(project);
		settings.setEnabled(true);
		service.apply(project, null, settings);
		assertNotNull(service.findBaseline(project), "CCPM apply must establish a baseline before opening status dialogs");
		assertTrue(project.getPercentComplete() > 0D && project.getPercentComplete() < 1D,
			"mixed task progress must produce an in-progress project percentage");
		showAndCapture(robot, project, CriticalChainStatusDialogBox.Surface.NETWORK,
			CriticalChainGraphPanel.class, "ccpm-sample-progress-network.png");
		showAndCapture(robot, project, CriticalChainStatusDialogBox.Surface.BUFFER_STATUS,
			CriticalChainBufferChartPanel.class, "ccpm-sample-progress-buffer.png");
	}

	private void showGantt(Project project) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()), "ccpm-sample-progress", null);
			SpreadSheetUtils.setFieldsAndContext(sheet, cache, SpreadSheetCategories.taskSpreadsheetCategory,
				"Spreadsheet.Task.entry", true);
			gantt = new Gantt(project, "Gantt");
			gantt.setCache(cache);
			gantt.setCoord(new CoordinatesConverter(project));
			gantt.setBarStyles((BarStyles) Dictionary.get(BarStyles.category, "standard"));
			frame = new JFrame("microProject — CCPM 標準システム導入");
			JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(sheet), new JScrollPane(gantt));
			split.setResizeWeight(0.46);
			frame.add(split);
			frame.setPreferredSize(new Dimension(1180, 640));
			frame.pack();
			frame.setLocationByPlatform(true);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
			gantt.updateSize();
		});
	}

	private void showAndCapture(Robot robot, Project project, CriticalChainStatusDialogBox.Surface surface,
		Class<? extends Component> expected, String artifact) throws Exception {
		observer = new DialogObserver();
		observer.open();
		SwingUtilities.invokeLater(() -> CriticalChainStatusDialogBox.show(frame, project, surface));
		CriticalChainStatusDialogBox dialog = observer.awaitDialog();
		GuiAcceptanceSupport.await(() -> visibleComponentExists(dialog, expected), "CCPM dialog did not render " + expected.getSimpleName());
		capture(robot, dialog, artifact);
		clickClose(robot, dialog);
		GuiAcceptanceSupport.await(() -> !dialog.isShowing(), "CCPM dialog did not close after the Robot click");
		observer.close();
		observer = null;
	}

	private boolean hasRenderedGanttNode() throws Exception {
		boolean[] rendered = new boolean[1];
		SwingUtilities.invokeAndWait(() -> {
			for (int y = 0; y < Math.min(gantt.getHeight(), gantt.getRowHeight() * 20) && !rendered[0]; y += 2) {
				for (int x = 0; x < Math.min(gantt.getWidth(), 1200) && !rendered[0]; x += 2) rendered[0] = gantt.getUI().getNodeAt(x, y) != null;
			}
		});
		return rendered[0];
	}

	private static void clickClose(Robot robot, CriticalChainStatusDialogBox dialog) throws Exception {
		AbstractButton button = findButton(dialog);
		assertNotNull(button, "CCPM status dialog must expose a close button");
		Rectangle bounds = new Rectangle();
		SwingUtilities.invokeAndWait(() -> {
			java.awt.Point point = button.getLocationOnScreen();
			bounds.setBounds(point.x, point.y, button.getWidth(), button.getHeight());
		});
		robot.mouseMove(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	private static AbstractButton findButton(java.awt.Container parent) {
		for (Component component : parent.getComponents()) {
			if (component instanceof AbstractButton button && button.isShowing()
				&& UsabilityStrings.text("common.close").equals(button.getText())) return button;
			if (component instanceof java.awt.Container nested) {
				AbstractButton button = findButton(nested);
				if (button != null) return button;
			}
		}
		return null;
	}

	private static boolean visibleComponentExists(Window window, Class<? extends Component> type) {
		AtomicReference<Boolean> visible = new AtomicReference<>(false);
		try {
			SwingUtilities.invokeAndWait(() -> {
				Deque<Component> pending = new ArrayDeque<>(); pending.add(window);
				while (!pending.isEmpty()) {
					Component component = pending.removeFirst();
					if (type.isInstance(component) && component.isShowing()) { visible.set(true); return; }
					if (component instanceof java.awt.Container container) for (Component child : container.getComponents()) pending.addLast(child);
				}
			});
		} catch (Exception exception) { throw new IllegalStateException("Could not inspect CCPM dialog", exception); }
		return visible.get();
	}

	private static void capture(Robot robot, Window window, String fileName) throws Exception {
		Rectangle bounds = new Rectangle();
		SwingUtilities.invokeAndWait(() -> {
			window.toFront(); window.requestFocus();
			RootPaneContainer rootPaneContainer = (RootPaneContainer) window;
			java.awt.Point point = rootPaneContainer.getRootPane().getLocationOnScreen();
			bounds.setBounds(point.x, point.y, rootPaneContainer.getRootPane().getWidth(), rootPaneContainer.getRootPane().getHeight());
		});
		BufferedImage image = robot.createScreenCapture(bounds);
		Path directory = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		ImageIO.write(image, "png", directory.resolve(fileName).toFile());
		assertTrue(image.getWidth() > 400 && image.getHeight() > 300, "captured UI is unexpectedly small");
	}

	private static Project loadSample() throws Exception {
		File sample = null;
		for (String prefix : new String[] { "samples", "../samples", "../../samples" }) {
			File candidate = Path.of(prefix, "CCPM 標準システム導入 20タスク.mpo").toFile();
			if (candidate.isFile()) { sample = candidate; break; }
		}
		assertTrue(sample.isFile(), "checked-in CCPM sample is missing");
		MpoFileImporter importer = new MpoFileImporter();
		importer.setFileName(sample.getAbsolutePath());
		importer.setProjectFactory(ProjectFactory.getInstance());
		importer.importFile();
		return importer.getProject();
	}

	private static Task task(Project project, String name) {
		for (java.util.Iterator<?> tasks = project.getTaskOutlineIterator(); tasks.hasNext();) {
			Task task = (Task) tasks.next();
			if (name.equals(task.getName())) return task;
		}
		throw new AssertionError("Missing task: " + name);
	}

	private static final class DialogObserver implements AWTEventListener {
		private final AtomicReference<CriticalChainStatusDialogBox> dialog = new AtomicReference<>();
		void open() { Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.WINDOW_EVENT_MASK); }
		void close() { Toolkit.getDefaultToolkit().removeAWTEventListener(this); }
		CriticalChainStatusDialogBox awaitDialog() throws Exception {
			GuiAcceptanceSupport.await(() -> dialog.get() != null, "CCPM status dialog did not open");
			return dialog.get();
		}
		@Override public void eventDispatched(AWTEvent event) {
			if (event instanceof WindowEvent windowEvent && windowEvent.getID() == WindowEvent.WINDOW_OPENED
				&& windowEvent.getWindow() instanceof CriticalChainStatusDialogBox statusDialog) dialog.compareAndSet(null, statusDialog);
		}
	}
}

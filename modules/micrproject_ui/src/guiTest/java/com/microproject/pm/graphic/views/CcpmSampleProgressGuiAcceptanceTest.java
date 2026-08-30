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
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.configuration.Dictionary;
import com.microproject.field.Field;
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
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.task.Task;
import com.microproject.options.CalendarOption;
import com.microproject.grouping.core.Node;
import com.microproject.testsupport.GuiAcceptanceSupport;

/** Robot acceptance coverage for the checked-in CCPM sample's visible progress state. */
class CcpmSampleProgressGuiAcceptanceTest {
	private JFrame frame;
	private Gantt gantt;
	private SpreadSheet sheet;
	private Project project;
	private final Map<String, Integer> taskRows = new LinkedHashMap<>();
	private int durationColumn;
	private int percentCompleteColumn;
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
		showGantt(project);
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		GuiAcceptanceSupport.await(() -> frame.isShowing() && gantt.isShowing(), "sample Gantt window did not become visible");
		assertTrue(hasRenderedGanttNode(), "the sample Gantt must render task bars");
		capture(robot, frame, "ccpm-sample-progress-gantt.png");

		// The checked-in sample begins at a realistic mixed-progress checkpoint.
		// Rewind it through the visible tracking sheet for this new-project scenario.
		setAllLeafTaskProgressThroughVisibleSpreadsheet(robot, 0D);
		assertEquals(0D, project.getPercentComplete(), 0.00001D);

		CriticalChainService service = new CriticalChainService();
		CriticalChainService.Settings settings = service.settings(project);
		settings.setEnabled(true);
		service.apply(project, null, settings);
		assertNotNull(service.findBaseline(project), "CCPM apply must establish a baseline before opening status dialogs");
		showAndCapture(robot, project, CriticalChainStatusDialogBox.Surface.NETWORK,
			CriticalChainGraphPanel.class, "ccpm-sample-progress-network.png");
		showBufferTransitionScenario(robot, project, service);
	}

	private void showGantt(Project project) throws Exception {
		this.project = project;
		SwingUtilities.invokeAndWait(() -> {
			sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()), "ccpm-sample-progress", null);
			SpreadSheetUtils.setFieldsAndContext(sheet, cache, SpreadSheetCategories.taskSpreadsheetCategory,
				"Spreadsheet.Task.summary", true);
			for (var iterator = project.getTaskOutlineIterator(); iterator.hasNext();) {
				Task task = (Task) iterator.next();
				if (!task.isSummary()) {
					Node node = (Node) cache.getModel().search(task);
					taskRows.put(task.getName(), ((SpreadSheetModel) sheet.getModel()).findGraphicNodeRow(cache.getGraphicNode(node)));
				}
			}
			durationColumn = findColumn(sheet, "Field.duration");
			percentCompleteColumn = findColumn(sheet, "Field.percentComplete");
			gantt = new Gantt(project, "Gantt");
			gantt.setCache(cache);
			gantt.setCoord(new CoordinatesConverter(project));
			gantt.setBarStyles((BarStyles) Dictionary.get(BarStyles.category, "standard"));
			frame = new JFrame("microProject — CCPM 標準システム導入");
			JComponent root = frame.getRootPane();
			root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "EditField");
			ActionMap actions = root.getActionMap();
			actions.put("EditField", new AbstractAction() {
				@Override public void actionPerformed(java.awt.event.ActionEvent event) { sheet.editActiveCell(); }
			});
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

	private void showBufferTransitionScenario(Robot robot, Project project, CriticalChainService service) throws Exception {
		CriticalChainService.Analysis safeStart = service.preview(project, null, service.findSettings(project));
		assertEquals(CriticalChainService.BufferStatus.GREEN, safeStart.projectBuffer().status(),
			"the baseline state must start in the safe zone");
		CriticalChainStatusDialogBox dialog = openBufferDialog(project);
		CriticalChainBufferChartPanel chart = findComponent(dialog, CriticalChainBufferChartPanel.class);
		assertNotNull(chart, "CCPM buffer chart is missing");
		assertEquals(1, CriticalChainBufferChartPanel.observationCount(chart), "the initial safe observation must be visible");
		assertEquals(0D, project.getPercentComplete(), 0.00001D, "the first chart point must be 0% / 0%");
		closeDialog(robot, dialog);

		Task completion = task(project, "プロジェクト完了");
		long originalDuration = completion.getDuration();
		int originalDays = (int) (originalDuration / CalendarOption.getInstance().getMillisPerDay());

		setAllLeafTaskProgressThroughVisibleSpreadsheet(robot, 0.25D);
		editDurationThroughVisibleSpreadsheet(robot, "プロジェクト完了", originalDays + 1);
		CriticalChainService.Analysis earlyDelay = service.analysis(project);
		assertTrue(project.getPercentComplete() > 0D && earlyDelay.projectBuffer().consumptionRatio() > 0D,
			"an early delivery checkpoint must move right and consume a small part of the buffer");
		dialog = openBufferDialog(project);
		chart = findComponent(dialog, CriticalChainBufferChartPanel.class);
		assertEquals(2, CriticalChainBufferChartPanel.observationCount(chart), "the early checkpoint must follow 0% / 0%");
		closeDialog(robot, dialog);

		setAllLeafTaskProgressThroughVisibleSpreadsheet(robot, 0.50D);
		editDurationThroughVisibleSpreadsheet(robot, "プロジェクト完了", originalDays + 2);
		CriticalChainService.Analysis midDelay = service.analysis(project);
		assertTrue(project.getPercentComplete() > 0.40D && midDelay.projectBuffer().consumptionRatio() > earlyDelay.projectBuffer().consumptionRatio(),
			"a mid-project integration delay must visibly increase buffer use");
		dialog = openBufferDialog(project);
		chart = findComponent(dialog, CriticalChainBufferChartPanel.class);
		assertEquals(3, CriticalChainBufferChartPanel.observationCount(chart), "the mid-project checkpoint must be retained");
		closeDialog(robot, dialog);

		setAllLeafTaskProgressThroughVisibleSpreadsheet(robot, 0.75D);
		editDurationThroughVisibleSpreadsheet(robot, "プロジェクト完了", originalDays + 1);
		CriticalChainService.Analysis recovery = service.analysis(project);
		assertTrue(project.getPercentComplete() > 0.65D && recovery.projectBuffer().consumptionRatio() < midDelay.projectBuffer().consumptionRatio(),
			() -> "resolving part of the integration delay must reduce buffer use while delivery keeps progressing: early="
				+ earlyDelay.projectBuffer().consumptionRatio() + ", mid=" + midDelay.projectBuffer().consumptionRatio()
				+ ", recovery=" + recovery.projectBuffer().consumptionRatio());
		dialog = openBufferDialog(project);
		chart = findComponent(dialog, CriticalChainBufferChartPanel.class);
		assertEquals(4, CriticalChainBufferChartPanel.observationCount(chart),
			"the chart must retain the initial, early, mid-project, and recovery checkpoints");
		capture(robot, dialog, "ccpm-sample-progress-buffer-transition.png");
		closeDialog(robot, dialog);
	}

	private CriticalChainStatusDialogBox openBufferDialog(Project project) throws Exception {
		observer = new DialogObserver(); observer.open();
		SwingUtilities.invokeLater(() -> CriticalChainStatusDialogBox.show(frame, project, CriticalChainStatusDialogBox.Surface.BUFFER_STATUS));
		CriticalChainStatusDialogBox dialog = observer.awaitDialog();
		GuiAcceptanceSupport.await(() -> visibleComponentExists(dialog, CriticalChainBufferChartPanel.class), "CCPM buffer dialog did not render its chart");
		return dialog;
	}

	private void closeDialog(Robot robot, CriticalChainStatusDialogBox dialog) throws Exception {
		clickClose(robot, dialog);
		GuiAcceptanceSupport.await(() -> !dialog.isShowing(), "CCPM dialog did not close after the Robot click");
		observer.close(); observer = null;
	}

	private void setAllLeafTaskProgressThroughVisibleSpreadsheet(Robot robot, double progress) throws Exception {
		for (String taskName : taskRows.keySet()) {
			editThroughVisibleSpreadsheet(robot, taskName, percentCompleteColumn, Math.round(progress * 100D) + "%");
		}
	}

	private void editDurationThroughVisibleSpreadsheet(Robot robot, String taskName, int days) throws Exception {
		editThroughVisibleSpreadsheet(robot, taskName, durationColumn, String.valueOf(days));
		assertEquals(days * CalendarOption.getInstance().getMillisPerDay(), task(project, taskName).getDuration());
	}

	private void editThroughVisibleSpreadsheet(Robot robot, String taskName, int column, String value) throws Exception {
		Rectangle cell = new Rectangle();
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront(); frame.requestFocus(); sheet.requestFocusInWindow();
			Integer row = taskRows.get(taskName);
			assertNotNull(row, "missing visible task row: " + taskName);
			sheet.changeSelection(row.intValue(), column, false, false);
			Rectangle bounds = sheet.getCellRect(row.intValue(), column, true);
			java.awt.Point point = sheet.getLocationOnScreen();
			cell.setBounds(point.x + bounds.x, point.y + bounds.y, bounds.width, bounds.height);
		});
		robot.mouseMove(cell.x + cell.width / 2, cell.y + cell.height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK); robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(sheet::isFocusOwner, "the duration cell did not receive focus");
		SwingUtilities.invokeAndWait(() -> KeyboardFocusManager.getCurrentKeyboardFocusManager().dispatchEvent(
			new KeyEvent(sheet, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_F2, KeyEvent.CHAR_UNDEFINED)));
		GuiAcceptanceSupport.await(sheet::isEditing, "F2 did not start visible duration editing");
		SwingUtilities.invokeAndWait(() -> {
			((JTextComponent) sheet.getEditorComponent()).setText(value);
			assertTrue(sheet.getCellEditor().stopCellEditing(), "visible scenario editor rejected: " + value);
		});
		GuiAcceptanceSupport.await(() -> !sheet.isEditing(), "duration edit did not commit");
	}

	private static int findColumn(SpreadSheet sheet, String fieldId) {
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		for (int modelColumn = 0; modelColumn < model.getColumnCount(); modelColumn++) {
			Field field = model.getFieldInColumn(modelColumn);
			if (field != null && fieldId.equals(field.getId())) return sheet.convertColumnIndexToView(modelColumn);
		}
		throw new IllegalArgumentException("Sample task table has no " + fieldId + " column");
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

	private static <T extends Component> T findComponent(java.awt.Container parent, Class<T> type) {
		for (Component component : parent.getComponents()) {
			if (type.isInstance(component)) return type.cast(component);
			if (component instanceof java.awt.Container nested) {
				T found = findComponent(nested, type);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static void capture(Robot robot, Window window, String fileName) throws Exception {
		Rectangle bounds = new Rectangle();
		SwingUtilities.invokeAndWait(() -> {
			Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
			window.setLocation(screen.x + Math.max(0, (screen.width - window.getWidth()) / 2),
				screen.y + Math.max(0, (screen.height - window.getHeight()) / 2));
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

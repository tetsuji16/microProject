/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;
import java.awt.event.InputEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;
import javax.swing.AbstractButton;
import javax.swing.JFileChooser;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.assignment.Assignment;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.pm.ccpm.CriticalChainService;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.graphic.views.CriticalChainBufferChartPanel;
import com.microproject.pm.graphic.views.CriticalChainGraphPanel;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.SwingFileChooserProvider;
import com.microproject.util.UiServices;

/**
 * Non-headless regression coverage for the CCPM result windows.  The analysis
 * is deliberately applied before the modal dialog opens, so it is cached and
 * can complete before the dialog becomes displayable (the timing that caused
 * the former blank-dialog defect).
 */
class CriticalChainStatusDialogGuiAcceptanceTest {
	private DialogObserver observer;
	private UiServices.FileChooserProvider previousChooser;
	private String previousSystemChooserProperty;
	private Path exportDirectory;

	@AfterEach
	void closeDialogs() throws Exception {
		if (observer != null) observer.close();
		UiServices.setFileChooserProvider(previousChooser);
		if (previousSystemChooserProperty == null) System.clearProperty("flatlaf.useSystemFileChooser");
		else System.setProperty("flatlaf.useSystemFileChooser", previousSystemChooserProperty);
		if (exportDirectory != null) {
			try (var files = Files.list(exportDirectory)) {
				for (Path file : files.toList()) Files.deleteIfExists(file);
			}
			Files.deleteIfExists(exportDirectory);
		}
		for (Window window : Window.getWindows()) {
			if ((window instanceof CriticalChainStatusDialogBox || window instanceof ResourceLevelingDialogBox)
				&& window.isDisplayable()) {
				SwingUtilities.invokeAndWait(window::dispose);
			}
		}
	}

	@Test
	void robotExportsCcpmCsvThroughSharedChooserAndCancelHtmlWithoutCreatingOutput() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for chooser acceptance coverage.");
		previousChooser = UiServices.getFileChooserProvider();
		previousSystemChooserProperty = System.getProperty("flatlaf.useSystemFileChooser");
		System.setProperty("flatlaf.useSystemFileChooser", "false");
		UiServices.setFileChooserProvider(new SwingFileChooserProvider());
		exportDirectory = Files.createTempDirectory("ccpm-report-chooser-");
		Project project = newProjectWithTasks();
		CriticalChainService service = new CriticalChainService();
		CriticalChainService.Settings settings = service.settings(project);
		settings.setEnabled(true);
		service.apply(project, null, settings);
		CriticalChainStatusDialogBox dialog = openStatusDialog(project);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		click(robot, findButton(dialog, "CSV"));
		GuiAcceptanceSupport.await(() -> visibleFileChooser() != null, "CCPM CSV export did not open the shared chooser");
		Window csvChooser = visibleFileChooser();
		JFileChooser csvComponent = findFileChooser(csvChooser);
		assertEquals("CSV (*.csv)", csvComponent.getFileFilter().getDescription(), "CCPM CSV export must select its CSV filter");
		SwingUtilities.invokeAndWait(() -> csvComponent.setSelectedFile(exportDirectory.resolve("buffer-report").toFile()));
		click(robot, findApproveButton(csvChooser));
		GuiAcceptanceSupport.await(() -> Files.exists(exportDirectory.resolve("buffer-report.csv")),
			"CCPM CSV approve did not write a .csv report");
		assertTrue(Files.size(exportDirectory.resolve("buffer-report.csv")) > 0, "CCPM CSV report must contain output");

		click(robot, findButton(dialog, "HTML"));
		GuiAcceptanceSupport.await(() -> visibleFileChooser() != null, "CCPM HTML export did not open the shared chooser");
		Window htmlChooser = visibleFileChooser();
		JFileChooser htmlApproveComponent = findFileChooser(htmlChooser);
		assertEquals("HTML (*.html)", htmlApproveComponent.getFileFilter().getDescription(), "CCPM HTML export must select its HTML filter");
		SwingUtilities.invokeAndWait(() -> htmlApproveComponent.setSelectedFile(exportDirectory.resolve("buffer-report").toFile()));
		click(robot, findApproveButton(htmlChooser));
		GuiAcceptanceSupport.await(() -> Files.exists(exportDirectory.resolve("buffer-report.html")),
			"CCPM HTML approve did not write an .html report");
		assertTrue(Files.size(exportDirectory.resolve("buffer-report.html")) > 0, "CCPM HTML report must contain output");

		click(robot, findButton(dialog, "HTML"));
		GuiAcceptanceSupport.await(() -> visibleFileChooser() != null, "CCPM HTML export did not reopen for cancellation");
		Window cancelledHtmlChooser = visibleFileChooser();
		JFileChooser htmlCancelComponent = findFileChooser(cancelledHtmlChooser);
		assertEquals("HTML (*.html)", htmlCancelComponent.getFileFilter().getDescription(), "CCPM HTML filter must persist on repeated open");
		SwingUtilities.invokeAndWait(() -> htmlCancelComponent.setSelectedFile(exportDirectory.resolve("cancelled-report.html").toFile()));
		click(robot, findCancelButton(cancelledHtmlChooser));
		GuiAcceptanceSupport.await(() -> visibleFileChooser() == null, "CCPM HTML Cancel did not close the shared chooser");
		assertFalse(Files.exists(exportDirectory.resolve("cancelled-report.html")), "cancel must not create an HTML report");

		click(robot, findButton(dialog, "CSV"));
		GuiAcceptanceSupport.await(() -> visibleFileChooser() != null, "CCPM CSV export did not reopen for cancellation");
		Window cancelledCsvChooser = visibleFileChooser();
		assertEquals("CSV (*.csv)", findFileChooser(cancelledCsvChooser).getFileFilter().getDescription(),
			"CCPM CSV filter must return after the HTML export");
		SwingUtilities.invokeAndWait(() -> findFileChooser(cancelledCsvChooser)
			.setSelectedFile(exportDirectory.resolve("cancelled-report.csv").toFile()));
		click(robot, findCancelButton(cancelledCsvChooser));
		GuiAcceptanceSupport.await(() -> visibleFileChooser() == null, "CCPM CSV Cancel did not close the shared chooser");
		assertFalse(Files.exists(exportDirectory.resolve("cancelled-report.csv")), "cancel must not create a CSV report");
		SwingUtilities.invokeAndWait(dialog::dispose);
	}

	@Test
	void appliedProjectShowsNetworkGraphAndBufferChartInVisibleDialogs() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for CCPM dialog coverage.");
		Project project = newProjectWithTasks();
		CriticalChainService service = new CriticalChainService();
		CriticalChainService.Settings settings = service.settings(project);
		settings.setEnabled(true);
		service.apply(project, null, settings);
		assertFalse(service.analysis(project).criticalTaskIds().isEmpty(), "fixture must create a cached critical-chain analysis");

		assertDialogShows(project, CriticalChainStatusDialogBox.Surface.NETWORK, CriticalChainGraphPanel.class);
		assertDialogShows(project, CriticalChainStatusDialogBox.Surface.BUFFER_STATUS, CriticalChainBufferChartPanel.class);
	}

	@Test
	void reloadedMpoRetainsCcpmBaselineAndRendersBothStatusSurfaces() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for CCPM dialog coverage.");
		Project project = newProjectWithTasks();
		CriticalChainService service = new CriticalChainService();
		CriticalChainService.Settings settings = service.settings(project);
		settings.setEnabled(true);
		service.apply(project, null, settings);

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		new MpoFileImporter().saveProject(project, saved);
		Project restored = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertTrue(service.findSettings(restored) != null && service.findSettings(restored).isEnabled(),
			"reloaded MPO must retain enabled CCPM settings");
		assertTrue(service.findBaseline(restored) != null, "reloaded MPO must retain the CCPM baseline");

		assertDialogShows(restored, CriticalChainStatusDialogBox.Surface.NETWORK, CriticalChainGraphPanel.class);
		assertDialogShows(restored, CriticalChainStatusDialogBox.Surface.BUFFER_STATUS, CriticalChainBufferChartPanel.class);
	}

	@Test
	void unconfiguredNetworkOffersPhysicalRouteToCcpmSettings() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for CCPM dialog coverage.");
		Project project = newProjectWithTasks();
		observer = new DialogObserver();
		observer.open();
		observer.show(project, CriticalChainStatusDialogBox.Surface.NETWORK);
		CriticalChainStatusDialogBox dialog = observer.awaitDialog();
		GuiAcceptanceSupport.await(dialog::isActive, "CCPM result dialog did not become active");
		SwingUtilities.invokeAndWait(() -> { dialog.setAlwaysOnTop(true); dialog.toFront(); dialog.requestFocus(); });
		AbstractButton configure = findButton(dialog, UsabilityStrings.text("ccpm.configure"));
		assertTrue(configure != null && configure.isShowing(),
			"An unconfigured CCPM view must expose a visible settings/apply button");

		Rectangle bounds = new Rectangle();
		SwingUtilities.invokeAndWait(() -> {
			java.awt.Point location = configure.getLocationOnScreen();
			bounds.setBounds(location.x, location.y, configure.getWidth(), configure.getHeight());
			java.awt.Point local = new java.awt.Point(location);
			SwingUtilities.convertPointFromScreen(local, dialog);
			if (SwingUtilities.getDeepestComponentAt(dialog, local.x + configure.getWidth() / 2, local.y + configure.getHeight() / 2) != configure)
				throw new AssertionError("CCPM configure button must be the physical hit target");
		});
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		robot.waitForIdle();
		robot.mouseMove(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> findResourceLevelingDialog() != null,
			"CCPM settings must open from the empty network view");
		ResourceLevelingDialogBox settingsDialog = findResourceLevelingDialog();
		assertTrue(settingsDialog != null && settingsDialog.isVisible(),
			"CCPM settings dialog must be visible after the physical click");
		SwingUtilities.invokeAndWait(settingsDialog::dispose);
		GuiAcceptanceSupport.await(() -> findVisibleStatusDialogCount() > 0,
			"The original CCPM result surface must return after settings are closed");
		SwingUtilities.invokeAndWait(() -> {
			for (Window window : Window.getWindows())
				if (window instanceof CriticalChainStatusDialogBox) window.dispose();
		});
	}

	@Test
	void robotAppliesCcpmAndUndoRedoSurvivesMpoReload() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for CCPM acceptance coverage.");
		Project project = newProjectWithTasks();
		// Keep the CCPM apply as the first history entry so one physical Ctrl+Z
		// proves that the user-facing Apply action is reversible.
		project.getUndoController().discardAllEdits();
		CriticalChainService service = new CriticalChainService();
		observer = new DialogObserver();
		observer.open();
		observer.show(project, CriticalChainStatusDialogBox.Surface.NETWORK);
		CriticalChainStatusDialogBox network = observer.awaitDialog();
		GuiAcceptanceSupport.await(() -> findButton(network, UsabilityStrings.text("ccpm.configure")) != null,
			"unconfigured network did not expose CCPM setup");
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		click(robot, findButton(network, UsabilityStrings.text("ccpm.configure")));
		GuiAcceptanceSupport.await(() -> findResourceLevelingDialog() != null,
			"physical configure route did not open CCPM settings");
		ResourceLevelingDialogBox settings = findResourceLevelingDialog();
		assertTrue(findButton(settings, UsabilityStrings.text("leveling.apply")) != null,
			"CCPM settings must expose Apply");
		click(robot, findButton(settings, UsabilityStrings.text("leveling.apply")));
		GuiAcceptanceSupport.await(() -> service.findBaseline(project) != null,
			"physical Apply did not generate and persist the CCPM baseline");
		assertFalse(service.analysis(project).criticalTaskIds().isEmpty(),
			"applied fixture must produce a non-empty critical chain");
		click(robot, findButton(settings, UsabilityStrings.text("common.close")));
		GuiAcceptanceSupport.await(() -> !settings.isShowing(), "CCPM settings did not close");
		GuiAcceptanceSupport.await(() -> findVisibleStatusDialogCount() > 0,
			"the network result view did not return after CCPM settings closed");
		SwingUtilities.invokeAndWait(() -> {
			for (Window window : Window.getWindows())
				if (window instanceof CriticalChainStatusDialogBox && window.isDisplayable()) window.dispose();
		});
		observer.close();
		observer = null;
		CriticalChainStatusDialogBox buffer = openStatusDialog(project);
		assertTrue(visibleComponentExists(buffer, CriticalChainBufferChartPanel.class),
			"applied plan must render the buffer view");
		robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL);
		robot.keyPress(java.awt.event.KeyEvent.VK_Z);
		robot.keyRelease(java.awt.event.KeyEvent.VK_Z);
		robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> service.findBaseline(project) == null,
			"Ctrl+Z did not undo the CCPM apply");
		robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL);
		robot.keyPress(java.awt.event.KeyEvent.VK_Y);
		robot.keyRelease(java.awt.event.KeyEvent.VK_Y);
		robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> service.findBaseline(project) != null,
			"Ctrl+Y did not restore the CCPM apply");
		SwingUtilities.invokeAndWait(buffer::dispose);
		observer.close();
		observer = null;

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, saved), "MPO save rejected the applied CCPM project");
		Project restored = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertTrue(service.findBaseline(restored) != null, "MPO reload lost the applied CCPM baseline");
		assertFalse(service.analysis(restored).criticalTaskIds().isEmpty(), "MPO reload lost the generated critical chain");
		assertDialogShows(restored, CriticalChainStatusDialogBox.Surface.NETWORK, CriticalChainGraphPanel.class);
		assertDialogShows(restored, CriticalChainStatusDialogBox.Surface.BUFFER_STATUS, CriticalChainBufferChartPanel.class);
	}

	private void assertDialogShows(Project project, CriticalChainStatusDialogBox.Surface surface,
		Class<? extends Component> expectedComponent) throws Exception {
		observer = new DialogObserver();
		observer.open();
		observer.show(project, surface);
		CriticalChainStatusDialogBox dialog = observer.awaitDialog();
		GuiAcceptanceSupport.await(() -> visibleComponentExists(dialog, expectedComponent),
			"CCPM " + surface + " dialog did not render " + expectedComponent.getSimpleName());
		assertTrue(isShowing(dialog), "CCPM result dialog must remain visibly open while its graph is rendered");
		SwingUtilities.invokeAndWait(dialog::dispose);
		assertTrue(observer.showReturned.await(5, TimeUnit.SECONDS), "modal CCPM dialog did not close");
		observer.close();
		observer = null;
	}

	private static boolean visibleComponentExists(Window window, Class<? extends Component> expectedComponent) {
		AtomicReference<Boolean> result = new AtomicReference<>(Boolean.FALSE);
		try {
			SwingUtilities.invokeAndWait(() -> {
				Deque<Component> pending = new ArrayDeque<>();
				pending.add(window);
				while (!pending.isEmpty()) {
					Component component = pending.removeFirst();
					if (expectedComponent.isInstance(component) && component.isShowing()) {
						result.set(Boolean.TRUE);
						return;
					}
					if (component instanceof java.awt.Container container) {
						for (Component child : container.getComponents()) pending.addLast(child);
					}
				}
			});
		} catch (Exception exception) {
			throw new IllegalStateException("Could not inspect the CCPM result dialog", exception);
		}
		return result.get().booleanValue();
	}

	private static boolean isShowing(Window window) {
		AtomicReference<Boolean> result = new AtomicReference<>(Boolean.FALSE);
		try {
			SwingUtilities.invokeAndWait(() -> result.set(window.isShowing()));
		} catch (Exception exception) {
			throw new IllegalStateException("Could not inspect CCPM dialog visibility", exception);
		}
		return result.get().booleanValue();
	}

	private static ResourceLevelingDialogBox findResourceLevelingDialog() {
		for (Window window : Window.getWindows()) {
			if (window instanceof ResourceLevelingDialogBox dialog && dialog.isVisible()) return dialog;
		}
		return null;
	}

	private static int findVisibleStatusDialogCount() {
		int count = 0;
		for (Window window : Window.getWindows())
			if (window instanceof CriticalChainStatusDialogBox && window.isVisible()) count++;
		return count;
	}

	static AbstractButton findButton(java.awt.Container container, String text) {
		for (Component child : container.getComponents()) {
			if (child instanceof AbstractButton button && text.equals(button.getText())) return button;
			if (child instanceof java.awt.Container nested) {
				AbstractButton button = findButton(nested, text);
				if (button != null) return button;
			}
		}
		return null;
	}

	private CriticalChainStatusDialogBox openStatusDialog(Project project) throws Exception {
		observer = new DialogObserver();
		observer.open();
		observer.show(project, CriticalChainStatusDialogBox.Surface.BUFFER_STATUS);
		CriticalChainStatusDialogBox dialog = observer.awaitDialog();
		GuiAcceptanceSupport.await(dialog::isActive, "CCPM result dialog did not become active");
		SwingUtilities.invokeAndWait(() -> { dialog.setAlwaysOnTop(true); dialog.toFront(); dialog.requestFocus(); });
		return dialog;
	}

	static void click(Robot robot, Component component) throws Exception {
		Point point = new Point();
		SwingUtilities.invokeAndWait(() -> {
			Point location = component.getLocationOnScreen();
			point.setLocation(location.x + component.getWidth() / 2, location.y + component.getHeight() / 2);
		});
		robot.mouseMove(point.x, point.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	static Window visibleFileChooser() {
		for (Window window : Window.getWindows())
			if (window.isShowing() && findFileChooser(window) != null) return window;
		return null;
	}

	static JFileChooser findFileChooser(java.awt.Container container) {
		for (Component component : container.getComponents()) {
			if (component instanceof JFileChooser chooser) return chooser;
			if (component instanceof java.awt.Container nested) {
				JFileChooser chooser = findFileChooser(nested);
				if (chooser != null) return chooser;
			}
		}
		return null;
	}

	static AbstractButton findApproveButton(Window chooser) {
		return findChooserButton(chooser, "approve");
	}

	static AbstractButton findCancelButton(Window chooser) {
		return findChooserButton(chooser, "cancel");
	}

	private static AbstractButton findChooserButton(Window chooser, String command) {
		Deque<Component> pending = new ArrayDeque<>();
		pending.add(chooser);
		while (!pending.isEmpty()) {
			Component component = pending.removeFirst();
			if (component instanceof AbstractButton button) {
				String action = button.getActionCommand();
				String text = button.getText();
				if (action != null && action.toLowerCase(java.util.Locale.ROOT).contains(command)) return button;
				if (text != null && (command.equalsIgnoreCase(text.trim())
					|| (command.equals("cancel") && (text.contains("取消") || text.contains("キャンセル")))
					|| (command.equals("approve") && (text.contains("保存") || text.contains("開く"))))) return button;
			}
			if (component instanceof java.awt.Container nested)
				java.util.Collections.addAll(pending, nested.getComponents());
		}
		throw new AssertionError("chooser has no " + command + " button");
	}

	static Project newProjectWithTasks() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("ccpm-status-dialog", undo);
		pool.setLocal(true);
		Project project = Project.createProject(pool, undo);
		project.setName("CCPM status dialog acceptance");
		// Move Project is a forward-scheduling command in MSP.  Make the fixture
		// satisfy that precondition so the Robot route exercises the real dialog.
		project.setForward(true);
		Task first = project.createScriptedTask();
		first.setName("Design");
		Task second = project.createScriptedTask();
		second.setName("Build");
		DependencyService.getInstance().newDependency(first, second, DependencyType.FS, 0L, project);
		Resource resource = pool.createScriptedResource();
		resource.setName("Shared resource");
		Assignment.getInstance(first, resource, 1.0, 0);
		Assignment.getInstance(second, resource, 1.0, 0);
		return project;
	}

	private static final class DialogObserver implements AWTEventListener {
		private final AtomicReference<CriticalChainStatusDialogBox> dialog = new AtomicReference<>();
		private final AtomicReference<Throwable> showFailure = new AtomicReference<>();
		private final CountDownLatch showReturned = new CountDownLatch(1);
		private final java.util.Set<Window> existingWindows = java.util.Collections.newSetFromMap(
			new java.util.IdentityHashMap<>());

		void open() {
			java.util.Collections.addAll(existingWindows, Window.getWindows());
			Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.WINDOW_EVENT_MASK);
		}

		void show(Project project, CriticalChainStatusDialogBox.Surface surface) {
			SwingUtilities.invokeLater(() -> {
				try {
					CriticalChainStatusDialogBox.show(null, project, surface);
				} catch (Throwable failure) {
					showFailure.compareAndSet(null, failure);
					throw new AssertionError("CCPM result dialog show failed on the EDT", failure);
				} finally {
					showReturned.countDown();
				}
			});
		}

		void close() {
			Toolkit.getDefaultToolkit().removeAWTEventListener(this);
		}

		CriticalChainStatusDialogBox awaitDialog() throws Exception {
			try {
				GuiAcceptanceSupport.await(() -> {
					Throwable failure = showFailure.get();
					if (failure != null) throw new AssertionError("CCPM result dialog show failed on the EDT", failure);
					if (dialog.get() != null && dialog.get().isShowing()) return true;
					for (Window window : Window.getWindows()) {
						if (!existingWindows.contains(window) && window instanceof CriticalChainStatusDialogBox statusDialog
							&& statusDialog.isShowing()) {
							dialog.compareAndSet(null, statusDialog);
							return true;
						}
					}
					return false;
				}, "CCPM result dialog did not open");
			} catch (AssertionError failure) {
				if (showFailure.get() != null) throw failure;
				throw new AssertionError(failure.getMessage() + "\n" + diagnostics(), failure);
			}
			return dialog.get();
		}

		private static String diagnostics() {
			StringBuilder details = new StringBuilder("Window inventory:");
			for (Window window : Window.getWindows()) {
				details.append("\n  ").append(window.getClass().getName())
					.append(" visible=").append(window.isVisible())
					.append(" displayable=").append(window.isDisplayable())
					.append(" bounds=").append(window.getBounds());
			}
			details.append("\nEDT thread stacks:");
			Thread.getAllStackTraces().forEach((thread, stack) -> {
				if (thread.getName().startsWith("AWT-EventQueue")) {
					details.append("\n  ").append(thread.getName()).append(" state=").append(thread.getState());
					for (StackTraceElement element : stack) details.append("\n    at ").append(element);
				}
			});
			return details.toString();
		}

		@Override
		public void eventDispatched(AWTEvent event) {
			if (event instanceof WindowEvent windowEvent && windowEvent.getID() == WindowEvent.WINDOW_OPENED
				&& windowEvent.getWindow() instanceof CriticalChainStatusDialogBox statusDialog) {
				dialog.compareAndSet(null, statusDialog);
			}
		}
	}
}

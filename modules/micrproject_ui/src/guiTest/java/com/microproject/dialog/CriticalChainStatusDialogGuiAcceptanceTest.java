/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;
import javax.swing.AbstractButton;

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

/**
 * Non-headless regression coverage for the CCPM result windows.  The analysis
 * is deliberately applied before the modal dialog opens, so it is cached and
 * can complete before the dialog becomes displayable (the timing that caused
 * the former blank-dialog defect).
 */
class CriticalChainStatusDialogGuiAcceptanceTest {
	private DialogObserver observer;

	@AfterEach
	void closeDialogs() throws Exception {
		if (observer != null) observer.close();
		for (Window window : Window.getWindows()) {
			if ((window instanceof CriticalChainStatusDialogBox || window instanceof ResourceLevelingDialogBox)
				&& window.isDisplayable()) {
				SwingUtilities.invokeAndWait(window::dispose);
			}
		}
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
		SwingUtilities.invokeLater(() -> CriticalChainStatusDialogBox.show(null, project,
			CriticalChainStatusDialogBox.Surface.NETWORK));
		CriticalChainStatusDialogBox dialog = observer.awaitDialog();
		AbstractButton configure = findButton(dialog, UsabilityStrings.text("ccpm.configure"));
		assertTrue(configure != null && configure.isShowing(),
			"An unconfigured CCPM view must expose a visible settings/apply button");

		Rectangle bounds = new Rectangle();
		SwingUtilities.invokeAndWait(() -> {
			java.awt.Point location = configure.getLocationOnScreen();
			bounds.setBounds(location.x, location.y, configure.getWidth(), configure.getHeight());
		});
		Robot robot = new Robot();
		robot.setAutoDelay(40);
		robot.mouseMove(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
		robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> findResourceLevelingDialog() != null,
			"CCPM settings must open from the empty network view");
		assertTrue(findResourceLevelingDialog().isVisible(), "CCPM settings dialog must be visible after the physical click");
		SwingUtilities.invokeAndWait(() -> findResourceLevelingDialog().dispose());
		GuiAcceptanceSupport.await(() -> findVisibleStatusDialogCount() > 0,
			"The original CCPM result surface must return after settings are closed");
		SwingUtilities.invokeAndWait(() -> {
			for (Window window : Window.getWindows())
				if (window instanceof CriticalChainStatusDialogBox) window.dispose();
		});
	}

	private void assertDialogShows(Project project, CriticalChainStatusDialogBox.Surface surface,
		Class<? extends Component> expectedComponent) throws Exception {
		observer = new DialogObserver();
		observer.open();
		CountDownLatch closed = new CountDownLatch(1);
		SwingUtilities.invokeLater(() -> {
			CriticalChainStatusDialogBox.show(null, project, surface);
			closed.countDown();
		});
		CriticalChainStatusDialogBox dialog = observer.awaitDialog();
		GuiAcceptanceSupport.await(() -> visibleComponentExists(dialog, expectedComponent),
			"CCPM " + surface + " dialog did not render " + expectedComponent.getSimpleName());
		assertTrue(isShowing(dialog), "CCPM result dialog must remain visibly open while its graph is rendered");
		SwingUtilities.invokeAndWait(dialog::dispose);
		assertTrue(closed.await(5, TimeUnit.SECONDS), "modal CCPM dialog did not close");
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
		for (Window window : Window.getWindows())
			if (window instanceof ResourceLevelingDialogBox dialog && dialog.isVisible()) return dialog;
		return null;
	}

	private static int findVisibleStatusDialogCount() {
		int count = 0;
		for (Window window : Window.getWindows())
			if (window instanceof CriticalChainStatusDialogBox && window.isVisible()) count++;
		return count;
	}

	private static AbstractButton findButton(java.awt.Container container, String text) {
		for (Component child : container.getComponents()) {
			if (child instanceof AbstractButton button && text.equals(button.getText())) return button;
			if (child instanceof java.awt.Container nested) {
				AbstractButton button = findButton(nested, text);
				if (button != null) return button;
			}
		}
		return null;
	}

	private static Project newProjectWithTasks() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("ccpm-status-dialog", undo);
		pool.setLocal(true);
		Project project = Project.createProject(pool, undo);
		project.setName("CCPM status dialog acceptance");
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

		void open() {
			Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.WINDOW_EVENT_MASK);
		}

		void close() {
			Toolkit.getDefaultToolkit().removeAWTEventListener(this);
		}

		CriticalChainStatusDialogBox awaitDialog() throws Exception {
			GuiAcceptanceSupport.await(() -> dialog.get() != null, "CCPM result dialog did not open");
			return dialog.get();
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

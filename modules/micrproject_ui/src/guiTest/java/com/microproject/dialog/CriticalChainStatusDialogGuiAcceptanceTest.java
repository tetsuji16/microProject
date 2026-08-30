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
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.assignment.Assignment;
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
			if (window instanceof CriticalChainStatusDialogBox && window.isDisplayable()) {
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

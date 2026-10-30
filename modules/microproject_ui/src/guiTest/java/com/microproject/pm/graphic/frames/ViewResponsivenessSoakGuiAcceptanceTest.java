/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.microproject.exchange.LocalFileImporter;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.pm.ccpm.CriticalChainService;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.task.Task;
import com.microproject.session.SessionFactory;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.transaction.ProjectMutationState;
import com.microproject.ui.util.SwingUiDispatcher;
import com.microproject.util.Environment;
import com.microproject.util.UiDispatch;

/**
 * Issue #946 reproducer: an MPO opened through the real route (collaboration session, recovery timer) must keep
 * Gantt/Resource views usable for a long session. Detects (a) a blocked EDT, (b) a leaked process-wide mutation
 * coordinator and (c) uncaught exceptions. Duration: -Dmicroproject.soak.minutes or MICROPROJECT_SOAK_MINUTES (12).
 */
class ViewResponsivenessSoakGuiAcceptanceTest {
	private static final long STALL_SECONDS = 15;

	@TempDir
	Path temp;

	@Test
	void mpoViewsStayResponsiveDuringLongSession() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Path pod = locateSample();
		Path mpo = temp.resolve("soak.mpo");
		LocalFileImporter in = new LocalFileImporter();
		in.setFileName(pod.toString());
		in.setProjectFactory(ProjectFactory.getInstance());
		in.importFile();
		MpoFileImporter out = new MpoFileImporter();
		out.setProject(in.getProject());
		out.setFileName(mpo.toString());
		out.exportFile();

		boolean clientSide = Environment.isClientSide(), ribbon = Environment.isRibbonUI(), newLook = Environment.isNewLook();
		Environment.setClientSide(true);
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		UiDispatch.setDispatcher(new SwingUiDispatcher());
		List<Throwable> uncaught = new CopyOnWriteArrayList<>();
		Thread.UncaughtExceptionHandler previousHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler((thread, failure) -> {
			uncaught.add(failure);
			if (previousHandler != null) previousHandler.uncaughtException(thread, failure);
		});
		MainRibbonFrame[] w = new MainRibbonFrame[1];
		GraphicManager[] m = new GraphicManager[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
				w[0] = new MainRibbonFrame("microProject - view responsiveness soak (#946)", null);
				m[0] = new GraphicManager(w[0]);
				w[0].setGraphicManager(m[0]);
				m[0].initView();
				SessionFactory.getInstance().setJobQueue(m[0].getJobQueue());
				w[0].setSize(1100, 700);
				w[0].setLocationByPlatform(true);
				w[0].setAlwaysOnTop(true);
				w[0].setVisible(true);
				m[0].openLocalProjectsSequentially(new String[] { mpo.toString() });
			});
			GuiAcceptanceSupport.await(() -> m[0].findFrameForProjectFile(mpo.toString()) != null,
				"real Open route did not open the MPO");
			DocumentFrame frame = m[0].findFrameForProjectFile(mpo.toString());
			Project project = frame.getProject();
			assertNotNull(project.getCollaborationSession(), "MPO opened through the real route must own a CollaborationSession");
			GuiAcceptanceSupport.await(frame::isShowing, "document frame did not become visible");
			applyCcpm(project);
			soak(m[0], frame, project, uncaught);
		} finally {
			Thread.setDefaultUncaughtExceptionHandler(previousHandler);
			try {
				if (w[0] != null) GuiAcceptanceSupport.runOnEdtWithTimeout(w[0]::dispose, "soak window disposal");
				if (m[0] != null) GuiAcceptanceSupport.runOnEdtWithTimeout(m[0]::cleanUp, "soak manager cleanup");
			} catch (Exception | Error cleanupFailure) {
				System.out.println("SOAK cleanup failed (EDT possibly blocked): " + cleanupFailure);
			}
			Environment.setClientSide(clientSide);
			Environment.setRibbonUI(ribbon);
			Environment.setNewLook(newLook);
		}
	}

	private void soak(GraphicManager manager, DocumentFrame frame, Project project, List<Throwable> uncaught) throws Exception {
		String configured = System.getProperty("microproject.soak.minutes", System.getenv("MICROPROJECT_SOAK_MINUTES"));
		long minutes = configured == null || configured.isBlank() ? 12 : Long.parseLong(configured.trim());
		long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(minutes);
		long nextProgress = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
		long nextRecovery = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		Task[] edited = new Task[1];
		String previousName = null;
		for (int cycle = 1; System.nanoTime() < deadline; cycle++) {
			SwingUtilities.invokeAndWait(frame::activateResourceView);
			int resourceRows = clickRow(robot, frame, cycle);
			SwingUtilities.invokeAndWait(frame::activateGanttView);
			int ganttRows = clickRow(robot, frame, cycle);
			if (cycle == 1) System.out.println("SOAK rows resource=" + resourceRows + " gantt=" + ganttRows
				+ (resourceRows == 0 ? " FINDING: Resource view is empty" : ""));
			if (cycle % 4 == 0 && ganttRows > 0) {
				SpreadSheet sheet = onEdt(frame::getActiveSpreadSheet);
				String before = editName(robot, sheet, nameCell(sheet, 1), "Soak edit " + cycle, edited);
				if (before != null) previousName = before;
			}
			if (System.nanoTime() >= nextRecovery) {
				nextRecovery = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
				SwingUtilities.invokeAndWait(() -> invokeRecoverySave(manager));
			}
			checkEdtRoundTrip(project);
			checkCoordinatorFromWorker();
			if (System.nanoTime() >= nextProgress) {
				nextProgress = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
				System.out.println("SOAK cycle=" + cycle + " uncaught=" + uncaught.size());
				for (Throwable t : uncaught) System.out.println(stack(t));
			}
		}
		if (edited[0] != null) {
			String expected = previousName;
			SwingUtilities.invokeAndWait(() -> project.getUndoController().undo());
			assertEquals(expected, onEdt(edited[0]::getName), "Undo of the last soak edit must restore the previous name");
		}
		assertNotNull(project.getCollaborationSession(), "collaboration session vanished during soak");
		for (Throwable t : uncaught) System.out.println(stack(t));
		assertTrue(uncaught.isEmpty(), "uncaught throwables during soak: " + uncaught.size() + ", first: "
			+ (uncaught.isEmpty() ? "" : stack(uncaught.get(0))));
	}

	/** Robot-clicks a data row of the active view's spreadsheet and asserts the selection followed. Returns the row count. */
	private int clickRow(Robot robot, DocumentFrame frame, int cycle) throws Exception {
		SpreadSheet sheet = onEdt(frame::getActiveSpreadSheet);
		GuiAcceptanceSupport.await(() -> Boolean.TRUE.equals(onEdtQuiet(sheet::isShowing)), "spreadsheet of the active view did not show (cycle " + cycle + ")");
		int rows = onEdt(sheet::getRowCount);
		if (rows == 0) return 0;
		int row = rows > 1 ? 1 + cycle % Math.min(rows - 1, 8) : 0;
		clickCell(robot, sheet, nameCell(sheet, row));
		GuiAcceptanceSupport.await(() -> Integer.valueOf(row).equals(onEdtQuiet(sheet::getSelectedRow)),
			"click on row " + row + " did not select it (cycle " + cycle + ", rows=" + rows + ")");
		return rows;
	}

	private int[] nameCell(SpreadSheet sheet, int row) throws Exception {
		return onEdt(() -> {
			for (int c = 0; c < sheet.getColumnCount(); c++) if (sheet.isNameFieldColumn(c)) return new int[] { row, c };
			return new int[] { row, 0 };
		});
	}

	private void clickCell(Robot robot, SpreadSheet sheet, int[] cell) throws Exception {
		Rectangle[] r = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			sheet.scrollRectToVisible(sheet.getCellRect(cell[0], cell[1], true));
			r[0] = sheet.getCellRect(cell[0], cell[1], true);
		});
		java.awt.Point origin = onEdt(sheet::getLocationOnScreen);
		robot.mouseMove(origin.x + r[0].x + Math.max(2, r[0].width / 2), origin.y + r[0].y + r[0].height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	/** F2 edit through Robot; returns the previous name (null when the row is no task) and stores the edited task in out[0]. */
	private String editName(Robot robot, SpreadSheet sheet, int[] cell, String newName, Task[] out) throws Exception {
		clickCell(robot, sheet, cell);
		Task task = onEdt(() -> {
			var node = ((SpreadSheetModel) sheet.getModel()).getNodeForDisplayRow(sheet.getSelectedRow());
			return node != null && node.getImpl() instanceof Task t ? t : null;
		});
		if (task == null) {
			System.out.println("SOAK edit skipped: selected row is not a task");
			return null;
		}
		String previous = onEdt(task::getName);
		robot.keyPress(KeyEvent.VK_F2);
		robot.keyRelease(KeyEvent.VK_F2);
		GuiAcceptanceSupport.await(() -> Boolean.TRUE.equals(onEdtQuiet(sheet::isEditing)), "F2 did not start editing");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		for (char ch : newName.toCharArray()) {
			boolean upper = Character.isUpperCase(ch);
			int code = KeyEvent.getExtendedKeyCodeForChar(ch);
			if (upper) robot.keyPress(KeyEvent.VK_SHIFT);
			robot.keyPress(code);
			robot.keyRelease(code);
			if (upper) robot.keyRelease(KeyEvent.VK_SHIFT);
		}
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(() -> newName.equals(onEdtQuiet(task::getName)),
			"edited name was not committed to the model (expected '" + newName + "')");
		String rendered = onEdt(() -> String.valueOf(sheet.getValueAt(cell[0], cell[1])));
		assertTrue(rendered.contains(newName), "rendered cell value '" + rendered + "' does not show '" + newName + "'");
		out[0] = task;
		return previous;
	}

	private void applyCcpm(Project project) {
		try {
			CriticalChainService service = new CriticalChainService();
			CriticalChainService.Settings settings = service.settings(project);
			settings.setEnabled(true);
			service.apply(project, null, settings);
			System.out.println("SOAK ccpm applied");
		} catch (RuntimeException refused) {
			System.out.println("SOAK ccpm refused: " + refused);
		}
	}

	private void invokeRecoverySave(GraphicManager manager) {
		try {
			var field = GraphicManager.class.getDeclaredField("autoRecoveryManager");
			field.setAccessible(true);
			Object recovery = field.get(manager);
			Method save = recovery.getClass().getDeclaredMethod("saveDirtyProjects");
			save.setAccessible(true);
			save.invoke(recovery);
		} catch (ReflectiveOperationException missing) {
			System.out.println("SOAK recovery save not available: " + missing);
		}
	}

	private void checkEdtRoundTrip(Project project) throws Exception {
		CountDownLatch done = new CountDownLatch(1);
		AtomicBoolean held = new AtomicBoolean();
		SwingUtilities.invokeLater(() -> {
			held.set(project.getMutationState().isHeldByCurrentThread());
			done.countDown();
		});
		if (!done.await(STALL_SECONDS, TimeUnit.SECONDS)) failWithStacks("EDT round trip exceeded " + STALL_SECONDS + "s (EDT blocked)");
		if (held.get()) failWithStacks("EDT holds a leaked ProjectMutationState scope while idle");
	}

	private void checkCoordinatorFromWorker() {
		CompletableFuture<Void> probe = new CompletableFuture<>();
		Thread worker = new Thread(() -> {
			try {
				ProjectMutationState.coordinated(() -> { });
				probe.complete(null);
			} catch (Throwable t) {
				probe.completeExceptionally(t);
			}
		}, "soak-coordinator-probe");
		worker.setDaemon(true);
		worker.start();
		try {
			probe.get(STALL_SECONDS, TimeUnit.SECONDS);
		} catch (java.util.concurrent.TimeoutException e) {
			failWithStacks("ProjectMutationState coordinator not obtainable from a worker within " + STALL_SECONDS + "s (leaked)");
		} catch (Exception e) {
			throw new AssertionError("coordinator probe failed: " + e, e);
		}
	}

	private void failWithStacks(String reason) {
		StringBuilder dump = new StringBuilder("SOAK FAILURE: ").append(reason).append('\n');
		for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
			dump.append('"').append(entry.getKey().getName()).append("\" ").append(entry.getKey().getState()).append('\n');
			for (StackTraceElement element : entry.getValue()) dump.append("\tat ").append(element).append('\n');
		}
		System.out.println(dump);
		try {
			Path file = Path.of("build", "soak-stacks-" + System.currentTimeMillis() + ".txt");
			Files.createDirectories(file.getParent());
			Files.writeString(file, dump);
			System.out.println("SOAK stacks written to " + file.toAbsolutePath());
		} catch (java.io.IOException e) {
			System.out.println("SOAK could not write stack file: " + e);
		}
		throw new AssertionError(reason);
	}

	private static String stack(Throwable t) {
		java.io.StringWriter w = new java.io.StringWriter();
		t.printStackTrace(new java.io.PrintWriter(w));
		return w.toString();
	}

	private static <T> T onEdt(java.util.concurrent.Callable<T> action) throws Exception {
		AtomicReference<T> result = new AtomicReference<>();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			try {
				result.set(action.call());
			} catch (Throwable t) {
				failure.set(t);
			}
		});
		if (failure.get() != null) throw new AssertionError("EDT action failed", failure.get());
		return result.get();
	}

	private static <T> T onEdtQuiet(java.util.concurrent.Callable<T> action) {
		try {
			return onEdt(action);
		} catch (Exception | Error e) {
			return null;
		}
	}

	private static Path locateSample() {
		for (String prefix : new String[] { "samples", "../samples", "../../samples" }) {
			File candidate = Path.of(prefix, "Commercial construction project plan2.pod").toFile();
			if (candidate.isFile()) return candidate.toPath();
		}
		throw new AssertionError("samples/Commercial construction project plan2.pod not found");
	}
}

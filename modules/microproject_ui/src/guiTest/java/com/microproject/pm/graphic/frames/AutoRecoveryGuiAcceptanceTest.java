/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.microproject.application.AutoRecoveryStore;
import com.microproject.dialog.UsabilityStrings;
import com.microproject.exchange.LocalFileImporter;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.renderer.NameCellComponent;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.task.Task;
import com.microproject.session.SessionFactory;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.GuiRobot;
import com.microproject.testsupport.WindowAcceptanceAssertions;

/** Physical recovery prompt and document state using only temporary storage. */
class AutoRecoveryGuiAcceptanceTest {
	@TempDir Path temp;

	@Test
	void recoversSnapshotAndSavesTheRecoveredDocument() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
		Path source = Path.of("../../samples/Commercial construction project plan2.pod").toAbsolutePath().normalize();
		LocalFileImporter importer = new LocalFileImporter();
		importer.setFileName(source.toString());
		importer.importFile();
		Project original = importer.getProject();
		Path file = temp.resolve("original.mpo");
		try (var out = Files.newOutputStream(file)) { assertTrue(new MpoFileImporter().saveProject(original, out)); }
		original.setFileName(file.toString());
		AutoRecoveryStore store = new AutoRecoveryStore(temp.resolve("recovery"));
		Preferences prefs = new MemoryPreferences();
		prefs.putBoolean(AutoRecoveryManager.ENABLED_PREFERENCE, true);
		ProjectFactory factory = ProjectFactory.getInstance();
		var previousJobQueue = SessionFactory.getInstance().getJobQueue();
		MainRibbonFrame[] window = new MainRibbonFrame[1];
		GraphicManager[] manager = new GraphicManager[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
				window[0] = new MainRibbonFrame("Recovery acceptance", null);
				manager[0] = new GraphicManager(window[0], temp.resolve("workspace"), store, prefs);
				window[0].setGraphicManager(manager[0]);
				manager[0].initView();
				// Production wires the session queue in StartupFactory; recovery saves and Ctrl+S need it.
				SessionFactory.getInstance().setJobQueue(manager[0].getJobQueue());
				factory.addProject(original, false, false);
				manager[0].addProjectFrame(original);
				window[0].setSize(1120, 700);
				window[0].setVisible(true);
			});
			Robot robot = new GuiRobot();
			robot.setAutoDelay(45);
			DocumentFrame frame = onEdt(() -> manager[0].getFrameForProject(original));
			SpreadSheet sheet = onEdt(() -> frame.getGanttView().getSpreadSheet());
			GuiAcceptanceSupport.await(() -> onEdt(() -> sheet.isShowing() && sheet.getRowCount() > 0), "original Gantt missing");
			int row = onEdt(() -> firstTaskRow(sheet));
			int column = onEdt(() -> nameColumn(sheet));
			Task task = onEdt(() -> (Task) ((SpreadSheetModel) sheet.getModel()).getNodeForDisplayRow(row).getImpl());
			editName(robot, sheet, row, column, "Recovered Task");
			GuiAcceptanceSupport.await(() -> onEdt(() -> "Recovered Task".equals(task.getName())), "edit did not commit");
			assertEquals("Recovered Task", renderedName(sheet, row, column));
			var field = GraphicManager.class.getDeclaredField("autoRecoveryManager");
			field.setAccessible(true);
			AutoRecoveryManager recovery = (AutoRecoveryManager) field.get(manager[0]);
			SwingUtilities.invokeAndWait(recovery::saveDirtyProjects);
			GuiAcceptanceSupport.await(() -> { try { return !store.listRecoverable().isEmpty(); } catch (Exception ex) { throw new RuntimeException(ex); } }, "recovery snapshot not published");
			SwingUtilities.invokeAndWait(() -> { manager[0].cleanUp(); window[0].dispose(); });
			manager[0] = null;
			factory.removeProject(original, false, false, false);
			GuiAcceptanceSupport.await(() -> !factory.getDirtyProjectList().contains(original), "original project did not close");
			AtomicReference<Boolean> outcome = new AtomicReference<>();
			SwingUtilities.invokeAndWait(() -> {
				window[0] = new MainRibbonFrame("Recovery acceptance", null);
				manager[0] = new GraphicManager(window[0], temp.resolve("workspace"), store, prefs);
				window[0].setGraphicManager(manager[0]);
				manager[0].initView();
				// Production wires the session queue in StartupFactory; recovery saves and Ctrl+S need it.
				SessionFactory.getInstance().setJobQueue(manager[0].getJobQueue());
				window[0].setSize(1120, 700);
				window[0].setVisible(true);
				manager[0].offerRecoveryAtStartup(outcome::set);
			});
			AtomicReference<javax.swing.AbstractButton> button = new AtomicReference<>();
			GuiAcceptanceSupport.await(() -> {
				for (Window candidate : Window.getWindows()) {
					if (!candidate.isShowing()) continue;
					try { button.set(GuiAcceptanceSupport.findShowingButtonByText(candidate, UsabilityStrings.text("recovery.recover"))); }
					catch (Exception | AssertionError notShownYet) { }
					if (button.get() != null) return true;
				}
				return false;
			}, "recovery prompt missing");
			WindowAcceptanceAssertions.clickComponent(robot, button.get());
			GuiAcceptanceSupport.await(() -> Boolean.TRUE.equals(outcome.get()), "recovery did not complete");
			DocumentFrame recoveredFrame = onEdt(() -> manager[0].getCurrentFrame());
			assertNotNull(recoveredFrame);
			Project recovered = onEdt(recoveredFrame::getProject);
			assertEquals(file.toString(), recovered.getFileName());
			assertTrue(recovered.isGroupDirty());
			SpreadSheet recoveredSheet = onEdt(() -> recoveredFrame.getGanttView().getSpreadSheet());
			GuiAcceptanceSupport.await(() -> onEdt(() -> recoveredSheet.isShowing() && recoveredSheet.getRowCount() > 0), "recovered Gantt missing");
			int recoveredRow = onEdt(() -> firstTaskRow(recoveredSheet));
			int recoveredColumn = onEdt(() -> nameColumn(recoveredSheet));
			Task recoveredTask = onEdt(() -> (Task) ((SpreadSheetModel) recoveredSheet.getModel()).getNodeForDisplayRow(recoveredRow).getImpl());
			assertEquals("Recovered Task", onEdt(recoveredTask::getName));
			assertEquals("Recovered Task", renderedName(recoveredSheet, recoveredRow, recoveredColumn));
			editName(robot, recoveredSheet, recoveredRow, recoveredColumn, "Final Task");
			GuiAcceptanceSupport.await(() -> onEdt(() -> "Final Task".equals(recoveredTask.getName())), "post-recovery edit missing");
			key(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
			GuiAcceptanceSupport.await(() -> onEdt(() -> "Recovered Task".equals(recoveredTask.getName())), "Undo failed");
			assertEquals("Recovered Task", renderedName(recoveredSheet, recoveredRow, recoveredColumn));
			key(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
			GuiAcceptanceSupport.await(() -> onEdt(() -> "Final Task".equals(recoveredTask.getName())), "Redo failed");
			key(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_S);
			GuiAcceptanceSupport.await(() -> onEdt(() -> !recovered.needsSaving()), "save did not complete");
			Project reloaded;
			try (var in = Files.newInputStream(file)) { reloaded = new MpoFileImporter().loadProject(in); }
			assertTrue(reloaded.getTaskList().stream().anyMatch(item -> "Final Task".equals(item.getName())));
		} finally {
			SwingUtilities.invokeAndWait(() -> { if (manager[0] != null) manager[0].cleanUp(); if (window[0] != null) window[0].dispose(); });
			SessionFactory.getInstance().setJobQueue(previousJobQueue);
		}
	}

	private static int firstTaskRow(SpreadSheet sheet) {
		for (int row = 0; row < sheet.getRowCount(); row++) {
			var node = ((SpreadSheetModel) sheet.getModel()).getNodeForDisplayRow(row);
			if (node != null && node.getImpl() instanceof Task task && task.getName() != null && !task.getName().isBlank()) return row;
		}
		throw new AssertionError("no task row");
	}
	private static int nameColumn(SpreadSheet sheet) {
		for (int column = 0; column < sheet.getColumnCount(); column++) if (sheet.isNameFieldColumn(column)) return column;
		throw new AssertionError("no name column");
	}
	private static void editName(Robot robot, SpreadSheet sheet, int row, int column, String text) throws Exception {
		Rectangle cell = onEdt(() -> { sheet.scrollRectToVisible(sheet.getCellRect(row, column, true)); Rectangle bounds = sheet.getCellRect(row, column, true); Point origin = sheet.getLocationOnScreen(); return new Rectangle(origin.x + bounds.x, origin.y + bounds.y, bounds.width, bounds.height); });
		robot.mouseMove(cell.x + cell.width / 2, cell.y + cell.height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK); robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> onEdt(sheet::isFocusOwner), "cell focus missing");
		robot.keyPress(KeyEvent.VK_F2); robot.keyRelease(KeyEvent.VK_F2);
		GuiAcceptanceSupport.await(() -> onEdt(sheet::isEditing), "editor missing");
		Component editor = onEdt(() -> ((NameCellComponent) sheet.getEditorComponent()).getTextComponent());
		SwingUtilities.invokeAndWait(editor::requestFocusInWindow);
		GuiAcceptanceSupport.await(() -> onEdt(editor::isFocusOwner), "editor focus missing");
		key(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_A);
		for (char c : text.toCharArray()) { int code = KeyEvent.getExtendedKeyCodeForChar(c); if (Character.isUpperCase(c)) robot.keyPress(KeyEvent.VK_SHIFT); robot.keyPress(code); robot.keyRelease(code); if (Character.isUpperCase(c)) robot.keyRelease(KeyEvent.VK_SHIFT); }
		robot.keyPress(KeyEvent.VK_ENTER); robot.keyRelease(KeyEvent.VK_ENTER);
	}
	private static String renderedName(SpreadSheet sheet, int row, int column) { return onEdt(() -> { Component c = sheet.prepareRenderer(sheet.getCellRenderer(row, column), row, column); return c instanceof NameCellComponent name ? name.getText() : ((javax.swing.JLabel) c).getText(); }); }
	private static void key(Robot robot, int modifier, int key) { robot.keyPress(modifier); robot.keyPress(key); robot.keyRelease(key); robot.keyRelease(modifier); robot.waitForIdle(); }
	private static <T> T onEdt(java.util.concurrent.Callable<T> action) { try { FutureTask<T> task = new FutureTask<>(action); SwingUtilities.invokeAndWait(task); return task.get(); } catch (Exception ex) { throw new RuntimeException(ex); } }
	private static final class MemoryPreferences extends AbstractPreferences {
		private final Map<String, String> values = new HashMap<>();
		MemoryPreferences() { super(null, ""); }
		protected void putSpi(String key, String value) { values.put(key, value); }
		protected String getSpi(String key) { return values.get(key); }
		protected void removeSpi(String key) { values.remove(key); }
		protected void removeNodeSpi() { values.clear(); }
		protected String[] keysSpi() { return values.keySet().toArray(String[]::new); }
		protected String[] childrenNamesSpi() { return new String[0]; }
		protected AbstractPreferences childSpi(String name) { throw new UnsupportedOperationException(); }
		protected void syncSpi() { }
		protected void flushSpi() { }
	}
}

/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.ResourceBundle;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import com.microproject.exchange.LocalFileImporter;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.GuiRobot;
import com.microproject.testsupport.RibbonGuiSupport;
import com.microproject.testsupport.WindowAcceptanceAssertions;
import com.microproject.ui.ribbon.RibbonDisplayMode;
import com.microproject.ui.ribbon.RibbonDisplayPreferences;
import com.microproject.util.Environment;

/** Real POD to MPO resource-view regression for issue #946. */
class PodResourceViewGuiAcceptanceTest {
	@TempDir Path tempDirectory;
	@Test
	void reopenedSampleResourcesAreVisibleAndEditableThroughRibbon() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Robot requires a desktop session");
		LocalFileImporter pod = new LocalFileImporter();
		pod.setFileName(Path.of("../../samples/Commercial construction project plan2.pod").toAbsolutePath().normalize().toString());
		pod.importFile();
		Project original = pod.getProject();
		assertNotNull(original);
		Path mpo = tempDirectory.resolve("sample.mpo");
		try (var output = Files.newOutputStream(mpo)) {
			assertTrue(new MpoFileImporter().saveProject(original, output));
		}
		Project reopened;
		try (var input = Files.newInputStream(mpo)) {
			reopened = new MpoFileImporter().loadProject(input);
		}
		Resource resource = reopened.getResourcePool().getResourceList().stream()
			.filter(candidate -> candidate.getName() != null && !candidate.getName().isBlank()).findFirst().orElseThrow();
		String originalName = resource.getName();
		boolean oldRibbon = Environment.isRibbonUI();
		boolean oldLook = Environment.isNewLook();
		RibbonDisplayMode oldMode = RibbonDisplayPreferences.load();
		MainRibbonFrame[] window = new MainRibbonFrame[1];
		GraphicManager[] manager = new GraphicManager[1];
		try {
			Environment.setRibbonUI(true);
			Environment.setNewLook(true);
			RibbonDisplayPreferences.save(RibbonDisplayMode.ALWAYS_SHOW);
			SwingUtilities.invokeAndWait(() -> {
				window[0] = new MainRibbonFrame("POD resource round trip", null);
				manager[0] = new GraphicManager(window[0]);
				window[0].setGraphicManager(manager[0]);
				manager[0].initView();
				manager[0].addProjectFrame(reopened);
				window[0].setSize(1120, 700);
				window[0].setVisible(true);
			});
			Robot robot = new GuiRobot();
			robot.setAutoDelay(45);
			DocumentFrame frame = onEdt(() -> manager[0].getFrameForProject(reopened));
			assertTrue(onEdt(() -> frame.getActiveTopView().showsTasks()));
			SpreadSheet gantt = onEdt(() -> frame.getGanttView().getSpreadSheet());
			GuiAcceptanceSupport.await(() -> onEdt(() -> gantt.isShowing() && gantt.getRowCount() > 0), "Gantt table did not appear");
			int ganttNameColumn = onEdt(() -> nameColumn(gantt));
			int ganttRow = onEdt(() -> {
				for (int i = 0; i < gantt.getRowCount(); i++) {
					Object impl = ((SpreadSheetModel) gantt.getModel()).getNodeForDisplayRow(i).getImpl();
					if (impl instanceof com.microproject.pm.task.Task task && task.getName() != null && !task.getName().isBlank()) return i;
				}
				return -1;
			});
			assertTrue(ganttRow >= 0, "sample must expose an actual task");
			com.microproject.pm.task.Task task = onEdt(() -> (com.microproject.pm.task.Task)
				((SpreadSheetModel) gantt.getModel()).getNodeForDisplayRow(ganttRow).getImpl());
			String oldTaskName = onEdt(task::getName);
			physicalEditName(robot, gantt, ganttRow, ganttNameColumn, "Edited Task");
			GuiAcceptanceSupport.await(() -> onEdt(() -> "Edited Task".equals(task.getName())), "Gantt task edit did not commit");
			assertEquals("Edited Task", renderedText(gantt, ganttRow, ganttNameColumn));
			key(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
			GuiAcceptanceSupport.await(() -> onEdt(() -> oldTaskName.equals(task.getName())), "Gantt Undo did not restore task");
			assertEquals(oldTaskName, renderedText(gantt, ganttRow, ganttNameColumn));
			key(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
			GuiAcceptanceSupport.await(() -> onEdt(() -> "Edited Task".equals(task.getName())), "Gantt Redo did not restore task");
			assertEquals("Edited Task", renderedText(gantt, ganttRow, ganttNameColumn));
			String viewTab = ResourceBundle.getBundle("com.microproject.menu.menu").getString("ViewRibbonTask.title");
			WindowAcceptanceAssertions.clickComponent(robot, GuiAcceptanceSupport.findShowingButtonByText(window[0], viewTab));
			WindowAcceptanceAssertions.clickComponent(robot, RibbonGuiSupport.findVisibleOrExpand(robot, window[0].getRibbonPanel(), "RibbonResources"));
			GuiAcceptanceSupport.await(() -> onEdt(() -> frame.getActiveTopView() instanceof ResourceView), "Resource ribbon did not open Resource View");
			SpreadSheet sheet = onEdt(() -> frame.getResourceView().getSpreadSheet());
			GuiAcceptanceSupport.await(() -> onEdt(() -> sheet.isShowing() && rowFor(sheet, resource) >= 0), "sample resource missing from visible outline: " + originalName);
			int column = onEdt(() -> nameColumn(sheet));
			int row = onEdt(() -> rowFor(sheet, resource));
			assertEquals(originalName, renderedText(sheet, row, column));
			Rectangle[] cell = new Rectangle[1];
			SwingUtilities.invokeAndWait(() -> {
				sheet.scrollRectToVisible(sheet.getCellRect(row, column, true));
				Rectangle bounds = sheet.getCellRect(row, column, true);
				Point origin = sheet.getLocationOnScreen();
				cell[0] = new Rectangle(origin.x + bounds.x, origin.y + bounds.y, bounds.width, bounds.height);
			});
			robot.mouseMove(cell[0].x + cell[0].width / 2, cell[0].y + cell[0].height / 2);
			robot.mousePress(InputEvent.BUTTON1_DOWN_MASK); robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
			GuiAcceptanceSupport.await(() -> onEdt(sheet::isFocusOwner), "resource cell did not receive physical focus");
			robot.keyPress(KeyEvent.VK_F2); robot.keyRelease(KeyEvent.VK_F2);
			GuiAcceptanceSupport.await(() -> onEdt(sheet::isEditing), "physical F2 did not open the resource editor");
			java.awt.Component editor = onEdt(sheet::getEditorComponent);
			SwingUtilities.invokeAndWait(editor::requestFocusInWindow);
			GuiAcceptanceSupport.await(() -> onEdt(editor::isFocusOwner), "resource editor did not receive focus");
			key(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_A);
			for (char c : "Edited Resource".toCharArray()) {
				int code = KeyEvent.getExtendedKeyCodeForChar(c);
				if (Character.isUpperCase(c)) robot.keyPress(KeyEvent.VK_SHIFT);
				robot.keyPress(code); robot.keyRelease(code);
				if (Character.isUpperCase(c)) robot.keyRelease(KeyEvent.VK_SHIFT);
			}
			robot.keyPress(KeyEvent.VK_ENTER); robot.keyRelease(KeyEvent.VK_ENTER);
			GuiAcceptanceSupport.await(() -> onEdt(() -> "Edited Resource".equals(resource.getName())), "resource edit did not commit");
			assertEquals("Edited Resource", renderedText(sheet, onEdt(() -> rowFor(sheet, resource)), column));
			key(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Z);
			GuiAcceptanceSupport.await(() -> onEdt(() -> originalName.equals(resource.getName())), "Undo did not restore resource name");
			assertEquals(originalName, renderedText(sheet, onEdt(() -> rowFor(sheet, resource)), column));
			key(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_Y);
			GuiAcceptanceSupport.await(() -> onEdt(() -> "Edited Resource".equals(resource.getName())), "Redo did not restore resource name");
			assertEquals("Edited Resource", renderedText(sheet, onEdt(() -> rowFor(sheet, resource)), column));
			try (var output = Files.newOutputStream(mpo)) {
				assertTrue(new MpoFileImporter().saveProject(reopened, output));
			}
			Project restored;
			try (var input = Files.newInputStream(mpo)) {
				restored = new MpoFileImporter().loadProject(input);
			}
			assertTrue(restored.getResourcePool().getResourceList().stream().anyMatch(item -> "Edited Resource".equals(item.getName())));
		} finally {
			SwingUtilities.invokeAndWait(() -> {
				if (manager[0] != null) manager[0].cleanUp();
				if (window[0] != null) window[0].dispose();
			});
			Environment.setRibbonUI(oldRibbon);
			Environment.setNewLook(oldLook);
			RibbonDisplayPreferences.save(oldMode);
		}
	}

	private static int nameColumn(SpreadSheet sheet) {
		for (int i = 0; i < sheet.getFieldArray().size(); i++)
			if ("Field.name".equals(sheet.getFieldArray().get(i).getId())) return sheet.convertColumnIndexToView(i);
		throw new AssertionError("Name column is missing");
	}

	private static void physicalEditName(Robot robot, SpreadSheet sheet, int row, int column, String value) throws Exception {
		Rectangle cell = onEdt(() -> {
			sheet.scrollRectToVisible(sheet.getCellRect(row, column, true));
			Rectangle bounds = sheet.getCellRect(row, column, true);
			Point origin = sheet.getLocationOnScreen();
			return new Rectangle(origin.x + bounds.x, origin.y + bounds.y, bounds.width, bounds.height);
		});
		robot.mouseMove(cell.x + cell.width / 2, cell.y + cell.height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK); robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> onEdt(sheet::isFocusOwner), "task cell did not receive focus");
		robot.keyPress(KeyEvent.VK_F2); robot.keyRelease(KeyEvent.VK_F2);
		GuiAcceptanceSupport.await(() -> onEdt(sheet::isEditing), "physical F2 did not open task editor");
		java.awt.Component editor = onEdt(sheet::getEditorComponent);
		SwingUtilities.invokeAndWait(editor::requestFocusInWindow);
		GuiAcceptanceSupport.await(() -> onEdt(editor::isFocusOwner), "task editor did not receive focus");
		key(robot, KeyEvent.VK_CONTROL, KeyEvent.VK_A);
		for (char c : value.toCharArray()) {
			int code = KeyEvent.getExtendedKeyCodeForChar(c);
			if (Character.isUpperCase(c)) robot.keyPress(KeyEvent.VK_SHIFT);
			robot.keyPress(code); robot.keyRelease(code);
			if (Character.isUpperCase(c)) robot.keyRelease(KeyEvent.VK_SHIFT);
		}
		robot.keyPress(KeyEvent.VK_ENTER); robot.keyRelease(KeyEvent.VK_ENTER);
	}

	private static <T> T onEdt(java.util.concurrent.Callable<T> action) {
		if (SwingUtilities.isEventDispatchThread()) {
			try { return action.call(); } catch (Exception ex) { throw new RuntimeException(ex); }
		}
		java.util.concurrent.FutureTask<T> task = new java.util.concurrent.FutureTask<>(action);
		try {
			SwingUtilities.invokeAndWait(task);
			return task.get();
		} catch (Exception ex) { throw new RuntimeException(ex); }
	}

	private static String renderedText(SpreadSheet sheet, int row, int column) throws Exception {
		String[] text = new String[1];
		SwingUtilities.invokeAndWait(() -> {
			java.awt.Component renderer = sheet.prepareRenderer(sheet.getCellRenderer(row, column), row, column);
			text[0] = renderer instanceof javax.swing.JLabel label ? label.getText() : renderer.toString();
		});
		return text[0];
	}

	private static int rowFor(SpreadSheet sheet, Resource resource) {
		for (int row = 0; row < sheet.getRowCount(); row++)
			if (((SpreadSheetModel) sheet.getModel()).getNodeForDisplayRow(row).getImpl() == resource) return row;
		return -1;
	}

	private static void key(Robot robot, int modifier, int key) {
		robot.keyPress(modifier); robot.keyPress(key); robot.keyRelease(key); robot.keyRelease(modifier);
		robot.waitForIdle();
	}
}

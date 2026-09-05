/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.DefaultSubProj;
import com.microproject.pm.task.DefaultSubprojectHandler;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;

/** GUI-MSP-READONLY-01: projected read-only child rows reject normal field editing. */
class ReadOnlySubprojectGuiAcceptanceTest {
	private JFrame frame;

	@AfterEach
	void closeWindow() throws Exception {
		if (frame != null)
			SwingUtilities.invokeAndWait(() -> frame.dispose());
	}

	@Test
	void robotCannotEditAReadOnlySubprojectTaskThroughTheMaster() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture();
		show(fixture.sheet);
		GuiAcceptanceSupport.await(() -> frame.isShowing() && fixture.sheet.isShowing(), "master spreadsheet was not visible");

		Robot robot = new Robot();
		robot.setAutoDelay(40);
		clickCell(robot, fixture.sheet, fixture.childRow, fixture.nameColumn);
		GuiAcceptanceSupport.await(() -> fixture.sheet.isFocusOwner(), "read-only child row did not receive focus");
		dispatchF2(fixture.sheet);
		GuiAcceptanceSupport.await(() -> Boolean.TRUE.equals(fixture.sheet.getClientProperty("gui.readOnlyEditAttempt")),
				"the visible F2 edit route was not invoked");
		assertFalse(fixture.sheet.isEditing(), "a read-only projected task must not enter a cell editor");
		assertEquals("Source task must remain unchanged", fixture.childTask.getName());
		capture(robot);
	}

	private Fixture createFixture() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project master = Project.createProject(ResourcePool.createRourcePool("master-pool", undo), undo);
		master.initialize(false, false);
		master.setName("Master read-only projection");
		master.setMaster(true);
		Project child = Project.createProject(ResourcePool.createRourcePool("child-pool", undo), undo);
		child.initialize(false, false);
		child.setName("Read-only source");
		NormalTask childTask = child.createScriptedTask();
		childTask.setName("Source task must remain unchanged");
		DefaultSubProj reference = new DefaultSubProj(master, child.getUniqueId());
		reference.setName("Read-only source");
		reference.setSubprojectFile("C:/plans/read-only-source.mpo");
		reference.setSubprojectReadOnly(true);
		Node referenceNode = NodeFactory.getInstance().createNode(reference);
		master.addToDefaultOutline(null, referenceNode);
		new DefaultSubprojectHandler(master).addSubproject(child, referenceNode, true, false);

		Fixture[] fixture = new Fixture[1];
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
					NodeModelCacheFactory.createTaskNodeModelCache(master, master.getTaskModel()), "read-only-master", null);
			SpreadSheetUtils.setFieldsAndContext(sheet, cache, SpreadSheetCategories.taskSpreadsheetCategory,
					"Spreadsheet.Task.entry", true);
			cache.update();
			int childRow = findRow(sheet, childTask);
			int nameColumn = findNameColumn(sheet);
			sheet.getColumnModel().getColumn(nameColumn).setPreferredWidth(250);
			fixture[0] = new Fixture(sheet, childTask, childRow, nameColumn);
		});
		return fixture[0];
	}

	private void show(SpreadSheet sheet) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			frame = new JFrame("microProject — Read-only subproject GUI acceptance");
			JComponent root = frame.getRootPane();
			root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "EditField");
			ActionMap actions = root.getActionMap();
			actions.put("EditField", new AbstractAction() {
				@Override public void actionPerformed(java.awt.event.ActionEvent event) {
					sheet.putClientProperty("gui.readOnlyEditAttempt", Boolean.TRUE);
					sheet.editActiveCell();
				}
			});
			frame.add(new JLabel("Read-only child projection — F2 edit rejected; source task unchanged", SwingConstants.CENTER),
					BorderLayout.NORTH);
			frame.add(new JScrollPane(sheet), BorderLayout.CENTER);
			frame.setSize(820, 360);
			frame.setLocationByPlatform(true);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
		});
	}

	private static int findRow(SpreadSheet sheet, NormalTask task) {
		for (int row = 0; row < sheet.getRowCount(); row++)
			if (sheet.getTaskAtRow(row) == task)
				return row;
		throw new AssertionError("projected child task was not visible in the master");
	}

	private static int findNameColumn(SpreadSheet sheet) {
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		for (int modelColumn = 0; modelColumn < model.getColumnCount(); modelColumn++)
			if (model.getFieldInColumn(modelColumn) != null && "Field.name".equals(model.getFieldInColumn(modelColumn).getId()))
				return sheet.convertColumnIndexToView(modelColumn);
		throw new AssertionError("Name column was not present");
	}

	private static void clickCell(Robot robot, SpreadSheet sheet, int row, int column) throws Exception {
		Rectangle[] cell = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			Rectangle bounds = sheet.getCellRect(row, column, true);
			Point location = sheet.getLocationOnScreen();
			cell[0] = new Rectangle(location.x + bounds.x, location.y + bounds.y, bounds.width, bounds.height);
		});
		robot.mouseMove(cell[0].x + cell[0].width / 2, cell[0].y + cell[0].height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	private static void dispatchF2(SpreadSheet sheet) throws Exception {
		SwingUtilities.invokeAndWait(() -> KeyboardFocusManager.getCurrentKeyboardFocusManager().dispatchEvent(
				new KeyEvent(sheet, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_F2, KeyEvent.CHAR_UNDEFINED)));
	}

	private void capture(Robot robot) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(frame.getLocationOnScreen(), frame.getSize()));
		Path artifact = Path.of(System.getProperty("micrproject.gui.artifacts.dir", "build/guiTest-artifacts"),
				"master-read-only-projection-edit-rejected.png");
		Files.createDirectories(artifact.getParent());
		BufferedImage image = robot.createScreenCapture(bounds[0]);
		ImageIO.write(image, "png", artifact.toFile());
	}

	private record Fixture(SpreadSheet sheet, NormalTask childTask, int childRow, int nameColumn) { }
}

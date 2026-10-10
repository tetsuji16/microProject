/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.views.UsageDetailView;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.strings.Messages;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;

/** Exercises the Resource Information assignment view through its visible tab. */
class ResourceInformationAssignmentsGuiAcceptanceTest {
	private MainRibbonFrame frame;
	private ResourceInformationDialog dialog;

	@AfterEach
	void closeWindows() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			if (dialog != null)
				dialog.dispose();
			if (frame != null)
				frame.dispose();
		});
	}

	@Test
	void resourceAssignmentsTabShowsAssignedTaskFromPhysicalTabClick() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("resource-information-assignments", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		project.setName("Resource Information assignments");
		NormalTask task = project.createScriptedTask();
		task.setName("Assigned GUI task");
		Resource resource = pool.createScriptedResource();
		resource.setName("Assigned GUI resource");
		AssignmentService.getInstance().newAssignment(task, resource, 1D, 0L, this);

		SwingUtilities.invokeAndWait(() -> {
			frame = new MainRibbonFrame("Resource Information assignment acceptance", null);
			GraphicManager manager = new GraphicManager(frame);
			frame.setGraphicManager(manager);
			manager.initView();
			manager.addProjectFrame(project);
			frame.setSize(1280, 760);
			frame.setLocationByPlatform(true);
			frame.setVisible(true);
			dialog = ResourceInformationDialog.getInstance(frame, resource);
			dialog.setAlwaysOnTop(true);
			dialog.pack();
			dialog.setLocationRelativeTo(frame);
		});
		SwingUtilities.invokeLater(() -> dialog.setVisible(true));
		GuiAcceptanceSupport.await(() -> dialog != null && dialog.isShowing(), "Resource Information dialog did not open");

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		JTabbedPane tabs = findTabs(dialog);
		assertTrue(tabs != null, "Resource Information must expose its tabs");
		int tasksTab = tabs.indexOfTab(Messages.getString("ResourceInformationDialog.Tasks")); //$NON-NLS-1$
		assertTrue(tasksTab >= 0, "Resource Information must expose the assigned Tasks tab");
		Rectangle tabBounds = boundsOnScreen(tabs);
		Rectangle taskTabBounds = tabs.getBoundsAt(tasksTab);
		robot.mouseMove(tabBounds.x + taskTabBounds.x + taskTabBounds.width / 2,
				tabBounds.y + taskTabBounds.y + taskTabBounds.height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> tabs.getSelectedIndex() == tasksTab,
				"Robot click did not select the assigned Tasks tab");

		assertEquals(UsageDetailView.taskAssignmentSpreadsheetCategory,
				dialog.assignmentSpreadSheet.getSpreadSheetCategory(),
				"Resource Information rows are tasks and must use task-assignment columns");
		assertTrue(dialog.assignmentSpreadSheet.getRowCount() > 0,
				"The Resource Information assignment view must display its existing task assignment");
		SpreadSheet sheet = dialog.assignmentSpreadSheet;
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		String displayedTaskName = null;
		for (int viewColumn = 0; viewColumn < sheet.getColumnModel().getColumnCount(); viewColumn++) {
			int modelColumn = sheet.convertColumnIndexToModel(viewColumn);
			if (model.getFieldInColumn(modelColumn).isNameField()) {
				displayedTaskName = String.valueOf(model.getValueAt(0, modelColumn));
				break;
			}
		}
		assertEquals(task.getName(), displayedTaskName,
				"Resource Information must show the assigned task name rather than its resource name");
		Path artifactDirectory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(artifactDirectory);
		ImageIO.write(robot.createScreenCapture(boundsOnScreen(dialog)), "png",
				artifactDirectory.resolve("resource-information-assignments.png").toFile());
	}

	private static JTabbedPane findTabs(Component component) {
		if (component instanceof JTabbedPane tabs)
			return tabs;
		if (component instanceof java.awt.Container container)
			for (Component child : container.getComponents()) {
				JTabbedPane tabs = findTabs(child);
				if (tabs != null)
					return tabs;
			}
		return null;
	}

	private static Rectangle boundsOnScreen(Component component) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(component.getLocationOnScreen(), component.getSize()));
		return bounds[0];
	}
}

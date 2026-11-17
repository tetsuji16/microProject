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
package com.microproject.pm.graphic.frames;

import static com.microproject.testsupport.ResourceIntervalTableFixture.click;
import static com.microproject.testsupport.ResourceIntervalTableFixture.find;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.menu.MenuActionConstants;
import com.microproject.options.GeneralOption;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.views.ResourceView;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.testsupport.GuiRobot;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Alert;
import com.microproject.util.Environment;
import com.microproject.ui.util.SwingAlertPresenter;

/**
 * Issue #997: the row popup fix made the Resource Sheet menu reachable. Its Delete command
 * must remove the clicked resource through the sheet's own action, and the project must
 * then prompt to save.
 */
class ResourceSheetRowPopupGuiAcceptanceTest {
	private MainRibbonFrame frame;
	private boolean confirmDeletes;
	private boolean previousClientSide;

	@AfterEach
	void closeWindow() throws Exception {
		if (frame != null) {
			MainRibbonFrame closing = frame;
			SwingUtilities.invokeAndWait(closing::dispose);
		}
		GeneralOption.getInstance().setConfirmDeletes(confirmDeletes);
		Environment.setClientSide(previousClientSide);
		Alert.setPresenter(null);
	}

	@Test
	void physicalRightClickOnAResourceRowDeletesIt() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"A desktop session is required for Robot acceptance coverage.");
		previousClientSide = Environment.isClientSide();
		Environment.setClientSide(true);
		Alert.setPresenter(new SwingAlertPresenter());
		confirmDeletes = GeneralOption.getInstance().isConfirmDeletes();
		GeneralOption.getInstance().setConfirmDeletes(false);

		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("resource-sheet-popup", undo);
		pool.setLocal(true);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		pool.addProject(project);
		project.setName("Resource sheet popup");
		Resource alpha = pool.createScriptedResource();
		alpha.setName("Alpha");
		Resource bravo = pool.createScriptedResource();
		bravo.setName("Bravo");
		assertEquals(2, pool.getResourceList().size());

		GraphicManager[] manager = new GraphicManager[1];
		SwingUtilities.invokeAndWait(() -> {
			frame = new MainRibbonFrame("resource-sheet-popup", null);
			manager[0] = new GraphicManager(frame);
			frame.setGraphicManager(manager[0]);
			manager[0].initView();
			manager[0].addProjectFrame(project);
			frame.setSize(1280, 760);
			frame.setLocationByPlatform(true);
			frame.setVisible(true);
		});
		SwingUtilities.invokeAndWait(() -> manager[0].getCurrentFrame().activateResourceView());
		DocumentFrame documentFrame = manager[0].getCurrentFrame();
		ResourceView resourceView = documentFrame.getResourceView();
		GuiAcceptanceSupport.await(() -> documentFrame.getActiveTopView() == resourceView,
				"Resource Sheet did not become active");

		GuiRobot robot = new GuiRobot();
		robot.setAutoDelay(45);
		// ResourceView presents a one-time product notice for ordinary projects.
		if (Arrays.stream(Window.getWindows()).anyMatch(candidate -> candidate instanceof Dialog && candidate.isShowing())) {
			robot.keyPress(KeyEvent.VK_ENTER);
			robot.keyRelease(KeyEvent.VK_ENTER);
			GuiAcceptanceSupport.await(() -> Arrays.stream(Window.getWindows())
					.noneMatch(candidate -> candidate instanceof Dialog && candidate.isShowing()),
					"the expected Resource Sheet notice did not close");
		}

		SpreadSheet sheet = resourceView.getSpreadSheet();
		GuiAcceptanceSupport.awaitInputAndEventQueueIdle(robot, "Resource Sheet painted");
		project.markSaved();
		int row = rowForResource(sheet, "Alpha");
		assertTrue(row >= 0, "the test resource must be present in the Resource Sheet rows");

		Rectangle[] cell = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> cell[0] = sheet.getRowHeader().getCellRect(row, 0, true));
		click(robot, sheet.getRowHeader(), cell[0], InputEvent.BUTTON3_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> sheet.getPopup() != null && sheet.getPopup().isVisible(),
				"Physical right-click on a resource row did not open the row popup");
		assertEquals(row, sheet.getPopup().getRow(), "Popup context must resolve the clicked resource row");

		JMenuItem delete = find(sheet.getPopup(), JMenuItem.class,
				c -> ("popup." + MenuActionConstants.ACTION_DELETE).equals(c.getName()));
		assertNotNull(delete, "The Resource Sheet popup must offer Delete");
		click(robot, delete, new Rectangle(delete.getSize()), InputEvent.BUTTON1_DOWN_MASK);

		GuiAcceptanceSupport.await(() -> pool.getResourceList().size() == 1,
				"Popup Delete did not remove the resource");
		assertTrue(project.needsSaving(), "Removing a resource must prompt to save");
	}

	private static int rowForResource(SpreadSheet sheet, String name) {
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		for (int row = 0; row < sheet.getRowCount(); row++) {
			var graphicNode = model.getNode(row);
			if (graphicNode == null)
				continue;
			var node = graphicNode.getNode();
			if (node != null && node.getImpl() instanceof Resource resource && name.equals(resource.getName()))
				return row;
		}
		return -1;
	}
}

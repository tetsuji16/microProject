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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.Calendar;

import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.menu.MenuActionConstants;
import com.microproject.options.GeneralOption;
import com.microproject.pm.availability.AvailabilityTable;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetPopupMenu;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.strings.Messages;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.DateTime;

/** Removes an availability row by physical right-click, then requires the project to prompt for saving (#987). */
class ResourceAvailabilityRowDeleteGuiAcceptanceTest {
	private MainRibbonFrame frame;
	private ResourceInformationDialog dialog;
	private boolean confirmDeletes;

	@AfterEach
	void closeWindows() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			if (dialog != null)
				dialog.dispose();
			if (frame != null)
				frame.dispose();
		});
		GeneralOption.getInstance().setConfirmDeletes(confirmDeletes);
	}

	@Test
	void physicalPopupDeleteOfAvailabilityRowMarksProjectForSaving() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		confirmDeletes = GeneralOption.getInstance().isConfirmDeletes();
		GeneralOption.getInstance().setConfirmDeletes(false);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("resource-availability-delete", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		pool.addProject(project);
		project.setName("Resource availability delete");
		ResourceImpl resource = pool.newResourceInstance();
		resource.setName("Availability GUI resource");
		AvailabilityTable table = resource.getAvailabilityTable();
		table.newValueObject(DateTime.calendarInstance(2030, Calendar.JANUARY, 1).getTimeInMillis());
		assertEquals(2, table.getList().size());

		SwingUtilities.invokeAndWait(() -> {
			frame = new MainRibbonFrame("resource-availability-delete", null);
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
		JTabbedPane tabs = find(dialog, JTabbedPane.class, c -> true);
		assertNotNull(tabs, "Resource Information must expose its tabs");
		int availabilityTab = tabs.indexOfTab(Messages.getString("ResourceInformationDialog.ResourceAvailability")); //$NON-NLS-1$
		assertTrue(availabilityTab >= 0, "Resource Information must expose the Resource Availability tab");
		Rectangle tabBounds = tabs.getBoundsAt(availabilityTab);
		click(robot, tabs, tabBounds, InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> tabs.getSelectedIndex() == availabilityTab,
				"Robot click did not select the Resource Availability tab");

		SpreadSheet sheet = find(dialog, SpreadSheet.class,
				c -> "availabilitySpreadsheet".equals(c.getSpreadSheetCategory()) && c.isShowing()); //$NON-NLS-1$
		assertNotNull(sheet, "The availability table must be showing");
		GuiAcceptanceSupport.awaitInputAndEventQueueIdle(robot, "availability table painted");
		project.markSaved();
		assertFalse(project.needsSaving(), "Opening the dialog must not count as an edit");

		Rectangle[] secondRow = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> secondRow[0] = sheet.getCellRect(1, 1, true));
		click(robot, sheet, secondRow[0], InputEvent.BUTTON3_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> popupVisible(sheet),
				"Physical right-click did not open the availability row popup");
		SpreadSheetPopupMenu popup = sheet.getPopup();
		assertEquals(1, popup.getRow(), "Popup context must resolve the clicked row");

		JMenuItem delete = find(popup, JMenuItem.class,
				c -> ("popup." + MenuActionConstants.ACTION_DELETE).equals(c.getName())); //$NON-NLS-1$
		assertNotNull(delete, "The availability popup must offer Delete");
		click(robot, delete, new Rectangle(delete.getSize()), InputEvent.BUTTON1_DOWN_MASK);

		GuiAcceptanceSupport.await(() -> table.getList().size() == 1, "Popup Delete did not remove the availability row");
		GuiAcceptanceSupport.await(() -> sheet.getRowCount() == 2, "The table must render one data row and the void row");
		assertTrue(project.needsSaving(), "Removing an availability row must prompt to save");
	}

	private static boolean popupVisible(SpreadSheet sheet) {
		boolean[] visible = new boolean[1];
		try {
			SwingUtilities.invokeAndWait(() -> visible[0] = sheet.getPopup() != null && sheet.getPopup().isVisible());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		return visible[0];
	}

	private static void click(Robot robot, Component component, Rectangle area, int button) throws Exception {
		Point[] origin = new Point[1];
		SwingUtilities.invokeAndWait(() -> origin[0] = component.getLocationOnScreen());
		robot.mouseMove(origin[0].x + area.x + area.width / 2, origin[0].y + area.y + area.height / 2);
		robot.mousePress(button);
		robot.mouseRelease(button);
		robot.waitForIdle();
	}

	private static <T extends Component> T find(Component root, Class<T> type, java.util.function.Predicate<T> accept) {
		if (type.isInstance(root) && accept.test(type.cast(root)))
			return type.cast(root);
		if (root instanceof Container container)
			for (Component child : container.getComponents()) {
				T found = find(child, type, accept);
				if (found != null)
					return found;
			}
		return null;
	}
}

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
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.util.Calendar;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.menu.MenuActionConstants;
import com.microproject.options.GeneralOption;
import com.microproject.pm.costing.CostRateTable;
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
import com.microproject.util.Alert;
import com.microproject.util.Environment;
import com.microproject.ui.util.SwingAlertPresenter;

/** Removes a cost rate row by physical right-click and verifies first-row protection (#997). */
class ResourceCostRateRowDeleteGuiAcceptanceTest {
	private MainRibbonFrame frame;
	private ResourceInformationDialog dialog;
	private boolean confirmDeletes;
	private boolean previousClientSide;

	@AfterEach
	void closeWindows() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			if (dialog != null)
				dialog.dispose();
			if (frame != null)
				frame.dispose();
		});
		GeneralOption.getInstance().setConfirmDeletes(confirmDeletes);
		Environment.setClientSide(previousClientSide);
		Alert.setPresenter(null);
	}

	@Test
	void physicalPopupDeleteOfCostRateRowMarksProjectForSaving() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousClientSide = Environment.isClientSide();
		Environment.setClientSide(true);
		Alert.setPresenter(new SwingAlertPresenter());
		confirmDeletes = GeneralOption.getInstance().isConfirmDeletes();
		GeneralOption.getInstance().setConfirmDeletes(false);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("resource-cost-rate-delete", undo);
		pool.setLocal(true);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		pool.addProject(project);
		project.setName("Resource cost rate delete");
		ResourceImpl resource = pool.newResourceInstance();
		resource.setName("Cost rate GUI resource");
		CostRateTable table = resource.getCostRateTable(0);
		table.newValueObject(DateTime.calendarInstance(2030, Calendar.JANUARY, 1).getTimeInMillis());
		assertEquals(2, table.getList().size());

		SwingUtilities.invokeAndWait(() -> {
			frame = new MainRibbonFrame("resource-cost-rate-delete", null);
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
		int costsTab = tabs.indexOfTab(Messages.getString("ResourceInformationDialog.Costs")); //$NON-NLS-1$
		assertTrue(costsTab >= 0, "Resource Information must expose the Costs tab");
		Rectangle tabBounds = tabs.getBoundsAt(costsTab);
		click(robot, tabs, tabBounds, InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> tabs.getSelectedIndex() == costsTab,
				"Robot click did not select the Costs tab");

		SpreadSheet sheet = find(dialog, SpreadSheet.class,
				c -> "costRates".equals(c.getSpreadSheetCategory()) && c.isShowing()); //$NON-NLS-1$
		assertNotNull(sheet, "The cost rate table must be showing");
		GuiAcceptanceSupport.awaitInputAndEventQueueIdle(robot, "cost rate table painted");
		project.markSaved();
		assertFalse(project.needsSaving(), "Opening the dialog must not count as an edit");

		Rectangle[] secondRow = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> secondRow[0] = sheet.getCellRect(1, 1, true));
		click(robot, sheet, secondRow[0], InputEvent.BUTTON3_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> popupVisible(sheet),
				"Physical right-click did not open the cost rate row popup");
		SpreadSheetPopupMenu popup = sheet.getPopup();
		assertEquals(1, popup.getRow(), "Popup context must resolve the clicked row");

		JMenuItem delete = find(popup, JMenuItem.class,
				c -> ("popup." + MenuActionConstants.ACTION_DELETE).equals(c.getName())); //$NON-NLS-1$
		assertNotNull(delete, "The cost rate popup must offer Delete");
		click(robot, delete, new Rectangle(delete.getSize()), InputEvent.BUTTON1_DOWN_MASK);

		GuiAcceptanceSupport.await(() -> table.getList().size() == 1, "Popup Delete did not remove the cost rate row");
		GuiAcceptanceSupport.await(() -> sheet.getRowCount() == 2, "The table must render one data row and the void row");
		assertTrue(project.needsSaving(), "Removing a cost rate row must prompt to save");

		// The first interval is protected. Exercise the actual row-header popup
		// cut route and verify that its rejection does not replace the clipboard.
		var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable previous = clipboard.getContents(null);
		try {
			String marker = "cost-rate-cut-clipboard-marker";
			clipboard.setContents(new StringSelection(marker), null);
			project.markSaved();
			var header = sheet.getRowHeader();
			Rectangle[] firstRow = new Rectangle[1];
			SwingUtilities.invokeAndWait(() -> firstRow[0] = header.getCellRect(0, 0, true));
			click(robot, header, firstRow[0], InputEvent.BUTTON1_DOWN_MASK);
			click(robot, header, firstRow[0], InputEvent.BUTTON3_DOWN_MASK);
			GuiAcceptanceSupport.await(() -> popupVisible(sheet), "Row-header popup did not open");
			JMenuItem cut = find(sheet.getPopup(), JMenuItem.class,
					c -> ("popup." + MenuActionConstants.ACTION_CUT).equals(c.getName()));
			assertNotNull(cut, "The row popup must offer Cut");
			click(robot, cut, new Rectangle(cut.getSize()), InputEvent.BUTTON1_DOWN_MASK);
			GuiAcceptanceSupport.await(() -> visibleError() != null, "Protected row cut must report the refusal");
			JOptionPane error = visibleError();
			assertEquals(Messages.getString("ValueObjectForIntervalTable.YouCannotRemoveTheFirst"), error.getMessage());
			robot.keyPress(java.awt.event.KeyEvent.VK_ENTER);
			robot.keyRelease(java.awt.event.KeyEvent.VK_ENTER);
			GuiAcceptanceSupport.await(() -> visibleError() == null, "Refusal dialog did not close");
			assertEquals(marker, clipboard.getData(DataFlavor.stringFlavor));
			assertEquals(1, table.getList().size());
			assertEquals(2, sheet.getRowCount());
			assertFalse(project.needsSaving(), "A refused cut must not dirty the project");
		} finally {
			if (previous != null) clipboard.setContents(previous, null);
		}
	}

	private static JOptionPane visibleError() {
		JOptionPane[] found = new JOptionPane[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
				for (Window window : Window.getWindows()) {
					if (window.isShowing()) {
						found[0] = find(window, JOptionPane.class,
								p -> p.getMessageType() == JOptionPane.ERROR_MESSAGE);
						if (found[0] != null) break;
					}
				}
			});
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		return found[0];
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

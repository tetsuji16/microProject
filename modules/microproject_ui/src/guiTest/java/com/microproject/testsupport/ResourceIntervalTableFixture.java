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
package com.microproject.testsupport;

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.util.Calendar;

import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import com.microproject.dialog.ResourceInformationDialog;
import com.microproject.interval.ValueObjectForIntervalTable;
import com.microproject.options.GeneralOption;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.strings.Messages;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Alert;
import com.microproject.util.DateTime;
import com.microproject.util.Environment;
import com.microproject.ui.util.SwingAlertPresenter;

/**
 * Opens the Resource Information dialog on a resource whose availability or A cost-rate
 * table has two effective-dated rows, the way a user reaches those tables.
 */
public final class ResourceIntervalTableFixture implements AutoCloseable {
	public final MainRibbonFrame frame;
	public final ResourceInformationDialog dialog;
	public final SpreadSheet sheet;
	public final ValueObjectForIntervalTable table;
	public final Project project;
	public final Robot robot;
	private final boolean previousClientSide;
	private final boolean confirmDeletes;

	private ResourceIntervalTableFixture(MainRibbonFrame frame, ResourceInformationDialog dialog, SpreadSheet sheet,
			ValueObjectForIntervalTable table, Project project, Robot robot, boolean previousClientSide,
			boolean confirmDeletes) {
		this.frame = frame;
		this.dialog = dialog;
		this.sheet = sheet;
		this.table = table;
		this.project = project;
		this.robot = robot;
		this.previousClientSide = previousClientSide;
		this.confirmDeletes = confirmDeletes;
	}

	/** Opens the dialog on the requested table and waits until it is painted. */
	public static ResourceIntervalTableFixture open(boolean costRate) throws Exception {
		boolean previousClientSide = Environment.isClientSide();
		Environment.setClientSide(true);
		Alert.setPresenter(new SwingAlertPresenter());
		boolean confirmDeletes = GeneralOption.getInstance().isConfirmDeletes();
		GeneralOption.getInstance().setConfirmDeletes(false);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("resource-interval-table", undo);
		pool.setLocal(true);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		pool.addProject(project);
		project.setName("Resource interval table");
		ResourceImpl resource = pool.newResourceInstance();
		resource.setName("Interval table resource");
		ValueObjectForIntervalTable table = costRate ? resource.getCostRateTable(0) : resource.getAvailabilityTable();
		table.newValueObject(DateTime.calendarInstance(2030, Calendar.JANUARY, 1).getTimeInMillis());
		if (table.getList().size() != 2)
			throw new IllegalStateException("the fixture needs two effective-dated rows");

		MainRibbonFrame[] frame = new MainRibbonFrame[1];
		ResourceInformationDialog[] dialog = new ResourceInformationDialog[1];
		SwingUtilities.invokeAndWait(() -> {
			frame[0] = new MainRibbonFrame("resource-interval-table", null);
			GraphicManager manager = new GraphicManager(frame[0]);
			frame[0].setGraphicManager(manager);
			manager.initView();
			manager.addProjectFrame(project);
			frame[0].setSize(1280, 760);
			frame[0].setLocationByPlatform(true);
			frame[0].setVisible(true);
			dialog[0] = ResourceInformationDialog.getInstance(frame[0], resource);
			dialog[0].setAlwaysOnTop(true);
			dialog[0].pack();
			dialog[0].setLocationRelativeTo(frame[0]);
		});
		SwingUtilities.invokeLater(() -> dialog[0].setVisible(true));
		GuiAcceptanceSupport.await(() -> dialog[0].isShowing(), "Resource Information dialog did not open");

		Robot robot = new GuiRobot();
		robot.setAutoDelay(40);
		JTabbedPane tabs = find(dialog[0], JTabbedPane.class, c -> true);
		if (tabs == null)
			throw new IllegalStateException("Resource Information must expose its tabs");
		int tab = tabs.indexOfTab(Messages.getString(costRate ? "ResourceInformationDialog.Costs" //$NON-NLS-1$
				: "ResourceInformationDialog.ResourceAvailability")); //$NON-NLS-1$
		if (tab < 0)
			throw new IllegalStateException("Resource Information must expose the table's tab");
		Rectangle tabBounds = tabs.getBoundsAt(tab);
		click(robot, tabs, tabBounds, InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> tabs.getSelectedIndex() == tab, "Robot click did not select the table tab");
		SpreadSheet sheet = find(dialog[0], SpreadSheet.class,
				c -> (costRate ? "costRates" : "availabilitySpreadsheet").equals(c.getSpreadSheetCategory()) && c.isShowing()); //$NON-NLS-1$
		if (sheet == null)
			throw new IllegalStateException("The interval table must be showing");
		GuiAcceptanceSupport.awaitInputAndEventQueueIdle(robot, "interval table painted");
		project.markSaved();
		if (project.needsSaving())
			throw new IllegalStateException("Opening the dialog must not count as an edit");
		return new ResourceIntervalTableFixture(frame[0], dialog[0], sheet, table, project, robot, previousClientSide,
				confirmDeletes);
	}

	/** Selects a row by its view index with the physical left click on the row header. */
	public void selectRowHeader(int row) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = sheet.getRowHeader().getCellRect(row, 0, true));
		click(robot, sheet.getRowHeader(), bounds[0], InputEvent.BUTTON1_DOWN_MASK);
	}

	/** Right-clicks a table cell so the row popup opens on that row. */
	public void rightClickCell(int row, int column) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = sheet.getCellRect(row, column, true));
		click(robot, sheet, bounds[0], InputEvent.BUTTON3_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> popupVisible(sheet), "Physical right-click did not open the row popup");
	}

	public static void click(Robot robot, Component component, Rectangle area, int button) throws Exception {
		Point[] origin = new Point[1];
		SwingUtilities.invokeAndWait(() -> origin[0] = component.getLocationOnScreen());
		robot.mouseMove(origin[0].x + area.x + area.width / 2, origin[0].y + area.y + area.height / 2);
		robot.mousePress(button);
		robot.mouseRelease(button);
		robot.waitForIdle();
	}

	/** The visible error dialog, or null when none is showing. */
	public static JOptionPane visibleError() {
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

	public static boolean popupVisible(SpreadSheet sheet) {
		boolean[] visible = new boolean[1];
		try {
			SwingUtilities.invokeAndWait(
					() -> visible[0] = sheet.getPopup() != null && sheet.getPopup().isVisible());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		return visible[0];
	}

	/** Dismisses an error dialog with the physical Enter key. */
	public void dismissError() throws Exception {
		robot.keyPress(java.awt.event.KeyEvent.VK_ENTER);
		robot.keyRelease(java.awt.event.KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(() -> visibleError() == null, "Refusal dialog did not close");
	}

	public static <T extends Component> T find(Component root, Class<T> type, java.util.function.Predicate<T> accept) {
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

	@Override
	public void close() {
		try {
			SwingUtilities.invokeAndWait(() -> {
				dialog.dispose();
				frame.dispose();
			});
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		GeneralOption.getInstance().setConfirmDeletes(confirmDeletes);
		Environment.setClientSide(previousClientSide);
		Alert.setPresenter(null);
	}
}

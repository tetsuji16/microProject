/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;
import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JComboBox;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.dialog.ResourceSortOrderPanel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.ribbon.RibbonCommandResult;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;

/** Physical header-click regression coverage for ResourceView sorting. */
class ResourceHeaderSortingGuiAcceptanceTest {
	private MainRibbonFrame window;
	private GraphicManager manager;
	private boolean previousRibbonUi;
	private boolean previousNewLook;

	@AfterEach
	void closeWindow() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			if (manager != null) manager.cleanUp();
			for (Window candidate : Window.getWindows())
				if (candidate == window) candidate.dispose();
		});
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
	}

	@Test
	void physicalHeaderClickSortsAndTogglesWithoutMutatingResources() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("resource-header-sort", undo);
		pool.setLocal(true);
		pool.setMaster(true);
		Project project = Project.createProject(pool, undo);
		project.setResourcePoolProject(true);
		project.initialize(false, false);
		Resource zulu = pool.createScriptedResource();
		zulu.setName("Zulu");
		Resource alpha = pool.createScriptedResource();
		alpha.setName("Alpha");
		List<Resource> originalOrder = new ArrayList<>(pool.getResourceList());

		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("resource-header-sort", null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			manager.addProjectFrame(project);
			window.setSize(1120, 700);
			window.setVisible(true);
			manager.getFrameForProject(project).activateResourceView();
		});
		DocumentFrame frame = manager.getFrameForProject(project);
		ResourceView resourceView = frame.getResourceView();
		SpreadSheet sheet = resourceView.getSpreadSheet();
		GuiAcceptanceSupport.await(() -> sheet.isShowing() && sheet.getTableHeader().isShowing() && sheet.getRowCount() >= 2,
			"resource sheet with test resources was not visible");

		int nameModelColumn = -1;
		for (int index = 0; index < sheet.getFieldArray().size(); index++) {
			if ("Field.name".equals(sheet.getFieldArray().get(index).getId())) nameModelColumn = index;
		}
		assertTrue(nameModelColumn >= 0, "resource Name column must be present");
		int nameViewColumn = sheet.convertColumnIndexToView(nameModelColumn);
		JTableHeader header = sheet.getTableHeader();
		Rectangle headerRect = header.getHeaderRect(nameViewColumn);
		assertTrue(java.util.Arrays.stream(header.getMouseListeners()).anyMatch(listener ->
			listener instanceof com.microproject.pm.graphic.spreadsheet.selection.event.HeaderMouseListener),
			"physical resource header must be wired through the shared header listener");
		assertEquals(nameViewColumn, header.columnAtPoint(new Point(headerRect.x + headerRect.width / 2,
			headerRect.y + headerRect.height / 2)), "the Robot target must be the Name header cell");
		Point[] headerLocation = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			window.toFront();
			window.requestFocus();
			assertTrue(header.isShowing(), "resource table header is not on screen: " + componentPath(header));
			headerLocation[0] = header.getLocationOnScreen();
		});
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(50);
		AtomicInteger physicalClicks = new AtomicInteger();
		header.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent event) { physicalClicks.incrementAndGet(); }
		});
		robot.mouseMove(headerLocation[0].x + headerRect.x + headerRect.width / 2,
			headerLocation[0].y + headerRect.y + headerRect.height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		robot.delay(200);
		assertTrue(physicalClicks.get() > 0, "Robot click did not reach the resource header component");
		GuiAcceptanceSupport.await(() -> namedTestResources(sheet).equals(List.of("Alpha", "Zulu")),
			"first physical Name-header click did not sort ascending: " + namedTestResources(sheet));

		SwingUtilities.invokeAndWait(() -> {
			int zuluRow = rowFor(sheet, zulu);
			assertTrue(zuluRow >= 0);
			sheet.setRowSelectionInterval(zuluRow, zuluRow);
		});
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> namedTestResources(sheet).equals(List.of("Zulu", "Alpha")),
			"second physical Name-header click did not sort descending");
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(List.of(zulu), sheet.getSelectedNodes().stream().map(node -> (Resource) node.getImpl()).toList(),
				"sorting should preserve selected resource identity");
			assertEquals(originalOrder, pool.getResourceList(), "view sorting must not reorder the resource model");
			ResourceView.Workspace workspace = (ResourceView.Workspace) resourceView.createWorkspace(0);
			assertEquals("Field.name", workspace.getSortedFieldId());
			assertFalse(workspace.isSortAscending());
		});

		String viewTabText = java.util.ResourceBundle.getBundle("com.microproject.menu.menu").getString("ViewRibbonTask.title");
		click(robot, findShowingButtonByText(window, viewTabText));
		GuiAcceptanceSupport.await(() -> findShowingButtonByCommand(window, "RibbonChooseSort") != null,
			"View ribbon did not expose the Sort command");
		click(robot, findShowingButtonByCommand(window, "RibbonChooseSort"));
		GuiAcceptanceSupport.await(() -> findDialogWith(ResourceSortOrderPanel.class) != null,
			"Resource Sort Order dialog did not open from the View ribbon");
		JDialog sortDialog = findDialogWith(ResourceSortOrderPanel.class);
		ResourceSortOrderPanel orderPanel = findComponent(sortDialog, ResourceSortOrderPanel.class);
		assertEquals("Field.name", orderPanel.getFieldId());
		click(robot, findShowingButtonByText(sortDialog, UIManager.getString("OptionPane.cancelButtonText")));
		GuiAcceptanceSupport.await(() -> manager.getLastRibbonCommandResult() != null
			&& manager.getLastRibbonCommandResult().status() == RibbonCommandResult.Status.NO_CHANGE
			&& "dialog-cancelled".equals(manager.getLastRibbonCommandResult().reason()),
			"cancelling Sort Order did not publish a no-change outcome");
		click(robot, findShowingButtonByCommand(window, "RibbonChooseSort"));
		GuiAcceptanceSupport.await(() -> findDialogWith(ResourceSortOrderPanel.class) != null,
			"Resource Sort Order dialog did not reopen after cancel");
		sortDialog = findDialogWith(ResourceSortOrderPanel.class);
		orderPanel = findComponent(sortDialog, ResourceSortOrderPanel.class);
		assertEquals("Field.name", orderPanel.getFieldId());
		JComboBox<?> direction = flatten(orderPanel).stream().filter(JComboBox.class::isInstance)
			.map(JComboBox.class::cast).filter(combo -> combo.getItemCount() == 2).findFirst()
			.orElseThrow(() -> new AssertionError("Sort Order direction control is missing"));
		click(robot, direction);
		robot.keyPress(KeyEvent.VK_HOME);
		robot.keyRelease(KeyEvent.VK_HOME);
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		robot.waitForIdle();
		click(robot, findShowingButtonByText(sortDialog, UIManager.getString("OptionPane.okButtonText")));
		GuiAcceptanceSupport.await(() -> namedTestResources(sheet).equals(List.of("Alpha", "Zulu")),
			"Sort Order confirmation did not apply ascending Name order");
		assertEquals(RibbonCommandResult.Status.CHANGED, manager.getLastRibbonCommandResult().status());
		assertEquals("RibbonChooseSort", manager.getLastRibbonCommandResult().commandId());
		ResourceView.Workspace persistedWorkspace = (ResourceView.Workspace) resourceView.createWorkspace(0);
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(List.of(zulu), sheet.getSelectedNodes().stream().map(node -> (Resource) node.getImpl()).toList(),
				"Sort Order must preserve selected resource identity");
			assertEquals(originalOrder, pool.getResourceList(), "Sort Order must remain view-only");
			assertEquals("Field.name", persistedWorkspace.getSortedFieldId());
			assertTrue(persistedWorkspace.isSortAscending());
		});
		SwingUtilities.invokeAndWait(() -> resourceView.restoreWorkspace(persistedWorkspace, 0));
		GuiAcceptanceSupport.await(() -> namedTestResources(sheet).equals(List.of("Alpha", "Zulu")),
			"restoring the saved Workspace did not restore the resource sort");
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(List.of(zulu), sheet.getSelectedNodes().stream().map(node -> (Resource) node.getImpl()).toList(),
				"Workspace restore should preserve the selected resource identity");
			assertEquals(originalOrder, pool.getResourceList(), "Workspace sort restore must remain view-only");
		});
	}

	private static void click(Robot robot, Component component) throws Exception {
		Point location = component.getLocationOnScreen();
		robot.mouseMove(location.x + component.getWidth() / 2, location.y + component.getHeight() / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private static AbstractButton findShowingButtonByText(Component root, String text) {
		for (Component component : flatten(root))
			if (component instanceof AbstractButton button && button.isShowing() && text.equals(button.getText())) return button;
		throw new AssertionError("Showing button not found: " + text);
	}

	private static AbstractButton findShowingButtonByCommand(Component root, String command) {
		for (Component component : flatten(root))
			if (component instanceof AbstractButton button && button.isShowing() && command.equals(button.getActionCommand())) return button;
		return null;
	}

	private static JDialog findDialogWith(Class<? extends Component> type) {
		for (Window candidate : Window.getWindows())
			if (candidate instanceof JDialog dialog && dialog.isShowing() && findComponent(dialog, type) != null) return dialog;
		return null;
	}

	private static <T extends Component> T findComponent(Component root, Class<T> type) {
		for (Component component : flatten(root)) if (type.isInstance(component)) return type.cast(component);
		return null;
	}

	private static List<Component> flatten(Component root) {
		List<Component> components = new ArrayList<>();
		components.add(root);
		if (root instanceof Container container)
			for (Component child : container.getComponents()) components.addAll(flatten(child));
		return components;
	}

	private static String componentPath(java.awt.Component component) {
		StringBuilder path = new StringBuilder();
		for (java.awt.Component current = component; current != null; current = current.getParent()) {
			if (!path.isEmpty()) path.append(" <- ");
			path.append(current.getClass().getSimpleName()).append("[visible=").append(current.isVisible())
				.append(",showing=").append(current.isShowing()).append(']');
		}
		return path.toString();
	}

	private static List<String> namedTestResources(SpreadSheet sheet) {
		List<String> names = new ArrayList<>();
		for (int row = 0; row < sheet.getRowCount(); row++) {
			Object value = ((com.microproject.pm.graphic.spreadsheet.SpreadSheetModel) sheet.getModel()).getNodeForDisplayRow(row).getImpl();
			if (value instanceof Resource resource && List.of("Alpha", "Zulu").contains(resource.getName())) names.add(resource.getName());
		}
		return names;
	}

	private static int rowFor(SpreadSheet sheet, Resource resource) {
		for (int row = 0; row < sheet.getRowCount(); row++) {
			Object value = ((com.microproject.pm.graphic.spreadsheet.SpreadSheetModel) sheet.getModel()).getNodeForDisplayRow(row).getImpl();
			if (value == resource) return row;
		}
		return -1;
	}
}

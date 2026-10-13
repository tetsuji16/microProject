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
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;
import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.datatype.Rate;
import com.microproject.field.Field;
import com.microproject.dialog.ResourceSortOrderPanel;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.ribbon.RibbonCommandResult;
import com.microproject.ui.diagnostics.UiButtonDiagnostics;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.Environment;

/** Physical header-click regression coverage for ResourceView sorting. */
class ResourceHeaderSortingGuiAcceptanceTest {
	private MainRibbonFrame window;
	private GraphicManager manager;
	private boolean previousRibbonUi;
	private boolean previousNewLook;
	private String previousUiDebug;
	private Level previousDiagnosticsLevel;
	private Handler diagnosticsHandler;
	private final StringBuffer diagnostics = new StringBuffer();

	@AfterEach
	void closeWindow() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			if (manager != null) manager.cleanUp();
			for (Window candidate : Window.getWindows())
				if (candidate == window) candidate.dispose();
		});
		Environment.setRibbonUI(previousRibbonUi);
		Environment.setNewLook(previousNewLook);
		if (diagnosticsHandler != null) {
			Logger logger = Logger.getLogger(UiButtonDiagnostics.class.getName());
			logger.removeHandler(diagnosticsHandler);
			logger.setLevel(previousDiagnosticsLevel);
		}
		if (previousUiDebug == null) System.clearProperty("microproject.ui.debug");
		else System.setProperty("microproject.ui.debug", previousUiDebug);
	}

	@Test
	void physicalHeaderClickSortsAndTogglesWithoutMutatingResources() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		previousUiDebug = System.getProperty("microproject.ui.debug");
		System.setProperty("microproject.ui.debug", "true");
		Logger diagnosticsLogger = Logger.getLogger(UiButtonDiagnostics.class.getName());
		previousDiagnosticsLevel = diagnosticsLogger.getLevel();
		diagnosticsLogger.setLevel(Level.FINE);
		diagnosticsHandler = new Handler() {
			@Override public void publish(LogRecord record) { diagnostics.append(record.getMessage()).append('\n'); }
			@Override public void flush() { }
			@Override public void close() { }
		};
		diagnosticsLogger.addHandler(diagnosticsHandler);
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
		zulu.setStandardRate(new Rate(100));
		Resource alpha = pool.createScriptedResource();
		alpha.setName("Alpha");
		alpha.setStandardRate(new Rate(50));
		Resource tiedAlpha = pool.createScriptedResource();
		tiedAlpha.setName("Alpha");
		tiedAlpha.setStandardRate(new Rate(50));
		Resource emptyName = pool.createScriptedResource();
		emptyName.setName("");
		emptyName.setStandardRate(new Rate(10));
		List<Resource> testResources = List.of(zulu, alpha, tiedAlpha, emptyName);
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
		GuiAcceptanceSupport.await(() -> visibleTestResources(sheet, testResources)
			.equals(List.of(emptyName, alpha, tiedAlpha, zulu)),
			"first physical Name-header click must sort empty then equal names stably: "
				+ visibleTestResources(sheet, testResources));

		SwingUtilities.invokeAndWait(() -> {
			int zuluRow = rowFor(sheet, zulu);
			assertTrue(zuluRow >= 0);
			sheet.setRowSelectionInterval(zuluRow, zuluRow);
			assertEquals(List.of(zuluRow), java.util.Arrays.stream(sheet.getSelectedRows()).boxed().toList(),
				"the fixture must select only Zulu before the descending sort");
			assertEquals(List.of(zulu), sheet.getSelectedNodes().stream()
				.map(node -> (Resource) node.getImpl()).toList(),
				"the selected model identity must be Zulu before the descending sort");
		});
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> visibleTestResources(sheet, testResources)
			.equals(List.of(zulu, alpha, tiedAlpha, emptyName)),
			"second physical Name-header click must sort descending and retain equal-name order");
		SwingUtilities.invokeAndWait(() -> {
			int projectionSize = resourceView.getCache().getVisibleNodes().getProjectionIndex().size();
			int[] selectedRows = sheet.getSelectedRows();
			assertTrue(java.util.Arrays.stream(selectedRows).allMatch(row -> row >= 0 && row < projectionSize),
				"selected JTable rows must belong to the published projection; rowCount=" + sheet.getRowCount()
					+ ", projectionSize=" + projectionSize + ", selectedRows="
					+ java.util.Arrays.toString(selectedRows) + ", projected=" + projectedResources(resourceView));
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

		// Document-derived contract: Microsoft documents View > Sort > Sort By in Project desktop;
		// expose that existing Sort Order action from its split-menu affordance, without adding a
		// second sort implementation. See https://support.microsoft.com/en-us/project/sort-tasks-resources-or-dates-in-project-desktop
		AbstractButton sortButton = findShowingButtonByCommand(window, "RibbonChooseSort");
		Point sortButtonLocation = sortButton.getLocationOnScreen();
		robot.mouseMove(sortButtonLocation.x + sortButton.getWidth() - 8,
			sortButtonLocation.y + sortButton.getHeight() / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
		JPopupMenu sortMenu = (JPopupMenu) sortButton.getClientProperty("MicroProject.ribbon.sortMenu");
		GuiAcceptanceSupport.await(() -> sortMenu != null && sortMenu.isShowing(),
			"the Sort split-button chevron did not open its menu");
		JMenuItem sortOrderItem = findComponent(sortMenu, JMenuItem.class);
		assertEquals(java.util.ResourceBundle.getBundle("com.microproject.menu.menu").getString("Sort.text"),
			sortOrderItem.getText());
		assertEquals("RibbonChooseSort", sortOrderItem.getActionCommand());
		assertEquals(sortButton.getAction(), sortOrderItem.getAction(),
			"the menu item and ribbon command must share the canonical action");
		AbstractButton sortMenuButton = (AbstractButton) sortButton.getClientProperty("MicroProject.ribbon.sortMenuButton");
		assertTrue(sortMenuButton.isFocusable(), "Sort menu arrow must be keyboard accessible");
		assertEquals(java.util.ResourceBundle.getBundle("com.microproject.menu.menu")
			.getString("SortMenu.accessibleName"), sortMenuButton.getAccessibleContext().getAccessibleName());
		robot.keyPress(KeyEvent.VK_ESCAPE);
		robot.keyRelease(KeyEvent.VK_ESCAPE);
		robot.waitForIdle();
		SwingUtilities.invokeAndWait(sortMenuButton::requestFocusInWindow);
		robot.keyPress(KeyEvent.VK_SPACE);
		robot.keyRelease(KeyEvent.VK_SPACE);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(sortMenu::isShowing, "Space on the accessible Sort menu arrow did not open its menu");
		click(robot, sortOrderItem);
		GuiAcceptanceSupport.await(() -> findDialogWith(ResourceSortOrderPanel.class) != null,
			"Sort Order menu item did not use the existing resource sort dialog");
		JDialog menuSortDialog = findDialogWith(ResourceSortOrderPanel.class);
		ResourceSortOrderPanel menuOrderPanel = findComponent(menuSortDialog, ResourceSortOrderPanel.class);
		assertEquals("Field.name", menuOrderPanel.getFieldId());
		List<Field> sortFields = resourceView.getResourceSortFields();
		int standardRateIndex = java.util.stream.IntStream.range(0, sortFields.size())
			.filter(index -> "Field.standardRate".equals(sortFields.get(index).getId())).findFirst()
			.orElseThrow(() -> new AssertionError("Resource Standard Rate field is missing"));
		JComboBox<?> fieldChoice = flatten(menuSortDialog).stream().filter(JComboBox.class::isInstance)
			.map(JComboBox.class::cast).filter(combo -> combo.getItemCount() == sortFields.size()).findFirst()
			.orElseThrow(() -> new AssertionError("Sort Order field control is missing"));
		GuiAcceptanceSupport.selectComboBoxItem(robot, fieldChoice, standardRateIndex);
		JComboBox<?> menuDirection = flatten(menuSortDialog).stream().filter(JComboBox.class::isInstance)
			.map(JComboBox.class::cast).filter(combo -> combo.getItemCount() == 2).findFirst()
			.orElseThrow(() -> new AssertionError("Sort Order direction control is missing"));
		GuiAcceptanceSupport.selectComboBoxItem(robot, menuDirection, 0);
		click(robot, findShowingButtonByText(menuSortDialog, UIManager.getString("OptionPane.okButtonText")));
		GuiAcceptanceSupport.await(() -> visibleTestResources(sheet, testResources)
			.equals(List.of(emptyName, alpha, tiedAlpha, zulu)),
			"Sort menu should sort numeric Standard Rate ascending and retain equal values stably");
		assertEquals(RibbonCommandResult.Status.CHANGED, manager.getLastRibbonCommandResult().status());
		assertEquals("Field.standardRate", resourceView.getSortedFieldId());
		assertTrue(resourceView.isSortAscending());
		assertEquals(originalOrder, pool.getResourceList(), "numeric view sorting must not reorder the resource model");
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(List.of(zulu), sheet.getSelectedNodes().stream().map(node -> (Resource) node.getImpl()).toList(),
				"numeric sorting must preserve selected resource identity");
			ResourceView.Workspace workspace = (ResourceView.Workspace) resourceView.createWorkspace(0);
			assertEquals("Field.standardRate", workspace.getSortedFieldId());
			assertTrue(workspace.isSortAscending());
		});

		click(robot, findShowingButtonByCommand(window, "RibbonChooseSort"));
		GuiAcceptanceSupport.await(() -> findDialogWith(ResourceSortOrderPanel.class) != null,
			"Resource Sort Order dialog did not open from the View ribbon");
		JDialog sortDialog = findDialogWith(ResourceSortOrderPanel.class);
		ResourceSortOrderPanel orderPanel = findComponent(sortDialog, ResourceSortOrderPanel.class);
		assertEquals("Field.standardRate", orderPanel.getFieldId());
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
		assertEquals("Field.standardRate", orderPanel.getFieldId());
		JComboBox<?> ribbonFieldChoice = flatten(sortDialog).stream().filter(JComboBox.class::isInstance)
			.map(JComboBox.class::cast).filter(combo -> combo.getItemCount() == sortFields.size()).findFirst()
			.orElseThrow(() -> new AssertionError("Sort Order field control is missing"));
		int nameFieldIndex = java.util.stream.IntStream.range(0, sortFields.size())
			.filter(index -> "Field.name".equals(sortFields.get(index).getId())).findFirst()
			.orElseThrow(() -> new AssertionError("Resource Name field is missing"));
		GuiAcceptanceSupport.selectComboBoxItem(robot, ribbonFieldChoice, nameFieldIndex);
		JComboBox<?> direction = flatten(orderPanel).stream().filter(JComboBox.class::isInstance)
			.map(JComboBox.class::cast).filter(combo -> combo.getItemCount() == 2).findFirst()
			.orElseThrow(() -> new AssertionError("Sort Order direction control is missing"));
		GuiAcceptanceSupport.selectComboBoxItem(robot, direction, 0);
		click(robot, findShowingButtonByText(sortDialog, UIManager.getString("OptionPane.okButtonText")));
		GuiAcceptanceSupport.await(() -> visibleTestResources(sheet, testResources)
			.equals(List.of(emptyName, alpha, tiedAlpha, zulu)),
			"Sort Order confirmation did not apply ascending Name order with empty/equal values");
		assertEquals(RibbonCommandResult.Status.CHANGED, manager.getLastRibbonCommandResult().status());
		assertEquals("RibbonChooseSort", manager.getLastRibbonCommandResult().commandId());
		GuiAcceptanceSupport.await(() -> diagnostics.toString().lines().anyMatch(line ->
			line.contains("UI_COMMAND id=RibbonChooseSort") && line.contains("viewChanged=true")),
			"a view-only Resource Sort change must be observable through the active sheet projection");
		assertFalse(diagnostics.toString().contains(
			"UI_COMMAND_FAILURE id=RibbonChooseSort reason=reported-change-not-observed"),
			"the successful view-only sort must not produce a false no-op diagnostic");
		ResourceView.Workspace persistedWorkspace = (ResourceView.Workspace) resourceView.createWorkspace(0);
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(List.of(zulu), sheet.getSelectedNodes().stream().map(node -> (Resource) node.getImpl()).toList(),
				"Sort Order must preserve selected resource identity");
			assertEquals(originalOrder, pool.getResourceList(), "Sort Order must remain view-only");
			assertEquals("Field.name", persistedWorkspace.getSortedFieldId());
			assertTrue(persistedWorkspace.isSortAscending());
		});
		SwingUtilities.invokeAndWait(() -> resourceView.restoreWorkspace(persistedWorkspace, 0));
		GuiAcceptanceSupport.await(() -> visibleTestResources(sheet, testResources)
			.equals(List.of(emptyName, alpha, tiedAlpha, zulu)),
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

	private static List<Resource> visibleTestResources(SpreadSheet sheet, List<Resource> testResources) {
		List<Resource> resources = new ArrayList<>();
		for (int row = 0; row < sheet.getRowCount(); row++) {
			Object value = ((com.microproject.pm.graphic.spreadsheet.SpreadSheetModel) sheet.getModel()).getNodeForDisplayRow(row).getImpl();
			if (value instanceof Resource resource && testResources.contains(resource)) resources.add(resource);
		}
		return resources;
	}

	private static List<String> projectedResources(ResourceView resourceView) {
		return resourceView.getCache().getVisibleNodes().getProjectionIndex().nodes().stream()
			.map(node -> node.getNode().getImpl() instanceof Resource resource
				? resource.getId() + ":" + resource.getName() : node.getNode().getImpl().getClass().getSimpleName())
			.toList();
	}

	private static int rowFor(SpreadSheet sheet, Resource resource) {
		for (int row = 0; row < sheet.getRowCount(); row++) {
			Object value = ((com.microproject.pm.graphic.spreadsheet.SpreadSheetModel) sheet.getModel()).getNodeForDisplayRow(row).getImpl();
			if (value == resource) return row;
		}
		return -1;
	}
}

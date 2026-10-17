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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import com.microproject.exchange.MpoFileImporter;
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
		assertEquals("Field.name", menuOrderPanel.getKeys().get(0).fieldId());
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
		assertEquals("Field.standardRate", orderPanel.getKeys().get(0).fieldId());
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
		assertEquals("Field.standardRate", orderPanel.getKeys().get(0).fieldId());
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
		// Multi-key: Name ascending, then Standard Rate descending breaks the former Alpha tie.
		SwingUtilities.invokeAndWait(() -> tiedAlpha.setStandardRate(new Rate(75)));
		click(robot, findShowingButtonByCommand(window, "RibbonChooseSort"));
		GuiAcceptanceSupport.await(() -> findDialogWith(ResourceSortOrderPanel.class) != null,
			"Resource Sort Order dialog did not reopen for the multi-key step");
		JDialog multiKeyDialog = findDialogWith(ResourceSortOrderPanel.class);
		JComboBox<?> secondField = (JComboBox<?>) flatten(multiKeyDialog).stream()
			.filter(component -> "microproject.resourceSort.key2".equals(component.getName())).findFirst()
			.orElseThrow(() -> new AssertionError("Second sort key control is missing"));
		JComboBox<?> secondDirection = (JComboBox<?>) flatten(multiKeyDialog).stream()
			.filter(component -> "microproject.resourceSort.direction2".equals(component.getName())).findFirst()
			.orElseThrow(() -> new AssertionError("Second sort direction control is missing"));
		// Resolve the row by its rendered label instead of index arithmetic over the optional "(none)" row.
		String standardRateLabel = sortFields.get(standardRateIndex).getName();
		int secondStandardRateRow = java.util.stream.IntStream.range(0, secondField.getItemCount())
			.filter(row -> standardRateLabel.equals(String.valueOf(secondField.getItemAt(row)))).findFirst()
			.orElseThrow(() -> new AssertionError("Standard Rate is missing from the second sort key"));
		GuiAcceptanceSupport.selectComboBoxItem(robot, secondField, secondStandardRateRow);
		ResourceSortOrderPanel multiKeyPanel = findComponent(multiKeyDialog, ResourceSortOrderPanel.class);
		GuiAcceptanceSupport.selectComboBoxItem(robot, secondDirection, 1);
		assertEquals(List.of(new ResourceView.SortKey("Field.name", true), new ResourceView.SortKey("Field.standardRate", false)),
			multiKeyPanel.getKeys(), "dialog keys before OK; items=" + java.util.stream.IntStream.range(0, secondField.getItemCount())
				.mapToObj(secondField::getItemAt).toList());
		click(robot, findShowingButtonByText(multiKeyDialog, UIManager.getString("OptionPane.okButtonText")));
		try { GuiAcceptanceSupport.await(() -> visibleTestResources(sheet, testResources)
			.equals(List.of(emptyName, tiedAlpha, alpha, zulu)),
			"the second key (Standard Rate descending) did not break the Name tie");
		} catch (AssertionError failure) {
			throw new AssertionError("multi-key sort mismatch: visible=" + visibleTestResources(sheet, testResources).stream().map(r -> r.getName() + "@" + r.getStandardRate()).toList()
				+ " keys=" + resourceView.getSortKeys() + " result=" + manager.getLastRibbonCommandResult()
				+ " key2=" + secondField.getSelectedItem() + " dir2=" + secondDirection.getSelectedIndex(), failure);
		}
		assertEquals(RibbonCommandResult.Status.CHANGED, manager.getLastRibbonCommandResult().status());
		ResourceView.Workspace multiKeyWorkspace = (ResourceView.Workspace) resourceView.createWorkspace(0);
		assertEquals("Field.name", multiKeyWorkspace.getSortedFieldId());
		assertEquals(List.of("-Field.standardRate"), List.of(multiKeyWorkspace.getAdditionalSortKeys()));
		assertEquals(originalOrder, pool.getResourceList(), "multi-key sorting must remain view-only");
		SwingUtilities.invokeAndWait(() -> resourceView.restoreWorkspace(multiKeyWorkspace, 0));
		GuiAcceptanceSupport.await(() -> visibleTestResources(sheet, testResources)
			.equals(List.of(emptyName, tiedAlpha, alpha, zulu)),
			"restoring the saved Workspace did not restore both sort keys");
	}

	@Test
	void physicalResourceStandardRateEditUndoRedoAndMpoRoundTrip() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		previousRibbonUi = Environment.isRibbonUI();
		previousNewLook = Environment.isNewLook();
		Environment.setRibbonUI(true);
		Environment.setNewLook(true);
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("resource-field-edit", undo);
		pool.setLocal(true);
		pool.setMaster(true);
		Project project = Project.createProject(pool, undo);
		project.setResourcePoolProject(true);
		project.initialize(false, false);
		pool.addProject(project);
		Resource resource = pool.createScriptedResource();
		resource.setName("Field Edit Resource");
		resource.setStandardRate(new Rate(20.0D
			/ com.microproject.datatype.Duration.timeUnitFactor(com.microproject.datatype.TimeUnit.HOURS),
			com.microproject.datatype.TimeUnit.HOURS));
		SwingUtilities.invokeAndWait(() -> {
			window = new MainRibbonFrame("resource-field-edit", null);
			manager = new GraphicManager(window);
			window.setGraphicManager(manager);
			manager.initView();
			manager.addProjectFrame(project);
			window.setSize(1120, 700);
			window.setVisible(true);
			manager.getFrameForProject(project).activateResourceView();
		});
		SpreadSheet sheet = manager.getFrameForProject(project).getResourceView().getSpreadSheet();
		GuiAcceptanceSupport.await(() -> sheet.isShowing() && rowFor(sheet, resource) >= 0,
			"resource spreadsheet did not show the edited resource");
		int modelColumn = -1;
		for (int index = 0; index < sheet.getFieldArray().size(); index++)
			if ("Field.standardRate".equals(sheet.getFieldArray().get(index).getId())) modelColumn = index;
		assertTrue(modelColumn >= 0, "resource Standard Rate column must be present");
		int column = sheet.convertColumnIndexToView(modelColumn);
		int row = rowFor(sheet, resource);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(45);
		clickVisibleCell(robot, window, sheet, row, column);
		try {
			GuiAcceptanceSupport.await(sheet::isFocusOwner, "Standard Rate cell did not receive physical focus");
		} catch (AssertionError failure) {
			String[] focusState = new String[1];
			SwingUtilities.invokeAndWait(() -> focusState[0] = "active=" + window.isActive() + ",focused="
				+ window.isFocused() + ",focusOwner="
				+ java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()
				+ ",sheet=" + sheet.isFocusOwner() + ",targetCell=" + row + "," + column);
			throw new AssertionError(failure.getMessage() + "; physical focus diagnostics: " + focusState[0], failure);
		}
		robot.keyPress(KeyEvent.VK_F2);
		robot.keyRelease(KeyEvent.VK_F2);
		GuiAcceptanceSupport.await(sheet::isEditing, "physical F2 did not start the Standard Rate editor");
		java.awt.Component editor = sheet.getEditorComponent();
		assertTrue(editor != null, "Standard Rate editor is missing");
		SwingUtilities.invokeAndWait(editor::requestFocusInWindow);
		GuiAcceptanceSupport.await(() -> editor.isFocusOwner(), "Standard Rate editor did not receive focus");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		for (char character : "137.5".toCharArray()) {
			int keyCode = KeyEvent.getExtendedKeyCodeForChar(character);
			robot.keyPress(keyCode);
			robot.keyRelease(keyCode);
		}
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(() -> !sheet.isEditing(), "Standard Rate edit did not commit");
		double expectedEditRate = 137.5D
			/ com.microproject.datatype.Duration.timeUnitFactor(com.microproject.datatype.TimeUnit.HOURS);
		assertEquals(expectedEditRate, resource.getStandardRate().getValue(), 0.0000001D,
			"physical input 137.5 must commit as the expected internal currency-per-millisecond rate");
		assertEquals(com.microproject.datatype.TimeUnit.HOURS, resource.getStandardRate().getTimeUnit());
		String editedRendered = renderedRateText(sheet, rowFor(sheet, resource), column);
		assertEquals(expectedRateText(sheet, rowFor(sheet, resource), column,
			new Rate(expectedEditRate, com.microproject.datatype.TimeUnit.HOURS)), editedRendered,
			"ResourceView must render the committed 137.5/hour value");
		robot.keyPress(KeyEvent.VK_CONTROL); robot.keyPress(KeyEvent.VK_Z);
		robot.keyRelease(KeyEvent.VK_Z); robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.waitForIdle();
		double expectedInitialRate = 20.0D
			/ com.microproject.datatype.Duration.timeUnitFactor(com.microproject.datatype.TimeUnit.HOURS);
		GuiAcceptanceSupport.await(() -> Math.abs(resource.getStandardRate().getValue() - expectedInitialRate) < 0.0000001D,
			"physical Ctrl+Z did not restore the initial 20/hour rate");
		assertEquals(expectedRateText(sheet, rowFor(sheet, resource), column,
			new Rate(expectedInitialRate, com.microproject.datatype.TimeUnit.HOURS)),
			renderedRateText(sheet, rowFor(sheet, resource), column), "Undo must refresh the rendered 20/hour value");
		robot.keyPress(KeyEvent.VK_CONTROL); robot.keyPress(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_Y); robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.waitForIdle();
		GuiAcceptanceSupport.await(() -> Math.abs(resource.getStandardRate().getValue() - expectedEditRate) < 0.0000001D,
			"physical Ctrl+Y did not restore the 137.5/hour rate");
		assertEquals(expectedRateText(sheet, rowFor(sheet, resource), column,
			new Rate(expectedEditRate, com.microproject.datatype.TimeUnit.HOURS)),
			renderedRateText(sheet, rowFor(sheet, resource), column), "Redo must refresh the rendered 137.5/hour value");
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, saved), "MPO save rejected the physical rate edit");
		Project restored = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		Resource restoredResource = restored.getResourcePool().getResourceList().stream()
			.filter(candidate -> "Field Edit Resource".equals(candidate.getName())).findFirst().orElseThrow();
		assertEquals(expectedEditRate, restoredResource.getStandardRate().getValue(), 0.0000001D,
			"MPO reopen lost the physically edited Standard Rate");
	}

	private static String renderedRateText(SpreadSheet sheet, int row, int column) throws Exception {
		String[] text = new String[1];
		SwingUtilities.invokeAndWait(() -> {
			Component rendered = sheet.prepareRenderer(sheet.getCellRenderer(row, column), row, column);
			text[0] = rendered instanceof javax.swing.JLabel label ? label.getText() : rendered.toString();
		});
		return text[0];
	}

	private static void clickVisibleCell(Robot robot, MainRibbonFrame window, SpreadSheet sheet, int row, int column)
		throws Exception {
		Rectangle[] target = new Rectangle[1];
		AtomicInteger pressedRow = new AtomicInteger(-1);
		AtomicInteger pressedColumn = new AtomicInteger(-1);
		MouseAdapter pressObserver = new MouseAdapter() {
			@Override public void mousePressed(MouseEvent event) {
				pressedRow.set(sheet.rowAtPoint(event.getPoint()));
				pressedColumn.set(sheet.columnAtPoint(event.getPoint()));
			}
		};
		SwingUtilities.invokeAndWait(() -> {
			window.toFront();
			window.requestFocus();
			sheet.scrollRectToVisible(sheet.getCellRect(row, column, true));
			sheet.addMouseListener(pressObserver);
		});
		GuiAcceptanceSupport.await(() -> window.isActive() && window.isFocused(),
			"resource document window did not become active before the cell click");
		SwingUtilities.invokeAndWait(() -> {
			Rectangle cell = sheet.getCellRect(row, column, true);
			Rectangle visible = sheet.getVisibleRect();
			assertTrue(visible.contains(cell), "Standard Rate cell must be fully visible before clicking; cell=" + cell
				+ ", viewport=" + visible + ", table=" + sheet.getBounds());
			Point location = sheet.getLocationOnScreen();
			target[0] = new Rectangle(location.x + cell.x, location.y + cell.y, cell.width, cell.height);
		});
		try {
			robot.mouseMove(target[0].x + target[0].width / 2, target[0].y + target[0].height / 2);
			robot.waitForIdle();
			int[] hoveredCell = {-1, -1};
			Point[] localPointer = new Point[1];
			SwingUtilities.invokeAndWait(() -> {
				localPointer[0] = sheet.getMousePosition();
				if (localPointer[0] != null) {
					hoveredCell[0] = sheet.rowAtPoint(localPointer[0]);
					hoveredCell[1] = sheet.columnAtPoint(localPointer[0]);
				}
			});
			assertEquals(row, hoveredCell[0], "Robot hover must reach the visible Standard Rate row; pointer="
				+ localPointer[0] + ", screenCell=" + target[0]);
			assertEquals(column, hoveredCell[1], "Robot hover must reach the visible Standard Rate column; pointer="
				+ localPointer[0] + ", screenCell=" + target[0]);
			robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
			robot.waitForIdle();
		} finally {
			SwingUtilities.invokeAndWait(() -> sheet.removeMouseListener(pressObserver));
		}
		assertEquals(row, pressedRow.get(), "physical mouse press must reach the visible Standard Rate row");
		assertEquals(column, pressedColumn.get(), "physical mouse press must reach the visible Standard Rate column");
	}

	private static String expectedRateText(SpreadSheet sheet, int row, int column, Rate rate) throws Exception {
		String[] text = new String[1];
		SwingUtilities.invokeAndWait(() -> text[0] = com.microproject.pm.graphic.spreadsheet.renderer.RateRenderer
			.getFormat(sheet, row, column).format(rate).toString());
		return text[0];
	}

	private static void click(Robot robot, Component component) throws Exception {
		Point location = component.getLocationOnScreen();
		robot.mouseMove(location.x + component.getWidth() / 2, location.y + component.getHeight() / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.waitForIdle();
	}

	private static AbstractButton findShowingButtonByText(Component root, String text) {
		try {
			return GuiAcceptanceSupport.findShowingButtonByText(root, text);
		} catch (Exception exception) {
			throw new AssertionError("Unable to locate showing button for semantic label: " + text, exception);
		}
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

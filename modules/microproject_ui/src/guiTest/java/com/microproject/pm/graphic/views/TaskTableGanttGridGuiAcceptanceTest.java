/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Point;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellRenderer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.configuration.Dictionary;
import com.microproject.configuration.Configuration;
import com.microproject.datatype.Duration;
import com.microproject.datatype.DurationFormat;
import com.microproject.datatype.TimeUnit;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.gantt.GanttUI;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.GuiAcceptanceShortcutInstaller;
import com.microproject.pm.graphic.frames.workspace.FrameHolder;
import com.microproject.pm.graphic.frames.workspace.FrameManager;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetColumnModel;
import com.microproject.pm.graphic.spreadsheet.renderer.NameCellComponent;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetColumnMenu;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetPopupMenu;
import com.microproject.pm.graphic.spreadsheet.common.SpreadSheetRowHeader;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.graphic.spreadsheet.editor.DateEditor;
import com.microproject.pm.graphic.spreadsheet.selection.SpreadSheetColumnsPopupMenu;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.graphic.views.synchro.ScrollPaneSynchronizer;
import com.microproject.pm.graphic.views.synchro.Synchronizer;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.options.CalendarOption;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.NormalTask;
import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.GanttProgress;
import com.microproject.workspace.SavableToWorkspace;
import com.microproject.workspace.WorkspaceSetting;

/** Verifies that the visible task-table/Gantt pair shares one grid-style path. */
class TaskTableGanttGridGuiAcceptanceTest {
	private JFrame frame;
	private Gantt gantt;
	private TaskSelectionController selectionController;
	private com.microproject.pm.graphic.timescale.ScaledScrollPane ganttTimescalePane;
	private GraphicManager popupGraphicManager;
	private GraphicManager productionGraphicManager;
	@AfterEach
	void closeWindow() throws Exception {
		if (popupGraphicManager != null) {
			SwingUtilities.invokeAndWait(() -> popupGraphicManager.cleanUp());
			popupGraphicManager = null;
		}
		if (productionGraphicManager != null) {
			SwingUtilities.invokeAndWait(() -> productionGraphicManager.cleanUp());
			productionGraphicManager = null;
		}
		if (selectionController != null) {
			SwingUtilities.invokeAndWait(() -> selectionController.close());
			selectionController = null;
		}
		if (frame != null) {
			SwingUtilities.invokeAndWait(() -> {
				frame.dispose();
				frame = null;
			});
		}
		if (gantt != null) {
			gantt.cleanUp();
			gantt = null;
		}
	}

	@Test
	void visibleTaskTableAndGanttRemainLaidOutWhenTheirSharedGridStyleChanges() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture();
		showFixture(fixture);

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.gantt.isShowing(), "task table or Gantt was not visible");
		robot.delay(500);
		assertTrue(hasRenderedGanttNode(fixture.gantt), "Gantt must render at least one task node");
		SwingUtilities.invokeAndWait(() -> {
			String tableName = fixture.sheet.getAccessibleContext().getAccessibleName();
			String rowHeaderName = fixture.sheet.getRowHeader().getAccessibleContext().getAccessibleName();
			assertTrue(tableName != null && !tableName.isBlank(),
				"the visible task table must expose an accessible name");
			assertTrue(rowHeaderName != null && !rowHeaderName.isBlank(),
				"the visible task row header must expose an accessible name");
			assertFalse(tableName.equals(rowHeaderName),
				"the task table and row-header accessible names must be distinct");
		});
		captureVisibleLayout(robot);

		SwingUtilities.invokeAndWait(() -> {
			assertTrue(fixture.sheet.getWidth() > 200 && fixture.sheet.getHeight() > 150, "task table layout collapsed");
			assertTrue(fixture.gantt.getWidth() > 200 && fixture.gantt.getHeight() > 150, "Gantt layout collapsed");

			TaskGanttSyncSupport.applySpreadsheetGridStyle(fixture.sheet, fixture.gantt, false, FlatUiSupport.tableGridColor());
			assertFalse(fixture.sheet.getShowHorizontalLines());
			assertFalse(fixture.sheet.getShowVerticalLines());
			assertFalse(fixture.sheet.getRowHeader().getShowHorizontalLines());
			assertFalse(fixture.gantt.isGridLinesVisible());

			TaskGanttSyncSupport.applySpreadsheetGridStyle(fixture.sheet, fixture.gantt, true, FlatUiSupport.tableGridColor());
			assertTrue(fixture.sheet.getShowHorizontalLines());
			assertTrue(fixture.sheet.getShowVerticalLines());
			assertTrue(fixture.sheet.getRowHeader().getShowHorizontalLines());
			assertTrue(fixture.gantt.isGridLinesVisible());
		});
	}

	private static boolean hasRenderedGanttNode(Gantt chart) throws Exception {
		boolean[] rendered = new boolean[1];
		SwingUtilities.invokeAndWait(() -> {
			for (int y = 0; y < Math.min(chart.getHeight(), chart.getRowHeight() * 20) && !rendered[0]; y += 2) {
				for (int x = 0; x < Math.min(chart.getWidth(), 1200) && !rendered[0]; x += 2) {
					rendered[0] = chart.getUI().getNodeAt(x, y) != null;
				}
			}
		});
		return rendered[0];
	}

	@Test
	void twentyMixedTasksRemainAccessibleAfterMouseScrollbarClick() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(20);
		showFixture(fixture);

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
			fixture.sheet.requestFocusInWindow();
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.gantt.isShowing(), "20-task table or Gantt was not visible");
		int initialRows = ((com.microproject.pm.graphic.spreadsheet.SpreadSheetModel) fixture.sheet.getModel()).getRowCount();
		assertTrue(initialRows >= 20, "all 20 task rows must be present before scrolling");
		assertEquals(20, fixture.taskCount, "fixture must contain exactly 20 tasks");
		assertEquals(9, fixture.sequentialDependencyCount, "first ten tasks must form one FS chain");
		assertEquals(10, fixture.independentTaskCount, "last ten tasks must remain independent");
		JScrollPane tableScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, fixture.sheet);
		assertTrue(tableScroll != null, "20-task table must be hosted by a scroll pane");
		assertTrue(tableScroll.getVerticalScrollBar().getMaximum() > tableScroll.getVerticalScrollBar().getVisibleAmount(),
			"20-task table must have a scrollable vertical range");
		Rectangle scrollBounds = new Rectangle();
		SwingUtilities.invokeAndWait(() -> {
			Rectangle bounds = tableScroll.getVerticalScrollBar().getBounds();
			java.awt.Point location = tableScroll.getVerticalScrollBar().getLocationOnScreen();
			scrollBounds.setBounds(location.x, location.y, bounds.width, bounds.height);
		});
		robot.mouseMove(scrollBounds.x + scrollBounds.width / 2, scrollBounds.y + scrollBounds.height - 8);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.delay(300);
		GuiAcceptanceSupport.await(() -> tableScroll.getVerticalScrollBar().getValue() > 0,
			"mouse wheel must scroll the 20-task table");
		assertTrue(tableScroll.getVerticalScrollBar().getValue() > 0, "table must remain scrollable with 20 tasks");
	}

	@Test
	void productionTaskColumnsReportReadableLayoutAtCommonWindowSizes() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(10);
		SpreadSheetFieldArray taskTemplate = SpreadsheetViewSupport.getTaskFields();
		String templateBeforeView = "defaultWidths=" + taskTemplate.getWidths()
			+ " defaultManualWidths=" + taskTemplate.getManualWidths();
		for (int index = 1; index <= 10; index++) {
			NormalTask task = (NormalTask) findTaskNode(fixture.project, "Sequential " + index).getImpl();
			task.setName(String.format(java.util.Locale.ROOT, "Task %02d", index));
		}
		DocumentFrame[] document = new DocumentFrame[1];
		SwingUtilities.invokeAndWait(() -> {
			var mainWindow = new com.microproject.pm.graphic.frames.MainRibbonFrame("Issue 432 task layout", null);
			frame = mainWindow;
			productionGraphicManager = new GraphicManager(mainWindow);
			mainWindow.setGraphicManager(productionGraphicManager);
			productionGraphicManager.initView();
			document[0] = productionGraphicManager.addProjectFrame(fixture.project);
			mainWindow.setSize(1300, 820);
			mainWindow.setLocation(20, 20);
			mainWindow.setAlwaysOnTop(true);
			mainWindow.setVisible(true);
			document[0].activateGanttView();
			taskLayoutView[0] = (GanttView) document[0].getActiveTopView();
		});
		GuiAcceptanceSupport.await(() -> frame.isShowing() && document[0].isShowing(),
			"production Gantt task view did not become visible");
		GanttView view = taskLayoutView[0];
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(35);
		Path directory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		StringBuilder evidence = new StringBuilder("Issue #432 production layout diagnostic\n")
			.append("taskTemplateBeforeView=").append(templateBeforeView).append('\n')
			.append("projectFieldArrayBeforeView=").append(fixture.project.getFieldArray()).append('\n');
		double[] previousGanttCenterDate = { Double.NaN };
		for (Dimension size : List.of(new Dimension(1300, 820), new Dimension(1009, 762), new Dimension(1300, 820))) {
			SwingUtilities.invokeAndWait(() -> frame.setSize(size));
			GuiAcceptanceSupport.await(() -> frame.getWidth() == size.width && frame.getHeight() == size.height,
				"window resize did not reach " + size.width + "x" + size.height);
			robot.delay(350);
			String layout = issue432LayoutSnapshot(view, fixture.project);
			evidence.append(size.width).append('x').append(size.height).append('\n').append(layout).append('\n');
			if (!Double.isNaN(previousGanttCenterDate[0])) {
				double visibleCenterX = view.getGantt().getVisibleRect().getCenterX();
				double preservedCenterX = view.coord.toX(previousGanttCenterDate[0]);
				assertTrue(Math.abs(visibleCenterX - preservedCenterX) <= 1.0d,
					"responsive resizing must keep the Gantt's visible center date; expectedX="
						+ preservedCenterX + " actualCenterX=" + visibleCenterX);
			}
			previousGanttCenterDate[0] = ganttVisibleCenterDate(view);
			robot.mouseMove(0, 0);
			robot.delay(100);
			captureVisibleLayout(robot, "issue-432-" + size.width + "x" + size.height + "-" + evidence.length() + ".png");
			Files.writeString(directory.resolve("issue-432-layout.txt"), evidence.toString());
			assertIssue432ReadableLayout(view, fixture.project);
		}
		int nameColumn = findColumn(view.getSpreadSheet(), "Field.name");
		int initialNameWidth = view.getSpreadSheet().getColumnModel().getColumn(nameColumn).getWidth();
		dragColumnBoundary(robot, view.getSpreadSheet(), nameColumn, -30);
		GuiAcceptanceSupport.await(() -> view.getSpreadSheet().getColumnModel().getColumn(nameColumn).getWidth() < initialNameWidth,
			"physical header drag did not reduce the task-name column");
		int manuallySavedNameWidth = view.getSpreadSheet().getColumnModel().getColumn(nameColumn).getWidth();
		assertTrue(manuallySavedNameWidth > 0, "the physical header resize must produce a usable width");
		assertTrue(manuallySavedNameWidth < 140, "the saved manual width must be narrower than the fresh name default");
		SpreadSheetColumnModel liveColumns = (SpreadSheetColumnModel) view.getSpreadSheet().getColumnModel();
		assertTrue(liveColumns.isWidthManuallyAdjusted("Field.name"),
			"a physical header drag must be recorded as a user-saved width");
		SpreadSheetFieldArray[] savedFields = new SpreadSheetFieldArray[1];
		SwingUtilities.invokeAndWait(() -> savedFields[0] = view.getSpreadSheet().getFieldArrayWithWidths(
			((SpreadSheetFieldArray) view.getSpreadSheet().getFieldArray()).clone()));
		SwingUtilities.invokeAndWait(() -> {
			fixture.project.setFieldArray(savedFields[0]);
			view.reinitialize();
		});
		GuiAcceptanceSupport.await(() -> view.getSpreadSheet().isShowing() && view.getGantt().isShowing(),
			"the production task view did not return after a saved-width rebuild");
		assertEquals(manuallySavedNameWidth,
			view.getSpreadSheet().getColumnModel().getColumn(findColumn(view.getSpreadSheet(), "Field.name")).getWidth(),
			"an explicitly saved manual width must survive the production view rebuild");
		assertTrue(((SpreadSheetColumnModel) view.getSpreadSheet().getColumnModel()).isWidthManuallyAdjusted("Field.name"),
			"the rebuilt column must retain its manual-width provenance");
		assertColumnHeadersAligned(view.getSpreadSheet());
		captureVisibleLayout(robot, "issue-432-manual-width-after-rebuild.png");
		SwingUtilities.invokeAndWait(() -> frame.setSize(1009, 762));
		GuiAcceptanceSupport.await(() -> frame.getWidth() == 1009 && frame.getHeight() == 762,
			"window resize after width restore did not settle");
		robot.delay(350);
		assertEquals(manuallySavedNameWidth,
			view.getSpreadSheet().getColumnModel().getColumn(findColumn(view.getSpreadSheet(), "Field.name")).getWidth(),
			"window resizing must not overwrite a manually saved width");
		assertTrue(view.getGantt().getWidth() > 200, "the Gantt must remain visible after resizing around a saved column width");
		assertColumnHeadersAligned(view.getSpreadSheet());
		captureVisibleLayout(robot, "issue-432-manual-width-after-resize.png");
		int dividerBeforeDrag = view.getDividerLocation();
		String dividerTarget = dragSplitDivider(robot, view, 35);
		assertTrue(dividerBeforeDrag != view.getDividerLocation(),
			"physical divider gesture did not move the split; target=" + dividerTarget
				+ " before=" + dividerBeforeDrag + " after=" + view.getDividerLocation());
		GuiAcceptanceSupport.await(view::isDividerLocationManuallyAdjusted,
			"physical divider drag must mark the chosen split as explicit; target=" + dividerTarget
				+ " before=" + dividerBeforeDrag + " after=" + view.getDividerLocation());
		int restoredDividerLocation = view.getDividerLocation() - 20;
		SwingUtilities.invokeAndWait(() -> view.restoreDividerLocation(restoredDividerLocation));
		assertEquals(restoredDividerLocation, view.getDividerLocation(),
			"restoring a saved divider location must apply the saved pixel position");
		SwingUtilities.invokeAndWait(() -> frame.setSize(1300, 820));
		GuiAcceptanceSupport.await(() -> frame.getWidth() == 1300 && frame.getHeight() == 820,
			"window resize after explicit divider drag did not settle");
		robot.delay(350);
		assertEquals(restoredDividerLocation, view.getDividerLocation(),
			"resizing must preserve the explicitly restored divider location");
		Files.writeString(directory.resolve("issue-432-layout.txt"), evidence.toString());
	}

	private final GanttView[] taskLayoutView = new GanttView[1];

	private static String issue432LayoutSnapshot(GanttView view, Project project) throws Exception {
		String[] snapshot = new String[1];
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = view.getSpreadSheet();
			var columns = sheet.getColumnModel();
			JScrollPane tableScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, sheet);
			JScrollPane ganttScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, view.getGantt());
			var columnModel = (com.microproject.pm.graphic.spreadsheet.SpreadSheetColumnModel) columns;
			StringBuilder details = new StringBuilder();
			SpreadSheetFieldArray viewFields = (SpreadSheetFieldArray) sheet.getFieldArray();
			SpreadSheetFieldArray defaults = SpreadsheetViewSupport.getTaskFields();
			details.append("projectFieldArray=").append(project.getFieldArray() == null ? "null" : project.getFieldArray().getWidths())
				.append(" viewFieldsAliasDefault=").append(viewFields == defaults)
				.append(" viewFieldWidths=").append(viewFields.getWidths())
				.append(" viewFieldManualWidths=").append(viewFields.getManualWidths())
				.append(" defaultWidths=").append(defaults.getWidths())
				.append(" defaultManualWidths=").append(defaults.getManualWidths()).append('\n');
			details.append("autoResizeMode=").append(sheet.getAutoResizeMode())
				.append(" sheet=").append(sheet.getWidth()).append('x').append(sheet.getHeight())
				.append(" tableViewport=").append(tableScroll.getViewport().getExtentSize())
				.append(" ganttViewport=").append(ganttScroll.getViewport().getExtentSize())
				.append(" ganttPosition=").append(ganttScroll.getViewport().getViewPosition())
				.append(" ganttCenterDate=").append(ganttVisibleCenterDate(view))
				.append(" split=").append(view.getWidth()).append('x').append(view.getHeight())
				.append(" divider=").append(view.getDividerLocation())
				.append(" coordOrigin=").append(view.coord.getOrigin())
				.append(" gantt=").append(view.getGantt().getWidth()).append('x').append(view.getGantt().getHeight())
				.append(" columns=").append(columns.getColumnCount()).append('\n');
			for (int index = 0; index < columns.getColumnCount(); index++) {
				var column = columns.getColumn(index);
				Field field = (Field) column.getIdentifier();
				details.append(field.getId()).append(" width=").append(column.getWidth())
					.append(" preferred=").append(column.getPreferredWidth())
					.append(" minimum=").append(column.getMinWidth())
					.append(" maximum=").append(column.getMaxWidth())
					.append(" savedManual=").append(columnModel.isWidthManuallyAdjusted(field.getId()))
					.append(" bodyX=").append(sheet.getCellRect(0, index, true).x)
					.append(" headerX=").append(sheet.getTableHeader().getHeaderRect(index).x).append('\n');
			}
			int nameColumn = findColumn(sheet, "Field.name");
			Component renderer = sheet.getCellRenderer(0, nameColumn).getTableCellRendererComponent(
				sheet, sheet.getValueAt(0, nameColumn), false, false, 0, nameColumn);
			details.append("taskNames=Task 01..10 nameCellWidth=").append(sheet.getColumnModel().getColumn(nameColumn).getWidth())
				.append(" rendererPreferred=").append(renderer.getPreferredSize().width).append('\n');
			int firstTaskRow = taskRow(sheet, project, "Task 01");
			GraphicNode firstTask = (GraphicNode) view.cache.getElementAt(firstTaskRow);
			Rectangle visibleGantt = view.gantt.getVisibleRect();
			details.append("firstTaskBarX=").append(view.coord.toX(firstTask.getStart())).append("..")
				.append(view.coord.toX(firstTask.getEnd())).append(" visibleX=").append(visibleGantt.x).append("..")
				.append(visibleGantt.x + visibleGantt.width).append(" visibleBars=")
				.append(visibleTaskBarCount(view, project)).append('\n');
			snapshot[0] = details.toString();
		});
		return snapshot[0];
	}

	private static void assertIssue432ReadableLayout(GanttView view, Project project) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = view.getSpreadSheet();
			SpreadSheetColumnModel columns = (SpreadSheetColumnModel) sheet.getColumnModel();
			assertTrue(view.getGantt().getWidth() > 200, "the Gantt view must remain visible beside the task table");
			JScrollPane ganttScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, view.getGantt());
			assertNotNull(ganttScroll, "the Gantt must remain hosted by its scroll pane");
			int minimumGanttWidth = view.getGantt().getFontMetrics(view.getGantt().getFont()).charWidth('M') * 16;
			assertTrue(ganttScroll.getViewport().getExtentSize().width >= minimumGanttWidth,
				"the responsive divider must preserve a font-scaled Gantt viewport; actual="
					+ ganttScroll.getViewport().getExtentSize().width + " minimum=" + minimumGanttWidth);
			assertTrue(visibleTaskBarCount(view, project) > 0,
				"at least one fixture task bar must remain visible in the Gantt after resizing; visible="
					+ view.getGantt().getVisibleRect() + " origin=" + view.coord.getOrigin());
			for (String fieldId : List.of("Field.name", "Field.duration", "Field.start", "Field.finish")) {
				int columnIndex = findColumn(sheet, fieldId);
				var column = columns.getColumn(columnIndex);
				int minimum = switch (fieldId) {
					case "Field.name" -> 140;
					case "Field.duration" -> 60;
					case "Field.start", "Field.finish" -> 100;
					default -> throw new IllegalArgumentException("Unexpected issue #432 field: " + fieldId);
				};
				assertTrue(column.getWidth() >= minimum,
					fieldId + " must meet its default readable width after layout; actual=" + column.getWidth()
						+ " preferred=" + column.getPreferredWidth());
				assertFalse(columns.isWidthManuallyAdjusted(fieldId),
					"initial layout must not mark " + fieldId + " as a user-saved manual width");
				assertEquals(sheet.getCellRect(0, columnIndex, true).x,
					sheet.getTableHeader().getHeaderRect(columnIndex).x,
					fieldId + " header and body must stay horizontally aligned");
				Component renderer = sheet.getCellRenderer(0, columnIndex).getTableCellRendererComponent(
					sheet, sheet.getValueAt(0, columnIndex), false, false, 0, columnIndex);
				assertTrue(sheet.getCellRect(0, columnIndex, true).width >= renderer.getPreferredSize().width,
					fieldId + " must show its first task value without clipping");
			}
			int nameColumn = findColumn(sheet, "Field.name");
			for (int index = 1; index <= 10; index++) {
				String taskName = String.format(java.util.Locale.ROOT, "Task %02d", index);
				int row = taskRow(sheet, project, taskName);
				Component renderer = sheet.getCellRenderer(row, nameColumn).getTableCellRendererComponent(
					sheet, sheet.getValueAt(row, nameColumn), false, false, row, nameColumn);
				assertEquals(taskName, renderer instanceof NameCellComponent nameCell ? nameCell.getText() : "",
					"the renderer must supply the full task name for " + taskName);
				assertTrue(sheet.getCellRect(row, nameColumn, true).width >= renderer.getPreferredSize().width,
					"the visible name cell must have room for its renderer content: " + taskName);
			}
			int finishColumn = findColumn(sheet, "Field.finish");
			Rectangle finishCell = sheet.getCellRect(0, finishColumn, true);
			int visibleRight = sheet.getVisibleRect().x + sheet.getVisibleRect().width;
			assertTrue(finishCell.x + finishCell.width <= visibleRight,
				"the Finish value must be visible without horizontal clipping; cellRight="
					+ (finishCell.x + finishCell.width) + " viewportRight=" + visibleRight
					+ " divider=" + view.getDividerLocation() + " manual=" + view.isDividerLocationManuallyAdjusted()
					+ " preferredLeft=" + view.preferredLeftPaneWidth() + " viewWidth=" + view.getWidth());
		});
	}

	private static int visibleTaskBarCount(GanttView view, Project project) {
		Rectangle visible = view.getGantt().getVisibleRect();
		int visibleCount = 0;
		for (int index = 1; index <= 10; index++) {
			int row = taskRow(view.getSpreadSheet(), project,
				String.format(java.util.Locale.ROOT, "Task %02d", index));
			GraphicNode task = (GraphicNode) view.cache.getElementAt(row);
			double startX = view.coord.toX(task.getStart());
			double endX = view.coord.toX(task.getEnd());
			double rowY = (double) row * view.getGantt().getRowHeight();
			if (endX >= visible.x && startX <= visible.x + visible.width
					&& rowY < visible.y + visible.height && rowY + view.getGantt().getRowHeight() > visible.y)
				visibleCount++;
		}
		return visibleCount;
	}

	private static double ganttVisibleCenterDate(GanttView view) {
		Rectangle visible = view.getGantt().getVisibleRect();
		return view.coord.toTime(visible.getCenterX());
	}

	private static void assertColumnHeadersAligned(SpreadSheet sheet) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (int index = 0; index < sheet.getColumnCount(); index++)
				assertEquals(sheet.getCellRect(0, index, true).x, sheet.getTableHeader().getHeaderRect(index).x,
					"body and header must remain aligned for view column " + index);
		});
	}

	private static void dragColumnBoundary(Robot robot, SpreadSheet sheet, int viewColumn, int delta) throws Exception {
		Point[] target = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			Rectangle header = sheet.getTableHeader().getHeaderRect(viewColumn);
			Point origin = sheet.getTableHeader().getLocationOnScreen();
			target[0] = new Point(origin.x + header.x + header.width - 1, origin.y + header.y + header.height / 2);
		});
		robot.mouseMove(target[0].x, target[0].y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseMove(target[0].x + delta, target[0].y);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.delay(250);
	}

	private static String dragSplitDivider(Robot robot, GanttView view, int delta) throws Exception {
		Point[] target = new Point[1];
		String[] componentName = new String[1];
		SwingUtilities.invokeAndWait(() -> {
			for (Component component : view.getComponents()) {
				if (component.getClass().getName().contains("Divider")) {
					Point origin = component.getLocationOnScreen();
					Rectangle bounds = component.getBounds();
					target[0] = new Point(origin.x + bounds.width / 2, origin.y + bounds.height / 2);
					componentName[0] = component.getClass().getName() + " bounds=" + bounds;
					return;
				}
			}
			throw new AssertionError("the Gantt split divider is not installed");
		});
		robot.mouseMove(target[0].x, target[0].y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseMove(target[0].x + delta, target[0].y);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.delay(250);
		return componentName[0] + " point=" + target[0];
	}

	/**
	 * Wheel gestures over the Gantt chart. Microsoft Project documents Ctrl+wheel as the Gantt zoom gesture
	 * (https://support.microsoft.com/en-us/project/zoom-in-or-out-of-a-gantt-chart); plain-wheel vertical scrolling
	 * and Shift+wheel horizontal timescale scrolling follow the standard Office scroll convention. Those two are
	 * document-derived compatibility decisions, not measured MSP results. {@code ScrollPaneSynchronizer} is the
	 * sole wheel owner. Whatever the gesture, the task model, selection and Undo history must stay unchanged.
	 */
	@Test
	void physicalGanttWheelModifiersHaveOneScrollOwnerAndOneStep() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(40);
		showFixture(fixture);
		SwingUtilities.invokeAndWait(() -> {
			frame.setSize(1200, 480);
			frame.setLocation(20, 20);
		});

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.gantt.isShowing(),
			"40-task table or Gantt was not visible");
		JScrollPane ganttScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, fixture.gantt);
		JScrollPane tableScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, fixture.sheet);
		assertTrue(ganttScroll != null, "Gantt must be hosted by its scroll pane");
		assertTrue(tableScroll != null, "task table must be hosted by its scroll pane");
		assertTrue(ganttScroll.getVerticalScrollBar().getMaximum() > ganttScroll.getVerticalScrollBar().getVisibleAmount(),
			"40-task Gantt must have a vertical scroll range");
		Synchronizer synchronizer = new Synchronizer();
		SwingUtilities.invokeAndWait(() -> synchronizer.addSynchro(ganttScroll, tableScroll,
			ScrollPaneSynchronizer.HORIZONTAL));

		try {
			int oneOwnerStep = fixture.gantt.getRowHeight() * 5;
			Rectangle visibleGantt = new Rectangle();
			SwingUtilities.invokeAndWait(() -> visibleGantt.setBounds(fixture.gantt.getVisibleRect()));
			Point point = screenCenter(fixture.gantt, visibleGantt);
			robot.mouseMove(point.x, point.y);
			robot.delay(150);

			SwingUtilities.invokeAndWait(() -> fixture.sheet.getSelectionModel().setSelectionInterval(2, 2));
			List<String> modelBefore = wheelModelSnapshot(fixture);
			List<Integer> selectionBefore = wheelSelectionSnapshot(fixture);
			assertEquals(List.of(2), selectionBefore, "fixture row 2 must be selected before the wheel gestures");
			UndoSnapshot undoBefore = UndoSnapshot.capture(fixture.project);
			int beforeX = ganttScroll.getHorizontalScrollBar().getValue();
			int beforeY = ganttScroll.getVerticalScrollBar().getValue();
			robot.mouseWheel(1);
			GuiAcceptanceSupport.await(() -> ganttScroll.getVerticalScrollBar().getValue() != beforeY,
				"plain physical Gantt wheel did not scroll vertically (before=" + beforeY + ", after="
					+ ganttScroll.getVerticalScrollBar().getValue() + ", point=" + point + ")");
			assertEquals(beforeY + oneOwnerStep, ganttScroll.getVerticalScrollBar().getValue(),
				"one plain wheel gesture must be applied by one vertical-scroll owner");
			assertEquals(beforeX, ganttScroll.getHorizontalScrollBar().getValue(),
				"plain wheel must not change horizontal position");
			assertEquals(ganttScroll.getVerticalScrollBar().getValue(), tableScroll.getVerticalScrollBar().getValue(),
				"plain wheel must keep the synchronized task table at the same vertical position");
			assertEquals(firstVisibleRow(tableScroll, fixture.gantt.getRowHeight()),
				firstVisibleRow(ganttScroll, fixture.gantt.getRowHeight()),
				"table and Gantt must show the same first visible task after the plain wheel");

			SwingUtilities.invokeAndWait(() -> {
				ganttScroll.getVerticalScrollBar().setValue(0);
				tableScroll.getVerticalScrollBar().setValue(0);
			});
			robot.delay(150);
			int horizontalBefore = ganttScroll.getHorizontalScrollBar().getValue();
			int verticalBefore = ganttScroll.getVerticalScrollBar().getValue();
			int horizontalStep = ganttScroll.getHorizontalScrollBar().getUnitIncrement(1);
			robot.keyPress(KeyEvent.VK_SHIFT);
			try {
				robot.mouseWheel(1);
			} finally {
				robot.keyRelease(KeyEvent.VK_SHIFT);
			}
			GuiAcceptanceSupport.await(() -> ganttScroll.getHorizontalScrollBar().getValue() != horizontalBefore,
				"Shift+wheel did not scroll horizontally");
			assertEquals(horizontalBefore + horizontalStep, ganttScroll.getHorizontalScrollBar().getValue(),
				"one Shift+wheel gesture must move one horizontal unit");
			assertEquals(verticalBefore, ganttScroll.getVerticalScrollBar().getValue(),
				"Shift+wheel must not change vertical position");
			int zoomBefore = fixture.gantt.getCoord().getTimescaleManager().getCurrentScaleIndex();
			int zoomCount = fixture.gantt.getCoord().getTimescaleManager().getScaleCount();
			int zoomDirection = zoomBefore == zoomCount - 1 ? -1 : 1;
			verticalBefore = ganttScroll.getVerticalScrollBar().getValue();
			robot.keyPress(KeyEvent.VK_CONTROL);
			try {
				robot.mouseWheel(zoomDirection);
			} finally {
				robot.keyRelease(KeyEvent.VK_CONTROL);
			}
			GuiAcceptanceSupport.await(() -> fixture.gantt.getCoord().getTimescaleManager().getCurrentScaleIndex() != zoomBefore,
				"Ctrl+wheel did not change Gantt zoom (index=" + zoomBefore + ", count=" + zoomCount + ")");
			assertEquals(zoomBefore + zoomDirection, fixture.gantt.getCoord().getTimescaleManager().getCurrentScaleIndex(),
				"one Ctrl+wheel gesture must apply exactly one zoom step");
			assertEquals(verticalBefore, ganttScroll.getVerticalScrollBar().getValue(),
				"Ctrl+wheel must not change vertical position");
			assertEquals(modelBefore, wheelModelSnapshot(fixture), "wheel gestures must not change the task model");
			assertEquals(selectionBefore, wheelSelectionSnapshot(fixture), "wheel gestures must not change the selection");
			assertEquals(undoBefore, UndoSnapshot.capture(fixture.project), "wheel gestures must not touch Undo history");
		} finally {
			SwingUtilities.invokeAndWait(() -> synchronizer.removeSynchro(ganttScroll, tableScroll,
				ScrollPaneSynchronizer.HORIZONTAL));
		}
	}

	@Test
	void physicalGanttPopupSelectsTheTaskAndOpensOnceOnThePlatformTrigger() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(3);
		showFixture(fixture);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		GraphicNode node = (GraphicNode) fixture.gantt.getModel().getCache().getElementAt(0);
		NormalTask task = (NormalTask) node.getNode().getImpl();
		BarGeometry bar = barGeometry(fixture.gantt, task);
		int x = bar.startX() + Math.max(1, bar.width() / 2);
		int row = fixture.gantt.getModel().getCache().getVisibleNodes().getProjectionIndex().rowForNode(node);
		int y = row * fixture.gantt.getRowHeight() + fixture.gantt.getRowHeight() / 2;
		Point point = screenCenter(fixture.gantt, new Rectangle(x, y, 1, 1));
		long startBefore = task.getStart();
		long durationBefore = task.getRawDuration();
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
		});
		robot.mouseMove(point.x, point.y);
		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
		GuiAcceptanceSupport.await(TaskTableGanttGridGuiAcceptanceTest::ganttPopupIsSelected,
			"a physical Gantt right-click did not open its popup on the platform popup trigger");
		SwingUtilities.invokeAndWait(() -> assertEquals(node,
			((GanttUI) fixture.gantt.getUI()).getInteractor().getSelectedObject(),
			"the task under the popup gesture must become the Gantt selection"));
		assertEquals(startBefore, task.getStart(), "opening the Gantt popup must not change task dates");
		assertEquals(durationBefore, task.getRawDuration(), "opening the Gantt popup must not change task duration");
		robot.keyPress(KeyEvent.VK_ESCAPE);
		robot.keyRelease(KeyEvent.VK_ESCAPE);
		GuiAcceptanceSupport.await(() -> !ganttPopupIsSelected(), "Escape did not close the Gantt popup");
	}

	@Test
	void physicalRowHeaderPopupSelectsTheClickedTaskAndOpensOnce() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(3);
		showFixture(fixture);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		int row = 1;
		GraphicNode targetNode = (GraphicNode) fixture.sheet.getCache().getElementAt(row);
		NormalTask targetTask = (NormalTask) targetNode.getNode().getImpl();
		SwingUtilities.invokeAndWait(() -> assertEquals(0, fixture.sheet.getSelectedRowCount(),
			"the row-header popup journey must start without a selected task"));
		SpreadSheetPopupMenu popup = fixture.sheet.getPopup();
		int[] popupOpenCount = { 0 };
		popup.addPopupMenuListener(new PopupMenuListener() {
			@Override public void popupMenuWillBecomeVisible(PopupMenuEvent event) { popupOpenCount[0]++; }
			@Override public void popupMenuWillBecomeInvisible(PopupMenuEvent event) { }
			@Override public void popupMenuCanceled(PopupMenuEvent event) { }
		});
		SpreadSheetRowHeader rowHeader = fixture.sheet.getRowHeader();
		Point target = screenCenter(rowHeader, rowHeader.getCellRect(row, 0, true));
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> frame.isActive() && rowHeader.isShowing(),
			"task table or row header was not ready for a physical popup operation");
		robot.mouseMove(target.x, target.y);
		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> popup.isVisible(), "physical row-header right-click did not open the task popup");
		SwingUtilities.invokeAndWait(() -> {
			assertSame(rowHeader, popup.getInvoker(), "row-header popup must retain its physical invoker");
			assertEquals(row, popup.getRow(), "popup context must resolve the clicked row");
			assertEquals(0, popup.getCol(), "row-header popup must use the row identity column");
			assertEquals(1, popupOpenCount[0], "one physical right-click must open the popup once");
			assertEquals(1, fixture.sheet.getSelectedGraphicNodes().size(),
				"right-clicking an unselected task must select exactly that task");
			assertTrue(fixture.sheet.getSelectedGraphicNodes().contains(targetNode),
				"selection must retain the clicked task's stable identity");
			assertEquals(targetTask.getUniqueId(), ((NormalTask) fixture.sheet.getSelectedGraphicNodes().iterator().next()
				.getNode().getImpl()).getUniqueId(), "selected task identity must match the task under the popup");
			assertTrue(fixture.sheet.isRowFullySelected(row), "row-header popup selection must cover the complete task row");
		});
	}

	private static boolean ganttPopupIsSelected() {
		MenuElement[][] path = new MenuElement[1][];
		try {
			SwingUtilities.invokeAndWait(() -> path[0] = MenuSelectionManager.defaultManager().getSelectedPath());
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to inspect the active popup path", exception);
		}
		for (MenuElement element : path[0]) {
			if (element instanceof JPopupMenu) return true;
		}
		return false;
	}

	@Test
	void emptyTaskBetweenTasksKeepsGanttRowsSeparated() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixtureWithEmptyMiddle();
		showFixture(fixture);

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.gantt.isShowing(), "task table or Gantt was not visible");
		robot.delay(500);
		captureVisibleLayout(robot, "task-table-gantt-grid-empty-middle.png");
	}

	@Test
	void physicalCellAndColumnHeaderClicksKeepSelectionHighlightCoordinatesAligned() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(3);
		showFixture(fixture);

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			selectionController = new TaskSelectionController(fixture.gantt, fixture.sheet);
			frame.toFront();
			frame.requestFocus();
			fixture.sheet.requestFocusInWindow();
		});
		GuiAcceptanceSupport.await(() -> frame.isActive(), "task table selection test frame did not become active");
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.sheet.getRowCount() >= 3,
			"task table was not ready for physical selection verification");
		robot.delay(300);

		// Microsoft documents Shift selection for adjacent tasks. The original
		// anchor must survive an upward row-header extension followed by a cell
		// extension; preserving it across these routes is a document-derived
		// Office-consistent decision because the documentation does not specify
		// cross-control anchor behavior. Source: https://support.microsoft.com/en-au/project/link-tasks-in-a-project
		SpreadSheetRowHeader rowHeader = fixture.sheet.getRowHeader();
		AtomicReference<UndoSnapshot> undoBeforeSelection = new AtomicReference<>();
		AtomicReference<List<TaskSnapshot>> tasksBeforeSelection = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			undoBeforeSelection.set(UndoSnapshot.capture(fixture.project));
			tasksBeforeSelection.set(TaskSnapshot.capture(fixture.project));
		});
		Set<Long> expectedTaskIds = Set.of(tasksBeforeSelection.get().get(1).uniqueId(),
			tasksBeforeSelection.get().get(2).uniqueId());
		Point middleHeaderPoint = screenCenter(rowHeader, rowHeader.getCellRect(1, 0, true));
		robot.mouseMove(middleHeaderPoint.x, middleHeaderPoint.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.delay(100);
		robot.keyPress(java.awt.event.KeyEvent.VK_SHIFT);
		try {
			Point firstHeaderPoint = screenCenter(rowHeader, rowHeader.getCellRect(0, 0, true));
			robot.mouseMove(firstHeaderPoint.x, firstHeaderPoint.y);
			robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
			robot.delay(100);
			SwingUtilities.invokeAndWait(() -> {
				assertEquals(Set.of(0, 1), java.util.Arrays.stream(fixture.sheet.getSelectedRows()).boxed()
					.collect(java.util.stream.Collectors.toSet()),
					"upward row-header Shift click must select the contiguous range from the middle-row anchor");
				assertEquals(Set.of(0, 1), fixture.gantt.getHighlightedRows(),
					"the Gantt must project the upward row-header range");
			});
			Point thirdCellPoint = screenCenter(fixture.sheet,
				fixture.sheet.getCellRect(2, Math.min(1, fixture.sheet.getColumnCount() - 1), true));
			robot.mouseMove(thirdCellPoint.x, thirdCellPoint.y);
			robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
			robot.delay(150);
			SwingUtilities.invokeAndWait(() -> {
				assertEquals(Set.of(1, 2), java.util.Arrays.stream(fixture.sheet.getSelectedRows()).boxed()
					.collect(java.util.stream.Collectors.toSet()),
					"cell Shift click must extend from the original middle-row anchor after the upward header range");
				assertEquals(expectedTaskIds,
					fixture.sheet.getSelectedGraphicNodes().stream()
						.map(node -> ((NormalTask) node.getNode().getImpl()).getUniqueId())
						.collect(java.util.stream.Collectors.toSet()),
					"cross-route selection must preserve the expected task identities");
				assertEquals(2, fixture.sheet.getSelection().getActiveRow(), "the Shift-clicked cell row must remain active");
				assertTrue(fixture.sheet.getSelection().isActiveCell(2, Math.min(1, fixture.sheet.getColumnCount() - 1)),
					"the Shift-clicked cell must remain active within the row range");
				assertEquals(Set.of(1, 2), fixture.gantt.getHighlightedRows(),
					"the Gantt must project the same task rows as the cross-route selection");
				assertEquals(tasksBeforeSelection.get(), TaskSnapshot.capture(fixture.project),
					"selection gestures must preserve task IDs, names, start, duration, and progress");
				assertEquals(undoBeforeSelection.get(), UndoSnapshot.capture(fixture.project),
					"selection gestures must preserve the Undo/Redo edit list and available state");
			});
		} finally {
			robot.keyRelease(java.awt.event.KeyEvent.VK_SHIFT);
		}

		final int row = 1;
		final int column = Math.min(1, fixture.sheet.getColumnCount() - 1);
		Point cellPoint = screenCenter(fixture.sheet, fixture.sheet.getCellRect(row, column, true));
		robot.mouseMove(cellPoint.x, cellPoint.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.delay(150);
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(row, fixture.sheet.getSelectedRow(), "physical cell click selected a different row");
			assertEquals(column, fixture.sheet.getSelection().getActiveColumn(),
				"active cell column must match the physically clicked column");
			assertTrue(fixture.sheet.getSelection().isActiveCell(row, column),
				"active cell must match the physically clicked cell");
			assertTrue(fixture.sheet.isRowFullySelected(row), "physical task-cell click must select the full task row");
			assertFalse(fixture.sheet.isHeaderColumnSelectionActive(),
				"a task-cell click must not be rendered as a column-header selection");
			assertHeaderHighlight(fixture.sheet, column);
			assertEquals(Set.of(row), fixture.gantt.getHighlightedRows(),
				"a task-cell selection must project only its task row to the Gantt");
		});
		GraphicNode[] retainedTask = new GraphicNode[1];
		int[] retainedRow = new int[1];
		SwingUtilities.invokeAndWait(() -> {
			retainedTask[0] = fixture.sheet.getSelectedGraphicNodes().getFirst();
			GraphicNode deletedEarlierTask = (GraphicNode) fixture.sheet.getCache().getElementAt(0);
			fixture.sheet.getCache().deleteNodes(List.of(deletedEarlierTask.getNode()));
		});
		SwingUtilities.invokeAndWait(() -> {
			assertTrue(fixture.sheet.getSelectedGraphicNodes().contains(retainedTask[0]),
				"deleting an earlier projected row must preserve the selected task identity");
			assertEquals(0, fixture.sheet.getSelectedRow(),
				"the retained task must be selected at its new table row after deletion");
			retainedRow[0] = fixture.sheet.getSelectedRow();
			assertEquals(Set.of(retainedRow[0]), fixture.gantt.getHighlightedRows(),
				"the Gantt highlight must follow the retained identity to its new row");
		});

		Point headerPoint = screenCenter(fixture.sheet.getTableHeader(), fixture.sheet.getTableHeader().getHeaderRect(column));
		robot.mouseMove(headerPoint.x, headerPoint.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.delay(150);
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(column, fixture.sheet.getSelectedColumn(),
				"physical column-header click selected a different column");
			assertTrue(fixture.sheet.isColumnFullySelected(column),
				"physical column-header click must select the complete column");
			assertTrue(fixture.sheet.isHeaderColumnSelectionActive(),
				"column-header selection must use the column-header rendering state");
			assertFalse(fixture.sheet.getSelection().isActiveCell(retainedRow[0], column),
				"a full column selection must not retain a misleading active-cell highlight");
			assertEquals(Set.of(retainedRow[0]), fixture.gantt.getHighlightedRows(),
				"a presentation-only column selection must not select every Gantt task row");
		});
		assertActiveHeaderOutlineClosedOnScreen(robot, fixture.sheet, column);

		int cellRow = 0;
		Point cellAfterHeader = screenCenter(fixture.sheet, fixture.sheet.getCellRect(cellRow, column, true));
		robot.mouseMove(cellAfterHeader.x, cellAfterHeader.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.keyPress(java.awt.event.KeyEvent.VK_RIGHT);
		robot.keyRelease(java.awt.event.KeyEvent.VK_RIGHT);
		GuiAcceptanceSupport.await(() -> fixture.sheet.getSelection().isActiveCell(cellRow, column + 1),
			"after a column selection, a cell click and Right Arrow must move to its adjacent visible cell");
		SwingUtilities.invokeAndWait(() -> {
			assertFalse(fixture.sheet.isHeaderColumnSelectionActive(),
				"a task-cell click must end column presentation selection before keyboard navigation");
			assertTrue(fixture.sheet.getSelection().isActiveCell(cellRow, column + 1),
				"Right Arrow must retain the clicked row and advance exactly one visible column");
		});

		SwingUtilities.invokeAndWait(() ->
			fixture.sheet.getCache().deleteNodes(List.of(retainedTask[0].getNode())));
		GuiAcceptanceSupport.await(() -> fixture.sheet.getCache().getVisibleNodes().getProjectionIndex()
			.rowForNode(retainedTask[0]) < 0, "deleted selected task must leave the visible projection");
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(0, fixture.sheet.getSelectedRowCount(),
				"deleting the selected task must clear its table selection");
			assertTrue(fixture.gantt.getHighlightedRows().isEmpty(),
				"a deleted selection key must be pruned from the Gantt and not reappear implicitly");
		});
	}

	@Test
	void workspaceV2RestoresTaskSelectionAndScrollAnchorAfterEarlierTaskDeletion() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(30);
		showFixture(fixture);

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		JScrollPane tableScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, fixture.sheet);
		assertTrue(tableScroll != null, "task table must be hosted by a scroll pane");
		SwingUtilities.invokeAndWait(() -> {
			selectionController = new TaskSelectionController(fixture.gantt, fixture.sheet);
			frame.setSize(1100, 500);
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> frame.isActive() && fixture.sheet.isShowing(),
			"workspace acceptance frame or task table was not ready");
		robot.delay(250);

		int selectedRow = 15;
		int anchorRow = 10;
		GraphicNode[] selectedTask = new GraphicNode[1];
		GraphicNode[] scrollAnchor = new GraphicNode[1];
		com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet.Workspace[] saved =
			new com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet.Workspace[1];
		SwingUtilities.invokeAndWait(() -> {
			JViewport viewport = tableScroll.getViewport();
			viewport.setViewPosition(new Point(0, fixture.sheet.getCellRect(anchorRow, 0, true).y + 5));
			selectedTask[0] = (GraphicNode) fixture.sheet.getCache().getElementAt(selectedRow);
		});
		Point selectedCell = screenCenter(fixture.sheet, fixture.sheet.getCellRect(selectedRow, 1, true));
		robot.mouseMove(selectedCell.x, selectedCell.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.delay(120);
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(selectedRow, fixture.sheet.getSelectedRow(), "physical row selection must choose the target task");
			JViewport viewport = tableScroll.getViewport();
			int topRow = fixture.sheet.rowAtPoint(new Point(0, viewport.getViewPosition().y));
			assertEquals(anchorRow, topRow, "the chosen task must remain below the saved scroll anchor");
			scrollAnchor[0] = (GraphicNode) fixture.sheet.getCache().getElementAt(topRow);
			saved[0] = (com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet.Workspace)
				fixture.sheet.createWorkspace(SavableToWorkspace.VIEW);
			assertTrue(saved[0].isStableTaskSelectionPresent(), "V2 workspace must contain durable selected task keys");
			assertTrue(saved[0].isStableScrollAnchorPresent(), "V2 workspace must contain the visible task scroll anchor");
		});
		WorkspaceSetting serializedWorkspace = roundTripWorkspace(saved[0]);

		SwingUtilities.invokeAndWait(() -> {
			GraphicNode earlierTask = (GraphicNode) fixture.sheet.getCache().getElementAt(0);
			fixture.sheet.getCache().deleteNodes(List.of(earlierTask.getNode()));
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.getCache().getVisibleNodes().getProjectionIndex()
			.rowForNode(selectedTask[0]) == selectedRow - 1, "deleting the first task must shift the selected task projection");
		SwingUtilities.invokeAndWait(() -> fixture.sheet.restoreWorkspace(serializedWorkspace, SavableToWorkspace.VIEW));
		SwingUtilities.invokeAndWait(() -> {
			assertTrue(fixture.sheet.getSelectedGraphicNodes().contains(selectedTask[0]),
				"V2 restore must retain task identity after an earlier row is removed");
			assertEquals(selectedRow - 1, fixture.sheet.getSelectedRow(),
				"V2 selection must resolve to the task's new table row");
			assertEquals(Set.of(selectedRow - 1), fixture.gantt.getHighlightedRows(),
				"the Gantt highlight must follow the restored stable selection");
			JViewport viewport = tableScroll.getViewport();
			int restoredTopRow = fixture.sheet.rowAtPoint(new Point(0, viewport.getViewPosition().y));
			assertTrue(restoredTopRow >= 0, "restored viewport must begin on a visible row");
			assertSame(scrollAnchor[0].getNode(), ((com.microproject.pm.graphic.spreadsheet.SpreadSheetModel)
				fixture.sheet.getModel()).getNodeForDisplayRow(restoredTopRow),
				"V2 scroll restore must keep the same task at the top anchor after row changes");
		});
		robot.delay(150);
		captureVisibleLayout(robot, "workspace-v2-restored-selection-anchor.png");

		SwingUtilities.invokeAndWait(() -> fixture.sheet.getCache().deleteNodes(List.of(selectedTask[0].getNode())));
		GuiAcceptanceSupport.await(() -> fixture.sheet.getCache().getVisibleNodes().getProjectionIndex()
			.rowForNode(selectedTask[0]) < 0, "deleted V2 selection key must leave the projection");
		SwingUtilities.invokeAndWait(() -> {
			fixture.sheet.restoreWorkspace(serializedWorkspace, SavableToWorkspace.VIEW);
			assertEquals(0, fixture.sheet.getSelectedRowCount(),
				"a valid V2 key that no longer exists must be pruned instead of resolving by its old row number");
			assertTrue(fixture.gantt.getHighlightedRows().isEmpty(),
				"a missing V2 selection key must not highlight a different task");
		});

		com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet.Workspace legacy =
			new com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet.Workspace();
		legacy.setWorkspaceVersion(1);
		legacy.setSelectedRows(new int[] { 2 });
		legacy.setViewPosition(new Point(0, fixture.sheet.getCellRect(1, 0, true).y));
		SwingUtilities.invokeAndWait(() -> {
			fixture.sheet.clearSelection();
			fixture.sheet.restoreWorkspace(legacy, SavableToWorkspace.VIEW);
			assertEquals(2, fixture.sheet.getSelectedRow(), "V1 must continue restoring its legacy row selection");
			assertEquals(legacy.getViewPosition(), tableScroll.getViewport().getViewPosition(),
				"V1 must continue restoring its legacy pixel scroll position");
		});

		com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet.Workspace malformed =
			new com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet.Workspace();
		malformed.setWorkspaceVersion(2);
		malformed.setStableTaskSelectionPresent(true);
		malformed.setSelectedProjectIds(new long[] { 1L, 1L });
		malformed.setSelectedTaskIds(new long[] { 2L });
		malformed.setSelectedOccurrences(new int[] { 0, 0 });
		malformed.setSelectedRows(new int[] { 3 });
		malformed.setViewPosition(new Point(0, fixture.sheet.getCellRect(1, 0, true).y));
		SwingUtilities.invokeAndWait(() -> {
			fixture.sheet.clearSelection();
			fixture.sheet.restoreWorkspace(malformed, SavableToWorkspace.VIEW);
			assertEquals(3, fixture.sheet.getSelectedRow(), "malformed V2 selection must fall back to its V1 row array");
			assertEquals(malformed.getViewPosition(), tableScroll.getViewport().getViewPosition(),
				"malformed V2 anchor must fall back to the V1 pixel position");
		});
	}

	private static WorkspaceSetting roundTripWorkspace(WorkspaceSetting workspace) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
			output.writeObject(workspace);
		}
		try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
			return (WorkspaceSetting) input.readObject();
		}
	}

	@Test
	void physicalHeaderRightClickHidesTheChosenColumnThroughTheColumnPopup() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(3);
		showFixture(fixture, true);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> hasVisibleColumns(fixture.sheet, 1),
			"task table was not ready for a header context-menu operation");

		int column = 1;
		int[] beforeFieldsValue = new int[1];
		String[] hiddenFieldIdValue = new String[1];
		SwingUtilities.invokeAndWait(() -> {
			beforeFieldsValue[0] = fixture.sheet.getFieldArray().size();
			hiddenFieldIdValue[0] = fixture.sheet.getFieldArray().get(column + 1).getId();
		});
		int beforeFields = beforeFieldsValue[0];
		// HeaderMouseListener passes the visible header index plus one because the
		// persisted field array reserves index zero for the hidden task ID field.
		String hiddenFieldId = hiddenFieldIdValue[0];
		Point headerPoint = screenCenter(fixture.sheet.getTableHeader(),
			fixture.sheet.getTableHeader().getHeaderRect(column));
		robot.mouseMove(headerPoint.x, headerPoint.y);
		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
		final SpreadSheetColumnMenu[] popup = new SpreadSheetColumnMenu[1];
		GuiAcceptanceSupport.await(() -> {
			for (MenuElement element : MenuSelectionManager.defaultManager().getSelectedPath()) {
				if (element instanceof SpreadSheetColumnMenu menu) {
					popup[0] = menu;
					return true;
				}
			}
			return false;
		}, "physical header right-click did not open the column popup");
		JMenuItem hide = (JMenuItem) popup[0].getComponent(1);
		Point hideLocation = new Point();
		SwingUtilities.invokeAndWait(() -> hideLocation.setLocation(hide.getLocationOnScreen()));
		robot.mouseMove(hideLocation.x + hide.getWidth() / 2, hideLocation.y + hide.getHeight() / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> fieldLayoutMatches(fixture.sheet, beforeFields - 1, hiddenFieldId, false),
			"Hide Column from the physical popup did not update the persistent field layout");
		assertTrue(fieldLayoutMatches(fixture.sheet, beforeFields - 1, hiddenFieldId, false),
			"the visible table must remove exactly the header column chosen from its popup");
		SwingUtilities.invokeAndWait(() -> fixture.sheet.requestFocusInWindow());
		GuiAcceptanceSupport.await(fixture.sheet::isFocusOwner, "task table did not regain focus before physical Undo");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_Z);
		robot.keyRelease(KeyEvent.VK_Z);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> fieldLayoutMatches(fixture.sheet, beforeFields, hiddenFieldId, true),
			"physical Ctrl+Z did not restore the hidden column");
		assertTrue(fieldLayoutMatches(fixture.sheet, beforeFields, hiddenFieldId, true),
			"Undo must restore the selected header in the persisted field layout");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> fieldLayoutMatches(fixture.sheet, beforeFields - 1, hiddenFieldId, false),
			"physical Ctrl+Y did not reapply the hidden column");
		assertTrue(fieldLayoutMatches(fixture.sheet, beforeFields - 1, hiddenFieldId, false),
			"Redo must remove the selected header from the persisted field layout again");

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(fixture.project, saved),
			"MPO save rejected the physical Hide Column layout");
		Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertFalse(reloaded.getFieldArray().stream().anyMatch(field -> hiddenFieldId.equals(field.getId())),
			"MPO reload restored a column that was physically hidden");
	}

	@Test
	void physicalHeaderInsertColumnUsesColumnDialogAndRoundTripsTheProjectLayout() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(3);
		showFixture(fixture);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.sheet.getColumnCount() > 1,
			"task table was not ready for a physical Insert Column operation");

		int beforeFields = fixture.sheet.getFieldArray().size();

		int column = 1;
		Point headerPoint = screenCenter(fixture.sheet.getTableHeader(),
			fixture.sheet.getTableHeader().getHeaderRect(column));
		robot.mouseMove(headerPoint.x, headerPoint.y);
		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
		final SpreadSheetColumnMenu[] popup = new SpreadSheetColumnMenu[1];
		GuiAcceptanceSupport.await(() -> {
			for (MenuElement element : MenuSelectionManager.defaultManager().getSelectedPath()) {
				if (element instanceof SpreadSheetColumnMenu menu) {
					popup[0] = menu;
					return true;
				}
			}
			return false;
		}, "physical header right-click did not open the column popup for Insert Column");

		JMenuItem insert = (JMenuItem) popup[0].getComponent(0);
		clickComponent(robot, insert);
		Window dialog = awaitWindowWith(JComboBox.class, "ColumnDialog did not open from the physical column popup");
		JComboBox<?> combo = findComponent(dialog, JComboBox.class);
		assertTrue(combo != null && combo.isShowing(), "ColumnDialog must expose a visible field selector");
		clickComponent(robot, combo);
		robot.keyPress(KeyEvent.VK_HOME);
		robot.keyRelease(KeyEvent.VK_HOME);
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		Field[] selected = new Field[1];
		SwingUtilities.invokeAndWait(() -> selected[0] = (Field) combo.getSelectedItem());
		assertTrue(selected[0] != null, "ColumnDialog physical selection must produce a field");
		JButton ok = findButton(dialog, "OK", "確認", "適用");
		assertTrue(ok != null && ok.isShowing() && ok.isEnabled(), "ColumnDialog must expose an enabled OK button");
		clickComponent(robot, ok);

		GuiAcceptanceSupport.await(() -> fixture.sheet.getFieldArray().size() == beforeFields + 1,
			"physical ColumnDialog confirmation did not update the task field layout");
		SwingUtilities.invokeAndWait(() -> assertTrue(fixture.sheet.getFieldArray().stream()
			.anyMatch(field -> selected[0].getId().equals(field.getId())),
			"the field selected through ColumnDialog was not inserted into the visible layout"));

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(fixture.project, saved),
			"MPO save rejected the project after physical Insert Column");
		Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertTrue(reloaded.getFieldArray().stream().anyMatch(field -> selected[0].getId().equals(field.getId())),
			"MPO reload lost the column layout created through the physical popup");
	}

	@Test
	void physicalColumnAutoFilterUsesDisplayedValuesAndUpdatesTheVisibleRows() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(3);
		showFixture(fixture);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.sheet.getRowCount() >= 3,
			"task table was not ready for a physical AutoFilter operation");
		int beforeRows = fixture.sheet.getRowCount();

		int nameFieldIndex = -1;
		for (int index = 0; index < fixture.sheet.getFieldArray().size(); index++) {
			if ("Field.name".equals(fixture.sheet.getFieldArray().get(index).getId())) {
				nameFieldIndex = index;
				break;
			}
		}
		assertTrue(nameFieldIndex > 0, "fixture must expose the task Name field after its hidden ID field");
		int nameColumn = nameFieldIndex - 1;
		Point headerPoint = screenCenter(fixture.sheet.getTableHeader(), fixture.sheet.getTableHeader().getHeaderRect(nameColumn));
		robot.mouseMove(headerPoint.x, headerPoint.y);
		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);

		final SpreadSheetColumnMenu[] popup = new SpreadSheetColumnMenu[1];
		GuiAcceptanceSupport.await(() -> {
			for (MenuElement element : MenuSelectionManager.defaultManager().getSelectedPath()) {
				if (element instanceof SpreadSheetColumnMenu menu) {
					popup[0] = menu;
					return true;
				}
			}
			return false;
		}, "physical header right-click did not open the column popup for AutoFilter");

		// AutoFilter is the final command for both localized and English menus.
		Component lastMenuComponent = popup[0].getComponent(popup[0].getComponentCount() - 1);
		assertTrue(lastMenuComponent instanceof JMenuItem, "column popup must expose its final AutoFilter command");
		JMenuItem autoFilter = (JMenuItem) lastMenuComponent;
		clickComponent(robot, autoFilter);
		Window dialog = awaitWindowWith(JCheckBox.class, "AutoFilter dialog did not open from the physical column popup");
		JCheckBox value = findCheckBox(dialog, "Sequential 1");
		assertTrue(value != null && value.isSelected(), "AutoFilter dialog must expose the selected Sequential 1 task value");
		clickComponent(robot, value);
		JButton apply = findButton(dialog, "Apply", "適用");
		assertTrue(apply != null && apply.isShowing() && apply.isEnabled(), "AutoFilter dialog must expose an enabled Apply button");
		clickComponent(robot, apply);

		GuiAcceptanceSupport.await(() -> fixture.sheet.getRowCount() == beforeRows - 1,
			"applying a physical AutoFilter selection did not remove exactly one visible task row");
		assertEquals(beforeRows - 1, fixture.gantt.getModel().getCache().getSize(),
			"AutoFilter must apply through the shared task-table/Gantt visible cache");
	}

	@Test
	void physicalCustomColumnRenameUsesTheHeaderPopupDialog() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(3);
		Field customField = fixture.sheet.getAvailableFields().stream().filter(Field::isCustom).findFirst().orElseThrow(
			() -> new AssertionError("task field catalog must expose a custom field for Rename"));
		String originalAlias = customField.getAlias();
		SwingUtilities.invokeAndWait(() -> fixture.sheet.setFieldArray(
			((SpreadSheetFieldArray) fixture.sheet.getFieldArray()).insertField(fixture.sheet.getFieldArray().size(), customField)));
		showFixture(fixture);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.sheet.getColumnCount() > 1,
			"task table was not ready for a physical custom-column Rename operation");

		int fieldIndex = fixture.sheet.getFieldArray().indexOf(customField);
		assertTrue(fieldIndex > 0, "custom field must be visible after the hidden ID field");
		StringBuilder layout = new StringBuilder("fieldArray=");
		SwingUtilities.invokeAndWait(() -> {
			for (Object field : fixture.sheet.getFieldArray())
				layout.append(((Field) field).getId().replace("Field.", "")).append(' ');
			layout.append("columns=");
			for (int index = 0; index < fixture.sheet.getColumnModel().getColumnCount(); index++)
				layout.append(fixture.sheet.getColumnModel().getColumn(index).getIdentifier() instanceof Field field
					? field.getId().replace("Field.", "") : "?").append(' ');
		});
		assertTrue(fieldIndex - 1 < fixture.sheet.getColumnCount(),
			"the field array and the visible columns disagree: " + layout);
		int customColumn = fieldIndex - 1;
		SwingUtilities.invokeAndWait(() -> fixture.sheet.scrollRectToVisible(fixture.sheet.getCellRect(0, customColumn, true)));
		Point headerPoint = screenCenter(fixture.sheet.getTableHeader(), fixture.sheet.getTableHeader().getHeaderRect(customColumn));
		robot.mouseMove(headerPoint.x, headerPoint.y);
		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);

		final SpreadSheetColumnMenu[] popup = new SpreadSheetColumnMenu[1];
		GuiAcceptanceSupport.await(() -> {
			for (MenuElement element : MenuSelectionManager.defaultManager().getSelectedPath()) {
				if (element instanceof SpreadSheetColumnMenu menu) {
					popup[0] = menu;
					return true;
				}
			}
			return false;
		}, "physical header right-click did not open the custom-column popup");

		JMenuItem rename = menuItemWithText(popup[0], com.microproject.strings.Messages.getString("RenameDialog.Rename"));
		clickComponent(robot, rename);
		Window dialog = awaitWindowWith(JTextField.class, "Rename dialog did not open from the physical custom-column popup");
		JTextField input = findComponent(dialog, JTextField.class);
		assertTrue(input != null && input.isShowing(), "Rename dialog must expose its visible alias input");
		clickComponent(robot, input);
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		for (char character : "gui alias".toCharArray()) {
			int keyCode = KeyEvent.getExtendedKeyCodeForChar(character);
			robot.keyPress(keyCode);
			robot.keyRelease(keyCode);
		}
		JButton ok = findButton(dialog, "OK", "確認");
		assertTrue(ok != null && ok.isShowing() && ok.isEnabled(), "Rename dialog must expose an enabled OK button");
		clickComponent(robot, ok);
		GuiAcceptanceSupport.await(() -> "gui alias".equals(customField.getName()),
			"physical Rename did not update the custom column alias");
		StringBuilder after = new StringBuilder("fieldArray=");
		SwingUtilities.invokeAndWait(() -> {
			for (Object field : fixture.sheet.getFieldArray())
				after.append(((Field) field).getId().replace("Field.", "")).append(' ');
			after.append("columns=");
			for (int index = 0; index < fixture.sheet.getColumnModel().getColumnCount(); index++)
				after.append(fixture.sheet.getColumnModel().getColumn(index).getIdentifier() instanceof Field field
					? field.getId().replace("Field.", "") : "?").append(' ');
			after.append("undo=").append(fixture.project.getUndoController().getEditNames());
		});
		assertTrue(customColumn < fixture.sheet.getColumnCount(), "the custom column vanished after Rename. before: "
			+ layout + " after: " + after);
		assertEquals("gui alias", fixture.sheet.getColumnName(customColumn),
			"spreadsheet header did not redraw the alias entered through Rename");
		// Fields are dictionary singletons. Restore the fixture-independent alias
		// so this physical route does not leak state into the remaining GUI suite.
		SwingUtilities.invokeAndWait(() -> customField.setAlias(originalAlias));
	}

	@Test
	void physicalCornerPresetChangesTaskColumnsAndRoundTripsTheProjectLayout() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(3);
		showFixture(fixture);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.sheet.getRowCount() >= 3,
			"task table was not ready for a physical column preset operation");

		JScrollPane tableScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, fixture.sheet);
		assertTrue(tableScroll != null, "task table must be hosted by a scroll pane");
		Component corner = tableScroll.getCorner(JScrollPane.UPPER_LEFT_CORNER);
		assertTrue(corner != null && corner.isShowing(), "task table corner must expose the column preset route");
		String beforeName = fixture.sheet.getFieldArray().toString();
		Point cornerPoint = screenCenter((JComponent) corner, new Rectangle(0, 0, corner.getWidth(), corner.getHeight()));
		robot.mouseMove(cornerPoint.x, cornerPoint.y);
		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);

		final SpreadSheetColumnsPopupMenu[] popup = new SpreadSheetColumnsPopupMenu[1];
		GuiAcceptanceSupport.await(() -> {
			for (MenuElement element : MenuSelectionManager.defaultManager().getSelectedPath()) {
				if (element instanceof SpreadSheetColumnsPopupMenu menu) {
					popup[0] = menu;
					return true;
				}
			}
			return false;
		}, "physical corner right-click did not open the column preset popup");

		JRadioButtonMenuItem preset = null;
		for (int index = 0; index < popup[0].getComponentCount(); index++) {
			Component component = popup[0].getComponent(index);
			if (component instanceof JRadioButtonMenuItem item && !item.isSelected()) {
				preset = item;
				break;
			}
		}
		assertTrue(preset != null, "column preset popup must expose an alternative preset");
		String selectedPreset = preset.getText();
		clickComponent(robot, preset);
		GuiAcceptanceSupport.await(() -> !beforeName.equals(fixture.sheet.getFieldArray().toString()),
			"physical column preset selection did not change the task field layout");
		assertTrue(fixture.sheet.getFieldArray().size() > 1, "selected column preset must retain visible task fields");

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(fixture.project, saved),
			"MPO save rejected the physical column preset layout");
		Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		assertEquals(fixture.sheet.getFieldArray().size(), reloaded.getFieldArray().size(),
			"MPO reload changed the field count selected by the physical preset");
		List<String> selectedIds = fixture.sheet.getFieldArray().stream().map(Field::getId).toList();
		List<String> reloadedIds = reloaded.getFieldArray().stream().map(Field::getId).toList();
		assertEquals(selectedIds, reloadedIds, "MPO reload lost the field layout selected by preset " + selectedPreset);
	}

	/**
	 * Issue #899 (with #894): the Task Mode column has no check boxes. A task row shows the icon and the Auto
	 * Scheduled / Manually Scheduled text, a void row shows nothing, a click on the active cell opens the list,
	 * choosing an item (mouse, or Alt+Down plus arrow and Enter) commits through the one field-edit Undo path.
	 * Rendered pixels come from the screen.
	 */
	@Test
	void physicalTaskModeColumnShowsTextAndIsChosenFromAList() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		final String fieldId = "Field.manuallyScheduled";
		final String manualLabel = com.microproject.strings.Messages.getString("TaskMode.manual");
		final String autoLabel = com.microproject.strings.Messages.getString("TaskMode.auto");
		Fixture fixture = createFixture(3);
		showFixture(fixture, true);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
			boolean present = fixture.sheet.getFieldArray().stream().anyMatch(field -> fieldId.equals(field.getId()));
			if (!present) {
				Field manual = fixture.sheet.getAvailableFields().stream().filter(field -> fieldId.equals(field.getId()))
					.findFirst().orElseThrow(() -> new AssertionError("task field catalog lacks " + fieldId));
				fixture.sheet.setFieldArray(((SpreadSheetFieldArray) fixture.sheet.getFieldArray())
					.insertField(fixture.sheet.getFieldArray().size(), manual));
			}
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.sheet.getColumnCount() > 1,
			"task table was not ready for the Task Mode column check");
		int column = findColumn(fixture.sheet, fieldId);
		assertEquals(com.microproject.strings.Messages.getString("Field.manuallyScheduled"), fixture.sheet.getColumnName(column),
			"the column header is the localized Task Mode");
		GraphicNode node = (GraphicNode) fixture.gantt.getModel().getCache().getElementAt(0);
		NormalTask task = (NormalTask) node.getNode().getImpl();
		long taskUid = task.getUniqueId();
		int taskRow = fixture.sheet.getCache().getRowAt(node);
		int emptyRow = fixture.taskCount + 1;
		int[] rowCount = new int[1];
		SwingUtilities.invokeAndWait(() -> rowCount[0] = fixture.sheet.getRowCount());
		assertTrue(rowCount[0] > emptyRow, "the sheet must expose void rows below the last task, rows=" + rowCount[0]);
		assertFalse(task.isManuallyScheduled(), "fixture task must start auto-scheduled");

		// Rendering: label text and icon on a task row, nothing on a void row, never a check box.
		assertEquals(autoLabel, renderedText(fixture.sheet, taskRow, column), "a task row shows Auto Scheduled");
		assertTrue(renderedHasIcon(fixture.sheet, taskRow, column), "a task row shows the Task Mode icon");
		assertEquals("", renderedText(fixture.sheet, emptyRow, column), "a void row shows no text");
		assertFalse(renderedHasIcon(fixture.sheet, emptyRow, column), "a void row shows no icon");
		BufferedImage emptyBefore = captureCellInterior(robot, fixture.sheet, emptyRow, column);
		BufferedImage taskBefore = captureCellInterior(robot, fixture.sheet, taskRow, column);
		assertEquals(0, countDifferentPixels(emptyBefore), "an empty row must paint nothing in " + fieldId);
		assertTrue(countDifferentPixels(taskBefore) > 0, "a real task row must paint its icon and label in " + fieldId);
		captureVisibleLayout(robot, "task-table-task-mode.png");

		// A first click only selects the row; a second click on the active cell opens the list.
		UndoSnapshot undoBefore = UndoSnapshot.capture(fixture.project);
		clickCell(robot, fixture.sheet, taskRow, column);
		GuiAcceptanceSupport.await(() -> fixture.sheet.getSelectedRow() == taskRow, "clicking the cell did not select its row");
		assertFalse(fixture.sheet.isEditing(), "a click on a cell that is not active only selects it");
		Thread.sleep(900); // past the multi-click interval so the next click is a new single click
		clickCell(robot, fixture.sheet, taskRow, column);
		JComboBox<?> list = awaitTaskModeList(fixture.sheet);
		GuiAcceptanceSupport.await(list::isPopupVisible, "a click on the active Task Mode cell did not open the list");
		assertEquals(List.of(manualLabel, autoLabel), comboItems(list), "the list offers Manually Scheduled then Auto Scheduled");
		captureVisibleLayout(robot, "task-table-task-mode-popup.png");
		assertFalse(task.isManuallyScheduled(), "opening the list must not change the task");
		clickListItem(robot, list, 0);
		GuiAcceptanceSupport.await(() -> task.isManuallyScheduled(), "choosing Manually Scheduled did not set manual scheduling");
		GuiAcceptanceSupport.await(() -> !fixture.sheet.isEditing(), "the list editor stayed active after choosing");
		assertEquals(manualLabel, renderedText(fixture.sheet, taskRow, column), "the cell shows Manually Scheduled");
		assertTrue(renderedHasIcon(fixture.sheet, taskRow, column), "the manual cell keeps its icon");
		BufferedImage taskManual = captureCellInterior(robot, fixture.sheet, taskRow, column);
		assertTrue(countDifferent(taskBefore, taskManual) > 0, "the task cell must repaint for the new mode");
		assertEquals(0, countDifferentPixels(captureCellInterior(robot, fixture.sheet, emptyRow, column)),
			"empty rows must still paint nothing after a task row changed");
		assertTrue(!undoBefore.equals(UndoSnapshot.capture(fixture.project)), "choosing must add an Undo edit");

		// Undo / Redo through the physical shortcuts.
		SwingUtilities.invokeAndWait(() -> fixture.sheet.requestFocusInWindow());
		GuiAcceptanceSupport.await(fixture.sheet::isFocusOwner, "task table did not regain focus before physical Undo");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_Z);
		robot.keyRelease(KeyEvent.VK_Z);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> !task.isManuallyScheduled(), "physical Ctrl+Z did not restore auto scheduling");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> task.isManuallyScheduled(), "physical Ctrl+Y did not reapply manual scheduling");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_Z);
		robot.keyRelease(KeyEvent.VK_Z);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> !task.isManuallyScheduled(), "second physical Ctrl+Z did not restore auto scheduling");

		// Keyboard route: Alt+Down opens the list of the active cell, Down+Enter chooses the first item.
		SwingUtilities.invokeAndWait(() -> {
			fixture.sheet.requestFocusInWindow();
			fixture.sheet.changeSelection(taskRow, column, false, false);
		});
		GuiAcceptanceSupport.await(fixture.sheet::isFocusOwner, "task table did not regain focus before Alt+Down");
		robot.keyPress(KeyEvent.VK_ALT);
		robot.keyPress(KeyEvent.VK_DOWN);
		robot.keyRelease(KeyEvent.VK_DOWN);
		robot.keyRelease(KeyEvent.VK_ALT);
		JComboBox<?> keyboardList = awaitTaskModeList(fixture.sheet);
		GuiAcceptanceSupport.await(keyboardList::isPopupVisible, "Alt+Down did not open the Task Mode list");
		// The highlight starts on the current value (Auto Scheduled, second item): Up moves to Manually Scheduled.
		robot.keyPress(KeyEvent.VK_UP);
		robot.keyRelease(KeyEvent.VK_UP);
		Thread.sleep(200);
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(() -> task.isManuallyScheduled(), "arrow plus Enter did not choose Manually Scheduled");
		GuiAcceptanceSupport.await(() -> !fixture.sheet.isEditing(), "the list editor stayed active after Enter");

		// Typed route: 'n' highlights the second item of a Yes/No style list; for Task Mode the first letter of
		// "Auto" / the generic n selects Auto Scheduled, Enter commits it.
		SwingUtilities.invokeAndWait(() -> {
			fixture.sheet.requestFocusInWindow();
			fixture.sheet.changeSelection(taskRow, column, false, false);
		});
		GuiAcceptanceSupport.await(fixture.sheet::isFocusOwner, "task table did not regain focus before typing");
		robot.keyPress(KeyEvent.VK_N);
		robot.keyRelease(KeyEvent.VK_N);
		JComboBox<?> typedList = awaitTaskModeList(fixture.sheet);
		GuiAcceptanceSupport.await(typedList::isPopupVisible, "a typed character did not open the Task Mode list");
		Thread.sleep(300);
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(() -> !task.isManuallyScheduled(), "typing n plus Enter did not choose Auto Scheduled");
		GuiAcceptanceSupport.await(() -> !fixture.sheet.isEditing(), "the list editor stayed active after typed Enter");

		// Cancelling the editor leaves the task unchanged.
		SwingUtilities.invokeAndWait(() -> {
			fixture.sheet.requestFocusInWindow();
			fixture.sheet.changeSelection(taskRow, column, false, false);
		});
		robot.keyPress(KeyEvent.VK_ALT);
		robot.keyPress(KeyEvent.VK_DOWN);
		robot.keyRelease(KeyEvent.VK_DOWN);
		robot.keyRelease(KeyEvent.VK_ALT);
		JComboBox<?> cancelList = awaitTaskModeList(fixture.sheet);
		GuiAcceptanceSupport.await(cancelList::isPopupVisible, "Alt+Down did not reopen the Task Mode list");
		// The document root-pane Escape action (GraphicManager) cancels the active sheet's editor; this fixture's
		// DocumentFrame does not own fixture.sheet, so cancel through that same editor call instead of the key.
		SwingUtilities.invokeAndWait(() -> fixture.sheet.getCellEditor().cancelCellEditing());
		GuiAcceptanceSupport.await(() -> !fixture.sheet.isEditing(), "cancelling did not close the Task Mode editor");
		assertFalse(task.isManuallyScheduled(), "cancelling must not change the task");

		// Save / reload with a manual task; identify the task by UID.
		SwingUtilities.invokeAndWait(() -> fixture.sheet.setValueAt(manualLabel, taskRow, column));
		GuiAcceptanceSupport.await(() -> task.isManuallyScheduled(), "the label text did not set manual scheduling");
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(fixture.project, saved), "MPO save rejected the Task Mode change");
		Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		NormalTask restored = reloaded.getTasks().stream()
			.filter(value -> value instanceof NormalTask && value.getUniqueId() == taskUid)
			.map(value -> (NormalTask) value).findFirst().orElseThrow();
		assertTrue(restored.isManuallyScheduled(), "MPO reload lost the manual-scheduling flag");

		// Clicking a void row's cell: record today's behaviour (must not throw, must not create a task).
		int tasksBefore = fixture.project.getTasks().size();
		Throwable[] uncaught = new Throwable[1];
		Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler((thread, error) -> uncaught[0] = error);
		try {
			clickCell(robot, fixture.sheet, emptyRow, column);
			SwingUtilities.invokeAndWait(() -> { });
			GuiAcceptanceSupport.await(() -> !fixture.sheet.isEditing(), "void-row click left an editor open");
		} finally {
			Thread.setDefaultUncaughtExceptionHandler(previous);
		}
		assertEquals(null, uncaught[0], "clicking a void row's Task Mode cell must not throw");
		assertEquals(tasksBefore, fixture.project.getTasks().size(),
			"a single click on a void row selects it; it does not create a task");
		assertEquals(0, countDifferentPixels(captureCellInterior(robot, fixture.sheet, emptyRow, column)),
			"a void row must still paint nothing after being clicked");
	}

	private static String renderedText(SpreadSheet sheet, int row, int column) throws Exception {
		String[] text = new String[1];
		SwingUtilities.invokeAndWait(() -> {
			Component component = sheet.getCellRenderer(row, column).getTableCellRendererComponent(
				sheet, sheet.getValueAt(row, column), false, false, row, column);
			text[0] = ((javax.swing.JLabel) component).getText();
		});
		return text[0];
	}

	private static boolean renderedHasIcon(SpreadSheet sheet, int row, int column) throws Exception {
		boolean[] icon = new boolean[1];
		SwingUtilities.invokeAndWait(() -> {
			Component component = sheet.getCellRenderer(row, column).getTableCellRendererComponent(
				sheet, sheet.getValueAt(row, column), false, false, row, column);
			icon[0] = ((javax.swing.JLabel) component).getIcon() != null;
		});
		return icon[0];
	}

	private static JComboBox<?> awaitTaskModeList(SpreadSheet sheet) throws Exception {
		JComboBox<?>[] combo = new JComboBox<?>[1];
		GuiAcceptanceSupport.await(() -> {
			boolean[] found = new boolean[1];
			try {
				SwingUtilities.invokeAndWait(() -> {
					if (sheet.isEditing() && sheet.getEditorComponent() instanceof JComboBox<?> editor) {
						combo[0] = editor;
						found[0] = true;
					}
				});
			} catch (Exception e) {
				throw new AssertionError(e);
			}
			return found[0];
		}, "no list editor became active in the Task Mode cell");
		return combo[0];
	}

	private static List<String> comboItems(JComboBox<?> combo) throws Exception {
		List<String> items = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			for (int index = 0; index < combo.getItemCount(); index++) items.add(String.valueOf(combo.getItemAt(index)));
		});
		return items;
	}

	/** Physical click on one item of the open list popup. */
	private static void clickListItem(Robot robot, JComboBox<?> combo, int index) throws Exception {
		Point[] target = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			javax.swing.plaf.basic.ComboPopup popup =
				(javax.swing.plaf.basic.ComboPopup) combo.getUI().getAccessibleChild(combo, 0);
			javax.swing.JList<?> items = popup.getList();
			Rectangle bounds = items.getCellBounds(index, index);
			Point origin = items.getLocationOnScreen();
			// An item can be wider than the popup (narrow column): aim at the visible part.
			int visibleWidth = Math.min(bounds.width, combo.getWidth());
			target[0] = new Point(origin.x + bounds.x + visibleWidth / 2, origin.y + bounds.y + bounds.height / 2);
		});
		robot.mouseMove(target[0].x, target[0].y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	private static void clickCell(Robot robot, SpreadSheet sheet, int row, int column) throws Exception {
		Point[] center = new Point[1];
		SwingUtilities.invokeAndWait(() -> {
			Rectangle cell = sheet.getCellRect(row, column, true);
			Point location = sheet.getLocationOnScreen();
			center[0] = new Point(location.x + cell.x + cell.width / 2, location.y + cell.y + cell.height / 2);
		});
		robot.mouseMove(center[0].x, center[0].y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	/** Screen pixels of a cell's interior, inset past the grid line. */
	private static BufferedImage captureCellInterior(Robot robot, SpreadSheet sheet, int row, int column) throws Exception {
		Rectangle[] area = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			Rectangle cell = sheet.getCellRect(row, column, false);
			Point location = sheet.getLocationOnScreen();
			area[0] = new Rectangle(location.x + cell.x + 3, location.y + cell.y + 3, cell.width - 6, cell.height - 6);
		});
		assertTrue(area[0].width > 4 && area[0].height > 4, "cell is too small to inspect");
		robot.mouseMove(0, 0);
		// Wait until the screen is repainted and two consecutive captures agree.
		BufferedImage previous = robot.createScreenCapture(area[0]);
		for (int attempt = 0; attempt < 30; attempt++) {
			Thread.sleep(100);
			BufferedImage current = robot.createScreenCapture(area[0]);
			if (countDifferent(previous, current) == 0) return current;
			previous = current;
		}
		throw new AssertionError("cell pixels never settled at row " + row + ", column " + column);
	}

	/** Pixels differing from the interior's own background (its top-left pixel). */
	private static int countDifferentPixels(BufferedImage image) {
		int background = image.getRGB(0, 0);
		int count = 0;
		for (int y = 0; y < image.getHeight(); y++)
			for (int x = 0; x < image.getWidth(); x++)
				if (image.getRGB(x, y) != background) count++;
		return count;
	}

	private static int countDifferent(BufferedImage before, BufferedImage after) {
		int count = 0;
		for (int y = 0; y < Math.min(before.getHeight(), after.getHeight()); y++)
			for (int x = 0; x < Math.min(before.getWidth(), after.getWidth()); x++)
				if (before.getRGB(x, y) != after.getRGB(x, y)) count++;
		return count;
	}

	private static void clickComponent(Robot robot, Component component) throws Exception {
		Point location = new Point();
		SwingUtilities.invokeAndWait(() -> location.setLocation(component.getLocationOnScreen()));
		robot.mouseMove(location.x + component.getWidth() / 2, location.y + component.getHeight() / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	private static Window awaitWindowWith(Class<? extends Component> type, String message) throws Exception {
		final Window[] result = new Window[1];
		GuiAcceptanceSupport.await(() -> {
			for (Window window : Window.getWindows()) {
				if (window.isShowing() && findComponent(window, type) != null) {
					result[0] = window;
					return true;
				}
			}
			return false;
		}, message);
		return result[0];
	}

	private static <T extends Component> T findComponent(Component root, Class<T> type) {
		if (type.isInstance(root)) return type.cast(root);
		if (root instanceof Container container) {
			for (Component child : container.getComponents()) {
				T found = findComponent(child, type);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static JCheckBox findCheckBox(Component root, String text) {
		if (root instanceof JCheckBox checkBox && text.equals(checkBox.getText())) return checkBox;
		if (root instanceof Container container) {
			for (Component child : container.getComponents()) {
				JCheckBox found = findCheckBox(child, text);
				if (found != null) return found;
			}
		}
		return null;
	}

	private static JButton findButton(Window window, String... labels) {
		for (String label : labels) {
			JButton button = findButton(window, label);
			if (button != null) return button;
		}
		return null;
	}

	private static JButton findButton(Component root, String label) {
		if (root instanceof JButton button && label.equals(button.getText())) return button;
		if (root instanceof Container container) {
			for (Component child : container.getComponents()) {
				JButton found = findButton(child, label);
				if (found != null) return found;
			}
		}
		return null;
	}

	@Test
	void calendarWhitespaceDragExtendsVisibleTaskSelectionWithoutEditingBars() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(3);
		showFixture(fixture);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			selectionController = new TaskSelectionController(fixture.gantt, fixture.sheet);
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> frame.isActive(), "calendar drag test frame did not become active");
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.gantt.isShowing(),
			"task table or Gantt was not visible for calendar whitespace drag");

		Point chartLocation = new Point();
		Rectangle visibleChart = new Rectangle();
		int rowHeight = fixture.gantt.getRowHeight();
		SwingUtilities.invokeAndWait(() -> {
			chartLocation.setLocation(fixture.gantt.getLocationOnScreen());
			visibleChart.setBounds(fixture.gantt.getVisibleRect());
		});
		int x = chartLocation.x + visibleChart.x + visibleChart.width - 12;
		int startY = chartLocation.y + rowHeight / 2;
		int endY = chartLocation.y + rowHeight * 2 + rowHeight / 2;
		robot.mouseMove(x, startY);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseMove(x, endY);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> fixture.sheet.getSelectedRowCount() == 3,
			"dragging over calendar whitespace must extend the selected visible task range");
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(Set.of(0, 1, 2), fixture.gantt.getHighlightedRows(),
				"the Gantt highlight must match the whitespace-drag task range");
			assertEquals(fixture.sheet.getColumnCount(), fixture.sheet.getSelectedColumnCount(),
				"whitespace selection must remain a task-row selection rather than a cell range");
		});
	}

	@Test
	void zeroPercentProgressLinePointSelectsTaskWithoutPanningTimescale() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(1);
		showFixture(fixture);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		AtomicReference<Gantt.BarClick> click = new AtomicReference<>();
		GraphicNode node = (GraphicNode) fixture.gantt.getModel().getCache().getElementAt(0);
		com.microproject.pm.task.Task task = (com.microproject.pm.task.Task) node.getNode().getImpl();
		SwingUtilities.invokeAndWait(() -> {
			task.setPercentComplete(0.0d);
			fixture.gantt.setProgressLineEnabled(true);
			fixture.gantt.setBarSelectionListener(click::set);
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> fixture.gantt.isShowing(), "Gantt was not visible for progress-line hit testing");
		robot.delay(250);

		GanttUI ui = (GanttUI) fixture.gantt.getUI();
		int x = (int) Math.round(fixture.gantt.getCoord().toX(
			GanttProgress.progressLineDate(task, task.getProject().getStatusDate())));
		int row = fixture.gantt.getModel().getCache().getVisibleNodes().getProjectionIndex().rowForNode(node);
		int y = (int) Math.round(ui.getBarY(row) + node.getGanttShapeOffset()
			+ node.getGanttShapeHeight() / 2.0d);
		JScrollPane pane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, fixture.gantt);
		assertTrue(pane != null, "Gantt must be hosted by a scroll pane");
		Point before = pane.getViewport().getViewPosition();
		Point location = new Point();
		SwingUtilities.invokeAndWait(() -> location.setLocation(fixture.gantt.getLocationOnScreen()));
		robot.mouseMove(location.x + x, location.y + y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> click.get() != null,
			"clicking the zero-percent progress point must select a task instead of starting a pan");
		assertEquals(node, click.get().node(), "progress-line click must select the rendered task node");
		Point after = pane.getViewport().getViewPosition();
		assertEquals(before, after, "progress-line click must not move the timescale viewport");
	}

	@Test
	void calendarRowWhitespaceClickSelectsTheMatchingTaskTableRow() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(3);
		showFixture(fixture);

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			selectionController = new TaskSelectionController(fixture.gantt, fixture.sheet);
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> frame.isActive(), "calendar row test frame did not become active");
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.gantt.isShowing(),
			"task table or Gantt was not visible for calendar-row selection");

		int row = 1;
		Point chartLocation = new Point();
		Rectangle visibleChart = new Rectangle();
		int rowHeight = fixture.gantt.getRowHeight();
		SwingUtilities.invokeAndWait(() -> {
			chartLocation.setLocation(fixture.gantt.getLocationOnScreen());
			visibleChart.setBounds(fixture.gantt.getVisibleRect());
		});
		// The far right edge is calendar whitespace for this one-day fixture;
		// selecting there must not require a task-bar hit.
		int x = chartLocation.x + visibleChart.x + visibleChart.width - 12;
		int y = chartLocation.y + row * rowHeight + rowHeight - 3;
		SwingUtilities.invokeAndWait(() -> assertTrue(((GanttUI) fixture.gantt.getUI()).getTaskRowAt(
			row * rowHeight + rowHeight - 3) != null, "test click must target a visible task row"));
		robot.mouseMove(x, y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> fixture.sheet.getSelectedRow() == row,
			"calendar-row click did not select the corresponding task-table row");
		SwingUtilities.invokeAndWait(() -> {
			assertEquals(row, fixture.sheet.getSelectedRow(), "calendar row must select its table row");
			assertTrue(fixture.sheet.isRowFullySelected(row), "calendar row selection must highlight the full table row");
			assertTrue(fixture.gantt.getHighlightedRows().contains(row), "calendar row selection must retain the Gantt highlight");
		});

		int secondRow = 2;
		int secondY = chartLocation.y + secondRow * rowHeight + rowHeight - 3;
		robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL);
		robot.mouseMove(x, secondY);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> fixture.sheet.getSelectedRowCount() == 2,
			"Ctrl+calendar-row click did not retain a noncontiguous task selection");
		SwingUtilities.invokeAndWait(() -> {
			assertTrue(fixture.sheet.getSelectionModel().isSelectedIndex(row), "first calendar row must remain selected");
			assertTrue(fixture.sheet.getSelectionModel().isSelectedIndex(secondRow), "Ctrl click must add the second calendar row");
			assertEquals(fixture.sheet.getColumnCount(), fixture.sheet.getSelectedColumnCount(),
				"every selected Gantt row must highlight all task-table columns");
		});
	}

	@Test
	void taskCellCtrlClickAndVerticalDragRetainEverySelectedRow() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(3);
		showFixture(fixture);

		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(50);
		SwingUtilities.invokeAndWait(() -> {
			selectionController = new TaskSelectionController(fixture.gantt, fixture.sheet);
			frame.toFront();
			frame.requestFocus();
			fixture.sheet.requestFocusInWindow();
		});
		int nameColumn = findColumn(fixture.sheet, "Field.name");
		Point firstCell = screenCenter(fixture.sheet, fixture.sheet.getCellRect(0, nameColumn, true));
		Point thirdCell = screenCenter(fixture.sheet, fixture.sheet.getCellRect(2, nameColumn, true));
		robot.mouseMove(firstCell.x, firstCell.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.mouseMove(thirdCell.x, thirdCell.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> fixture.sheet.getSelectedRowCount() == 2,
			"Ctrl+click in the task table did not retain two noncontiguous rows");
		SwingUtilities.invokeAndWait(() -> {
			assertTrue(fixture.sheet.getSelectionModel().isSelectedIndex(0), "the original task-table row must stay selected");
			assertTrue(fixture.sheet.getSelectionModel().isSelectedIndex(2), "Ctrl+click must select the second task-table row");
			assertEquals(fixture.sheet.getColumnCount(), fixture.sheet.getSelectedColumnCount(),
				"Ctrl+click must highlight every selected task row across all columns");
			assertEquals(Set.of(0, 2), fixture.gantt.getHighlightedRows(),
				"Gantt highlighting must match noncontiguous task-table selection");
		});

		Point secondCell = screenCenter(fixture.sheet, fixture.sheet.getCellRect(1, nameColumn, true));
		robot.mouseMove(firstCell.x, firstCell.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseMove(firstCell.x, firstCell.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.delay(100);
		robot.mouseMove(secondCell.x, secondCell.y);
		robot.delay(100);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> fixture.sheet.getSelectedRowCount() == 2,
			"task-table drag selection collapsed after mouse release");
		SwingUtilities.invokeAndWait(() -> {
			assertTrue(fixture.sheet.getSelectionModel().isSelectedIndex(0), "drag range must retain its first task row");
			assertTrue(fixture.sheet.getSelectionModel().isSelectedIndex(1), "drag range must retain its last task row");
			assertEquals(Set.of(0, 1), fixture.gantt.getHighlightedRows(),
				"Gantt highlighting must retain every row selected by a task-table drag");
		});
	}

	/**
	 * Microsoft Project "Header Wrapping" (Table Definition): a heading that does not fit wraps by default and
	 * the header row grows to the tallest wrapped heading; the Gantt timescale header, the row-header corner and
	 * the first rows of both halves stay aligned; the switch is on the column popup, undoable and saved in MPO.
	 */
	@Test
	void physicalHeaderWrapGrowsTheHeaderRowKeepsGanttAlignedAndCanBeSwitchedOffFromThePopup() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(3);
		showFixture(fixture, true, true);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(40);
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
		});
		GuiAcceptanceSupport.await(() -> hasVisibleColumns(fixture.sheet, 3), "task table was not ready for header wrapping");
		int baseHeight = com.microproject.graphic.configuration.GraphicConfiguration.getInstance().getColumnHeaderHeight();
		String fieldId = "Field.percentWorkComplete";
		int[] columnValue = new int[1];
		SwingUtilities.invokeAndWait(() -> {
			columnValue[0] = findColumn(fixture.sheet, fieldId);
			var column = fixture.sheet.getColumnModel().getColumn(columnValue[0]);
			column.setPreferredWidth(44);
			column.setWidth(44);
		});
		int column = columnValue[0];
		GuiAcceptanceSupport.await(() -> headerHeightsAgree(fixture, baseHeight, true),
			"a narrow column with a long heading did not grow the header row, the corner and the Gantt timescale together");
		robot.delay(300);

		SwingUtilities.invokeAndWait(() -> {
			var tableColumn = fixture.sheet.getColumnModel().getColumn(column);
			var renderer = (com.microproject.pm.graphic.spreadsheet.renderer.SpreadSheetColumnHeaderRenderer) tableColumn.getHeaderRenderer();
			renderer.getTableCellRendererComponent(fixture.sheet, tableColumn.getHeaderValue(), false, false, -1, column);
			List<String> lines = renderer.getWrappedLines();
			assertTrue(lines.size() > 1, "the heading must wrap onto several lines, lines=" + lines);
			assertEquals(String.valueOf(tableColumn.getHeaderValue()).replace(" ", ""),
				String.join("", lines).replace(" ", ""), "wrapping must show the full heading text");
			assertFalse(String.join("", lines).contains("…"), "a wrapped heading must not end in an ellipsis");
			java.awt.FontMetrics metrics = fixture.sheet.getTableHeader().getFontMetrics(renderer.getFont());
			assertTrue(fixture.sheet.getTableHeader().getHeight() >= lines.size() * metrics.getHeight(),
				"the header row must be tall enough for every wrapped line");
		});
		captureVisibleLayout(robot, "task-table-header-wrap.png");
		assertRowsAndHeadersAligned(fixture);

		// Physical click selects the column; the outline must stay a closed rectangle on the taller header.
		Point headerPoint = screenCenter(fixture.sheet.getTableHeader(), fixture.sheet.getTableHeader().getHeaderRect(column));
		robot.mouseMove(headerPoint.x, headerPoint.y);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		GuiAcceptanceSupport.await(() -> activeHeaderColumn(fixture.sheet) == column, "header click did not select the wrapped column");
		SwingUtilities.invokeAndWait(() -> assertHeaderHighlight(fixture.sheet, column));
		robot.mouseMove(0, 0);
		robot.delay(300);
		assertActiveHeaderOutlineClosedOnScreen(robot, fixture.sheet, column, "task-table-header-wrap-outline.png");

		int wrappedHeight = headerHeight(fixture);
		String stateBeforeToggle = headerState(fixture, column);
		openHeaderPopupAndToggleWrap(robot, fixture, column, true);
		GuiAcceptanceSupport.await(() -> !isHeaderWrapped(fixture, column) && headerHeight(fixture) < wrappedHeight
				&& headerHeightsAgree(fixture, baseHeight, false),
			"Header Text Wrapping off must show one line, shrink the header row and let the Gantt timescale follow");
		assertRowsAndHeadersAligned(fixture);
		SwingUtilities.invokeAndWait(() -> assertFalse(fixture.sheet.isColumnHeaderWrap(column + 1),
			"the column must record that its heading no longer wraps"));

		SwingUtilities.invokeAndWait(() -> fixture.sheet.requestFocusInWindow());
		GuiAcceptanceSupport.await(fixture.sheet::isFocusOwner, "task table did not regain focus before physical Undo");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_Z);
		robot.keyRelease(KeyEvent.VK_Z);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		try {
			GuiAcceptanceSupport.await(() -> isHeaderWrapped(fixture, column) && headerHeight(fixture) == wrappedHeight
					&& headerHeightsAgree(fixture, baseHeight, true),
				"physical Ctrl+Z did not restore the wrapped heading and the taller header row");
		} catch (AssertionError failure) {
			throw new AssertionError(failure.getMessage() + "; before toggle: " + stateBeforeToggle
				+ "; after Ctrl+Z: " + headerState(fixture, column), failure);
		}
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_Y);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		GuiAcceptanceSupport.await(() -> !isHeaderWrapped(fixture, column) && headerHeightsAgree(fixture, baseHeight, false),
			"physical Ctrl+Y did not switch wrapping off again");

		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(fixture.project, saved), "MPO save rejected the header wrap switch");
		Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		SpreadSheetFieldArray restored = reloaded.getFieldArray();
		for (int index = 0; index < restored.size(); index++)
			assertEquals(!fieldId.equals(restored.get(index).getId()), restored.isHeaderWrap(index),
				"MPO reload must keep exactly the switched-off column unwrapped: " + restored.get(index).getId());
	}

	private static int headerHeight(Fixture fixture) {
		int[] height = new int[1];
		try {
			SwingUtilities.invokeAndWait(() -> height[0] = fixture.sheet.getTableHeader().getHeight());
		} catch (Exception exception) {
			throw new AssertionError("Could not read the header height", exception);
		}
		return height[0];
	}

	private static int activeHeaderColumn(SpreadSheet sheet) {
		int[] column = new int[1];
		try {
			SwingUtilities.invokeAndWait(() -> column[0] = sheet.getActiveHeaderColumn());
		} catch (Exception exception) {
			throw new AssertionError("Could not read the active header column", exception);
		}
		return column[0];
	}

	/** Diagnostic snapshot of the wrapped column, the header heights and the Undo history. */
	private String headerState(Fixture fixture, int column) {
		String[] state = new String[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
				var tableColumn = fixture.sheet.getColumnModel().getColumn(column);
				var renderer = (com.microproject.pm.graphic.spreadsheet.renderer.SpreadSheetColumnHeaderRenderer) tableColumn.getHeaderRenderer();
				renderer.getTableCellRendererComponent(fixture.sheet, tableColumn.getHeaderValue(), false, false, -1, column);
				JScrollPane sheetPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, fixture.sheet);
				java.awt.Component corner = sheetPane.getCorner(JScrollPane.UPPER_LEFT_CORNER);
				state[0] = "field=" + ((com.microproject.field.Field) tableColumn.getIdentifier()).getId()
					+ " wrapFlag=" + fixture.sheet.isColumnHeaderWrap(column + 1)
					+ " width=" + tableColumn.getWidth() + " lines=" + renderer.getWrappedLines()
					+ " header=" + fixture.sheet.getTableHeader().getHeight()
					+ " headerPreferred=" + fixture.sheet.getTableHeader().getPreferredSize().height
					+ " sheetHeaderHeight=" + fixture.sheet.getColumnHeaderHeight()
					+ " corner=" + (corner == null ? "none" : corner.getHeight())
					+ " timescale=" + ganttTimescalePane.getTimeScaleComponent().getHeight()
					+ " undo=" + fixture.project.getUndoController().getEditNames();
			});
		} catch (Exception exception) {
			return "unavailable: " + exception;
		}
		return state[0];
	}

	private static boolean isHeaderWrapped(Fixture fixture, int column) {
		boolean[] wrapped = new boolean[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
				var tableColumn = fixture.sheet.getColumnModel().getColumn(column);
				var renderer = (com.microproject.pm.graphic.spreadsheet.renderer.SpreadSheetColumnHeaderRenderer) tableColumn.getHeaderRenderer();
				renderer.getTableCellRendererComponent(fixture.sheet, tableColumn.getHeaderValue(), false, false, -1, column);
				wrapped[0] = renderer.getWrappedLines().size() > 1;
			});
		} catch (Exception exception) {
			throw new AssertionError("Could not inspect the header renderer", exception);
		}
		return wrapped[0];
	}

	/** Sheet header, row-header corner and Gantt timescale have one on-screen height (taller than a single line when expected). */
	private boolean headerHeightsAgree(Fixture fixture, int baseHeight, boolean expectTaller) {
		boolean[] agree = new boolean[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
				JScrollPane sheetPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, fixture.sheet);
				if (!fixture.sheet.isShowing() || !ganttTimescalePane.getTimeScaleComponent().isShowing()
						|| sheetPane.getCorner(JScrollPane.UPPER_LEFT_CORNER) == null
						|| !sheetPane.getCorner(JScrollPane.UPPER_LEFT_CORNER).isShowing()) return;
				int header = fixture.sheet.getTableHeader().getHeight();
				int corner = sheetPane.getCorner(JScrollPane.UPPER_LEFT_CORNER).getHeight();
				int timescale = ganttTimescalePane.getTimeScaleComponent().getHeight();
				agree[0] = header == corner && header == timescale && header == fixture.sheet.getColumnHeaderHeight()
					&& (expectTaller ? header > baseHeight : header >= baseHeight);
			});
		} catch (Exception exception) {
			throw new AssertionError("Could not compare the header heights", exception);
		}
		return agree[0];
	}

	private void assertRowsAndHeadersAligned(Fixture fixture) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			JScrollPane sheetPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, fixture.sheet);
			int sheetHeaderTop = fixture.sheet.getTableHeader().getLocationOnScreen().y;
			int timescaleTop = ganttTimescalePane.getTimeScaleComponent().getLocationOnScreen().y;
			int cornerTop = sheetPane.getCorner(JScrollPane.UPPER_LEFT_CORNER).getLocationOnScreen().y;
			assertEquals(sheetHeaderTop, timescaleTop, "the sheet header and the Gantt timescale must start on the same line");
			assertEquals(sheetHeaderTop, cornerTop, "the corner must sit on the header row");
			assertEquals(fixture.sheet.getTableHeader().getHeight(), ganttTimescalePane.getTimeScaleComponent().getHeight(),
				"production code must keep the Gantt timescale as tall as the sheet header");
			int sheetFirstRow = fixture.sheet.getLocationOnScreen().y + fixture.sheet.getCellRect(0, 0, true).y;
			int ganttFirstRow = fixture.gantt.getLocationOnScreen().y;
			assertEquals(sheetFirstRow, ganttFirstRow, "the first task-table row and the first Gantt row must start on the same line");
		});
	}


	private void openHeaderPopupAndToggleWrap(Robot robot, Fixture fixture, int column, boolean expectChecked) throws Exception {
		Point headerPoint = screenCenter(fixture.sheet.getTableHeader(), fixture.sheet.getTableHeader().getHeaderRect(column));
		robot.mouseMove(headerPoint.x, headerPoint.y);
		robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
		final SpreadSheetColumnMenu[] popup = new SpreadSheetColumnMenu[1];
		GuiAcceptanceSupport.await(() -> {
			for (MenuElement element : MenuSelectionManager.defaultManager().getSelectedPath()) {
				if (element instanceof SpreadSheetColumnMenu menu) {
					popup[0] = menu;
					return true;
				}
			}
			return false;
		}, "physical header right-click did not open the column popup");
		javax.swing.JCheckBoxMenuItem[] item = new javax.swing.JCheckBoxMenuItem[1];
		SwingUtilities.invokeAndWait(() -> {
			for (Component component : popup[0].getComponents())
				if (component instanceof javax.swing.JCheckBoxMenuItem check) item[0] = check;
			assertTrue(item[0] != null, "the column popup must offer the Header Text Wrapping check box");
			assertEquals(com.microproject.strings.Messages.getString("SpreadSheetColumnMenu.HeaderWrap"), item[0].getText());
			assertEquals(expectChecked, item[0].isSelected(), "the check box must show the column's current wrapping");
		});
		clickComponent(robot, item[0]);
	}

	private static void assertHeaderHighlight(SpreadSheet sheet, int activeColumn) {
		for (int column = 0; column < sheet.getColumnCount(); column++) {
			TableCellRenderer renderer = sheet.getColumnModel().getColumn(column).getHeaderRenderer();
			Component component = renderer.getTableCellRendererComponent(sheet, null, false, false, -1, column);
			assertTrue(component instanceof javax.swing.JComponent, "header renderer must return a Swing component");
			assertTrue(component.getBackground().equals(column == activeColumn
					? FlatUiSupport.spreadsheetHeaderSelectedBackground()
					: FlatUiSupport.spreadsheetHeaderBackground()),
				"header renderer must highlight only the clicked view column");
			if (column == activeColumn)
				assertTrue(((javax.swing.JComponent) component).getBorder() instanceof LineBorder,
					"active header must retain its visible focus border");
		}
	}

	/** The popup item with this text, so tests do not depend on menu order. */
	private static JMenuItem menuItemWithText(JPopupMenu menu, String text) {
		for (java.awt.Component component : menu.getComponents())
			if (component instanceof JMenuItem item && text.equals(item.getText()))
				return item;
		throw new AssertionError("popup has no item " + text);
	}

	/**
	 * Issue #860: the selected column header must show a closed outline on the
	 * physical screen. Each edge is scanned along its full length, skipping the
	 * corners; within its outermost two pixels it must contain a pixel closer to
	 * the active outline colour than to the header fill, because fractional UI
	 * scales blend the 1px line with the fill. The device-resolution capture is
	 * used so that downsampling to user space cannot drop a fractional-scale
	 * edge. The screen is sampled until the asynchronous repaint settles, and
	 * the last capture is kept as evidence.
	 */
	private static void assertActiveHeaderOutlineClosedOnScreen(Robot robot, SpreadSheet sheet, int column) throws Exception {
		assertActiveHeaderOutlineClosedOnScreen(robot, sheet, column, "task-table-active-column-header.png");
	}

	private static void assertActiveHeaderOutlineClosedOnScreen(Robot robot, SpreadSheet sheet, int column,
			String artifactName) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			Rectangle cell = sheet.getTableHeader().getHeaderRect(column);
			Point origin = sheet.getTableHeader().getLocationOnScreen();
			// One logical pixel of margin on every side: at fractional scales the cell's
			// edges fall between device pixels, and a capture of exactly the cell can
			// round inward and miss the outline's last device pixel (seen at 150%).
			bounds[0] = new Rectangle(origin.x + cell.x - 1, origin.y + cell.y - 1, cell.width + 2, cell.height + 2);
		});
		java.awt.Color outline = FlatUiSupport.spreadsheetActiveCellBorderColor();
		long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(3);
		BufferedImage capture;
		String openEdge;
		do {
			robot.waitForIdle();
			robot.delay(100);
			capture = deviceResolutionCapture(robot, bounds[0]);
			openEdge = findOpenOutlineEdge(capture, outline);
		} while (openEdge != null && System.nanoTime() < deadline);
		Path directory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		ImageIO.write(capture, "png", directory.resolve(artifactName).toFile());
		assertTrue(openEdge == null, "selected column header outline is open: " + openEdge + " capture="
			+ capture.getWidth() + "x" + capture.getHeight() + " bounds=" + bounds[0] + " outline="
			+ Integer.toHexString(outline.getRGB() & 0xFFFFFF) + " rightBand=" + rowHex(capture, Math.min(3, capture.getHeight() - 1), 6)
			+ " headerRectNow=" + headerRectOnScreen(sheet, column) + " activeHeaderColumn=" + sheet.getActiveHeaderColumn());
	}

	private static Rectangle headerRectOnScreen(SpreadSheet sheet, int column) {
		Rectangle[] rect = new Rectangle[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
				Rectangle cell = sheet.getTableHeader().getHeaderRect(column);
				Point origin = sheet.getTableHeader().getLocationOnScreen();
				rect[0] = new Rectangle(origin.x + cell.x, origin.y + cell.y, cell.width, cell.height);
			});
		} catch (Exception exception) {
			return null;
		}
		return rect[0];
	}

	/** Hex colours of the last {@code count} pixels of a capture row (failure diagnostics). */
	private static String rowHex(BufferedImage capture, int y, int count) {
		StringBuilder hex = new StringBuilder();
		for (int x = Math.max(0, capture.getWidth() - count); x < capture.getWidth(); x++)
			hex.append(Integer.toHexString(capture.getRGB(x, y) & 0xFFFFFF)).append(' ');
		return hex.toString().trim();
	}

	private static String findOpenOutlineEdge(BufferedImage capture, java.awt.Color outline) {
		int width = capture.getWidth();
		int height = capture.getHeight();
		int band = 3;
		java.awt.Color fill = new java.awt.Color(capture.getRGB(band + 1, band + 1));
		for (int x = band; x < width - band; x++) {
			if (!nearColor(capture, x, 0, x, band - 1, outline, fill))
				return "top edge at x=" + x;
			if (!nearColor(capture, x, height - band, x, height - 1, outline, fill))
				return "bottom edge at x=" + x;
		}
		for (int y = band; y < height - band; y++) {
			if (!nearColor(capture, 0, y, band - 1, y, outline, fill))
				return "left edge at y=" + y;
			if (!nearColor(capture, width - band, y, width - 1, y, outline, fill))
				return "right edge at y=" + y;
		}
		return null;
	}

	private static BufferedImage deviceResolutionCapture(Robot robot, Rectangle bounds) {
		java.awt.Image largest = null;
		for (java.awt.Image variant : robot.createMultiResolutionScreenCapture(bounds).getResolutionVariants()) {
			if (largest == null || variant.getWidth(null) > largest.getWidth(null))
				largest = variant;
		}
		if (largest instanceof BufferedImage buffered)
			return buffered;
		BufferedImage copy = new BufferedImage(largest.getWidth(null), largest.getHeight(null), BufferedImage.TYPE_INT_RGB);
		java.awt.Graphics2D graphics = copy.createGraphics();
		try {
			graphics.drawImage(largest, 0, 0, null);
		} finally {
			graphics.dispose();
		}
		return copy;
	}

	private static boolean nearColor(BufferedImage image, int x0, int y0, int x1, int y1, java.awt.Color expected,
			java.awt.Color fill) {
		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				java.awt.Color actual = new java.awt.Color(image.getRGB(x, y));
				if (colorDistance(actual, expected) < colorDistance(actual, fill))
					return true;
			}
		}
		return false;
	}

	private static int colorDistance(java.awt.Color a, java.awt.Color b) {
		return Math.abs(a.getRed() - b.getRed()) + Math.abs(a.getGreen() - b.getGreen())
			+ Math.abs(a.getBlue() - b.getBlue());
	}

	private static Point screenCenter(javax.swing.JComponent component, Rectangle bounds) throws Exception {
		Point location = new Point();
		SwingUtilities.invokeAndWait(() -> location.setLocation(component.getLocationOnScreen()));
		return new Point(location.x + bounds.x + bounds.width / 2, location.y + bounds.y + bounds.height / 2);
	}

	private static com.microproject.grouping.core.Node findTaskNode(Project project, String name) {
		java.util.Iterator<com.microproject.pm.task.Task> iterator = project.getTaskOutlineIterator();
		while (iterator.hasNext()) {
			var task = iterator.next();
			if (name.equals(task.getName()))
				return project.getTaskModel().search(task);
		}
		throw new AssertionError("task not found in fixture: " + name);
	}

	private static int taskRow(SpreadSheet sheet, Project project, String name) {
		GraphicNode graphicNode = (GraphicNode) sheet.getCache().getGraphicNode(findTaskNode(project, name));
		int row = sheet.getCache().getRowAt(graphicNode);
		if (row < 0)
			throw new AssertionError("task row not visible in fixture: " + name);
		return row;
	}

	private void captureVisibleLayout(Robot robot) throws Exception {
		captureVisibleLayout(robot, "task-table-gantt-grid.png");
	}

	private void captureVisibleLayout(Robot robot, String fileName) throws Exception {
		Rectangle[] bounds = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> bounds[0] = new Rectangle(frame.getRootPane().getLocationOnScreen(), frame.getRootPane().getSize()));
		BufferedImage screenshot = robot.createScreenCapture(bounds[0]);
		Path directory = Path.of(System.getProperty("microproject.gui.artifacts.dir", "build/guiTest-artifacts"));
		Files.createDirectories(directory);
		ImageIO.write(screenshot, "png", directory.resolve(fileName).toFile());
		assertTrue(screenshot.getWidth() > 400 && screenshot.getHeight() > 300, "captured layout is unexpectedly small");
	}

	private void showFixture(Fixture fixture) throws Exception {
		showFixture(fixture, false);
	}

	private void showFixture(Fixture fixture, boolean installDocumentShortcuts) throws Exception {
		showFixture(fixture, installDocumentShortcuts, false);
	}

	/**
	 * @param productionTimescale lay the chart out like GanttView does: a real ScaledScrollPane timescale
	 *            bound to the sheet by production code, instead of a fixed-height stand-in header
	 */
	private void showFixture(Fixture fixture, boolean installDocumentShortcuts, boolean productionTimescale) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			if (installDocumentShortcuts) {
				FixtureFrame fixtureFrame = new FixtureFrame();
				frame = fixtureFrame;
				FixtureGraphicManager manager = fixtureFrame.manager;
				popupGraphicManager = manager;
				DocumentFrame document = new DocumentFrame(manager, fixture.project, "task-table-column-popup-test");
				document.setActive(true);
				manager.setDocumentFrame(document);
				GuiAcceptanceShortcutInstaller.install(manager, fixtureFrame);
			} else {
				frame = new JFrame("Task table and Gantt GUI acceptance");
			}
			JScrollPane ganttPane;
			JScrollPane sheetPane;
			if (productionTimescale) {
				// Nothing here aligns the two header rows: the production binding must do it.
				ganttTimescalePane = new com.microproject.pm.graphic.timescale.ScaledScrollPane(fixture.gantt,
					new CoordinatesConverter(fixture.project), null, fixture.sheet.getRowHeight());
				ganttTimescalePane.followColumnHeaderHeight(fixture.sheet);
				ganttPane = ganttTimescalePane;
				sheetPane = SpreadSheetUtils.makeSpreadsheetScrollPane(fixture.sheet);
			} else {
				ganttPane = new JScrollPane(fixture.gantt);
				// The production GanttView puts a time-scale header above the chart.
				// Keep this lightweight fixture's row origin aligned with the table.
				int headerHeight = fixture.sheet.getTableHeader().getPreferredSize().height;
				JPanel header = new JPanel();
				header.setPreferredSize(new Dimension(980, headerHeight));
				ganttPane.setColumnHeaderView(header);
				sheetPane = new JScrollPane(fixture.sheet);
			}
			JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				sheetPane, ganttPane);
			splitPane.setResizeWeight(0.45);
			frame.add(splitPane);
			frame.setPreferredSize(new Dimension(1100, 520));
			frame.pack();
			frame.setLocationByPlatform(true);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
			gantt.updateSize();
		});
	}

	private static final class FixtureFrame extends JFrame implements FrameHolder {
		private static final long serialVersionUID = 1L;
		private FixtureGraphicManager manager;

		FixtureFrame() {
			super("Task table and Gantt GUI acceptance");
			manager = new FixtureGraphicManager(this);
			manager.getMenuManager();
		}

		@Override public FrameManager getFrameManager() { return null; }
		@Override public GraphicManager getGraphicManager() { return manager; }
		@Override public void setGraphicManager(GraphicManager manager) { this.manager = (FixtureGraphicManager) manager; }
	}

	private static final class FixtureGraphicManager extends GraphicManager {
		private DocumentFrame documentFrame;

		FixtureGraphicManager(FixtureFrame frame) { super(frame); }
		void setDocumentFrame(DocumentFrame frame) { documentFrame = frame; }
		@Override public DocumentFrame getCurrentFrame() { return documentFrame; }
	}

	private Fixture createFixture() throws Exception {
		return createFixture(1);
	}

	private Fixture createFixtureWithEmptyMiddle() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("gui-task-gantt-empty-middle-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		List<NormalTask> tasks = new ArrayList<>();
		for (int index = 1; index <= 3; index++) {
			NormalTask task = project.createScriptedTask();
			task.setName(index == 2 ? "" : "Task " + (index == 1 ? "A" : "B"));
			task.getCurrentSchedule().setStart(project.getStart());
			task.setDuration(index == 2 ? 0L : (index == 1
			? CalendarOption.getInstance().getMillisPerDay() / 2
			: CalendarOption.getInstance().getMillisPerDay() * 3));
			tasks.add(task);
		}
		project.recalculate();
		Fixture[] fixture = new Fixture[1];
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()), "gui-task-gantt-empty-middle-test", null);
			SpreadSheetUtils.setFieldsAndContext(sheet, cache, SpreadSheetCategories.taskSpreadsheetCategory, "Spreadsheet.Task.entry", true);
			gantt = new Gantt(project, "Gantt");
			gantt.setCache(cache);
			gantt.setCoord(new CoordinatesConverter(project));
			gantt.setBarStyles((BarStyles) Dictionary.get(BarStyles.category, "standard"));
			gantt.updateSize();
			fixture[0] = new Fixture(sheet, gantt, project, tasks.size(), 0, 0);
		});
		return fixture[0];
	}

	private Fixture createFixture(int taskCount) throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("gui-task-gantt-grid-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		List<NormalTask> tasks = new ArrayList<>();
		for (int index = 1; index <= taskCount; index++) {
			NormalTask task = project.createScriptedTask();
			task.setName(index <= 10 ? "Sequential " + index : "Independent " + index);
			task.getCurrentSchedule().setStart(project.getStart());
			task.setDuration(fixtureDuration(index, taskCount));
			tasks.add(task);
			if (index > 1 && index <= 10) {
				DependencyService.getInstance().newDependency(tasks.get(index - 2), task, DependencyType.FS, 0L, project);
			}
		}
		project.recalculate();
		int sequentialDependencyCount = (int) tasks.subList(1, Math.min(10, tasks.size())).stream()
			.filter(task -> task.getPredecessorList().size() == 1).count();
		int independentTaskCount = (int) tasks.subList(Math.min(10, tasks.size()), tasks.size()).stream()
			.filter(task -> task.getPredecessorList().isEmpty()).count();
		Fixture[] fixture = new Fixture[1];
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()), "gui-task-gantt-grid-test", null);
			SpreadSheetUtils.setFieldsAndContext(sheet, cache, SpreadSheetCategories.taskSpreadsheetCategory, "Spreadsheet.Task.entry", true);
			gantt = new Gantt(project, "Gantt");
			gantt.setCache(cache);
			gantt.setCoord(new CoordinatesConverter(project));
			gantt.setBarStyles((BarStyles) Dictionary.get(BarStyles.category, "standard"));
			gantt.updateSize();
			fixture[0] = new Fixture(sheet, gantt, project, tasks.size(), sequentialDependencyCount, independentTaskCount);
		});
		return fixture[0];
	}

	@Test
	void physicalTaskTableDurationEditDoesNotPanGanttViewport() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(1);
		showFixture(fixture);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(35);
		GraphicNode node = (GraphicNode) fixture.gantt.getModel().getCache().getElementAt(0);
		NormalTask task = (NormalTask) node.getNode().getImpl();
		int durationColumn = findColumn(fixture.sheet, "Field.duration");
		SwingUtilities.invokeAndWait(() -> {
			frame.toFront();
			frame.requestFocus();
			fixture.sheet.changeSelection(fixture.sheet.getCache().getRowAt(node), durationColumn, false, false);
		});
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.gantt.isShowing(),
			"task table and Gantt must be visible before duration input");
		JScrollPane pane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, fixture.gantt);
		assertTrue(pane != null, "Gantt must be hosted by a scroll pane");
		Point viewportBefore = pane.getViewport().getViewPosition();
		BarGeometry before = barGeometry(fixture.gantt, task);
		editCellPhysically(robot, fixture.sheet, fixture.sheet.getCache().getRowAt(node), durationColumn, "10");
		assertEquals(10L * CalendarOption.getInstance().getMillisPerDay(), task.getRawDuration(),
			"the task-table physical duration edit must commit the complete value");
		GuiAcceptanceSupport.await(() -> task.getRawDuration() == 10L * CalendarOption.getInstance().getMillisPerDay(),
			"duration edit did not reach the task model");
		SwingUtilities.invokeAndWait(fixture.gantt::updateSize);
		Point viewportAfter = pane.getViewport().getViewPosition();
		BarGeometry after = barGeometry(fixture.gantt, task);
		assertEquals(viewportBefore, viewportAfter,
			"a task-table duration commit must not move the Gantt timescale viewport");
		assertEquals(before.startX(), after.startX(),
			"the duration edit must preserve the task start bar position");
		assertTrue(after.width() > before.width(),
			"the committed duration must change only the planned bar width, not the viewport");
	}

	@Test
	void physicalTaskTableDateEditRepositionsBarWithoutPanningGanttViewport() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		Fixture fixture = createFixture(1);
		showFixture(fixture);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(35);
		GraphicNode node = (GraphicNode) fixture.gantt.getModel().getCache().getElementAt(0);
		NormalTask task = (NormalTask) node.getNode().getImpl();
		int startColumn = findColumn(fixture.sheet, "Field.start");
		JScrollPane pane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, fixture.gantt);
		assertTrue(pane != null, "Gantt must be hosted by a scroll pane");
		GuiAcceptanceSupport.await(() -> fixture.sheet.isShowing() && fixture.gantt.isShowing(),
			"task table and Gantt must be visible before date input");
		Point viewportBefore = pane.getViewport().getViewPosition();
		long originalStart = task.getStart();
		Calendar expectedStart = Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT"));
		expectedStart.setTimeInMillis(originalStart);
		expectedStart.add(Calendar.DAY_OF_MONTH, 3);
		// The standard project calendar does not schedule task starts on weekends.
		// Keep the physical edit target on a working weekday so this test verifies
		// date entry and viewport behavior instead of weekend scheduling policy.
		while (expectedStart.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
				|| expectedStart.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
			expectedStart.add(Calendar.DAY_OF_MONTH, 1);
		}
		String inputDate = String.format(java.util.Locale.ROOT, "%d/%d/%d",
			expectedStart.get(Calendar.YEAR), expectedStart.get(Calendar.MONTH) + 1,
			expectedStart.get(Calendar.DAY_OF_MONTH));
		editCellPhysically(robot, fixture.sheet, fixture.sheet.getCache().getRowAt(node), startColumn, inputDate);
		Calendar committed = Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT"));
		committed.setTimeInMillis(task.getStart());
		assertEquals(expectedStart.get(Calendar.YEAR), committed.get(Calendar.YEAR), "date input must commit the year");
		assertEquals(expectedStart.get(Calendar.MONTH), committed.get(Calendar.MONTH), "date input must commit the month");
		assertEquals(expectedStart.get(Calendar.DAY_OF_MONTH), committed.get(Calendar.DAY_OF_MONTH),
			"date input must commit the day");
		assertTrue(task.getStart() != originalStart, "a valid date input must change the task start");
		Point viewportAfter = pane.getViewport().getViewPosition();
		assertEquals(viewportBefore, viewportAfter,
			"a task-table date commit must not move the Gantt timescale viewport");
	}

	private static void assertBareDurationFieldConversion() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("duration-field-conversion-test", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		FieldContext context = new FieldContext();
		context.setTaskSheetUpdate(true);

		Duration parsedText = (Duration) DurationFormat.getInstance().parseObject("10");
		assertNotNull(parsedText, "the duration parser must accept the same bare numeric input as the task table");
		assertEquals(TimeUnit.DAYS, Duration.getType(parsedText.getEncodedMillis()),
			"the configured bare numeric duration input is interpreted in the current entry unit");
		Configuration.getFieldFromId("Field.duration").setValue(task, task, "10", context);

		assertEquals(Duration.millis(parsedText.getEncodedMillis()), Duration.millis(task.getRawDuration()));
		assertEquals(TimeUnit.NONE, Duration.getType(task.getRawDuration()),
			"the schedule workflow stores entered duration as unitless milliseconds before MPO normalization");
	}

	@Test
	void physicalTaskTableDurationEditRoundTripsThroughMpo() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "A desktop session is required for Robot acceptance coverage.");
		assertBareDurationFieldConversion();
		Fixture fixture = createFixture(1);
		showFixture(fixture);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(35);
		GraphicNode node = (GraphicNode) fixture.gantt.getModel().getCache().getElementAt(0);
		editCellPhysically(robot, fixture.sheet, fixture.sheet.getCache().getRowAt(node),
			findColumn(fixture.sheet, "Field.duration"), "10");
		NormalTask edited = fixture.project.getTasks().stream()
			.filter(value -> value instanceof NormalTask && "Sequential 1".equals(((NormalTask) value).getName()))
			.map(value -> (NormalTask) value).findFirst().orElseThrow();
		assertEquals(10L * CalendarOption.getInstance().getMillisPerDay(), Duration.millis(edited.getRawDuration()));
		assertEquals(TimeUnit.NONE, Duration.getType(edited.getRawDuration()),
			"the live schedule stores the entered duration as unitless milliseconds");
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(fixture.project, saved),
			"MPO save rejected the physical duration edit");
		Project reloaded = new MpoFileImporter().loadProject(new ByteArrayInputStream(saved.toByteArray()));
		NormalTask restored = reloaded.getTasks().stream()
			.filter(value -> value instanceof NormalTask && "Sequential 1".equals(((NormalTask) value).getName()))
			.map(value -> (NormalTask) value).findFirst().orElseThrow();
		assertEquals(10L * CalendarOption.getInstance().getMillisPerDay(), Duration.millis(restored.getRawDuration()),
			"MPO reload lost the physical duration value");
		assertEquals(TimeUnit.DAYS, Duration.getType(restored.getRawDuration()),
			"MPO reload lost the physical duration unit");
	}

	/** Sheet origin and header height on screen (failure diagnostics). */
	private static String sheetLayout(SpreadSheet sheet) {
		String[] layout = new String[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
				StringBuilder columns = new StringBuilder();
				for (int index = 0; index < sheet.getColumnModel().getColumnCount(); index++) {
					var tableColumn = sheet.getColumnModel().getColumn(index);
					columns.append(tableColumn.getIdentifier() instanceof com.microproject.field.Field field
						? field.getId().replace("Field.", "") : "?").append('=').append(tableColumn.getWidth()).append(' ');
				}
				layout[0] = "sheetOnScreen=" + sheet.getLocationOnScreen()
					+ " header=" + sheet.getTableHeader().getHeight() + " headerPreferred="
					+ sheet.getTableHeader().getPreferredSize().height + " visible=" + sheet.getVisibleRect()
					+ " columns=[" + columns.toString().trim() + "]";
			});
		} catch (Exception exception) {
			return "unavailable: " + exception;
		}
		return layout[0];
	}

	private static void editCellPhysically(Robot robot, SpreadSheet sheet, int row, int column, String value) throws Exception {
		Rectangle bounds = new Rectangle();
		// A user scrolls a cell into view before clicking it; at 150% the narrow
		// task-table pane does not show every column.
		SwingUtilities.invokeAndWait(() -> sheet.scrollRectToVisible(sheet.getCellRect(row, column, true)));
		robot.waitForIdle();
		SwingUtilities.invokeAndWait(() -> {
			Rectangle cell = sheet.getCellRect(row, column, true);
			Point location = sheet.getLocationOnScreen();
			bounds.setBounds(location.x + cell.x, location.y + cell.y, cell.width, cell.height);
		});
		String layoutBeforeClick = sheetLayout(sheet);
		robot.mouseMove(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
		try {
			GuiAcceptanceSupport.await(sheet::isFocusOwner, "duration cell did not receive focus");
		} catch (AssertionError failure) {
			throw new AssertionError(failure.getMessage() + "; clicked=" + bounds + " before: " + layoutBeforeClick
				+ " after: " + sheetLayout(sheet) + " focusOwner="
				+ java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(), failure);
		}
		SwingUtilities.invokeAndWait(() -> assertTrue(sheet.editCellAt(row, column, null),
			"duration cell editor did not start"));
		GuiAcceptanceSupport.await(sheet::isEditing, "duration editor did not start");
		SwingUtilities.invokeAndWait(() -> sheet.getEditorComponent().requestFocusInWindow());
		GuiAcceptanceSupport.await(() -> sheet.getEditorComponent() != null && sheet.getEditorComponent().isFocusOwner(),
			"duration editor did not receive focus");
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		for (char character : value.toCharArray()) {
			int keyCode = KeyEvent.getExtendedKeyCodeForChar(character);
			robot.keyPress(keyCode);
			robot.keyRelease(keyCode);
		}
		assertEquals(value, activeEditorText(sheet), "physical duration input must reach the editor");
		robot.keyPress(KeyEvent.VK_ENTER);
		robot.keyRelease(KeyEvent.VK_ENTER);
		GuiAcceptanceSupport.await(() -> !sheet.isEditing(), "duration edit did not commit");
	}

	private static boolean fieldLayoutMatches(SpreadSheet sheet, int expectedFields, String fieldId, boolean contains)
		throws AssertionError {
		boolean[] matches = new boolean[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
				boolean found = sheet.getFieldArray().stream().anyMatch(field -> fieldId.equals(field.getId()));
				matches[0] = sheet.getFieldArray().size() == expectedFields
					&& sheet.getColumnCount() + 1 == expectedFields && found == contains;
			});
		} catch (Exception exception) {
			throw new AssertionError("Could not inspect the task-table layout on the EDT", exception);
		}
		return matches[0];
	}

	private static boolean hasVisibleColumns(SpreadSheet sheet, int minimumColumns) throws AssertionError {
		boolean[] matches = new boolean[1];
		try {
			SwingUtilities.invokeAndWait(() -> matches[0] = sheet.isShowing() && sheet.getColumnCount() > minimumColumns);
		} catch (Exception exception) {
			throw new AssertionError("Could not inspect task-table visibility on the EDT", exception);
		}
		return matches[0];
	}

	private static String activeEditorText(SpreadSheet sheet) throws Exception {
		String[] text = new String[1];
		SwingUtilities.invokeAndWait(() -> {
			if (sheet.getEditorComponent() instanceof JTextComponent component) text[0] = component.getText();
			else if (sheet.getEditorComponent() instanceof DateEditor.ExtDateField date) text[0] = date.getTextField().getText();
		});
		return text[0];
	}

	private static int findColumn(SpreadSheet sheet, String fieldId) {
		com.microproject.pm.graphic.spreadsheet.SpreadSheetModel model =
			(com.microproject.pm.graphic.spreadsheet.SpreadSheetModel) sheet.getModel();
		for (int column = 0; column < model.getColumnCount(); column++) {
			Field field = model.getFieldInColumn(column);
			if (field != null && fieldId.equals(field.getId())) return sheet.convertColumnIndexToView(column);
		}
		throw new IllegalArgumentException("Missing field: " + fieldId);
	}

	private static BarGeometry barGeometry(Gantt chart, NormalTask task) throws Exception {
		double[] values = new double[2];
		SwingUtilities.invokeAndWait(() -> {
			values[0] = chart.getCoord().toX(task.getStart());
			values[1] = chart.getCoord().toX(task.getEnd()) - values[0];
		});
		return new BarGeometry((int) Math.round(values[0]), (int) Math.round(values[1]));
	}

	private record BarGeometry(int startX, int width) { }

	private static long fixtureDuration(int index, int taskCount) {
		long day = CalendarOption.getInstance().getMillisPerDay();
		if (taskCount == 1) return day;
		return switch (index % 4) {
			case 0 -> day * 5;
			case 1 -> day / 2;
			case 2 -> day;
			default -> day * 3;
		};
	}

	private static int firstVisibleRow(JScrollPane pane, int rowHeight) {
		return pane.getViewport().getViewPosition().y / rowHeight;
	}

	private static List<String> wheelModelSnapshot(Fixture fixture) throws Exception {
		List<String> snapshot = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			for (Object task : fixture.project.getTaskList()) {
				NormalTask normal = (NormalTask) task;
				snapshot.add(normal.getName() + "|" + normal.getStart() + "|" + normal.getRawDuration() + "|"
					+ normal.getPredecessorList().size());
			}
		});
		return snapshot;
	}

	private static List<Integer> wheelSelectionSnapshot(Fixture fixture) throws Exception {
		List<Integer> rows = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			for (int row : fixture.sheet.getSelectedRows()) rows.add(row);
		});
		return rows;
	}

	private record Fixture(SpreadSheet sheet, Gantt gantt, Project project, int taskCount, int sequentialDependencyCount,
		int independentTaskCount) { }

	private record UndoSnapshot(List<String> editNames, boolean canUndo, boolean canRedo) {
		private static UndoSnapshot capture(Project project) {
			List<String> editNames = project.getUndoController().getEditNames();
			return new UndoSnapshot(editNames == null ? List.of() : List.copyOf(editNames),
				project.getUndoController().canUndo(), project.getUndoController().canRedo());
		}
	}

	private record TaskSnapshot(long uniqueId, String name, long start, long duration, double percentComplete) {
		private static List<TaskSnapshot> capture(Project project) {
			return project.getTasks().stream()
				.map(task -> new TaskSnapshot(task.getUniqueId(), task.getName(), task.getStart(), task.getDuration(),
					task.getPercentComplete()))
				.toList();
		}
	}
}

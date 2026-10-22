/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.awt.event.InputEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.configuration.FieldDictionary;
import com.microproject.datatype.Duration;
import com.microproject.datatype.DurationFormat;
import com.microproject.datatype.DurationDefaults;
import com.microproject.datatype.TimeUnit;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.configuration.Dictionary;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.frames.MainRibbonFrame;
import com.microproject.menu.MenuActionConstants;
import com.microproject.grouping.core.Node;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.RevisionedProjectionIndex;
import com.microproject.pm.graphic.model.transform.NodeCacheTransformer;
import com.microproject.grouping.core.transform.ViewTransformer;
import com.microproject.grouping.core.transform.sorting.TaskSortSpec;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.graphic.spreadsheet.renderer.NameCellComponent;
import com.microproject.ribbon.RibbonCommandResult;
import com.microproject.pm.graphic.spreadsheet.editor.DateEditor;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.task.Task;
import com.microproject.exchange.MpoFileImporter;
import com.microproject.testsupport.GuiAcceptanceSupport;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.undo.EditListener;
import com.microproject.undo.EditSupport;
import com.microproject.util.DateTime;

/** U-26 physical multi-character spreadsheet input transaction coverage. */
class U26SpreadsheetInputTransactionGuiAcceptanceTest {
	private MainRibbonFrame frame;
	private GraphicManager graphicManager;
	private JTabbedPane tabs;
	private String previousUiDebug;
	private Logger undoTraceLogger;
	private Level previousUndoTraceLevel;
	private Handler undoTraceHandler;
	private final ConcurrentLinkedQueue<String> undoTrace = new ConcurrentLinkedQueue<>();
	private EditListener undoHistoryListener;
	private EditSupport undoHistorySupport;
	private final ConcurrentLinkedQueue<String> undoHistoryTrace = new ConcurrentLinkedQueue<>();

	@AfterEach
	void closeWindow() throws Exception {
		if (frame != null) SwingUtilities.invokeAndWait(() -> {
			if (graphicManager != null) graphicManager.cleanUp();
			frame.dispose();
			frame = null;
			graphicManager = null;
		});
		if (undoTraceLogger != null) undoTraceLogger.removeHandler(undoTraceHandler);
		if (undoHistoryListener != null && undoHistorySupport != null)
			undoHistorySupport.removeEditListener(undoHistoryListener);
		if (undoTraceLogger != null) undoTraceLogger.setLevel(previousUndoTraceLevel);
		if (previousUiDebug == null) System.clearProperty("microproject.ui.debug");
		else System.setProperty("microproject.ui.debug", previousUiDebug);
		undoTraceLogger = null;
		undoTraceHandler = null;
	}

	@Test
	void robotTypesDurationAndPercentAsOneInputTransaction() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for U-26 physical input coverage.");
		Fixture fixture = createFixture();
		showFixture(fixture);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(35);
		SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(0));
		activate(fixture.entrySheet, robot);
		editWithPhysicalKeys(robot, fixture.entrySheet, fixture.entryRow, fixture.durationColumn, "10", "8 days");
		assertEquals(10L * 60L * 60L * 1000L, Duration.millis(fixture.task.getRawDuration()),
			"physical bare duration input must use the owning project's default hours");
		// Task schedules normalize entered durations to physical milliseconds; parsing
		// the bare input with this project's Hours setting records that per-task unit.
		assertEquals(TimeUnit.HOURS, fixture.task.getProject().getDurationDefaults().durationEnteredIn(),
			"the owning project must retain Hours as its duration-entry and display unit");
		assertEquals(DurationFormat.getProjectInstance().format(fixture.task.getDurationForDisplay(),
			fixture.task.getProject().getDurationDefaults()), durationText(fixture.task,
			((SpreadSheetModel) fixture.entrySheet.getModel()).getFieldContext()),
			"the committed duration cell must render using the owning project's Hours default");
		NormalTask successor = fixture.task.getProject().createScriptedTask();
		successor.setName("U-26 duration successor");
		successor.setDuration(Duration.getInstance(8.0D, TimeUnit.HOURS,
			fixture.task.getProject().getDurationDefaults()));
		DependencyService.getInstance().newDependency(fixture.task, successor, DependencyType.FS, 0L, this);
		fixture.task.getProject().recalculate();
		SwingUtilities.invokeAndWait(() -> {
			((SpreadSheetModel) fixture.entrySheet.getModel()).getCache().update();
			fixture.entrySheet.revalidate();
			fixture.gantt.updateSize();
		});
		long predecessorStartBeforeDurationTransaction = fixture.task.getStart();
		long predecessorFinishBeforeDurationTransaction = fixture.task.getEnd();
		long successorStartBeforeDurationTransaction = successor.getStart();
		long successorFinishBeforeDurationTransaction = successor.getEnd();
		String predecessorDurationBeforeTransaction = renderedCellText(fixture.entrySheet,
			findTaskRow(fixture.entrySheet, fixture.task), fixture.durationColumn);
		BarGeometry predecessorBarBeforeDurationTransaction = ganttBarGeometry(fixture, fixture.task);
		BarGeometry successorBarBeforeDurationTransaction = ganttBarGeometry(fixture, successor);
		int successorRow = findTaskRow(fixture.entrySheet, successor);
		editWithPhysicalKeys(robot, fixture.entrySheet, fixture.entryRow, fixture.durationColumn, "12", "10 hours");
		long predecessorFinishAfterDurationTransaction = fixture.task.getEnd();
		long successorStartAfterDurationTransaction = successor.getStart();
		long successorFinishAfterDurationTransaction = successor.getEnd();
		assertEquals(predecessorStartBeforeDurationTransaction, fixture.task.getStart());
		assertTrue(predecessorFinishAfterDurationTransaction > predecessorFinishBeforeDurationTransaction,
			"physical duration commit must move the predecessor finish");
		assertTrue(successorStartAfterDurationTransaction > successorStartBeforeDurationTransaction,
			"physical duration commit must recalculate the dependent successor");
		assertTrue(successorFinishAfterDurationTransaction > successorFinishBeforeDurationTransaction);
		String predecessorDurationAfterTransaction = renderedCellText(fixture.entrySheet,
			findTaskRow(fixture.entrySheet, fixture.task), fixture.durationColumn);
		assertEquals("1.5 days", predecessorDurationAfterTransaction,
			"duration cell must redraw 12 hours using the project's six-hour day");
		int successorStartColumn = findColumn(fixture.entrySheet, "Field.start");
		Field successorStartField = ((SpreadSheetModel) fixture.entrySheet.getModel()).getFieldInColumn(
			fixture.entrySheet.convertColumnIndexToModel(successorStartColumn));
		assertEquals(successorStartField.getText(successor,
			((SpreadSheetModel) fixture.entrySheet.getModel()).getFieldContext()),
			renderedCellText(fixture.entrySheet, successorRow, successorStartColumn),
			"successor Start cell must redraw after dependent scheduling");
		assertTrue(!ganttBarGeometry(fixture, fixture.task).equals(predecessorBarBeforeDurationTransaction),
			"predecessor Gantt bar must redraw for its committed duration");
		assertTrue(!ganttBarGeometry(fixture, successor).equals(successorBarBeforeDurationTransaction),
			"successor Gantt bar must redraw for its recalculated schedule");
		assertFalse(fixture.entrySheet.isEditing(), "physical duration commit must finish the cell editor before Undo");
		beginUndoTraceCapture();
		undoHistoryListener = new EditListener() {
			@Override public void editPosted(javax.swing.undo.UndoableEdit edit) {
				undoHistoryTrace.add("posted=" + edit.getPresentationName());
			}
			@Override public void historySettled() { undoHistoryTrace.add("history-settled"); }
		};
		undoHistorySupport = fixture.task.getProject().getUndoController().getEditSupport();
		undoHistorySupport.addEditListener(undoHistoryListener);
		physicalUndoRedo(robot, KeyEvent.VK_Z);
		assertEquals(predecessorFinishBeforeDurationTransaction, fixture.task.getEnd(),
			"physical Ctrl+Z must restore the predecessor model schedule");
		assertEquals(successorStartBeforeDurationTransaction, successor.getStart(),
			"physical Ctrl+Z must restore the successor model schedule");
		assertEquals(successorFinishBeforeDurationTransaction, successor.getEnd());
		assertEquals(predecessorDurationBeforeTransaction,
			renderedCellText(fixture.entrySheet, findTaskRow(fixture.entrySheet, fixture.task), fixture.durationColumn),
			"physical Ctrl+Z must redraw the predecessor duration");
		assertEquals(successorStartField.getText(successor,
			((SpreadSheetModel) fixture.entrySheet.getModel()).getFieldContext()),
			renderedCellText(fixture.entrySheet, findTaskRow(fixture.entrySheet, successor), successorStartColumn),
			"physical Ctrl+Z must redraw the successor schedule");
		assertEquals(predecessorBarBeforeDurationTransaction, ganttBarGeometry(fixture, fixture.task));
		assertEquals(successorBarBeforeDurationTransaction, ganttBarGeometry(fixture, successor));
		assertTrue(fixture.task.getProject().getUndoController().canRedo(),
			"Undo must leave this edit available for physical Redo; pending="
				+ fixture.task.getProject().getUndoController().getEditSupport().hasPendingHistory()
				+ " edits=" + fixture.task.getProject().getUndoController().getEditNames()
				+ " redoName=" + fixture.task.getProject().getUndoController().getRedoName()
				+ " trace=" + List.copyOf(undoTrace)
				+ " historyTrace=" + List.copyOf(undoHistoryTrace));
		activate(fixture.entrySheet, robot);
		physicalUndoRedo(robot, KeyEvent.VK_Y);
		GuiAcceptanceSupport.await(() -> fixture.task.getEnd() == predecessorFinishAfterDurationTransaction,
			"physical Ctrl+Y did not restore the predecessor finish");
		assertEquals(predecessorFinishAfterDurationTransaction, fixture.task.getEnd(),
			"physical Ctrl+Y must reapply the predecessor model schedule");
		assertEquals(successorStartAfterDurationTransaction, successor.getStart(),
			"physical Ctrl+Y must reapply the successor model schedule");
		assertEquals(successorFinishAfterDurationTransaction, successor.getEnd());
		assertEquals(predecessorDurationAfterTransaction,
			renderedCellText(fixture.entrySheet, findTaskRow(fixture.entrySheet, fixture.task), fixture.durationColumn));
		assertEquals(successorStartField.getText(successor,
			((SpreadSheetModel) fixture.entrySheet.getModel()).getFieldContext()),
			renderedCellText(fixture.entrySheet, findTaskRow(fixture.entrySheet, successor), successorStartColumn));
		assertTrue(!ganttBarGeometry(fixture, fixture.task).equals(predecessorBarBeforeDurationTransaction));
		assertTrue(!ganttBarGeometry(fixture, successor).equals(successorBarBeforeDurationTransaction));
		editWithPhysicalKeys(robot, fixture.entrySheet, fixture.entryRow, fixture.durationColumn, "10", "12 hours");

		editWithPhysicalKeys(robot, fixture.entrySheet, fixture.entryRow, fixture.startColumn, "2026/9/25", null,
			KeyEvent.VK_ENTER, false);
		Calendar committedStart = DateTime.calendarInstance();
		committedStart.setTimeInMillis(fixture.task.getStart());
		int startModelColumn = fixture.entrySheet.convertColumnIndexToModel(fixture.startColumn);
		Field startField = ((SpreadSheetModel) fixture.entrySheet.getModel()).getFieldInColumn(startModelColumn);
		assertEquals(2026, committedStart.get(Calendar.YEAR), "physical date input must commit the year");
		assertEquals(Calendar.SEPTEMBER, committedStart.get(Calendar.MONTH),
			"physical date input must commit the month: " + committedStart.getTime()
				+ " field=" + (startField == null ? null : startField.getId())
				+ " editable=" + fixture.entrySheet.isCellEditable(fixture.entryRow, fixture.startColumn)
				+ " cell=" + ((SpreadSheetModel) fixture.entrySheet.getModel()).getValueAt(fixture.entryRow, startModelColumn));
		assertEquals(25, committedStart.get(Calendar.DAY_OF_MONTH), "physical date input must commit the day");
		assertEquals(DurationFormat.getProjectInstance().format(new Duration(fixture.task.getRemainingDuration()),
			fixture.task.getProject().getDurationDefaults()), remainingDurationText(fixture),
			"0% renderer must show the full remaining duration using project defaults");
		fixture.setProgressBaseline();

		SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(1));
		activate(fixture.trackingSheet, robot);
		List<Long> selectedTaskIdsBeforeTyping = typePercentFromFirstPhysicalKey(robot, fixture.trackingSheet, fixture.trackingRow,
			fixture.percentColumn, "10");
		RibbonCommandResult ribbonOutcome = graphicManager.getLastRibbonCommandResult();
		assertTrue(ribbonOutcome != null, "physical cell edit must publish through the active application GraphicManager");
		assertEquals("SpreadsheetCellEdit", ribbonOutcome.commandId());
		assertEquals(RibbonCommandResult.Status.CHANGED, ribbonOutcome.status());
		assertEquals(selectedTaskIdsBeforeTyping, ribbonOutcome.selectedTaskIds(),
			"the physical cell edit must publish the selection scope observed immediately before typing");
		assertEquals(java.util.List.of(fixture.task.getUniqueId()), ribbonOutcome.affectedTaskIds());
		assertEquals(graphicManager.getTopViewId(), ribbonOutcome.activeViewId(),
			"cell edit outcome must identify the actual document frame's active top view");
		assertEquals(1, selectedTabIndex(), "the tracking tab must remain the active view after field commit");
		assertEquals(0.10d, fixture.task.getPercentComplete(), 0.00001d,
			"physical multi-character percent input must commit the complete value");
		assertEquals("10%", renderedCellText(fixture.trackingSheet, fixture.trackingRow, fixture.percentColumn),
			"the committed progress value must render as 10% in the selected task cell");
		SwingUtilities.invokeAndWait(() -> fixture.task.getProject().getUndoController().undo());
		assertEquals(0.0d, fixture.task.getPercentComplete(), 0.00001d,
			"Undo must restore the progress model value before later progress edits");
		assertEquals("0%", renderedCellText(fixture.trackingSheet, fixture.trackingRow, fixture.percentColumn),
			"Undo must restore the rendered tracking cell value");
		SwingUtilities.invokeAndWait(() -> fixture.task.getProject().getUndoController().redo());
		assertEquals(0.10d, fixture.task.getPercentComplete(), 0.00001d,
			"Redo must restore the progress model value before later progress edits");
		assertEquals("10%", renderedCellText(fixture.trackingSheet, fixture.trackingRow, fixture.percentColumn),
			"Redo must restore the rendered tracking cell value");

		assertTrue(fixture.task.getRemainingDuration() > 0L, "10% progress must leave a positive remaining duration");
		assertEquals(DurationFormat.getProjectInstance().format(new Duration(fixture.task.getRemainingDuration()),
			fixture.task.getProject().getDurationDefaults()), remainingDurationText(fixture),
			"10% renderer must show the intermediate remaining duration");
		GanttGeometry afterTen = ganttGeometry(fixture);
		assertEquals(fixture.progressViewportBefore(), afterTen.viewport(),
			"10% task-table input must not move the Gantt timescale viewport");
		assertEquals(fixture.progressBarBefore(), afterTen.bar(),
			"10% task-table input must not change the planned bar geometry");
		editWithPhysicalKeys(robot, fixture.trackingSheet, fixture.trackingRow, fixture.percentColumn, "50");
		assertEquals(0.50d, fixture.task.getPercentComplete(), 0.00001d,
			"physical intermediate percent input must commit 50%");
		assertTrue(fixture.task.getRemainingDuration() > 0L, "50% progress must leave a positive remaining duration");
		GanttGeometry afterHalf = ganttGeometry(fixture);
		assertEquals(fixture.progressViewportBefore(), afterHalf.viewport(),
			"50% task-table input must not move the Gantt timescale viewport");
		assertEquals(fixture.progressBarBefore(), afterHalf.bar(),
			"50% task-table input must not change the planned bar geometry");
		editWithPhysicalKeys(robot, fixture.trackingSheet, fixture.trackingRow, fixture.percentColumn, "99");
		assertEquals(0.99d, fixture.task.getPercentComplete(), 0.00001d,
			"physical near-complete percent input must commit 99%");
		assertTrue(fixture.task.getRemainingDuration() > 0L, "99% progress must leave a positive remaining duration");
		GanttGeometry afterNearComplete = ganttGeometry(fixture);
		assertEquals(fixture.progressViewportBefore(), afterNearComplete.viewport(),
			"99% task-table input must not move the Gantt timescale viewport");
		assertEquals(fixture.progressBarBefore(), afterNearComplete.bar(),
			"99% task-table input must not change the planned bar geometry");
		editWithPhysicalKeys(robot, fixture.trackingSheet, fixture.trackingRow, fixture.percentColumn, "100");
		assertEquals(1.0d, fixture.task.getPercentComplete(), 0.00001d,
			"physical 100% input must commit the complete value");
		assertEquals(DurationFormat.getProjectInstance().format(new Duration(fixture.task.getRemainingDuration()),
			fixture.task.getProject().getDurationDefaults()), remainingDurationText(fixture),
			"100% renderer must show zero remaining duration using project defaults");
		GanttGeometry afterComplete = ganttGeometry(fixture);
		assertEquals(fixture.progressViewportBefore(), afterComplete.viewport(),
			"100% task-table input must not move the Gantt timescale viewport");
		assertEquals(fixture.progressBarBefore(), afterComplete.bar(),
			"100% task-table input must not change the planned bar geometry");

		SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(0));
		activate(fixture.entrySheet, robot);
		editWithPhysicalKeys(robot, fixture.entrySheet, fixture.entryRow, fixture.durationColumn, "1 day", "10 hours");
		assertEquals(6L * 60L * 60L * 1000L, Duration.millis(fixture.task.getRawDuration()),
			"with a six-hour project day, physical 1 day input must store six hours");
		assertEquals(TimeUnit.DAYS, fixture.task.getDurationDisplayUnit());
		assertEquals("1 day", durationText(fixture.task,
			((SpreadSheetModel) fixture.entrySheet.getModel()).getFieldContext()));
		editWithPhysicalKeys(robot, fixture.entrySheet, fixture.entryRow, fixture.durationColumn, "6 hours", "1 day");
		assertEquals(6L * 60L * 60L * 1000L, Duration.millis(fixture.task.getRawDuration()),
			"physical 6 hours input must match 1 day on this project's calendar");
		assertEquals(TimeUnit.HOURS, fixture.task.getDurationDisplayUnit());
		assertEquals("6 hours", durationText(fixture.task,
			((SpreadSheetModel) fixture.entrySheet.getModel()).getFieldContext()));
		editWithPhysicalKeys(robot, fixture.entrySheet, fixture.entryRow, fixture.durationColumn, "10", "6 hours");
		assertEquals(10L * 60L * 60L * 1000L, Duration.millis(fixture.task.getRawDuration()),
			"restore the bare project-hour duration for the structural save/reload assertions");
		assertEquals(TimeUnit.HOURS, fixture.task.getDurationDisplayUnit());
		SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(1));
		activate(fixture.trackingSheet, robot);
		editWithPhysicalKeys(robot, fixture.trackingSheet, fixture.trackingRow, fixture.percentColumn, "100");
		assertEquals(1.0d, fixture.task.getPercentComplete(), 0.00001d,
			"the final physical progress input must commit 100% before the structural round-trip");
		assertEquals("100%", renderedCellText(fixture.trackingSheet, fixture.trackingRow, fixture.percentColumn),
			"the final physical progress input must render as 100% before the structural round-trip");

		Project project = fixture.task.getProject();
		SwingUtilities.invokeAndWait(() -> {
			NormalTask structuralParent = project.createScriptedTask();
			structuralParent.setName("U-26 structural parent");
			project.setLocalParent(fixture.task, structuralParent);
			assertEquals(structuralParent, fixture.task.getWbsParentTask(), "fixture must make the edited task a child before outdent");
			project.setLocalParent(fixture.task, null);
			assertEquals(null, fixture.task.getWbsParentTask(), "outdent must restore the edited task to the project root");
		});

		ByteArrayOutputStream mpo = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, mpo), "MPO save after structural change must succeed");
		MpoFileImporter reader = new MpoFileImporter();
		reader.setProjectFactory(ProjectFactory.getInstance());
		Project reopened = reader.loadProject(new ByteArrayInputStream(mpo.toByteArray()));
		NormalTask restored = (NormalTask) reopened.findByUniqueId(fixture.task.getUniqueId());
		NormalTask restoredSuccessor = (NormalTask) reopened.findByUniqueId(successor.getUniqueId());
		assertEquals(fixture.task.getUniqueId(), restored.getUniqueId(),
			"MPO reopen must restore the same physically edited task identity");
		assertEquals(null, restored.getWbsParentTask(), "the edited task must remain at the project root after reload");
		assertEquals(10L * 60L * 60L * 1000L, Duration.millis(restored.getRawDuration()),
			"the physically entered project-unit duration must survive a structural edit and MPO reload");
		assertEquals(1.0d, restored.getPercentComplete(), 0.00001d,
			"the physically entered 100% progress must survive a structural edit and MPO reload");
		assertEquals(fixture.task.getStart(), restored.getStart(),
			"the physically entered start date must survive a structural edit and MPO reload");
		assertEquals(fixture.task.getEnd(), restored.getEnd(),
			"the physically entered duration's finish must survive MPO reload");
		assertTrue(restored.getUniqueIdSuccessors().contains(Long.toString(restoredSuccessor.getUniqueId())),
			"MPO reload must retain the duration task's dependent successor link");
		assertTrue(restoredSuccessor.getUniqueIdPredecessors().contains(Long.toString(restored.getUniqueId())),
			"MPO reload must retain the successor's predecessor link");
		assertEquals(successor.getStart(), restoredSuccessor.getStart(),
			"MPO reload must retain the dependent successor's recalculated Start");
		assertEquals(successor.getEnd(), restoredSuccessor.getEnd(),
			"MPO reload must retain the dependent successor's recalculated Finish");
		assertEquals(0L, restored.getRemainingDuration(),
			"the 100% task's remaining duration must survive a structural edit and MPO reload");
		assertEquals(new DurationDefaults(360.0D, 1800.0D, 20.0D, TimeUnit.HOURS, TimeUnit.HOURS),
			reopened.getDurationDefaults(), "MPO reopen must retain the project's duration defaults");
		FieldContext reopenedContext = new FieldContext();
		reopenedContext.setDurationDefaults(reopened.getDurationDefaults());
		assertEquals(TimeUnit.HOURS, restored.getDurationDisplayUnit(),
			"MPO task format must restore the explicit per-task duration unit");
		assertEquals(DurationFormat.getProjectInstance().format(restored.getDurationForDisplay(),
			reopened.getDurationDefaults()), durationText(restored, reopenedContext),
			"MPO reopen must retain the project duration display unit for the task cell");
		assertEquals("0 hours", FieldDictionary.getInstance().getFieldFromId("Field.remainingDuration")
			.getText(restored, reopenedContext), "MPO reopen must render zero remaining duration using project defaults");
		assertEquals("100%", FieldDictionary.getInstance().getFieldFromId("Field.percentComplete")
			.getText(restored, reopenedContext), "MPO reopen must render the committed progress value as 100%");

	}

	@Test
	void robotFirstKeyEditsStableTaskThroughFilteredGroupedSortedProjection() throws Exception {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
			"A desktop session is required for U-26 physical input coverage.");
		Fixture fixture = createFixture();
		Project project = fixture.task.getProject();
		NormalTask target = fixture.task;
		target.setName("Alpha short");
		target.setDuration(Duration.getInstance(1.0D, TimeUnit.DAYS, project.getDurationDefaults()));
		NormalTask zulu = project.createScriptedTask();
		zulu.setName("Zulu short");
		zulu.setDuration(Duration.getInstance(1.0D, TimeUnit.DAYS, project.getDurationDefaults()));
		NormalTask bravo = project.createScriptedTask();
		bravo.setName("Bravo long");
		bravo.setDuration(Duration.getInstance(2.0D, TimeUnit.DAYS, project.getDurationDefaults()));
		NormalTask completed = project.createScriptedTask();
		completed.setName("Hidden complete");
		completed.setDuration(Duration.getInstance(1.0D, TimeUnit.DAYS, project.getDurationDefaults()));
		completed.setPercentComplete(1.0d);
		List<Task> modelOrder = List.copyOf(project.getTaskList());
		SwingUtilities.invokeAndWait(() -> {
			configureProjection(((SpreadSheetModel) fixture.entrySheet.getModel()).getCache());
			configureProjection(((SpreadSheetModel) fixture.trackingSheet.getModel()).getCache());
			((SpreadSheetModel) fixture.entrySheet.getModel()).getCache().update();
			((SpreadSheetModel) fixture.trackingSheet.getModel()).getCache().update();
			fixture.entrySheet.revalidate();
			fixture.trackingSheet.revalidate();
			fixture.gantt.setCache(((SpreadSheetModel) fixture.entrySheet.getModel()).getCache());
			fixture.gantt.updateSize();
		});
		showFixture(fixture);
		Robot robot = new com.microproject.testsupport.GuiRobot();
		robot.setAutoDelay(35);
		SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(1));
		activate(fixture.trackingSheet, robot);
		int targetRow = findTaskRow(fixture.trackingSheet, target);
		List<String> projectedNames = visibleTaskNames(((SpreadSheetModel) fixture.trackingSheet.getModel()).getCache());
		assertEquals(List.of("Bravo long", "Alpha short", "Zulu short"), projectedNames,
			"the physical fixture must keep duration buckets and sort names within each bucket");
		assertTrue(!visibleTaskNames(((SpreadSheetModel) fixture.trackingSheet.getModel()).getCache())
			.contains("Hidden complete"), "the incomplete-task filter must hide completed tasks");
		Node targetNode = (Node) project.getTaskModel().search(target);
		long stableId = target.getUniqueId();
		long originalDuration = target.getDuration();
		long originalStart = target.getStart();
		String originalName = target.getName();
		long durationBeforeProjectionStartEdit = target.getDuration();
		double percentBeforeProjectionStartEdit = target.getPercentComplete();
		long zuluStartBeforeProjectionEdit = zulu.getStart();
		long bravoStartBeforeProjectionEdit = bravo.getStart();
		int startColumn = findColumn(fixture.entrySheet, "Field.start");
		int visibleStartRow = findTaskRow(fixture.entrySheet, target);
		assertEquals(stableId, taskIdAtVisibleRow(fixture.entrySheet, visibleStartRow),
			"the physical Start edit target must be resolved from its visible row stable ID");
		SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(0));
		activate(fixture.entrySheet, robot);
		editWithPhysicalKeys(robot, fixture.entrySheet, visibleStartRow, startColumn, "2026/9/25", null,
			KeyEvent.VK_TAB, true);
		Calendar committedStart = DateTime.calendarInstance();
		committedStart.setTimeInMillis(target.getStart());
		assertEquals(2026, committedStart.get(Calendar.YEAR));
		assertEquals(Calendar.SEPTEMBER, committedStart.get(Calendar.MONTH));
		assertEquals(25, committedStart.get(Calendar.DAY_OF_MONTH),
			"physical Start input must commit the exact date to the stable projected task");
		assertEquals(stableId, selectedStableTaskId(fixture.entrySheet),
			"the selected task identity must survive redraw after the Start edit");
		int committedStartRow = findTaskRow(fixture.entrySheet, target);
		assertEquals(stableId, taskIdAtVisibleRow(fixture.entrySheet, committedStartRow));
		int startModelColumn = fixture.entrySheet.convertColumnIndexToModel(startColumn);
		Field startField = ((SpreadSheetModel) fixture.entrySheet.getModel()).getFieldInColumn(startModelColumn);
		assertEquals(startField.getText(target,
			((SpreadSheetModel) fixture.entrySheet.getModel()).getFieldContext()),
			renderedCellText(fixture.entrySheet, committedStartRow, startColumn),
			"the committed date must be rendered in the Start cell after redraw");
		int startTableProjectionRow = ((SpreadSheetModel) fixture.entrySheet.getModel()).getCache()
			.getVisibleNodes().getProjectionIndex().rowForNode((GraphicNode)
				((SpreadSheetModel) fixture.entrySheet.getModel()).getCache().getGraphicNode(targetNode));
		int startGanttProjectionRow = fixture.gantt.getModel().getCache().getVisibleNodes().getProjectionIndex()
			.rowForNode((GraphicNode) fixture.gantt.getModel().getCache().getGraphicNode(targetNode));
		assertEquals(committedStartRow, startTableProjectionRow);
		assertEquals(startTableProjectionRow, startGanttProjectionRow,
			"table and Gantt must map the Start-edited task to the same projection row");
		assertEquals(zuluStartBeforeProjectionEdit, zulu.getStart(), "editing Start must preserve unrelated task values");
		assertEquals(bravoStartBeforeProjectionEdit, bravo.getStart(), "editing Start must preserve unrelated task values");
		assertEquals(durationBeforeProjectionStartEdit, target.getDuration());
		assertEquals(percentBeforeProjectionStartEdit, target.getPercentComplete(), 0.00001d);
		assertEquals("Bravo long", bravo.getName());
		assertEquals("Zulu short", zulu.getName());
		assertEquals("Hidden complete", completed.getName());
		SwingUtilities.invokeAndWait(() -> project.getUndoController().undo());
		assertEquals(originalStart, target.getStart(), "Undo must restore the projected task Start");
		assertEquals(startField.getText(target,
			((SpreadSheetModel) fixture.entrySheet.getModel()).getFieldContext()),
			renderedCellText(fixture.entrySheet, findTaskRow(fixture.entrySheet, target), startColumn));
		SwingUtilities.invokeAndWait(() -> project.getUndoController().redo());
		Calendar redoneStart = DateTime.calendarInstance();
		redoneStart.setTimeInMillis(target.getStart());
		assertEquals(25, redoneStart.get(Calendar.DAY_OF_MONTH), "Redo must restore the projected Start date");
		long committedProjectionStart = target.getStart();
		SwingUtilities.invokeAndWait(() -> tabs.setSelectedIndex(1));
		activate(fixture.trackingSheet, robot);

		List<Long> selectedBefore = typePercentFromFirstPhysicalKey(robot, fixture.trackingSheet, targetRow,
			fixture.percentColumn, "25", KeyEvent.VK_TAB, true);
		assertEquals(List.of(stableId), selectedBefore,
			"the physical click on the sorted visible row must resolve to the intended stable task");
		RibbonCommandResult outcome = graphicManager.getLastRibbonCommandResult();
		assertEquals("SpreadsheetCellEdit", outcome.commandId());
		assertEquals(RibbonCommandResult.Status.CHANGED, outcome.status());
		assertEquals(List.of(stableId), outcome.selectedTaskIds());
		assertEquals(List.of(stableId), outcome.affectedTaskIds());
		assertEquals(graphicManager.getTopViewId(), outcome.activeViewId());
		assertEquals(0.25d, target.getPercentComplete(), 0.00001d);
		assertEquals(originalName, target.getName());
		assertEquals(originalDuration, target.getDuration());
		assertEquals(committedProjectionStart, target.getStart(),
			"progress edit must preserve the Start date committed before work began");
		assertEquals(modelOrder, project.getTaskList(), "view projection and input must preserve project outline order");
		assertEquals(List.of("Bravo long", "Alpha short", "Zulu short"), projectedNames,
			"editing progress must leave the visible grouped and sorted task order stable");
		assertEquals(stableId, selectedStableTaskId(fixture.trackingSheet),
			"the selected task identity must survive redraw after commit");
		assertEquals(targetRow, findTaskRow(fixture.trackingSheet, target),
			"the task must remain at its correct sorted table row after redraw");
		int tableProjectionRow = ((SpreadSheetModel) fixture.trackingSheet.getModel()).getCache()
			.getVisibleNodes().getProjectionIndex().rowForNode(
				(GraphicNode) ((SpreadSheetModel) fixture.trackingSheet.getModel()).getCache().getGraphicNode(targetNode));
		int ganttProjectionRow = fixture.gantt.getModel().getCache().getVisibleNodes().getProjectionIndex()
			.rowForNode((GraphicNode) fixture.gantt.getModel().getCache().getGraphicNode(targetNode));
		assertEquals(targetRow, tableProjectionRow, "table row must map through the visible projection");
		assertEquals(tableProjectionRow, ganttProjectionRow,
			"table and Gantt must resolve the stable task to the same visible projection row");
		assertEquals("25%", renderedCellText(fixture.trackingSheet, targetRow, fixture.percentColumn));

		int nameColumn = findColumn(fixture.trackingSheet, "Field.name");
		int visibleRowById = findTaskRow(fixture.trackingSheet, target);
		assertEquals(stableId, taskIdAtVisibleRow(fixture.trackingSheet, visibleRowById),
			"the physical name edit target must be resolved from its visible row stable ID");
		List<Long> nameSelection = typePercentFromFirstPhysicalKey(robot, fixture.trackingSheet,
			visibleRowById, nameColumn, "Aardvark short", KeyEvent.VK_TAB, true);
		assertEquals(List.of(stableId), nameSelection);
		RibbonCommandResult nameOutcome = graphicManager.getLastRibbonCommandResult();
		assertEquals("SpreadsheetCellEdit", nameOutcome.commandId());
		assertEquals(RibbonCommandResult.Status.CHANGED, nameOutcome.status());
		assertEquals(List.of(stableId), nameOutcome.selectedTaskIds());
		assertEquals(List.of(stableId), nameOutcome.affectedTaskIds());
		assertEquals("Aardvark short", target.getName(), "the physical Name edit must commit to the intended task");
		assertEquals(committedProjectionStart, target.getStart(), "the physical Name edit must not change the task start");
		assertEquals(originalDuration, target.getDuration(), "the physical Name edit must not change task duration");
		assertEquals(0.25d, target.getPercentComplete(), 0.00001d,
			"the physical Name edit must not change progress committed by the earlier transaction");
		assertEquals("Aardvark short", renderedCellText(fixture.trackingSheet,
			findTaskRow(fixture.trackingSheet, target), nameColumn));
		assertEquals(stableId, selectedStableTaskId(fixture.trackingSheet),
			"the selected stable task identity must survive the sort reorder after redraw");
		int renamedRow = findTaskRow(fixture.trackingSheet, target);
		assertEquals(renamedRow, fixture.gantt.getModel().getCache().getVisibleNodes().getProjectionIndex()
			.rowForNode((GraphicNode) fixture.gantt.getModel().getCache().getGraphicNode(targetNode)),
			"table and Gantt must remain aligned after the edited name changes sort position");
		assertEquals(List.of("Bravo long", "Aardvark short", "Zulu short"),
			visibleTaskNames(((SpreadSheetModel) fixture.trackingSheet.getModel()).getCache()));
		assertEquals("Zulu short", zulu.getName(), "editing the projected task must not mutate another task");
		assertEquals("Bravo long", bravo.getName(), "editing the projected task must not mutate another task");
		SwingUtilities.invokeAndWait(() -> project.getUndoController().undo());
		assertEquals("Alpha short", target.getName(), "Undo must restore the original task name");
		assertEquals("Alpha short", renderedCellText(fixture.trackingSheet,
			findTaskRow(fixture.trackingSheet, target), nameColumn));
		SwingUtilities.invokeAndWait(() -> project.getUndoController().redo());
		assertEquals("Aardvark short", target.getName(), "Redo must restore the edited task name");
		assertEquals("Aardvark short", renderedCellText(fixture.trackingSheet,
			findTaskRow(fixture.trackingSheet, target), nameColumn));

		ByteArrayOutputStream projectionMpo = new ByteArrayOutputStream();
		assertTrue(new MpoFileImporter().saveProject(project, projectionMpo), "MPO save of projected Start edit must succeed");
		MpoFileImporter projectionReader = new MpoFileImporter();
		projectionReader.setProjectFactory(ProjectFactory.getInstance());
		Project projectionReopened = projectionReader.loadProject(new ByteArrayInputStream(projectionMpo.toByteArray()));
		assertEquals("Aardvark short", ((NormalTask) projectionReopened.findByUniqueId(stableId)).getName(),
			"MPO reload must retain the physical Name edit on the same stable task");
		NormalTask reopenedTarget = (NormalTask) projectionReopened.findByUniqueId(stableId);
		Calendar reopenedStart = DateTime.calendarInstance();
		reopenedStart.setTimeInMillis(reopenedTarget.getStart());
		assertEquals(2026, reopenedStart.get(Calendar.YEAR));
		assertEquals(Calendar.SEPTEMBER, reopenedStart.get(Calendar.MONTH));
		assertEquals(25, reopenedStart.get(Calendar.DAY_OF_MONTH),
			"MPO reload must retain the physical Start edit on the same stable task");
	}

	private void showFixture(Fixture fixture) throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			frame = new MainRibbonFrame("microProject — U-26 spreadsheet input transaction", null);
			graphicManager = new GraphicManager(frame);
			frame.setGraphicManager(graphicManager);
			graphicManager.initView();
			graphicManager.addProjectFrame(fixture.task.getProject());
			tabs = new JTabbedPane();
			tabs.addTab("Entry", new JScrollPane(fixture.entrySheet));
			tabs.addTab("Tracking", new JScrollPane(fixture.trackingSheet));
			tabs.addChangeListener(event -> {
				if (graphicManager.getCurrentFrame() != null)
					graphicManager.getCurrentFrame().activateView(tabs.getSelectedIndex() == 0
						? MenuActionConstants.ACTION_GANTT : MenuActionConstants.ACTION_TRACKING_GANTT);
			});
			JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabs, new JScrollPane(fixture.gantt));
			frame.add(split);
			frame.setPreferredSize(new Dimension(1400, 560));
			frame.pack();
			split.setDividerLocation(0.58d);
			frame.validate();
			frame.setLocationByPlatform(true);
			frame.setAlwaysOnTop(true);
			frame.setVisible(true);
		});
	}

	private int selectedTabIndex() throws Exception {
		int[] selected = new int[1];
		SwingUtilities.invokeAndWait(() -> selected[0] = tabs.getSelectedIndex());
		return selected[0];
	}

	private void activate(SpreadSheet sheet, Robot robot) throws Exception {
		awaitSelectedSheetReady(sheet, false);
		assertTrue(selectedSpreadsheetViewportWidth() >= 500,
			"selected U-26 spreadsheet viewport must have a usable width of at least 500 px");
		GuiAcceptanceSupport.activateAndAwaitRenderedWindow(frame, tabs, robot, true,
			"U-26 spreadsheet window");
		SwingUtilities.invokeAndWait(sheet::requestFocusInWindow);
		awaitSelectedSheetReady(sheet, true);
	}

	private void awaitSelectedSheetReady(SpreadSheet sheet, boolean requireFocus) throws Exception {
		GuiAcceptanceSupport.await(() -> isSelectedSheetReady(sheet, requireFocus),
			"selected U-26 task table did not finish showing and laying out");
	}

	private boolean isSelectedSheetReady(SpreadSheet sheet, boolean requireFocus) {
		boolean[] ready = new boolean[1];
		try {
			SwingUtilities.invokeAndWait(() -> ready[0] = frame.isShowing()
				&& sheet.isShowing() && sheet.isValid() && sheet.getWidth() > 0 && sheet.getHeight() > 0
				&& sheet.getVisibleRect().width > 0 && sheet.getVisibleRect().height > 0
				&& SwingUtilities.isDescendingFrom(sheet, tabs.getSelectedComponent())
				&& (!requireFocus || frame.isActive() && sheet.isFocusOwner()));
		} catch (Exception exception) {
			throw new AssertionError("could not inspect the selected U-26 sheet on the EDT", exception);
		}
		return ready[0];
	}

	private int selectedSpreadsheetViewportWidth() throws Exception {
		int[] width = new int[1];
		SwingUtilities.invokeAndWait(() -> {
			JScrollPane selectedScrollPane = (JScrollPane) tabs.getSelectedComponent();
			width[0] = selectedScrollPane.getViewport().getExtentSize().width;
		});
		return width[0];
	}

	private void editWithPhysicalKeys(Robot robot, SpreadSheet sheet, int row, int column, String value) throws Exception {
		editWithPhysicalKeys(robot, sheet, row, column, value, null, KeyEvent.VK_ENTER, false);
	}

	private List<Long> typePercentFromFirstPhysicalKey(Robot robot, SpreadSheet sheet, int row, int column, String value)
			throws Exception {
		return typePercentFromFirstPhysicalKey(robot, sheet, row, column, value, KeyEvent.VK_ENTER, false);
	}

	private List<Long> typePercentFromFirstPhysicalKey(Robot robot, SpreadSheet sheet, int row, int column, String value,
			int commitKey, boolean shiftCommit) throws Exception {
		cellOnScreen(sheet, row, column); // Bring the target into view before measuring its physical position.
		Rectangle cell = currentCellOnScreen(sheet, row, column);
		clickCell(robot, cell, 0);
		robot.waitForIdle();
		int[] selected = new int[2];
		SwingUtilities.invokeAndWait(() -> {
			selected[0] = sheet.getSelection().getActiveRow();
			selected[1] = sheet.getSelection().getActiveColumn();
		});
		assertEquals(row, selected[0], "physical click must select the target task row before typing");
		assertTrue(!sheet.isEditing(), "the target cell must not already be editing before the first physical key");
		for (int navigation = 0; navigation < sheet.getColumnCount() && selected[1] < column; navigation++) {
			robot.keyPress(KeyEvent.VK_RIGHT);
			robot.keyRelease(KeyEvent.VK_RIGHT);
			robot.waitForIdle();
			SwingUtilities.invokeAndWait(() -> selected[1] = sheet.getSelection().getActiveColumn());
		}
		assertEquals(column, selected[1], "physical arrow navigation must focus the target cell before typing");
		List<Long> selectedTaskIdsBeforeTyping = stableSelectedTaskIds(sheet);

		int firstKey = KeyEvent.getExtendedKeyCodeForChar(value.charAt(0));
		boolean firstUppercase = Character.isUpperCase(value.charAt(0));
		if (firstUppercase) robot.keyPress(KeyEvent.VK_SHIFT);
		robot.keyPress(firstKey);
		robot.keyRelease(firstKey);
		if (firstUppercase) robot.keyRelease(KeyEvent.VK_SHIFT);
		GuiAcceptanceSupport.await(sheet::isEditing, "the first physical character must start cell editing without F2");
		JTextComponent input = editorInputComponent(sheet);
		assertTrue(input.isFocusOwner(), "the first physical character must focus the active cell editor");
		for (char character : value.substring(1).toCharArray()) {
			int keyCode = KeyEvent.getExtendedKeyCodeForChar(character);
			if (keyCode == KeyEvent.VK_UNDEFINED) throw new AssertionError("No key code for " + character);
			boolean uppercase = Character.isUpperCase(character);
			if (uppercase) robot.keyPress(KeyEvent.VK_SHIFT);
			robot.keyPress(keyCode);
			robot.keyRelease(keyCode);
			if (uppercase) robot.keyRelease(KeyEvent.VK_SHIFT);
		}
		robot.waitForIdle();
		assertEquals(value, editorInputComponent(sheet).getText(),
			"the editor must receive the complete multi-character value from physical key input");
		assertEquals(selectedTaskIdsBeforeTyping, stableSelectedTaskIds(sheet),
			"the stable selected task scope must survive all physical digits up to commit");
		if (shiftCommit) robot.keyPress(KeyEvent.VK_SHIFT);
		robot.keyPress(commitKey);
		robot.keyRelease(commitKey);
		if (shiftCommit) robot.keyRelease(KeyEvent.VK_SHIFT);
		GuiAcceptanceSupport.await(() -> !sheet.isEditing(), "physical first-key input did not commit: " + value);
		return selectedTaskIdsBeforeTyping;
	}

	private static List<Long> stableSelectedTaskIds(SpreadSheet sheet) throws Exception {
		AtomicReference<List<Long>> selectedTaskIds = new AtomicReference<>(List.of());
		SwingUtilities.invokeAndWait(() -> {
			List<Long> ids = new ArrayList<>();
			for (Node selectedNode : sheet.getSelectedNodes()) {
				if (selectedNode != null && selectedNode.getImpl() instanceof Task selectedTask
						&& !ids.contains(selectedTask.getUniqueId()))
					ids.add(selectedTask.getUniqueId());
			}
			selectedTaskIds.set(List.copyOf(ids));
		});
		return selectedTaskIds.get();
	}

	private static String renderedCellText(SpreadSheet sheet, int row, int column) throws Exception {
		String[] text = new String[1];
		SwingUtilities.invokeAndWait(() -> {
			Component rendered = sheet.prepareRenderer(sheet.getCellRenderer(row, column), row, column);
			text[0] = rendered instanceof NameCellComponent nameCell ? nameCell.getText()
				: rendered instanceof javax.swing.JLabel label ? label.getText() : rendered.toString();
		});
		return text[0];
	}

	private static boolean editorTextEquals(SpreadSheet sheet, String expected) {
		try {
			return expected.equals(activeEditorText(sheet));
		} catch (Exception exception) {
			throw new AssertionError("could not inspect the active spreadsheet editor", exception);
		}
	}

	private void editWithPhysicalKeys(Robot robot, SpreadSheet sheet, int row, int column, String value,
			String expectedEditorSeed) throws Exception {
		editWithPhysicalKeys(robot, sheet, row, column, value, expectedEditorSeed, KeyEvent.VK_ENTER, false);
	}

	private void editWithPhysicalKeys(Robot robot, SpreadSheet sheet, int row, int column, String value,
			String expectedEditorSeed, int commitKey, boolean shiftCommit) throws Exception {
		Rectangle anchor = cellOnScreen(sheet, row, Math.min(1, column));
		clickCell(robot, anchor, 0);
		robot.waitForIdle();
		SwingUtilities.invokeAndWait(() -> {
			sheet.changeSelection(row, column, false, false);
			assertTrue(sheet.editCellAt(row, column, null),
				"the input transaction must start editing row=" + row + " column=" + column);
		});
		GuiAcceptanceSupport.await(sheet::isEditing, "physical F2 did not start cell editing");
		assertEquals(column, sheet.getEditingColumn(), "physical click/F2 must edit the clicked field");
		JTextComponent input = editorInputComponent(sheet);
		GuiAcceptanceSupport.await(input::isFocusOwner, "cell editor did not receive focus");
		assertEquals(input, KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(),
			"physical input must target the active cell editor");
		SwingUtilities.invokeAndWait(input::requestFocusInWindow);
		GuiAcceptanceSupport.await(input::isFocusOwner, "cell editor lost focus before physical typing");
		if (expectedEditorSeed != null)
			assertEquals(expectedEditorSeed, input.getText(), "duration editor must seed from the task's display format");
		robot.waitForIdle();
		robot.delay(150);
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_A);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		for (char character : value.toCharArray()) {
			int keyCode = KeyEvent.getExtendedKeyCodeForChar(character);
			if (keyCode == KeyEvent.VK_UNDEFINED) throw new AssertionError("No key code for " + character);
			robot.keyPress(keyCode);
			robot.keyRelease(keyCode);
		}
		robot.waitForIdle();
		assertEquals(value, activeEditorText(sheet), "physical keys must reach the active editor before commit");
		if (shiftCommit) robot.keyPress(KeyEvent.VK_SHIFT);
		robot.keyPress(commitKey);
		robot.keyRelease(commitKey);
		if (shiftCommit) robot.keyRelease(KeyEvent.VK_SHIFT);
		GuiAcceptanceSupport.await(() -> !sheet.isEditing(), "physical input did not commit: " + value);
	}

	private Rectangle cellOnScreen(SpreadSheet sheet, int row, int column) throws Exception {
		Rectangle[] result = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			Component selectedTab = tabs.getSelectedComponent();
			boolean selectedAncestor = selectedTab != null && SwingUtilities.isDescendingFrom(sheet, selectedTab);
			if (!sheet.isShowing() || !sheet.isValid() || !selectedAncestor)
				throw new AssertionError("U-26 cell coordinates require the selected, laid-out task table to be showing: "
					+ "frameShowing=" + frame.isShowing() + ", selectedIndex=" + tabs.getSelectedIndex()
					+ ", selectedTab=" + componentState(selectedTab) + ", sheet=" + componentState(sheet)
					+ ", selectedAncestor=" + selectedAncestor);
			sheet.changeSelection(row, Math.max(0, column - 1), false, false);
			Rectangle bounds = sheet.getCellRect(row, column, true);
			sheet.scrollRectToVisible(bounds);
			if (!sheet.getVisibleRect().intersects(bounds))
				throw new AssertionError("U-26 target cell is outside the laid-out spreadsheet viewport");
			Point location = sheet.getLocationOnScreen();
			result[0] = new Rectangle(location.x + bounds.x, location.y + bounds.y, bounds.width, bounds.height);
		});
		return result[0];
	}

	private Rectangle currentCellOnScreen(SpreadSheet sheet, int row, int column) throws Exception {
		Rectangle[] result = new Rectangle[1];
		SwingUtilities.invokeAndWait(() -> {
			Rectangle bounds = sheet.getCellRect(row, column, true);
			Point location = sheet.getLocationOnScreen();
			result[0] = new Rectangle(location.x + bounds.x, location.y + bounds.y, bounds.width, bounds.height);
		});
		return result[0];
	}

	private static String componentState(Component component) {
		if (component == null) return "null";
		return component.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(component))
			+ "[showing=" + component.isShowing() + ",valid=" + component.isValid()
			+ ",parent=" + (component.getParent() == null ? "null"
				: component.getParent().getClass().getSimpleName() + "@"
					+ Integer.toHexString(System.identityHashCode(component.getParent())))
			+ ",size=" + component.getWidth() + "x" + component.getHeight() + "]";
	}

	private static void clickCell(Robot robot, Rectangle cell, int xOffset) {
		robot.mouseMove(cell.x + cell.width / 2 + xOffset, cell.y + cell.height / 2);
		robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
	}

	private static JTextComponent editorInputComponent(SpreadSheet sheet) throws Exception {
		JTextComponent[] result = new JTextComponent[1];
		SwingUtilities.invokeAndWait(() -> {
			Component editor = sheet.getEditorComponent();
			if (editor instanceof DateEditor.ExtDateField date) {
				result[0] = date.getTextField();
			} else {
				result[0] = findTextComponent(editor);
			}
		});
		return result[0];
	}

	private static JTextComponent findTextComponent(Component component) {
		if (component instanceof JTextComponent text) return text;
		if (component instanceof java.awt.Container container) {
			for (Component child : container.getComponents()) {
				JTextComponent result = findTextComponent(child);
				if (result != null) return result;
			}
		}
		return null;
	}

	private static String activeEditorText(SpreadSheet sheet) throws Exception {
		String[] result = new String[1];
		SwingUtilities.invokeAndWait(() -> {
			if (sheet.getEditorComponent() instanceof JTextComponent text) result[0] = text.getText();
			else if (sheet.getEditorComponent() instanceof DateEditor.ExtDateField date) result[0] = date.getTextField().getText();
		});
		return result[0];
	}

	private static Fixture createFixture() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("u26-physical-input", undo), undo);
		project.setDurationDefaults(new DurationDefaults(360.0D, 1800.0D, 20.0D,
			TimeUnit.HOURS, TimeUnit.HOURS));
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setName("U-26 physical input");
		task.setStart(DateTime.calendarInstance(2026, Calendar.SEPTEMBER, 1).getTimeInMillis());
		task.setDuration(Duration.getInstance(8.0D, TimeUnit.DAYS, project.getDurationDefaults()));
		task.setPercentComplete(0.0d);
		final Fixture[] result = new Fixture[1];
		SwingUtilities.invokeAndWait(() -> {
			SheetSetup entry = createSheet(project, "u26-physical-input-entry", "Spreadsheet.Task.entry");
			SheetSetup tracking = createSheet(project, "u26-physical-input-tracking", "Spreadsheet.Task.tracking");
		int entryRow = findTaskRow(entry, task);
		int trackingRow = findTaskRow(tracking, task);
			Gantt gantt = new Gantt(project, "Gantt");
			gantt.setCache(entry.cache());
			gantt.setCoord(new CoordinatesConverter(project));
			gantt.setBarStyles((BarStyles) Dictionary.get(BarStyles.category, "standard"));
			gantt.updateSize();
			result[0] = new Fixture(entry.sheet(), tracking.sheet(), gantt, task, entryRow, trackingRow,
				findColumn(entry.sheet(), "Field.duration"), findColumn(tracking.sheet(), "Field.percentComplete"),
				findColumn(entry.sheet(), "Field.start"));
		});
		return result[0];
	}

	private static SheetSetup createSheet(Project project, String cacheId, String spreadsheetId) {
		SpreadSheet sheet = new SpreadSheet();
		sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
		NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
			NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()), cacheId, null);
		SpreadSheetUtils.setFieldsAndContext(sheet, cache, SpreadSheetCategories.taskSpreadsheetCategory, spreadsheetId, true);
		((SpreadSheetModel) sheet.getModel()).getFieldContext().setDurationDefaultsResolver(project::getDurationDefaults);
		return new SheetSetup(sheet, cache);
	}

	private static int findTaskRow(SheetSetup setup, NormalTask task) {
		SpreadSheetModel model = (SpreadSheetModel) setup.sheet().getModel();
		Node node = (Node) setup.cache().getModel().search(task);
		return model.findGraphicNodeRow(setup.cache().getGraphicNode(node));
	}

	private static int findTaskRow(SpreadSheet sheet, NormalTask task) {
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		Node node = (Node) model.getCache().getModel().search(task);
		return model.findGraphicNodeRow(model.getCache().getGraphicNode(node));
	}

	private static void configureProjection(NodeModelCache cache) {
		ViewTransformer transformer = ((NodeCacheTransformer) cache.getVisibleNodes().getTransformer()).getTransformer();
		transformer.setUserFilterId("Filter.IncompleteTasks");
		transformer.setUserGrouperId("Grouper.Duration");
		transformer.setTaskSortSpec(new TaskSortSpec(
			List.of(new TaskSortSpec.Key("Sorter.Name", true)), true));
	}

	private static List<String> visibleTaskNames(NodeModelCache cache) {
		return cache.getVisibleNodes().getElements().stream().filter(GraphicNode.class::isInstance)
			.map(GraphicNode.class::cast).filter(node -> !node.isGroup() && node.getNode().getImpl() instanceof NormalTask)
			.map(node -> ((NormalTask) node.getNode().getImpl()).getName()).toList();
	}

	private static long selectedStableTaskId(SpreadSheet sheet) throws Exception {
		long[] result = {-1L};
		SwingUtilities.invokeAndWait(() -> {
			for (Node node : sheet.getSelectedNodes()) {
				if (node != null && node.getImpl() instanceof Task task) {
					result[0] = task.getUniqueId();
					return;
				}
			}
		});
		return result[0];
	}

	private static long taskIdAtVisibleRow(SpreadSheet sheet, int row) throws Exception {
		long[] result = {-1L};
		SwingUtilities.invokeAndWait(() -> {
			Node node = ((SpreadSheetModel) sheet.getModel()).getNodeForDisplayRow(row);
			if (node != null && node.getImpl() instanceof Task task) result[0] = task.getUniqueId();
		});
		return result[0];
	}

	private static int findColumn(SpreadSheet sheet, String fieldId) {
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		for (int column = 0; column < model.getColumnCount(); column++) {
			Field field = model.getFieldInColumn(column);
			if (field != null && fieldId.equals(field.getId())) return sheet.convertColumnIndexToView(column);
		}
		throw new IllegalArgumentException("Missing field: " + fieldId);
	}

	private static String remainingDurationText(Fixture fixture) {
		Field field = FieldDictionary.getInstance().getFieldFromId("Field.remainingDuration");
		return field.getText(fixture.task,
			((SpreadSheetModel) fixture.entrySheet.getModel()).getFieldContext());
	}

	private static String durationText(NormalTask task, FieldContext context) {
		Field field = FieldDictionary.getInstance().getFieldFromId("Field.duration");
		return field.getText(task, context);
	}

	private static GanttGeometry ganttGeometry(Fixture fixture) throws Exception {
		GanttGeometry[] result = new GanttGeometry[1];
		SwingUtilities.invokeAndWait(() -> {
			JScrollPane pane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, fixture.gantt);
			Point viewport = pane == null ? new Point() : pane.getViewport().getViewPosition();
			double start = fixture.gantt.getCoord().toX(fixture.task.getStart());
			double width = fixture.gantt.getCoord().toX(fixture.task.getEnd()) - start;
			result[0] = new GanttGeometry(viewport, new BarGeometry((int) Math.round(start), (int) Math.round(width)));
		});
		return result[0];
	}

	private static BarGeometry ganttBarGeometry(Fixture fixture, NormalTask task) throws Exception {
		BarGeometry[] result = new BarGeometry[1];
		SwingUtilities.invokeAndWait(() -> {
			double start = fixture.gantt.getCoord().toX(task.getStart());
			double width = fixture.gantt.getCoord().toX(task.getEnd()) - start;
			result[0] = new BarGeometry((int) Math.round(start), (int) Math.round(width));
		});
		return result[0];
	}

	private static void physicalUndoRedo(Robot robot, int key) throws Exception {
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(key);
		robot.keyRelease(key);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		robot.waitForIdle();
		SwingUtilities.invokeAndWait(() -> { });
	}

	private void beginUndoTraceCapture() {
		previousUiDebug = System.getProperty("microproject.ui.debug");
		System.setProperty("microproject.ui.debug", "true");
		undoTraceLogger = Logger.getLogger(GraphicManager.class.getName());
		previousUndoTraceLevel = undoTraceLogger.getLevel();
		undoTraceLogger.setLevel(Level.FINE);
		undoTraceHandler = new Handler() {
			@Override public void publish(LogRecord record) {
				if (record != null && record.getMessage() != null) undoTrace.add(record.getMessage());
			}
			@Override public void flush() { }
			@Override public void close() { }
		};
		undoTraceLogger.addHandler(undoTraceHandler);
	}

	private record BarGeometry(int startX, int width) { }
	private record GanttGeometry(Point viewport, BarGeometry bar) { }

	private static final class Fixture {
		final SpreadSheet entrySheet;
		final SpreadSheet trackingSheet;
		final Gantt gantt;
		final NormalTask task;
		final int entryRow, trackingRow, durationColumn, percentColumn, startColumn;
		private Point progressViewportBefore;
		private BarGeometry progressBarBefore;

		Fixture(SpreadSheet entrySheet, SpreadSheet trackingSheet, Gantt gantt, NormalTask task, int entryRow,
			int trackingRow, int durationColumn, int percentColumn, int startColumn) {
			this.entrySheet = entrySheet; this.trackingSheet = trackingSheet; this.gantt = gantt; this.task = task;
			this.entryRow = entryRow; this.trackingRow = trackingRow; this.durationColumn = durationColumn;
			this.percentColumn = percentColumn; this.startColumn = startColumn;
		}

		void setProgressBaseline() throws Exception {
			GanttGeometry geometry = ganttGeometry(this);
			progressViewportBefore = geometry.viewport();
			progressBarBefore = geometry.bar();
		}
		Point progressViewportBefore() { return progressViewportBefore; }
		BarGeometry progressBarBefore() { return progressBarBefore; }
	}
	private record SheetSetup(SpreadSheet sheet, NodeModelCache cache) { }
}

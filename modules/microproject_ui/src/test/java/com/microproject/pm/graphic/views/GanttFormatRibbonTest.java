/*
 * Copyright (c) 2026 microProject
 * Licensed under the MIT License.
 */
package com.microproject.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.configuration.FieldDictionary;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.grouping.core.transform.ViewTransformer;
import com.microproject.options.CalendarOption;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.gantt.GanttBarColorScheme;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.pm.task.TaskPathAnalyzer;
import com.microproject.undo.DataFactoryUndoController;

/** Behaviour behind the Gantt Chart Format ribbon controls that works on a Gantt view without a full frame. */
class GanttFormatRibbonTest {
	private static Project newProject(String name) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool(name, undo), undo);
		project.initialize(false, false);
		return project;
	}

	private static NormalTask task(Project project, String name) {
		NormalTask task = new NormalTask(project);
		task.setName(name);
		project.connectTask(task);
		project.getSchedulingAlgorithm().addObject(task);
		task.getCurrentSchedule().setStart(project.getStart());
		task.setDuration(CalendarOption.getInstance().getMillisPerDay());
		return task;
	}

	@Test
	void barSwitchesHaveMicrosoftProjectDefaultsAndToggle() {
		Gantt gantt = new Gantt(newProject("gfmt-toggles"), "Gantt");
		try {
			assertTrue(gantt.isCriticalTasksVisible());
			assertFalse(gantt.isSlackVisible());
			assertFalse(gantt.isLateTasksVisible());
			assertTrue(gantt.isBaselineVisible());
			assertFalse(gantt.isSlippageVisible());
			assertSame(GanttBarColorScheme.DEFAULT, gantt.getBarColorScheme());
			gantt.setCriticalTasksVisible(false);
			gantt.setSlackVisible(true);
			gantt.setLateTasksVisible(true);
			gantt.setBaselineVisible(false);
			gantt.setSlippageVisible(true);
			assertFalse(gantt.isCriticalTasksVisible());
			assertTrue(gantt.isSlackVisible());
			assertTrue(gantt.isLateTasksVisible());
			assertFalse(gantt.isBaselineVisible());
			assertTrue(gantt.isSlippageVisible());
		} finally {
			gantt.cleanUp();
		}
	}

	@Test
	void barSwitchesAndStyleSchemeRoundTripThroughTheSerializedWorkspace() throws Exception {
		Gantt gantt = new Gantt(newProject("gfmt-workspace-source"), "Gantt");
		Gantt reopened = new Gantt(newProject("gfmt-workspace-target"), "Gantt");
		try {
			gantt.setCriticalTasksVisible(false);
			gantt.setSlackVisible(true);
			gantt.setLateTasksVisible(true);
			gantt.setBaselineVisible(false);
			gantt.setSlippageVisible(true);
			gantt.setBarColorScheme(GanttBarColorScheme.PURPLE);
			GanttView.Workspace saved = new GanttView.Workspace();
			GanttView.captureBarFormat(gantt, saved);

			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
				out.writeObject(saved);
			}
			GanttView.Workspace loaded;
			try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
				loaded = (GanttView.Workspace) in.readObject();
			}
			GanttView.applyBarFormat(reopened, loaded);

			assertFalse(reopened.isCriticalTasksVisible());
			assertTrue(reopened.isSlackVisible());
			assertTrue(reopened.isLateTasksVisible());
			assertFalse(reopened.isBaselineVisible());
			assertTrue(reopened.isSlippageVisible());
			assertSame(GanttBarColorScheme.PURPLE, reopened.getBarColorScheme());
		} finally {
			gantt.cleanUp();
			reopened.cleanUp();
		}
	}

	@Test
	void workspaceWithoutTheSwitchesAppliesTheDefaults() {
		Gantt gantt = new Gantt(newProject("gfmt-legacy-workspace"), "Gantt");
		try {
			gantt.setCriticalTasksVisible(false);
			gantt.setSlackVisible(true);
			gantt.setBarColorScheme(GanttBarColorScheme.GRAY);
			GanttView.applyBarFormat(gantt, new GanttView.Workspace());
			assertTrue(gantt.isCriticalTasksVisible());
			assertFalse(gantt.isSlackVisible());
			assertTrue(gantt.isBaselineVisible());
			assertSame(GanttBarColorScheme.DEFAULT, gantt.getBarColorScheme());
		} finally {
			gantt.cleanUp();
		}
	}

	@Test
	void styleGalleryEntriesResolveToTheirSchemeAndUnknownIdsFallBack() {
		for (GanttBarColorScheme scheme : GanttBarColorScheme.values())
			assertSame(scheme, GanttBarColorScheme.fromId(scheme.getId()));
		assertSame(GanttBarColorScheme.DEFAULT, GanttBarColorScheme.fromId("no-such-scheme"));
		assertSame(GanttBarColorScheme.DEFAULT, GanttBarColorScheme.fromId(null));
		assertNull(GanttBarColorScheme.DEFAULT.getTaskColor());
		assertNotNull(GanttBarColorScheme.BLUE.getTaskColor());
	}

	@Test
	void taskPathModesHighlightTheExpectedTasksAndRemoveClearsThem() throws Exception {
		Project project = newProject("gfmt-task-path");
		Gantt gantt = new Gantt(project, "Gantt");
		try {
			NormalTask a = task(project, "A");
			NormalTask b = task(project, "B");
			NormalTask c = task(project, "C");
			DependencyService.getInstance().newDependency(a, b, DependencyType.FS, 0L, this);
			DependencyService.getInstance().newDependency(b, c, DependencyType.FS, 0L, this);
			project.recalculate();

			gantt.setTaskPath(TaskPathAnalyzer.Kind.PREDECESSORS, List.of(c));
			assertSame(TaskPathAnalyzer.Kind.PREDECESSORS, gantt.getTaskPathKind());
			assertEquals(Set.<Task>of(a, b), gantt.getTaskPathTasks());
			gantt.setTaskPath(TaskPathAnalyzer.Kind.SUCCESSORS, List.of(a));
			assertEquals(Set.<Task>of(b, c), gantt.getTaskPathTasks());
			gantt.setTaskPath(TaskPathAnalyzer.Kind.DRIVING_PREDECESSORS, List.of(c));
			assertEquals(Set.<Task>of(a, b), gantt.getTaskPathTasks());
			gantt.setTaskPath(TaskPathAnalyzer.Kind.DRIVEN_SUCCESSORS, List.of(a));
			assertEquals(Set.<Task>of(b, c), gantt.getTaskPathTasks());
			gantt.setTaskPath(TaskPathAnalyzer.Kind.PREDECESSORS, List.<Task>of());
			assertTrue(gantt.getTaskPathTasks().isEmpty(), "no selection highlights nothing");

			gantt.setTaskPath(null, List.of(c));
			assertNull(gantt.getTaskPathKind());
			assertTrue(gantt.getTaskPathTasks().isEmpty(), "Remove Highlighting clears the path");
		} finally {
			gantt.cleanUp();
		}
	}

	@Test
	void outlineNumbersSwitchLivesOnTheSheet() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			assertFalse(sheet.isOutlineNumbersVisible());
			sheet.setOutlineNumbersVisible(true);
			assertTrue(sheet.isOutlineNumbersVisible());
			sheet.setOutlineNumbersVisible(false);
			assertFalse(sheet.isOutlineNumbersVisible());
		});
	}

	@Test
	void summaryTasksSwitchIsAViewTransformerFlag() {
		ViewTransformer transformer = new ViewTransformer();
		assertFalse(transformer.isHideSummaryTasks());
		transformer.setHideSummaryTasks(true);
		assertTrue(transformer.isHideSummaryTasks());
		transformer.setHideSummaryTasks(false);
		assertFalse(transformer.isHideSummaryTasks());
	}

	@Test
	void columnAlignmentIsPerColumnAndLeavesTheOthersAlone() {
		FieldDictionary dictionary = FieldDictionary.getInstance();
		SpreadSheetFieldArray fields = new SpreadSheetFieldArray();
		fields.add(dictionary.getFieldFromId("Field.name"));
		fields.add(dictionary.getFieldFromId("Field.duration"));
		fields.add(dictionary.getFieldFromId("Field.percentComplete"));
		SpreadSheetFieldArray aligned = fields.withAlignment(1, SpreadSheetFieldArray.ALIGN_LEFT)
			.withAlignment(2, SpreadSheetFieldArray.ALIGN_CENTER);
		assertEquals(SpreadSheetFieldArray.ALIGN_LEFT, aligned.getAlignment(1));
		assertEquals(SpreadSheetFieldArray.ALIGN_CENTER, aligned.getAlignment(2));
		assertEquals(SpreadSheetFieldArray.ALIGN_DEFAULT, aligned.getAlignment(0));
		assertEquals(SpreadSheetFieldArray.ALIGN_RIGHT,
			aligned.withAlignment(1, SpreadSheetFieldArray.ALIGN_RIGHT).getAlignment(1));
	}
}

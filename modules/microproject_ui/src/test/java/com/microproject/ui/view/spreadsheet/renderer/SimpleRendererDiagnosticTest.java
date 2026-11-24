/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.ui.view.spreadsheet.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.preference.GlobalPreferences;
import com.microproject.undo.DataFactoryUndoController;

class SimpleRendererDiagnosticTest {
	@Test
	void warningUnderlineAppliesOnlyToStartFinishAndDurationOfAffectedTasks() throws Exception {
		GlobalPreferences preferences = new GlobalPreferences();
		boolean originalWarnings = preferences.isShowTaskWarnings();
		boolean originalSuggestions = preferences.isShowTaskSuggestions();
		try {
			preferences.setShowTaskWarnings(true);
			preferences.setShowTaskSuggestions(false);
			NormalTask successor = conflictingSuccessor();

			assertEquals(SimpleRenderer.Diagnostic.WARNING, SimpleRenderer.diagnosticFor(successor, "Field.start"));
			assertEquals(SimpleRenderer.Diagnostic.WARNING, SimpleRenderer.diagnosticFor(successor, "Field.finish"));
			assertEquals(SimpleRenderer.Diagnostic.WARNING, SimpleRenderer.diagnosticFor(successor, "Field.duration"));
			assertEquals(SimpleRenderer.Diagnostic.NONE, SimpleRenderer.diagnosticFor(successor, "Field.name"));
			assertEquals(SimpleRenderer.Diagnostic.NONE, SimpleRenderer.diagnosticFor(successor, null));
			assertEquals(SimpleRenderer.Diagnostic.NONE, SimpleRenderer.diagnosticFor("not a task", "Field.start"));

			preferences.setShowTaskWarnings(false);
			assertEquals(SimpleRenderer.Diagnostic.NONE, SimpleRenderer.diagnosticFor(successor, "Field.start"));

			preferences.setShowTaskWarnings(true);
			successor.setIgnoreWarnings(true);
			assertEquals(SimpleRenderer.Diagnostic.NONE, SimpleRenderer.diagnosticFor(successor, "Field.start"));
		} finally {
			preferences.setShowTaskWarnings(originalWarnings);
			preferences.setShowTaskSuggestions(originalSuggestions);
		}
	}

	@Test
	void sharedRendererResetsTheFlagForCellsWithoutDiagnostics() {
		SimpleRenderer renderer = new SimpleRenderer();
		renderer.getTableCellRendererComponent(null, "value", false, false, -1, -1);
		assertEquals(SimpleRenderer.Diagnostic.NONE, renderer.getDiagnostic());
	}

	private static NormalTask conflictingSuccessor() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("renderer-diagnostic", undo), undo);
		project.initialize(false, false);
		NormalTask predecessor = new NormalTask(project);
		predecessor.setName("Predecessor");
		project.connectTask(predecessor);
		project.getSchedulingAlgorithm().addObject(predecessor);
		predecessor.getCurrentSchedule().setStart(project.getStart());
		predecessor.setDuration(2L * com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		NormalTask successor = new NormalTask(project);
		successor.setName("Successor");
		project.connectTask(successor);
		project.getSchedulingAlgorithm().addObject(successor);
		successor.getCurrentSchedule().setStart(project.getStart());
		successor.setDuration(com.microproject.options.CalendarOption.getInstance().getMillisPerDay());
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, new Object());
		successor.setManualDates(predecessor.getStart(), predecessor.getEnd());
		return successor;
	}
}

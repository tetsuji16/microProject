/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.view.views;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.microproject.view.configuration.GraphicConfiguration;
import com.microproject.model.resource.ResourcePool;
import com.microproject.model.snapshot.DataSnapshot;
import com.microproject.model.snapshot.Snapshottable;
import com.microproject.model.task.NormalTask;
import com.microproject.model.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class GanttRowHeightTest {
	@Test
	void rowHeightTracksIntegerBaselineIndexes() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("row-height", undo), undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setSnapshot(Snapshottable.BASELINE_2, new MarkerSnapshot());
		TreeSet<Integer> baselines = new TreeSet<>();

		int rowHeight = GanttRowHeight.calculate(project, baselines);

		assertEquals(GraphicConfiguration.getInstance().getRowHeight()
			+ 3 * GraphicConfiguration.getInstance().getBaselineHeight(), rowHeight);
		assertEquals(new TreeSet<>(java.util.List.of(2)), baselines);
	}

	private record MarkerSnapshot() implements DataSnapshot { }
}

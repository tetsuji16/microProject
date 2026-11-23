/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.print;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.offline_graphics.GanttSVGRenderer;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

/**
 * Issue #1336: fitting a drawing to a page grid must not stretch it. A Gantt chart fitted to "1 page wide by
 * 1 page tall" used to get one scale for the width and another for the height, so the print preview showed the
 * table and the bars squashed horizontally.
 */
class GraphPageableFitToZoomTest {

	@Test
	void fittingToOnePageWideAndTallKeepsTheDrawingProportions() {
		GraphPageable document = document();

		document.updateZoom(1, 1);

		assertEquals(document.getTotalZoomX(), document.getTotalZoomY(), 1e-9,
				"a fit-to grid must not use a different scale per axis");
		assertEquals(1, document.getNumberOfPages(), "a 1 x 1 fit must stay on one page");
		assertTrue(document.getTotalZoomX() < PrintDocument.DEFAULT_ZOOM,
				"a drawing wider than the page must be scaled down, not stretched: " + document.getTotalZoomX());
	}

	@Test
	void fittingToOnePageWideByThreeTallKeepsTheDrawingProportions() {
		GraphPageable document = document();

		document.updateZoom(1, 3);

		assertEquals(document.getTotalZoomX(), document.getTotalZoomY(), 1e-9,
				"the height limit must not be applied as an independent vertical stretch");
		assertTrue(document.getNumberOfPages() <= 3,
				"the drawing must stay inside the requested page grid: " + document.getNumberOfPages());
	}

	@Test
	void fittingToAWidthOnlyUsesOneScaleForBothAxes() {
		GraphPageable document = document();

		document.updateZoom(1, 0);

		assertEquals(document.getTotalZoomX(), document.getTotalZoomY(), 1e-9,
				"an automatic page height must follow the width scale");
	}

	/** A landscape page with a dozen tasks, so the drawing is both taller and wider than one page. */
	private static GraphPageable document() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("fit-to-zoom", undo), undo);
		project.initialize(false, false);
		for (int index = 0; index < 12; index++) {
			NormalTask task = project.createScriptedTask();
			task.setName("Fit to zoom task " + index + " 日本語のタスク名");
		}
		project.recalculate();
		var referenceCache = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
		var cache = NodeModelCacheFactory.getInstance().createFilteredCache(referenceCache, "OfflineGantt", null);
		GanttSVGRenderer renderer = new GanttSVGRenderer();
		renderer.init(project, cache, null, null, -1, true);
		return new GraphPageable(renderer, false, false, true);
	}
}

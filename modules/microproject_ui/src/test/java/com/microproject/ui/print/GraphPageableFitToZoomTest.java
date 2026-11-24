/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.print;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.print.PrintService;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSizeName;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.ui.export.svg.GanttSVGRenderer;
import com.microproject.ui.view.model.cache.NodeModelCacheFactory;
import com.microproject.model.task.NormalTask;
import com.microproject.model.task.Project;
import com.microproject.model.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.print.ExtendedPageFormat;
import com.microproject.print.PrintSettings;
import com.microproject.print.ScaleToSettings;

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

	@Test
	void savedPercentageScalingInitializesPagesBeforePreview() {
		GraphPageable document = document(true);
		assertTrue(document.getNumberOfPages() > 0, "saved percentage scaling must initialize the preview pages");
	}

	@Test
	void backstagePreviewRefreshesPageCountAfterFitChanges() throws Exception {
		GraphPageable document = document(true);
		assertTrue(document.getNumberOfPages() > 1, "fixture must start with multiple pages");
		JLabel label = new JLabel();
		Class<?> panelType = Class.forName("com.microproject.ui.ribbon.ModernRibbonPanel$BackstagePrintPreviewPanel");
		var constructor = panelType.getDeclaredConstructor(GraphPageable.class, JLabel.class);
		constructor.setAccessible(true);
		var refresh = panelType.getDeclaredMethod("refreshDocument");
		refresh.setAccessible(true);
		SwingUtilities.invokeAndWait(() -> {
			try {
				Object panel = constructor.newInstance(document, label);
				document.updateZoom(1, 1);
				refresh.invoke(panel);
				assertEquals("1 / 1", label.getText());
			} catch (ReflectiveOperationException failure) {
				throw new AssertionError(failure);
			}
		});
	}

	/** A landscape page with a dozen tasks, so the drawing is both taller and wider than one page. */
	private static GraphPageable document() {
		return document(false);
	}

	private static GraphPageable document(boolean percentageScaling) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("fit-to-zoom", undo), undo);
		project.initialize(false, false);
		for (int index = 0; index < 12; index++) {
			NormalTask task = project.createScriptedTask();
			task.setName("Fit to zoom task " + index + " 日本語のタスク名");
		}
		project.recalculate();
		if (percentageScaling) {
			PrintSettings settings = new PrintSettings();
			settings.setScalingSettings(new ArrayList<>(List.of(new ScaleToSettings())));
			settings.setScalingIndex(0);
			project.setTmpSettings(settings);
		}
		var referenceCache = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
		var cache = NodeModelCacheFactory.getInstance().createFilteredCache(referenceCache, "OfflineGantt", null);
		GanttSVGRenderer renderer = new GanttSVGRenderer();
		renderer.init(project, cache, null, null, -1, true);
		GraphPageable document = new GraphPageable(renderer, false, false, false, new PrintService[0], null);
		document.setPageFormat(new ExtendedPageFormat(MediaSizeName.NA_LETTER,
				new MediaPrintableArea(28f / 72, 28f / 72, 556f / 72, 736f / 72, MediaPrintableArea.INCH)));
		return document;
	}
}

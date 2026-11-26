/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.ui.print;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.print.PageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.print.PrintService;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

import com.microproject.i18n.Messages;
import com.microproject.model.resource.ResourcePool;
import com.microproject.model.task.NormalTask;
import com.microproject.model.task.Project;
import com.microproject.print.ExtendedPageFormat;
import com.microproject.print.FitToSettings;
import com.microproject.print.PrintSettings;
import com.microproject.print.PrintSettingsManager;
import com.microproject.print.ScaleToSettings;
import com.microproject.print.ScalingSettings;
import com.microproject.ui.export.svg.GanttSVGRenderer;
import com.microproject.ui.view.model.cache.NodeModelCacheFactory;
import com.microproject.undo.DataFactoryUndoController;

class PageSetupSettingsTransactionTest {

	@Test
	void localizedPageSetupKeepsPaperAndScalingControlsAtPreferredWidth() {
		Locale original = Locale.getDefault();
		try {
			withLocalSettings(settings(1, 3, letterLandscape()), () -> {
				GraphPageable document = document(true);
				for (Locale locale : new Locale[] { Locale.JAPANESE, Locale.ENGLISH }) {
					Locale.setDefault(locale);
					Messages.reset();
					PageSetup setup = PageSetup.getInstance(document);
					JComponent panel = setup.createContentPanel(false);
					layoutAtPreferredSize((JPanel) panel);
					for (JComponent control : List.of(setup.paperWidthLabel, setup.paperWidth,
							setup.paperHeightLabel, setup.paperHeight, setup.marginLeftLabel, setup.marginLeft,
							setup.marginRightLabel, setup.marginRight, setup.marginTopLabel, setup.marginTop,
							setup.marginBottomLabel, setup.marginBottom, setup.scaleToWidth, setup.scaleToHeight,
							setup.fitToWidth, setup.fitToHeight, setup.scaleToWidthLabel, setup.scaleToHeightLabel,
							setup.fitToWidthLabel, setup.fitToHeightLabel)) {
						assertTrue(control.getWidth() >= control.getPreferredSize().width,
								() -> "Page Setup locale=" + locale + " clips " + control.getClass().getSimpleName()
									+ " bounds=" + control.getBounds() + " preferred=" + control.getPreferredSize());
					}
				}
			});
		} finally {
			Locale.setDefault(original);
			Messages.reset();
		}
	}

	@Test
	void requestedFitHeightSurvivesLayoutRefreshAndPaperChange() {
		withLocalSettings(settings(1, 3, letterLandscape()), () -> {
			GraphPageable document = document(true);
			PageSetup setup = PageSetup.getInstance(document);
			setup.createContentPanel(false);

			assertEquals(3, ((Number) setup.fitToHeight.getValue()).intValue());
			document.updateZoom(1, 3);
			document.update();
			org.junit.jupiter.api.Assertions.assertTrue(document.getDocumentRowCount() < 3,
					"the fixture must occupy fewer pages than the requested maximum");
			assertEquals(3, ((Number) setup.fitToHeight.getValue()).intValue(),
					"rendered page count must not replace the requested height limit");

			document.setPageFormat(a5Landscape());
			setup.updateZoom();
			document.update();

			assertEquals(3, ((Number) setup.fitToHeight.getValue()).intValue(),
					"a paper change must continue to use the original requested height limit");
			assertEquals(1, document.getDocumentColCount());
			org.junit.jupiter.api.Assertions.assertTrue(document.getDocumentRowCount() <= 3,
					"the drawing must remain within the requested page grid");
		});
	}

	@Test
	void cancelRestoresPreviewAndLeavesThePrintSettingsUsedByTheNextDocumentUntouched() {
		PrintSettings before = settings(1, 3, letterLandscape());
		withLocalSettings(before, () -> {
			GraphPageable document = document(true);
			PageSetup setup = PageSetup.getInstance(document);
			setup.createContentPanel(false);
			double originalZoomX = document.getTotalZoomX();
			double originalZoomY = document.getTotalZoomY();

			setRequestedLimits(setup, 2, 4);
			document.setPageFormat(a5Landscape());
			document.updateZoom(2, 4);
			document.setScaleToSelected(true);
			setup.cancelDialogChanges();

			assertSame(before, PrintSettingsManager.getSettings(null),
					"Cancel must not replace local settings used by a new print document");
			assertEquals(MediaSizeName.NA_LETTER, document.getPageFormat().getSizeName());
			assertEquals(originalZoomX, document.getTotalZoomX(), 1e-9);
			assertEquals(originalZoomY, document.getTotalZoomY(), 1e-9);
			assertFalse(document.isScaleToSelected());

			GraphPageable nextPrint = document(true);
			assertEquals(MediaSizeName.NA_LETTER, nextPrint.getPageFormat().getSizeName());
			assertEquals(3, nextPrint.getFitToSettings().getRows());
		});
	}

	@Test
	void acceptedSettingsAreReadByANewPrintDocument() {
		withLocalSettings(settings(1, 3, letterLandscape()), () -> {
			GraphPageable preview = document(true);
			PageSetup setup = PageSetup.getInstance(preview);
			setup.createContentPanel(false);
			setRequestedLimits(setup, 2, 4);
			preview.setPageFormat(a5Landscape());
			setup.updateZoom();
			preview.update();
			setup.saveDialogSettings(); // The PageSetupDialog OK path.

			GraphPageable print = document(true);
			assertEquals(MediaSizeName.ISO_A5, print.getPageFormat().getSizeName());
			assertEquals(2, print.getFitToSettings().getColumns());
			assertEquals(4, print.getFitToSettings().getRows());
			assertFalse(print.isScaleToSelected());
			assertEquals(preview.getPrintService().getName(), print.getPrintService().getName());
			assertEquals(preview.getTotalZoomX(), print.getTotalZoomX(), 1e-9);
			assertEquals(preview.getTotalZoomY(), print.getTotalZoomY(), 1e-9);
		});
	}

	private static void layoutAtPreferredSize(JPanel panel) {
		panel.setSize(panel.getPreferredSize());
		panel.doLayout();
		for (Component child : panel.getComponents())
			if (child instanceof JPanel nested)
				layoutAtPreferredSize(nested);
	}

	private static void withLocalSettings(PrintSettings settings, Runnable test) {
		PrintSettings original = (PrintSettings) PrintSettingsManager.getSettings(null).clone();
		try {
			PrintSettingsManager.saveSettings(settings, null, false);
			test.run();
		} finally {
			PrintSettingsManager.saveSettings(original, null, false);
		}
	}

	private static void setRequestedLimits(PageSetup setup, int columns, int rows) {
		setup.beginUpdate();
		setup.fitToWidth.setValue(columns);
		setup.fitToHeight.setValue(rows);
		setup.endUpdate();
	}

	private static PrintSettings settings(int columns, int rows, ExtendedPageFormat pageFormat) {
		PrintSettings settings = new PrintSettings();
		settings.setPageFormat(pageFormat);
		settings.setPdfService(true);
		ArrayList<ScalingSettings> scales = new ArrayList<>(List.of(new ScaleToSettings(), fitTo(columns, rows)));
		settings.setScalingSettings(scales);
		settings.setScalingIndex(1);
		return settings;
	}

	private static FitToSettings fitTo(int columns, int rows) {
		FitToSettings fit = new FitToSettings();
		fit.setColumns(columns);
		fit.setRows(rows);
		return fit;
	}

	private static ExtendedPageFormat letterLandscape() {
		ExtendedPageFormat format = new ExtendedPageFormat(MediaSizeName.NA_LETTER,
				new MediaPrintableArea(10f, 10f, 195.9f, 259.4f, MediaSize.MM));
		format.setOrientation(PageFormat.LANDSCAPE);
		return format;
	}

	private static ExtendedPageFormat a5Landscape() {
		ExtendedPageFormat format = new ExtendedPageFormat(MediaSizeName.ISO_A5,
				new MediaPrintableArea(8f, 8f, 132f, 194f, MediaSize.MM));
		format.setOrientation(PageFormat.LANDSCAPE);
		return format;
	}

	private static GraphPageable document(boolean localSettings) {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("page-setup", undo), undo);
		project.initialize(false, false);
		for (int index = 0; index < 12; index++) {
			NormalTask task = project.createScriptedTask();
			task.setName("Page setup task " + index);
		}
		project.recalculate();
		var source = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
		var cache = NodeModelCacheFactory.getInstance().createFilteredCache(source, "OfflineGantt", null);
		GanttSVGRenderer renderer = new GanttSVGRenderer();
		renderer.init(project, cache, null, null, -1, true);
		return new GraphPageable(renderer, false, false, localSettings, new PrintService[0], null);
	}
}

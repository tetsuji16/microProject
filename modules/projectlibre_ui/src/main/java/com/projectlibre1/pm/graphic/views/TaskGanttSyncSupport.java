package com.projectlibre1.pm.graphic.views;

import java.awt.Color;
import java.awt.Dimension;
import java.util.SortedSet;

import javax.swing.JScrollPane;
import javax.swing.JViewport;

import com.projectlibre1.graphic.configuration.BarStyles;
import com.projectlibre1.graphic.configuration.BarStyle;
import com.projectlibre1.pm.graphic.gantt.Gantt;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheet;
import com.projectlibre1.util.FlatUiSupport;

/**
 * Shared support for the Gantt view's left/right pane synchronization.
 */
final class TaskGanttSyncSupport {
	private TaskGanttSyncSupport() {
	}

	static int calculateRowHeight(SortedSet<Integer> baseLines, int defaultRowHeight, int baselineHeight) {
		int baselineCount = (baseLines == null || baseLines.isEmpty()) ? 0 : baseLines.last().intValue() + 1;
		return defaultRowHeight + baselineCount * baselineHeight;
	}

	static void applyRowHeight(SpreadSheet spreadSheet, Gantt gantt, int rowHeight) {
		if (spreadSheet != null) {
			spreadSheet.setRowHeight(rowHeight);
		}
		if (gantt != null) {
			gantt.setRowHeight(rowHeight);
			gantt.synchronizeViewportSize();
		}
	}

	static void applySpreadsheetGridStyle(SpreadSheet spreadSheet, Gantt gantt, boolean spreadsheetGridVisible, Color gridLineColor) {
		if (spreadSheet != null) {
			spreadSheet.setGridColor(gridLineColor);
			spreadSheet.setShowHorizontalLines(false);
			spreadSheet.setShowVerticalLines(spreadsheetGridVisible);
			if (spreadSheet.getRowHeader() != null) {
				spreadSheet.getRowHeader().setGridColor(gridLineColor);
				spreadSheet.getRowHeader().repaint();
			}
			spreadSheet.repaint();
		}
		if (gantt != null) {
			gantt.setGridLinesVisible(spreadsheetGridVisible);
		}
	}

	static Color resolveGridLineColor(Gantt gantt) {
		return gantt != null ? gantt.getGridLineColor() : FlatUiSupport.tableGridColor();
	}

	static void synchronizeGanttHeightWithSpreadsheet(JScrollPane rightScrollPane, Dimension spreadsheetSize) {
		if (spreadsheetSize == null || rightScrollPane == null || rightScrollPane.getViewport() == null) {
			return;
		}
		JViewport viewport = rightScrollPane.getViewport();
		if (!(viewport.getView() instanceof Gantt ganttView)) {
			return;
		}
		int height = Math.min(spreadsheetSize.height, ganttView.getScrollableHeight(viewport.getExtentSize().height));
		ganttView.setPreferredSize(new Dimension(viewport.getViewSize().width, height));
		ganttView.clampViewportPosition(viewport, height);
		viewport.revalidate();
	}

	static String getAnnotationFieldId(BarStyles barStyles, String defaultFieldId) {
		if (barStyles == null) {
			return defaultFieldId;
		}
		for (Object object : barStyles.getRows()) {
			if (object instanceof BarStyle style && style.isAnnotation() && style.getBarFormat() != null) {
				String fieldId = style.getBarFormat().getFieldId();
				return fieldId == null ? defaultFieldId : fieldId;
			}
		}
		return defaultFieldId;
	}

}

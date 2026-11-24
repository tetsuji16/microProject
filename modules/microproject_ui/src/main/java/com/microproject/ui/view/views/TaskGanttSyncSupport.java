/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.ui.view.views;

import java.awt.Color;
import java.awt.Dimension;
import java.util.SortedSet;

import javax.swing.JScrollPane;
import javax.swing.JViewport;

import com.microproject.view.configuration.BarStyles;
import com.microproject.view.configuration.BarStyle;
import com.microproject.ui.view.gantt.Gantt;
import com.microproject.ui.view.spreadsheet.SpreadSheet;
import com.microproject.ui.util.FlatUiSupport;
import com.microproject.ui.view.views.RowHeights;

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
		int rowCount = spreadSheet != null ? spreadSheet.getRowCount()
				: gantt != null && gantt.getCache() != null ? gantt.getCache().getSize() : 0;
		applyRowHeight(spreadSheet, gantt, new RowHeights(rowHeight, rowCount));
	}

	static void applyRowHeight(SpreadSheet spreadSheet, Gantt gantt, RowHeights rowHeights) {
		if (spreadSheet != null) {
			spreadSheet.setRowHeight(rowHeights.getDefaultHeight());
			for (int row = 0; row < rowHeights.getRowCount(); row++) {
				spreadSheet.setRowHeight(row, rowHeights.heightOf(row));
			}
		}
		if (gantt != null) {
			gantt.setRowHeight(rowHeights.getDefaultHeight());
			RowHeights ganttRowHeights = gantt.getRowHeights();
			ganttRowHeights.setRowCount(gantt.getCache() == null ? 0 : gantt.getCache().getSize());
			for (int row = 0; row < rowHeights.getRowCount(); row++) {
				if (row < ganttRowHeights.getRowCount() && rowHeights.heightOf(row) != ganttRowHeights.getDefaultHeight()) {
					ganttRowHeights.setRowHeight(row, rowHeights.heightOf(row));
				}
			}
			gantt.synchronizeViewportSize();
		}
	}

	static void applySpreadsheetGridStyle(SpreadSheet spreadSheet, Gantt gantt, boolean spreadsheetGridVisible, Color gridLineColor) {
		if (spreadSheet != null) {
			spreadSheet.setGridColor(gridLineColor);
			spreadSheet.setShowHorizontalLines(spreadsheetGridVisible);
			spreadSheet.setShowVerticalLines(spreadsheetGridVisible);
			if (spreadSheet.getRowHeader() != null) {
				spreadSheet.getRowHeader().setGridColor(gridLineColor);
				spreadSheet.getRowHeader().setShowHorizontalLines(spreadsheetGridVisible);
				spreadSheet.getRowHeader().repaint();
			}
			spreadSheet.repaint();
		}
		if (gantt != null) {
			gantt.setGridLineColor(gridLineColor);
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

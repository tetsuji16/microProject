/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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
package com.microproject.pm.graphic.spreadsheet.renderer;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.border.Border;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.microproject.graphic.configuration.CellFormat;
import com.microproject.util.FlatUiSupport;


/**
 *
 */
public class CellUtility {
	public static void setAppearance(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column, JComponent component){
		CommonSpreadSheetModel model = (CommonSpreadSheetModel) table.getModel();
		GraphicNode node = model.getNode(row);
		CellFormat cellFormat=model.getCellProperties(node);
		boolean editingThisCell = table.isEditing() && table.getEditingRow() == row && table.getEditingColumn() == column;
		boolean activeCell = isActiveCell(table, row, column, hasFocus);
		Color foreground=cellFormat.getForegroundObject();
		Color background=cellFormat.getBackgroundObject();
		Color resolvedForeground=SpreadsheetAppearanceSupport.resolveForeground(foreground);
		Color resolvedBackground=SpreadsheetAppearanceSupport.resolveBodyBackground(background, row);
		component.setForeground(resolvedForeground);
		component.setBackground(resolvedBackground);
		Border baseBorder;
		if (editingThisCell && table.isCellEditable(row, column)) {
			baseBorder = resolveCellBorder(true, false, false);
			component.setForeground(resolveSelectionForeground(foreground));
			component.setBackground(resolveSelectionBackground(background));
		} else if (activeCell) {
			baseBorder = resolveCellBorder(false, true, false);
			component.setForeground(resolveTableForeground(foreground));
			component.setBackground(resolveSelectionBackground(background));
		} else if (isSelected) {
			baseBorder = resolveCellBorder(false, false, true);
			component.setForeground(resolveSelectionForeground(foreground));
			component.setBackground(resolveSelectionBackground(background));
		} else {
			baseBorder = resolveCellBorder(false, false, false);
			int modelColumn = table.convertColumnIndexToModel(column);
			if (modelColumn >= 0 && !model.isCellEditable(row, modelColumn)){
				component.setForeground(FlatUiSupport.spreadsheetReadOnlyForeground());
			}
		}
		component.setBorder(baseBorder);
	}

	public static void setAppearance(CellFormat format, JComponent component){
		Color foreground=format.getForegroundObject();
		component.setForeground(SpreadsheetAppearanceSupport.resolveForeground(foreground));
		Color background=format.getBackgroundObject();
		component.setBackground(SpreadsheetAppearanceSupport.resolveBodyBackground(background, 0));
		component.setBorder(FlatUiSupport.tableCellBorder());
//			if (!model.isRowEditable(row))
//				component.setForeground(Color.GRAY);

	}

	private static Color resolveTableForeground(Color foreground) {
		return foreground == null ? FlatUiSupport.tableForeground() : foreground;
	}

	private static Color resolveTableBackground(Color background) {
		return SpreadsheetAppearanceSupport.resolveBodyBackground(background, 0);
	}

	static Color resolveTableBackground(Color background, int row, boolean selected) {
		if (selected)
			return SpreadsheetAppearanceSupport.resolveSelectionBackground(background);
		return SpreadsheetAppearanceSupport.resolveBodyBackground(background, row);
	}

	private static Color resolveSelectionForeground(Color foreground) {
		return foreground == null ? FlatUiSupport.tableSelectionForeground() : foreground;
	}

	static Color resolveSelectionBackground(Color background) {
		return SpreadsheetAppearanceSupport.resolveSelectionBackground(background);
	}

	static Border resolveCellBorder(boolean editingThisCell, boolean activeCell, boolean selectedCell) {
		if (editingThisCell)
			return FlatUiSupport.spreadsheetEditingCellBorder();
		if (activeCell)
			return FlatUiSupport.spreadsheetActiveCellBorder();
		if (selectedCell)
			return FlatUiSupport.tableCellBorder();
		return FlatUiSupport.tableCellBorder();
	}

	static Border withRowGridOverlay(JTable table, Border baseBorder) {
		if (table == null || !table.getShowHorizontalLines())
			return baseBorder;
		return FlatUiSupport.withRowGridOverlay(baseBorder, table.getGridColor());
	}

	static boolean isActiveCell(JTable table, int row, int column, boolean hasFocus) {
		if (table == null)
			return false;
		// A header selection covers all rows in one column.  JTable still keeps
		// a lead row, but that lead coordinate is not a separately selected cell.
		if (table instanceof CommonSpreadSheet spreadSheet
				? spreadSheet.isHeaderColumnSelectionActive()
				: table.getSelectedColumnCount() == 1 && table.getSelectedRowCount() == table.getRowCount())
			return false;
		/*
		 * Renderer coordinates are view coordinates.  The selection models may
		 * retain a lead index from before a row/column was moved or filtered,
		 * which makes the active-cell border appear one cell away from the
		 * actual selection.  JTable resolves the current view selection for us;
		 * use that same coordinate space as the renderer callback.
		 */
		if (table.getRowSelectionAllowed() && table.getColumnSelectionAllowed()
				&& table.getSelectedRow() == row && table.getSelectedColumn() == column)
			return true;
		return hasFocus && table.getSelectedRow() == row && table.getSelectedColumn() == column;
	}


}



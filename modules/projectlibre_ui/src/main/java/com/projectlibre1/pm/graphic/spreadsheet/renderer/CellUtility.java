/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License 
 * Version 1.0 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public 
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of 
 * software over a computer network and provide for limited attribution for the 
 * Original Developer. In addition, Exhibit A has been modified to be consistent 
 * with Exhibit B. 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the 
 * specific language governing rights and limitations under the License. The 
 * Original Code is ProjectLibre. The Original Developer is the Initial Developer 
 * and is ProjectLibre Inc. All portions of the code written by ProjectLibre are 
 * Copyright (c) 2012-2019. All Rights Reserved. All portions of the code written by 
 * ProjectLibre are Copyright (c) 2012-2019. All Rights Reserved. Contributor 
 * ProjectLibre, Inc.
 *
 * Alternatively, the contents of this file may be used under the terms of the 
 * ProjectLibre End-User License Agreement (the ProjectLibre License) in which case 
 * the provisions of the ProjectLibre License are applicable instead of those above. 
 * If you wish to allow use of your version of this file only under the terms of the 
 * ProjectLibre License and not to allow others to use your version of this file 
 * under the CPAL, indicate your decision by deleting the provisions above and 
 * replace them with the notice and other provisions required by the ProjectLibre 
 * License. If you do not delete the provisions above, a recipient may use your 
 * version of this file under either the CPAL or the ProjectLibre Licenses. 
 *
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text of the notices 
 * in the Source Code files of the Original Code. You should use the text of this 
 * Exhibit A rather than the text found in the Original Code Source Code for Your 
 * Modifications.] 
 *
 * EXHIBIT B. Attribution Information for ProjectLibre required
 *
 * Attribution Copyright Notice: Copyright (c) 2012-2019, ProjectLibre, Inc.
 * Attribution Phrase (not exceeding 10 words): 
 * ProjectLibre, open source project management software.
 * Attribution URL: http://www.projectlibre.com
 * Graphic Image as provided in the Covered Code as file: projectlibre-logo.png with 
 * alternatives listed on http://www.projectlibre.com/logo 
 *
 * Display of Attribution Information is required in Larger Works which are defined 
 * in the CPAL as a work which combines Covered Code or portions thereof with code 
 * not governed by the terms of the CPAL. However, in addition to the other notice 
 * obligations, all copies of the Covered Code in Executable and Source Code form 
 * distributed must, as a form of attribution of the original author, include on 
 * each user interface screen the "ProjectLibre" logo visible to all users. 
 * The ProjectLibre logo should be located horizontally aligned with the menu bar 
 * The 
 * logo must be at least 144 x 31 pixels. When users click on the "ProjectLibre" 
 * logo it must direct them back to http://www.projectlibre.com. 
 *******************************************************************************/
package com.projectlibre1.pm.graphic.spreadsheet.renderer;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.border.Border;
import com.projectlibre1.pm.graphic.model.cache.GraphicNode;
import com.projectlibre1.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.projectlibre1.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.projectlibre1.graphic.configuration.CellFormat;
import com.projectlibre1.util.FlatUiSupport;


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
		component.setBorder(withSpreadsheetGrid(table, baseBorder));
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

	public static Border withSpreadsheetGrid(JTable table, Border baseBorder) {
		Border resolvedBase = baseBorder == null ? BorderFactory.createEmptyBorder() : baseBorder;
		if (table == null)
			return resolvedBase;
		Color separatorColor = table.getGridColor();
		return FlatUiSupport.withRowSeparator(resolvedBase, separatorColor);
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



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

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.plaf.UIResource;

import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetParams;
import com.microproject.field.Field;
import com.microproject.pm.graphic.spreadsheet.renderer.CellUtility;
import com.microproject.util.FlatUiSupport;
/**
 * JTable.BooleanRenderer modified version
 */
public class OfflineCapableBooleanRenderer extends JCheckBox implements OfflineRenderer, UIResource {
	//
	public OfflineCapableBooleanRenderer() {
		super();
		setHorizontalAlignment(JLabel.CENTER);
		setBorderPainted(true);
	}

	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		if (table!=null){
			boolean activeCell = CellUtility.isActiveCell(table, row, column, hasFocus);
			if (isSelected) {
				setForeground(table.getSelectionForeground());
				super.setBackground(table.getSelectionBackground());
			}
			else {
				setForeground(table.getForeground());
				setBackground(table.getBackground());
			}
			if (activeCell) {
				setForeground(table.getForeground());
			}
		}
		setSelected((value != null && ((Boolean)value).booleanValue()));

		if (table != null && table.isEditing() && table.getEditingRow() == row && table.getEditingColumn() == column) {
			setBorder(FlatUiSupport.spreadsheetEditingCellBorder());
		} else if (CellUtility.isActiveCell(table, row, column, hasFocus)) {
			setBorder(FlatUiSupport.spreadsheetActiveCellBorder());
		} else {
			setBorder(FlatUiSupport.tableCellBorder());
		}

		return this;
	}

	public Component getComponent(Object value, GraphicNode node, Field field,
			SpreadSheetParams params) {
		return getTableCellRendererComponent(null, value, false, false, -1, -1);
	}

}


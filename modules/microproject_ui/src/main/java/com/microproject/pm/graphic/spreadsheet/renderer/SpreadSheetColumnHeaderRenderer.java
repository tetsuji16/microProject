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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetParams;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.microproject.field.Field;
import com.microproject.util.FlatUiSupport;
/**
 *
 */
public class SpreadSheetColumnHeaderRenderer extends DefaultTableCellRenderer implements OfflineRenderer{
	private static final long serialVersionUID = 8545499263038985045L;
	JLabel override = null;
	/**
	 *
	 */
	public SpreadSheetColumnHeaderRenderer(JLabel override) {
		super();
		this.override = override;
	}

	public SpreadSheetColumnHeaderRenderer() {
		super();
	}
	Component last=null;
	public Component getTableCellRendererComponent (JTable table, Object value,boolean isSelected, boolean hasFocus, int row, int column){
		JLabel component;
		boolean active = false;
		if (override != null)
			component = override;
		else if (table==null){
			setValue(null);
			component=this;
		} else component =(JLabel)super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);


		if (table!=null){
			active = table instanceof CommonSpreadSheet spreadSheet
					? spreadSheet.getActiveHeaderColumn() == column
					: table.getSelectedColumn() == column;
			FlatUiSupport.applyTableHeaderCellStyle(component, isSelected, active);
			Border border = active ? FlatUiSupport.spreadsheetActiveCellBorder() : FlatUiSupport.tableHeaderBorder();
			component.setBorder(border);
		}

		component.setHorizontalAlignment (CENTER);
		if (override == null) {
			component.setText (value == null ? "" : value.toString ());
		}
		return component;
	}


	public Component getComponent(Object value, GraphicNode node,Field field,SpreadSheetParams params){
		JComponent component=(JComponent)getTableCellRendererComponent(null, value, false, false, -1, -1);
		component.setBorder(null);
		return component;
	}

}

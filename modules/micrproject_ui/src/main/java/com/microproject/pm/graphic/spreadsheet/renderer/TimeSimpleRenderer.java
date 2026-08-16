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
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetParams;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.microproject.datatype.Duration;
import com.microproject.field.Field;

/**
 *
 */
public class TimeSimpleRenderer extends DefaultTableCellRenderer/*ContextSensitiveCellRenderer*/   implements OfflineRenderer{
	static Font boldFont = null;
	
	/**
	 * 
	 */
	public TimeSimpleRenderer() {
		super();
	}
	
	/**
	 * @see javax.swing.table.TableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
	 */
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		JLabel component;
		if (table==null){
			setValue(null);
			component=this;
		}
		else{
			component=(JLabel)super.getTableCellRendererComponent(table, value, isSelected,hasFocus, row, column);
		    CommonSpreadSheetModel model=(CommonSpreadSheetModel)table.getModel();
		    FontManager.setComponentFont(model.getCellProperties(model.getNode(row)),component);
		}
		
		if (value!=null&& value instanceof Duration){
			Duration duration=(Duration)value;
			component.setText(duration+"");
		}
		
		if (value instanceof String) component.setHorizontalAlignment(SwingConstants.LEFT);
		else component.setHorizontalAlignment(SwingConstants.RIGHT);
		return component;
	}
	
	public Component getComponent(Object value, GraphicNode node,Field field,SpreadSheetParams params){
		Component component=getTableCellRendererComponent(null, value, false, false, -1, -1);
	    //set font here since it's not set in getTableCellRendererComponent
		FontManager.setComponentFont(params.getFieldArray().getCellStyle().getCellFormat(node),component);
		return component;
	}

	
}


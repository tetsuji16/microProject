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
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.microproject.dialog.util.LookupField;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetParams;
import com.microproject.field.Field;

public class LookupRenderer extends DefaultTableCellRenderer implements OfflineRenderer{
	private static final long serialVersionUID = -1;
	
	public LookupRenderer() {
		super();
	}
	
	
	
	public Component getTableCellRendererComponent (JTable table, Object value,boolean isSelected, boolean hasFocus, int row, int column){
		SpreadSheetModel model  = (SpreadSheetModel) table.getModel();
		Object rowObject = model.getObjectInRow(row);
		Field field = model.getFieldInViewColumn(column);
		JComponent c = new LookupField(field,value).getDisplay(); // not bothering with button for now
		return c;
	}
	
	
	public Component getComponent(Object value, GraphicNode node,Field field,SpreadSheetParams params){
		Component component=getTableCellRendererComponent(null, value, false, false, -1, -1);
		return component;
	}

}


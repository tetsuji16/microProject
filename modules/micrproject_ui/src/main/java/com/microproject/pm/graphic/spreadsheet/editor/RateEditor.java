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
package com.microproject.pm.graphic.spreadsheet.editor;

import java.awt.Component;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.JTable;
import javax.swing.JTextField;

import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.renderer.RateRenderer;
import com.microproject.datatype.CanSupplyRateUnit;
import com.microproject.datatype.RateFormat;

/**
 *
 */
public class RateEditor extends SimpleEditor {
	// NumberFormat/DecimalFormat is not thread-safe; use a fresh instance per call (issue #184).
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean arg2, int row, int column) {
		SpreadSheetModel model  = (SpreadSheetModel) table.getModel();
		Object rowObject = model.getObjectInRow(row);
		boolean labor = true;
		if (rowObject instanceof CanSupplyRateUnit) 
			labor = ((CanSupplyRateUnit)rowObject).getTimeUnitLabel() == null;
		if (!labor) {
			percent = false;
			temporal =false;
		}
		
		JTextField result = (JTextField) super.getTableCellEditorComponent(table, value, arg2, row, column);
		result.setText(RateRenderer.getFormat(table,row,column).format(value));
		return result;
	}
	String timeUnit =null;
	boolean money = false;
	boolean percent = false;
	boolean temporal = true;
	/**
	 * @param timeUnit - For material resource unit, otherwise use null
	 * 
	 */
	public RateEditor(String timeUnit, boolean money, boolean percent, boolean temporal) {
		super();
//		System.out.println("RateEditor timeUnit="+timeUnit + " money="+money);
		this.money = money;
		this.percent = percent;
		this.temporal = temporal;
		this.timeUnit = timeUnit;
	}


	/**
	 * @see javax.swing.CellEditor#getCellEditorValue()
	 */
	public Object getCellEditorValue() {
		try { // try parsing as currency
			return RateFormat.getInstance(timeUnit, money,percent,temporal).parseObject(component.getText());
		} catch (ParseException e) {
		}
		try { // try parsing as number
			return NumberFormat.getNumberInstance().parseObject(component.getText());
		} catch (ParseException e1) {
		}
		//neither parse was good
		return null;
	}
}


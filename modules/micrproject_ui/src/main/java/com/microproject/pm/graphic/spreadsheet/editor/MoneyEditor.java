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

import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;

import com.microproject.datatype.Money;

/**
 *
 */
public class MoneyEditor extends SimpleEditor {
	private static NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance();
	
//	public Component getTableCellEditorComponent(JTable arg0, Object value,
//			boolean arg2, int arg3, int arg4) {
//		JTextField result = (JTextField) super.getTableCellEditorComponent(arg0, value, arg2, arg3, arg4);
//		result.setText(RateRenderer.getRateFormatter().format(value));
//		result.selectAll();
//		return result;
//	}
	/**
	 * 
	 */
	public MoneyEditor() {
		super();
	}

	public MoneyEditor(Class arg0) {
		super(arg0);
	}

	public MoneyEditor(Class arg0, Format arg1) {
		super(arg0, arg1);
	}
	/**
	 * @see javax.swing.CellEditor#getCellEditorValue()
	 */
	public Object getCellEditorValue() {
		try { // try parsing as currency
			return Money.getMoneyFormatInstance().parseObject(component.getText());
		} catch (ParseException e) {
		}
		try { // try parsing as number
			return NUMBER_FORMAT.parseObject(component.getText());
		} catch (ParseException e1) {
		}
		//neither parse was good
		return null;
	}
}


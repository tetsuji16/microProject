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

import com.microproject.datatype.PercentFormat;

public class PercentEditor extends SimpleEditor {
	private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance();

	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		JTextField result = (JTextField)super.getTableCellEditorComponent(table, value, isSelected, row, column);
		result.setText(value == null ? "" : PercentFormat.getInstance().format(value));
		result.setHorizontalAlignment(JTextField.RIGHT);
		result.selectAll();
		return result;
	}

	public Object getCellEditorValue() {
		String text = component.getText();
		if (text == null)
			return null;
		text = text.trim();
		if (text.length() == 0)
			return null;
		try {
			Number parsed;
			if (text.indexOf('%') >= 0) {
				parsed = (Number)PercentFormat.getInstance().parseObject(text);
			} else {
				parsed = NUMBER_FORMAT.parse(text);
				double value = parsed.doubleValue();
				parsed = Double.valueOf(Math.abs(value) <= 1.0D ? value : value / 100.0D);
			}
			double value = parsed.doubleValue();
			if (value < 0.0D)
				value = 0.0D;
			if (value > 1.0D)
				value = 1.0D;
			return Double.valueOf(value);
		} catch (ParseException e) {
			return null;
		}
	}
}

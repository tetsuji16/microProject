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

import javax.swing.ComboBoxModel;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;

import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.field.Field;

/**
 *
 */
public class SimpleComboBoxEditor extends DefaultCellEditor  {
	JTable cachedTable = null;
	JComboBox component;
	String oldValue;
	Field field;
	/**
	 * @param arg0
	 */
	public SimpleComboBoxEditor(ComboBoxModel arg0) {
		super(new JComboBox(arg0));
	}
	
	public Component getTableCellEditorComponent(JTable arg0, Object arg1,
			boolean arg2, int row, int column) {
		cachedTable = arg0;
		component=(JComboBox)super.getTableCellEditorComponent(arg0, arg1, arg2, row, column);
		field = ((SpreadSheetModel)arg0.getModel()).getFieldInViewColumn(column);
		component.setEditable(field.hasDynamicSelect()); // don't allow the user to type a choice if field has fixed values
		JTextField f = (JTextField) component.getEditor().getEditorComponent();

		oldValue = f.getText();
		return component;
	}

	
}


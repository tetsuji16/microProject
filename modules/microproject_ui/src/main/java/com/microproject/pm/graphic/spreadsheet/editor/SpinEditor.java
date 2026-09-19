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
import java.util.Locale;

import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.JSpinner.NumberEditor;

import com.microproject.configuration.Settings;
import com.microproject.datatype.PercentFormat;
import com.microproject.datatype.Rate;
import com.microproject.field.Field;
import com.microproject.field.Range;
import com.microproject.util.MathUtils;

/**
 * A spinner in a spreadsheet or dialog
 */
public class SpinEditor extends SimpleEditor {
	private Double defaultValue = Double.valueOf(1.0D);
	private static double MAX_VALUE = 60000000.0; 
	private static final String NUMBER_TEMPLATE="#######################";
	Field field;
	KeyboardFocusSpinner spin;

	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean arg2, int arg3, int arg4) {
		
		cachedTable = table;
		if (value == null || (field.isPercent() && value instanceof Double && PercentFormat.isSpecialValue((Double)value))) {
			spin.setValue(defaultValue);
		} else {
			if (value instanceof Rate)
				value = Double.valueOf(((Rate)value).getValue());
			spin.setValue(value);
		}
		return spin;
	}

/** A static function to get a JSpinner.  This may be used outside a spreadsheet
 * 
 * @param field
 * @param value
 * @return
 */
	public static JSpinner getJSpinnerInstance(Field field, double value, boolean inSpreadSheet) {
		double min = 0;
		double max = MAX_VALUE;
		double step = 0.5;
		double spinnerValue = value;
		Range range = field.getRange();
		if (range != null) {
			max = range.getMaximum();
			min = range.getMinimum();
			step = range.getStep();
			spinnerValue = Math.max(min,Math.min(value,max)); // put in range
		}
		
		
		// Set focus to editor always
		JSpinner spinner;
		if (inSpreadSheet)
			spinner = new KeyboardFocusSpinner(new SpinnerNumberModel(spinnerValue,min,max,step));
		else
			spinner = new JSpinner(new SpinnerNumberModel(spinnerValue,min,max,step));
		
		String template = NUMBER_TEMPLATE.substring(Double.toString(max).length()); // enough space to hold biggest
		JSpinner.NumberEditor editor;
		if (field.isPercent())
			editor = new JSpinner.NumberEditor(spinner,template + Settings.PERCENT);
		else
			editor = new JSpinner.NumberEditor(spinner,template);
		spinner.setEditor(editor);
		editor.getTextField().setHorizontalAlignment(JTextField.RIGHT);
		return spinner;
	}
	
	public static Object getValue(JSpinner spinner, Field field) {
		JSpinner.NumberEditor editor = (NumberEditor) spinner.getEditor();
		Object value = null;
		try {
			if (field.isPercent()) {
				value = PercentFormat.getInstance().parseObject(editor.getTextField().getText());
//	JSpinner screws up and sometimes adds a small fraction to the value.  Round it to get rid of it.  Example, 15% shows up with a miniscule .000000000000000002 at the end
				value = Double.valueOf(MathUtils.roundToDecentPrecision(((Number) value).doubleValue()));
			}
			else
				value = NumberFormat.getInstance(Locale.getDefault()).parseObject(editor.getTextField().getText());
		} catch (ParseException e) {
			return null;
		}
		if (field.isRate())
			value = new Rate(((Number)value).doubleValue());
		return value;
	}

	public SpinEditor(Field field) {
		super();
		this.field = field;
		spin = (KeyboardFocusSpinner) getJSpinnerInstance(field,0.0,true);
	}

	/**
	 * @see javax.swing.CellEditor#getCellEditorValue()
	 */
	public Object getCellEditorValue() {
		return getValue(spin,field);
	}
	
	public void cancelCellEditing() {
		super.cancelCellEditing();
	}

	public boolean stopCellEditing() {
		fireEditingStopped(); 
		if (handledPostErrorFocus()) {
			spin.getTextField().setValue(spin.getValue()); // put back old value
			return false;
		}
		return true;
	};
	
	
}

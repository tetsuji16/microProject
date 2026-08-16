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
import java.text.Format;
import java.text.ParseException;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;

import com.microproject.dialog.util.FixedSizeFilter;
import com.microproject.pm.graphic.ChangeAwareTextField;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.microproject.datatype.CanSupplyRateUnit;
import com.microproject.datatype.DurationFormat;
import com.microproject.field.Field;
import com.microproject.field.FieldConverter;
import com.microproject.field.FieldParseException;
/**
 *
 */
public class SimpleEditor extends DefaultCellEditor   {
	protected ChangeAwareTextField component;
	protected Class clazz;
	protected Format useFormat = null;
	JTable cachedTable = null;
	/**
	 * 
	 */
	public SimpleEditor() {
		super(new ChangeAwareTextField());
		component = (ChangeAwareTextField) getComponent();
		clazz = String.class;
		
	}
	public SimpleEditor(Class clazz) {
		this();
		this.clazz=clazz;
	}
	public SimpleEditor(Class arg0, Format arg1) {
		this(arg0);		
		useFormat = arg1;
	}
	
	
	
	
	/**
	 * @see javax.swing.table.TableCellEditor#getTableCellEditorComponent(javax.swing.JTable, java.lang.Object, boolean, int, int)
	 */
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean arg2, int row, int column) {
		cachedTable = table;
		useFormat = null;
		String stringValue;
		if(value==null)
			stringValue=null;
		else 
			stringValue=FieldConverter.toString(value);
		
		component.setText(stringValue);
		//component.resetChange();
		if (table.getModel() instanceof SpreadSheetModel) {
			SpreadSheetModel model  = (SpreadSheetModel) table.getModel();
			Field field = model.getFieldInViewColumn(column);
			int width = field.getTextWidth(null,null);
			((AbstractDocument)component.getDocument()).setDocumentFilter(null);
			if (width != Integer.MAX_VALUE) {
				((AbstractDocument)component.getDocument()).setDocumentFilter(new FixedSizeFilter(width));
			}
			component.setHorizontalAlignment(field.getHorizontalAlignment());
			if (field.isWork()) {
				Object rowObject = model.getObjectInRow(row);
				if (rowObject instanceof CanSupplyRateUnit && ((CanSupplyRateUnit)rowObject).isMaterial())
					useFormat = DurationFormat.getNonTemporalWorkInstance();
			}
		} else {		
			if (value == null || value instanceof String ) 
				component.setHorizontalAlignment(JTextField.LEFT);
			else 
				component.setHorizontalAlignment(JTextField.RIGHT);
		}
		component.selectAll();
		return component;
	}
	

	public Object getCellEditorValue() {
		if (useFormat == null)
			try {
				return FieldConverter.convert(component.getText(),clazz,null);
			} catch (FieldParseException e1) {
				return null;
			}
		else
			try {
				return useFormat.parseObject(component.getText());
			} catch (ParseException e) {
				return null;
			}
	}
	
	
	
	
	public boolean stopCellEditing() {
		if (component.hasChanged()) {
			boolean result = super.stopCellEditing();
			if (handledPostErrorFocus())
				return false;
			return result;	
		}else{
			cancelCellEditing();
			return true;
		}
	}
	
	public void cancelCellEditing() {
		super.cancelCellEditing();
	}

	protected boolean handledPostErrorFocus() {
		if (cachedTable != null && cachedTable instanceof CommonSpreadSheet) {
			if (((CommonSpreadSheet)cachedTable).getLastException() != null) {
				cachedTable.requestFocus();
				return true;
			}
		}
		return false;

	}
	

	
}


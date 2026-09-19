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

import java.awt.Color;
import java.awt.Component;
import java.awt.event.FocusEvent;
import java.text.Format;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;

import com.microproject.pm.graphic.ChangeAwareTextField;
import com.microproject.datatype.Money;
import com.microproject.field.FieldConverter;
import com.microproject.util.FlatUiSupport;

/**
 *
 */
public class TimeSimpleEditor extends DefaultCellEditor {
	protected ChangeAwareTextField component;
	protected Class clazz;
	protected Format useFormat = null;
	/**
	 * 
	 */
	public TimeSimpleEditor() {
		super(new ChangeAwareTextField());
		component = (ChangeAwareTextField) getComponent();
		clazz = String.class;
	}
	public TimeSimpleEditor(Class clazz) {
		this();
		this.clazz=clazz;
	}
	public TimeSimpleEditor(Class arg0, Format arg1) {
		this(arg0);		
		useFormat = arg1;
	}
	
	
	
	
	/**
	 * @see javax.swing.table.TableCellEditor#getTableCellEditorComponent(javax.swing.JTable, java.lang.Object, boolean, int, int)
	 */
	public Component getTableCellEditorComponent(JTable arg0, Object value,
			boolean arg2, int arg3, int arg4) {
		//component=(JTextField)super.getTableCellEditorComponent(arg0, value, arg2, arg3, arg4);
		String stringValue;
		if(value==null)
			stringValue=null;
//		else if (value instanceof Money) 
//			//this should be handled with an ObjectConverter in the
//			//editor specific context
//			stringValue=value.toString();
		else stringValue=FieldConverter.toString(value);
		component.setText(stringValue);
		//component.resetChange();
		component.setBorder(FlatUiSupport.tableEditorBorder());
		component.setBackground(FlatUiSupport.tableBackground());
		component.setSelectedTextColor(FlatUiSupport.tableSelectionForeground());
		component.setSelectionColor(FlatUiSupport.tableSelectionBackground());
		component.setHorizontalAlignment(JTextField.RIGHT);
		return component;
	}
	
	/**
	 * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
	 */
	public void focusGained(FocusEvent arg0) {
	}
	/**
	 * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
	 */
	public void focusLost(FocusEvent arg0) {
	}
	
	
	public Object getCellEditorValue() {
		Object value;
		if (Money.class.equals(clazz)){
			try {
				//this should be handled with an ObjectConverter in the
				//editor specific context
				value=new Money(component.getText());
			} catch (NumberFormatException e) {
				value=null; //to force an error popup
			}
		}else value=FieldConverter.fromString(component.getText(),clazz);
		return value;
	}
	
	
	
	
	public boolean stopCellEditing() {
		if (component.hasChanged())
			return super.stopCellEditing();
		else{
			cancelCellEditing();
			return true;
		}
	}
	
	
	
	
	
	public void cancelCellEditing() {
		super.cancelCellEditing();
	}
}


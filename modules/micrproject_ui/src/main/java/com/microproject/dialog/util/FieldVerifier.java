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
package com.microproject.dialog.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.undo.UndoableEditSupport;

import com.microproject.dialog.FieldDialog;
import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.field.FieldParseException;
import com.microproject.field.ObjectRef;
import com.microproject.field.Select;
import com.microproject.options.EditOption;
import com.microproject.undo.FieldEdit;
import com.microproject.util.Alert;
import com.microproject.util.DateFieldSupport;
import com.microproject.util.FlatUiSupport;

/**
 *
 */
@SuppressWarnings("deprecation")
public class FieldVerifier extends InputVerifier {
	protected FieldContext context = null;
	protected Field field;
	protected ObjectRef objectRef = null;
	protected Object source;
	protected Object value;
	protected Exception exception = null;
	protected boolean updating = false;
	boolean testing = false;
//	private UndoableEditSupport undoableEditSupport;
	/**
	 * @param value the value that should be compared against the current field state
	 * 
	 */
	public FieldVerifier(Field field, ObjectRef objectRef, Object value/*,UndoableEditSupport undoableEditSupport*/) {
		super();
		this.field = field;
		this.objectRef = objectRef;
		setValue(value);
		this.source = this;
//		this.undoableEditSupport=undoableEditSupport;
	}
	
	/**
	 * Get the top level component.  For dates and spinners, the verification is triggered on a grandchild.  We need the grandparent
	 * @param component
	 * @return
	 */
	static JComponent valueHoldingComponent(JComponent component) {
		Object p = component.getParent();
		// for spinners and dates, need to go up to grandparent to get the control which holds the value
		if (p != null && p instanceof LookupField)
			p = ((LookupField)p).getDisplay();
		else if (p != null && p instanceof Component)
			p = ((Component)p).getParent();
		if (p instanceof JSpinner || p instanceof ExtDateField)
			component = (JComponent) p;
		return component;
	}


	
	public boolean verify(JComponent component) {
		if (updating) {
			return true;
		}

		FieldDialog parentFieldDialog = ComponentFactory.getParentFieldDialog(component);
		if (parentFieldDialog != null)
			parentFieldDialog.setDirtyComponent(null);
		
		
		JComponent c = component;
		component = valueHoldingComponent(component);
		
		component.setForeground(FlatUiSupport.infoForeground());
		c.setForeground(FlatUiSupport.infoForeground());
		
		Object newValue = ComponentFactory.getValueFromComponent(component, field);
//System.out.println("new value " + newValue + " " + (newValue != null ?newValue.getClass():""));
		
		// avoid validating unchanged controls
		if (newValue == value || (newValue != null && newValue.equals(value))) { //unchanged
			if (component instanceof JSpinner || component instanceof ExtDateField) { // if a spinner, check for modified text
				String text = ((JTextField)c).getText();
				try {
					if (component instanceof ExtDateField) {
						if (text.trim().length() > 0) {
							newValue = DateFieldSupport.parseYearless(text, ((ExtDateField) component).getDateFormat(), null);
						} else {
							((JTextField)c).setText(""); // put in empty text
							newValue = null;
						}
					} else {
						newValue = field.getFormat().parseObject(text);
					}
				} catch (ParseException e1) {
					exception = new FieldParseException(field.syntaxErrorForField());
					component.setForeground(FlatUiSupport.errorForeground());
					c.setForeground(FlatUiSupport.errorForeground());
					if (parentFieldDialog != null)
						parentFieldDialog.setDirtyComponent(c);

					return false;
				}
			} else {
				return true;
			}
		}
		if (newValue != null && value != null && newValue.toString().equals(value.toString()))
			return true;
		
		exception = null;
		try {
			if (field.hasOptions())  {
				if (newValue == null)
					newValue = Select.EMPTY;
				
				if (!(objectRef instanceof FieldComponentMap map)
						|| !map.write(field, source, newValue.toString(), context, true))
					field.setText(objectRef,newValue.toString(),context);
			} else {
				if (field.isDate()) {
					if (newValue != null && newValue instanceof String) {
						try {
							newValue = EditOption.getInstance().getDateFormat().parseObject((String) newValue);
						} catch (ParseException e) {
						}
					}
					if (newValue == null || newValue.toString().trim().equals("")) // empty text on date is a null date
						newValue = com.microproject.util.DateTime.getZeroDate();
				}
				if (newValue != value){
					Object oldValue=field.getValue(objectRef, context);
					boolean routed = objectRef instanceof FieldComponentMap map
							&& map.write(field, source, newValue, context, field.isMoney());
					if (!routed) {
						if (field.isMoney()) field.setText(objectRef,""+newValue,context);
						else field.setValue(objectRef,source,newValue,context);
						UndoableEditSupport undoableEditSupport=objectRef.getDataFactory().getUndoController().getEditSupport();
						if (undoableEditSupport!=null)
							undoableEditSupport.postEdit(new FieldEdit(field,objectRef,value,oldValue,this,context));
					}
				}
				
			}
		} catch (FieldParseException e) {
			exception = e;
			component.setForeground(FlatUiSupport.errorForeground());
			c.setForeground(FlatUiSupport.errorForeground());
			if (parentFieldDialog != null)
				parentFieldDialog.setDirtyComponent(c);
			return false;
		}
		setValue(newValue); // set to new value for next time
		return true;
	}

	
	public boolean shouldYieldFocus(JComponent arg0) {
		if (testing) // sempaphore to protect infinite focus loop when popping up error dialog.  Does not need to be synchronized since the verifier is not shared
			return true;
		testing = true;
		boolean result = super.shouldYieldFocus(arg0);
		if (result == false)
			Alert.error(exception.getMessage(),arg0);
		testing = false;
		return result;
	}
	/**
	 * @param value The value to set.
	 */
	void setValue(Object value) {
		this.value = value;
	}

	/** A generic listener class that will validate on an event */
	public static class VerifierListener implements ActionListener {
	    public void actionPerformed(ActionEvent e){
	    	JComponent c = (JComponent)e.getSource();
	    	InputVerifier v = c.getInputVerifier();
	    	if (v != null) // on init, it is null
	    		v.verify(c);
	    }
	}
	public final void setUpdating(boolean doNotVerify) {
		this.updating = doNotVerify;
	}
	final Object getValue() {
		return value;
	}

	/**
	 * @return
	 */
	final boolean isUpdating() {
		return updating;
	}
	public final Exception getException() {
		return exception;
	}
}

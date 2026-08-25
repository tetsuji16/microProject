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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.field.FieldParseException;
import com.microproject.field.ObjectRef;
import com.microproject.util.Alert;

/**
 * Currently only supports Buttons!
 */
public class FieldChangeListener implements ItemListener,ChangeListener {
	private static final Logger logger = Logger.getLogger(FieldChangeListener.class.getName());
	private FieldContext context = null;
	private Field field;
	private ObjectRef objectRef = null;
	/**
	 * Creates a listener that writes the selected value back to the given field.
	 * 
	 */
	public FieldChangeListener(Field field, ObjectRef objectRef) {
		super();
		this.field = field;
		this.objectRef = objectRef;
	}

	public void itemStateChanged(ItemEvent evt) {
		Object source = evt.getSource();
		Boolean value = (evt.getStateChange() == ItemEvent.SELECTED) ? Boolean.TRUE : Boolean.FALSE;
		try {
			if (!(objectRef instanceof FieldComponentMap map) || !map.write(field, source, value, context, false))
				field.setValue(objectRef,source,value,context);
		} catch (FieldParseException e) {
			Alert.error(e.getMessage());
			((JComponent)source).requestFocus();
		}
		
	}

	public void stateChanged(ChangeEvent e) {
		if (e.getSource() instanceof LookupField) {
			LookupField f = (LookupField)e.getSource();
			try {
				if (!(objectRef instanceof FieldComponentMap map)
						|| !map.write(field, f, f.getValue(), context, true))
					field.setText(objectRef,f.getValue(),context);
			} catch (FieldParseException e1) {
				logger.log(Level.WARNING, "Failed to update field from lookup value", e1);
			}
		}
	}

}

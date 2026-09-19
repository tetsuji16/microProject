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
package com.microproject.undo;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.field.FieldParseException;

/**
 *
 */
public class AssignmentSetFieldsEdit extends AbstractUndoableEdit{
	protected Field field;
	protected Object object;
	protected Object value,oldValue;
	protected FieldContext context;
	protected Object source;
	
	/**
	 * @param field
	 * @param object
	 * @param value
	 * @param oldValue
	 * @param context
	 */
	public AssignmentSetFieldsEdit(Field field,Object object,
			Object source,Object value, Object oldValue,
			FieldContext context) {
		super();
		this.field = field;
		this.object=object;
		this.value = value;
		this.oldValue = oldValue;
		this.context = context;
		this.source=source;
	}
	public void redo() throws CannotRedoException {
		super.redo();
		try {
			field.setValue(object, this, value, context);
		} catch (FieldParseException e) {
			CannotRedoException failure = new CannotRedoException();
			failure.initCause(e);
			throw failure;
		}
	}
	public void undo() throws CannotUndoException {
		super.undo();
		try {
			field.setValue(object, this, oldValue, context);
		} catch (FieldParseException e) {
			CannotUndoException failure = new CannotUndoException();
			failure.initCause(e);
			throw failure;
		}
	}
	public String getPresentationName() {
		return "AssignmentSetFields";
	}
}

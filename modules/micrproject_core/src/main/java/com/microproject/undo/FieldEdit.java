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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.field.FieldParseException;
import com.microproject.field.ObjectRef;

/**
 *
 */
public class FieldEdit extends AbstractUndoableEdit{
	private static final Logger logger = Logger.getLogger(FieldEdit.class.getName());
	protected Field field;
	protected Object object,value,oldValue,source;
	protected FieldContext context;
	
	public FieldEdit(Field field, Object object, Object value, Object oldValue, Object source, FieldContext context) {
		super();
		this.field = field;
		this.object = object;
		this.value = value;
		this.oldValue = oldValue;
		this.source = source;
		this.context = context;
	}
	public boolean canRedo() {
		return super.canRedo();
	}
	public boolean canUndo() {
		return super.canUndo();
	}
	public String getPresentationName() {
		return "Field (id=" + field.getId() + ", obj=" + object + ", value=" + value + ", oldValue=" + oldValue + ")";
	}
	public void redo() throws CannotRedoException {
		if (!canRedo()) throw new CannotRedoException();
		try {
			if (object instanceof ObjectRef) field.setValue((ObjectRef)object,source,value,context);
			else field.setValue(object,source,value,context);
		} catch (FieldParseException e) {
			logger.log(Level.WARNING, "Failed to redo field edit", e);
			CannotRedoException failure = new CannotRedoException();
			failure.initCause(e);
			throw failure;
		}
		super.redo();
	}
	public void undo() throws CannotUndoException {
		if (!canUndo()) throw new CannotUndoException();
		try {
			if (object instanceof ObjectRef) field.setValue((ObjectRef)object,source,oldValue,context);
			else field.setValue(object,source,oldValue,context);
		} catch (FieldParseException e) {
			logger.log(Level.WARNING, "Failed to undo field edit", e);
			CannotUndoException failure = new CannotUndoException();
			failure.initCause(e);
			throw failure;
		}
		super.undo();
	}
}

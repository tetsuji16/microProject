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
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;

/**
 *
 */
public class ModelFieldEdit extends AbstractUndoableEdit{
	private static final Logger logger = Logger.getLogger(ModelFieldEdit.class.getName());
	protected NodeModel model;
	protected Field field;
	protected Node node;
	protected Object value,oldValue;
	protected FieldContext context;
	protected Object source;
	
	/**
	 * @param model
	 * @param field
	 * @param node
	 * @param value
	 * @param oldValue
	 * @param context
	 */
	public ModelFieldEdit(NodeModel model, Field field,
			Node node, Object source,Object value, Object oldValue,
			FieldContext context) {
		super();
		this.model = model;
		this.field = field;
		this.node = node;
		this.value = value;
		this.oldValue = oldValue;
		this.context = context;
		this.source=source;
	}
	public boolean canRedo() {
		return super.canRedo();
	}
	public boolean canUndo() {
		return super.canUndo();
	}
	public String getPresentationName() {
		return "ModelField(id=" + field.getId() + ", node=" + node + ", value=" + value + ", oldValue=" + oldValue + ")";
		
		
	}
	public void redo() throws CannotRedoException {
		super.redo();
		try {
			model.setFieldValue(field,node,source,value,context,NodeModel.EVENT);
		} catch (FieldParseException e) {
			logger.log(Level.WARNING, "Failed to redo model field edit", e);
		}
	}
	public void undo() throws CannotUndoException {
		super.undo();
		try {
			model.setFieldValue(field,node,source,oldValue,context,NodeModel.EVENT);
		} catch (FieldParseException e) {
			logger.log(Level.WARNING, "Failed to undo model field edit", e);
		}
	}
}

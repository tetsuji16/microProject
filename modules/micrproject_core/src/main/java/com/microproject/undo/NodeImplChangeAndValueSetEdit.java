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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
public class NodeImplChangeAndValueSetEdit extends AbstractUndoableEdit{
	private static final Logger logger = Logger.getLogger(NodeImplChangeAndValueSetEdit.class.getName());
	protected NodeModel model;
	protected Node node;
	protected LinkedList previous;
	protected List previousPosition;
	protected Object oldImpl,newImpl;
	protected Field field;
	protected Object value;
	protected FieldContext context;
	protected Object source;
	
	/**
	 * @param model
	 * @param node
	 * @param oldImpl
	 * @param field
	 * @param value
	 * @param context
	 * @param source
	 */
	public NodeImplChangeAndValueSetEdit(NodeModel model, Node node, LinkedList previous, List previousPosition,
			Object oldImpl, Field field, Object value,
			FieldContext context, Object source) {
		super();
		this.model = model;
		this.node = node;
		this.previous = previous;
		this.previousPosition = previousPosition;
		this.oldImpl = oldImpl;
		this.field = field;
		this.value = value;
		this.context = context;
		this.source = source;
		newImpl=node.getImpl();
	}
	public void redo() throws CannotRedoException {
		super.redo();
		try {
			model.replaceImplAndSetFieldValue(node,previous,newImpl,field,source,value,context,NodeModel.EVENT);
		} catch (FieldParseException e) {
			logger.log(Level.WARNING, "Failed to redo node implementation change", e);
		}
	}
	public void undo() throws CannotUndoException {
		super.undo();
		if (previousPosition!=null){
			for (Iterator i=previousPosition.iterator();i.hasNext();){
				Position p=(Position)i.next();
				model.remove(p.child, NodeModel.SILENT);
				model.add(p.parent, p.child, p.index,NodeModel.SILENT);
			}
		}
		Object impl=node.getImpl();
		model.replaceImpl(node,oldImpl,source,NodeModel.EVENT);
		model.getDataFactory().remove(impl,model,false,false,true);
	}
	
	public static class Position{
		Node parent,child;
		int index;
		public Position(Node parent, Node child, int index) {
			super();
			this.parent = parent;
			this.child = child;
			this.index = index;
		}
	}
	public String getPresentationName() {
		return "NodeImplChangeAndValueSet";
	}
	
}

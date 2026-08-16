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

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;

/**
 *
 */
public class NodeImplChangeEdit extends AbstractUndoableEdit{
	protected NodeModel model;
	protected Node node;
	protected Object impl;
	protected Object source;
	
	/**
	 * @param model
	 * @param node
	 * @param impl
	 * @param source
	 */
	public NodeImplChangeEdit(NodeModel model, Node node, Object impl,
			Object source) {
		super();
		this.model = model;
		this.node = node;
		this.impl = impl;
		this.source = source;
	}
	public void redo() throws CannotRedoException {
		super.redo();
		changeImpl();
	}
	public void undo() throws CannotUndoException {
		super.undo();
		changeImpl();
	}
	
	private void changeImpl(){
		Object oldImpl=node.getImpl();
		model.replaceImpl(node,impl,source,NodeModel.EVENT);
		impl=oldImpl;
	}
	public String getPresentationName() {
		return "NodeImplChange";
	}
	
}

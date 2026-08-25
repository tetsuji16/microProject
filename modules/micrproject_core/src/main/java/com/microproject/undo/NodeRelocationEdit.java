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

import java.util.ArrayList;
import java.util.List;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;

/** Undoable, identity-preserving relocation of an outline branch block. */
public class NodeRelocationEdit extends AbstractUndoableEdit {
	private static final long serialVersionUID = 1L;
	private final NodeModel model;
	private final List<Node> nodes;
	private final Node beforeParent;
	private final int beforeIndex;
	private final Node afterParent;
	private final int afterIndex;

	public NodeRelocationEdit(NodeModel model,List<Node> nodes,Node beforeParent,int beforeIndex,
			Node afterParent,int afterIndex){
		this.model=model;
		this.nodes=new ArrayList<Node>(nodes);
		this.beforeParent=beforeParent;
		this.beforeIndex=beforeIndex;
		this.afterParent=afterParent;
		this.afterIndex=afterIndex;
	}

	@Override
	public void undo() throws CannotUndoException {
		if (!canUndo()) throw new CannotUndoException();
		if (!model.relocate(nodes,beforeParent,beforeIndex,NodeModel.EVENT)) throw new CannotUndoException();
		super.undo();
	}

	@Override
	public void redo() throws CannotRedoException {
		if (!canRedo()) throw new CannotRedoException();
		if (!model.relocate(nodes,afterParent,afterIndex,NodeModel.EVENT)) throw new CannotRedoException();
		super.redo();
	}

	@Override
	public String getPresentationName(){
		return "Move Task";
	}
}

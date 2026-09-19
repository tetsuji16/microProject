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

import java.util.LinkedList;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.DefaultNodeModel.RemovalSnapshot;
import com.microproject.grouping.core.model.DefaultNodeModel.RemovalSnapshot.Entry;
import com.microproject.grouping.core.model.DefaultNodeModel.RemovalSnapshot.SubprojectState;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.task.Project;

/**
 *
 */
public class NodeDeletionEdit extends AbstractUndoableEdit{
	protected NodeModel model;
	protected RemovalSnapshot removalSnapshot;
	
	
	
	/**
	 * @param model
	 * @param removalSnapshot immutable deletion state captured before removal
	 */
	public NodeDeletionEdit(NodeModel model, RemovalSnapshot removalSnapshot) {
		super();
		this.model = model;
		this.removalSnapshot = removalSnapshot;
	}
	public void redo() throws CannotRedoException {
		super.redo();
		for (SubprojectState state : removalSnapshot.getSubprojects())
			state.cancelRestore();
		model.remove(removalSnapshot.getNodes(),NodeModel.EVENT);
	}
	public void undo() throws CannotUndoException {
		super.undo();
		for (Entry entry : removalSnapshot.getEntries()) {
			LinkedList nodes=new LinkedList();
			nodes.add(entry.getNode());
			model.paste(entry.getParent(), nodes, entry.getPosition(), NodeModel.EVENT);
		}
		restoreSubprojects();
	}	

	private void restoreSubprojects() {
		if (!(model.getDataFactory() instanceof Project))
			return;
		Project parentProject = (Project) model.getDataFactory();
		for (SubprojectState state : removalSnapshot.getSubprojects())
			state.restoreAfterClose(parentProject);
	}
	public String getPresentationName() {
		return "NodeDeletion";
	}
}

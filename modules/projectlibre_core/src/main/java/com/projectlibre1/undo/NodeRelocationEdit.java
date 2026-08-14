/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License
 * Version 1.0 (the "License"); you may not use this file except in compliance with
 * the License. The Original Code is ProjectLibre.
 *******************************************************************************/
package com.projectlibre1.undo;

import java.util.ArrayList;
import java.util.List;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import com.projectlibre1.grouping.core.Node;
import com.projectlibre1.grouping.core.model.NodeModel;

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
		super.undo();
		if (!model.relocate(nodes,beforeParent,beforeIndex,NodeModel.EVENT)) throw new CannotUndoException();
	}

	@Override
	public void redo() throws CannotRedoException {
		super.redo();
		if (!model.relocate(nodes,afterParent,afterIndex,NodeModel.EVENT)) throw new CannotRedoException();
	}

	@Override
	public String getPresentationName(){
		return "Move Task";
	}
}

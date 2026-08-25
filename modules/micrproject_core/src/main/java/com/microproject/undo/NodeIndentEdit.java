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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;

/**
 *
 */
public class NodeIndentEdit extends AbstractUndoableEdit{
	protected NodeModel model;
	protected List nodes;
	protected int deltaLevel;
	protected List beforePositions;
	protected List afterPositions;
	
	/**
	 * @param hierarchy
	 * @param nodes
	 * @param deltaLevel
	 */
	public NodeIndentEdit(NodeModel model, List nodes, int deltaLevel) {
		super();
		this.model = model;
		this.nodes = nodes;
		this.deltaLevel = deltaLevel;
	}

	public NodeIndentEdit(NodeModel model, List nodes, int deltaLevel, List beforePositions, List afterPositions) {
		this(model, nodes, deltaLevel);
		this.beforePositions = beforePositions;
		this.afterPositions = afterPositions;
	}
	
	public void redo() throws CannotRedoException {
		if (!canRedo()) throw new CannotRedoException();
		if (!restorePositions(afterPositions))
			model.getHierarchy().indent(nodes,deltaLevel,model,NodeModel.EVENT);
		super.redo();
	}
	public void undo() throws CannotUndoException {
		if (!canUndo()) throw new CannotUndoException();
		if (!restorePositions(beforePositions))
			model.getHierarchy().indent(nodes,-deltaLevel,model,NodeModel.EVENT);
		super.undo();
	}	
	public String getPresentationName() {
		return "NodeIndent";
	}

	private boolean restorePositions(List positions) {
		if (positions == null || positions.size() == 0)
			return false;
		List sorted = new ArrayList(positions);
		Collections.sort(sorted, new Comparator() {
			public int compare(Object o1, Object o2) {
				Position p1 = (Position)o1;
				Position p2 = (Position)o2;
				if (p1.parent == p2.parent)
					return p1.index - p2.index;
				return 0;
			}
		});
		for (Iterator i = sorted.iterator(); i.hasNext();) {
			Position position = (Position)i.next();
			if (position == null || position.parent == null || position.node == null)
				continue;
			List one = new ArrayList(1);
			one.add(position.node);
			int index = Math.max(0, Math.min(position.index, position.parent.getChildCount()));
			model.getHierarchy().add(position.parent, one, index, NodeModel.EVENT);
		}
		return true;
	}

	public static final class Position {
		public final Node parent;
		public final Node node;
		public final int index;

		public Position(Node parent, Node node, int index) {
			this.parent = parent;
			this.node = node;
			this.index = index;
		}
	}
}

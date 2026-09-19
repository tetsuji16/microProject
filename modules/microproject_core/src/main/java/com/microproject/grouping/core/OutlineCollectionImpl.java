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
package com.microproject.grouping.core;

import java.util.Collection;

import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.model.NodeModelDataFactory;
import com.microproject.grouping.core.model.NodeModelFactory;

/**
 * Class which implements a collection of outlines 
 */
public class OutlineCollectionImpl implements OutlineCollection {
	private int currentOutline = DEFAULT_OUTLINE;
	private NodeModelDataFactory dataFactory = null;	
	private NodeModel outlines[];


	public OutlineCollectionImpl(int size,NodeModelDataFactory dataFactory) {
		outlines = new NodeModel[size];
		this.dataFactory=dataFactory;
	}
	
	
	public NodeModel getOutline() {
		return getOutline(currentOutline);
	}

	public NodeModel getDefaultOutline() {
		return getOutline(DEFAULT_OUTLINE);
	}

	public NodeModel getOutline(int outlineNumber) {
		NodeModel outline = outlines[outlineNumber];
		if (outline == null) {
			outline =  NodeModelFactory.getInstance().createNodeModel(dataFactory);
			outlines[outlineNumber] = outline;
			if (dataFactory!=null){
				dataFactory.initOutline(outline);
				outline.setUndoController(dataFactory.getUndoController());
			}
		}
		return outline;
	}
	
	//for internal use (undo)
	public NodeModel[] getOutlines(){
		return outlines;
	}
	
	/**
	 * @return Returns the currentHierarchy.
	 */
	public int getCurrentOutline() {
		return currentOutline;
	}
	/**
	 * @param currentHierarchy The currentHierarchy to set.
	 */
	public void setCurrentOutline(int currentOutline) {
		this.currentOutline = currentOutline;
	}
	
	// override this if needed and call base class
	public void addToDefaultOutline(Node parentNode, Node childNode) {
		getDefaultOutline().add(parentNode,childNode,NodeModel.SILENT);
	}
	public void addToDefaultOutline(Node parentNode, Node childNode,int position,boolean event) {
		getDefaultOutline().add(parentNode,childNode,position,(event)?NodeModel.EVENT:NodeModel.SILENT);
	}
	
	/**
	 * Add to all outlines unless it is already present
	 * @param object
	 * @param except - A node model to ignore.  This node model should be treated separately.
	 */
	public void addToAll(Object object, NodeModel except) {
		for (int i=0; i < outlines.length; i++) {
			if (outlines[i] != null && outlines[i] != except) {
				if (outlines[i].search(object) == null) 
					outlines[i].add(NodeFactory.getInstance().createNode(object),NodeModel.SILENT);
			}
		}
	}

	public void removeFromAll(Object object, NodeModel except) {
		Node node;
		for (int i=0; i < outlines.length; i++) {
			if (outlines[i] != null && outlines[i] != except) {
				node = outlines[i].search(object);
				if (node != null)
					outlines[i].remove(node,NodeModel.SILENT);
			}
		}
	}
	
}

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
package com.microproject.pm.graphic.model.cache;

import java.util.List;
import java.util.ListIterator;

import javax.swing.tree.TreeModel;

import com.microproject.pm.graphic.model.event.CacheListener;
import com.microproject.association.InvalidAssociationException;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.model.WalkersNodeModel;
/**
 * This class lies between the SpreadSheet and the SpreadSheetModel.
 * It holds the states directly linked to the view.
 * The collapsed state and level of the nodes here.
 * The level is not a view state but it is calculated and cached for performance purposes.
 */

public interface NodeModelCache extends TreeModel{
	public static final int ASSIGNMENT_TYPE=1;
	public static final int TASK_TYPE=2;
	public static final int RESOURCE_TYPE=4;
	public static final int PROJECT_TYPE=8;
	
	public void setType(int type);
	public int getType();

	public NodeModel getModel();
	public WalkersNodeModel getWalkersModel();
	public void setModel(NodeModel model);
	
	public Object getElementAt(int i);
	public ListIterator getIterator();
	public ListIterator getIterator(int i);
//	public static interface CacheClosure{
//		public void execute(GraphicNode node,int deltaLevel);
//	}
	//public void forEach(CacheClosure c);
	public int getMaxLevel();
	public List<Object> getElementsAt(int[] i);
	public List<Object> getNodesAt(int[] i);
	public int getRowAt(Object obj);
	public Object getEdgeElementAt(int i);
	public ListIterator getEdgesIterator();
	public ListIterator getEdgesIterator(int i);
	public int getSize();
	public int getEdgesSize();
	public GraphicNode getParent(GraphicNode node);
	public List<Object> getChildren(GraphicNode node);

	public Object getGraphicNode(Object base);
	public Object getGraphicDependency(Object base);

	
	//public boolean isSummary(GraphicNode node);
	
	public void changeCollapsedState(GraphicNode node);
	public void expandNodes(List nodes, boolean expand);
	
	public void newNode(GraphicNode node);
	/** Inserts and returns a new empty node immediately before the stable task node. */
	public Node newNodeBefore(Node node);
	public void newNode(List nodes);
	
	public void deleteNodes(List nodes);
	public void indentNodes(List nodes);
	public void outdentNodes(List nodes);
	public void cutNodes(List gnodes);
	public void copyNodes(List gnodes);
	public boolean pasteNodes(Node parent,List nodes,int position);
	public void addNodes(Node sibling,List nodes);
	public boolean isTaskOrderEditable();
	public boolean canMoveNodes(List nodes,int direction);
	public boolean moveNodes(List nodes,int direction);
	public boolean canRelocateNodes(List nodes,Node anchor,boolean after);
	public boolean relocateNodes(List nodes,Node anchor,boolean after);

	
	public void createDependency(GraphicNode startNode,GraphicNode endNode) throws InvalidAssociationException;
	public void createHierarchyDependency(GraphicNode startNode,GraphicNode endNode) throws InvalidAssociationException;

	/**
	 * Returns the parent/previous,position identification of the void node at row
	 * Apply this to a void node row only
	 * @param row
	 * @return
	 */
//	public NodeHierarchyVoidLocation getVoidNodeInfoObject(GraphicNode node);
	
	
	
	public void addNodeModelListener(CacheListener l);
	public void removeNodeModelListener(CacheListener l);
	public CacheListener[] getNodeModelListeners();
	
	public void close();
	
	
	public int getLevel(GraphicNode node);
	public int getPertLevel(GraphicNode node);
	public void setPertLevel(GraphicNode node,int level);
	
	
	public void update(); //test only
	
	public ReferenceNodeModelCache getReference();

    public VisibleDependencies getVisibleDependencies();
    public VisibleNodes getVisibleNodes();
    
    public boolean isReceiveEvents();
    public void setReceiveEvents(boolean receiveEvents);
	
}


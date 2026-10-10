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
package com.microproject.grouping.core.model;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.tree.TreeModel;
import com.microproject.undo.EditSupport;

import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.field.FieldParseException;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.hierarchy.NodeHierarchy;
import com.microproject.undo.UndoController;

/**
 *
 */
public interface NodeModel extends TreeModel, WalkersNodeModel{
	public static int EVENT=1;
	public static int UNDO=2;

	public static int SILENT=0;
	public static int NORMAL=3;

	//Node structure modification
	public void add(Node child,int actionType);
	public void add(Node parent,Node child,int position,int actionType);
	public void add(Node parent,Node child,int actionType);
	public void add(Node parent,List<Node> children,int actionType);
	public void add(Node parent,List<Node> children,int position,int actionType);
	public void addBefore(LinkedList<Node> siblings,Node newNode,int actionType);
	public void addBefore(Node sibling,Node newNode,int actionType);
	public void addBefore(Node sibling,List<Node> newNodes,int actionType);
	public void addImplCollection(Node parent, Collection<?> collection,int actionType);
	public Node newNode(Node parent,int position,int actionType);
	public List<Node> newNodes(Node parent,int position,int count,int actionType);

	public void paste(Node parent,List<Node> nodes,int position,int actionType);

	public void remove(Node node,int actionType);
	public void remove(List<Node> nodes,int actionType);
	public void remove(Node node,int actionType,boolean removeDependencies);
	public void remove(List<Node> nodes,int actionType,boolean removeDependencies);
	public void removeAll(int actionType);
	//internal
	public boolean removeApartFromHierarchy(Node node,boolean cleanAssignment,int actionType,boolean removeDependencies);
	boolean confirmRemove(List<Node> nodes);
	public List<Node> cut(List<Node> nodes,int actionType);
	public List<Node> copy(List<Node> nodes,int actionType);

	public void move(Node parent,List<Node> nodes,int position,int actionType);
	public boolean canRelocate(List<Node> nodes,Node parent,int position);
	public boolean relocate(List<Node> nodes,Node parent,int position,int actionType);
	public boolean canMoveSelectedNodes(List<Node> nodes,int direction);
	public boolean moveSelectedNodes(List<Node> nodes,int direction,int actionType);


	//Node implementation or field modifications
	public void setFieldValue(Field field, Node node, Object eventSource, Object value, FieldContext context,int actionType) throws FieldParseException;
	public Node replaceImplAndSetFieldValue(Node node, LinkedList<Node> previous, Object newImpl, Field field, Object eventSource, Object value, FieldContext context,int actionType) throws FieldParseException;
	public Node replaceImplAndSetFieldValue(Node node, LinkedList<Node> previous, Field field, Object eventSource, Object value, FieldContext context,int actionType) throws FieldParseException;
	public Node replaceImpl(Node node, Object nodeImpl, Object eventSource,int actionType);



	public boolean hasChildren(Node node);
	public boolean isSummary(Node node);

	public Iterator<Node> iterator();
	public Iterator<Node> iterator(Node rootNode);
	public Iterator<Node> shallowIterator(int maxLevel,boolean returnRoot);
	public NodeHierarchy getHierarchy();
	public void setHierarchy(NodeHierarchy hierarchy);

	public Object clone();
	public Node search(Object key, Comparator<Object> c);
	public Node search(Object key);



	public NodeModelDataFactory getDataFactory();
	public void setDataFactory(NodeModelDataFactory dataFactory);

	//shortcut used by walkers
	public List<Node> getChildren(Node parent);
	public Node getParent(Node child);

	public void setUndoController(UndoController undoController);
	public UndoController getUndoController();
	public EditSupport getUndoableEditSupport();

	public boolean isLocal();
	public void setLocal(boolean local);
	public boolean isMaster();
	public void setMaster(boolean master);

}

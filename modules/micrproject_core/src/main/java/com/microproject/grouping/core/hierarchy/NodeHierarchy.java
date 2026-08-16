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
package com.microproject.grouping.core.hierarchy;

import java.util.Comparator;
import java.util.function.Consumer;
import java.util.EventListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.tree.TreeModel;

import org.apache.commons.collections.Predicate;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.event.HierarchyListener;
import com.microproject.grouping.core.model.NodeModel;

/**
 *
 */
public interface NodeHierarchy extends TreeModel{
	
	
//	public void add(Node parent,Node child,int actionType);
    //public void add(Node parent,Node child,int position,int actionType);
    //public void add(Node parent,List children,int actionType);
    public void add(Node parent,List children,int position,int actionType);
    public void paste(Node parent,List children,int position,NodeModel model,int actionType);
    public void cleanVoidChildren();
	public void checkEndVoidNodes(int actionType);
	public void checkEndVoidNodes(boolean subproject,int actionType);

    //public void remove(Node node,NodeModel model,int actionType);
	public void remove(List nodes,NodeModel model,int actionType,boolean removeDependencies);
	public void removeAll(NodeModel model,int actionType);
	
//    public void move(Node node,Node newParent);
//    public void move(List nodes,Node newParent);
    public void move(Node node,Node newParent,int actionType);	
	public boolean relocate(Node parent,List nodes,int position,int actionType);
//	public void indent(Node node,int deltaLevel,int actionType);
	public void indent(List nodes,int deltaLevel, NodeModel model, int actionType);
	
	public void renumber();
	
	
	
	
	public Node getParent(Node child);
	public List getChildren(Node parent);
	public int getLevel(Node node);
	public Object clone();
	public Iterator iterator();
	public Iterator iterator(Node rootNode);
	public Iterator shallowIterator(int maxLevel,boolean returnRoot);
	public void visitAll(Consumer<Object> visitor);
	public void visitAll(Node parent, Consumer<Object> visitor);
    public void visitAllLevelOrder(Node root, boolean skipLazyParents,Consumer<Object> visitor);
    public void visitAll(Node root, boolean skipLazyParents,Consumer<Object> visitor);
    public void visitLeaves(Node node, Consumer<Object> visitor);
	public Node search(Object key, Comparator c);
	public int getIndexOfNode(Node key, boolean skipVoid);
	public boolean isSummary(Node node);
    public Node getNext(Node current);
    public Node getPrevious(Node current);
    public List toList(boolean isNode, Predicate filter);
	
	public int getNbEndVoidNodes();
	public void setNbEndVoidNodes(int nbEndVoidNodes);
	
	public void addHierarchyListener(HierarchyListener l);
	public void removeHierarchyListener(HierarchyListener l);
	public HierarchyListener[] getHierarchyListeners();
    public EventListener[] getHierarchyListeners(Class listenerType);
   
    public void fireUpdate();
    public void fireUpdate(Node[] nodes);
    public void fireInsertion(Node[] nodes);
    public void fireRemoval(Node[] nodes);
    
    
}

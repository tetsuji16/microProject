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
import java.util.List;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.event.HierarchyEvent;
import com.microproject.grouping.core.event.HierarchyListener;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.transform.filtering.NodeFilter;

/**
 *
 */
public class FilteredNodeHierarchy extends AbstractMutableNodeHierarchy implements HierarchyListener {
	protected NodeHierarchy hierarchy;
	protected NodeFilter filter;
	
	public FilteredNodeHierarchy(NodeHierarchy hierarchy) {
		this.hierarchy=hierarchy;
	}

	
	
	
	public NodeFilter getFilter() {
		return filter;
	}
	public void setFilter(NodeFilter filter) {
		this.filter = filter;
		fireStructureChanged(this);
	}
	public NodeHierarchy getHierarchy() {
		return hierarchy;
	}
	public void setHierarchy(NodeHierarchy hierarchy) {
		this.hierarchy = hierarchy;
		fireStructureChanged(this);
	}
	
//    public void cleanNullChildren(){
//    	hierarchy.cleanNullChildren();
//    }
	
	
	
//	public void add(Node parent, Node child, int actionType) {
//		hierarchy.add(parent, child, actionType);
//	}
//    public void add(Node parent,Node child,int position,int actionType){
//		hierarchy.add(parent, child, position,actionType);
//    }
//	public void add(Node parent, List children, int actionType) {
//		hierarchy.add(parent, children, actionType);
//	}
   public void add(Node parent,List children,int position,int actionType){
		hierarchy.add(parent, children, position,actionType);
    }
    public void paste(Node parent,List children,int position, NodeModel model, int actionType){
		hierarchy.paste(parent, children, position, model, actionType);
	}
	public boolean relocate(Node parent,List nodes,int position,int actionType){
		return hierarchy.relocate(parent,nodes,position,actionType);
	}
    public void cleanVoidChildren(){
    	hierarchy.cleanVoidChildren();
    }

	public void checkEndVoidNodes(int actionType) {
		hierarchy.checkEndVoidNodes(actionType);
	}
	public void checkEndVoidNodes(boolean subproject,int actionType){
		hierarchy.checkEndVoidNodes(subproject,actionType);
	}
//	public int deleteVoidNodesAfter(NodeHierarchyLocation location) {
//		return hierarchy.deleteVoidNodesAfter(location);
//	}
//	public int deleteVoidNodesAfter(NodeHierarchyLocation location, int n,
//			boolean event) {
//		return hierarchy.deleteVoidNodesAfter(location, n, event);
//	}
	public int getLevel(Node node) {
		return hierarchy.getLevel(node);
	}
	public Object getRoot() {
		return hierarchy.getRoot();
	}
	public void indent(List nodes, int deltaLevel, NodeModel nodeModel,int actionType) {
		hierarchy.indent(nodes, deltaLevel, nodeModel,actionType);
	}
//	public void indent(Node node, int deltaLevel,int actionType) {
//		hierarchy.indent(node, deltaLevel,actionType);
//	}
//	public int insertVoidNodesAfter(NodeHierarchyLocation location, int n,
//			boolean event) {
//		return hierarchy.insertVoidNodesAfter(location, n, event);
//	}
	public boolean isLeaf(Object node) {
		return hierarchy.isLeaf(node);
	}
	public boolean isSummary(Node node) {
		return hierarchy.isSummary(node);
	}
//	public void promoteVoidNode(NodeHierarchyVoidLocation info,
//			Object newNodeImpl) {
//		hierarchy.promoteVoidNode(info, newNodeImpl);
//	}
//	public void remove(Node node, NodeModel model, int actionType) {
//		hierarchy.remove(node, model, actionType);
//	}
	public void remove(List nodes, NodeModel model, int actionType,boolean removeDependencies) {
		hierarchy.remove(nodes, model, actionType,removeDependencies);
	}
//    public void move(Node node,Node newParent){
//        hierarchy.move(node,newParent);
//    }
    public void move(Node node,Node newParent, int actionType){
        hierarchy.move(node,newParent,actionType);
    }
//
//    public void move(List nodes,Node newParent){
//        hierarchy.move(nodes,newParent);
//    }
//	public void replaceVoidNode(Node child, NodeHierarchyVoidLocation info,
//			boolean event) {
//		hierarchy.replaceVoidNode(child, info, event);
//	}
	public void setNbEndVoidNodes(int nbEndVoidNodes) {
		hierarchy.setNbEndVoidNodes(nbEndVoidNodes);
	}
	public int getNbEndVoidNodes() {
		return hierarchy.getNbEndVoidNodes();
	}
	public Node getParent(Node child) {
		return hierarchy.getParent(child);
	}
//	public int getVoidNodesCountAfter(NodeHierarchyLocation location) {
//		return hierarchy.getVoidNodesCountAfter(location);
//	}
//	public ArrayList getVoidNodes(NodeHierarchyLocation location) {
//		return hierarchy.getVoidNodes(location);
//	}
	public void valueForPathChanged(TreePath path, Object newValue) {
 		//TODO works ?
		hierarchy.valueForPathChanged(path, newValue);
	}
	public void addTreeModelListener(TreeModelListener l) {
		hierarchy.addTreeModelListener(l);
	}
	public void removeTreeModelListener(TreeModelListener l) {
		hierarchy.removeTreeModelListener(l);
	}
//	public Map getVoidNodesMap(){
//	    return hierarchy.getVoidNodesMap();
//	}
	
	public void removeAll(NodeModel model, int actionType) {
		hierarchy.removeAll(model,actionType);
	}
	
	
	public List getChildren(Node parent) {
		return filter.filterList(hierarchy.getChildren(parent));
	}
	public Node search(Object key, Comparator c) {
		Node node=hierarchy.search(key, c);
		if (node==null)  return null;
		return (filter.evaluate(node))?node:null;
	}
	
	
	
	public Object clone(){
		FilteredNodeHierarchy newHierarchy=new FilteredNodeHierarchy(hierarchy);
		newHierarchy.setFilter(filter);
		return newHierarchy;
	}
	

	
	public void nodesChanged(HierarchyEvent e) {
		Object[] nodes=filter.filterArray(e.getNodes());
		fireNodesChanged(e.getSource(),nodes,e.getNodes(),e.getFlag());
	}
	public void nodesInserted(HierarchyEvent e) {
		Object[] nodes=filter.filterArray(e.getNodes());
		fireNodesInserted(e.getSource(),nodes,e.getNodes(),e.getFlag());
	}
	public void nodesRemoved(HierarchyEvent e) {
		Object[] nodes=filter.filterArray(e.getNodes());
		fireNodesRemoved(e.getSource(),nodes,e.getNodes(),e.getFlag());
	}
	public void structureChanged(HierarchyEvent e) {
		Object[] nodes=filter.filterArray(e.getNodes());
		fireStructureChanged(e.getSource());
	}
	
    public void fireUpdate(){
    	hierarchy.fireUpdate();
    }
    public void fireUpdate(Node[] nodes){
    	hierarchy.fireUpdate(nodes);
    }
    public void fireInsertion(Node[] nodes){
    	hierarchy.fireInsertion(nodes);
    }
    public void fireRemoval(Node[] nodes){
    	hierarchy.fireRemoval(nodes);
    }
	
	
	
	
	
	
	
	


}

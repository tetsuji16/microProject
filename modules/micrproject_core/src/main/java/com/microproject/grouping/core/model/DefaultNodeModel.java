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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import com.microproject.association.Association;
import com.microproject.association.AssociationList;
import com.microproject.document.Document;
import com.microproject.field.Field;
import com.microproject.field.FieldContext;
import com.microproject.field.FieldParseException;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeBridge;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.grouping.core.VoidNodeImpl;
import com.microproject.grouping.core.hierarchy.HierarchyUtils;
import com.microproject.grouping.core.hierarchy.MutableNodeHierarchy;
import com.microproject.grouping.core.hierarchy.NodeHierarchy;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.assignment.HasAssignments;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.HasDependencies;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.task.SubProj;
import com.microproject.pm.task.Task;
import com.microproject.pm.task.TaskLinkReference;
import com.microproject.pm.task.TaskLinkReferenceImpl;
import com.microproject.undo.ModelFieldEdit;
import com.microproject.undo.NodeCreationEdit;
import com.microproject.undo.NodeDeletionEdit;
import com.microproject.undo.NodeImplChangeAndValueSetEdit;
import com.microproject.undo.NodeImplChangeEdit;
import com.microproject.undo.NodePasteEdit;
import com.microproject.undo.NodeRelocationEdit;
import com.microproject.undo.NodeUndoInfo;
import com.microproject.undo.UndoController;
import com.microproject.util.Environment;

/**
 *
 */
public class DefaultNodeModel implements NodeModel {

	protected NodeHierarchy hierarchy;
	protected NodeModelDataFactory dataFactory = null;
	private transient Map searchIndex = new IdentityHashMap();

	/**
	 *
	 */
	public DefaultNodeModel() {
		hierarchy=new MutableNodeHierarchy();
		rebuildSearchIndex();
	}

	public DefaultNodeModel(NodeModelDataFactory dataFactory) {
		this();
		this.dataFactory = dataFactory;
	}
	//for clone()
	DefaultNodeModel(NodeHierarchy hierarchy, NodeModelDataFactory dataFactory) {
		this.hierarchy=hierarchy;
		this.dataFactory = dataFactory;
		rebuildSearchIndex();
	}

	public void addBefore(LinkedList siblings,Node newNode,int actionType){
		Node previous,next,parent;
		boolean firstChild;
		if (siblings.size()==0){
			return;
		}else if (siblings.size()==1){
			previous=null;
			next=(Node)siblings.removeLast();
			parent=null;
			firstChild=true;
		}else{
			previous=(Node)siblings.removeFirst();  //no need to clone, list used only here, see CommonSpreadSheetModel
			next=(Node)siblings.removeLast();
			parent=(Node)next.getParent();
			firstChild=(parent==previous);
			if (firstChild) parent=previous;
			else parent=(Node)previous.getParent();
			remove(siblings, actionType);
		}
		siblings.add(newNode);
		add(parent,siblings,(firstChild)?0:(parent.getIndex(previous)+1),actionType);

		getDataFactory().setGroupDirty(true);
	}

	public void addBefore(Node sibling,Node newNode,int actionType){
		Node parent=(Node)sibling.getParent();
		add(parent,newNode,parent.getIndex(sibling),actionType);
	}
	public void addBefore(Node sibling,List newNodes,int actionType){
		Node parent=(Node)sibling.getParent();
		add(parent,newNodes,parent.getIndex(sibling),actionType);
	}
	public void add(Node parent,Node child,int actionType){
		add(parent,child,-1,actionType);
	}
	public void add(Node parent,Node child,int position,int actionType){
		ArrayList children = new ArrayList();
		children.add(child);
		add(parent,children,position,actionType);
		//hierarchy.add(parent,child,position,actionType);
	}
	public void add(Node parent,List children,int actionType){
		add(parent,children,-1,actionType);
		//hierarchy.add(parent,children,actionType);
	}
	public void add(Node parent,List children,int position,int actionType){
		hierarchy.add(parent,children,position,actionType);
		registerNodes(children);
		//Undo
		if (isUndo(actionType)) postEdit(new NodeCreationEdit(this,parent,children,position));

	}


	public void add(Node child,int actionType){
		add((Node)hierarchy.getRoot(),child,actionType);
	}

	public Node newNode(Node parent,int position,int actionType){
		//check if position is correct
		Node node;
		int p=position;
		int i=0;
		for (Enumeration e=parent.children();e.hasMoreElements();i++){
			node=(Node)e.nextElement();
			if (i==p){
				if (node.getImpl() instanceof Assignment) p++;
				else{
					Node newNode=NodeFactory.getInstance().createVoidNode();
					add(parent,newNode,p,actionType);
					return newNode;
				}
			}
		}
		Node newNode=NodeFactory.getInstance().createVoidNode();
		add(parent,newNode,-1,actionType);
		return newNode;

	}

	public void paste(Node parent,List nodes,int position,int actionType){
		//nodes=copy(nodes,NodeModel.SILENT); //make an other copy, in case it is copied more than one time
		//done in transfert handler

		hierarchy.paste(parent,nodes,position,this,actionType);
		registerNodes(nodes);
		//Undo
		if (isUndo(actionType)) postEdit(new NodePasteEdit(this,parent,nodes,position));
	}

	public boolean isAncestor(Node parent, Node child) {
		if (child == null)
			return false;
		if (parent == child)
			return true;
		return isAncestor(parent,getParent(child));
	}

	public boolean isAncestorOrDescendant(Node one, Node two) {
		return isAncestor(one,two) || isAncestor(two,one);
	}

	public boolean testAncestorOrDescendant(Node one, List nodes) {
		Iterator i = nodes.iterator();
		while (i.hasNext()) {
			if (isAncestorOrDescendant(one,(Node)i.next()))
				return false;
		}
		return true;
	}

	public void move(Node parent,List nodes,int position,int actionType){
		if (!testAncestorOrDescendant(parent,nodes)) // don't allow circular
			return;
		List cutNodes=cut(nodes,false,actionType);
		paste(parent,cutNodes,position,actionType);
	}

	/**
	 * Moves a contiguous set of sibling outline branches while preserving the task
	 * instances, unique IDs, dependencies, assignments, baselines, and notes.
	 */
	public boolean canRelocate(List nodes,Node parent,int position){
		ArrayList<Node> branches=collectRelocationRoots(nodes);
		if (branches.isEmpty()) return false;
		Node sourceParent=(Node)branches.get(0).getParent();
		if (sourceParent==null||!areContiguousSiblings(branches,sourceParent)) return false;
		Node destination=(parent==null)?(Node)hierarchy.getRoot():parent;
		if (!canRelocateTo(branches,destination)) return false;
		int remainingChildren=destination.getChildCount();
		for (Node branch:branches)
			if (branch.getParent()==destination) remainingChildren--;
		int finalPosition=Math.max(0,Math.min(position,remainingChildren));
		return sourceParent!=destination||sourceParent.getIndex(branches.get(0))!=finalPosition;
	}

	public boolean relocate(List nodes,Node parent,int position,int actionType){
		if (!canRelocate(nodes,parent,position)) return false;
		ArrayList<Node> branches=collectRelocationRoots(nodes);
		Node sourceParent=(Node)branches.get(0).getParent();
		Node destination=(parent==null)?(Node)hierarchy.getRoot():parent;

		int beforeIndex=sourceParent.getIndex(branches.get(0));
		int remainingChildren=destination.getChildCount();
		for (Node branch:branches)
			if (branch.getParent()==destination) remainingChildren--;
		int finalPosition=Math.max(0,Math.min(position,remainingChildren));
		boolean doTransaction=getDocument()!=null&&isEvent(actionType);
		int transactionId=0;
		if (doTransaction) transactionId=getDocument().fireMultipleTransaction(0,true);
		boolean changed;
		try {
			changed=hierarchy.relocate(destination,branches,finalPosition,actionType);
		} finally {
			if (doTransaction) getDocument().fireMultipleTransaction(transactionId,false);
		}
		if (!changed) return false;

		int afterIndex=destination.getIndex(branches.get(0));
		if (isUndo(actionType))
			postEdit(new NodeRelocationEdit(this,branches,sourceParent,beforeIndex,destination,afterIndex));
		if (dataFactory!=null) dataFactory.setGroupDirty(true);
		return true;
	}

	public boolean canMoveSelectedNodes(List nodes,int direction){
		if (direction!=-1&&direction!=1) return false;
		ArrayList<Node> branches=collectRelocationRoots(nodes);
		if (branches.isEmpty()) return false;
		Node parent=(Node)branches.get(0).getParent();
		if (parent==null||!areContiguousSiblings(branches,parent)) return false;
		int start=parent.getIndex(branches.get(0));
		int end=start+branches.size()-1;
		return direction<0?start>0:end<parent.getChildCount()-1;
	}

	public boolean moveSelectedNodes(List nodes,int direction,int actionType){
		if (!canMoveSelectedNodes(nodes,direction)) return false;
		ArrayList<Node> branches=collectRelocationRoots(nodes);
		Node parent=(Node)branches.get(0).getParent();
		int start=parent.getIndex(branches.get(0));
		return relocate(branches,parent,direction<0?start-1:start+1,actionType);
	}

	private ArrayList<Node> collectRelocationRoots(List nodes){
		ArrayList<Node> branches=new ArrayList<Node>(nodes == null ? 0 : nodes.size());
		if (nodes==null) return branches;
		HierarchyUtils.extractParents(nodes,branches);
		if (branches.isEmpty()) return branches;
		Node parent=(Node)branches.get(0).getParent();
		if (parent==null){
			branches.clear();
			return branches;
		}
		for (Node branch:branches){
			if (branch==null||branch.isRoot()||branch.getParent()!=parent){
				branches.clear();
				return branches;
			}
		}
		branches.sort((left,right)->parent.getIndex(left)-parent.getIndex(right));
		return branches;
	}

	private boolean areContiguousSiblings(List<Node> branches,Node parent){
		int expected=parent.getIndex(branches.get(0));
		for (Node branch:branches){
			if (parent.getIndex(branch)!=expected++) return false;
		}
		return true;
	}

	private boolean canRelocateTo(List<Node> branches,Node destination){
		if (destination==null||destination.isLazyParent()) return false;
		for (Node branch:branches){
			if (branch==destination||isAncestor(branch,destination)) return false;
			if (!destination.isRoot()&&!branch.canBeChildOf(destination)) return false;
		}
		return true;
	}


	/**
	 * Convenience method to add a collection of objects (not nodes) to the node model
	 * @param parent
	 * @param collection
	 */
	public void addImplCollection(Node parent, Collection collection,int actionType) {
		Iterator i = collection.iterator();
		Node child;
		while (i.hasNext()) {
			child = NodeFactory.getInstance().createNode(i.next());
			add(parent,child,actionType);
		}

	}

	public void remove(Node node,int actionType){
		remove(node, actionType, true,true);
	}
	public void remove(Node node,int actionType,boolean removeDependencies){
		remove(node, actionType, true,removeDependencies);
	}
	public void remove(Node node,int actionType,boolean filterAssignments,boolean removeDependencies){
		ArrayList nodes = new ArrayList();
		nodes.add(node);
		remove(nodes,actionType,filterAssignments,removeDependencies);
		//hierarchy.remove(node,this,actionType);
		//it calls back removeApartFromHierarchy for each node to remove
	}
	public void remove(List nodes,int actionType){
		remove(nodes, actionType, true);
	}
	public void remove(List nodes,int actionType,boolean removeDependencies){
		remove(nodes, actionType, true,removeDependencies);
	}
	public void remove(List nodes,int actionType,boolean filterAssignments,boolean removeDependencies){
		beginUndoUpdate(actionType);
		try {
			ArrayList roots = collectRemovalRoots(nodes, filterAssignments);
			RemovalSnapshot removalSnapshot = RemovalSnapshot.capture(roots);
			if (!confirmRemove(roots))
				return;

			hierarchy.remove(roots,this,actionType,removeDependencies);
			unregisterNodes(roots);
			//it calls back removeApartFromHierarchy for each node to remove
			hierarchy.checkEndVoidNodes(actionType);

			//Undo
			if (undoController!=null&&isUndo(actionType))
				postEdit(new NodeDeletionEdit(this,removalSnapshot));
		} finally{
			endUndoUpdate(actionType);
		}
	}
	public void removeAll(int actionType){
		hierarchy.removeAll(this,actionType);
	}
	public boolean removeApartFromHierarchy(Node node,boolean cleanAssignment,int actionType,boolean removeDependencies){
		if (!isEvent(actionType))
			return true;
//		try {
//			beginUpdate();
			if (node.getImpl() instanceof Assignment){
				Assignment assignment=(Assignment)node.getImpl();
//				if (cleanAssignment)
					AssignmentService.getInstance().remove(assignment,cleanAssignment,this,isUndo(actionType)); //LC 8/4/2006 - hk 7/8/2006 changed null to this so event will be fired
//				else if (assignment.getResource()!=ResourceImpl.getUnassignedInstance()){
//					assignment.getResource().removeAssignment(assignment);
//				}


			//AssignmentService.getInstance().remove((Assignment)node.getImpl(),this);
			}else if (dataFactory!=null&&!node.isVoid())
				dataFactory.remove(node.getImpl(),this,false,isUndo(actionType),removeDependencies);
//		} finally {
//			endUpdate();
//		}
		return true;
	}

	public List cut(List nodes,int actionType){
		return cut(nodes,true,actionType);
	}
	public List cut(List nodes,boolean clone,int actionType){
		List newNodes=copy(nodes,clone,actionType);
		remove(nodes,actionType);
		return newNodes;
//		ArrayList parentNodes =new Vector(nodes.size());
//		HierarchyUtils.extractParents(nodes,parentNodes);
//		remove(parentNodes,actionType);
//		return parentNodes;
	}


	public List copy(List nodes,int actionType){
		return copy(nodes,true,actionType);
	}

	public List copy(List nodes,boolean clone,int actionType){
		ArrayList parentNodes = new ArrayList(nodes.size());
		HierarchyUtils.extractParents(nodes,parentNodes);
		if (!clone) return parentNodes;
		Set assignedNodes=new HashSet();
		Map implMap=new HashMap();
		Set<Dependency> predecessors=new HashSet<Dependency>();
		Set<Dependency> successors=new HashSet<Dependency>();
		for (ListIterator i=parentNodes.listIterator();i.hasNext();){
			Node parent=(Node)i.next();
			Node newParent=cloneNode(parent,null,implMap,predecessors,successors);
			cloneBranch(parent,newParent,assignedNodes,implMap,predecessors,successors);
			i.remove();
			i.add(newParent);
		}

		rebuildCopiedDependencies(implMap, predecessors, successors);

		for (Iterator i=assignedNodes.iterator();i.hasNext();){
			addAssignments((Node)i.next());
		}

		for (ListIterator i=parentNodes.listIterator();i.hasNext();){
			Node node=(Node)i.next();
			cleanBranch(node);
		}

		return parentNodes;
	}
	private void cloneBranch(Node parent,Node newParent,Set assignedNodes,Map implMap,Set<Dependency> predecessors,Set<Dependency> successors){
		for (Iterator i=parent.childrenIterator();i.hasNext();){
				Node child=(Node)i.next();
				if (child.getImpl() instanceof Assignment){
					assignedNodes.add(newParent);
				}else{
					Node newChild=cloneNode(child,newParent,implMap,predecessors,successors);
					cloneBranch(child,newChild,assignedNodes,implMap,predecessors,successors);
				}
		}
	}
	private Node cloneNode(Node oldNode,Node newParent,Map implMap,Set<Dependency> predecessors,Set<Dependency> successors){
		Object oldNodeImpl=oldNode.getImpl();
		Object newNodeImpl=cloneNodeImpl(oldNodeImpl);
		implMap.put(oldNodeImpl, newNodeImpl);
		if (oldNodeImpl instanceof Task){
			Task t=(Task)oldNodeImpl;
			addDependencies(predecessors, t.getDependencyList(true));
			addDependencies(successors, t.getDependencyList(false));
		}
		Object parentImpl = (newParent==null)?null:newParent.getImpl();
		NodeModelDataFactory factory = getFactory(parentImpl);

		factory.addUnvalidatedObject(newNodeImpl,this,parentImpl);

		Node newNode=NodeFactory.getInstance().createNode(newNodeImpl);
		if (newParent!=null) newParent.add(newNode);
		registerNodeSubtree(newNode);
		if (parentImpl != null&& parentImpl instanceof Task)
			((Task)parentImpl).setWbsChildrenNodes(getHierarchy().getChildren(newParent)); //rebuild children task's wbs cache
		return newNode;
	}
	private static void addDependencies(Set<Dependency> target, AssociationList source) {
		for (Association association : source) {
			if (association instanceof Dependency)
				target.add((Dependency) association);
		}
	}
	private Object cloneNodeImpl(Object impl){
				if (impl instanceof VoidNodeImpl){
					return new VoidNodeImpl();
				}else if (impl instanceof NormalTask){
					return ((NormalTask)impl).clone();
				}else if (impl instanceof ResourceImpl){
					return ((ResourceImpl)impl).clone();
				}//TOTO assignments
		return null;
	}

	private void cleanBranch(Node parent){
		for (Iterator i=parent.childrenIterator();i.hasNext();){
				Node child=(Node)i.next();
				cleanNodeImpl(child.getImpl());
				cleanBranch(child);
		}
	}
	private void cleanNodeImpl(Object impl){
		if (impl instanceof NormalTask){
			((NormalTask)impl).cleanClone();
		}else if (impl instanceof ResourceImpl){
			((ResourceImpl)impl).cleanClone();
		}
	}

	private void addAssignments(Node node){
		if (node.getImpl() instanceof HasAssignments){
			AssociationList assignments=((HasAssignments)node.getImpl()).getAssignments();
			if (assignments==null) return;
			for (ListIterator i=assignments.listIterator(assignments.size());i.hasPrevious();){
				Assignment assignment=(Assignment)i.previous();
				if (assignment.isDefault()) continue;
				Node assignmentNode=NodeFactory.getInstance().createNode(assignment);
				node.insert(assignmentNode,0);
				registerNodeSubtree(assignmentNode);
			}
		}
	}







	public Object clone(){
		return new DefaultNodeModel((NodeHierarchy)hierarchy.clone(), dataFactory);
	}

	public Iterator iterator(){
		return hierarchy.iterator();
	}
	public Iterator iterator(Node rootNode){
		return hierarchy.iterator(rootNode);
	}
	public Iterator shallowIterator(int maxLevel,boolean returnRoot){
		return hierarchy.shallowIterator(maxLevel,returnRoot);
	}
	/**
	 * @return Returns the hierarchy.
	 */
	public NodeHierarchy getHierarchy() {
		return hierarchy;
	}



	/**
	 * @param hierarchy The hierarchy to set.
	 */
	public void setHierarchy(NodeHierarchy hierarchy) {
		this.hierarchy = hierarchy;
		rebuildSearchIndex();
	}

	public boolean hasChildren(Node node) {
		return !hierarchy.isLeaf(node);
	}
	public boolean isSummary(Node node){
		return hierarchy.isSummary(node);
	}
	/**
	 * @param key
	 * @param c
	 * @return
	 */
	public Node search(Object key, Comparator c) {
		return hierarchy.search(key, c);
	}



	private static ImplComparator implComparatorInstance = null;
	public static ImplComparator getImplComparatorInstance() {
		if (implComparatorInstance == null)
			implComparatorInstance = new ImplComparator();
		return implComparatorInstance;
	}

	public static class ImplComparator implements Comparator {
		ImplComparator() {}
		public int compare(Object node, Object impl) {
			if (((Node)node).getImpl() == impl)
				return 0;
			else
				return 1;
		}
	}

	public Node search(Object key) {
		if (key == null) return null;
		Node node=(Node)searchIndex.get(key);
		if (node==null){
			node=hierarchy.search(key,getImplComparatorInstance());
			if (node!=null) searchIndex.put(key,node);
		}
		return node;
	}


	// Below is for tree model
	/**
	 * @param arg0
	 */
	public void addTreeModelListener(TreeModelListener arg0) {
		hierarchy.addTreeModelListener(arg0);
	}
	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 */
	public Object getChild(Object arg0, int arg1) {
		return hierarchy.getChild(arg0, arg1);
	}
	/**
	 * @param arg0
	 * @return
	 */
	public int getChildCount(Object arg0) {
		return hierarchy.getChildCount(arg0);
	}
	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 */
	public int getIndexOfChild(Object arg0, Object arg1) {
		return hierarchy.getIndexOfChild(arg0, arg1);
	}
	/**
	 * @return
	 */
	public Object getRoot() {
		return hierarchy.getRoot();
	}
	/**
	 * @param arg0
	 * @return
	 */
	public boolean isLeaf(Object arg0) {
		return hierarchy.isLeaf(arg0);
	}
	/**
	 * @param arg0
	 */
	public void removeTreeModelListener(TreeModelListener arg0) {
		hierarchy.removeTreeModelListener(arg0);
	}
	/**
	 * @param arg0
	 * @param arg1
	 */
	public void valueForPathChanged(TreePath arg0, Object arg1) {
		hierarchy.valueForPathChanged(arg0, arg1);
	}

	public void setFieldValue(Field field, Node node, Object eventSource, Object value, FieldContext context,int actionType) throws FieldParseException {
		Object oldValue=field.getValue(node,this,context);

//		// this prevents the field from sending an update message.  However, ideally the field will send the message and the hiearchy event wont
//		if (context != null)
//			context.setUserObject(FieldContext.getNoUpdateInstance());


		field.setValue(node, this,eventSource, value, context);

//		No longer sending update event
//		if (isEvent(actionType)) hierarchy.fireUpdate(new Node[]{node});
		// Field notifications are handled by the field implementation.

		//Undo
		if (isUndo(actionType)) postEdit(new ModelFieldEdit(this,field,node,eventSource,value,oldValue,context));

	}

	public Node replaceImplAndSetFieldValue(Node node, LinkedList previous, Field field, Object eventSource, Object value,FieldContext context,int actionType) throws FieldParseException {
		//the line following a subproject is connected to the main project
		if (previous!=null&&previous.size()>0){
			Node p=(Node)previous.getFirst();
			if (p!=null&&p.isInSubproject()&&node.getSubprojectLevel()<p.getSubprojectLevel()){
				while (node.getSubprojectLevel()<p.getSubprojectLevel()) p=(Node)p.getParent();
				LinkedList newPrevious=new LinkedList();
				newPrevious.add(p);
				Node vn,pvn;
				for (Iterator i=previous.iterator();i.hasNext();){
					vn=(Node)i.next();
					pvn=(Node)vn.getParent();
					while(pvn!=null&&pvn!=p) pvn=(Node)pvn.getParent();
					if (pvn!=p) newPrevious.add(vn);
				}
				Object parentImpl = p.getImpl();
				NodeModelDataFactory factory = getFactory(parentImpl);
				return replaceImplAndSetFieldValue(node,newPrevious,factory.createUnvalidatedObject(this, parentImpl),field,eventSource,value,context,actionType);

			}
//			if (p!=null&&p.getImpl() instanceof NormalTask){
//				Task task=(Task)p.getImpl();
//				boolean subprojectParent=false;
//				while (task.getOwningProject()!=task.getProject()){
//					Node pParent=(Node)p.getParent();
//					if (pParent.getIndex(p)==pParent.getChildCount()-1){
//						p=pParent;
//						subprojectParent=true;
//					}else{
//						subprojectParent=false;
//						break;
//					}
//				}
//				if (subprojectParent){
//					LinkedList newPrevious=(LinkedList)previous.clone();
//					newPrevious.set(0, p);
//					Object parentImpl = p.getImpl();
//					NodeModelDataFactory factory = getFactory(parentImpl);
//					return replaceImplAndSetFieldValue(node,newPrevious,factory.createUnvalidatedObject(this, parentImpl),field,eventSource,value,context,actionType);
//
//				}
//			}

		}

		Node parent=(Node)node.getParent();
		Object parentImpl = (parent==getHierarchy().getRoot())?null:parent.getImpl();
		NodeModelDataFactory factory = getFactory(parentImpl);
		return replaceImplAndSetFieldValue(node,previous,factory.createUnvalidatedObject(this, parentImpl),field,eventSource,value,context,actionType);
	}

	private NodeModelDataFactory getFactory(Object parentImpl) {
		if (parentImpl == null)
			return dataFactory;
		else
			return dataFactory.getFactoryToUseForChildOfParent(parentImpl);
	}

	private void beginUndoUpdate(int actionType) {
		if (undoController!=null&&isUndo(actionType)){
			undoController.getEditSupport().beginUpdate();
		}
	}

	private void endUndoUpdate(int actionType) {
		if (undoController!=null&&isUndo(actionType)){
			undoController.getEditSupport().endUpdate();
		}
	}

	private ArrayList collectRemovalRoots(List nodes, boolean filterAssignments) {
		ArrayList roots = new ArrayList();
		HierarchyUtils.extractParents(nodes, roots);
		if (filterAssignments){
			for (Iterator i=roots.iterator();i.hasNext();){
				Node node=(Node)i.next();
				if (node.getImpl() instanceof Assignment){
					i.remove();
				}
			}
		}
		return roots;
	}

	/** Immutable placement and subproject state captured before a deletion. */
	public static final class RemovalSnapshot {
		private final List<Entry> entries;
		private final List<SubprojectState> subprojects;

		private RemovalSnapshot(List<Entry> entries, List<SubprojectState> subprojects) {
			this.entries = java.util.Collections.unmodifiableList(entries);
			this.subprojects = java.util.Collections.unmodifiableList(subprojects);
		}

		static RemovalSnapshot capture(List roots) {
			List<Entry> entries = new ArrayList<Entry>(roots.size());
			List<SubprojectState> subprojects = new ArrayList<SubprojectState>();
			for (Iterator i = roots.iterator(); i.hasNext();) {
				Node node = (Node) i.next();
				Node parent = (Node) node.getParent();
				entries.add(new Entry(parent, node, parent.getIndex(node)));
				collectSubprojects(node, subprojects);
			}
			return new RemovalSnapshot(entries, subprojects);
		}

		private static void collectSubprojects(Node node, List<SubprojectState> subprojects) {
			if (node.getImpl() instanceof SubProj) {
				SubProj subproject = (SubProj) node.getImpl();
				subprojects.add(new SubprojectState(node, subproject.getSubproject()));
			}
			for (Iterator i = node.childrenIterator(); i.hasNext();)
				collectSubprojects((Node) i.next(), subprojects);
		}

		public List<Entry> getEntries() {
			return entries;
		}

		public List<SubprojectState> getSubprojects() {
			return subprojects;
		}

		public List getNodes() {
			List nodes = new ArrayList(entries.size());
			for (Entry entry : entries)
				nodes.add(entry.getNode());
			return nodes;
		}

		public static final class Entry {
			private final Node parent;
			private final Node node;
			private final int position;

			private Entry(Node parent, Node node, int position) {
				this.parent = parent;
				this.node = node;
				this.position = position;
			}

			public Node getParent() { return parent; }
			public Node getNode() { return node; }
			public int getPosition() { return position; }
		}

		public static final class SubprojectState {
			private final Node node;
			private final Project project;
			private long restoreGeneration;

			private SubprojectState(Node node, Project project) {
				this.node = node;
				this.project = project;
			}

			public Node getNode() { return node; }
			public Project getProject() { return project; }

			public synchronized void cancelRestore() {
				restoreGeneration++;
			}

			public void restoreAfterClose(Project parentProject) {
				if (project == null || parentProject == null)
					return;
				final long generation;
				synchronized (this) {
					generation = ++restoreGeneration;
				}
				ProjectFactory.getInstance().runAfterProjectClosed(project.getUniqueId(), () -> {
					synchronized (SubprojectState.this) {
						if (generation != restoreGeneration)
							return;
					}
					if (ProjectFactory.getInstance().findFromId(project.getUniqueId()) == null) {
						project.initializeProject();
						ProjectFactory.getInstance().addProject(project, false, false);
					}
					ProjectFactory.getInstance().openSubproject(parentProject, node, false);
				});
			}
		}
	}

	private void rebuildSearchIndex() {
		searchIndex = new IdentityHashMap();
		for (Iterator i = iterator(); i.hasNext();) {
			registerNodeSubtree((Node)i.next());
		}
	}

	private void registerNodes(Collection nodes) {
		for (Iterator i = nodes.iterator(); i.hasNext();) {
			registerNodeSubtree((Node)i.next());
		}
	}

	private void registerNodeSubtree(Node node) {
		if (node == null)
			return;
		searchIndex.put(node.getImpl(), node);
		for (Iterator i = node.childrenIterator(); i.hasNext();) {
			registerNodeSubtree((Node)i.next());
		}
	}

	private void unregisterNodes(Collection nodes) {
		for (Iterator i = nodes.iterator(); i.hasNext();) {
			unregisterNodeSubtree((Node)i.next());
		}
	}

	private void unregisterNodeSubtree(Node node) {
		if (node == null)
			return;
		searchIndex.remove(node.getImpl());
		for (Iterator i = node.childrenIterator(); i.hasNext();) {
			unregisterNodeSubtree((Node)i.next());
		}
	}

	private void rebuildCopiedDependencies(Map implMap, Set<Dependency> predecessors, Set<Dependency> successors) {
		//rebuild dependencies
		if (Environment.isKeepExternalLinks()){
			rebuildCopiedDependenciesWithExternalLinks(implMap, predecessors, successors);
		}else{
			rebuildCopiedDependenciesWithinProject(implMap, predecessors, successors);
		}
	}

	private void rebuildCopiedDependenciesWithExternalLinks(Map implMap, Set<Dependency> predecessors,
			Set<Dependency> successors) {
		for (Dependency dependency : successors) {
			Dependency copy = recreateDependencyLink(dependency, implMap);
			predecessors.remove(copy);
		}
		for (Dependency dependency : predecessors) {
			recreateDependencyLink(dependency, implMap);
		}
	}

	private void rebuildCopiedDependenciesWithinProject(Map implMap, Set<Dependency> predecessors,
			Set<Dependency> successors) {
		for (Dependency dependency : predecessors) {
			if (successors.contains(dependency)){
				Task predecessor=(Task)implMap.get(dependency.getPredecessor());
				Task successor=(Task)implMap.get(dependency.getSuccessor());
				if (predecessor!=null&&successor!=null){
					createAndAttachDependency(predecessor, successor, dependency);
				}
			}
		}
	}

	private Dependency recreateDependencyLink(Dependency dependency, Map implMap) {
		TaskLinkReference pt=(TaskLinkReference)dependency.getPredecessor();
		TaskLinkReference st=(TaskLinkReference)dependency.getSuccessor();

		TaskLinkReference predecessor=(TaskLinkReference)implMap.get(pt);
		TaskLinkReference successor=(TaskLinkReference)implMap.get(st);

		if (predecessor==null) predecessor=new TaskLinkReferenceImpl(pt.getUniqueId(),pt.getProject());
		if (successor==null) successor=new TaskLinkReferenceImpl(st.getUniqueId(),st.getProject());
		return createAndAttachDependency(predecessor, successor, dependency);
	}

	private Dependency createAndAttachDependency(HasDependencies predecessor, HasDependencies successor,
			Dependency dependency) {
		Dependency d=Dependency.getInstance(predecessor, successor, dependency.getDependencyType(), dependency.getLag());
		d.setDirty(true);
		predecessor.getDependencyList(false).add(d);
		successor.getDependencyList(true).add(d);
		return d;
	}

	public Node replaceImplAndSetFieldValue(Node node, LinkedList previous, Object newImpl, Field field, Object eventSource, Object value,FieldContext context,int actionType) throws FieldParseException {
		List previousPosition = repositionPreviousNodes(node, previous, actionType);

		Node parent=(Node)node.getParent();

		Object parentImpl = (parent==getHierarchy().getRoot())?null:parent.getImpl();
		NodeModelDataFactory factory = getFactory(parentImpl);
		factory.addUnvalidatedObject(newImpl,this, parentImpl);
		Object oldImpl=node.getImpl();
		unregisterNodeSubtree(node);
		node.setImpl(newImpl);
		registerNodeSubtree(node);
		try {
			field.setValue(node, this,null, value, context); // will throw if error
		} catch (FieldParseException e) {
			unregisterNodeSubtree(node);
			node.setImpl(oldImpl);
			registerNodeSubtree(node);
			factory.rollbackUnvalidated(this, newImpl); // in some cases, such as ValueObjectForInterval, some cleanup is needed
			throw e;
		}
		// if no exception was thrown, then validate the object and hook it into model
		factory.validateObject(newImpl, this, eventSource, null,true);

		hierarchy.renumber();

//		dataFactory.fireCreated(newImpl);
		hierarchy.checkEndVoidNodes(actionType^NodeModel.EVENT);
		fireNodeReplaced(node);

		//Undo
		if (isUndo(actionType)) postEdit(new NodeImplChangeAndValueSetEdit(this,node,previous,previousPosition,oldImpl,field,value,context,eventSource));
		return node;
	}
	public Node replaceImpl(Node node,Object newImpl, Object eventSource,int actionType){
		Node parent = getParent(node);
		Object parentImpl = (parent==getHierarchy().getRoot())?null:parent.getImpl();
		NodeModelDataFactory factory = getFactory(parentImpl);

		factory.addUnvalidatedObject(newImpl,this, parentImpl);
		Object oldImpl=node.getImpl();
		unregisterNodeSubtree(node);
		node.setImpl(newImpl);
		factory.validateObject(newImpl, this, eventSource,null,false);
		registerNodeSubtree(node);

		hierarchy.renumber();

//		dataFactory.fireCreated(newImpl);
		fireNodeReplaced(node);

		hierarchy.checkEndVoidNodes(actionType);
		//Undo
		if (isUndo(actionType)) postEdit(new NodeImplChangeEdit(this,node,oldImpl,eventSource));
		return node;
	}


	public NodeModelDataFactory getDataFactory() {
		return dataFactory;
	}

	private void fireNodeReplaced(Node node) {
		getHierarchy().fireUpdate(new Node[]{node});
	}

	private List repositionPreviousNodes(Node node, LinkedList previous, int actionType) {
		if (previous == null)
			return null;
		LinkedList p=(LinkedList)previous.clone();
		Node sibling=(Node)p.removeFirst();
		Node parent=(Node)sibling.getParent();
		p.add(node);
		List previousPosition = null;
		if (getUndoableEditSupport()!=null&isUndo(actionType)){
			previousPosition=new ArrayList(p.size());
			for (Iterator i=p.iterator();i.hasNext();){
				Node n=(Node)i.next();
				previousPosition.add(new NodeImplChangeAndValueSetEdit.Position((Node)n.getParent(),n,n.getParent().getIndex(n)));
			}
		}
		remove(p, NodeModel.SILENT);
		add(parent,p,parent.getIndex(sibling)+1,NodeModel.SILENT);
		return previousPosition;
	}
	/**
	 * @param dataFactory The dataFactory to set.
	 */
	public void setDataFactory(NodeModelDataFactory dataFactory) {
		this.dataFactory = dataFactory;
	}

	public List getChildren(Node parent){
		return getHierarchy().getChildren(parent);
	}
	public Node getParent(Node child){
		return getHierarchy().getParent(child);
	}

    public Document getDocument() {
        return null;
    }

    public static boolean isEvent(int actionType){
    	return (actionType&NodeModel.EVENT)==NodeModel.EVENT;
    }
    public static boolean isUndo(int actionType){
    	return (actionType&NodeModel.UNDO)==NodeModel.UNDO;
    }


    protected UndoController undoController;



	public UndoController getUndoController() {
		return undoController;
	}

	public void setUndoController(UndoController undoController) {
		this.undoController = undoController;
	}

	public UndoableEditSupport getUndoableEditSupport() {
		if (undoController==null) return null;
		return undoController.getEditSupport();
	}
//	public void setUndoableEditSupport(UndoableEditSupport undoableEditSupport) {
//		this.undoableEditSupport = undoableEditSupport;
//	}

	public void postEdit(UndoableEdit edit){
		if (getUndoableEditSupport()!=null){
			getUndoableEditSupport().postEdit(edit);
		}

	}

	public boolean confirmRemove(List nodes) {
		return true;
	}


	protected boolean local,master=true;

	public boolean isLocal() {
		return local;
	}

	public void setLocal(boolean local) {
		this.local = local;
	}

	public boolean isMaster() {
		return master;
	}

	public void setMaster(boolean master) {
		this.master = master;
	}




//	protected int updateLevel=0;
//	protected synchronized void beginUpdate(){
//		updateLevel++;
//	}
//	protected synchronized void endUpdate(){
//		updateLevel--;
//	}
//	protected synchronized int getUpdateLevel(){
//		return updateLevel;
//	}


}

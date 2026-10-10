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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.microproject.undo.EditSupport;


import com.microproject.document.Document;
import com.microproject.document.ObjectEvent;
import com.microproject.association.Association;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.grouping.core.hierarchy.HierarchyUtils;
import com.microproject.grouping.core.hierarchy.MutableNodeHierarchy;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.AssignmentService;
import com.microproject.pm.assignment.HasAssignments;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.AssignmentCreationEdit;
import com.microproject.undo.AssignmentDeletionEdit;

/**
 *
 */
public class AssignmentNodeModel extends DefaultNodeModel implements ObjectEvent.Listener{
	protected Document document;
	protected boolean containsLeftObjects;


	public AssignmentNodeModel(Document document, boolean containsLeftObjects) {
		super();
		this.containsLeftObjects=containsLeftObjects;
		setDocument(document);
	}
	public AssignmentNodeModel(NodeModelDataFactory dataFactory) {
		this(dataFactory,null,false);
	}
	public AssignmentNodeModel(NodeModelDataFactory dataFactory,Document document, boolean containsLeftObjects) {
		super(dataFactory);
		this.containsLeftObjects=containsLeftObjects;
		setDocument(document);
	}
	AssignmentNodeModel(/*Vector list,*/ MutableNodeHierarchy hierarchy,
			NodeModelDataFactory dataFactory,Document document, boolean containsLeftObjects) {
		super(/*list,*/ hierarchy, dataFactory);
		this.containsLeftObjects=containsLeftObjects;
		setDocument(document);
	}


	public void objectChanged(ObjectEvent objectEvent) {
		if (objectEvent.getObject() instanceof Assignment assignment) {
			if (assignment.isDefault()) return;
			if (assignment.getDocument(containsLeftObjects) == document) { //TODO check if it's correct
				if (objectEvent.isCreate()) {
					Object parentObject=containsLeftObjects ? assignment.getLeft() : assignment.getRight();
							Node parent=search(parentObject);
					if (parent != null){ // the new assignment has to be added
						Node child=null;
						if (objectEvent.getInfo()!=null && dataFactory instanceof Project ){
							//don't want a node shared by Projet and ResourcePool
							if (objectEvent.getInfo().getNode()!=null) child=objectEvent.getInfo().getNode();
							else child = NodeFactory.getInstance().createNode(assignment);
						}
						else{
							//search if assignment already exists in hierarchy
							for (Enumeration<?> children=parent.children();children.hasMoreElements();){
								Node c=(Node) children.nextElement();
								if (c.getImpl()==assignment){
									child=c;
									break;
								}
							}
							if (child==null) child = NodeFactory.getInstance().createNode(assignment);
						}
						// Shared assignment models can receive the same redo create event.
						// Once the node already belongs to this parent there is nothing to
						// insert (and reinserting it would remove then insert at a stale index).
						boolean added = false;
						if (child.getParent() != parent) {
							int position=0;
							for (Enumeration<?> children=parent.children();children.hasMoreElements();position++){
								if (!(((Node) children.nextElement()).getImpl() instanceof Assignment))
									break;
							}
							position = Math.min(position, parent.getChildCount());
							add(parent,child,position,EVENT);
							added = true;
						}

						if (added && (objectEvent.getInfo()==null||(objectEvent.getInfo()!=null&&objectEvent.getInfo().isUndo()))&& dataFactory instanceof Project){
							EditSupport undoableEditSupport=getUndoableEditSupport();
							if (undoableEditSupport!=null){
								undoableEditSupport.postEdit(new AssignmentCreationEdit(child));
							}
						}

					}



				} else if (objectEvent.isDelete()) {
					Node node=search(assignment);
					if (node != null){
						remove(node,EVENT,false,false);
						if ((objectEvent.getInfo()==null||(objectEvent.getInfo()!=null&&objectEvent.getInfo().isUndo()))&& dataFactory instanceof Project){
							EditSupport undoableEditSupport=getUndoableEditSupport();
							if (undoableEditSupport!=null){
								undoableEditSupport.postEdit(new AssignmentDeletionEdit(node));
							}
						}
					}


				} else { //update
				}
			}
		}
	}

	public boolean isContainsLeftObjects() {
		return containsLeftObjects;
	}
	public void setContainsLeftObjects(boolean containsLeftObjects) {
		this.containsLeftObjects = containsLeftObjects;
	}

	public Document getDocument() {
		return document;
	}
	public void setDocument(Document document) {
		if (this.document!=null) this.document.removeObjectListener(this);
		this.document = document;
		if (document!=null) document.addObjectListener(this);
	}


	public void addAssignments(){
		addAssignments(iterator());
	}

	public void addAssignments(Iterator<Node> nodes){
		Map<Node, Node> assignments = new HashMap<>();
		while (nodes.hasNext()) { // go thru tasks or resources
			Node parent = nodes.next();
			if (!(parent.getImpl() instanceof HasAssignments hasAssignments)) {
				continue; //TODO currently getting voidNodeImpl's.  This should go away when fixed
			}
			for (Association association : hasAssignments.getAssignments()) {
				Assignment assignment = (Assignment) association;
				if (assignment.isDefault()) continue;
				Node child = NodeFactory.getInstance().createNode(assignment);
				assignments.put(child,parent);
			}
		}
		for (Map.Entry<Node, Node> assignmentEntry : assignments.entrySet()) {
			Node child = assignmentEntry.getKey();
			Node parent = assignmentEntry.getValue();

			//search if assignment already exists in hierarchy
			//fixes bug about adding a second assignment when the view is first shown
			boolean found = false;
			for (Enumeration<?> children = parent.children(); children.hasMoreElements();) {
				Node c = (Node) children.nextElement();
				if (c.getImpl()==child.getImpl()){
					child=c;
					found=true;
					break;
				}
			}

			if (!found) add(parent,child,SILENT);
		}
	}

	public boolean confirmRemove(List<Node> nodes) {
		// Deletion is reversible through Undo; per-node prompts duplicated the shared command flow.
		return true;
	}



	public void paste(Node parent,List<Node> nodes,int position,int actionType){
		super.paste(parent, nodes, position, actionType);
		List<Node> roots = new ArrayList<>(nodes.size());
		HierarchyUtils.extractParents(nodes, roots);
		List<Node> freeAssignments = new ArrayList<>();
		for (Node root : roots) {
			hierarchy.visitLeaves(root, value -> {
				Node node = (Node) value;
				if (node.getImpl() instanceof Assignment assignment) {
					Node owner = (Node) node.getParent();
					if (owner.getImpl() instanceof NormalTask task
							&& task.findAssignment(assignment.getResource()) == null) {
						freeAssignments.add(node);
					}
				}
			});
		}
		for (Node node : freeAssignments) {
			node.removeFromParent();
			AssignmentService.getInstance().connect(node,this,isUndo(actionType));
		}

	}



}

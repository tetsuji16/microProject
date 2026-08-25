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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import com.microproject.association.AssociationList;
import com.microproject.association.InvalidAssociationException;
import com.microproject.document.Document;
import com.microproject.document.ObjectEvent;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.event.HierarchyEvent;
import com.microproject.grouping.core.event.HierarchyListener;
import com.microproject.grouping.core.hierarchy.NodeHierarchy;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.dependency.HasDependencies;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.scheduling.ScheduleEvent;
import com.microproject.pm.scheduling.ScheduleEventListener;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.transaction.DomainChangeSet;
/**
 * This class lies between the SpreadSheet and the SpreadSheetModel.
 * It holds the states directly linked to the view.
 * The collapsed state and level of the nodes here.
 * The level is not a view state but it is calculated and cached for performance purposes.
 */

@SuppressWarnings("unchecked")
public class ReferenceNodeModelCache implements ObjectEvent.Listener, HierarchyListener, /*TreeModel,*/ ScheduleEventListener {
	private static final Logger logger = Logger.getLogger(ReferenceNodeModelCache.class.getName());
	private NodeModel model;
	
	protected NodeCache nodeCache;
	protected DependencyCache edgeCache;
	protected Document document;
	private final LegacyChangeAccumulator legacyChanges;
	private volatile boolean closed;
	
	protected int type;
		
	
	/**
	 * @param model
	 */
	public ReferenceNodeModelCache(NodeModel model, Document document, int type) {
		this.document = document;
		legacyChanges = document instanceof Project project
				? new LegacyChangeAccumulator(project.getDomainChangeJournal())
				: null;
		nodeCache=new NodeCache();
		edgeCache=new DependencyCache();
		setModel(model);
		this.type=type;
	}
//	public ReferenceNodeModelCache(NodeModel model) {
//		this(model,null);
//	}
	/**
	 * 
	 */
//	public ReferenceNodeModelCache(Document document) {
//		this.document = document;
//		nodeCache=new NodeCache();
//		edgeCache=new DependencyCache();
//	}

	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	
	public GraphicNode getGraphicNode(Node node) {
		return (GraphicNode)nodeCache.getElement(node);
	}
	public void bindView(VisibleNodes nodes,VisibleDependencies deps){
	    nodeCache.addVisibleElements(nodes);
	    edgeCache.addVisibleElements(deps);
	    //updateVisibleElements(nodes,deps,new HashSet());
	    //updateVisibleElements(nodes,new HashSet());
	}
	public void unbindView(VisibleNodes nodes,VisibleDependencies deps){
	    nodeCache.removeVisibleElements(nodes);
	    edgeCache.removeVisibleElements(deps);
	}
	
	
	
	public void close(){
		if (closed) return;
		closed = true;
		receiveEvents = false;
		if (legacyChanges != null)
			legacyChanges.close();
	    if (model!=null) {
	    	removeListeners();
	    	nodeCache.removeAllVisibleElements();
	    	nodeCache.clear();
	    	edgeCache.removeAllVisibleElements();
	    	edgeCache.clear();
	    }
	}
	
	private void removeListeners() {
		model.getHierarchy().removeHierarchyListener(this);
    	if (document!=null) document.removeObjectListener(this);
	    if (document!=null&&document instanceof Project) ((Project)document).removeScheduleListener(this);
	}
	
	
	public Document getDocument(){
	    return document;
	}
	boolean hasPendingLegacyChange() { return legacyChanges != null && legacyChanges.isPending(); }
	void flushPendingLegacyChange() { if (legacyChanges != null) legacyChanges.flushNowIfPending(); }
	
	
	
	public GraphicNode getParent(GraphicNode node){
		Node parent=getModel().getHierarchy().getParent(node.getNode()); //can be null
		return (GraphicNode)nodeCache.getElement(parent);
	}
	public List<Object> getChildren(GraphicNode node){
	    Collection<?> children=getModel().getHierarchy().getChildren((node==null)?null:node.getNode());
	    if (children==null) return null;
		List<Object> list=new ArrayList<>(children.size());
		for (Object value:children){
			Object child=nodeCache.getElement(value);
			if (child!=null) list.add(child);
		}
		return list;
	}
	public List<Object> getEdges(){
		return edgeCache.getCache();
	}
	
	
	public Object getGraphicNode(Object base){
		return nodeCache.getElement(base);
	}
	public Object getGraphicDependency(Object base){
		return edgeCache.getElement(base);
	}
	
	
	
//update	
	public void update(){
		update(new HashSet(),false);
	}
	public void update(boolean reschedule){
		update(new HashSet(),reschedule);
	}
	public void update(Set change,boolean reschedule){
//		System.out.println("ReferenceNodeModelCache update");
		NodeCache newCache=new NodeCache();
		update(null,newCache,change,reschedule);
		
		//edges
		Set edgeChange=new HashSet();
		updateEdges(edgeChange);
		
		
		nodeCache.copyContent(newCache);
		
		updateVisibleElements(change,edgeChange);
	}
	
	protected void updateVisibleElements(Set change,Set edgeChange){
//		long t0=System.currentTimeMillis();
		nodeCache.updateVisibleElements(change);
//		long t1=System.currentTimeMillis();
//		System.out.println("\tcache nodeCache.updateVisibleElements ran in "+(t1-t0)+"ms");

		edgeCache.updateVisibleElements(edgeChange);
//		t0=System.currentTimeMillis();
//		System.out.println("\tcache edgeCache.updateVisibleElements ran in "+(t0-t1)+"ms");

		nodeCache.fireEvents(this);
//		t1=System.currentTimeMillis();
//		System.out.println("\tcache nodeCache.fireEvents ran in "+(t1-t0)+"ms");

	}
	protected void updateVisibleElements(VisibleNodes nodes/*,Set change*/){
		nodeCache.updateVisibleElements(nodes,new HashSet());
		edgeCache.updateVisibleElements(nodes.getVisibleDependencies(),new HashSet());
		nodeCache.fireEvents(this,nodes);
	}
	
	
	
	public void updateEdges(Set change){
	    GraphicDependency current;
	    for (Iterator i=edgeCache.getCacheIterator();i.hasNext();){
	        current=(GraphicDependency)i.next();
	        if (current.isDirty()){
	            current.setDirty(false);
	            change.add(current);
	        }
	    }
	}
	
	public void update(GraphicNode node,NodeCache newCache, Set change,boolean reschedule){
		int level=(node==null)?0:node.getLevel();
		
		int collapseLevel=GraphicConfiguration.getInstance().getCollapseLevel();
		
		GraphicNode current;
		Collection children=model.getHierarchy().getChildren((node==null)?null:node.getNode());
		boolean summary=false;
		if (children!=null){
			Node child;
			for (Iterator i=children.iterator();i.hasNext();){
				child=(Node)i.next();
				Object impl=child.getImpl();
				if (!(impl instanceof Assignment)) summary=true;
				current=(GraphicNode) nodeCache.getElement(child);
				if (current==null){
					current=createNode(child);
					if (collapseLevel!=-1&&level>=collapseLevel-1) current.setCollapsed(true);
				}
				newCache.insertElement(current,current.getNode());
				if (current.getLevel()!=level+1){
					current.setLevel(level+1);
				}
				if (current.isVoid()&&(!current.getNode().isVoid())){
					current.setVoid(false);
				}else if (!current.isVoid()&&(current.getNode().isVoid())){
					current.setVoid(true);
				}
				if (reschedule&&impl instanceof Task&&((Task)impl).isJustModified()){
					current.setDirty(true);
				}
				if (current.isDirty()){
					change.add(current);
					current.setDirty(false);
				}
				update(current,newCache,change,reschedule);
			}
		}
		
		if (node!=null){
			boolean composite=!(children==null||children.size()==0);
			if (node.isComposite()!=composite){
				node.setComposite(composite);
			}
			if (node.isSummary()!=summary){
				node.setSummary(summary);
			}
			
			
			if (node.isDirty()){
				change.add(node);
				node.setDirty(false);
			}
			
			node.updateScheduleCache();
		}

	}
		
		
		
	
	
	
	/**
	 * @return Returns the model.
	 */
	public NodeModel getModel() {
		return model;
	}
	/**
	 * @param model The model to set.
	 */
	public void setModel(NodeModel model) {
	    if (this.model!=null) {
	    	removeListeners();
	    }
		this.model=model;//new FilteredNodeModel(model);
		//this.model.setFilter(filter);
	    model.getHierarchy().addHierarchyListener(this);
	    if (document!=null) document.addObjectListener(this);
	    if (document!=null&&document instanceof Project) ((Project)document).addScheduleListener(this);
		buildCache();
	}
	
	private void buildCache(){
		nodeCache.clear();
		edgeCache.clear();
		getModel().getHierarchy().checkEndVoidNodes(NodeModel.SILENT);
		update();
		buildEdges();
		syncEdges();
	}
	

	
	public void changeCollapsedState(GraphicNode gnode){
		if (gnode.isComposite()) gnode.setCollapsed(!gnode.isCollapsed());
		update();
	}
	
//edges
	public void buildEdges(){
		Map implMap=new HashMap();
		List gnodes=new ArrayList();
		for (Iterator i=nodeCache.getCache().iterator();i.hasNext();){
			GraphicNode gnode=(GraphicNode)i.next();
			if (gnode.isVoid()||gnode.isAssignment()) continue;
			if (!(gnode.getNode().getImpl() instanceof HasDependencies))
				continue; // only task-like nodes contribute dependency edges
			gnodes.add(gnode);
			implMap.put(gnode.getNode().getImpl(),gnode);
		}
		
		for (Iterator i=gnodes.iterator();i.hasNext();){
			GraphicNode gnode=(GraphicNode)i.next();
			
			HasDependencies task=(HasDependencies)gnode.getNode().getImpl();			
			AssociationList dependencyList=task.getSuccessorList();
			for (Iterator j=dependencyList.iterator();j.hasNext();){
				Dependency dep=(Dependency)j.next();
				
				HasDependencies pre=dep.getPredecessor();
				HasDependencies suc=dep.getSuccessor();
				GraphicNode preGNode=(GraphicNode)implMap.get(pre);
				GraphicNode sucGNode=(GraphicNode)implMap.get(suc);
				if (preGNode!=null&&sucGNode!=null){
					newGraphicDependency(preGNode,sucGNode,dep);
				} else {
					logger.log(Level.FINE, "no graphic node");
				}
			}
		}
	}
	
	
	public GraphicDependency newGraphicDependency(GraphicNode preGNode,GraphicNode sucGNode,Dependency dep){
		GraphicDependency gdep = new GraphicDependency(preGNode,sucGNode,dep);
		int depType=dep.getDependencyType();
		//gdep.setType(depType);
		edgeCache.insertElement(gdep,dep);
		return gdep;
	}
	
	
	private void syncEdges(){
		edgeCache.updateAllVisibleElements();
	}

	
	public void createDependency(GraphicNode startNode,GraphicNode endNode) throws InvalidAssociationException{
		DependencyService service=DependencyService.getInstance();
		HasDependencies startObject=(HasDependencies)startNode.getNode().getImpl();
		HasDependencies endObject=(HasDependencies)endNode.getNode().getImpl();
		//try {
			Dependency dep=service.newDependency(startObject,endObject,DependencyType.FS,0L,this);
		//} catch (InvalidAssociationException e) {
		//	e.printStackTrace();
		//}
	}
//	public void createHierarchyDependency(GraphicNode startNode,GraphicNode endNode){
//	    model.getHierarchy().move(endNode.getNode(),startNode.getNode());
//	}
	

	public void removeEdge(GraphicDependency dep){
		if (dep==null) return;
		edgeCache.deleteElement(dep);
	}
	public void modifyEdge(GraphicDependency dep,int type){
		if (type!=-1){
			//dep.setType(type);
		}
	}
	

	
	
	
	protected int getLevel(Node node){
	    int level=0;
	    NodeHierarchy hierarchy=getModel().getHierarchy();
	    for(Node current=node;current!=null;current=hierarchy.getParent(current)) level++;
	    return level;
	}
	protected boolean isComposite(Node node){
	    return !getModel().getHierarchy().isLeaf(node);
	}
	protected boolean isSummary(Node node){
	    return getModel().getHierarchy().isSummary(node);
	}
	
	public GraphicNode createNode(Node node){
		return new GraphicNode(node,-1);
	}
	
	
	
	protected boolean receiveEvents=true;
	public boolean isReceiveEvents() {
		return receiveEvents;
	}
	public void setReceiveEvents(boolean receiveEvents) {
		this.receiveEvents = receiveEvents;
	}
	
	public void scheduleChanged(ScheduleEvent e){
		if (closed || !receiveEvents) return;
		if (!SwingUtilities.isEventDispatchThread()) {
			recordLegacyChangeImmediately(e);
			SwingUtilities.invokeLater(() -> scheduleChangedOnEdt(e, false));
			return;
		}
		scheduleChangedOnEdt(e, true);
	}

	private void scheduleChangedOnEdt(ScheduleEvent e, boolean recordRevision) {
		if (closed || !receiveEvents) return;
		//System.out.println("ScheduleEvent: type="+e.getType()+", snap="+e.getSnapshot()+", object="+e.getObject());
		if (!receiveEvents) return;
		if (recordRevision) recordLegacyChange();
//		nodeCache.updateCachedSchedule();
//		nodeCache.fireScheduleEvent(e.getSource(),e);
		update(true);
	}
	
	
	public void objectChanged(ObjectEvent objectEvent) {
		if (closed || !receiveEvents) return;
		if (!SwingUtilities.isEventDispatchThread()) {
			recordLegacyChangeImmediately(objectEvent);
			SwingUtilities.invokeLater(() -> objectChangedOnEdt(objectEvent, false));
			return;
		}
		objectChangedOnEdt(objectEvent, true);
	}

	private void objectChangedOnEdt(ObjectEvent objectEvent, boolean recordRevision) {
		if (closed || !receiveEvents) return;
		//System.out.println("ObjectEvent: type="+objectEvent.getType()+", field="+objectEvent.getField()+", object="+objectEvent.getObject());
		if (!receiveEvents) return;
		Object object=objectEvent.getObject();
		if (object instanceof Dependency) {
			Dependency dependency = ((Dependency)object);
			if (dependency.getDocument() == document || dependency.getMasterDocument() == document) { // links can come from other projects too, but successor should be in this project
				if (recordRevision) recordLegacyChange();
				if (objectEvent.isCreate()) {
					Node preNode=(Node)model.search(dependency.getPredecessor());
					Node sucNode=(Node)model.search(dependency.getSuccessor());
					GraphicNode preGNode=(GraphicNode)nodeCache.getElement(preNode);
					GraphicNode sucGNode=(GraphicNode)nodeCache.getElement(sucNode);
					if (preGNode!=null&&sucGNode!=null){
						GraphicDependency edge=(GraphicDependency)edgeCache.getElement(dependency);
						if (edge == null) { // for external tasks in subprojects, it's possible they already were created
							edge=newGraphicDependency(preGNode,sucGNode,dependency);
							update();
						}
					}
				} else if (objectEvent.isDelete()) {
					GraphicDependency edge=(GraphicDependency)edgeCache.getElement(dependency);
					if (edge!=null){
						removeEdge(edge);
						update();
					}
					//edgeCache.fireEdgesRemoved(this,new Object[]{edge});
				} else { //update
					GraphicDependency edge=(GraphicDependency)edgeCache.getElement(dependency);
					if (edge!=null){
						modifyEdge(edge,dependency.getDependencyType());
						update();
					}
					//edgeCache.fireEdgesUpdated(this,new Object[]{edge});
				}
			}
		}else{
			if (object!=null&&((object instanceof Task && (type&NodeModelCache.TASK_TYPE)==NodeModelCache.TASK_TYPE)||
				(object instanceof Resource && (type&NodeModelCache.RESOURCE_TYPE)==NodeModelCache.RESOURCE_TYPE)||
				(object instanceof Assignment && (type&NodeModelCache.ASSIGNMENT_TYPE)==NodeModelCache.ASSIGNMENT_TYPE)||
				(object instanceof Project && (type&NodeModelCache.PROJECT_TYPE)==NodeModelCache.PROJECT_TYPE))){
				if (recordRevision) recordLegacyChange();
				if (object!=null&&!objectEvent.isDelete()){ //because node is already deleted
					Node node=model.search(object);
					if (node !=null) {
						for(;!node.isRoot();node=model.getParent(node)){
//						System.out.println("objectChanged "+objectEvent.getType()+": "+node);
						GraphicNode gnode=getGraphicNode(node);
						if (gnode != null) // on project list it is null
							gnode.setDirty(true);
						//nodeCache.fireObjectEvent(objectEvent.getSource(),objectEvent);
						}
					}
				}
				update();
			}
					
		}
	}

	
	
	public void nodesChanged(HierarchyEvent e) {
		if (closed || !receiveEvents) return;
		if (!SwingUtilities.isEventDispatchThread()) { recordLegacyChangeImmediately(e); SwingUtilities.invokeLater(() -> handleHierarchyEvent(e, false)); return; }
		handleHierarchyEvent(e, true);
	}
	private void handleHierarchyEvent(HierarchyEvent e, boolean recordRevision) {
		if (closed || !receiveEvents) return;
	    if (receiveEvents&&!e.isConsumed()) { if (recordRevision) recordLegacyChange(); update(); }
	}
	public void nodesInserted(HierarchyEvent e) {
		if (closed || !receiveEvents) return;
		if (!SwingUtilities.isEventDispatchThread()) { recordLegacyChangeImmediately(e); SwingUtilities.invokeLater(() -> handleHierarchyEvent(e, false)); return; }
		handleHierarchyEvent(e, true);
	}
	public void nodesRemoved(HierarchyEvent e) {
		if (closed || !receiveEvents) return;
		if (closed) return;
		if (!SwingUtilities.isEventDispatchThread()) { recordLegacyChangeImmediately(e); SwingUtilities.invokeLater(() -> handleHierarchyEvent(e, false)); return; }
		handleHierarchyEvent(e, true);
	}
	public void structureChanged(HierarchyEvent e) {
		if (closed || !receiveEvents) return;
		if (closed) return;
		if (!SwingUtilities.isEventDispatchThread()) { recordLegacyChangeImmediately(e); SwingUtilities.invokeLater(() -> handleHierarchyEvent(e, false)); return; }
		handleHierarchyEvent(e, true);
	}

	private void recordLegacyChange() {
		if (legacyChanges != null)
			legacyChanges.record();
	}

	private void recordLegacyChangeImmediately(Object eventIdentity) {
		if (legacyChanges != null)
			legacyChanges.recordImmediately(eventIdentity);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
//	public void addTreeModelListener(TreeModelListener arg0) {
//		model.addTreeModelListener(arg0);
//	}
//	public Object getChild(Object arg0, int arg1) {
//		return model.getChild(arg0, arg1);
//	}
//	public int getChildCount(Object arg0) {
//		return model.getChildCount(arg0);
//	}
//	public int getIndexOfChild(Object arg0, Object arg1) {
//		return model.getIndexOfChild(arg0, arg1);
//	}
	
	protected GraphicNode root=null; 
	public Object getRoot() {
		if (root==null) root=new GraphicNode((Node)model.getRoot(),0); 
		return root;
	}
//	public boolean isLeaf(Object arg0) {
//		return model.isLeaf(arg0);
//	}
//	public void removeTreeModelListener(TreeModelListener arg0) {
//		model.removeTreeModelListener(arg0);
//	}
//	public void valueForPathChanged(TreePath arg0, Object arg1) {
//		model.valueForPathChanged(arg0, arg1);
//	}
	
	
	
	
	public String toString(){
		return nodeCache.getVisibleElements().toString();
	}
	
	
	
	
	
	
	
	
	
	
}

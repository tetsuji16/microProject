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
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.association.AssociationList;
import com.microproject.association.Association;
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
/**
 * This class lies between the SpreadSheet and the SpreadSheetModel.
 * It holds the states directly linked to the view.
 * The collapsed state and level of the nodes here.
 * The level is not a view state but it is calculated and cached for performance purposes.
 */

public class ReferenceNodeModelCache implements ObjectEvent.Listener, HierarchyListener, ScheduleEventListener {
	private static final Logger logger = Logger.getLogger(ReferenceNodeModelCache.class.getName());
	private NodeModel model;
	
	protected NodeCache nodeCache;
	protected DependencyCache edgeCache;
	protected Document document;
	
	protected int type;
		
	
	/**
	 * @param model
	 */
	public ReferenceNodeModelCache(NodeModel model, Document document, int type) {
		this.document = document;
		nodeCache=new NodeCache();
		edgeCache=new DependencyCache();
		setModel(model);
		this.type=type;
	}
	/**
	 * 
	 */

	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	
	public GraphicNode getGraphicNode(Node node) {
		return nodeCache.getElement(node);
	}
	public void bindView(VisibleNodes nodes,VisibleDependencies deps){
	    nodeCache.addVisibleElements(nodes);
	    edgeCache.addVisibleElements(deps);
	}
	public void unbindView(VisibleNodes nodes,VisibleDependencies deps){
	    nodeCache.removeVisibleElements(nodes);
	    edgeCache.removeVisibleElements(deps);
	}
	
	
	
	public void close(){
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
	
	
	
	public GraphicNode getParent(GraphicNode node){
		Node parent=getModel().getHierarchy().getParent(node.getNode()); //can be null
		return nodeCache.getElement(parent);
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
	public List<GraphicDependency> getEdges(){
		return edgeCache.getCache();
	}
	
	
	public Object getGraphicNode(Object base){
		return nodeCache.getElement(base);
	}
	public Object getGraphicDependency(Object base){
		return edgeCache.getElement(base);
	}
	
	
	
	// update
	public void update(){
		update(new HashSet<GraphicNode>(),false);
	}
	public void update(boolean reschedule){
		update(new HashSet<GraphicNode>(),reschedule);
	}
	public void update(Set<? super GraphicNode> change,boolean reschedule){
		NodeCache newCache=new NodeCache();
		update(null,newCache,change,reschedule);
		
		//edges
		Set<GraphicDependency> edgeChange=new HashSet<>();
		updateEdges(edgeChange);
		
		
		nodeCache.copyContent(newCache);
		
		updateVisibleElements(change,edgeChange);
	}
	
	protected void updateVisibleElements(Set<?> change,Set<?> edgeChange){
		nodeCache.updateVisibleElements(change);

		edgeCache.updateVisibleElements(edgeChange);

		nodeCache.fireEvents(this);

	}
	protected void updateVisibleElements(VisibleNodes nodes){
		nodeCache.updateVisibleElements(nodes,new HashSet<GraphicNode>());
		edgeCache.updateVisibleElements(nodes.getVisibleDependencies(),new HashSet<GraphicDependency>());
		nodeCache.fireEvents(this,nodes);
	}
	
	
	
	public void updateEdges(Set<? super GraphicDependency> change){
	    GraphicDependency current;
	    for (ListIterator<GraphicDependency> i=edgeCache.getCacheIterator();i.hasNext();){
	        current=i.next();
	        if (current.isDirty()){
	            current.setDirty(false);
	            change.add(current);
	        }
	    }
	}
	
	public void update(GraphicNode node,NodeCache newCache, Set<? super GraphicNode> change,boolean reschedule){
		int level=(node==null)?0:node.getLevel();
		
		int collapseLevel=GraphicConfiguration.getInstance().getCollapseLevel();
		
		GraphicNode current;
		List<Node> children=model.getHierarchy().getChildren((node==null)?null:node.getNode());
		boolean summary=false;
		if (children!=null){
			for (Node child : children){
				Object impl=child.getImpl();
				if (!(impl instanceof Assignment)) summary=true;
				current=nodeCache.getElement(child);
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
		int nodeCount=nodeCache.getCacheSize();
		Map<Object, GraphicNode> implMap = HashMap.newHashMap(nodeCount);
		List<GraphicNode> gnodes = new ArrayList<>(nodeCount);
		for (GraphicNode gnode : nodeCache.getCache()) {
			if (gnode.isVoid()||gnode.isAssignment()) continue;
			if (!(gnode.getNode().getImpl() instanceof HasDependencies))
				continue; // only task-like nodes contribute dependency edges
			gnodes.add(gnode);
			implMap.put(gnode.getNode().getImpl(),gnode);
		}
		
		for (GraphicNode gnode : gnodes) {
			
			HasDependencies task=(HasDependencies)gnode.getNode().getImpl();			
			AssociationList dependencyList=task.getSuccessorList();
			for (Association association : dependencyList) {
				Dependency dep = (Dependency) association;
				
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
			Dependency dep=service.newDependency(startObject,endObject,DependencyType.Kind.FS,0L,this);
		//} catch (InvalidAssociationException e) {
		//	e.printStackTrace();
		//}
	}
	

	public void removeEdge(GraphicDependency dep){
		if (dep==null) return;
		edgeCache.deleteElement(dep);
	}
	public void modifyEdge(GraphicDependency dep,DependencyType.Kind type){
		if (type!=null){
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
		if (!receiveEvents) return;
		update(true);
	}
	
	
	public void objectChanged(ObjectEvent objectEvent) {
		if (!receiveEvents) return;
		Object object=objectEvent.getObject();
		if (object instanceof Dependency) {
			Dependency dependency = ((Dependency)object);
			if (dependency.getDocument() == document || dependency.getMasterDocument() == document) { // links can come from other projects too, but successor should be in this project
				if (objectEvent.isCreate()) {
					Node preNode=(Node)model.search(dependency.getPredecessor());
					Node sucNode=(Node)model.search(dependency.getSuccessor());
					GraphicNode preGNode=nodeCache.getElement(preNode);
					GraphicNode sucGNode=nodeCache.getElement(sucNode);
					if (preGNode!=null&&sucGNode!=null){
						GraphicDependency edge=edgeCache.getElement(dependency);
						if (edge == null) { // for external tasks in subprojects, it's possible they already were created
							edge=newGraphicDependency(preGNode,sucGNode,dependency);
							update();
						}
					}
				} else if (objectEvent.isDelete()) {
					GraphicDependency edge=edgeCache.getElement(dependency);
					if (edge!=null){
						removeEdge(edge);
						update();
					}
					//edgeCache.fireEdgesRemoved(this,new Object[]{edge});
				} else { //update
					GraphicDependency edge=edgeCache.getElement(dependency);
					if (edge!=null){
						modifyEdge(edge,dependency.getDependencyKind());
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
				if (object!=null&&!objectEvent.isDelete()){ //because node is already deleted
					Node node=model.search(object);
					if (node !=null) {
						for(;!node.isRoot();node=model.getParent(node)){
						GraphicNode gnode=getGraphicNode(node);
						if (gnode != null) // on project list it is null
							gnode.setDirty(true);
						}
					}
				}
				update();
			}
					
		}
	}

	
	
	public void nodesChanged(HierarchyEvent e) {
	    if (receiveEvents&&!e.isConsumed()) update();
	}
	public void nodesInserted(HierarchyEvent e) {
		if (receiveEvents&&!e.isConsumed()) update();
	}
	public void nodesRemoved(HierarchyEvent e) {
		if (receiveEvents&&!e.isConsumed()) update();
	}
	public void structureChanged(HierarchyEvent e) {
		if (receiveEvents&&!e.isConsumed()) update();
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	protected GraphicNode root=null; 
	public Object getRoot() {
		if (root==null) root=new GraphicNode((Node)model.getRoot(),0); 
		return root;
	}
	
	
	
	
	public String toString(){
		return nodeCache.getVisibleElements().toString();
	}
	
	
	
	
	
	
	
	
	
	
}


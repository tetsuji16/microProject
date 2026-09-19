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
package com.microproject.pm.graphic.graph;

import java.io.Serializable;
import java.util.EventListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.swing.event.EventListenerList;

import com.microproject.pm.graphic.graph.event.GraphEvent;
import com.microproject.pm.graphic.graph.event.GraphListener;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.event.CacheListener;
import com.microproject.pm.graphic.model.event.CompositeCacheEvent;
import com.microproject.configuration.Configuration;
import com.microproject.field.Field;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.util.Environment;

/**
 *
 */
public class GraphModel implements Serializable, /*ScheduleEventListener,*/ CacheListener  /*ObjectEvent.Listener*/{
	private static final long serialVersionUID = -6589463266745797527L;
	protected NodeModelCache cache;
    protected BarStyles barStyles;
    protected Project project;
    protected static Field nameField=Configuration.getFieldFromId("Field.name");
    
    
    
	public GraphModel(Project project,String viewName) {
		this.project = project;
	}

	
	public void close(){
		setCache(null);
	}
	
	public BarStyles getBarStyles() {
		return barStyles;
	}
	public void setBarStyles(BarStyles barStyles) {
		this.barStyles = barStyles;
	}

	
	
//cache: nodes
	public NodeModelCache getCache() {
		return cache;
	}
	public void setCache(NodeModelCache cache){
		if (this.cache!=null){
			this.cache.removeNodeModelListener(this);
		}
		this.cache = cache;
		cache.addNodeModelListener(this);
		
	}
	
	
	public ListIterator getNodeIterator(){
		return cache.getIterator();
	}
	public ListIterator getNodeIterator(int i){
		return cache.getIterator(i);
	}
	public ListIterator getDependencyIterator(){
		return cache.getEdgesIterator();
	}
	
	
	

	
	public List searchJustModifiedNodes(){
		List<GraphicNode> gnodes=new LinkedList<GraphicNode>();
		GraphicNode gnode;
		Object impl;
		for (Iterator<?> i=getCache().getIterator();i.hasNext();){
			gnode=(GraphicNode)i.next();
			impl=gnode.getNode().getImpl();
			if (impl instanceof Task){
				if (((Task)impl).isJustModified()) gnodes.add(gnode);
			}else if (impl instanceof Assignment){ //assignment
				if (((Task)getCache().getModel().getParent(gnode.getNode()).getImpl()).isJustModified()) gnodes.add(gnode);
			}
		}
		return gnodes;
	}
	public List searchNode(Object impl){
		List<GraphicNode> gnodes=new LinkedList<GraphicNode>();
		GraphicNode gnode;
		for (Iterator<?> i=getCache().getIterator();i.hasNext();){
			gnode=(GraphicNode)i.next();
			if (gnode.getNode().getImpl()==impl){
				gnodes.add(gnode);
				break;
			}
		}
		return gnodes;
	}
	
	
	//ScheduleEventListener
	 
	//NodeModelListener
	public void graphicNodesCompositeEvent(CompositeCacheEvent compositeEvent){
	    if (compositeEvent.getRemovedNodes()!=null) remove(compositeEvent.getRemovedNodes(),false);
        
	    if (compositeEvent.getInsertedNodes()!=null) insert(compositeEvent.getInsertedNodes(),false);
        
//	    updateAll(false);
	    update(compositeEvent.getUpdatedNodes(),false);
 
	    if (compositeEvent.getRemovedEdges()!=null) removeEdges(compositeEvent.getRemovedEdges(),false);
	    
	    if (compositeEvent.getInsertedEdges()!=null) insertEdges(compositeEvent.getInsertedEdges(),false);
        
        if (compositeEvent.getUpdatedEdges()!=null) updateEdges(compositeEvent.getUpdatedEdges(),false);
	}

	
	public void updateAll(boolean event) {
		update(null,event);
	}
	
	protected void update(List nodes, boolean event){
		fireUpdate(this,nodes);
	}
	
	public void insertAll(boolean event){
		if (cache.getSize()>0) insert(null,true,event);
	}	
	protected void insert(List nodes,boolean event){
		insert(nodes,false,event);
	}
	
	
	protected void insert(List nodes,boolean init,boolean event){
	}
	
	public void removeAll(boolean event) {
		remove((List)null,event);
	}
	
	protected void remove(List nodes,boolean event){
	}
	
	
	protected void insertEdges(List edges, boolean event){
	}
	
	protected void removeEdges(List edges, boolean event){
	}
	
	protected void updateEdges(List edges, boolean event){
	}

	
	
	

	
	

	
	//view events
	protected EventListenerList listenerList = new EventListenerList();

	public void addGraphListener(GraphListener l) {
		listenerList.add(GraphListener.class, l);
	}
	public void removeGraphListener(GraphListener l) {
		listenerList.remove(GraphListener.class, l);
	}
	public GraphListener[] getGraphListeners() {
		return (GraphListener[]) listenerList.getListeners(GraphListener.class);
	}
	 protected void fireUpdate(Object source,List nodes) {
			Object[] listeners = listenerList.getListenerList();
			GraphEvent e = null;
			for (int i = listeners.length - 2; i >= 0; i -= 2) {
				if (listeners[i] == GraphListener.class) {
					if (e == null) {
						e = new GraphEvent(source,nodes);
					}
					((GraphListener) listeners[i + 1]).updateGraph(e);
				}
			}
		}



    public EventListener[] getListeners(Class listenerType) { 
    	return listenerList.getListeners(listenerType); 
       }
	
    public boolean isReadOnly(){
    	NodeModel nodeModel=cache.getModel();
    	return !nodeModel.isLocal()&&!nodeModel.isMaster()&&!Environment.getStandAlone();
    }
	

	
}


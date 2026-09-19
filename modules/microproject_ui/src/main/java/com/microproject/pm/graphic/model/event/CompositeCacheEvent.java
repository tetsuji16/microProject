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
package com.microproject.pm.graphic.model.event;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.ListUtils;

/**
 *
 */
public class CompositeCacheEvent extends EventObject {
    protected List nodeEvents;
    protected List edgeEvents;
   
    /**
     * @param source
     * @param nodeEvents
     * @param edgeEvents
     */
    public CompositeCacheEvent(Object source,List nodeEvents,
            List edgeEvents) {
        super(source);
        this.nodeEvents = nodeEvents;
        this.edgeEvents = edgeEvents;
    }
   
    public List getEdgeEvents() {
        return edgeEvents;
    }
    public void setEdgeEvents(List edgeEvents) {
        this.edgeEvents = edgeEvents;
    }
    public List getNodeEvents() {
        return nodeEvents;
    }
    public void setNodeEvents(List nodeEvents) {
        this.nodeEvents = nodeEvents;
    }
    
//	public ScheduleEvent getScheduleEvent() {
//		return scheduleEvent;
//	}
//	public ObjectEvent getObjectEvent() {
//		return objectEvent;
//	}
	
    public String toString(){
        return "CompositeGraphicNodeEvent: \n\t"+nodeEvents+" \n\t"+edgeEvents;
    }

    
    protected List insertedNodes;
    protected List removedNodes;
    protected List updatedNodes;
    protected List insertedEdges;
    protected List removedEdges;
    protected List updatedEdges;
    protected boolean diffListsGenerated=false;
    private void generateDiffLists(){
        if (diffListsGenerated) return;
        
        
        //nodes
        CacheEvent event;
        List nodes;
        for (Iterator i=nodeEvents.iterator();i.hasNext();){
            event=(CacheEvent)i.next();
            nodes=event.getNodes();
            switch (event.getType()) {
            case CacheEvent.NODES_CHANGED:
                if (nodes!=null&&nodes.size()>0){
                    if (updatedNodes==null) updatedNodes=new ArrayList(nodes.size());
                    updatedNodes.addAll(nodes);
                }
                break;
            case CacheEvent.NODES_INSERTED:
//              check for hidden updates
                if (removedNodes!=null){
                    List inter=ListUtils.intersection(nodes,removedNodes);
                    if (inter.size()>0){
                        removedNodes.removeAll(inter);
                        nodes.removeAll(inter);
                        if (updatedNodes==null) updatedNodes=new ArrayList(nodes.size());
                        updatedNodes.addAll(inter);
                    }
                }
                
                if (nodes!=null&&nodes.size()>0){
                    if (insertedNodes==null) insertedNodes=new ArrayList(nodes.size());
                    insertedNodes.addAll(nodes);
                }
                break;
            case CacheEvent.NODES_REMOVED:
                //INSERT FOLLOWED BY REMOVE NEVER HAPPENS
                //nothing special to handle
                if (nodes!=null&&nodes.size()>0){
                    if (removedNodes==null) removedNodes=new ArrayList(nodes.size());
                    removedNodes.addAll(nodes);
                }
                break;
            default:
                break;
            }
        }
        
        
        //edges
        for (Iterator i=edgeEvents.iterator();i.hasNext();){
            event=(CacheEvent)i.next();
            nodes=event.getNodes();
            switch (event.getType()) {
            case CacheEvent.NODES_CHANGED:
                if (nodes!=null&&nodes.size()>0){
                    if (updatedEdges==null) updatedEdges=new ArrayList(nodes.size());
                    updatedEdges.addAll(nodes);
                }
                break;
            case CacheEvent.NODES_INSERTED:
                if (nodes!=null&&nodes.size()>0){
                    if (insertedEdges==null) insertedEdges=new ArrayList(nodes.size());
                    insertedEdges.addAll(nodes);
                }
                break;
            case CacheEvent.NODES_REMOVED:
                if (nodes!=null&&nodes.size()>0){
                    if (removedEdges==null) removedEdges=new ArrayList(nodes.size());
                    removedEdges.addAll(nodes);
                }
                break;
            default:
                break;
            }
        }
        
        diffListsGenerated=true;
    }

    
    
    public List getInsertedNodes() {
        generateDiffLists();
        return insertedNodes;
    }
    public List getRemovedNodes() {
        generateDiffLists();
        return removedNodes;
    }
    public List getUpdatedNodes() {
        generateDiffLists();
        return updatedNodes;
    }
    public List getInsertedEdges() {
        generateDiffLists();
        return insertedEdges;
    }
    public List getRemovedEdges() {
        generateDiffLists();
        return removedEdges;
    }
    public List getUpdatedEdges() {
        generateDiffLists();
        return updatedEdges;
    }
    
    public boolean isNodeHierarchy(){
    	return nodeEvents!=null;
    }
}


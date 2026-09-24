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
import java.util.List;

/**
 *
 */
public class CompositeCacheEvent extends EventObject {
    protected List<CacheEvent> nodeEvents;
    protected List<CacheEvent> edgeEvents;
   
    /**
     * @param source
     * @param nodeEvents
     * @param edgeEvents
     */
    public CompositeCacheEvent(Object source, List<CacheEvent> nodeEvents,
            List<CacheEvent> edgeEvents) {
        super(source);
        this.nodeEvents = nodeEvents;
        this.edgeEvents = edgeEvents;
    }
   
    public List<CacheEvent> getEdgeEvents() {
        return edgeEvents;
    }
    public void setEdgeEvents(List<CacheEvent> edgeEvents) {
        this.edgeEvents = edgeEvents;
    }
    public List<CacheEvent> getNodeEvents() {
        return nodeEvents;
    }
    public void setNodeEvents(List<CacheEvent> nodeEvents) {
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

    
    protected List<Object> insertedNodes;
    protected List<Object> removedNodes;
    protected List<Object> updatedNodes;
    protected List<Object> insertedEdges;
    protected List<Object> removedEdges;
    protected List<Object> updatedEdges;
    protected boolean diffListsGenerated=false;
    private void generateDiffLists(){
        if (diffListsGenerated) return;
        
        
        //nodes
        for (CacheEvent event : nodeEvents) {
            List<?> nodes = event.getNodes();
            switch (event.getType()) {
            case CacheEvent.NODES_CHANGED:
                if (nodes != null && !nodes.isEmpty()) {
                    if (updatedNodes == null) updatedNodes = new ArrayList<>(nodes.size());
                    updatedNodes.addAll(nodes);
                }
                break;
            case CacheEvent.NODES_INSERTED:
//              check for hidden updates
                if (nodes != null && !nodes.isEmpty()) {
                    if (removedNodes != null) {
                        List<Object> intersection = new ArrayList<>();
                        for (Object node : nodes) {
                            if (removedNodes.contains(node)) {
                                intersection.add(node);
                            }
                        }
                        if (!intersection.isEmpty()) {
                            removedNodes.removeAll(intersection);
                            List<Object> remainingNodes = new ArrayList<>(nodes);
                            remainingNodes.removeAll(intersection);
                            if (updatedNodes == null) updatedNodes = new ArrayList<>(nodes.size());
                            updatedNodes.addAll(intersection);
                            nodes = remainingNodes;
                        }
                    }
                }
                
                if (nodes != null && !nodes.isEmpty()) {
                    if (insertedNodes == null) insertedNodes = new ArrayList<>(nodes.size());
                    insertedNodes.addAll(nodes);
                }
                break;
            case CacheEvent.NODES_REMOVED:
                //INSERT FOLLOWED BY REMOVE NEVER HAPPENS
                //nothing special to handle
                if (nodes != null && !nodes.isEmpty()) {
                    if (removedNodes == null) removedNodes = new ArrayList<>(nodes.size());
                    removedNodes.addAll(nodes);
                }
                break;
            default:
                break;
            }
        }
        
        
        //edges
        for (CacheEvent event : edgeEvents) {
            List<?> nodes = event.getNodes();
            switch (event.getType()) {
            case CacheEvent.NODES_CHANGED:
                if (nodes != null && !nodes.isEmpty()) {
                    if (updatedEdges == null) updatedEdges = new ArrayList<>(nodes.size());
                    updatedEdges.addAll(nodes);
                }
                break;
            case CacheEvent.NODES_INSERTED:
                if (nodes != null && !nodes.isEmpty()) {
                    if (insertedEdges == null) insertedEdges = new ArrayList<>(nodes.size());
                    insertedEdges.addAll(nodes);
                }
                break;
            case CacheEvent.NODES_REMOVED:
                if (nodes != null && !nodes.isEmpty()) {
                    if (removedEdges == null) removedEdges = new ArrayList<>(nodes.size());
                    removedEdges.addAll(nodes);
                }
                break;
            default:
                break;
            }
        }
        
        diffListsGenerated=true;
    }

    
    
    public List<Object> getInsertedNodes() {
        generateDiffLists();
        return insertedNodes;
    }
    public List<Object> getRemovedNodes() {
        generateDiffLists();
        return removedNodes;
    }
    public List<Object> getUpdatedNodes() {
        generateDiffLists();
        return updatedNodes;
    }
    public List<Object> getInsertedEdges() {
        generateDiffLists();
        return insertedEdges;
    }
    public List<Object> getRemovedEdges() {
        generateDiffLists();
        return removedEdges;
    }
    public List<Object> getUpdatedEdges() {
        generateDiffLists();
        return updatedEdges;
    }
    
    public boolean isNodeHierarchy(){
    	return nodeEvents!=null;
    }
}


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
package com.microproject.pm.graphic.pert;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.microproject.pm.graphic.model.cache.GraphicDependency;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.NodeModelCache;

/**
 *
 */
public class DependencyGraph{
	protected HashMap nodeMap =new HashMap();
	protected NodeModelCache cache;
	
	public void setCache(NodeModelCache cache){
		this.cache=cache;
		nodeMap.clear();
	}
	
	public void insertDependency(GraphicDependency dependency){
		//System.out.println("insertDependency");
	    GraphicNode preValue=(GraphicNode)dependency.getPredecessor();
	    GraphicNode sucValue=(GraphicNode)dependency.getSuccessor();
	    Node pre=(Node)nodeMap.get(preValue);
	    if (pre==null){
	        pre=new Node(preValue);
	        nodeMap.put(preValue,pre);
	    }
	    Node suc=(Node)nodeMap.get(sucValue);
	    if (suc==null){
	        suc=new Node(sucValue);
	        nodeMap.put(sucValue,suc);
	    }
	    
	    pre.addSuccessor(suc);
	    suc.addPredecessor(pre);
	}
	public void removeDependency(GraphicDependency dependency){
		//System.out.println("removeDependency");
	    GraphicNode preValue=(GraphicNode)dependency.getPredecessor();
	    GraphicNode sucValue=(GraphicNode)dependency.getSuccessor();
	    Node pre=(Node)nodeMap.get(preValue);
	    Node suc=(Node)nodeMap.get(sucValue);
	    if (pre==null||suc==null)return;
	    
	    pre.removeSuccessor(suc);
	    suc.removePredecessor(pre);
	    if (pre.isolated()) nodeMap.remove(pre.getValue());
	    if (suc.isolated()) nodeMap.remove(suc.getValue());
	}
	
	public void insertDependencies(List dependencies){
	    for (Iterator i=dependencies.iterator();i.hasNext();) insertDependency((GraphicDependency)i.next());
	}
	public void removeDependencies(List dependencies){
	    for (Iterator i=dependencies.iterator();i.hasNext();) removeDependency((GraphicDependency)i.next());
	}
	
	
	public void updatePertLevels(){
//		System.out.println("updatePertLevels");
	    for (Iterator i=cache.getIterator();i.hasNext();){
	        resetCachePertLevel((GraphicNode)i.next());
	    }
	    
	    Set predecessors=new HashSet();
	    Set successors=new HashSet();
	    for (Iterator i=nodeMap.values().iterator();i.hasNext();){
	        Node node=(Node)i.next();
	        GraphicNode gnode=(GraphicNode)node.getValue();
	        //resetCachePertLevel(gnode);
	        if (node.getPredecessors().size()==0) predecessors.add(node);
	    }
	    
	    while (predecessors.size()>0){
	        updateSuccessorsPertLevel(predecessors,successors);
	        Set tmp=predecessors;
	        predecessors=successors;
	        successors=tmp;
	        successors.clear();
	    }
	}
	
	
	private void updateSuccessorsPertLevel(Set predecessors,Set successors){
	    for (Iterator i=predecessors.iterator();i.hasNext();){
	        Node pre=(Node)i.next();
	        GraphicNode gpre=(GraphicNode)pre.getValue();
	        for (Iterator j=pre.getSuccessors().iterator();j.hasNext();){
	            Node suc=(Node)j.next();
	            successors.add(suc);
		        GraphicNode gsuc=(GraphicNode)suc.getValue();
		        correctPertLevel(gpre,gsuc);
	        }
	    }
	}
	
	private void resetCachePertLevel(GraphicNode gnode){
	    cache.setPertLevel(gnode,cache.getLevel(gnode));
	}
	private void correctPertLevel(GraphicNode gpre,GraphicNode gsuc){
        if (cache.getPertLevel(gsuc)<=cache.getPertLevel(gpre)){
            cache.setPertLevel(gsuc,cache.getPertLevel(gpre)+1);
        }
	}
	
	
	
	public class Node{
	    protected Object value;
	    protected List predecessors;
	    protected List successors;
	    public Node(Object value){
	        this.value=value;
	        predecessors=new LinkedList();
	        successors=new LinkedList();
	    }
        public Object getValue() {
            return value;
        }
        public void setValue(Object value) {
            this.value = value;
        }
        
        public void addSuccessor(Node successor){
            successors.add(successor);
        }
        public void removeSuccessor(Node successor){
            successors.remove(successor);
        }
        public List getSuccessors(){
            return successors;
        }
        
        public void addPredecessor(Node predecessor){
            predecessors.add(predecessor);
        }
        public void removePredecessor(Node predecessor){
            predecessors.remove(predecessor);
        }
        public List getPredecessors(){
            return predecessors;
        }
        
        public boolean isolated(){
            return predecessors.size()==0&&successors.size()==0;
        }
        
	}

}


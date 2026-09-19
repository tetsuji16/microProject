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
package com.microproject.pm.criticalpath;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.tree.DefaultMutableTreeNode;

import com.microproject.pm.dependency.HasDependencies;

/**
 * Modified version of PredessorTaskList for PERT layouting
 * It builds a PredessorTaskList and convert it to a tree
 *
*/
public class PredecessorTaskTree {
	private static final Logger logger = Logger.getLogger(PredecessorTaskTree.class.getName());
	protected PertLayoutTreeNode root=new PertLayoutTreeNode(null);
	protected Map taskMap=new HashMap();
	
	public PertLayoutTreeNode getRoot(){
		return root;
	}
	
	protected PertLayoutTreeNode getNode(HasDependencies task){
		PertLayoutTreeNode node=(PertLayoutTreeNode)taskMap.get(task);
		if (node==null) {
			node=new PertLayoutTreeNode(task);
			taskMap.put(task,node);
		}
		return node;
	}
	
	public void addTask(HasDependencies hasDependencies) {
	    if (taskMap.containsKey(hasDependencies)) return;
	    PertLayoutTreeNode node=getNode(hasDependencies);
		arrangeTask(node);
		dump();
		
	}
	public void removeTask(HasDependencies task) {
		if (!taskMap.containsKey(task)) return;
		PertLayoutTreeNode node=getNode(task);
		PertLayoutTreeNode parent=(PertLayoutTreeNode)node.getParent();
		if (parent!=null) parent.remove(node);
		taskMap.remove(task);
		dump();
	}

	protected void arrangeTask(PertLayoutTreeNode node) {
	    HasDependencies task=(HasDependencies)node.getUserObject();
	    PertLayoutTreeNode current;
	    HasDependencies currentTask;
	    for (Enumeration e=root.postorderEnumeration();e.hasMoreElements();){
	        current=(PertLayoutTreeNode)e.nextElement();
	        currentTask=(HasDependencies)current.getUserObject();
	        if (currentTask==null||task.dependsOn(currentTask)){
	            for (Enumeration f=current.children();f.hasMoreElements();){
	                PertLayoutTreeNode currentChild=(PertLayoutTreeNode)f.nextElement();
	                HasDependencies currentChildTask=(HasDependencies)currentChild.getUserObject();
	                if (currentChildTask.dependsOn(task)) node.add(currentChild);
	            }
	            current.add(node);
	            break;
	        }
	    }
	    
    }
    
	public Enumeration enumeration() {
		return root.preorderEnumeration();
	}
	
	/*protected void cleanTree(){
		//removeChildren(root);
		for (Iterator i=taskMap.values().iterator();i.hasNext();){
			PertLayoutTreeNode node=(PertLayoutTreeNode)i.next();
			node.removeFromParent();
			node.removeAllChildren();
		}
	}*/
	
	public void rearrangeAll() {
		//cleanTree();
	}
	
	
	private void dumpChildren(PertLayoutTreeNode node,String prefix){
		Enumeration childrens=node.children();
		logger.log(Level.FINE, "{0}{1}", new Object[] { prefix, (node==root) ? "" : (node + "") });
		while (childrens.hasMoreElements()){
			dumpChildren((PertLayoutTreeNode)childrens.nextElement(),"-"+prefix);
		}
	}
	public void dump() {
		dumpChildren(root,">");
		Enumeration e=enumeration();
		while (e.hasMoreElements()) {
			logger.log(Level.FINE, "node: {0}", e.nextElement());
		}
	}
	
	public class PertLayoutTreeNode extends DefaultMutableTreeNode{
	    //protected boolean dirty;
        public PertLayoutTreeNode(Object userObject) {
            super(userObject);
            //dirty=true;
        }
        /*public boolean isDirty() {
            return dirty;
        }
        public void setDirty(boolean dirty) {
            this.dirty = dirty;
        }*/
        
        
	}
	
	
}

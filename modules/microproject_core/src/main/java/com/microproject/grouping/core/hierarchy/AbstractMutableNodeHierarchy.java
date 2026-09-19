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

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.event.EventListenerList;
import javax.swing.tree.TreeNode;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Predicate;

import com.microproject.grouping.core.LazyParent;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeBridge;
import com.microproject.grouping.core.event.HierarchyEvent;
import com.microproject.grouping.core.event.HierarchyListener;
import com.microproject.pm.key.HasKey;

/**
 *
 */
public abstract class AbstractMutableNodeHierarchy implements NodeHierarchy{
    private static final Logger logger = Logger.getLogger(AbstractMutableNodeHierarchy.class.getName());
	public abstract Object getRoot();
	public Object getChild(Object parent, int index) {
		return ((Node)parent).getChildAt(index);
	}
	public int getChildCount(Object parent) {
		return ((Node)parent).getChildCount();
	}
	public int getIndexOfChild(Object parent, Object child) {
		return ((Node)parent).getIndex((Node)child);
	}
    public int getIndexOfNode(Node key, boolean skipVoid) {
    	return getIndexOfNode((Node)getRoot(),key,new Counter(),skipVoid);
    }
  
	public Iterator iterator(Node rootNode){
		NodeBridge r;
		if (rootNode!=null && rootNode instanceof NodeBridge) r=(NodeBridge)rootNode;
		else r=(NodeBridge)getRoot();
		return IteratorUtils.asIterator(r.preorderEnumeration());
	}
	public Iterator iterator(){
		return iterator(null);
	}
	
    final class ShallowPreorderInterator implements Iterator<TreeNode> {
    	protected Stack stack;
    	protected int maxLevel;
    	protected Stack<Integer> levelStack;

    	public ShallowPreorderInterator(TreeNode rootNode,int maxLevel,boolean returnRoot) {
    	    super();
    	    ArrayList v = new ArrayList(1);
    	    v.add(rootNode);	// PENDING: don't really need a ArrayList stack = new Stack();
    	    stack.push(Collections.enumeration(v));
    	    levelStack=new Stack<Integer>();
    	    levelStack.push(0);
    	    this.maxLevel=maxLevel;
    	    if (!returnRoot&&hasNext()) next(); 
    	}

    	public boolean hasNext() {
    	    return (!stack.empty() &&
    		    ((Enumeration)stack.peek()).hasMoreElements());
    	}

    	public TreeNode next() {
    	    Enumeration	enumer = (Enumeration)stack.peek();
    	    int level=levelStack.peek();
    	    TreeNode	node = (TreeNode)enumer.nextElement();
    	    Enumeration	children = level==maxLevel?null:node.children();

    	    if (!enumer.hasMoreElements()) {
    		stack.pop();
    		levelStack.pop();
    	    }
    	    if (children!=null&&children.hasMoreElements()) {
    		stack.push(children);
    		levelStack.push(level+1);
    	    }
    	    return node;
    	}
		public void remove() {
			throw new UnsupportedOperationException("Remove not supported");
		}
    }

	
	public Iterator shallowIterator(int maxLevel,boolean returnRoot){
		return new ShallowPreorderInterator((TreeNode)getRoot(),maxLevel,returnRoot);
	}
	

    
    private class Counter { // an int object that is mutable
    	int count = 0;
    }
    
    private int getIndexOfNode(Node node, Node key, Counter counter, boolean skipVoid) {
    	if (key == node)
    		return counter.count;
    	if (!skipVoid || !node.isVirtual())
    		counter.count++;
    	Collection children = getChildren(node);
    	if (children == null)
    		return -1;
    	Iterator i = children.iterator();
    	int found = -1;
    	while (i.hasNext()) {
    		if ((found = getIndexOfNode(key,(Node)i.next(),counter,skipVoid)) != -1)
    			break;
    	}
    	return found;
    	
    }
    public void visitAll(Consumer<Object> visitor) {
    	visitAll(null,visitor);
    }
    public void visitAll(Node parent, Consumer<Object> visitor) {
    	if (parent != null)
    		visitor.accept(parent);
    	Collection children = getChildren(parent);
    	if (children != null) {
        	Iterator i = children.iterator();
        	while (i.hasNext()) {
        		visitAll((Node)i.next(),visitor);
        	}
    	}
    }
    //doesn't visit parent
    public void visitAllLevelOrder(Node parent, boolean skipLazyParents, Consumer<Object> visitor) {
    	visitAllLevelOrder(true,parent,skipLazyParents,visitor);
    }

   	public void visitAllLevelOrder(boolean first, Node parent, boolean skipLazyParents, Consumer<Object> visitor) {
   		// when saving a project, we do not want to save the children of subproject tasks, except when 
   		// saving a suproject itself, in which case, the root element will be a subproject task and first will be true
    	if (!first && skipLazyParents && parent != null && parent.getImpl() instanceof LazyParent) 
    		return;
    	Collection children = getChildren(parent);
    	if (children != null) {
        	Iterator i = children.iterator();
        	while (i.hasNext()) {
        		visitor.accept(i.next());
        	}
        	i=children.iterator();
        	while (i.hasNext()) {
        		visitAllLevelOrder(false,(Node)i.next(),skipLazyParents,visitor);
        	}
    	}
    }
   	
    public void visitAll(Node parent, boolean skipLazyParents, Consumer<Object> visitor) {
    	visitAll(true,parent,skipLazyParents,visitor);
    }
   	public void visitAll(boolean first, Node parent, boolean skipLazyParents, Consumer<Object> visitor) {
   		// when saving a project, we do not want to save the children of subproject tasks, except when 
   		// saving a suproject itself, in which case, the root element will be a subproject task and first will be true
    	if (!first && skipLazyParents && parent != null && parent.getImpl() instanceof LazyParent) 
    		return;
    	Collection children = getChildren(parent);
    	if (children != null) {
        	Iterator i = children.iterator();
        	i=children.iterator();
        	while (i.hasNext()) {
        		Node node=(Node)i.next();
        		visitor.accept(node);
        		visitAll(false,node,skipLazyParents,visitor);
        	}
    	}
    }
   	
    public void visitLeaves(Node node, Consumer<Object> visitor) {
    	if (node.isLeaf()) visitor.accept(node);
    	else for (Enumeration e=node.children();e.hasMoreElements();){
    		visitLeaves((Node)e.nextElement(), visitor);
    	}
    }


/**
 * Get next non virtual node
 */    
    public Node getNext(Node current) {
    	Node node = current;
    	while (true) {
    		node = getNext(node,true);
    		if (node == null || !node.isVirtual())
    			break;
    	}
    	return node;
    }
    
    private Node getNext(Node current, boolean doChildren) {
    	List children;
    	if (doChildren) { // if haven't visited children yet
    		children = getChildren(current);
       		if (children != null && children.size() > 0) // if parent, next is first child
    			return (Node)children.get(0);
    	}
       	if (current == null) // null parent has no parent
       		return null;
   		Node parent =getParent(current);
		children = getChildren(parent);
		Iterator i = children.iterator();
        while (i.hasNext()) { // get next element after this one.  If it is the last then try its parent
        	if (i.next() == current) {
        		if (i.hasNext())
        			return (Node)i.next();
        		else
        			return getNext(parent,false);
        	}
        }
        return null;
    }
    public Node getPrevious(Node current) {
    	Node node = current;
    	while (true) {
    		node = getPrevious(node,true);
    		if (node == null || !node.isVirtual())
    			break;
    	}
    	return node;
    }
    
    private Node getPrevious(Node current, boolean doChildren) {
       	if (current == null) // null parent has no parent
       		return null;
    	List children;

    	Node parent =getParent(current);
		children = getChildren(parent);
    	if (doChildren) { // if haven't visited children yet
			ListIterator i = children.listIterator(children.size()); // reverse iterator
	        while (i.hasPrevious()) { // get next element after this one.  If it is the last then try its parent
	        	if (i.previous() == current) {
	        		if (i.hasPrevious())
	        			return getPrevious((Node)i.previous(),false);
	        		else
	        			return parent;
	        	}
	        }
    	}

		children = getChildren(current);
   		if (children != null && children.size() > 0) // if parent, previous is last child
			return getPrevious((Node)children.get(children.size()-1),doChildren);
   		
   		return current;
    }

	
    public void dump() {
    	dump(null,"",new Consumer<Object>() { public void accept(Object obj) {
    			logger.log(Level.FINE, "{0}", obj);
    		}
    	});
   }
    public void dump(final StringBuffer buf) {
    	dump(null,"",new Consumer<Object>() { public void accept(Object obj) {
    			buf.append((String)obj).append('\n');
    		}
    	});
   }
    
    private void dump(Node parent, String indent,Consumer<Object> c) {
    	if (parent != null)
    		c.accept(indent + ">"+parent.toString());
    	Collection children = getChildren(parent);
    	if (children != null) {
        	Iterator i = children.iterator();
        	while (i.hasNext()) {
        		dump((Node)i.next(),indent+"--",c);
        	}
    	}
    }
	
    
	public abstract Object clone();
	
	
	
	
	
	protected EventListenerList hierarchyListenerList = new EventListenerList();

	public void addHierarchyListener(HierarchyListener l) {
		hierarchyListenerList.add(HierarchyListener.class, l);
	}
	public void removeHierarchyListener(HierarchyListener l) {
		hierarchyListenerList.remove(HierarchyListener.class, l);
	}
	public HierarchyListener[] getHierarchyListeners() {
		return (HierarchyListener[]) hierarchyListenerList.getListeners(HierarchyListener.class);
	}
    public EventListener[] getHierarchyListeners(Class listenerType) { 
    	return hierarchyListenerList.getListeners(listenerType); 
    }
    
 	protected void fireStructureChanged(Object source) {
		Object[] listeners = hierarchyListenerList.getListenerList();
		HierarchyEvent e = null;
//		for (int i = listeners.length - 2; i >= 0; i -= 2) {
		for (int i = 0; i < listeners.length; i += 2) {
			if (listeners[i] == HierarchyListener.class) {
				if (e == null) {
					e = new HierarchyEvent(source, 
							HierarchyEvent.STRUCTURE_CHANGED, null);
				}
				((HierarchyListener) listeners[i + 1]).structureChanged(e);
		
			}
		}
	}
	protected void fireNodesChanged(Object source, Object[] nodes,Object[] oldNodes,Object flag) {
		Object[] listeners = hierarchyListenerList.getListenerList();
		HierarchyEvent e = null;
		for (int i = 0; i < listeners.length; i += 2) {
			if (listeners[i] == HierarchyListener.class) {
				if (e == null) {
					e = new HierarchyEvent(source, 
							HierarchyEvent.NODES_CHANGED, nodes,oldNodes,flag);
				}
				((HierarchyListener) listeners[i + 1]).nodesChanged(e);
		
			}
		}
	}
	protected void fireNodesInserted(Object source, Object[] nodes,Object[] oldNodes,Object flag) {
		Object[] listeners = hierarchyListenerList.getListenerList();
		HierarchyEvent e = null;
		for (int i = 0; i < listeners.length; i += 2) {
			if (listeners[i] == HierarchyListener.class) {
				if (e == null) {
					e = new HierarchyEvent(source, 
							HierarchyEvent.NODES_INSERTED, nodes,oldNodes,flag);
				}
				((HierarchyListener) listeners[i + 1]).nodesInserted(e);
		
			}
		}
	}
	protected void fireNodesRemoved(Object source, Object[] nodes,Object[] oldNodes,Object flag) {
		Object[] listeners = hierarchyListenerList.getListenerList();
		HierarchyEvent e = null;
		for (int i = 0; i < listeners.length; i += 2) {
			if (listeners[i] == HierarchyListener.class) {
				if (e == null) {
					e = new HierarchyEvent(source, 
							HierarchyEvent.NODES_REMOVED, nodes,oldNodes,flag);
				}
				((HierarchyListener) listeners[i + 1]).nodesRemoved(e);
		
			}
		}
	}
	protected void fireNodesChanged(Object source, Object[] nodes) {
//		System.out.println("Hierarchy="+hashCode()+", fireNodesChanged, nodes="+nodes);
//		dump();
	    fireNodesChanged(source,nodes,null,null);
	}
	protected void fireNodesInserted(Object source, Object[] nodes) {
//		System.out.println("Hierarchy="+hashCode()+", fireNodesInserted, nodes="+nodes);
//		dump();
	    fireNodesInserted(source,nodes,null,null);
	}
	protected void fireNodesRemoved(Object source, Object[] nodes) {
//		System.out.println("Hierarchy="+hashCode()+", fireNodesRemoved, nodes="+nodes);
//		dump();
	    fireNodesRemoved(source,nodes,null,null);
	}
	/**
	 * Convenience method to convert hierarchy to a list of nodes in depth-first order.
	 * @return
	 */
	public List toList(final boolean isNode, final Predicate filter) {
		final ArrayList list = new ArrayList();
    	visitAll(new Consumer<Object>() { public void accept(Object node) {
				if (filter != null  && !filter.evaluate(((Node) node).getImpl()))
					return;
				if (isNode) 
					list.add(node);
				else
					list.add(((Node) node).getImpl());
			}});
    	return list;
    }
	
	
	public void renumber(){
		visitAll(new Consumer<Object>(){
			private int index=0;
			public void accept(Object o) {
				Node node=(Node)o;
				if (node.hasNumber()){
					HasKey impl=(HasKey)node.getImpl();
					if (impl.getId()!=++index){
						impl.setId(index);
					}
				}
			}
		});
	}
	
	
	protected int updateLevel=0;
	protected synchronized void beginUpdate(){
		updateLevel++;
	}
	protected synchronized void endUpdate(){
		updateLevel--;
	}
	protected synchronized int getUpdateLevel(){
		return updateLevel;
	}


}

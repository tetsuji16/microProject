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
package com.microproject.grouping.core;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.iterators.EmptyIterator;
import org.apache.commons.collections.iterators.EmptyListIterator;

import com.microproject.grouping.core.model.NodeModelUtil;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.task.Task;
import com.microproject.server.data.DataObject;

/**
 * Bridge of the bridge pattern
 */
public class NodeBridge extends DefaultMutableTreeNode implements Node{
	//protected Object impl;
	protected boolean virtual = false;
	protected boolean voidNode=false;
	protected boolean root=false;
	protected transient boolean lazyParent = false; // for subprojects
	/**
	 * Use NodeFactory instead
	 */
	NodeBridge(Object impl) {
		//this.impl = impl;
		setImpl(impl);
	}
	/**
	 * Use NodeFactory instead
	 */
	NodeBridge(Object impl, boolean virtual) {
		this(impl);
		this.virtual = virtual;
	}
	/**
	 * @see com.microproject.grouping.core.Node#isVirtual()
	 */
	public boolean isVirtual() {
		return virtual;
	}
	/**
	 * @param virtual
	 *            The virtual to set.
	 */
	public void setVirtual(boolean virtual) {
		this.virtual = virtual;
	}

	public boolean isVoid() {
		return voidNode;
	}
	public void setVoid(boolean voidNode) {
		this.voidNode = voidNode;
	}
	public boolean isRoot() {
		return root;
	}
	public void setRoot(boolean root) {
		this.root = root;
	}
	/**
	 * @see com.microproject.analysis.core.Node#getType()
	 */
	public Class getType() throws NodeException {
		Object impl=getUserObject();
		if (impl == null)
			throw new NodeException("No Implementation");
		return impl.getClass();
	}
	/**
	 * @see com.microproject.analysis.core.Node#accept(com.microproject.analysis.core.NodeVisitor)
	 */
	public void accept(NodeVisitor visitor) {
		visitor.accept(this);
	}

	public String toString() {
		Object impl=getUserObject();
		if (impl == null)
			return "null";
		return impl.toString();
	}
	/**
	 * @return Returns the impl.
	 */
	public Object getImpl() {
		return getUserObject();
	}
	public void setImpl(Object impl) {
		virtual=(impl instanceof GroupNodeImpl);
		voidNode=(impl instanceof VoidNodeImpl);
		setUserObject(impl);
	}

	static ListIterator emptyListIterator(){
		return new ListIterator(){
			public boolean hasNext() {
				return false;
			}
			public Object next() {
				return null;
			}
			public void remove() {
				throw new UnsupportedOperationException();
			}
			public void add(Object o) {
			}
			public boolean hasPrevious() {
				return false;
			}
			public int nextIndex() {
				return 0;
			}
			public Object previous() {
				return null;
			}
			public int previousIndex() {
				return -1;
			}
			public void set(Object o) {
			}
		};
	}

    public ListIterator childrenIterator(){
    	return (children==null)?emptyListIterator():children.listIterator();
    }
    public ListIterator childrenIterator(int i){
    	return (children==null)?emptyListIterator():children.listIterator(i);
    }
    public List getChildren(){
    	return children;
    }


    public boolean isIndentable(int value){
    	if (!(value==1||value==-1)) return false;
    	return !root&&!voidNode&&!virtual&&!(getImpl() instanceof Assignment);
    }


    public boolean canBeChildOf(Node p){
    	if (p.isVoid())
    		return false;
    	return NodeModelUtil.canBeChildOf(p,this);
    }

	public boolean isDirty() {
		Object impl=getImpl();
		if (impl instanceof DataObject) return ((DataObject)impl).isDirty();
		else return false;
	}
	public void setDirty(boolean dirty) {
		//System.out.println("NodeBridge _setDirty("+dirty+")");
		Object impl=getImpl();
		if (impl instanceof DataObject) ((DataObject)impl).setDirty(dirty);
	}
	public final boolean isLazyParent() {
		return getImpl() instanceof LazyParent;
	}

	public final boolean isValidLazyParent() {
		return getImpl() instanceof LazyParent
		&& ((LazyParent)getImpl()).isValid();
	}

	public boolean hasNumber(){
		Object impl=getImpl();
		return  impl instanceof Task || impl instanceof Resource;
	}

	protected int subprojectLevel;
	public int getSubprojectLevel() {
		return subprojectLevel;
	}
	public void setSubprojectLevel(int subprojectLevel) {
		this.subprojectLevel = subprojectLevel;
	}
	public boolean isInSubproject(){
		return getSubprojectLevel()>0;
	}
//	public void setInSubproject(boolean inSubproject){
//		setSubprojectLevel(inSubproject?1:0);
//	}


}

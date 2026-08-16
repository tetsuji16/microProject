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

import java.util.List;
import java.util.ListIterator;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;



/**
 * This is the bridge interface of the bridge pattern
 * Don't implement this interface, implement NodeImpl instead
 * It represents project management objects like Task, Resource, Assignment 
 * but doesn't implements them directly
 */
public interface Node extends MutableTreeNode{
	
	/**
	 * Calculation visitor.
	 * @param visitor
	 */
	public void accept(NodeVisitor visitor);
	
	
    /**
     * Equivalent to getImpl().getClass(). This is used by NodeFactory
     * @throws NodeException if implementation is not set
     */
    public Class getType() throws NodeException;
    
    /**
     * Consolidation node
     */
    public boolean isVirtual();
    public void setVirtual(boolean virtual);
	public boolean isVoid();
	public void setVoid(boolean voidNode);
	public boolean isRoot() ;
	public void setRoot(boolean root);
	public boolean hasNumber() ;

    public Object getImpl();
    public void setImpl(Object imp);
    
    public ListIterator childrenIterator();
    public ListIterator childrenIterator(int i);
    public List getChildren();
    
    public void add(MutableTreeNode node);
    
    public DefaultMutableTreeNode getPreviousSibling();
    public DefaultMutableTreeNode getNextSibling();
    
    public boolean isIndentable(int value);
    public boolean isLazyParent();
    public boolean canBeChildOf(Node parent);
    
	public boolean isDirty();
	public void setDirty(boolean dirty);
	
	public int getSubprojectLevel();
	public void setSubprojectLevel(int subprojectLevel);
	public boolean isInSubproject();
	//public void setInSubproject(boolean inSubproject);
	


}

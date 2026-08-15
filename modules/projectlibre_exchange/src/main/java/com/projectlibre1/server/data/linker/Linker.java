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

package com.projectlibre1.server.data.linker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.Closure;

import com.projectlibre1.grouping.core.Node;
import com.projectlibre1.grouping.core.hierarchy.NodeHierarchy;
import com.projectlibre1.pm.assignment.Assignment;

/**
 *
 */
public abstract class Linker {
	//protected boolean globalIdsOnly=true;
	protected Map transformationMap=new HashMap();
	protected Collection transformed=new ArrayList();
	protected Iterator iterator=null;
	protected Object parent;
	protected Object transformedParent;
	protected Object[] args=null;
	protected boolean incremental;

	public Map getTransformationMap() {
		return transformationMap;
	}
	public Collection getTransformed() {
		return transformed;
	}
//	public boolean isGlobalIdsOnly() {
//		return globalIdsOnly;
//	}
//	public void setGlobalIdsOnly(boolean globalIdsOnly) {
//		this.globalIdsOnly = globalIdsOnly;
//	}

	public boolean isIncremental() {
		return incremental;
	}
	public void setIncremental(boolean incremental) {
		this.incremental = incremental;
	}

	public Object getParent() {
		return parent;
	}
	public void setParent(Object parent) {
		this.parent = parent;
	}
	public Object getTransformedParent() {
		return transformedParent;
	}
	public void setTransformedParent(Object transformedParent) {
		this.transformedParent = transformedParent;
	}

	public Object[] getArgs() {
		return args;
	}
	public void setArgs(Object[] args) {
		this.args = args;
	}
	public boolean hasNext(){return (iterator==null)?false:iterator.hasNext();}
	public void addTransformedObjects() throws Exception{
		while(hasNext()){
			Object obj=executeNext();
			if (obj!=null){
				Object trans=addTransformedObjects(obj);
				if (trans!=null) transformed.add(trans);
			}
		}
		executeFinally();
	}
	public void init(){
		transformationMap.clear();
		transformed.clear();
		initIterator();
	}

	//private int lastIndex;
    public void addOutline(Node root){
    	final Set endVoids=new HashSet();
    	//lastIndex=0;
        getHierarchy().visitAll(root, true,new Closure(){
        	//int tmpIndex=0;
        	public void execute(Object arg){
        		Node node=(Node)arg;
        		Object nodeImpl=node.getImpl();
        		if (!(nodeImpl instanceof Assignment)){
//        			if (!node.isVoid()) lastIndex=tmpIndex;
//        			tmpIndex++;
        			if (node.isVoid()) endVoids.add(node);
        			else endVoids.clear();
        		}
        	}
        });

        getHierarchy().visitAllLevelOrder(root, true,new Closure(){
        	Node thisParent=null;
        	long position=0;
        	//int index=0;
        	public void execute(Object arg){
        		//if (index++>lastIndex) return;
        		Node node=(Node)arg;
        		if (endVoids.contains(node)) return;
        		Object nodeImpl=node.getImpl();
        		if (!(nodeImpl instanceof Assignment)){
        			Node currentParent=getHierarchy().getParent(node);
        			if (currentParent!=null&&currentParent.isRoot()) currentParent=null; //for compatibility
        			if (thisParent!=currentParent){
        				thisParent=currentParent;
        				position=0;
        			}
        			if (node.isVoid()||addOutlineElement(nodeImpl,(thisParent==null)?null:thisParent.getImpl(),position))
        				position++; //skip voids but increment position
        		}
        	}
        });
    }
//    public void addOutline(Node root){
//        getHierarchy().visitAllLevelOrder(root, true,new Closure(){
//        	Node thisParent=null;
//        	long position=0;
//        	public void execute(Object arg){
//        		Node node=(Node)arg;
//        		Object nodeImpl=node.getImpl();
//        		if (!(nodeImpl instanceof Assignment)&&!node.isVoid()){
//        			Node currentParent=getHierarchy().getParent(node);
//        			if (currentParent!=null&&currentParent.isRoot()) currentParent=null; //for compatibility
//        			if (thisParent!=currentParent){
//        				thisParent=currentParent;
//        				position=0;
//        			}
//        			if (addOutlineElement(nodeImpl,(thisParent==null)?null:thisParent.getImpl(),position))
//        				position++;
//        		}
//        	}
//        });
//    }






	protected void initIterator(){}
	public Object executeNext(){
		throw new UnsupportedOperationException(getClass().getSimpleName() + " must implement executeNext()");
	}
	public void executeFinally(){}
    public abstract Object addTransformedObjects(Object child) throws Exception;
    public NodeHierarchy getHierarchy(){
		throw new UnsupportedOperationException(getClass().getSimpleName() + " must implement getHierarchy()");
	}
    public boolean addOutlineElement(Object outlineChild,Object outlineParent,long position){
		throw new UnsupportedOperationException(getClass().getSimpleName() + " must implement addOutlineElement()");
	}
}

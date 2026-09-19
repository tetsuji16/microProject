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
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;


/**
 *
 */
public abstract class CellCache{
	protected ArrayList cache;
	protected ArrayList visibleElements;
	protected Map baseIndex;
	
	/**
	 * 
	 */
	public CellCache() {
		cache=new ArrayList();
		visibleElements=new ArrayList();
		baseIndex=new HashMap();
	}
		
	
	public Object getElement(Object base){
		if (base==null) return null;
		return baseIndex.get(base);
	}
	public abstract Object getBase(Object base);
	
	public Object getCacheElementAt(int row) {
		return cache.get(row);
	}
		
	public int getCacheSize() {
		return cache.size();
	}
	
	public ListIterator getCacheIterator(){
		return cache.listIterator();
	}
	public ListIterator getCacheIterator(int i){
		return cache.listIterator(i);
	}
	
	
    public ArrayList getVisibleElements() {
        return visibleElements;
    }
    public void addVisibleElements(VisibleElements elements){
        visibleElements.add(elements);
    }
    public void removeVisibleElements(VisibleElements elements){
        visibleElements.remove(elements);
    }
	public void removeAllVisibleElements(){
	    visibleElements.clear();
	}
    
    
//insert, delete
	public void insertElement(Object element,Object base){
	    cache.add(element);
		baseIndex.put(base,element);
	}
	public void registerElement(Object element,Object base){
		baseIndex.put(base,element);
	}
	public void deleteElement(Object element){
		baseIndex.remove(getBase(element));
		cache.remove(element);
	}
	
	public void modifyBase(Object oldBase,Object newBase){
	    Object element=baseIndex.remove(oldBase);
	    baseIndex.put(newBase,element);
	}
	
	
	public void clear(){
		cache.clear();
		for (Iterator i=visibleElements.iterator();i.hasNext();){
		    ((VisibleElements)i.next()).clear();
		}
		baseIndex.clear();
	}
	
	
	
	
	
	/**
	 * @return Returns the cache.
	 */
	public ArrayList getCache() {
		return cache;
	}
	
	
	
	
	
	
	
	Map getBaseIndex() {
		return baseIndex;
	}
	void setBaseIndex(Map baseIndex) {
		this.baseIndex = baseIndex;
	}
	void setCache(ArrayList cache) {
		this.cache = cache;
	}
	
	void copyContent(CellCache c){
	    setCache(c.getCache());
	    setBaseIndex(c.getBaseIndex());
	}
	
	public static Collection getContainsCollection(Collection c){
		if (c==null||c.size()<10) return c;
		HashSet set=new HashSet();
		set.addAll(c);
		return set;
	}
	
	
	
}


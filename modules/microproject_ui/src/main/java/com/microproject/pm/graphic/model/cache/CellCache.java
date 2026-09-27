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
public abstract class CellCache<E, V extends VisibleElements<?>>{
	protected ArrayList<E> cache;
	protected ArrayList<V> visibleElements;
	protected Map<Object, E> baseIndex;
	
	/**
	 * 
	 */
	public CellCache() {
		cache=new ArrayList<>();
		visibleElements=new ArrayList<>();
		baseIndex = new HashMap<>();
	}
		
	
	public E getElement(Object base){
		if (base==null) return null;
		return baseIndex.get(base);
	}
	public abstract Object getBase(Object base);
	
	public E getCacheElementAt(int row) {
		return cache.get(row);
	}
		
	public int getCacheSize() {
		return cache.size();
	}
	
	public ListIterator<E> getCacheIterator(){
		return cache.listIterator();
	}
	public ListIterator<E> getCacheIterator(int i){
		return cache.listIterator(i);
	}
	
	
	public ArrayList<V> getVisibleElements() {
        return visibleElements;
    }
	public void addVisibleElements(V elements){
        visibleElements.add(elements);
    }
	public void removeVisibleElements(V elements){
        visibleElements.remove(elements);
    }
	public void removeAllVisibleElements(){
	    visibleElements.clear();
	}
    
    
//insert, delete
	public void insertElement(E element,Object base){
	    cache.add(element);
		baseIndex.put(base,element);
	}
	public void registerElement(E element,Object base){
		baseIndex.put(base,element);
	}
	public void deleteElement(E element){
		baseIndex.remove(getBase(element));
		cache.remove(element);
	}
	
	public void modifyBase(Object oldBase,Object newBase){
	    E element=baseIndex.remove(oldBase);
	    baseIndex.put(newBase,element);
	}
	
	
	public void clear(){
		cache.clear();
		for (V element : visibleElements) {
		    element.clear();
		}
		baseIndex.clear();
	}
	
	
	
	
	
	/**
	 * @return Returns the cache.
	 */
	public ArrayList<E> getCache() {
		return cache;
	}
	
	
	
	
	
	
	
	Map<Object, E> getBaseIndex() {
		return baseIndex;
	}
	void setBaseIndex(Map<Object, E> baseIndex) {
		this.baseIndex = baseIndex;
	}
	void setCache(ArrayList<E> cache) {
		this.cache = cache;
	}
	
	void copyContent(CellCache<E, ?> c){
	    setCache(c.getCache());
	    setBaseIndex(c.getBaseIndex());
	}
	
	public static <T> Collection<T> getContainsCollection(Collection<T> c){
		if (c==null||c.size()<10) return c;
		HashSet<T> set=new HashSet<>();
		set.addAll(c);
		return set;
	}
	
	
	
}

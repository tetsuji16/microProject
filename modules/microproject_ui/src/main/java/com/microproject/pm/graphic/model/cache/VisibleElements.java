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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.microproject.pm.graphic.model.transform.CacheTransformer;

/**
 *
 */
public abstract class VisibleElements{
	protected ArrayList elements;
    protected CacheTransformer transformer;
    protected List<Object> events;
    protected String viewName;

    public VisibleElements(String viewName,CacheTransformer transformer) {
        this.transformer=transformer;
        this.viewName=viewName;
        elements=new ArrayList();
        events=new ArrayList();
    }
    
    
    public ArrayList getElements() {
        return elements;
    }
	void setElements(ArrayList elements) {
		this.elements = elements;
	}
    
    
    public CacheTransformer getTransformer() {
        return transformer;
    }
    public void setTransformer(CacheTransformer transformer) {
        this.transformer = transformer;
    }
    public void applyTransformer(){
        transformer.transfrom(elements);
    }
    
	public int getRow(Object element){
	    int pos=0;
	    for(Iterator i=elements.iterator();i.hasNext();pos++){
	        if (i.next().equals(element)) return pos;
	    }
	    return -1;
	}
	public Object getElementAt(int row) {
		return elements.get(row);
	}
	public int getSize() {
		return elements.size();
	}
	public ListIterator getIterator(){
		return elements.listIterator();
	}
	public ListIterator getIterator(int i){
		return elements.listIterator(i);
	}
	
	public void clear(){
	    elements.clear();
	}
	public boolean isVisible(Object element){
		return elements.contains(element);
	}
	public ArrayList getVisibleElements() {
		return elements;
	}
    
    
	//public abstract void sendEvents();
	
	
    public List<Object> getEvents() {
        return events;
    }
    public void addEvent(Object event) {
        events.add(event);
    }
    public void clearEvents() {
        events.clear();
    }


	public String getViewName() {
		return viewName;
	}

	public void setViewName(String viewName) {
		this.viewName = viewName;
	}
    
	public String toString(){
		return viewName;
	}
    
}


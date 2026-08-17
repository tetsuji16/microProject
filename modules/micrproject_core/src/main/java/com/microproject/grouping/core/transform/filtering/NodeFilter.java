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
package com.microproject.grouping.core.transform.filtering;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Predicate;

import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.transform.CommonTransform;

/**
 *
 */
public abstract class NodeFilter extends CommonTransform implements Predicate{
	protected boolean showSummary = true;
	protected boolean showEmptyLines = true;
	protected boolean showEndEmptyLines = true;
	protected NodeModel model;
	public NodeModel getModel() {
		return model;
	}
	public void setModel(NodeModel model) {
		this.model = model;
	}
	
	
	
    public boolean isShowEmptyLines() {
        return showEmptyLines;
    }
    public void setShowEmptyLines(boolean showEmptyLines) {
        this.showEmptyLines = showEmptyLines;
    }
    
    public boolean isShowEndEmptyLines() {
		return showEndEmptyLines;
	}
	public void setShowEndEmptyLines(boolean showEndEmptyLines) {
		this.showEndEmptyLines = showEndEmptyLines;
	}
	
	public boolean isShowSummary() {
        return showSummary;
    }
    public void setShowSummary(boolean showSummary) {
        this.showSummary = showSummary;
    }
    
	protected boolean showEmptySummaries = true;
	public boolean isShowEmptySummaries() {
		return showEmptySummaries;
	}
	public void setShowEmptySummaries(boolean showEmptySummaries) {
		this.showEmptySummaries = showEmptySummaries;
	}
	protected boolean showAssignments = true;
    public boolean isShowAssignments() {
		return showAssignments;
	}
	public void setShowAssignments(boolean showAssignments) {
		this.showAssignments = showAssignments;
	}
    
	protected boolean preserveHierarchy = true;
	public boolean isPreserveHierarchy() {
		return preserveHierarchy;
	}
	public void setPreserveHierarchy(boolean preserveHierarchy) {
		this.preserveHierarchy = preserveHierarchy;
	}
    
    
	//util
	public ListIterator filteredListIterator(ListIterator i){
		return IteratorUtils.filteredListIterator(i,this);
	}
	public Iterator filteredIterator(Iterator i){
		return IteratorUtils.filteredIterator(i,this);
	}
	
	public List filterList(List list){
		if (list==null) return null;
		for (Iterator i=list.iterator();i.hasNext();){
			if (!evaluate(i.next())) i.remove();
		}
		return list;
		
	}
	public Object[] filterArray(Object[] list){
		if (list==null) return null;
		ArrayList filtered = new ArrayList(list.length);
		for (int i=0;i<list.length;i++){
			Object obj=list[i];
			if (evaluate(obj)) filtered.add(obj);
		}
		return filtered.toArray();
	}
    public void setRedefinitionCallBack(Consumer<Object> callback){}
    
    /**
     * Whether this filter currently restricts the visible set. Subclasses
     * with an "empty criteria means no filtering" semantics override it.
     */
    public boolean isActive() {
        return true;
    }
    
    public void reset(){} //for state filters
	
}

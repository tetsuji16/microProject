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
package com.microproject.grouping.core.transform.sorting;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import com.microproject.configuration.FieldDictionary;
import com.microproject.field.Field;
import com.microproject.field.FieldConverter;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.transform.CommonTransform;
import com.microproject.grouping.core.transform.HierarchicObject;

/**
 *
 */
public class NodeSorter extends CommonTransform implements Comparator{
	private static final Logger logger = Logger.getLogger(NodeSorter.class.getName());
	protected boolean showSummary = true;
	protected boolean showEmptyLines = true;
	protected boolean showEndEmptyLines = true;
	protected List fields = null;

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
	
	public List sortList(List list,boolean preserverHierarchy){
	    return sortList(list,this,preserverHierarchy);
	}
	public List sortList(List list,Comparator comparator,boolean preserveHierarchy){
		Collections.sort(list,comparator);
		if (preserveHierarchy)
		for (Iterator i=list.iterator();i.hasNext();){
			HierarchicObject child=(HierarchicObject)i.next();
			if (child.getChildren().size()>0) sortList(child.getChildren(), comparator,true);
		}
		return list;
	}

public List getList() {
	try {
		return (List) pool.borrowObject();
	} catch (Exception e) {
		return null;
	}
}

	public void recycleList(List list) {
		try {
			list.clear();
			pool.returnObject(list);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Sort error", e);
		}
	}

private GenericObjectPool pool = new GenericObjectPool(new ListFactory());

private class ListFactory extends BasePoolableObjectFactory {
	public Object makeObject() { //claur
		return new ArrayList();
	}

	public void activateObject(Object arg0){ //claur{
//		Stack stack = (Stack)arg0;
//		stack.clear();
	}
}



















    public void setRedefinitionCallBack(Consumer<Object> callback){}
	
    
    //used by label formula
    protected String getFieldName(String fieldId){
		Field field=FieldDictionary.getInstance().getFieldFromId(fieldId);
		return field.getName();
    }
   /* protected Object getFieldValue(String fieldId,Node node){
		Field field=FieldDictionary.getInstance().getFieldFromId(fieldId);
		return field.getValue(node,model,null);
    }*/
    
    protected String toString(Object value){
        return FieldConverter.toString(value,value.getClass(),null);
    }
    protected String toString(String fieldId,Object value){
		Field field=FieldDictionary.getInstance().getFieldFromId(fieldId);
		if (field.hasOptions()) {
			return field.convertValueToStringUsingOptions(value);
		} //test, use getValue instead
		else return toString(value);
    } 
    
    public String getGroupName(Object impl){
        return "";
    }
    public String getGroupName(String fieldId,Object object){
        return getFieldName(fieldId)+": "+object;
    }
    public String getStringGroupName(String label,Object object){
        return label+": "+object;
    }

    protected ListIterator currentSorter=null;
    public int compare(Object o1, Object o2) {
        NodeSorter sorter;
        List<Object> sorters=getSubTransforms();
        if (sorters!=null){
            currentSorter=sorters.listIterator();
	        while (currentSorter.hasNext()){
	            sorter=(NodeSorter)currentSorter.next();
	            int r=sorter.compare(o1,o2);
	            if (r!=0){
	                currentSorter.previous();
	                return r;
	            }
	        }
        }
        return 0;
    }
    public ListIterator getCurrentSorter(){
        if (currentSorter!=null) return currentSorter;
        if (getSubTransforms()==null) return null;
        else return getSubTransforms().listIterator();
    }

    
}

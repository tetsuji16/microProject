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
package com.microproject.grouping.core.transform;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.EventListener;
import java.util.List;
import java.util.StringTokenizer;

import com.microproject.grouping.core.transform.filtering.NodeFilter;
import com.microproject.grouping.core.transform.grouping.NodeGrouper;
import com.microproject.grouping.core.transform.sorting.NodeSorter;
import com.microproject.grouping.core.transform.transformer.NodeTransformer;
import com.microproject.util.ListenerRegistry;

/**
 *
 */
public class ViewTransformer{
	public static final String FILTER_NONE_ID="Filter.None";
	public static final String SORTER_NONE_ID="Sorter.None";
	public static final String GROUPER_NONE_ID="Grouper.None";


    protected List<String> filters=null;
    protected List<String> sorters=null;
    protected List<String> groupers=null;

    protected NodeFilter hiddenFilter;
    protected NodeFilter userFilter;
    protected NodeSorter hiddenSorter;
    protected NodeTransformer transformer;
    protected NodeSorter userSorter;
    protected NodeGrouper hiddenGrouper;
    protected NodeGrouper userGrouper;

    protected String hiddenFilterId;
    protected String userFilterId=FILTER_NONE_ID;
    protected String hiddenSorterId;
    protected String userSorterId=SORTER_NONE_ID;
    protected String hiddenGrouperId;
    protected String userGrouperId=GROUPER_NONE_ID;
    protected String transformerId;

    protected boolean hiddenFilterIdDirty=false;
    protected boolean userFilterIdDirty=false;
    protected boolean hiddenSorterIdDirty=false;
    protected boolean userSorterIdDirty=false;
    protected boolean hiddenGrouperIdDirty=false;
    protected boolean userGrouperIdDirty=false;
    protected boolean transformerIdDirty=false;




    public List<String> getFilterList() {
        return filters;
    }
    public void setFilters(String slist) {
        filters=tokenize(slist);
    }
    public List<String> getSorterList() {
        return sorters;
    }
    public void setSorters(String slist) {
        sorters=tokenize(slist);
    }
    public List<String> getGrouperList() {
        return groupers;
    }
    public void setGroupers(String slist) {
        groupers=tokenize(slist);
    }

    private static List<String> tokenize(String value) {
        StringTokenizer tokenizer = new StringTokenizer(value, ";, \t");
        List<String> tokens = new ArrayList<>(tokenizer.countTokens());
        while (tokenizer.hasMoreTokens()) tokens.add(tokenizer.nextToken());
        return tokens;
    }


    private Consumer<Object> redefinition = this::fireTransformerChanged;

    public void setFilterId(TransformId id) {
        if (id.isHidden()){
        	hiddenFilterId=id.getId();
        	hiddenFilterIdDirty=true;
        }
        else{
        	userFilterId=id.getId();
        	userFilterIdDirty=true;
        }
    }
    public void setSorterId(TransformId id) {
        if (id.isHidden()){
        	hiddenSorterId=id.getId();
        	hiddenSorterIdDirty=true;
        }
        else{
        	userSorterId=id.getId();
        	userSorterIdDirty=true;
        }
    }
    public void setGrouperId(TransformId id) {
        if (id.isHidden()){
        	hiddenGrouperId=id.getId();
        	hiddenGrouperIdDirty=true;
        }
        else{
        	userGrouperId=id.getId();
        	userGrouperIdDirty=true;
        }
    }
    public void setTransformerId(TransformId id) {
        transformerId=id.getId();
        transformerIdDirty=true;
    }

	public String getHiddenFilterId() {
		return hiddenFilterId;
	}
	public void setHiddenFilterId(String hiddenFilterId) {
		this.hiddenFilterId = hiddenFilterId;
		hiddenFilterIdDirty=true;
		fireTransformerChanged(this);
	}
	public String getHiddenGrouperId() {
		return hiddenGrouperId;
	}
	public void setHiddenGrouperId(String hiddenGrouperId) {
		this.hiddenGrouperId = hiddenGrouperId;
		hiddenGrouperIdDirty=true;
		fireTransformerChanged(this);
	}
	public String getHiddenSorterId() {
		return hiddenSorterId;
	}
	public void setHiddenSorterId(String hiddenSorterId) {
		this.hiddenSorterId = hiddenSorterId;
		hiddenSorterIdDirty=true;
		fireTransformerChanged(this);
	}
	public String getUserFilterId() {
		return userFilterId;
	}
	public void setUserFilterId(String userFilterId) {
		this.userFilterId = userFilterId;
		userFilterIdDirty=true;
		fireTransformerChanged(this);
	}
	public String getUserGrouperId() {
		return userGrouperId;
	}
	public void setUserGrouperId(String userGrouperId) {
		this.userGrouperId = userGrouperId;
		userGrouperIdDirty=true;
		fireTransformerChanged(this);
	}
	public String getUserSorterId() {
		return userSorterId;
	}
	public void setUserSorterId(String userSorterId) {
		this.userSorterId = userSorterId;
		userSorterIdDirty=true;
		fireTransformerChanged(this);
	}
	public String getTransformerId() {
		return transformerId;
	}
	public void setTransformerId(String transformerId) {
		this.transformerId = transformerId;
		transformerIdDirty=true;
		fireTransformerChanged(this);
	}

	public void update(){
		fireTransformerChanged(this);
	}



    private CommonTransform getTransform(String listName,String id){
    	TransformList list=TransformList.getInstance(listName);
    	if (list==null) return null;
    	CommonTransform transform=(CommonTransform)list.getTransform(id);
    	if (transform!=null) transform.askForParameters();
    	return transform;
    }

    public NodeFilter getHiddenFilter() {
        if (hiddenFilterIdDirty){
        	hiddenFilter=(NodeFilter)getTransform("hidden_filters",hiddenFilterId);
        	hiddenFilter.setRedefinitionCallBack(redefinition);
        	hiddenFilterIdDirty=false;
        }
        return hiddenFilter;
    }
    public void setHiddenFilter(NodeFilter hiddenFilter) {
        this.hiddenFilter = hiddenFilter;
        if (hiddenFilter != null) {
            hiddenFilter.setRedefinitionCallBack(redefinition);
        }
        fireTransformerChanged(this);
    }
    public NodeGrouper getHiddenGrouper() {
        if (hiddenGrouperIdDirty){
        	hiddenGrouper=(NodeGrouper)getTransform("hidden_groupers",hiddenGrouperId);
        	hiddenGrouper.setRedefinitionCallBack(redefinition);
        	hiddenGrouperIdDirty=false;
        }
       return hiddenGrouper;
    }
    public void setHiddenGrouper(NodeGrouper hiddenGrouper) {
        this.hiddenGrouper = hiddenGrouper;
    }
    public NodeSorter getHiddenSorter() {
        if (hiddenSorterIdDirty){
        	hiddenSorter=(NodeSorter)getTransform("hidden_sorters",hiddenSorterId);
        	hiddenSorter.setRedefinitionCallBack(redefinition);
        	hiddenSorterIdDirty=false;
        }
       return hiddenSorter;
    }
    public void setHiddenSorter(NodeSorter hiddenSorter) {
        this.hiddenSorter = hiddenSorter;
    }
    public NodeFilter getUserFilter() {
        if (userFilterIdDirty){
        	userFilter=(NodeFilter)getTransform("user_filters",userFilterId);
        	userFilterIdDirty=false;
        }
        return userFilter;
    }
    public void setUserFilter(NodeFilter userFilter) {
        this.userFilter = userFilter;
    }
    public NodeGrouper getUserGrouper() {
       if (userGrouperIdDirty){
       		userGrouper=(NodeGrouper)getTransform("user_groupers",userGrouperId);
       		userGrouperIdDirty=false;
       }
       return userGrouper;
    }
    public void setUserGrouper(NodeGrouper userGrouper) {
        this.userGrouper = userGrouper;
    }
    public NodeSorter getUserSorter() {
        if (userSorterIdDirty){
        	userSorter=(NodeSorter)getTransform("user_sorters",userSorterId);
        	userSorterIdDirty=false;
        }
        return userSorter;
    }
    public void setUserSorter(NodeSorter userSorter) {
        this.userSorter = userSorter;
    }
    public NodeTransformer getTransformer() {
        if (transformerIdDirty){
            transformer=(NodeTransformer)getTransform("transformers",transformerId);
        	//hiddenFilter.setRedefinitionCallBack(redefinition);
           transformerIdDirty=false;
        }
        return transformer;
    }
    public void settransformer(NodeTransformer transformer) {
        this.transformer = transformer;
    }

    public boolean isShowSummary(){
    	if (!isShowSummary(getHiddenFilter())) return false;
       	if (!isShowSummary(getUserFilter())) return false;
       	if (!isShowSummary(getHiddenSorter())) return false;
       	if (!isShowSummary(getUserSorter())) return false;
       	if (!isShowSummary(getHiddenGrouper())) return false;
       	if (!isShowSummary(getUserGrouper())) return false;
    	return true;
    }
    private boolean isShowSummary(CommonTransform t){return (t==null)?true:t.isShowSummary();}

    public boolean isPreserveHierarchy(){
    	if (!isPreserveHierarchy(getHiddenFilter())) return false;
       	if (!isPreserveHierarchy(getUserFilter())) return false;
       	if (!isPreserveHierarchy(getHiddenSorter())) return false;
       	if (!isPreserveHierarchy(getUserSorter())) return false;
       	if (!isPreserveHierarchy(getHiddenGrouper())) return false;
       	if (!isPreserveHierarchy(getUserGrouper())) return false;
    	return true;
    }
    private boolean isPreserveHierarchy(CommonTransform t){return (t==null)?true:t.isPreserveHierarchy();}

    public boolean isShowAssignments(){
    	if (!isShowAssignments(getHiddenFilter())) return false;
       	if (!isShowAssignments(getUserFilter())) return false;
       	if (!isShowAssignments(getHiddenSorter())) return false;
       	if (!isShowAssignments(getUserSorter())) return false;
       	if (!isShowAssignments(getHiddenGrouper())) return false;
       	if (!isShowAssignments(getUserGrouper())) return false;
    	return true;
    }
    private boolean isShowAssignments(CommonTransform t){return (t==null)?true:t.isShowAssignments();}

    public boolean isShowEmptyLines(){
    	if (!isNoneSorter()) return false;
    	if (!isNoneGrouper()) return false;
    	if (!isShowEmptyLines(getHiddenFilter())) return false;
       	if (!isShowEmptyLines(getUserFilter())) return false;
    	return true;
    }
    private boolean isShowEmptyLines(CommonTransform t){return (t==null)?true:t.isShowEmptyLines();}

    public boolean isShowEndEmptyLines(){
    	if (!isNoneSorter()) return false;
    	if (!isNoneGrouper()) return false;
    	if (!isShowEndEmptyLines(getHiddenFilter())) return false;
       	if (!isShowEndEmptyLines(getUserFilter())) return false;
    	return true;
    }
    private boolean isShowEndEmptyLines(CommonTransform t){return (t==null)?true:t.isShowEndEmptyLines();}

    public boolean isShowEmptySummaries(){
    	if (!isShowEmptySummaries(getHiddenFilter())) return false;
       	if (!isShowEmptySummaries(getUserFilter())) return false;
    	return true;
    }
    private boolean isShowEmptySummaries(CommonTransform t){return (t==null)?true:t.isShowEmptySummaries();}


    public boolean isTreatAssignmentsAsTasks(){
    	return false;
    }




    public boolean isNoneFilter(){
    	return userFilterId==null||FILTER_NONE_ID.equals(userFilterId);
    }
    public boolean isNoneSorter(){
    	return userSorterId==null||SORTER_NONE_ID.equals(userSorterId);
    }
    public boolean isNoneGrouper(){
    	return userGrouperId==null||GROUPER_NONE_ID.equals(userGrouperId);
    }






	private final ListenerRegistry<ViewTransformerListener> listeners = new ListenerRegistry<>();

	public void addViewTransformerListener(ViewTransformerListener l) {
		listeners.add(l);
	}
	public void removeViewTransformerListener(ViewTransformerListener l) {
		listeners.remove(l);
	}
	public ViewTransformerListener[] getTimeScaleListeners() {
		return listeners.snapshotReverse().toArray(ViewTransformerListener[]::new);
	}
	protected void fireTransformerChanged(Object source) {
		ViewTransformerEvent e = null;
		for (ViewTransformerListener listener : listeners.snapshotReverse()) {
			if (e == null)
				e = new ViewTransformerEvent(source);
			listener.transformerChanged(e);
		}
	}
	public EventListener[] getListeners(Class listenerType) {
		if (listenerType == null)
			throw new NullPointerException("listenerType");
		if (listenerType != ViewTransformerListener.class)
			return (EventListener[]) Array.newInstance(listenerType, 0);
		return listeners.snapshotReverse().toArray(ViewTransformerListener[]::new);
       }














}

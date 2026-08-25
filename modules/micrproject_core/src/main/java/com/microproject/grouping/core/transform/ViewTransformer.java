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

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.EventListener;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.event.EventListenerList;


import com.microproject.grouping.core.transform.filtering.NodeFilter;
import com.microproject.grouping.core.transform.grouping.NodeGrouper;
import com.microproject.grouping.core.transform.sorting.NodeSorter;
import com.microproject.grouping.core.transform.transformer.NodeTransformer;

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
        StringTokenizer st=new StringTokenizer(slist,";, \t");
        filters=new ArrayList<>();
        while (st.hasMoreTokens()) filters.add(st.nextToken());
    }
    public List<String> getSorterList() {
        return sorters;
    }
    public void setSorters(String slist) {
        StringTokenizer st=new StringTokenizer(slist,";, \t");
        sorters=new ArrayList<>();
        while (st.hasMoreTokens()) sorters.add(st.nextToken());
    }
    public List<String> getGrouperList() {
        return groupers;
    }
    public void setGroupers(String slist) {
        StringTokenizer st=new StringTokenizer(slist,";, \t");
        groupers=new ArrayList<>();
        while (st.hasMoreTokens()) groupers.add(st.nextToken());
    }


    private Consumer<Object> redefinition=new Consumer<Object>() { public void accept(Object o) {
            fireTransformerChanged(o);
        }
    };

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
	    CommonTransform transform=null;
	    CommonTransformFactory factory=list.getFactory(id);
	    try {
		transform=factory==null?(CommonTransform)list.getTransform(id):factory.getTransform();
	    } catch (com.microproject.field.InvalidFormulaException exception) {
		throw new IllegalArgumentException("Invalid view transform: "+id,exception);
	    }
    	if (transform!=null) transform.askForParameters();
    	return transform;
    }

	/** Creates a listener-free, mutable transform state for one view session. */
	public ViewTransformer copyForSession() {
		ViewTransformer copy=new ViewTransformer();
		copy.filters=filters==null?null:new ArrayList<>(filters);
		copy.sorters=sorters==null?null:new ArrayList<>(sorters);
		copy.groupers=groupers==null?null:new ArrayList<>(groupers);
		copy.hiddenFilterId=hiddenFilterId;
		copy.userFilterId=userFilterId;
		copy.hiddenSorterId=hiddenSorterId;
		copy.userSorterId=userSorterId;
		copy.hiddenGrouperId=hiddenGrouperId;
		copy.userGrouperId=userGrouperId;
		copy.transformerId=transformerId;
		copy.hiddenFilterIdDirty=hiddenFilterId!=null;
		copy.userFilterIdDirty=userFilterId!=null;
		copy.hiddenSorterIdDirty=hiddenSorterId!=null;
		copy.userSorterIdDirty=userSorterId!=null;
		copy.hiddenGrouperIdDirty=hiddenGrouperId!=null;
		copy.userGrouperIdDirty=userGrouperId!=null;
		copy.transformerIdDirty=transformerId!=null;
		if (hiddenFilterId==null) copy.hiddenFilter=copyDirect(hiddenFilter);
		if (userFilterId==null) copy.userFilter=copyDirect(userFilter);
		if (hiddenSorterId==null) copy.hiddenSorter=copyDirect(hiddenSorter);
		if (userSorterId==null) copy.userSorter=copyDirect(userSorter);
		if (hiddenGrouperId==null) copy.hiddenGrouper=copyDirect(hiddenGrouper);
		if (userGrouperId==null) copy.userGrouper=copyDirect(userGrouper);
		if (transformerId==null) copy.transformer=copyDirect(transformer);
		return copy;
	}

	@SuppressWarnings("unchecked")
	private static <T extends CommonTransform> T copyDirect(T transform) {
		if (transform == null) return null;
		return (T)transform.copyForSession();
	}

    public NodeFilter getHiddenFilter() {
        if (hiddenFilterIdDirty){
        	hiddenFilter=(NodeFilter)getTransform("hidden_filters",hiddenFilterId);
			if (hiddenFilter != null) hiddenFilter.setRedefinitionCallBack(redefinition);
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
			if (hiddenGrouper != null) hiddenGrouper.setRedefinitionCallBack(redefinition);
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
			if (hiddenSorter != null) hiddenSorter.setRedefinitionCallBack(redefinition);
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
//       	if (!isShowEmptyLines(getHiddenSorter())) return false;
//       	if (!isShowEmptyLines(getUserSorter())) return false;
//       	if (!isShowEmptyLines(getHiddenGrouper())) return false;
//       	if (!isShowEmptyLines(getUserGrouper())) return false;
    	return true;
    }
    private boolean isShowEmptyLines(CommonTransform t){return (t==null)?true:t.isShowEmptyLines();}

    public boolean isShowEndEmptyLines(){
    	if (!isNoneSorter()) return false;
    	if (!isNoneGrouper()) return false;
    	if (!isShowEndEmptyLines(getHiddenFilter())) return false;
       	if (!isShowEndEmptyLines(getUserFilter())) return false;
//    	if (!isShowEndEmptyLines(getHiddenSorter())) return false;
//       	if (!isShowEndEmptyLines(getUserSorter())) return false;
    	return true;
    }
    private boolean isShowEndEmptyLines(CommonTransform t){return (t==null)?true:t.isShowEndEmptyLines();}

    public boolean isShowEmptySummaries(){
    	if (!isShowEmptySummaries(getHiddenFilter())) return false;
       	if (!isShowEmptySummaries(getUserFilter())) return false;
//       	if (!isShowEmptyLines(getHiddenSorter())) return false;
//       	if (!isShowEmptyLines(getUserSorter())) return false;
//       	if (!isShowEmptyLines(getHiddenGrouper())) return false;
//       	if (!isShowEmptyLines(getUserGrouper())) return false;
    	return true;
    }
    private boolean isShowEmptySummaries(CommonTransform t){return (t==null)?true:t.isShowEmptySummaries();}

//    public boolean isShowBadBranches(){
//    	if (!isShowBadBranches(getHiddenFilter())) return false;
//       	if (!isShowBadBranches(getUserFilter())) return false;
//    	return true;
//    }
//    private boolean isShowBadBranches(CommonTransform t){return (t==null)?true:t.isShowBadBranches();}


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






	protected EventListenerList listenerList = new EventListenerList();

	public void addViewTransformerListener(ViewTransformerListener l) {
		listenerList.add(ViewTransformerListener.class, l);
	}
	public void removeViewTransformerListener(ViewTransformerListener l) {
		listenerList.remove(ViewTransformerListener.class, l);
	}
	public ViewTransformerListener[] getTimeScaleListeners() {
		return (ViewTransformerListener[]) listenerList.getListeners(ViewTransformerListener.class);
	}
	protected void fireTransformerChanged(Object source) {
		Object[] listeners = listenerList.getListenerList();
		ViewTransformerEvent e = null;
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ViewTransformerListener.class) {
				if (e == null) {
					e = new ViewTransformerEvent(source);
				}
				((ViewTransformerListener) listeners[i + 1]).transformerChanged(e);
			}
		}
	}
    public EventListener[] getListeners(Class listenerType) {
    	return listenerList.getListeners(listenerType);
       }














}

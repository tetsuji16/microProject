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
package com.microproject.script;

import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.grouping.core.transform.TransformList;
import com.microproject.grouping.core.transform.filtering.NodeFilter;
import com.microproject.script.object.TimeIntervals;
import com.microproject.strings.Messages;



public class  ConverterContext implements Cloneable{
	public static final int ALL=0;
	public static final int CHANGE=1;
	public static final int SCALE=2;
	public static final int TRANSLATE=3;

	protected int type;
	protected String id,name;
	protected String fieldArrayId;
	protected String hiddenFieldArrayId;
	protected String filterId;
	protected String groupFieldId,sortFieldId;
	protected String roles;
	protected boolean distribution;
	protected int summaryLevel=-1;
	protected long s=Long.MAX_VALUE,e=0;
	protected int scale=TimeIntervals.WEEK;

	protected int timeType;

	protected int actionType;
	protected long winS=0,winE=Long.MAX_VALUE;



	public int getActionType() {
		return actionType;
	}
	public void setActionType(int actionType) {
		this.actionType = actionType;
	}

	public ConverterContext(){

	}

	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}


	public String getName() {
		if (name == null && id != null)
			name = Messages.getString(id);
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getFieldArrayId() {
		return fieldArrayId;
	}
	public void setFieldArrayId(String fieldArrayId) {
		this.fieldArrayId = fieldArrayId;
	}



	public String getHiddenFieldArrayId() {
		return hiddenFieldArrayId;
	}
	public void setHiddenFieldArrayId(String hiddenFieldArrayId) {
		this.hiddenFieldArrayId = hiddenFieldArrayId;
	}

	public String getFilterId() {
		return filterId;
	}
	public void setFilterId(String filterId) {
		this.filterId = filterId;
	}
	public String getGroupFieldId() {
		return groupFieldId;
	}
	public void setGroupFieldId(String groupFieldId) {
		this.groupFieldId = groupFieldId;
	}
	public String getSortFieldId() {
		return sortFieldId;
	}
	public void setSortFieldId(String sortFieldId) {
		this.sortFieldId = sortFieldId;
	}


	public int getSummaryLevel() {
		return summaryLevel;
	}
	public void setSummaryLevel(int summaryLevel) {
		this.summaryLevel = summaryLevel;
	}




	public String getRoles() {
		return roles;
	}
	public void setRoles(String roles) {
		this.roles = roles;
	}

	public long getE() {
		return e;
	}

	public void setE(long e) {
		this.e = e;
	}

	public long getS() {
		return s;
	}

	public void setS(long s) {
		this.s = s;
	}


	public int getScale() {
		return scale;
	}

	public void setScale(int scale) {
		this.scale = scale;
	}

	public long getWinE() {
		return winE;
	}

	public void setWinE(long winE) {
		this.winE = winE;
	}

	public long getWinS() {
		return winS;
	}

	public void setWinS(long winS) {
		this.winS = winS;
	}



//	public GlobalCache getCache(){
//		return (GlobalCache)confiObjectStore;
//	}

//	public ConfigObjectStore getConfiObjectStore() {
//		return confiObjectStore;
//	}



//	public void setConfiObjectStore(ConfigObjectStore confiObjectStore) {
//		this.confiObjectStore = confiObjectStore;
//	}
//
//	public IdentifiedObject getConfigObject(String category) {
//		return confiObjectStore==null?null:confiObjectStore.getConfigObject(category);
//	}


//	public SpreadSheetFieldArray getFieldArray(String category) {
//		return FieldArrayUtil.createFieldArray(category,fieldArrayId==null?FieldArrayUtil.getDefaultConfigObjectId(category):fieldArrayId);
//	}

	protected transient SpreadSheetFieldArray fieldArray;
	protected transient boolean fieldArrayInitialized;
	public SpreadSheetFieldArray retrieveFieldArray(){ //to "retrieve" avoid "get"
		if (fieldArrayInitialized) return fieldArray;
		fieldArrayInitialized=true;
		if (fieldArrayId==null) return null;
		fieldArray=FieldArrayUtil.getFieldArray(type, fieldArrayId);
		return fieldArray;
	}

	protected transient SpreadSheetFieldArray hiddenFieldArray;
	protected transient boolean hiddenFieldArrayInitialized;
	public SpreadSheetFieldArray retrieveHiddenFieldArray(){ //to "retrieve" avoid "get"
		if (hiddenFieldArrayInitialized) return hiddenFieldArray;
		hiddenFieldArrayInitialized=true;
		if (hiddenFieldArrayId==null) return null;
		hiddenFieldArray=FieldArrayUtil.getHiddenFieldArray(type, hiddenFieldArrayId);
		return hiddenFieldArray;
	}

	protected transient NodeFilter filter;
	protected transient boolean filterInitialized;
	public NodeFilter retrieveFilter(){ //to "retrieve" avoid "get"
		if (filterInitialized) return filter;
		filterInitialized=true;
		if (filterId==null) return null;
		filter=(NodeFilter)TransformList.getInstance("report_filters").getTransform(filterId);
		if (filter==null) filter=(NodeFilter)TransformList.getInstance("user_filters").getTransform(filterId);
		if (filter==null) filter=(NodeFilter)TransformList.getInstance("hidden_filters").getTransform(filterId);
		//check user and hidden filters groups too
//		if (filterId==null)
//			return null;
//		filter=filterFromList("report_filters",filterId);
//		if (filter==null)
//			filter=filterFromList("report_user",filterId);
//		if (filter==null)
//			filter=filterFromList("Filters.user",filterId);
//		if (filter==null)
//			filter=filterFromList("Filters.hidden",filterId);

		return filter;
	}

//	private NodeFilter filterFromList(String filterListId,String filterId) {
//		TransformList list = TransformList.getInstance(filterListId);
//		if (list != null)
//			return (NodeFilter)list.getTransform(filterId);
//		else
//			return null;
//	}

	public String toString(){
		return "{"+
			"id="+id+", "+
			"type="+type+", "+
			"fieldArrayId="+fieldArrayId+", "+
			"hiddenFieldArrayId="+hiddenFieldArrayId+", "+
			"filterId="+filterId+", "+
			"}";
	}


	public Object clone(){
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}

	}
	public boolean isDistribution() {
		return distribution;
	}
	public void setDistribution(boolean distribution) {
		this.distribution = distribution;
	}
	public int getTimeType() {
		return timeType;
	}
	public void setTimeType(int timeType) {
		this.timeType = timeType;
	}



}

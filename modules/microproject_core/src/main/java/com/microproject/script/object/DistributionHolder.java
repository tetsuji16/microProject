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
package com.microproject.script.object;

import java.util.ArrayList;
import java.util.List;

import com.microproject.datatype.Duration;
import com.microproject.datatype.TimeUnit;
import com.microproject.field.FieldContext;
import com.microproject.pm.assignment.TimeDistributedFields;
import com.microproject.pm.key.HasId;
import com.microproject.pm.key.HasName;
import com.microproject.pm.scheduling.TimeSheetSchedule;
import com.microproject.pm.time.MutableHasStartAndEnd;
import com.microproject.server.data.ExtendedDistributionData;

public class DistributionHolder implements HasId,HasName,MutableHasStartAndEnd,TimeDistributedFields,TimeSheetSchedule{
	protected long id,uniqueId;
	protected String name;
	protected long start,end,c;
	protected List<ExtendedDistributionData> dist;
	protected List<DistributionHolder> children;
	protected long parentId;
	protected Object extension;
	public List<ExtendedDistributionData> getDist() {
		return dist;
	}
	public void setDist(List<ExtendedDistributionData> dist) {
		this.dist = dist;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getUniqueId() {
		return uniqueId;
	}
	public void setUniqueId(long uniqueId) {
		this.uniqueId = uniqueId;
	}
	public String getName() {
		return name;
	}
	public String getName(FieldContext context){
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public long getEnd() {
		return end;
	}
	public void setEnd(long end) {
		this.end = end;
	}
	public long getStart() {
		return start;
	}
	public void setStart(long start) {
		this.start = start;
	}
	public List<DistributionHolder> getChildren() {
		return children;
	}
	public void setChildren(List<DistributionHolder> children) {
		this.children = children;
	}
	public void addChild(DistributionHolder child){
		if (children==null) children=new ArrayList<DistributionHolder>();
		children.add(child);
	}
	public Object getExtension() {
		return extension;
	}
	public void setExtension(Object extension) {
		this.extension = extension;
	}
	public long getParentId() {
		return parentId;
	}
	public void setParentId(long parentId) {
		this.parentId = parentId;
	}
	public long getC() {
		return c;
	}
	public void setC(long c) {
		this.c = c;
	}
	
	
	//TimeDistributedFields
	protected double work,actualWork,cost,actualCost;
	public double getActualCost() {
		return actualCost;
	}
	public void setActualCost(double actualCost) {
		this.actualCost = actualCost;
	}
	public double getActualWork() {
		return actualWork;
	}
	public void setActualWork(double actualWork) {
		this.actualWork = actualWork;
	}
	public double getCost() {
		return cost;
	}
	public void setCost(double cost) {
		this.cost = cost;
	}
	public double getWork() {
		return work;
	}
	public void setWork(double work) {
		this.work = work;
	}

	public String dumpTimeDistributedFields(){
		return "work="+work+", actualWork="+actualWork+", cost="+cost+", ="+actualCost+", ="+actualCost;
	}
	
	
	public boolean fieldHideActualCost(FieldContext fieldContext) {
		return false;
	}
	public boolean fieldHideActualFixedCost(FieldContext fieldContext) {
		return false;
	}
	public boolean fieldHideActualWork(FieldContext fieldContext) {
		return false;
	}
	public boolean fieldHideBaselineCost(int numBaseline, FieldContext fieldContext) {
		return false;
	}
	public boolean fieldHideBaselineWork(int numBaseline, FieldContext fieldContext) {
		return false;
	}
	public boolean fieldHideCost(FieldContext fieldContext) {
		return false;
	}
	public boolean fieldHideWork(FieldContext fieldContext) {
		return false;
	}
	public double getActualCost(FieldContext fieldContext) {
		return getActualCost();
	}
	public double getActualFixedCost(FieldContext fieldContext) {
		return 0;
	}
	public long getActualWork(FieldContext fieldContext) {
		return Duration.getInstance(Math.round(getActualWork()),TimeUnit.DAYS);
	}
	public double getBaselineCost(int numBaseline, FieldContext fieldContext) {
		return 0;
	}
	public long getBaselineWork(int numBaseline, FieldContext fieldContext) {
		return 0;
	}
	public double getCost(FieldContext fieldContext) {
		 return getCost();
	}
	public double getFixedCost(FieldContext fieldContext) {
		return 0;
	}
	public double getRemainingCost(FieldContext fieldContext) {
		return 0;
	}
	public long getRemainingWork(FieldContext fieldContext) {
		return 0;
	}
	public long getWork(FieldContext fieldContext) {
		return Duration.getInstance(Math.round(getWork()),TimeUnit.DAYS);
	}
	public boolean isReadOnlyActualWork(FieldContext fieldContext) {
		return false;
	}
	public boolean isReadOnlyFixedCost(FieldContext fieldContext) {
		return false;
	}
	public boolean isReadOnlyRemainingWork(FieldContext fieldContext) {
		return false;
	}
	public boolean isReadOnlyWork(FieldContext fieldContext) {
		return false;
	}
	public void setActualWork(long actualWork, FieldContext fieldContext) {
		setActualWork(actualWork);
	}
	public void setFixedCost(double fixedCost, FieldContext fieldContext) {
	}
	public void setRemainingWork(long remainingWork, FieldContext fieldContext) {
	}
	public void setWork(long work, FieldContext fieldContext) {
	}
	//TimeSheetSchedule
	
	
	public double getPercentComplete() {
		return work==0?0.0:actualWork/work;
	}
	public long getRemainingDuration() {
		return 0;
	}
	public boolean isComplete() {
		return false;
	}
	public void setComplete(boolean complete) {
	}
	public void setPercentComplete(double percentComplete) {
	}
	public void setRemainingDuration(long remainingDuration) {
	}
	
	//Schedule?
	
	
	
	
	
}

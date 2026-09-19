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
package com.microproject.pm.assignment;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.microproject.configuration.Configuration;
import com.microproject.datatype.CanSupplyRateUnit;
import com.microproject.datatype.Rate;
import com.microproject.datatype.TimeUnit;
import com.microproject.document.Document;
import com.microproject.field.Field;
import com.microproject.pm.key.HasKey;
import com.microproject.pm.task.BelongsToDocument;
import com.microproject.pm.task.Task;
import com.microproject.util.ClassUtils;

/**
 * Used to enter new assignments in dialog
 */
public class AssignmentEntry implements HasRequestDemandType, BelongsToDocument,CanSupplyRateUnit {
	HasAssignments resource;
	ArrayList assignments;
	Document document;

	private static Field rateFieldInstance = null;
	public static Field getRateField() {
		if (rateFieldInstance == null)
			rateFieldInstance = Configuration.getFieldFromId("Field.assignmentEntryRate");
		return rateFieldInstance;
	}


	/**
	 * 
	 */
	public AssignmentEntry(HasAssignments resource, ArrayList assignments, Document document) {
		this.resource = resource;
		this.assignments = assignments;
		this.document = document;
	}
	
	public String getName() {
		return ((HasKey) resource).getName();
	}
	
	public void setRequestDemandType(int requestDemandType) {
		if (!isAssigned()) //requestDemand type only settable if already assigned 
			return;
		Iterator i = assignments.iterator();
		Assignment assignment;
		while (i.hasNext()) {
			assignment = (Assignment)i.next();
			assignment.setRequestDemandType(requestDemandType);
		}
	}

	public int getRequestDemandType() {
		Integer commonRequestDemandType = (Integer)Assignment.getRequestDemandTypeField().getCommonValue(assignments,false,false);
		if (commonRequestDemandType == null)
			return RequestDemandType.NONE;
		else
			return commonRequestDemandType.intValue();
	}
	
	private boolean isAssignmentListEmpty() {
		return (assignments == null) || assignments.size() == 0;
	}
	
	public Rate getRate() {
		Rate commonValue = (Rate) Assignment.getRateField().getCommonValue(assignments,false,false);
		if (commonValue == null) {
			if (isAssignmentListEmpty()) {
				if (getResource().isLabor())
					return ClassUtils.defaultRate;
				else
					return ClassUtils.defaultUnitlessRate;
			} else
				return ClassUtils.RATE_MULTIPLE_VALUES;
		}
		else {
			return commonValue;
		}
	}
	
	public void setRate(Rate rate) throws ParseException {
		Iterator i = assignments.iterator();
		Assignment assignment;
		int timeUnit = rate.getTimeUnit();
		while (i.hasNext()) {
			assignment = (Assignment)i.next();
			if (timeUnit != TimeUnit.NON_TEMPORAL)
				if (assignment.isLabor()) {
					assignment.adjustRemainingUnits(rate.getValue(), 0, true, false);
					assignment.forceUnits(rate.getValue());
				} else
					assignment.setRate(rate);
			else
				assignment.forceUnits(rate.getValue());
			assignment.setRateUnit(timeUnit);
			Assignment.getRateField().fireEvent(assignment, this,null); // update assignment rows
		}
	}
	
	public boolean isAssigned() {
		return assignments != null && assignments.size() > 0;
	}

	public int getAssignmentCount() {
		if (assignments == null)
			return 0;
		return assignments.size();
	}
	
	private void addAssignment(Assignment assignment) {
		if (assignments == null)
			assignments = new ArrayList();
		assignments.add(assignment);
	}
	
	/**
	 * Given a list of tasks, the assignments list will be filled to contain those assignments which
	 * refer to a task in the taskList
	 * @param taskList
	 */
	public void setAssignmentsFromTaskList(List taskList) {
		assignments = null;
		Iterator t = taskList.iterator();
		Task task;
		Object current;
		while (t.hasNext()) {
			current = t.next();
			if (!(current instanceof Task))
				continue;
			task = (Task)current;
			Assignment assignment = resource.findAssignment(task);
			if (assignment != null)
				addAssignment(assignment);
		}
		
	}
	
	public HasAssignments getResource() {
		return resource;
	}


	public Document getDocument() {
		return document;
	}


	public String getTimeUnitLabel() {
		return ((CanSupplyRateUnit)resource).getTimeUnitLabel();
	}
	
	public boolean isMaterial() {
		return ((CanSupplyRateUnit)resource).isMaterial();
	}
	

	
}

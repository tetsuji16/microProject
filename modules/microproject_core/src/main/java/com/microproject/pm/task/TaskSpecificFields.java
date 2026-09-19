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
package com.microproject.pm.task;

import com.microproject.field.FieldContext;
import com.microproject.field.FieldParseException;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.resource.Resource;

/**
 * 
 */
public interface TaskSpecificFields {
	double getFixedCost();
	void setFixedCost(double fixedCost);	
	boolean isWbsParent();
	String getWbsParentName();
	WorkCalendar getTaskCalendar();
	void setTaskCalendar(WorkCalendar workCalendar);
	String getResourceInitials();
	void setResourceInitials(String resourceInitials) throws FieldParseException;
	String getResourcePhonetics();
	String getResourceGroup();
	String getResourceNames();
	void setResourceNames(String resourceNames) throws FieldParseException;
	double getPercentComplete();
	void setPercentComplete(double percentComplete);
	double getPercentWorkComplete();
	void setPercentWorkComplete(double percentWorkComplete);
	long getTotalSlackStart();
	long getTotalSlackEnd();
	// task type is taken care of by schedulingRule
	boolean isMarkTaskAsMilestone();
	void setMarkTaskAsMilestone(boolean markTaskAsMilestone);
	public int getEarnedValueMethod();
	public void setEarnedValueMethod(int earnedValueMethod);
	public boolean isEstimated();
	public void setEstimated(boolean estimated);
	public boolean isIgnoreResourceCalendar();
	public void setIgnoreResourceCalendar(boolean ignoreResourceCalendar);
    public int getFixedCostAccrual();
    public void setFixedCostAccrual(int fixedCostAccrual);
    String getSubprojectFile();
    void setSubprojectFile(String sub);
	boolean fieldHideSubprojectFile(FieldContext fieldContext);

    boolean isSubprojectReadOnly();
	boolean fieldHideSubprojectReadOnly(FieldContext fieldContext);
	Resource getDelegatedTo();
	void setDelegatedTo(Resource delegatedTo);
}

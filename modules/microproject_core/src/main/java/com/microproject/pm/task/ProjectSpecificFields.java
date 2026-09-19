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
import com.microproject.pm.resource.ResourcePool;

/**
 * Project fields
 */
public interface ProjectSpecificFields {
	long getStatusDate();
	void setStatusDate(long statusDate);
	String getManager();
	void setManager(String manager);
	String getSchedulingMethod();
	ResourcePool getResourcePool();
	boolean isForward();
	void setForward(boolean forward);
	
	long getStartDate();
	void setStartDate(long start);
	boolean isReadOnlyStartDate(FieldContext fieldContext);
	long getFinishDate();
	void setFinishDate(long finish);
	boolean isReadOnlyFinishDate(FieldContext fieldContext);
	long getCurrentDate();
	void setCurrentDate(long currentDate);
	String getSubprojectOf();
	long getReferringSubprojectTaskDependencyDate();
	long getEarliestStartingTask();
	long getLatestFinishingTask();
	double getRisk();
	void setRisk(double risk);
	int getProjectType();
	void setProjectType(int projectType);
	int getProjectStatus();
	void setProjectStatus(int projectStatus);
	String getDivision();
	void setDivision(String division);
	String getGroup();
	void setGroup(String group);
	int getAccessControlPolicy();
	void setAccessControlPolicy(int accessControlPolicy);
//	boolean isShowProjectResourcesOnly();
//	void setShowProjectResourcesOnly(boolean showProjectResourcesOnly);
	public int getBenefit();
	public void setBenefit(int benefit);
	public double getNetPresentValue();
	public void setNetPresentValue(double netPresentValue);


}

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
	/** Type-safe view of the stable project type code. */
	@SuppressWarnings("deprecation")
	default ProjectType.Kind getProjectTypeKind() {
		return ProjectType.Kind.fromCode(getProjectType());
	}
	/** Stores a project type using its stable persisted code. */
	@SuppressWarnings("deprecation")
	default void setProjectTypeKind(ProjectType.Kind projectType) {
		setProjectType(java.util.Objects.requireNonNull(projectType, "projectType").code());
	}
	/** Type-safe view of the stable project status code. */
	@SuppressWarnings("deprecation")
	default ProjectStatus.Kind getProjectStatusKind() {
		return ProjectStatus.Kind.fromCode(getProjectStatus());
	}
	/** Stores a project status using its stable persisted code. */
	@SuppressWarnings("deprecation")
	default void setProjectStatusKind(ProjectStatus.Kind projectStatus) {
		setProjectStatus(java.util.Objects.requireNonNull(projectStatus, "projectStatus").code());
	}
	/** @deprecated use {@link #getProjectTypeKind()} for typed access. */
	@Deprecated
	int getProjectType();
	/** @deprecated use {@link #setProjectTypeKind(ProjectType.Kind)} for typed access. */
	@Deprecated
	void setProjectType(int projectType);
	/** @deprecated use {@link #getProjectStatusKind()} for typed access. */
	@Deprecated
	int getProjectStatus();
	/** @deprecated use {@link #setProjectStatusKind(ProjectStatus.Kind)} for typed access. */
	@Deprecated
	void setProjectStatus(int projectStatus);
	String getDivision();
	void setDivision(String division);
	String getGroup();
	void setGroup(String group);
	int getAccessControlPolicy();
	void setAccessControlPolicy(int accessControlPolicy);
	default AccessControlPolicy.Kind getAccessControlPolicyKind() {
		return AccessControlPolicy.Kind.fromCodeOrNull(getAccessControlPolicy());
	}
	default void setAccessControlPolicy(AccessControlPolicy.Kind accessControlPolicy) {
		setAccessControlPolicy(java.util.Objects.requireNonNull(accessControlPolicy, "accessControlPolicy").code());
	}
//	boolean isShowProjectResourcesOnly();
//	void setShowProjectResourcesOnly(boolean showProjectResourcesOnly);
	public int getBenefit();
	public void setBenefit(int benefit);
	public double getNetPresentValue();
	public void setNetPresentValue(double netPresentValue);


}

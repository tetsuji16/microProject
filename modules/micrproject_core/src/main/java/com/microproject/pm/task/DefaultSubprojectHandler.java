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

import java.util.Collection;
import java.util.ArrayList;

import com.microproject.grouping.core.Node;

public class DefaultSubprojectHandler implements SubprojectHandler {
	private final Project dummyProject;
	private Task containingSubprojectTask;
	private Collection referringSubprojectTasks = new ArrayList();

	public DefaultSubprojectHandler(Project dummy) {
		this.dummyProject = dummy;
	}
	public Task getContainingSubprojectTask() {
		return containingSubprojectTask;
	}

	public long getReferringSubprojectTaskDependencyDate() {
		long result = 0L;
		for (Object value : referringSubprojectTasks) {
			if (value instanceof Task)
				result = Math.max(result, ((Task) value).getDependencyStart());
		}
		return result;
	}

	public Collection getReferringSubprojectTasks() {
		return referringSubprojectTasks;
	}

	public String getSubprojectOf() {
		return dummyProject == null ? null : dummyProject.getName();
	}

	public void setContainingSubprojectTask(Task containingSubprojectTask) {
		this.containingSubprojectTask = containingSubprojectTask;
	}

	public void setReferringSubprojectTasks(Collection referringSubprojectTasks) {
		this.referringSubprojectTasks = referringSubprojectTasks == null
				? new ArrayList()
				: new ArrayList(referringSubprojectTasks);
	}

	public void switchToResourcesOfProject(Project useMe) {
		if (dummyProject != null && useMe != null)
			dummyProject.setResourcePool(useMe.getResourcePool());
	}

	public void addSubproject(Project subproject, Node subprojectNode, boolean creating, boolean currentlyOpen) {
		if (subproject == null)
			throw new IllegalArgumentException("Subproject cannot be null");
		subproject.setOpenedAsSubproject(true);
		if (subprojectNode != null && subprojectNode.getImpl() instanceof Task)
			subproject.setContainingSubprojectTask((Task) subprojectNode.getImpl());
	}

	public boolean canInsertProject(long projectId) {
		if (projectId <= 0L)
			return false;
		Project candidate = ProjectFactory.getInstance().findFromId(projectId);
		return candidate != null && candidate != dummyProject && !candidate.isOpenedAsSubproject();
	}

	public SubProj createSubProj(long subprojectUniqueId) {
		return new DefaultSubProj(dummyProject, subprojectUniqueId);
	}

}

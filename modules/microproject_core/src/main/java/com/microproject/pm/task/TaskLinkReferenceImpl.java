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

import com.microproject.association.AssociationList;
import com.microproject.pm.calendar.HasCalendar;
import com.microproject.pm.dependency.HasDependencies;

public class TaskLinkReferenceImpl implements TaskLinkReference{
	//static final long serialVersionUID = 18289183023830L;
	protected long uniqueId;
	protected Project project;
	public TaskLinkReferenceImpl(long uniqueId,Project project) {
		super();
		this.uniqueId = uniqueId;
		this.project = project;
	}
	public long getUniqueId() {
		return uniqueId;
	}
	public void setUniqueId(long uniqueId) {
		this.uniqueId = uniqueId;
	}
	public Project getProject() {
		return project;
	}
	public void setProject(Project project) {
		this.project = project;
	}
	public boolean dependsOn(HasDependencies other) {
		return false;
	}
	public HasCalendar getHasCalendar() {
		return null;
	}
	public AssociationList getPredecessorList() {
		return predecessors;
	}
	public AssociationList getSuccessorList() {
		return successors;
	}
	private transient AssociationList predecessors = new AssociationList();
	private transient AssociationList successors = new AssociationList();
	public AssociationList getDependencyList(boolean pred) {
		return pred ? predecessors : successors;
	}

}

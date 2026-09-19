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
package com.microproject.server.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class IncrementalData implements Serializable{
	private static final long serialVersionUID = 9272293000322L;
	protected ProjectData project;
	protected Set<ResourceData> resources;
	protected Set<AssignmentData> assignments;
	protected Set<LinkData> links;//,calendars;
	protected Map<TaskData, TaskData> tasks;
	protected Map<EnterpriseResourceData, EnterpriseResourceData> enterpriseResources;
	//calendars actually serialized inside projects, tasks and enterprise resources
	public Set<AssignmentData> getAssignments() {
		return assignments;
	}
	public void setAssignments(Set<AssignmentData> assignments) {
		this.assignments = assignments;
	}
	public void addAssignment(AssignmentData data) {
		if (assignments==null) assignments=new HashSet<>();
		data.emtpy();
		assignments.add(data);
	}
//	public List getCalendars() {
//		return calendars;
//	}
//	public void setCalendars(List calendars) {
//		this.calendars = calendars;
//	}
//	public void addCalendar(CalendarData data) {
//		if (calendars==null) calendars=new Vector();
//		calendars.add(data);
//	}
	public Set<LinkData> getLinks() {
		return links;
	}
	public void setLinks(Set<LinkData> links) {
		this.links = links;
	}
	public void addLink(LinkData data) {
		if (links==null) links=new HashSet<>();
		data.emtpy();
		links.add(data);
	}
	public ProjectData getProject() {
		return project;
	}
	public void setProject(ProjectData project) {
		project.emtpy();
		this.project = project;
	}
	public Set<ResourceData> getResources() {
		return resources;
	}
	public void setResources(Set<ResourceData> resources) {
		this.resources = resources;
	}
	public void addResource(ResourceData data) {
		if (resources==null) resources=new HashSet<>();
		data.emtpy();
		resources.add(data);
	}
	public Map<EnterpriseResourceData, EnterpriseResourceData> getEnterpriseResources() {
		return enterpriseResources;
	}
	public void setEnterpriseResources(Map<EnterpriseResourceData, EnterpriseResourceData> enterpriseResources) {
		this.enterpriseResources = enterpriseResources;
	}
	public void addEnterpriseResource(EnterpriseResourceData data) {
		if (enterpriseResources==null) enterpriseResources=new HashMap<>();
		data.emtpy();
		enterpriseResources.put(data,data);
	}
	public Map<TaskData, TaskData> getTasks() {
		return tasks;
	}
	public void setTasks(Map<TaskData, TaskData> tasks) {
		this.tasks = tasks;
	}
	public void addTask(TaskData data) {
		if (tasks==null) tasks=new HashMap<>();
		data.emtpy();
		tasks.put(data,data);
	}
	public Object clone(){ 
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
	
	
	
}

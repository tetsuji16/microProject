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
package com.microproject.core.pm.exchange.converters.mpx;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.mpxj.ProjectCalendar;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.mspdi.schema.TimephasedDataType;

import com.microproject.exchange.ImportedCalendarService;
import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Task;

/**
 * @author Laurent Chretienneau
 *
 */
public class MpxImportState {
	protected ProjectCalendar mpxStandardBaseCalendar;
	protected Map<String,ProjectCalendar> mpxBaseCalendarMap=new HashMap<String, ProjectCalendar>();
	protected Map<String,WorkCalendar> baseCalendarMap=new HashMap<String, WorkCalendar>();
	protected Map<net.sf.mpxj.Task,Task> taskMap=new HashMap<net.sf.mpxj.Task, Task>();
	protected Map<net.sf.mpxj.Resource,Resource> resourceMap=new HashMap<net.sf.mpxj.Resource, Resource>();
	protected Map<ResourceAssignment,List<TimephasedDataType>> mpxTimephasedMap;
	
	protected WorkCalendar projectBaseCalendar;
	protected String projectTitle;
	protected ResourcePool resourcePool;
	protected boolean mspdi;
	protected ProjectFile mpxProjectFile;
	
	public ResourcePool getResourcePool() {
		return resourcePool;
	}
	public void setResourcePool(ResourcePool resourcePool) {
		this.resourcePool = resourcePool;
	}
	public ProjectCalendar getMpxStandardBaseCalendar() {
		return mpxStandardBaseCalendar;
	}
	public void setMpxStandardBaseCalendar(ProjectCalendar mpxStandardBaseCalendar) {
		this.mpxStandardBaseCalendar = mpxStandardBaseCalendar;
	}
	public String getProjectTitle() {
		return projectTitle;
	}
	public void setProjectTitle(String projectTitle) {
		this.projectTitle = projectTitle;
	}
	/**
	 * Returns the microproject-native calendar bridge used during import.
	 * Base calendars are registered into the global CalendarService through it.
	 */
	public ImportedCalendarService getImportedCalendarService() {
		return ImportedCalendarService.getInstance();
	}
	public WorkCalendar getProjectBaseCalendar() {
		return projectBaseCalendar;
	}
	public void setProjectBaseCalendar(WorkCalendar projectBaseCalendar) {
		this.projectBaseCalendar = projectBaseCalendar;
	}
	
	public void mapBaseCalendar(WorkCalendar calendar,ProjectCalendar mpxCalendar){
		mpxBaseCalendarMap.put(mpxCalendar.getName(),mpxCalendar);
		baseCalendarMap.put(calendar.getName(),calendar);
	}
	public void registerImportedCalendar(WorkCalendar calendar, ProjectCalendar mpxCalendar) {
		if (calendar == null) {
			throw new IllegalArgumentException("calendar must not be null");
		}
		if (mpxCalendar == null) {
			throw new IllegalArgumentException("mpxCalendar must not be null");
		}
		if (calendar == projectBaseCalendar) {
			return;
		}
		ImportedCalendarService.getInstance().addImportedCalendar((WorkingCalendar) calendar, mpxCalendar);
		mapBaseCalendar(calendar, mpxCalendar);
	}
	public ProjectCalendar getMappedMpxBaseCalendar(String calendarName){
		return mpxBaseCalendarMap.get(calendarName);
	}
	public WorkCalendar getMappedBaseCalendar(String calendarName){
		return baseCalendarMap.get(calendarName);
	}
	/**
	 * Resolves a previously imported microproject calendar for the given MPXJ calendar.
	 */
	public WorkCalendar getImportedCalendar(ProjectCalendar mpxCalendar){
		return ImportedCalendarService.getInstance().findImportedCalendar(mpxCalendar);
	}
	
	public void mapTask(net.sf.mpxj.Task mpxTask, Task task){
		taskMap.put(mpxTask,task);
	}
	public Task getTask(net.sf.mpxj.Task mpxTask){
		return taskMap.get(mpxTask);
	}

	public void mapResource(net.sf.mpxj.Resource mpxResource, Resource resource){
		resourceMap.put(mpxResource,resource);
	}
	public Resource getResource(net.sf.mpxj.Resource mpxResource){
		return resourceMap.get(mpxResource);
	}
	public boolean isMspdi() {
		return mspdi;
	}
	public void setMspdi(boolean mspdi) {
		this.mspdi = mspdi;
	}
	public Map<ResourceAssignment, List<TimephasedDataType>> getMpxTimephasedMap() {
		return mpxTimephasedMap;
	}
	public void setMpxTimephasedMap(
			Map<ResourceAssignment, List<TimephasedDataType>> mpxTimephasedMap) {
		this.mpxTimephasedMap = mpxTimephasedMap;
	}
	public ProjectFile getMpxProjectFile() {
		return mpxProjectFile;
	}
	public void setMpxProjectFile(ProjectFile mpxProjectFile) {
		this.mpxProjectFile = mpxProjectFile;
	}

}

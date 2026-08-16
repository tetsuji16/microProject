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
package com.microproject.core.pm.exchange.converters.op;

import java.util.HashMap;
import java.util.Map;

import com.microproject.pm.calendar.WorkCalendar;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.task.Task;
import com.microproject.grouping.core.Node;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.task.NormalTask;

/**
 * Import state shared by the OpenProj (POD) converters. Calendar bookkeeping is
 * keyed by the microproject WorkingCalendar identifier (the deleted CalendarId /
 * CalendarManager model is no longer used; see issue #154).
 * @author Laurent Chretienneau
 */
public class OpImportState {
	protected Map<Long,WorkingCalendar> opBaseCalendarMap=new HashMap<Long, WorkingCalendar>();
	protected Map<Long,WorkCalendar> baseCalendarMap=new HashMap<Long, WorkCalendar>();
	protected Map<Task,NormalTask> opTaskMap=new HashMap<Task, NormalTask>();
	protected Map<NormalTask,Node> opTaskNodeMap=new HashMap<NormalTask, Node>();
	protected Map<Resource,com.microproject.pm.resource.Resource> opResourceMap=new HashMap<Resource, com.microproject.pm.resource.Resource>();
	protected Map<com.microproject.pm.resource.Resource,Node> opResourceNodeMap=new HashMap<com.microproject.pm.resource.Resource, Node>();

	public void mapBaseCalendar(WorkCalendar calendar,WorkingCalendar opCalendar){
		Long id = opCalendar.getId();
		opBaseCalendarMap.put(id,opCalendar);
		baseCalendarMap.put(id,calendar);
	}
	public WorkingCalendar getMappedOpBaseCalendar(Long id){
		return opBaseCalendarMap.get(id);
	}
	public WorkCalendar getMappedBaseCalendar(Long id){
		return baseCalendarMap.get(id);
	}
	
	public void mapOpTask(Task task, NormalTask opTask){
		opTaskMap.put(task,opTask);
	}
	public NormalTask getOpTask(Task task){
		return opTaskMap.get(task);
	}
	public void mapOpTaskNode(NormalTask task, Node taskNode){
		opTaskNodeMap.put(task,taskNode);
	}
	public Node getOpTaskNode(NormalTask task){
		return opTaskNodeMap.get(task);
	}
	public void mapOpResource(Resource resource, com.microproject.pm.resource.Resource opResource){
		opResourceMap.put(resource,opResource);
	}
	public com.microproject.pm.resource.Resource getOpResource(Resource resource){
		return opResourceMap.get(resource);
	}
	public void mapOpResourceNode(com.microproject.pm.resource.Resource resource, Node resourceNode){
		opResourceNodeMap.put(resource,resourceNode);
	}
	public Node getOpResourceNode(com.microproject.pm.resource.Resource resource){
		return opResourceNodeMap.get(resource);
	}

}

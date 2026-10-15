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

import java.util.ArrayList;
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
	private final List<Runnable> postScheduleValidations = new ArrayList<>();
	private Map<Long, RawMspdiAssignmentWork> rawMspdiAssignmentWork = Map.of();
	private Map<Long, Map<Integer, List<RawMspdiWorkInterval>>> rawMspdiAssignmentBaselineWork = Map.of();
	private Map<Long, Map<Integer, List<RawMspdiCostInterval>>> rawMspdiAssignmentBaselineCost = Map.of();
	private Map<Long, Map<Integer, RawMspdiAssignmentBaseline>> rawMspdiAssignmentBaselines = Map.of();
	private Map<Long, Map<Integer, RawMspdiTaskBaseline>> rawMspdiTaskBaselines = Map.of();
	private Map<Long, RawMspdiResourceRates> rawMspdiResourceRates = Map.of();

	public record RawMspdiWorkInterval(int type, long start, long finish, long work) { }
	public record RawMspdiCostInterval(int type, long start, long finish, double cost) { }
	public record RawMspdiAssignmentBaseline(Long start, Long finish, Long work) { }
	public record RawMspdiTaskBaseline(Long durationMillis, Integer durationFormat, Double fixedCost) { }
	public record RawMspdiResourceRates(com.microproject.datatype.Rate standardRate,
		com.microproject.datatype.Rate overtimeRate, Double costPerUse) { }

	/** MSPDI assignment baseline work Type values: 4, then 16 + 6 per slot. */
	public static int assignmentBaselineWorkType(int slot) {
		if (slot < 0 || slot > 10) throw new IllegalArgumentException("Invalid baseline slot: " + slot);
		return slot == 0 ? 4 : 16 + (slot - 1) * 6;
	}

	/** MSPDI assignment baseline cost Type values immediately follow each work type. */
	public static int assignmentBaselineCostType(int slot) {
		return assignmentBaselineWorkType(slot) + 1;
	}
	public record RawMspdiAssignmentWork(Long start, Long finish, Long work, Long actualWork, Long remainingWork,
		List<RawMspdiWorkInterval> intervals) {
		public RawMspdiAssignmentWork {
			intervals = List.copyOf(intervals);
		}
	}

	public void setRawMspdiAssignmentWork(Map<Long, RawMspdiAssignmentWork> values) {
		rawMspdiAssignmentWork = Map.copyOf(values);
	}
	public RawMspdiAssignmentWork getRawMspdiAssignmentWork(Long uid) {
		return uid == null ? null : rawMspdiAssignmentWork.get(uid);
	}
	public void setRawMspdiAssignmentBaselineWork(
		Map<Long, Map<Integer, List<RawMspdiWorkInterval>>> values) {
		Map<Long, Map<Integer, List<RawMspdiWorkInterval>>> copy = new HashMap<>();
		values.forEach((uid, slots) -> {
			Map<Integer, List<RawMspdiWorkInterval>> slotCopy = new HashMap<>();
			slots.forEach((slot, intervals) -> slotCopy.put(slot, List.copyOf(intervals)));
			copy.put(uid, Map.copyOf(slotCopy));
		});
		rawMspdiAssignmentBaselineWork = Map.copyOf(copy);
	}
	public List<RawMspdiWorkInterval> getRawMspdiAssignmentBaselineWork(Long uid, int slot) {
		Map<Integer, List<RawMspdiWorkInterval>> slots = uid == null ? null
			: rawMspdiAssignmentBaselineWork.get(uid);
		return slots == null ? null : slots.get(Integer.valueOf(slot));
	}
	public void setRawMspdiAssignmentBaselineCost(
		Map<Long, Map<Integer, List<RawMspdiCostInterval>>> values) {
		Map<Long, Map<Integer, List<RawMspdiCostInterval>>> copy = new HashMap<>();
		values.forEach((uid, slots) -> {
			Map<Integer, List<RawMspdiCostInterval>> slotCopy = new HashMap<>();
			slots.forEach((slot, intervals) -> slotCopy.put(slot, List.copyOf(intervals)));
			copy.put(uid, Map.copyOf(slotCopy));
		});
		rawMspdiAssignmentBaselineCost = Map.copyOf(copy);
	}
	public List<RawMspdiCostInterval> getRawMspdiAssignmentBaselineCost(Long uid, int slot) {
		Map<Integer, List<RawMspdiCostInterval>> slots = uid == null ? null
			: rawMspdiAssignmentBaselineCost.get(uid);
		return slots == null ? null : slots.get(Integer.valueOf(slot));
	}
	public void setRawMspdiAssignmentBaselines(
		Map<Long, Map<Integer, RawMspdiAssignmentBaseline>> values) {
		Map<Long, Map<Integer, RawMspdiAssignmentBaseline>> copy = new HashMap<>();
		values.forEach((uid, baselines) -> copy.put(uid, Map.copyOf(baselines)));
		rawMspdiAssignmentBaselines = Map.copyOf(copy);
	}
	public boolean hasRawMspdiAssignmentBaselines(Long uid) {
		return uid != null && rawMspdiAssignmentBaselines.containsKey(uid);
	}
	public RawMspdiAssignmentBaseline getRawMspdiAssignmentBaseline(Long uid, int slot) {
		Map<Integer, RawMspdiAssignmentBaseline> slots = uid == null ? null
			: rawMspdiAssignmentBaselines.get(uid);
		return slots == null ? null : slots.get(Integer.valueOf(slot));
	}
	public void setRawMspdiTaskBaselines(Map<Long, Map<Integer, RawMspdiTaskBaseline>> values) {
		Map<Long, Map<Integer, RawMspdiTaskBaseline>> copy = new HashMap<>();
		values.forEach((uid, baselines) -> copy.put(uid, Map.copyOf(baselines)));
		rawMspdiTaskBaselines = Map.copyOf(copy);
	}
	public RawMspdiTaskBaseline getRawMspdiTaskBaseline(Long uid, int slot) {
		Map<Integer, RawMspdiTaskBaseline> baselines = uid == null ? null : rawMspdiTaskBaselines.get(uid);
		return baselines == null ? null : baselines.get(Integer.valueOf(slot));
	}
	public void setRawMspdiResourceRates(Map<Long, RawMspdiResourceRates> values) {
		rawMspdiResourceRates = Map.copyOf(values);
	}
	public RawMspdiResourceRates getRawMspdiResourceRates(Long uid) {
		return uid == null ? null : rawMspdiResourceRates.get(uid);
	}
	
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
	public void initializeMpxTimephasedMap() {
		postScheduleValidations.clear();
		int assignmentCount = mpxProjectFile == null ? 0 : mpxProjectFile.getResourceAssignments().size();
		mpxTimephasedMap = new HashMap<>(mapCapacity(assignmentCount));
	}
	public void addPostScheduleValidation(Runnable validation) {
		if (validation == null) {
			throw new IllegalArgumentException("validation must not be null");
		}
		postScheduleValidations.add(validation);
	}
	public void validatePostSchedule() {
		List<Runnable> validations = List.copyOf(postScheduleValidations);
		postScheduleValidations.clear();
		for (Runnable validation : validations) {
			validation.run();
		}
	}
	public ProjectFile getMpxProjectFile() {
		return mpxProjectFile;
	}
	public void setMpxProjectFile(ProjectFile mpxProjectFile) {
		this.mpxProjectFile = mpxProjectFile;
		int taskCount = mpxProjectFile == null ? 0 : mpxProjectFile.getTasks().size();
		int resourceCount = mpxProjectFile == null ? 0 : mpxProjectFile.getResources().size();
		taskMap = new HashMap<>(mapCapacity(taskCount));
		resourceMap = new HashMap<>(mapCapacity(resourceCount));
	}

	private static int mapCapacity(int expectedSize) {
		if (expectedSize <= 12) return 16;
		long capacity = ((long) expectedSize * 4 + 2) / 3;
		return (int) Math.min(1L << 30, capacity);
	}

}

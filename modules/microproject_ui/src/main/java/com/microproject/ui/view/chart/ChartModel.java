/*******************************************************************************
 * MIT License
 *
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
package com.microproject.ui.view.chart;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.List;

import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.microproject.association.Association;
import com.microproject.ui.view.timescale.CoordinatesConverter;
import com.microproject.ui.view.views.ChartView;
import com.microproject.algorithm.TimeIteratorGenerator;
import com.microproject.algorithm.buffer.CalculatedValues;
import com.microproject.algorithm.buffer.GroupedCalculatedValues;
import com.microproject.algorithm.buffer.NonGroupedCalculatedValues;
import com.microproject.algorithm.buffer.SeriesCallback;
import com.microproject.field.Field;
import com.microproject.interval.ValueObjectForInterval;
import com.microproject.options.CalendarOption;
import com.microproject.model.availability.Availability;
import com.microproject.model.assignment.Assignment;
import com.microproject.model.assignment.HasAssignments;
import com.microproject.model.assignment.HasTimeDistributedData;
import com.microproject.model.assignment.TimeDistributedConstants;
import com.microproject.model.assignment.contour.AbstractContourBucket;
import com.microproject.model.assignment.contour.ContourBucketIntervalGenerator;
import com.microproject.model.calendar.WorkCalendar;
import com.microproject.model.calendar.WorkDay;
import com.microproject.model.calendar.WorkRange;
import com.microproject.util.DateTime;
import com.microproject.model.resource.Resource;
import com.microproject.model.resource.ResourceImpl;
import com.microproject.model.task.Project;
import com.microproject.timescale.TimeInterval;
import com.microproject.timescale.TimeIterator;
import com.microproject.util.Environment;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ChartModel implements TimeDistributedConstants, Serializable {
	private static final long serialVersionUID = -1617376166476506096L;
	private static final Logger logger = Logger.getLogger(ChartModel.class.getName());
	private ChartView chartModel;
	XYSeriesCollection seriesCollection;
	XYSeriesCollection secondSeriesCollection = null;
	private ResourceGraphData resourceGraph;
	CoordinatesConverter coord;

	public ChartModel(CoordinatesConverter coord) {
		this.coord = coord;
	}
	
	public CalculatedValues computeTrace(Iterator<?> taskIterator, List<Resource> resources, Object trace,  boolean histogram, boolean cumulative) {
		if (taskIterator == null || !taskIterator.hasNext()) // if no task selected, dont change chart
			return null;
		CalculatedValues calculatedValues;
		if (histogram)
			calculatedValues = new GroupedCalculatedValues();
		else
			calculatedValues = new NonGroupedCalculatedValues(cumulative,coord.getOrigin());
		

		TimeIterator timeIterator = null;
		TimeIteratorGenerator generator;
		Iterator<?> i = taskIterator;
		Object current;
		boolean hasValues = false;
		while (i.hasNext()) { //loop thru tasks
			current = i.next();
			if (current instanceof HasAssignments hasAssignments) {
				Iterator<Association> a = hasAssignments.getAssignments().iterator();
				while (a.hasNext()) { // loop through assignments,
					Association association = a.next();
					if (!(association instanceof Assignment assignment))
						continue;
					if (histogram) {
						timeIterator = coord.getProjectTimeIterator();
						generator = histogram ? TimeIteratorGenerator.getInstance(timeIterator) : null;
					} else {
						generator = null;
					}
					if (isTaskBased(trace)) {
						hasValues = true;
						assignment.calcDataBetween(trace,generator,calculatedValues);
						break;
					}
					if (!assignment.isDefault()) {// skip dummy assignment
						if (resources == null || resources.contains(assignment.getResource())) {
							hasValues = true;
							assignment.calcDataBetween(trace,generator,calculatedValues);
						}
					}
				}
			}
		}
		if (!hasValues) // if nothing processed
			return null;
		return calculatedValues;
	}


	public CalculatedValues computeAvailability(List<Resource> resources) {
		CalculatedValues calculatedValues = new GroupedCalculatedValues();
		if (resources != null) {
			for (Resource res : resources) {
				TimeIterator timeIterator = coord.getProjectTimeIterator();
				TimeIteratorGenerator generator = TimeIteratorGenerator.getInstance(timeIterator);
				Assignment.calcResourceAvailabilityBetween(res, generator, calculatedValues);
			}
		}
		return calculatedValues;
	}
	
	// Other Projects
	// Other Projects
	public CalculatedValues computeOtherProjects(List<?> tasks, List<Resource> resources) {
		CalculatedValues calculatedValues = new GroupedCalculatedValues();
		if ((resources!=null&&resources.size()>0)||(tasks!=null&&tasks.size()>0)){ //resources can be put in tasks list, is it a bug?
			List<?> otherProjectItems = (resources==null||resources.size()==0)?tasks:resources;
			GroupedCalculatedValues c=(GroupedCalculatedValues)calculatedValues;
			
			TimeIterator timeIterator = coord.getProjectTimeIterator();
			for (int k=0;timeIterator.hasNext();k++){
				TimeInterval interval=timeIterator.next();
				c.set(k,interval.getStart1(),interval.getEnd1(),0.0,null);
			}
				//return calculatedValues; 
			for (Object obj : otherProjectItems) {
				ResourceImpl resource;
				if (obj instanceof Assignment assignment)
					resource=(ResourceImpl)assignment.getResource();
				else if (obj instanceof ResourceImpl resourceImpl)
					resource=resourceImpl;
				else
					continue;
				GroupedCalculatedValues global=resource.getGlobalResource().getGlobalWorkVector();
				if (global != null) {
					global=global.dayByDayConvert();
					c.mergeIn(global);
				}
			}
			return c;
		}
		return calculatedValues;
	}

	public void computeHistogram(Project project, List<?> tasks, List<Resource> resources,Object[] traces) {
		boolean stackCurrentOnTop = traces == HasTimeDistributedData.histogramTypes;

		// Availability
		GroupedCalculatedValues availabilityCalculatedValues = (GroupedCalculatedValues) computeAvailability(resources);
		GroupedCalculatedValues otherProjectsCalculatedValues = null;

		//This Project
		GroupedCalculatedValues thisProjectCalculatedValues = (GroupedCalculatedValues) computeTrace(project.getTaskOutlineIterator(), resources, WORK,  true, false);

		//Selected
		GroupedCalculatedValues selectedCalculatedValues = (GroupedCalculatedValues) computeTrace(tasks==null?null:tasks.iterator(),resources, WORK,  true, false);
		XYSeries overallocatedSeries = new XYSeries(OVERALLOCATED.toString(), false, true);


		// stack so that order is (from botom to top) other projects, this project, selected
		for (int i = 0; i < availabilityCalculatedValues.size(); i++) {
			double thisProject = thisProjectCalculatedValues != null ? thisProjectCalculatedValues.getUnscaledValue(i) : 0D;
			double allProjects = thisProject;
			if (otherProjectsCalculatedValues!=null) allProjects+= otherProjectsCalculatedValues.getUnscaledValue(i); // stack
			double selected = selectedCalculatedValues != null ? selectedCalculatedValues.getUnscaledValue(i) : 0D;
			Long date = availabilityCalculatedValues.getDate(i);
			if (date == null && thisProjectCalculatedValues != null) date = thisProjectCalculatedValues.getDate(i);
			if (date != null) {
				double excess = overallocatedAmount(allProjects, availabilityCalculatedValues.getUnscaledValue(i));
				if (excess > 0D) overallocatedSeries.add(date.doubleValue(), excess / getScaleFactor(WORK));
			}
			if (stackCurrentOnTop) {
				if (selectedCalculatedValues != null)
					selectedCalculatedValues.setValue(i, allProjects);
				if (thisProjectCalculatedValues != null)
					thisProjectCalculatedValues.setValue(i, allProjects - selected);
			} else {
				if (otherProjectsCalculatedValues!=null) otherProjectsCalculatedValues.setValue(i,allProjects);
			}
		}
		
		XYSeries availabilitySeries = buildHistogramSeries(AVAILABILITY,availabilityCalculatedValues);
		XYSeries otherProjectsSeries = null;
		if (otherProjectsCalculatedValues!=null) otherProjectsSeries=buildHistogramSeries(OTHER_PROJECTS,otherProjectsCalculatedValues);
		XYSeries thisProjectSeries = buildHistogramSeries(THIS_PROJECT,thisProjectCalculatedValues);
		XYSeries selectedSeries = buildHistogramSeries(SELECTED,selectedCalculatedValues);
		
		
		seriesCollection = new XYSeriesCollection();
		if (stackCurrentOnTop) {
			seriesCollection.addSeries(selectedSeries);
			seriesCollection.addSeries(thisProjectSeries);
			if (otherProjectsSeries!=null) seriesCollection.addSeries(otherProjectsSeries);
		} else {
			if (otherProjectsSeries!=null) seriesCollection.addSeries(otherProjectsSeries);
			seriesCollection.addSeries(thisProjectSeries);
			seriesCollection.addSeries(selectedSeries);
		}

		secondSeriesCollection = new XYSeriesCollection();
		secondSeriesCollection.addSeries(availabilitySeries);
		secondSeriesCollection.addSeries(overallocatedSeries);
	}

	/**
	 * Computes the Resource Graph for the graph type of the settings: the per-interval work, cost and
	 * availability of the filtered resources and of the selected tasks, turned into series by
	 * {@link ResourceGraphData}.
	 */
	public void computeResourceGraph(Project project, List<?> tasks, List<Resource> resources, ResourceGraphSettings settings,
			boolean selectedOnTop) {
		GroupedCalculatedValues availability = (GroupedCalculatedValues) computeAvailability(resources);
		int n = availability.size();
		long[] dates = new long[n];
		double[] availabilityValues = new double[n];
		for (int i = 0; i < n; i++) {
			Long date = availability.getDate(i);
			dates[i] = date == null ? 0L : date.longValue();
			availabilityValues[i] = availability.getUnscaledValue(i);
		}
		ResourceGraphSettings.GraphType type = settings.getGraphType();
		boolean cost = type == ResourceGraphSettings.GraphType.COST || type == ResourceGraphSettings.GraphType.CUMULATIVE_COST;
		Object trace = cost ? COST : WORK;
		GroupedCalculatedValues total = (GroupedCalculatedValues) computeTrace(project.getTaskOutlineIterator(), resources, trace, true, false);
		GroupedCalculatedValues selected = (GroupedCalculatedValues) computeTrace(tasks == null ? null : tasks.iterator(), resources, trace, true, false);
		double[] totalValues = new double[n];
		double[] selectedValues = new double[n];
		for (int i = 0; i < n; i++) {
			if (dates[i] == 0L && total != null && i < total.size() && total.getDate(i) != null)
				dates[i] = total.getDate(i).longValue();
			totalValues[i] = total == null ? 0D : total.getUnscaledValue(i);
			selectedValues[i] = selected == null ? 0D : selected.getUnscaledValue(i);
		}
		double[] peakTotal = null;
		double[] peakSelected = null;
		double[] peakOverallocated = null;
		double[] peakSelectedOverallocated = null;
		if (!cost && type == ResourceGraphSettings.GraphType.PEAK_UNITS) {
			long[] ends = periodEnds(coord.getProjectTimeIterator(), n);
			peakTotal = exactPeakUnits(project.getTaskOutlineIterator(), resources, dates, ends);
			peakSelected = exactPeakUnits(tasks == null ? null : tasks.iterator(), resources, dates, ends);
			ResourceGraphData.PeakOverallocated overload = exactPeakOverallocatedUnits(project, tasks, resources, dates, ends);
			peakOverallocated = overload.total();
			peakSelectedOverallocated = overload.selected();
		}
		resourceGraph = ResourceGraphData.compute(type, dates, cost ? null : totalValues, cost ? null : selectedValues,
			availabilityValues, cost ? totalValues : null, cost ? selectedValues : null, getScaleFactor(WORK), selectedOnTop,
			peakTotal, peakSelected, peakOverallocated, peakSelectedOverallocated);
	}

	/** Computes the exact simultaneous assignment units in each graph period from the work contours. */
	static double[] exactPeakUnits(Iterator<?> tasks, List<Resource> resources, long[] starts, long[] ends) {
		List<ResourceGraphData.AllocationInterval> intervals = allocationIntervals(tasks, resources);
		return ResourceGraphData.peakUnits(starts, ends, intervals);
	}

	private static List<ResourceGraphData.AllocationInterval> allocationIntervals(Iterator<?> tasks, List<Resource> resources) {
		List<ResourceGraphData.AllocationInterval> intervals = new ArrayList<>();
		if (tasks == null)
			return intervals;
		while (tasks.hasNext()) {
			Object task = tasks.next();
			if (!(task instanceof HasAssignments hasAssignments))
				continue;
			for (Association association : hasAssignments.getAssignments()) {
				if (!(association instanceof Assignment assignment)
						|| assignment.isDefault()
						|| resources != null && !resources.contains(assignment.getResource()))
					continue;
				ContourBucketIntervalGenerator generator = assignment.contourGeneratorInstance(WORK);
				while (generator.hasNext() && generator.test(null)) {
					AbstractContourBucket bucket = (AbstractContourBucket) generator.current();
					if (bucket == null)
						continue;
					double units = Math.max(0D, bucket.getEffectiveUnits(assignment.getUnits()));
					if (units > 0D && generator.currentStart() < generator.currentEnd())
						addWorkingIntervals(intervals, assignment.getEffectiveWorkCalendar(), generator.currentStart(),
							generator.currentEnd(), units, assignment.getResource());
				}
			}
		}
		return intervals;
	}

	/** Computes the exact peak load above resource capacity at matching work instants in each period. */
	static double[] exactPeakOverallocatedUnits(Project project, List<Resource> resources, long[] starts, long[] ends) {
		return exactPeakOverallocatedUnits(project, null, resources, starts, ends).total();
	}

	static ResourceGraphData.PeakOverallocated exactPeakOverallocatedUnits(Project project, List<?> tasks,
			List<Resource> resources, long[] starts, long[] ends) {
		if (starts.length == 0 || ends.length == 0)
			return new ResourceGraphData.PeakOverallocated(new double[starts.length], new double[starts.length]);
		List<ResourceGraphData.AllocationInterval> loads = allocationIntervals(project.getTaskOutlineIterator(), resources);
		List<ResourceGraphData.AllocationInterval> selectedLoads = allocationIntervals(tasks == null ? null : tasks.iterator(), resources);
		List<ResourceGraphData.AllocationInterval> capacities = new ArrayList<>();
		List<Resource> capacityResources = resources == null ? project.getResourcePool().getResourceList() : resources;
		for (Resource resource : capacityResources)
			addResourceCapacityIntervals(capacities, project, resource, starts[0], ends[ends.length - 1]);
		return ResourceGraphData.peakOverallocatedUnits(starts, ends, loads, selectedLoads, capacities);
	}

	private static void addResourceCapacityIntervals(List<ResourceGraphData.AllocationInterval> target, Project project,
			Resource resource, long from, long to) {
		if (resource == null || from >= to)
			return;
		WorkCalendar calendar = resource.getEffectiveWorkCalendar();
		if (calendar == null) calendar = project.getEffectiveWorkCalendar();
		if (calendar == null)
			return;
		var availabilityTable = resource.getAvailabilityTable();
		List<ValueObjectForInterval> availabilityValues = availabilityTable.getList();
		com.microproject.model.calendar.CalendarDefinition definition = calendar.getConcreteInstance();
		for (long day = DateTime.dayFloor(from); day < to; day = DateTime.nextDay(day)) {
			WorkDay workDay = definition.getWorkDay(day);
			if (workDay == null) workDay = WorkDay.getDefaultWorkDay();
			for (WorkRange range : workDay.getWorkingHours().getIntervals()) {
				if (range == null) continue;
				long workingStart = Math.max(from, timeOnDay(day, range.getStart(), range.getStart()));
				long workingEnd = Math.min(to, timeOnDay(day, range.getEnd(), range.getStart()));
				if (workingEnd <= workingStart) continue;
				List<Long> boundaries = new ArrayList<>();
				boundaries.add(workingStart);
				for (ValueObjectForInterval value : availabilityValues) {
					if (value.getStart() > workingStart && value.getStart() < workingEnd) boundaries.add(value.getStart());
					if (value.getEnd() > workingStart && value.getEnd() < workingEnd) boundaries.add(value.getEnd());
				}
				boundaries.add(workingEnd);
				boundaries.sort(Long::compare);
				for (int i = 1; i < boundaries.size(); i++) {
					long segmentStart = boundaries.get(i - 1);
					long segmentEnd = boundaries.get(i);
					if (segmentEnd <= segmentStart) continue;
					Object value = availabilityTable.findActive(segmentStart);
					double units = value instanceof Availability availability
						? availability.getMaximumUnits() : resource.getMaximumUnits();
					if (units > 0D)
						target.add(new ResourceGraphData.AllocationInterval(segmentStart, segmentEnd, units, resource));
				}
			}
		}
	}

	/** Returns the actual timescale boundary after each plotted interval, including the final interval. */
	static long[] periodEnds(com.microproject.timescale.TimeIterator periods, int count) {
		long[] ends = new long[count];
		for (int i = 0; i < count && periods.hasNext(); i++)
			ends[i] = periods.next().getEnd();
		return ends;
	}

	/** Intersects a contour bucket's wall-clock span with the assignment's effective working calendar. */
	private static void addWorkingIntervals(List<ResourceGraphData.AllocationInterval> target, WorkCalendar calendar,
			long from, long to, double units, Resource resource) {
		if (calendar == null || from >= to)
			return;
		com.microproject.model.calendar.CalendarDefinition definition = calendar.getConcreteInstance();
		for (long day = DateTime.dayFloor(from); day < to; day = DateTime.nextDay(day)) {
			WorkDay workDay = definition.getWorkDay(day);
			if (workDay == null)
				workDay = WorkDay.getDefaultWorkDay();
			for (WorkRange range : workDay.getWorkingHours().getIntervals()) {
				if (range == null)
					continue;
				long workingStart = timeOnDay(day, range.getStart(), range.getStart());
				long workingEnd = timeOnDay(day, range.getEnd(), range.getStart());
				long activeStart = Math.max(from, workingStart);
				long activeEnd = Math.min(to, workingEnd);
				if (activeEnd > activeStart)
					target.add(new ResourceGraphData.AllocationInterval(activeStart, activeEnd, units, resource));
			}
		}
	}

	/** Applies the hour/minute fields from a calendar work range to a concrete local date (DST-safe). */
	private static long timeOnDay(long day, long rangeTime, long rangeStart) {
		java.util.Calendar range = DateTime.calendarInstance();
		range.setTimeInMillis(rangeTime);
		java.util.Calendar result = DateTime.calendarInstance();
		result.setTimeInMillis(day);
		if (DateTime.dayFloor(rangeTime) > DateTime.dayFloor(rangeStart))
			result.add(java.util.Calendar.DAY_OF_MONTH, 1);
		result.set(java.util.Calendar.HOUR_OF_DAY, range.get(java.util.Calendar.HOUR_OF_DAY));
		result.set(java.util.Calendar.MINUTE, range.get(java.util.Calendar.MINUTE));
		result.set(java.util.Calendar.SECOND, range.get(java.util.Calendar.SECOND));
		result.set(java.util.Calendar.MILLISECOND, range.get(java.util.Calendar.MILLISECOND));
		return result.getTimeInMillis();
	}

	/** The series computed by the last {@link #computeResourceGraph}, or null before the first one. */
	public ResourceGraphData getResourceGraph() {
		return resourceGraph;
	}

	/** Resource Graph over-allocation is the work above the resource availability. */
	static double overallocatedAmount(double work, double availability) {
		return Math.max(0D, work - availability);
	}
	
	private XYSeries buildHistogramSeries(Object trace, CalculatedValues values) {
		if (values == null)
			return dummySeries(trace);
		XYSeries series = new XYSeries(trace.toString(),false,true); // dont bother sorting it already is
		makeSeries(series, trace, false, values);
		return series;
	}

	private XYSeries dummySeries(Object trace) {
		return new XYSeries(trace.toString(),false,true); // dont bother sorting it already is
		
	}
	private int findTrace(Object[] traces,Object trace) {
		for (int i = 0; i < traces.length; i++)
			if (traces[i] == trace)
				return i;
		return -1;
	}
	
	public void dumpDataset(Object[] traces) {
		for (int i = 0; i < seriesCollection.getSeriesCount(); i++) {
			logger.log(Level.FINE, "series {0} {1}", new Object[] { i, traces[i] });
			dumpSeries(seriesCollection.getSeries(i));
		}
	}
	public static void dumpSeries(XYSeries series) {
		for (int i = 0; i < series.getItemCount(); i++) {
			Logger.getLogger(ChartModel.class.getName()).log(Level.FINE,
				"{0} {1}", new Object[] { new java.util.Date(series.getX(i).longValue()), series.getY(i) });
		}
	}
	
	private boolean isTraceRectilinear(Object trace) {
		return trace == AVAILABILITY;
	}
	private void makeSeries(XYSeries series, final Object trace, boolean cumulative, CalculatedValues calculatedValues) {
		final double scaleFactor = getScaleFactor(trace); 
		final XYSeries _series = series;
		if (isTraceRectilinear(trace)) {
			calculatedValues.makeRectilinearSeries(new SeriesCallback() {
				public void add(int index, double x, double y) {
					_series.add(x,y / scaleFactor);
				}
			});
		} else {
			calculatedValues.makeSeries(cumulative,new SeriesCallback() {
				public void add(int index, double x, double y) {
					_series.add(x,y / scaleFactor);
				}
			});
		}
	}
	
	
	
	public void computeValues(List<?> tasks, List<Resource> resources, boolean cumulative, Object[] traces, boolean histogram) {
		if (tasks == null)
			return;
		CalculatedValues valuesArray[] = new CalculatedValues[traces.length];
		seriesCollection = new XYSeriesCollection();

		secondSeriesCollection = null;
		XYSeries series;
		for (int i = 0; i < traces.length; i++) {
			valuesArray[i] = computeTrace(tasks==null?null:tasks.iterator(),resources,traces[i],histogram,cumulative);
		}
		
		
		// done in a second step in case traces depend on each other.  Right now, there is no case like that
		for (int i = 0; i < traces.length; i++) {
			series = new XYSeries(traces[i].toString(),false,true); // dont bother sorting it already is
			if (valuesArray[i] == null) {
				logger.log(Level.FINE, "skipping null values array {0}", traces[i]);
				continue;
			}
			makeSeries(series,traces[i],cumulative,valuesArray[i]);

			seriesCollection.addSeries(series);
			
		}
	}
	
	
	private double getScaleFactor(Object trace) {
		if (trace instanceof Field && !((Field)trace).isDurationOrWork())
			return 1.0;
		double hourScale = CalendarOption.getInstance().getHoursPerDay() / 24.0D; // need to adjust for number of working hours compared to 24 hours in a day
		return hourScale * coord.getIntervalDuration();
	}
	/**
	 * @return Returns the dataset.
	 */
	public AbstractXYDataset getDataset() {
		return seriesCollection;
	}
	
	private final boolean isTaskBased(Object trace) {
		return (trace == FIXED_COST ||
				trace == ACTUAL_FIXED_COST);
	}

	public XYDataset getSecondDataset() {
		return secondSeriesCollection;
	}
	
}

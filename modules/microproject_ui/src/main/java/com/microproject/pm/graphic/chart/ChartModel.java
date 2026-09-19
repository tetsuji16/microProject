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
package com.microproject.pm.graphic.chart;

import java.io.Serializable;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.List;

import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.microproject.association.Association;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.graphic.views.ChartView;
import com.microproject.algorithm.TimeIteratorGenerator;
import com.microproject.algorithm.buffer.CalculatedValues;
import com.microproject.algorithm.buffer.GroupedCalculatedValues;
import com.microproject.algorithm.buffer.NonGroupedCalculatedValues;
import com.microproject.algorithm.buffer.SeriesCallback;
import com.microproject.field.Field;
import com.microproject.options.CalendarOption;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.assignment.HasAssignments;
import com.microproject.pm.assignment.HasTimeDistributedData;
import com.microproject.pm.assignment.TimeDistributedConstants;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.task.Project;
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
		Assignment assignment;	
		boolean hasValues = false;
		while (i.hasNext()) { //loop thru tasks
			current = i.next();
			if (current instanceof HasAssignments) {
				Iterator<Association> a = ((HasAssignments) current).getAssignments().iterator();
				while (a.hasNext()) { // loop through assignments,
					Association association = a.next();
					if (!(association instanceof Assignment))
						continue;
					assignment = (Assignment) association;
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
			Iterator<?> j=((resources==null||resources.size()==0)?tasks:resources).iterator();
			GroupedCalculatedValues c=(GroupedCalculatedValues)calculatedValues;
			
			TimeIterator timeIterator = coord.getProjectTimeIterator();
			for (int k=0;timeIterator.hasNext();k++){
				TimeInterval interval=timeIterator.next();
				c.set(k,interval.getStart1(),interval.getEnd1(),0.0,null);
			}
				//return calculatedValues; 
			while (j.hasNext()){
				ResourceImpl resource;
				Object obj=j.next();
				if (obj instanceof Assignment)
					resource=(ResourceImpl)((Assignment)obj).getResource();
				else if (obj instanceof ResourceImpl)
					resource=(ResourceImpl)obj;
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
		boolean stackCurrentOnTop = traces == (Environment.getStandAlone()?HasTimeDistributedData.histogramTypes:HasTimeDistributedData.serverHistogramTypes);

		// Availability
		GroupedCalculatedValues availabilityCalculatedValues = (GroupedCalculatedValues) computeAvailability(resources);

		// Other Projects
		GroupedCalculatedValues otherProjectsCalculatedValues = null;
		if (!Environment.getStandAlone()) otherProjectsCalculatedValues=(GroupedCalculatedValues) computeOtherProjects(tasks,resources);
		
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
			//System.out.println("\n trace #"+i);
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

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
import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.JFreeChart;

import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.event.CacheListener;
import com.microproject.pm.graphic.model.event.CompositeCacheEvent;
import com.microproject.pm.graphic.spreadsheet.selection.event.SelectionNodeEvent;
import com.microproject.pm.graphic.spreadsheet.selection.event.SelectionNodeListener;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.graphic.views.ChartView;
import com.microproject.document.ObjectEvent;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.scheduling.ScheduleEvent;
import com.microproject.pm.scheduling.ScheduleEventListener;
import com.microproject.pm.task.Project;
import com.microproject.timescale.TimeScaleEvent;
import com.microproject.timescale.TimeScaleListener;
import com.microproject.workspace.SavableToWorkspace;
import com.microproject.workspace.WorkspaceSetting;

/**
 * This class serves as a moderator between the ChartPanel and the ChartLegend
 */
public class ChartInfo implements Serializable, SelectionNodeListener, ScheduleEventListener, TimeScaleListener, ObjectEvent.Listener, CacheListener, SavableToWorkspace{
	private static final long serialVersionUID = -6593093924980192805L;
	Project project;
	NodeModel nodeModel;
	ChartView chartView;
	
	boolean simple;
	List<Object> selectedObjects = new ArrayList<>();
	ChartModel model;
	ChartLegend chartLegend;
	JFreeChart chart;
	CoordinatesConverter coord;
	List<?> tasks;
	List<Resource> resources;
	boolean cumulative = false;
	boolean histogram = true;
	boolean selectedOnTop = true;
	boolean work=true;
	Object traces[] = {};	
	TimeChartPanel chartPanel;
	AxisPanel axisPanel;
	NodeModelCache cache = null; // for resources
	private boolean restoring = false;
	/**
	 * 
	 */
	public ChartInfo() {
		super();
	}

	/**
	 * @return Returns the chartView.
	 */
	public ChartView getChartView() {
		return chartView;
	}
	/**
	 * @param chartView The chartView to set.
	 */
	public void setChartView(ChartView chartView) {
		this.chartView = chartView;
	}
	/**
	 * @return Returns the coord.
	 */
	public CoordinatesConverter getCoord() {
		return coord;
	}
	/**
	 * @param coord The coord to set.
	 */
	public void setCoord(CoordinatesConverter coord) {
		if (this.coord != null)
			this.coord.removeTimeScaleListener(this);
		this.coord = coord;
		model = new ChartModel(coord);
		coord.addTimeScaleListener(this);
	}
	/**
	 * @return Returns the cumulative.
	 */
	public boolean isCumulative() {
		return cumulative;
	}

	public boolean isSelectedOnTop() {
		return selectedOnTop;
	}

	/**
	 * @return Returns the histogram.
	 */
	public boolean isHistogram() {
		return histogram;
	}
	/**
	 * @param histogram The histogram to set.
	 */
	public void setHistogram(boolean histogram) {
		this.histogram = histogram;
		updateChart(tasks,resources);		
	}
	/**
	 * @return Returns the model.
	 */
	public ChartModel getModel() {
		return model;
	}
	/**
	 * @param model The model to set.
	 */
	public void setModel(ChartModel model) {
		this.model = model;
	}
	/**
	 * @return Returns the resources.
	 */
	public List<Resource> getResources() {
		return resources;
	}
	/**
	 * @param resources The resources to set.
	 */
	public void setResources(List<Resource> resources) {
		this.resources = resources;
	}
	/**
	 * @return Returns the selectedObjects.
	 */
	public List<Object> getSelectedObjects() {
		return selectedObjects;
	}
	/**
	 * @param selectedObjects The selectedObjects to set.
	 */
	public void setSelectedObjects(List<Object> selectedObjects) {
		this.selectedObjects = selectedObjects;
	}
	/**
	 * @return Returns the simple.
	 */
	public boolean isSimple() {
		return simple;
	}
	/**
	 * @param simple The simple to set.
	 */
	public void setSimple(boolean simple) {
		this.simple = simple;
	}
	/**
	 * @return Returns the tasks.
	 */
	public List<?> getTasks() {
		return tasks;
	}
	/**
	 * @param tasks The tasks to set.
	 */
	public void setTasks(List<?> tasks) {
		this.tasks = tasks;
	}
	/**
	 * @return Returns the traces.
	 */
	public Object[] getTraces() {
		return traces;
	}
	/**
	 * @return Returns the chart.
	 */
	public JFreeChart getChart() {
		return chart;
	}
	/**
	 * @param chart The chart to set.
	 */
	public JFreeChart setChart(JFreeChart chart) {
		this.chart = chart;
		return chart;
	}
	/**
	 * @return Returns the chartPanel.
	 */
	public TimeChartPanel getChartPanel() {
		return chartPanel;
	}
	/**
	 * @param chartPanel The chartPanel to set.
	 */
	public void setChartPanel(TimeChartPanel chartPanel) {
		this.chartPanel = chartPanel;
	}
	
	
	/**
	 * @return Returns the project.
	 */
	public Project getProject() {
		return project;
	}
	/**
	 * @param project The project to set.
	 */
	public void setProject(Project project) {
		if (this.project != null) {
			this.project.removeScheduleListener(this);
			this.project.getResourcePool().removeObjectListener(this);
		}
		this.project = project;
		project.addScheduleListener(this);
		project.getResourcePool().addObjectListener(this);
		nodeModel = project.getResourcePool().getResourceOutline();		
	}
	

		
	/**
	 * @return Returns the nodeModel.
	 */
	public NodeModel getNodeModel() {
		return nodeModel;
	}
	/**
	 * @param nodeModel The nodeModel to set.
	 */
	public void setNodeModel(NodeModel nodeModel) {
		this.nodeModel = nodeModel;
	}
	public void selectionChanged(SelectionNodeEvent e) {
		if (!isVisible())
			return;
		chartLegend.selectionChanged(e); // pass it along
	}
	
	/**
	 * @return Returns the chartLegend.
	 */
	public ChartLegend getChartLegend() {
		return chartLegend;
	}
	/**
	 * @param chartLegend The chartLegend to set.
	 */
	public void setChartLegend(ChartLegend chartLegend) {
		this.chartLegend = chartLegend;
	}

	public void setTraces(Object[] traces) {
		if (traces.length == 0) // will happen if changing from cost to work.  Don't change it
			return;
		this.traces = traces;
		if (chartPanel != null) // the very first time on histogram we need to set traces before chart panel is created
			updateChart(tasks,resources);
	}	
	public void timeScaleChanged(TimeScaleEvent e) {
		if (!isVisible())
			return;
		
		updateChart(tasks,resources);
	}	
	public void scheduleChanged(ScheduleEvent evt) {
		if (!isVisible())
			return;
		
		updateChart(tasks,resources);
	}
	public void setCumulative(boolean cumulative) {
		this.cumulative = cumulative;
		updateChart(tasks,resources);
	}
	public void setSelectedOnTop(boolean multiproject) {
		this.selectedOnTop = multiproject;
	}
	
	public double getFooterHeight() {
		return chartPanel.getNonPlotHeight();
	}
	public double getHeaderHeight() {
		return chartView.getHeaderComponentHeight();
	}
	
	public void updateChart(List<?> tasks, List<Resource> resources) {
		this.tasks = tasks;
		this.resources = resources;
		if (isSimple())
			model.computeHistogram(getProject(), tasks, resources,traces);
		else
			model.computeValues(tasks,resources, cumulative, traces, histogram);
		chart = chartPanel.buildChart();
		setChart(chart);
		chartPanel.updateChart();
		axisPanel.setAxis(getChart().getXYPlot().getRangeAxis());
		axisPanel.repaint();
	}
	
	/**
	 * @return Returns the axisPanel.
	 */
	public AxisPanel getAxisPanel() {
		return axisPanel;
	}
	/**
	 * @param axisPanel The axisPanel to set.
	 */
	public void setAxisPanel(AxisPanel axisPanel) {
		this.axisPanel = axisPanel;
	}

	public void objectChanged(ObjectEvent objectEvent) {
		if (!isVisible())
			return;

		if (objectEvent.getObject() instanceof Resource) {
			chartLegend.rebuildTree(); // take into account different resources
			updateChart(tasks,resources);
		}
	}
	
	boolean isVisible() {
		return chartView.isVisible();
	}

	public void setCache(NodeModelCache cache) {
		if (this.cache != null)
			this.cache.removeNodeModelListener(this);
		this.cache = cache;
		cache.update();
		cache.addNodeModelListener(this);
	}

	public final NodeModelCache getCache() {
		return cache;
	}

	public void graphicNodesCompositeEvent(CompositeCacheEvent e) {
		if (!isVisible())
			return;
		chartLegend.rebuildTree();
	}

	public void setCumulativeCostMode() {
		cumulative = true;
		histogram = false;
	}
	public boolean isWork() {
		return work;
	}

	public void setWork(boolean work) {
		this.work = work;
	}

	public void restoreWorkspace(WorkspaceSetting w, int context) {
		Workspace ws = (Workspace) w;
		cumulative = ws.cumulative;
		histogram = ws.histogram;
		selectedOnTop = ws.selectedOnTop;
		work = ws.work;
		chartPanel.setVerticalScrolling(ws.verticalScroll);
		chartPanel.verticalScrollingItem.setSelected(ws.verticalScroll);
		if (!simple)
			setTraces(SpreadSheetFieldArray.fromIdArray(ws.traces));
	}
	public WorkspaceSetting createWorkspace(int context) {
		Workspace ws = new Workspace();
		ws.cumulative = cumulative;
		ws.histogram = histogram;
		ws.selectedOnTop = selectedOnTop;
		ws.work = work;
		ws.verticalScroll = chartPanel.isVerticalScrolling();
		if (!simple)
			ws.traces = SpreadSheetFieldArray.toIdArray(traces);
		return ws;
	}

	public static class Workspace implements WorkspaceSetting { 
		private static final long serialVersionUID = -1369065811123053002L;
		boolean cumulative;
		boolean histogram;
		boolean selectedOnTop;
		boolean work;
		Object[] traces;
		boolean verticalScroll;
		public boolean isCumulative() {
			return cumulative;
		}
		public void setCumulative(boolean cumulative) {
			this.cumulative = cumulative;
		}
		public boolean isHistogram() {
			return histogram;
		}
		public void setHistogram(boolean histogram) {
			this.histogram = histogram;
		}
		public boolean isSelectedOnTop() {
			return selectedOnTop;
		}
		public void setSelectedOnTop(boolean selectedOnTop) {
			this.selectedOnTop = selectedOnTop;
		}
		public boolean isWork() {
			return work;
		}
		public void setWork(boolean work) {
			this.work = work;
		}
		public Object[] getTraces() {
			return traces;
		}
		public void setTraces(Object[] traces) {
			this.traces = traces;
		}
		public boolean isVerticalScroll() {
			return verticalScroll;
		}
		public void setVerticalScroll(boolean verticalScroll) {
			this.verticalScroll = verticalScroll;
		}
	}

	public boolean isRestoring() {
		return restoring;
	}

	public void setRestoring(boolean restoring) {
		this.restoring = restoring;
	}

}

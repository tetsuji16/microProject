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
package com.microproject.ui.view.views;

import java.util.function.Consumer;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JScrollPane;


import com.microproject.ui.menu.MenuActionConstants;
import com.microproject.ui.menu.MenuManager;
import com.microproject.ui.view.chart.ChartInfo;
import com.microproject.ui.view.chart.ChartLegend;
import com.microproject.ui.view.chart.ChartMode;
import com.microproject.pm.resource.Resource;
import com.microproject.ui.view.chart.TimeChartPanel;
import com.microproject.ui.frame.DocumentFrame;
import com.microproject.ui.view.model.cache.NodeModelCache;
import com.microproject.ui.view.model.cache.NodeModelCacheFactory;
import com.microproject.ui.view.model.cache.ReferenceNodeModelCache;
import com.microproject.ui.view.spreadsheet.SpreadSheet;
import com.microproject.ui.view.spreadsheet.selection.event.SelectionNodeEvent;
import com.microproject.ui.view.spreadsheet.selection.event.SelectionNodeListener;
import com.microproject.ui.view.timescale.CoordinatesConverter;
import com.microproject.ui.view.timescale.ScaledScrollPane;
import com.microproject.ui.view.views.synchro.Synchronizer;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.undo.UndoController;
import com.microproject.workspace.WorkspaceSetting;

/**
 * 
 */
public class ChartView extends SplittedView implements SelectionNodeListener, BaseView {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3633037900192609747L;
	ScaledScrollPane scrollPane = null;
	ChartInfo chartInfo;
	MenuManager menuManager;
	DocumentFrame documentFrame;
	private ChartLegend chartLegend;
	private Consumer<Object> transformerClosure;
	/**
	 * @param synchronizer 
	 * @param manager
	 *  
	 */
	public ChartView(DocumentFrame documentFrame, ChartMode mode, MenuManager menuManager, Synchronizer synchronizer,Consumer<Object> transformerClosure) {
		super(synchronizer);
		this.menuManager = menuManager;
		this.documentFrame = documentFrame;
		this.sync=false;
		this.transformerClosure=transformerClosure;
		chartInfo = new ChartInfo();
		chartInfo.setProject(documentFrame.getProject());
		chartInfo.setMode(mode);
		chartInfo.setChartView(this);
		chartInfo.setCache(getCache());
		setDeltaDivider(GraphicConfiguration.getInstance().getRowChartHeaderWidth());
		setNeedVoidBar(false);
		//setScaled(true);
	}

	public void init(CoordinatesConverter coord) {
		chartInfo.setCoord(coord);
		super.init();

	}

	public void cleanUp() {	
		super.cleanUp();
		chartInfo.getCoord().removeTimeScaleListener(scrollPane);
		scrollPane = null;
		chartInfo = null;
		menuManager = null;
		documentFrame = null;
	}

	protected JScrollPane createLeftScrollPane() {
		chartLegend = new ChartLegend(chartInfo);
		chartInfo.setChartLegend(chartLegend);
		JScrollPane result =  new JScrollPane(chartLegend.createContentPanel());
		result.getVerticalScrollBar().setUnitIncrement(20);
		return result;
	}


	protected JScrollPane createRightScrollPane() {
		TimeChartPanel chartPanel = new TimeChartPanel(chartInfo);
		chartPanel.setPreferredSize(new Dimension(0,0)); //to avoid initial vertical scroll bar
		chartInfo.setChartPanel(chartPanel);
		scrollPane = new ScaledScrollPane(chartPanel, chartInfo.getCoord(),documentFrame,10);
		chartInfo.getAxisPanel().setPreferredSize(new Dimension(GraphicConfiguration.getInstance().getRowChartHeaderWidth(),(int)chartPanel.getPreferredSize().getHeight()));
 		chartPanel.configureScrollPaneHeaders(scrollPane,chartInfo.getAxisPanel());
		chartInfo.setChartPanel(chartPanel);
		return scrollPane;
	}
	   public void activateEmptyRowHeader(boolean activate){
	    scrollPane.activateEmptyRowHeader(activate);
	   }

	public void resetScrollPane() {
		scrollPane.getViewport().add(chartInfo.getChartPanel());
	}
/**
 * Pass message on to chart info mediator
 */
	public void selectionChanged(SelectionNodeEvent e) {
		chartInfo.selectionChanged(e); // pass it along
	}
	
	/** The Resource Graph Format settings; meaningful for the Resource Graph mode. */
	public void setResourceSelectionListener(Runnable listener) {
		chartInfo.setResourceSelectionListener(listener);
	}

	public com.microproject.ui.view.chart.ResourceGraphSettings getResourceGraphSettings() {
		return chartInfo.getResourceGraphSettings();
	}

	/** Re-plots after the Resource Graph Format settings changed. */
	public void resourceGraphSettingsChanged() {
		chartInfo.resourceGraphSettingsChanged();
	}

	public boolean selectAdjacentResource(int delta) {
		return chartInfo.selectAdjacentResource(delta);
	}

	public boolean hasAdjacentResource(int delta) {
		return chartInfo.hasAdjacentResource(delta);
	}

	public int getHeaderComponentHeight() {
		if (scrollPane == null)
			return GraphicConfiguration.getInstance().getColumnHeaderHeight();
		return scrollPane.getTimeScaleComponent().getHeight();
	}

	public UndoController getUndoController() {
		return null; // charts are read only
	}

	public void zoomIn() {
		chartInfo.getCoord().zoomIn();
	}

	public void zoomOut() {
		chartInfo.getCoord().zoomOut();
	}
	public boolean canZoomIn() {
		return chartInfo.getCoord().canZoomIn();
	}

	public boolean canZoomOut() {
		return chartInfo.getCoord().canZoomOut();
	}
	public int getScale() {
		return chartInfo.getCoord().getTimescaleManager().getCurrentScaleIndex();
	}

	public SpreadSheet getSpreadSheet() {
		return null;
	}

	public boolean hasNormalMinWidth() {
		return true;
	}

	public String getViewName() {
		return chartInfo.getMode().getViewAction();
	}

	public boolean showsTasks() {
		return false;
	}

	public boolean showsResources() {
		return false;
	}
	
	NodeModelCache cache = null;
	public NodeModelCache getCache() {
		if (cache == null)
			cache = NodeModelCacheFactory.getInstance().createFilteredCache(documentFrame.getResourceNodeModelCache(),chartInfo.getMode().getViewAction(),transformerClosure);
		return cache;
	}

	public void onActivate(boolean activate) {
	}

	public boolean isPrintable() {
		return false;
	}
	
	public void restoreWorkspace(WorkspaceSetting w, int context) {
		Workspace ws = (Workspace) w;
		chartInfo.setRestoring(true);
		chartInfo.restoreWorkspace(ws.chartInfo, context);
		scrollPane.restoreWorkspace(ws.scrollPane, context);
		restoreDividerLocation(ws.dividerLocation);
		chartLegend.setControlValues();
		chartLegend.restoreWorkspace(ws.chartLegend, context);
		getLeftScrollPane().getViewport().setViewPosition(ws.legendViewPosition);
		getRightScrollPane().getViewport().setViewPosition(ws.chartViewPosition);
		chartInfo.setRestoring(false);
		chartInfo.replotRestoredResourceGraph();
	}

	/** Captures chart selections before a workspace reload resets the main view selection. */
	public SelectionState snapshotSelection() {
		List<Object> selectedObjects = chartLegend.getSelectedObjects();
		List<Resource> selectedResources = chartLegend.getSelectedResources();
		return new SelectionState(selectedObjects, selectedResources);
	}

	/** Reapplies selected objects and resources after the chart workspace state is restored. */
	public void restoreSelection(SelectionState selection) {
		if (selection != null)
			chartLegend.restoreSelection(selection.selectedObjects, selection.selectedResources);
	}

	public static final class SelectionState {
		private final List<Object> selectedObjects;
		private final List<Resource> selectedResources;

		public SelectionState(List<?> selectedObjects, List<Resource> selectedResources) {
			this.selectedObjects = selectedObjects == null ? null : new ArrayList<>(selectedObjects);
			this.selectedResources = selectedResources == null ? List.of() : new ArrayList<>(selectedResources);
		}

		public List<Object> getSelectedObjects() {
			return selectedObjects == null ? null : List.copyOf(selectedObjects);
		}

		public List<Resource> getSelectedResources() {
			return List.copyOf(selectedResources);
		}
	}
	public WorkspaceSetting createWorkspace(int context) {
		Workspace ws = new Workspace();
		ws.chartInfo = chartInfo.createWorkspace(context);
		ws.scrollPane = scrollPane.createWorkspace(context);
		ws.dividerLocation = getDividerLocation();
		ws.chartLegend = chartLegend.createWorkspace(context);
		ws.legendViewPosition = getLeftScrollPane().getViewport().getViewPosition();
		ws.chartViewPosition = getRightScrollPane().getViewport().getViewPosition();
		return ws;
	}

	public static class Workspace implements WorkspaceSetting { 
		private static final long serialVersionUID = 5497933936501158451L;
		WorkspaceSetting chartInfo;
		WorkspaceSetting scrollPane;
		WorkspaceSetting chartLegend;
		int dividerLocation;
		Point legendViewPosition;
		Point chartViewPosition;
		
		public WorkspaceSetting getChartInfo() {
			return chartInfo;
		}

		public void setChartInfo(WorkspaceSetting chartInfo) {
			this.chartInfo = chartInfo;
		}

		public WorkspaceSetting getScrollPane() {
			return scrollPane;
		}

		public void setScrollPane(WorkspaceSetting scrollPane) {
			this.scrollPane = scrollPane;
		}

		public int getDividerLocation() {
			return dividerLocation;
		}

		public void setDividerLocation(int dividerLocation) {
			this.dividerLocation = dividerLocation;
		}

		public WorkspaceSetting getChartLegend() {
			return chartLegend;
		}

		public void setChartLegend(WorkspaceSetting chartLegend) {
			this.chartLegend = chartLegend;
		}

		public Point getLegendViewPosition() {
			return legendViewPosition;
		}

		public void setLegendViewPosition(Point legendViewPosition) {
			this.legendViewPosition = legendViewPosition;
		}

		public Point getChartViewPosition() {
			return chartViewPosition;
		}

		public void setChartViewPosition(Point chartViewPosition) {
			this.chartViewPosition = chartViewPosition;
		}
	}

	public boolean canScrollToTask() {
		return false;
	}

	public void scrollToTask() {
	}

}

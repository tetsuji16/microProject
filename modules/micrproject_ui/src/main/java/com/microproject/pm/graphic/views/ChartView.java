/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License 
 * Version 1.0 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public 
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of 
 * software over a computer network and provide for limited attribution for the 
 * Original Developer. In addition, Exhibit A has been modified to be consistent 
 * with Exhibit B. 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the 
 * specific language governing rights and limitations under the License. The 
 * Original Code is ProjectLibre. The Original Developer is the Initial Developer 
 * and is ProjectLibre Inc. All portions of the code written by ProjectLibre are 
 * Copyright (c) 2012-2019. All Rights Reserved. All portions of the code written by 
 * ProjectLibre are Copyright (c) 2012-2019. All Rights Reserved. Contributor 
 * ProjectLibre, Inc.
 *
 * Alternatively, the contents of this file may be used under the terms of the 
 * ProjectLibre End-User License Agreement (the ProjectLibre License) in which case 
 * the provisions of the ProjectLibre License are applicable instead of those above. 
 * If you wish to allow use of your version of this file only under the terms of the 
 * ProjectLibre License and not to allow others to use your version of this file 
 * under the CPAL, indicate your decision by deleting the provisions above and 
 * replace them with the notice and other provisions required by the ProjectLibre 
 * License. If you do not delete the provisions above, a recipient may use your 
 * version of this file under either the CPAL or the ProjectLibre Licenses. 
 *
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text of the notices 
 * in the Source Code files of the Original Code. You should use the text of this 
 * Exhibit A rather than the text found in the Original Code Source Code for Your 
 * Modifications.] 
 *
 * EXHIBIT B. Attribution Information for ProjectLibre required
 *
 * Attribution Copyright Notice: Copyright (c) 2012-2019, ProjectLibre, Inc.
 * Attribution Phrase (not exceeding 10 words): 
 * ProjectLibre, open source project management software.
 * Attribution URL: http://www.projectlibre.com
 * Graphic Image as provided in the Covered Code as file: projectlibre-logo.png with 
 * alternatives listed on http://www.projectlibre.com/logo 
 *
 * Display of Attribution Information is required in Larger Works which are defined 
 * in the CPAL as a work which combines Covered Code or portions thereof with code 
 * not governed by the terms of the CPAL. However, in addition to the other notice 
 * obligations, all copies of the Covered Code in Executable and Source Code form 
 * distributed must, as a form of attribution of the original author, include on 
 * each user interface screen the "ProjectLibre" logo visible to all users. 
 * The ProjectLibre logo should be located horizontally aligned with the menu bar 
 * The 
 * logo must be at least 144 x 31 pixels. When users click on the "ProjectLibre" 
 * logo it must direct them back to http://www.projectlibre.com. 
 *******************************************************************************/
package com.microproject.pm.graphic.views;

import java.util.function.Consumer;

import java.awt.Dimension;
import java.awt.Point;

import javax.swing.JScrollPane;


import com.microproject.menu.MenuActionConstants;
import com.microproject.menu.MenuManager;
import com.microproject.pm.graphic.chart.ChartInfo;
import com.microproject.pm.graphic.chart.ChartLegend;
import com.microproject.pm.graphic.chart.TimeChartPanel;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.selection.event.SelectionNodeEvent;
import com.microproject.pm.graphic.spreadsheet.selection.event.SelectionNodeListener;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.graphic.timescale.ScaledScrollPane;
import com.microproject.pm.graphic.views.synchro.Synchronizer;
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
	public ChartView(DocumentFrame documentFrame, boolean simple, MenuManager menuManager, Synchronizer synchronizer,Consumer<Object> transformerClosure) {
		super(synchronizer);
		this.menuManager = menuManager;
		this.documentFrame = documentFrame;
		this.sync=false;
		this.transformerClosure=transformerClosure;
		chartInfo = new ChartInfo();
		chartInfo.setProject(documentFrame.getProject());
		chartInfo.setSimple(simple);
		if (!simple) {
			chartInfo.setCumulativeCostMode();
		}
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
		if (chartInfo.isSimple())
			return MenuActionConstants.ACTION_HISTOGRAM;
		else 
			return MenuActionConstants.ACTION_CHARTS;
	}

	public boolean showsTasks() {
		return false;
	}

	public boolean showsResources() {
		return false;
	}
	
	NodeModelCache cache = null;
	public NodeModelCache getCache() {
		if (cache == null) // note that histogram and charts share same filtered cache
			cache = NodeModelCacheFactory.getInstance().createFilteredCache((ReferenceNodeModelCache)documentFrame.getResourceNodeModelCache(),MenuActionConstants.ACTION_CHARTS,transformerClosure);
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


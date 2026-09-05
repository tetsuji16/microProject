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
package com.microproject.pm.graphic.views;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Collection;

import javax.swing.JScrollPane;


import com.microproject.help.HelpUtil;
import com.microproject.menu.MenuManager;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.graphic.model.transform.NodeCacheTransformer;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.selection.event.SelectionNodeEvent;
import com.microproject.pm.graphic.spreadsheet.selection.event.SelectionNodeListener;
import com.microproject.pm.graphic.spreadsheet.time.FieldArrayEvent;
import com.microproject.pm.graphic.spreadsheet.time.FieldArrayListener;
import com.microproject.pm.graphic.spreadsheet.time.TimeSpreadSheet;
import com.microproject.pm.graphic.spreadsheet.time.TimeSpreadSheetModel;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.graphic.timescale.ScaledScrollPane;
import com.microproject.pm.graphic.views.synchro.Synchronizer;
import com.microproject.configuration.Dictionary;
import com.microproject.field.FieldContext;
import com.microproject.graphic.configuration.CellStyle;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.transform.ViewTransformer;
import com.microproject.grouping.core.transform.filtering.NodeFilter;
import com.microproject.grouping.core.transform.filtering.SelectionFilter;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.task.Project;
import com.microproject.strings.Messages;
import com.microproject.undo.UndoController;
import com.microproject.workspace.WorkspaceSetting;

/**
 * 
 */
public class UsageDetailView extends SplittedView implements BaseView, FieldArrayListener, SelectionNodeListener {
	private static final long serialVersionUID = 8603734334991284800L;
	protected SpreadSheet spreadSheet;
	protected TimeSpreadSheet timeSpreadSheet;
	protected ReferenceNodeModelCache refCache;
	protected NodeModelCache cache;
	protected CoordinatesConverter coord;
	private Project project;
	DocumentFrame documentFrame;
	protected boolean taskUsage;
	FieldContext fieldContext;
	protected ScaledScrollPane timeScrollPane;
	protected CellStyle cellStyle;
	protected String viewName;
	protected boolean hasEmptyRows;
	private boolean subView;

	/**
	 * @param project
	 * @param manager
	 * 
	 */
	public UsageDetailView(DocumentFrame documentFrame, MenuManager manager,Synchronizer synchronizer) {
		super(synchronizer);
		this.documentFrame = documentFrame;
		this.project = documentFrame.getProject();
		setNeedVoidBar(true);
		// setScaled(true);

		setDeltaDivider(GraphicConfiguration.getInstance().getRowHeaderWidth());
	}

	public void init(ReferenceNodeModelCache refCache, boolean taskUsage, CoordinatesConverter coord, boolean subView, String viewName,Consumer<Object> transformerClosure) {
		this.coord = coord;
		this.subView = subView;
		hasEmptyRows = !subView;
		this.viewName = viewName;
		this.cache = NodeModelCacheFactory.getInstance().createFilteredCache(refCache, viewName,transformerClosure);


		this.taskUsage = taskUsage;
		fieldContext = new FieldContext();
		fieldContext.setLeftAssociation(taskUsage);
		HelpUtil.addDocHelp(this,taskUsage ? (subView ? "Task_Usage" : "Task_Usage_Detail") : (subView ? "Resource_Usage" : "Resource_Usage_Detail"));
		super.init();
		// cache.update(); //this is not required by certain views
	}
	public static String getUsageAssignmentSpreadsheetId(boolean taskUsage) {
		return taskUsage ? "Spreadsheet.Assignment.taskUsage" : "Spreadsheet.Assignment.resourceUsage";
	}
	public static String getUsageDistributionSpreadsheetId(boolean taskUsage) {
		return taskUsage ? "Spreadsheet.TaskUsage.default" : "Spreadsheet.ResourceUsage.default";
	}
	public void cleanUp() {
		super.cleanUp();
		if (coord != null && timeScrollPane != null) {
			coord.removeTimeScaleListener(timeScrollPane);
		}
		if (timeSpreadSheet != null && timeSpreadSheet.getModel() instanceof TimeSpreadSheetModel timeSpreadSheetModel) {
			timeSpreadSheetModel.removeFieldArrayListener(this);
		}
		if (spreadSheet != null) {
			spreadSheet.cleanUp();
		}
		if (timeSpreadSheet != null) {
			timeSpreadSheet.cleanUp();
		}
		spreadSheet = null;
		timeSpreadSheet = null;
		refCache = null;
		cache = null;
		coord = null;
		project = null;
		documentFrame = null;
		fieldContext = null;
		timeScrollPane = null;
		cellStyle = null;
		viewName = null;
	}

	protected JScrollPane createLeftScrollPane() {
		spreadSheet = new SpreadSheet() {
			private static final long serialVersionUID = 1996911145637609217L;

			public boolean isNodeDeletable(Node node) {
				return !(node.getImpl() instanceof Resource); // only delete resource on res list
			}};
		spreadSheet.setSpreadSheetCategory((taskUsage)?taskAssignmentSpreadsheetCategory:resourceAssignmentSpreadsheetCategory); // for columns.  must do first
		SpreadSheetFieldArray fields = getFields();
		spreadSheet.setCache(cache, fields, fields.getCellStyle(),fields.getActionList());
		((SpreadSheetModel) spreadSheet.getModel()).setFieldContext(fieldContext);

		cache.update(); //this is not required by certain views 

		if (project.isReadOnly())
			spreadSheet.setReadOnly(true);
		return SpreadSheetUtils.makeSpreadsheetScrollPane(spreadSheet);
	}

	protected JScrollPane createRightScrollPane() {
		timeSpreadSheet = new TimeSpreadSheet(project);
		timeSpreadSheet.setSpreadSheetCategory(timeSpreadsheetCategory);
		SpreadSheetFieldArray fields = getDistributionFields();
		timeSpreadSheet.setCache(cache, fields, fields.getCellStyle(),fields.getActionList());
		((TimeSpreadSheetModel) timeSpreadSheet.getModel()).addFieldArrayListener(this);

		timeScrollPane = new ScaledScrollPane(timeSpreadSheet, coord, documentFrame,timeSpreadSheet.getRowHeight());
		timeSpreadSheet.createDefaultColumnsFromModel();
		forceUpdateOfTimeSpreadSheet();
//		timeSpreadSheet.revalidate();
//		timeSpreadSheet.repaint();
		if (project.isReadOnly())
			timeSpreadSheet.setEnabled(false);
		return timeScrollPane;
	}

	// spreadsheet fields
	private SpreadSheetFieldArray getFields() {
		String spreadsheetId = getUsageAssignmentSpreadsheetId(taskUsage);
		String category = taskUsage ? taskAssignmentSpreadsheetCategory : resourceAssignmentSpreadsheetCategory;
		SpreadSheetFieldArray fields = (SpreadSheetFieldArray) Dictionary.get(category, spreadsheetId);
		if (fields != null)
			return fields;
		// A partially loaded/old configuration must not make the entire view
		// impossible to construct.  Reuse the canonical category fields as a
		// safe fallback; the normal configured array is still preferred.
		SpreadSheetFieldArray fallback = new SpreadSheetFieldArray();
		Collection<?> categoryFields = SpreadSheetUtils.getFieldsForCategory(category);
		if (categoryFields != null)
			fallback.addAll((Collection) categoryFields);
		return fallback;
	}

	private SpreadSheetFieldArray getDistributionFields() {
		SpreadSheetFieldArray fields = (SpreadSheetFieldArray) Dictionary.get(timeSpreadsheetCategory,
				getUsageDistributionSpreadsheetId(taskUsage));
		return fields != null ? fields : new SpreadSheetFieldArray();
	}

	/**
	 * @return Returns the spreadSheet.
	 */
	public SpreadSheet getSpreadSheet() {
		return spreadSheet;
	}

	public TimeSpreadSheet getTimeSpreadSheet() {
		return timeSpreadSheet;
	}

	public void forceUpdateOfTimeSpreadSheet() {
		if (!isTimeSpreadSheetReady())
			return;
		// dynamic time spreadsheets don't update themselves for a stange reason fix here
		int height=((CommonSpreadSheetModel)spreadSheet.getModel()).getRowCount()*((TimeSpreadSheetModel)timeSpreadSheet.getModel()).getRowMultiple()*GraphicConfiguration.getInstance().getRowHeight();
		timeSpreadSheet.setPreferredSize(new Dimension((int)coord.toW(coord.getEnd() - coord.getOrigin()), height/*spreadSheet.getPreferredSize().height*/));
		timeSpreadSheet.setSize(timeSpreadSheet.getPreferredSize());
		timeSpreadSheet.revalidate();
	}
	
	public void fieldArrayChanged(FieldArrayEvent e) {
		if (!isTimeSpreadSheetReady())
			return;
		int num = e.getFieldArray().size();
		int rowHeight = GraphicConfiguration.getInstance().getRowHeight() * num;
		spreadSheet.setRowHeight(rowHeight);
		forceUpdateOfTimeSpreadSheet(); // because it doesn't update automatically
	}

	public void selectionChanged(SelectionNodeEvent e) {
		if (!isTimeSpreadSheetReady())
			return;
		if (e.getSource() == spreadSheet || !(e.getSource() instanceof CommonSpreadSheet))
			return;
		CommonSpreadSheet sp = (CommonSpreadSheet) e.getSource();
		boolean taskSelection;
		if (taskSpreadsheetCategory.equals(sp.getSpreadSheetCategory())||taskAssignmentSpreadsheetCategory.equals(sp.getSpreadSheetCategory()))
			taskSelection = true;
		else if (resourceSpreadsheetCategory.equals(sp.getSpreadSheetCategory())||resourceAssignmentSpreadsheetCategory.equals(sp.getSpreadSheetCategory()))
			taskSelection = false;
		else
			return;

		ViewTransformer transformer = ((NodeCacheTransformer) cache.getVisibleNodes().getTransformer()).getTransformer();
		NodeFilter filter = transformer.getHiddenFilter();
		if (filter instanceof SelectionFilter) {
			((SelectionFilter) filter).setSelectedNodesImpl(documentFrame.getTopSpreadSheet().getSelectedNodesImpl(), taskSelection);
			forceUpdateOfTimeSpreadSheet(); // because it doesn't update automatically
		}
	}

	private boolean isTimeSpreadSheetReady() {
		return spreadSheet != null
			&& spreadSheet.getModel() instanceof CommonSpreadSheetModel
			&& timeSpreadSheet != null
			&& timeSpreadSheet.getModel() instanceof TimeSpreadSheetModel
			&& timeScrollPane != null;
	}

	public UndoController getUndoController() {
		if (showsTasks())
			return project.getUndoController();
		else
			return project.getResourcePool().getUndoController();
	}

	public void zoomIn() {
		coord.zoomIn();
	}

	public void zoomOut() {
		coord.zoomOut();
	}
	public boolean canZoomIn() {
		return coord.canZoomIn();
	}
	public boolean canZoomOut() {
		return coord.canZoomOut();
	}
	public int getScale() {
		return coord.getTimescaleManager().getCurrentScaleIndex();
	}

	public boolean hasNormalMinWidth() {
		return false;
	}

	public String getViewName() {
		return viewName;
	}

	public boolean showsTasks() {
		return taskUsage;
	}

	public boolean showsResources() {
		return !taskUsage;
	}
	public void onActivate(boolean activate) {
	}
	public boolean isPrintable() {
		return false;
	}
	public void restoreWorkspace(WorkspaceSetting w, int context) {
		Workspace ws = (Workspace) w;
		if (spreadSheet != null && ws.spreadSheet != null) {
			spreadSheet.restoreWorkspace(ws.spreadSheet, context);
		}
		if (timeSpreadSheet != null && ws.timeSpreadSheet != null) {
			timeSpreadSheet.restoreWorkspace(ws.timeSpreadSheet, context);
		}
		if (timeSpreadSheet != null && ws.selectedFieldArray != null) {
			timeSpreadSheet.setSelectedFieldArray((ArrayList) SpreadSheetFieldArray.fromIdArray(ws.selectedFieldArray));
		}
		if (timeScrollPane != null && ws.scrollPane != null) {
			timeScrollPane.restoreWorkspace(ws.scrollPane, context);
		}
		restoreDividerLocation(ws.dividerLocation);
	}
	public WorkspaceSetting createWorkspace(int context) {
		Workspace ws = new Workspace();
		if (spreadSheet != null) {
			ws.spreadSheet = spreadSheet.createWorkspace(context);
		}
		if (timeSpreadSheet != null) {
			ws.timeSpreadSheet = timeSpreadSheet.createWorkspace(context);
			ws.selectedFieldArray = SpreadSheetFieldArray.toIdArray(timeSpreadSheet.getSelectedFieldArray());
		}
		if (timeScrollPane != null) {
			ws.scrollPane = timeScrollPane.createWorkspace(context);
		}
		ws.dividerLocation = getDividerLocation();
		return ws;
	}

	public static class Workspace implements WorkspaceSetting { 
		private static final long serialVersionUID = 8588696931239304763L;
		WorkspaceSetting spreadSheet;
		WorkspaceSetting timeSpreadSheet;
		Collection selectedFieldArray;
		WorkspaceSetting scrollPane;
		int dividerLocation;
		public WorkspaceSetting getSpreadSheet() {
			return spreadSheet;
		}
		public void setSpreadSheet(WorkspaceSetting spreadSheet) {
			this.spreadSheet = spreadSheet;
		}
		public Collection getSelectedFieldArray() {
			return selectedFieldArray;
		}
		public void setSelectedFieldArray(Collection selectedFieldArray) {
			this.selectedFieldArray = selectedFieldArray;
		}
		public WorkspaceSetting getTimeSpreadSheet() {
			return timeSpreadSheet;
		}
		public void setTimeSpreadSheet(WorkspaceSetting timeSpreadSheet) {
			this.timeSpreadSheet = timeSpreadSheet;
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
		
	}

	public boolean canScrollToTask() {
		return false;
	}

	public void scrollToTask() {
	}
	
	public NodeModelCache getCache(){
		return cache;
	}
	
	

}

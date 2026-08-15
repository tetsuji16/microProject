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
		return (SpreadSheetFieldArray) Dictionary.get(category, spreadsheetId);
	}

	private SpreadSheetFieldArray getDistributionFields() {
		return (SpreadSheetFieldArray) Dictionary.get(timeSpreadsheetCategory, getUsageDistributionSpreadsheetId(taskUsage));
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


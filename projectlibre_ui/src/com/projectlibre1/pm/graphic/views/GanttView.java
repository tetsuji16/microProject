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
 * and left justified on the top left of the screen adjacent to the File menu. The 
 * logo must be at least 144 x 31 pixels. When users click on the "ProjectLibre" 
 * logo it must direct them back to http://www.projectlibre.com. 
 *******************************************************************************/
package com.projectlibre1.pm.graphic.views;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.projectlibre1.help.HelpUtil;
import com.projectlibre1.pm.graphic.fx.FxLog;
import com.projectlibre1.menu.MenuActionConstants;
import com.projectlibre1.menu.MenuManager;
import com.projectlibre1.pm.graphic.frames.DocumentFrame;
import com.projectlibre1.pm.graphic.gantt.Gantt;
import com.projectlibre1.pm.graphic.model.cache.NodeModelCache;
import com.projectlibre1.pm.graphic.model.cache.NodeModelCacheFactory;
import com.projectlibre1.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.projectlibre1.pm.graphic.model.cache.GraphicNode;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheet;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheetModel;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.projectlibre1.pm.graphic.spreadsheet.common.CommonSpreadSheetModel;
import com.projectlibre1.pm.graphic.spreadsheet.fx.FxSpreadsheetPane;
import com.projectlibre1.pm.graphic.timescale.CoordinatesConverter;
import com.projectlibre1.pm.graphic.timescale.ScaledScrollPane;
import com.projectlibre1.pm.graphic.views.synchro.Synchronizer;
import com.projectlibre1.configuration.Configuration;
import com.projectlibre1.configuration.Dictionary;
import com.projectlibre1.configuration.Settings;
import com.projectlibre1.field.FieldContext;
import com.projectlibre1.graphic.configuration.BarStyles;
import com.projectlibre1.graphic.configuration.CellStyle;
import com.projectlibre1.graphic.configuration.GraphicConfiguration;
import com.projectlibre1.graphic.configuration.SpreadSheetCategories;
import com.projectlibre1.graphic.configuration.SpreadSheetFieldArray;
import com.projectlibre1.grouping.core.model.NodeModel;
import com.projectlibre1.pm.scheduling.ScheduleEvent;
import com.projectlibre1.pm.scheduling.ScheduleEventListener;
import com.projectlibre1.pm.snapshot.Snapshottable;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.pm.task.Task;
import com.projectlibre1.pm.task.TaskSnapshot;
import com.projectlibre1.pm.time.HasStartAndEnd;
import com.projectlibre1.strings.Messages;
import com.projectlibre1.undo.UndoController;
import com.projectlibre1.workspace.WorkspaceSetting;

/**
 *
 */
public class GanttView extends SplittedView implements BaseView, ScheduleEventListener{
	/**
	 *
	 */
	private static final long serialVersionUID = 514828655690086836L;
	private static final Logger LOGGER = FxLog.logger(GanttView.class);
	protected SpreadSheet spreadSheet;
	protected FxSpreadsheetPane fxSpreadsheetPane;
	protected Gantt gantt;
    protected SortedSet baseLines=new TreeSet();

    protected ScaledScrollPane ganttScrollPane;
	protected NodeModel model;
	protected NodeModelCache cache;
	protected CoordinatesConverter coord;
	private Project project;
	DocumentFrame documentFrame;
	FieldContext fieldContext;
	CellStyle cellStyle;
	private boolean tracking = false;
	private boolean standardProgressLineEnabled = false;
	private boolean trackingProgressLineEnabled = true;
	public static final String spreadsheetCategory=taskSpreadsheetCategory;



	/**
	 * @param project
	 * @param manager
	 *
	 */
	public GanttView(DocumentFrame documentFrame, MenuManager manager, Synchronizer synchronizer) {
		super(synchronizer);
		this.documentFrame = documentFrame;
		this.project = documentFrame.getProject();
		HelpUtil.addDocHelp(this,"Gantt_Chart");
		setNeedVoidBar(true);
		//setScaled(true);
	}
	public void init(ReferenceNodeModelCache cache, NodeModel model,CoordinatesConverter coord){
		this.coord=coord;
		this.cache=NodeModelCacheFactory.getInstance().createFilteredCache((ReferenceNodeModelCache)cache,getViewName(),null);
		LOGGER.fine("init: cache=" + (cache != null) + ", coord=" + (coord != null) + ", project=" + (project != null));

		fieldContext = new FieldContext();
		fieldContext.setLeftAssociation(true);
		/*cellStyle=new CellStyle(){
			CellFormat cellProperties=new CellFormat();
			public CellFormat getCellProperties(GraphicNode node){
				cellProperties.setBold(node.isSummary());
				cellProperties.setItalic(node.isAssignment());
				//cellProperties.setBackground((node.isAssignment())?"NORMAL_LIGHT_YELLOW":"NORMAL_YELLOW");
				cellProperties.setCompositeIcon(node.isComposite());
				return cellProperties;
			}

		};*/
		super.init();
		LOGGER.fine("init: swing views created");
		updateHeight(project);
		updateSize();

		//sync the height of spreadsheet and gantt
		leftScrollPane.getViewport().addChangeListener(new ChangeListener(){
			private Dimension olddl=null;
			public void stateChanged(ChangeEvent e){
				Dimension dl=leftScrollPane.getViewport().getViewSize();
				if (dl.equals(olddl)) return;
				olddl=dl;
				syncGanttHeight();
			}
		});


		//TODO automatic scrolling to add as an option
//		spreadSheet.getRowHeader().getSelectionModel().addListSelectionListener(new ListSelectionListener(){
//			public void valueChanged(ListSelectionEvent e) {
//				if (!e.getValueIsAdjusting()&&spreadSheet.getRowHeader().getSelectedRowCount()==1){
//					List impls=spreadSheet.getSelectedNodesImpl();
//					if (impls.size()!=1) return;
//					Object impl=impls.get(0);
//					if (!(impl instanceof HasStartAndEnd)) return;
//					HasStartAndEnd interval=(HasStartAndEnd)impl;
//					gantt.scrollToTask(interval, true);
//				}
//			}
//		});


		cache.update();
		LOGGER.fine("init: cache updated rows=" + (this.cache != null ? this.cache.getSize() : -1));
		expandAllVisibleSummaries();
		LOGGER.fine("init: summaries expanded rows=" + (this.cache != null ? this.cache.getSize() : -1));
		updateSize();
		if (fxSpreadsheetPane != null) {
			fxSpreadsheetPane.refresh();
		}
		if (ganttScrollPane != null) {
			ganttScrollPane.updateTimeScaleComponentSize();
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				LOGGER.fine("init: enforcing divider location");
				setDividerLocation(Math.max(getDividerLocation(), 320));
			}
		});

		//Call this last to be sure everything is initialized
		//gantt.insertCacheData(); //useless?

	}
	public void cleanUp() {
		super.cleanUp();
		coord.removeTimeScaleListener(ganttScrollPane);
		project.removeScheduleListener(this);
		spreadSheet.cleanUp();
		if (fxSpreadsheetPane != null) {
			fxSpreadsheetPane.cleanUp();
		}
		gantt.cleanUp();
		spreadSheet=null;
		fxSpreadsheetPane=null;
		gantt=null;
	    baseLines=null;
	    ganttScrollPane=null;
		model=null;
		cache=null;
		coord=null;
		project=null;
		documentFrame=null;
		fieldContext=null;
		cellStyle=null;
	}

	public void setBarStyles(String styleName) {
		if (gantt == null)
			return;
		gantt.setBarStyles((BarStyles) Dictionary.get(BarStyles.category,styleName));
	}

	protected JScrollPane createLeftScrollPane() {
        spreadSheet = new SpreadSheet();
        spreadSheet.setName(project.getName());
		spreadSheet.setSpreadSheetCategory(spreadsheetCategory); // for columns.  Must do first
		SpreadSheetFieldArray fields=getFields();
		if (project.getFieldArray() != null) {
			fields = project.getFieldArray();
		}
		if (fields == null) {
			fields = new SpreadSheetFieldArray();
		}
		spreadSheet.setCache(cache,fields,fields.getCellStyle(),fields.getActionList());
		if (project.getFieldArray() != null)
			spreadSheet.setFieldArrayWithWidths(fields);
		((SpreadSheetModel)spreadSheet.getModel()).setFieldContext(fieldContext);
		project.removeScheduleListener(this); // in case was already attached and recreating (applet)
		project.addScheduleListener(this);
		if (project.isReadOnly())
			spreadSheet.setReadOnly(true);
		fxSpreadsheetPane = new FxSpreadsheetPane(spreadSheet);
		fxSpreadsheetPane.setFieldArray(fields);
		fxSpreadsheetPane.setRowHeight(spreadSheet.getRowHeight());
		fxSpreadsheetPane.setReadOnly(project.isReadOnly());
		return fxSpreadsheetPane.getScrollPane();
   }
   protected JScrollPane createRightScrollPane() {
		gantt=new Gantt(project,"Gantt");
		gantt.setCache(cache);
		gantt.setBarStyles((BarStyles) Dictionary.get(BarStyles.category,"standard"));
		ganttScrollPane=new ScaledScrollPane(gantt,coord,documentFrame,spreadSheet.getRowHeight());
		syncGanttHeight();
		return ganttScrollPane;
    }

   public void activateEmptyRowHeader(boolean activate){
    ganttScrollPane.activateEmptyRowHeader(activate);
   }

	//spreadsheet fields
	private static SpreadSheetFieldArray getFields() {
		return (SpreadSheetFieldArray) Dictionary.get(spreadsheetCategory,Messages.getString("Spreadsheet.Task.entry")); //TODO don't hardcode
	}
	/**
	 *
	 * @param name
	 * @return old field array
	 */
	public ArrayList setColumns(String name){
		ArrayList old = spreadSheet.getFieldArray();
		setColumns((ArrayList) Dictionary.get(spreadsheetCategory,Messages.getString(name)));
		return old;
	}
	public void setColumns(ArrayList fields){
		spreadSheet.setFieldArray(fields);
		if (fxSpreadsheetPane != null) {
			fxSpreadsheetPane.setFieldArray(fields);
		}
	}
	/**
	 * @return Returns the spreadSheet.
	 */
	public SpreadSheet getSpreadSheet() {
		return spreadSheet;
	}



	public void updateHeight(Integer snapshotId, boolean add){
		if (add)
			baseLines.add(snapshotId);
		else baseLines.remove(snapshotId);
		int num=(baseLines.size()==0)?0:(((Integer)baseLines.last()).intValue()+1);
		int rowHeight=GraphicConfiguration.getInstance().getRowHeight()
				+num*GraphicConfiguration.getInstance().getBaselineHeight();
		LOGGER.fine("updateHeight(snapshot): snapshot=" + snapshotId + ", add=" + add + ", rowHeight=" + rowHeight);
		spreadSheet.setRowHeight(rowHeight);
		if (fxSpreadsheetPane != null) {
			fxSpreadsheetPane.setRowHeight(rowHeight);
		}
		gantt.setRowHeight(rowHeight);
		syncGanttHeight();
	}

	public void updateHeight(Project project){
	    baseLines.clear();
	    int rowHeight=project.getRowHeight(baseLines);
		LOGGER.fine("updateHeight(project): rowHeight=" + rowHeight + ", baselines=" + baseLines.size());
//        for (Iterator i=project.getTaskOutlineIterator();i.hasNext();){
//            Task task=(Task)i.next();
//            int current=Snapshottable.CURRENT.intValue();
//            for (int s=0;s<Settings.numGanttBaselines();s++){
//                if (s==current) continue;
//                TaskSnapshot snapshot=(TaskSnapshot)task.getSnapshot(new Integer(s));
//                if (snapshot!=null) baseLines.add(new Integer(s));
//            }
//        }
//		int num=(baseLines.size()==0)?0:(((Integer)baseLines.last()).intValue()+1);
//		int rowHeight=GraphicConfiguration.getInstance().getRowHeight()
//				+num*GraphicConfiguration.getInstance().getBaselineHeight();
		spreadSheet.setRowHeight(rowHeight);
		if (fxSpreadsheetPane != null) {
			fxSpreadsheetPane.setRowHeight(rowHeight);
		}
		gantt.setRowHeight(rowHeight);
		syncGanttHeight();
	}

	public void scheduleChanged(ScheduleEvent evt) {
		if (evt.getType() == ScheduleEvent.SCHEDULE) {
			//gantt.updateSize(); //done throught cache
		} else if (evt.getType() == ScheduleEvent.ACTUAL) {
		} else if (evt.getType() == ScheduleEvent.BASELINE){
		    updateHeight(evt.getSnapshot(),evt.isSaveSnapshot());
		    //Warning: listeners order is important.
		    //This one must be before GanttModel one which calls updateAll after the height is setted
		}
	}

	public void updateSize(){
		LOGGER.fine("updateSize: syncing gantt height and FX canvas");
		syncGanttHeight();
		gantt.updateSize();
	}
	public UndoController getUndoController() {
		return project.getUndoController();
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
		return true;
	}
	public String getViewName() {
		return MenuActionConstants.ACTION_GANTT;
	}
	public boolean showsTasks() {
		return true;
	}
	public boolean showsResources() {
		return false;
	}
	public void onActivate(boolean activate) {
	}



	public Gantt getGantt() {
		return gantt;
	}
	public boolean isPrintable() {
		return true;
	}

	public void reinitialize() { // applet
		createLeftScrollPane();
	}
	public void restoreWorkspace(WorkspaceSetting w, int context) {
		Workspace ws = (Workspace) w;
		spreadSheet.restoreWorkspace(ws.spreadSheet, context);
		if (fxSpreadsheetPane != null) {
			fxSpreadsheetPane.setFieldArray(spreadSheet.getFieldArray());
			fxSpreadsheetPane.setRowHeight(spreadSheet.getRowHeight());
		}
		gantt.setProgressLineEnabled(ws.progressLineEnabled);
		if (tracking)
			trackingProgressLineEnabled = ws.progressLineEnabled;
		else
			standardProgressLineEnabled = ws.progressLineEnabled;
		ganttScrollPane.restoreWorkspace(ws.scrollPane, context);
		int dividerLocation = Math.max(ws.dividerLocation, 320);
		LOGGER.fine("restoreWorkspace: divider=" + dividerLocation + ", progressLine=" + ws.progressLineEnabled);
		setDividerLocation(dividerLocation);
	}
	public WorkspaceSetting createWorkspace(int context) {
		Workspace ws = new Workspace();
		ws.spreadSheet = spreadSheet.createWorkspace(context);
		ws.scrollPane = ganttScrollPane.createWorkspace(context);
		ws.progressLineEnabled = gantt.isProgressLineEnabled();
		ws.dividerLocation = getDividerLocation();
		return ws;
	}

	private void syncGanttHeight() {
		if (gantt == null || fxSpreadsheetPane == null) {
			return;
		}
		int rowHeight = spreadSheet.getRowHeight();
		int rowCount = 0;
		if (spreadSheet.getModel() instanceof CommonSpreadSheetModel) {
			rowCount = ((CommonSpreadSheetModel) spreadSheet.getModel()).getRowCount();
		}
		int height = Math.max(rowHeight, rowCount * rowHeight);
		Dimension ganttSize = new Dimension((int) Math.ceil(coord == null ? 1.0d : coord.getWidth()), height);
		LOGGER.fine("syncGanttHeight: width=" + ganttSize.width + ", height=" + ganttSize.height + ", rows=" + rowCount);
		gantt.setPreferredSize(ganttSize);
		gantt.setSize(ganttSize);
		if (ganttScrollPane != null) {
			ganttScrollPane.getViewport().revalidate();
			ganttScrollPane.getViewport().setViewSize(ganttSize);
		}
	}

	private void expandAllVisibleSummaries() {
		if (cache == null) {
			return;
		}
		for (int pass = 0; pass < 16; pass++) {
			List collapsed = new ArrayList();
			for (Iterator i = cache.getIterator(); i.hasNext();) {
				GraphicNode node = (GraphicNode) i.next();
				if (node != null && node.isComposite() && node.isCollapsed()) {
					collapsed.add(node);
				}
			}
			if (collapsed.isEmpty()) {
				break;
			}
			cache.expandNodes(collapsed, true);
		}
	}

	public static class Workspace implements WorkspaceSetting {
		private static final long serialVersionUID = -407561451956813994L;
		WorkspaceSetting spreadSheet;
		WorkspaceSetting scrollPane;
		boolean progressLineEnabled;
		int dividerLocation;
		public WorkspaceSetting getSpreadSheet() {
			return spreadSheet;
		}
		public void setSpreadSheet(WorkspaceSetting spreadSheet) {
			this.spreadSheet = spreadSheet;
		}
		public WorkspaceSetting getScrollPane() {
			return scrollPane;
		}
		public void setScrollPane(WorkspaceSetting scrollPane) {
			this.scrollPane = scrollPane;
		}
		public boolean isProgressLineEnabled() {
			return progressLineEnabled;
		}
		public void setProgressLineEnabled(boolean progressLineEnabled) {
			this.progressLineEnabled = progressLineEnabled;
		}
		public int getDividerLocation() {
			return dividerLocation;
		}
		public void setDividerLocation(int dividerLocation) {
			this.dividerLocation = dividerLocation;
		}
	}

	public boolean canScrollToTask() {
		return true;
	}
	public void scrollToTask() {
		List impls=spreadSheet.getSelectedNodesImpl();
		if (impls.size()==0) return;
		Object impl=impls.get(0);
		if (!(impl instanceof HasStartAndEnd)) return;
		HasStartAndEnd interval=(HasStartAndEnd)impl;
		gantt.scrollToTask(interval, false);
	}

	public NodeModelCache getCache(){
		return cache;
	}
	public boolean isTracking() {
		return tracking;
	}
	public void setTracking(boolean tracking) {
		if (gantt != null) {
			if (this.tracking)
				trackingProgressLineEnabled = gantt.isProgressLineEnabled();
			else
				standardProgressLineEnabled = gantt.isProgressLineEnabled();
		}
		this.tracking = tracking;
		if (gantt != null)
			gantt.setProgressLineEnabled(tracking ? trackingProgressLineEnabled : standardProgressLineEnabled);
		HelpUtil.addDocHelp(this,tracking ? "Tracking_Gantt_Chart":"Gantt_Chart");
	}

}

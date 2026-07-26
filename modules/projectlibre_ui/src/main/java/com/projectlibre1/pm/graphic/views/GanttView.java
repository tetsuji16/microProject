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
package com.projectlibre1.pm.graphic.views;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.projectlibre1.help.HelpUtil;
import com.projectlibre1.menu.MenuActionConstants;
import com.projectlibre1.menu.MenuManager;
import com.projectlibre1.pm.graphic.frames.DocumentFrame;
import com.projectlibre1.pm.graphic.gantt.Gantt;
import com.projectlibre1.pm.graphic.model.cache.NodeModelCache;
import com.projectlibre1.pm.graphic.model.cache.NodeModelCacheFactory;
import com.projectlibre1.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheet;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheetModel;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.projectlibre1.pm.graphic.timescale.CoordinatesConverter;
import com.projectlibre1.pm.graphic.timescale.ScaledScrollPane;
import com.projectlibre1.pm.graphic.views.synchro.ScrollPaneSynchronizer;
import com.projectlibre1.pm.graphic.views.synchro.Synchronizer;
import com.projectlibre1.configuration.Dictionary;
import com.projectlibre1.field.FieldContext;
import com.projectlibre1.graphic.configuration.BarStyles;
import com.projectlibre1.graphic.configuration.GraphicConfiguration;
import com.projectlibre1.grouping.core.model.NodeModel;
import com.projectlibre1.pm.scheduling.ScheduleEvent;
import com.projectlibre1.pm.scheduling.ScheduleEventListener;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.pm.time.HasStartAndEnd;
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
	private static final String DEFAULT_GANTT_BAR_STYLE = "standard";
	public static final String ANNOTATION_FIELD_RESOURCE_NAMES = "Field.resourceNames";
	public static final String ANNOTATION_FIELD_TASK_NAME = "Field.name";
	private String currentBarStyleName = DEFAULT_GANTT_BAR_STYLE;
	protected SpreadSheet spreadSheet;
	protected Gantt gantt;
    protected SortedSet<Integer> baseLines = new TreeSet<>();

    protected ScaledScrollPane ganttScrollPane;
	protected NodeModel model;
	protected NodeModelCache cache;
	protected CoordinatesConverter coord;
	private Project project;
	DocumentFrame documentFrame;
	FieldContext fieldContext;
	private boolean tracking = false;
	private boolean standardProgressLineEnabled = false;
	private boolean trackingProgressLineEnabled = true;
	private boolean spreadsheetGridVisible = true;
	private String currentAnnotationFieldId = ANNOTATION_FIELD_RESOURCE_NAMES;
	private ChangeListener spreadsheetViewportListener;
	private boolean synchronizingRowGeometry;
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
		this.coord = coord;
		this.cache = NodeModelCacheFactory.getInstance().createFilteredCache((ReferenceNodeModelCache) cache, getViewName(), null);

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
		setSpreadsheetGridVisible(spreadsheetGridVisible);
		updateHeight(project);
		updateSize();

		//sync the height of spreadsheet and gantt
		installSpreadsheetViewportListener();

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

		//Call this last to be sure everything is initialized
		//gantt.insertCacheData(); //useless?

	}
	public void cleanUp() {
		removeSpreadsheetViewportListener();
		if (coord != null && ganttScrollPane != null) {
			coord.removeTimeScaleListener(ganttScrollPane);
		}
		if (project != null) {
			project.removeScheduleListener(this);
		}
		cleanupContentViews();
		super.cleanUp();
		spreadSheet=null;
		gantt=null;
	    baseLines=null;
	    ganttScrollPane=null;
		model=null;
		cache=null;
		coord=null;
		project=null;
		documentFrame=null;
		fieldContext=null;
	}

	public void setBarStyles(String styleName) {
		if (gantt == null)
			return;
		currentBarStyleName = styleName == null ? DEFAULT_GANTT_BAR_STYLE : styleName;
		BarStyles styles = (BarStyles) Dictionary.get(BarStyles.category, currentBarStyleName);
		if (styles == null) {
			currentBarStyleName = DEFAULT_GANTT_BAR_STYLE;
			styles = (BarStyles) Dictionary.get(BarStyles.category, DEFAULT_GANTT_BAR_STYLE);
		}
		gantt.setBarStyles(styles);
    }

	protected JScrollPane createLeftScrollPane() {
        spreadSheet = new SpreadSheet();
        spreadSheet.setName(project.getName());
		spreadSheet.setSpreadSheetCategory(spreadsheetCategory); // for columns.  Must do first
		var fields = SpreadsheetViewSupport.resolveTaskFields(project.getFieldArray());
		spreadSheet.setCache(cache, fields, fields.getCellStyle(), fields.getActionList());
		if (project.getFieldArray() != null) {
			spreadSheet.setFieldArrayWithWidths(fields);
		}
		((SpreadSheetModel) spreadSheet.getModel()).setFieldContext(fieldContext);
		project.removeScheduleListener(this); // in case was already attached and recreating (applet)
		project.addScheduleListener(this);
		if (project.isReadOnly()) {
			spreadSheet.setReadOnly(true);
		}
		applySpreadsheetGridStyle();
		return SpreadSheetUtils.makeSpreadsheetScrollPane(spreadSheet);
   }
   protected JScrollPane createRightScrollPane() {
		gantt = new Gantt(project, "Gantt");
		gantt.setCache(cache);
		gantt.setAnnotationFieldId(currentAnnotationFieldId);
		gantt.setTrackingView(tracking);
		gantt.setBarStyles((BarStyles) Dictionary.get(BarStyles.category, DEFAULT_GANTT_BAR_STYLE));
		ganttScrollPane = new ScaledScrollPane(gantt, coord, documentFrame, spreadSheet.getRowHeight());
		ganttScrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
		applySpreadsheetGridStyle();
		return ganttScrollPane;
    }

   public void activateEmptyRowHeader(boolean activate){
    ganttScrollPane.activateEmptyRowHeader(activate);
   }

	/**
	 *
	 * @param name
	 * @return old field array
	 */
	public ArrayList setColumns(String name){
		ArrayList old = spreadSheet.getFieldArray();
		setColumns(SpreadsheetViewSupport.getTaskFields(name));
		return old;
	}
	public void setColumns(ArrayList fields){
		spreadSheet.setFieldArray(fields);
	}
	/**
	 * @return Returns the spreadSheet.
	 */
	public SpreadSheet getSpreadSheet() {
		return spreadSheet;
	}



	public void updateHeight(Integer snapshotId, boolean add){
		if (snapshotId == null || baseLines == null) {
			return;
		}
		if (add) {
			baseLines.add(snapshotId);
		} else {
			baseLines.remove(snapshotId);
		}
		applyRowHeight(TaskGanttSyncSupport.calculateRowHeight(baseLines,
				GraphicConfiguration.getInstance().getRowHeight(),
				GraphicConfiguration.getInstance().getBaselineHeight()));
	}

	public void updateHeight(Project project){
		if (project == null || baseLines == null) {
			return;
		}
	    baseLines.clear();
	    int rowHeight = project.getRowHeight(baseLines);
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
		applyRowHeight(rowHeight);
	}

	public void scheduleChanged(ScheduleEvent evt) {
		var eventType = evt.getType();
		if (eventType == ScheduleEvent.SCHEDULE) {
			//gantt.updateSize(); //done throught cache
			return;
		}
		if (eventType == ScheduleEvent.ACTUAL) {
			return;
		}
		if (eventType == ScheduleEvent.BASELINE) {
			updateHeight(evt.getSnapshot(), evt.isSaveSnapshot());
			//Warning: listeners order is important.
			//This one must be before GanttModel one which calls updateAll after the height is setted
		}
	}

	public void updateSize(){
		if (gantt != null) {
			gantt.updateSize();
		}
	}
	public UndoController getUndoController() {
		return project.getUndoController();
	}
	public void zoomIn() {
		if (gantt == null || !ScrollPaneSynchronizer.zoomIn(gantt)) {
			coord.zoomIn();
		}
	}
	public void zoomOut() {
		if (gantt == null || !ScrollPaneSynchronizer.zoomOut(gantt)) {
			coord.zoomOut();
		}
	}
	public boolean isProgressLineEnabled() {
		return gantt != null && gantt.isProgressLineEnabled();
	}
	public void setProgressLineEnabled(boolean progressLineEnabled) {
		if (tracking)
			trackingProgressLineEnabled = progressLineEnabled;
		else
			standardProgressLineEnabled = progressLineEnabled;
		if (gantt != null)
			gantt.setProgressLineEnabled(progressLineEnabled);
	}
	public String getCurrentAnnotationFieldId() {
		return currentAnnotationFieldId;
	}
	public void setCurrentAnnotationFieldId(String fieldId) {
		currentAnnotationFieldId = fieldId == null ? ANNOTATION_FIELD_RESOURCE_NAMES : fieldId;
		if (gantt != null) {
			gantt.setAnnotationFieldId(currentAnnotationFieldId);
			gantt.getModel().updateAll(true);
		}
	}
	public boolean isResourceNameAnnotationSelected() {
		return ANNOTATION_FIELD_RESOURCE_NAMES.equals(getCurrentAnnotationFieldId());
	}
	public boolean isTaskNameAnnotationSelected() {
		return ANNOTATION_FIELD_TASK_NAME.equals(getCurrentAnnotationFieldId());
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
	public int getScaleCount() {
		return coord.getTimescaleManager().getScaleCount();
	}
	public void setScale(int scaleIndex) {
		if (coord == null)
			return;
		int boundedScale = ScrollPaneSynchronizer.clampTargetScaleIndex(scaleIndex, getScaleCount());
		if (gantt != null && ScrollPaneSynchronizer.zoomToScale(gantt, boundedScale)) {
			return;
		}
		while (getScale() < boundedScale && canZoomOut()) {
			zoomOut();
		}
		while (getScale() > boundedScale && canZoomIn()) {
			zoomIn();
		}
	}
	public String getCurrentBarStyleName() {
		return currentBarStyleName;
	}
	public boolean isSpreadsheetGridVisible() {
		return spreadsheetGridVisible;
	}
	public void setSpreadsheetGridVisible(boolean visible) {
		spreadsheetGridVisible = visible;
		if (gantt != null) {
			gantt.setGridLinesVisible(visible);
		}
		applySpreadsheetGridStyle();
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
		removeSpreadsheetViewportListener();
		if (coord != null && ganttScrollPane != null) {
			coord.removeTimeScaleListener(ganttScrollPane);
		}
		cleanupContentViews();
		super.reinitialize();
		setSpreadsheetGridVisible(spreadsheetGridVisible);
		updateHeight(project);
		updateSize();
		installSpreadsheetViewportListener();
		synchronizeGanttHeightWithSpreadsheet(leftScrollPane.getViewport().getViewSize());
	}
	public void restoreWorkspace(WorkspaceSetting w, int context) {
		Workspace ws = (Workspace) w;
		spreadSheet.restoreWorkspace(ws.spreadSheet, context);
		setCurrentAnnotationFieldId(ws.annotationFieldId);
		gantt.setProgressLineEnabled(ws.progressLineEnabled);
		if (tracking)
			trackingProgressLineEnabled = ws.progressLineEnabled;
		else
			standardProgressLineEnabled = ws.progressLineEnabled;
		ganttScrollPane.restoreWorkspace(ws.scrollPane, context);
		setDividerLocation(ws.dividerLocation);
	}
	public WorkspaceSetting createWorkspace(int context) {
		Workspace ws = new Workspace();
		ws.spreadSheet = spreadSheet.createWorkspace(context);
		ws.scrollPane = ganttScrollPane.createWorkspace(context);
		ws.progressLineEnabled = gantt.isProgressLineEnabled();
		ws.annotationFieldId = currentAnnotationFieldId;
		ws.dividerLocation = getDividerLocation();
		return ws;
	}

	public static class Workspace implements WorkspaceSetting {
		private static final long serialVersionUID = -407561451956813994L;
		WorkspaceSetting spreadSheet;
		WorkspaceSetting scrollPane;
		boolean progressLineEnabled;
		String annotationFieldId = ANNOTATION_FIELD_RESOURCE_NAMES;
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
		public String getAnnotationFieldId() {
			return annotationFieldId;
		}
		public void setAnnotationFieldId(String annotationFieldId) {
			this.annotationFieldId = annotationFieldId;
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
		List impls = spreadSheet.getSelectedNodesImpl();
		if (impls.isEmpty() || !(impls.get(0) instanceof HasStartAndEnd interval)) {
			return;
		}
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
		if (gantt != null) {
			gantt.setTrackingView(tracking);
			gantt.setProgressLineEnabled(tracking ? trackingProgressLineEnabled : standardProgressLineEnabled);
		}
		HelpUtil.addDocHelp(this,tracking ? "Tracking_Gantt_Chart":"Gantt_Chart");
	}

	private void cleanupContentViews() {
		SpreadsheetViewSupport.cleanup(spreadSheet);
		if (gantt != null) {
			gantt.cleanUp();
		}
	}

	private void applyRowHeight(int rowHeight) {
		if (synchronizingRowGeometry) {
			return;
		}
		synchronizingRowGeometry = true;
		try {
			TaskGanttSyncSupport.applyRowHeight(spreadSheet, gantt, rowHeight);
			if (leftScrollPane != null) {
				synchronizeGanttHeightWithSpreadsheet(leftScrollPane.getViewport().getViewSize());
			}
		} finally {
			synchronizingRowGeometry = false;
		}
	}

	private void installSpreadsheetViewportListener() {
		if (spreadsheetViewportListener == null && leftScrollPane != null) {
			spreadsheetViewportListener = new ChangeListener() {
				private Dimension oldSize;

				@Override
				public void stateChanged(ChangeEvent e) {
					if (synchronizingRowGeometry || leftScrollPane == null) {
						return;
					}
					Dimension size = leftScrollPane.getViewport().getViewSize();
					if (oldSize != null && oldSize.height == size.height) {
						return;
					}
					oldSize = new Dimension(size);
					synchronizeGanttHeightWithSpreadsheet(size);
				}
			};
			leftScrollPane.getViewport().addChangeListener(spreadsheetViewportListener);
		}
	}

	private void removeSpreadsheetViewportListener() {
		if (spreadsheetViewportListener != null && leftScrollPane != null
				&& leftScrollPane.getViewport() != null) {
			leftScrollPane.getViewport().removeChangeListener(spreadsheetViewportListener);
		}
		spreadsheetViewportListener = null;
	}

	private void applySpreadsheetGridStyle() {
		TaskGanttSyncSupport.applySpreadsheetGridStyle(spreadSheet, gantt, spreadsheetGridVisible, getSpreadsheetGridLineColor());
	}

	static void applySpreadsheetGridStyle(SpreadSheet spreadSheet, Gantt gantt, boolean spreadsheetGridVisible, Color gridLineColor) {
		TaskGanttSyncSupport.applySpreadsheetGridStyle(spreadSheet, gantt, spreadsheetGridVisible, gridLineColor);
	}

	private Color getSpreadsheetGridLineColor() {
		return TaskGanttSyncSupport.resolveGridLineColor(gantt);
	}

	private void synchronizeGanttHeightWithSpreadsheet(Dimension spreadsheetSize) {
		TaskGanttSyncSupport.synchronizeGanttHeightWithSpreadsheet(rightScrollPane, spreadsheetSize);
	}

}


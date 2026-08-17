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

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionListener;

import com.microproject.help.HelpUtil;
import com.microproject.menu.MenuActionConstants;
import com.microproject.menu.MenuManager;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.graphic.timescale.ScaledScrollPane;
import com.microproject.pm.graphic.views.synchro.ScrollPaneSynchronizer;
import com.microproject.pm.graphic.views.synchro.Synchronizer;
import com.microproject.configuration.Dictionary;
import com.microproject.field.FieldContext;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.pm.scheduling.ScheduleEvent;
import com.microproject.pm.scheduling.ScheduleEventListener;
import com.microproject.pm.task.Project;
import com.microproject.pm.time.HasStartAndEnd;
import com.microproject.undo.UndoController;
import com.microproject.workspace.WorkspaceSetting;

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
	private ListSelectionListener spreadsheetSelectionListener;
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
		installSpreadsheetSelectionListener();
		installGanttBarSelectionListener();

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
		removeSpreadsheetSelectionListener();
		if (gantt != null) {
			gantt.setBarSelectionListener(null);
		}
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
		installSpreadsheetSelectionListener();
		installGanttBarSelectionListener();
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
		restoreDividerLocation(ws.dividerLocation);
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

	/**
	 * Keeps the Gantt chart's row highlight in sync with the task table
	 * selection: selecting tasks in the table highlights their complete
	 * calendar row in the chart (issue #179).
	 */
	private void installSpreadsheetSelectionListener() {
		if (spreadsheetSelectionListener == null && spreadSheet != null) {
			spreadsheetSelectionListener = e -> {
				if (e.getValueIsAdjusting()) {
					return;
				}
				updateGanttHighlightedRows();
			};
			spreadSheet.getSelectionModel().addListSelectionListener(spreadsheetSelectionListener);
		}
	}

	private void removeSpreadsheetSelectionListener() {
		if (spreadsheetSelectionListener != null && spreadSheet != null
				&& spreadSheet.getSelectionModel() != null) {
			spreadSheet.getSelectionModel().removeListSelectionListener(spreadsheetSelectionListener);
		}
		spreadsheetSelectionListener = null;
	}

	private void updateGanttHighlightedRows() {
		if (gantt == null || spreadSheet == null) {
			return;
		}
		int[] rows = spreadSheet.getSelectedRows();
		if (rows == null || rows.length == 0) {
			gantt.setHighlightedRows(Collections.emptySet());
			return;
		}
		Set<Integer> highlighted = new HashSet<>(rows.length);
		for (int row : rows) {
			if (row >= 0) {
				highlighted.add(row);
			}
		}
		gantt.setHighlightedRows(highlighted);
	}

	/**
	 * Selecting a task bar directly in the chart also selects its row in the
	 * task table, so both panes highlight the same tasks (issue #179).
	 */
	private void installGanttBarSelectionListener() {
		if (gantt == null) {
			return;
		}
		gantt.setBarSelectionListener(this::selectSpreadsheetRowForGraphicNode);
	}

	private void selectSpreadsheetRowForGraphicNode(GraphicNode node) {
		if (node == null || spreadSheet == null
				|| !(spreadSheet.getModel() instanceof com.microproject.pm.graphic.spreadsheet.SpreadSheetModel model)) {
			return;
		}
		int row = model.findGraphicNodeRow(node);
		if (row < 0 || row >= spreadSheet.getRowCount()) {
			return;
		}
		int column = spreadSheet.getSelectedColumn();
		if (column < 0) {
			column = 0;
		}
		if (column >= spreadSheet.getColumnCount()) {
			column = Math.max(0, spreadSheet.getColumnCount() - 1);
		}
		spreadSheet.changeSelection(row, column, false, false);
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


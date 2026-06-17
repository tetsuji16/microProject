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

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

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
import com.projectlibre1.pm.graphic.views.synchro.Synchronizer;
import com.projectlibre1.configuration.Dictionary;
import com.projectlibre1.field.FieldContext;
import com.projectlibre1.graphic.configuration.BarStyles;
import com.projectlibre1.graphic.configuration.CellStyle;
import com.projectlibre1.graphic.configuration.GraphicConfiguration;
import com.projectlibre1.graphic.configuration.SpreadSheetFieldArray;
import com.projectlibre1.grouping.core.model.NodeModel;
import com.projectlibre1.pm.scheduling.ScheduleEvent;
import com.projectlibre1.pm.scheduling.ScheduleEventListener;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.pm.time.HasStartAndEnd;
import com.projectlibre1.strings.Messages;
import com.projectlibre1.undo.UndoController;
import com.projectlibre1.workspace.WorkspaceSetting;
import com.projectlibre1.util.FlatUiSupport;

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
	CellStyle cellStyle;
	private boolean tracking = false;
	private boolean standardProgressLineEnabled = false;
	private boolean trackingProgressLineEnabled = true;
	private boolean spreadsheetGridVisible = true;
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
		setSpreadsheetGridVisible(true);
		updateHeight(project);
		updateSize();

		//sync the height of spreadsheet and gantt
		leftScrollPane.getViewport().addChangeListener(new ChangeListener(){
			private Dimension olddl = null;

			public void stateChanged(ChangeEvent e){
				Dimension dl = leftScrollPane.getViewport().getViewSize();
				if (olddl != null && dl.height == olddl.height) {
					return;
				}
				olddl = dl;
				synchronizeGanttHeightWithSpreadsheet(dl);
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

		//Call this last to be sure everything is initialized
		//gantt.insertCacheData(); //useless?

	}
	public void cleanUp() {
		super.cleanUp();
		if (coord != null && ganttScrollPane != null) {
			coord.removeTimeScaleListener(ganttScrollPane);
		}
		if (project != null) {
			project.removeScheduleListener(this);
		}
		if (spreadSheet != null) {
			spreadSheet.cleanUp();
		}
		if (gantt != null) {
			gantt.cleanUp();
		}
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
		cellStyle=null;
	}

	public void setBarStyles(String styleName) {
		if (gantt == null)
			return;
		currentBarStyleName = styleName == null ? DEFAULT_GANTT_BAR_STYLE : styleName;
		gantt.setBarStyles((BarStyles) Dictionary.get(BarStyles.category, styleName));
    }

	protected JScrollPane createLeftScrollPane() {
        spreadSheet = new SpreadSheet();
        spreadSheet.setName(project.getName());
		spreadSheet.setSpreadSheetCategory(spreadsheetCategory); // for columns.  Must do first
		var fields = resolveFieldArray();
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
		gantt.setBarStyles((BarStyles) Dictionary.get(BarStyles.category, DEFAULT_GANTT_BAR_STYLE));
		ganttScrollPane = new ScaledScrollPane(gantt, coord, documentFrame, spreadSheet.getRowHeight());
		ganttScrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
		applySpreadsheetGridStyle();
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
		setColumns((ArrayList) Dictionary.get(spreadsheetCategory, Messages.getString(name)));
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
		applyRowHeight(calculateRowHeight());
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
		coord.zoomIn();
	}
	public void zoomOut() {
		coord.zoomOut();
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
		if (gantt == null || gantt.getBarStyles() == null)
			return ANNOTATION_FIELD_RESOURCE_NAMES;
		return getAnnotationFieldId(gantt.getBarStyles());
	}
	public void setCurrentAnnotationFieldId(String fieldId) {
		if (gantt == null || gantt.getBarStyles() == null)
			return;
		applyAnnotationField(gantt.getBarStyles(), fieldId);
		gantt.getModel().updateAll(true);
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
		int boundedScale = Math.max(0, Math.min(scaleIndex, getScaleCount() - 1));
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
		createLeftScrollPane();
	}
	public void restoreWorkspace(WorkspaceSetting w, int context) {
		Workspace ws = (Workspace) w;
		spreadSheet.restoreWorkspace(ws.spreadSheet, context);
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
		ws.dividerLocation = getDividerLocation();
		return ws;
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
		if (gantt != null)
			gantt.setProgressLineEnabled(tracking ? trackingProgressLineEnabled : standardProgressLineEnabled);
		HelpUtil.addDocHelp(this,tracking ? "Tracking_Gantt_Chart":"Gantt_Chart");
	}

	private SpreadSheetFieldArray resolveFieldArray() {
		var fields = getFields();
		if (project.getFieldArray() != null) {
			fields = project.getFieldArray();
		}
		if (fields == null) {
			fields = new SpreadSheetFieldArray();
		}
		return fields;
	}

	private int calculateRowHeight() {
		var baselineCount = baseLines.isEmpty() ? 0 : baseLines.last().intValue() + 1;
		return GraphicConfiguration.getInstance().getRowHeight()
				+ baselineCount * GraphicConfiguration.getInstance().getBaselineHeight();
	}

	private void applyRowHeight(int rowHeight) {
		if (spreadSheet != null) {
			spreadSheet.setRowHeight(rowHeight);
		}
		if (gantt != null) {
			gantt.setRowHeight(rowHeight);
			gantt.synchronizeViewportSize();
		}
	}

	private void applySpreadsheetGridStyle() {
		Color gridLineColor = getSpreadsheetGridLineColor();
		if (spreadSheet != null) {
			spreadSheet.setGridColor(gridLineColor);
			spreadSheet.setShowHorizontalLines(spreadsheetGridVisible);
			spreadSheet.setShowVerticalLines(spreadsheetGridVisible);
			if (spreadSheet.getRowHeader() != null) {
				spreadSheet.getRowHeader().setGridColor(gridLineColor);
				spreadSheet.getRowHeader().repaint();
			}
			spreadSheet.repaint();
		}
		if (gantt != null) {
			gantt.setGridLinesVisible(spreadsheetGridVisible);
		}
	}

	private Color getSpreadsheetGridLineColor() {
		if (gantt != null) {
			return gantt.getGridLineColor();
		}
		return FlatUiSupport.tableGridColor();
	}

	private void synchronizeGanttHeightWithSpreadsheet(Dimension spreadsheetSize) {
		if (spreadsheetSize == null || rightScrollPane == null || rightScrollPane.getViewport() == null)
			return;
		JViewport viewport = rightScrollPane.getViewport();
		if (!(viewport.getView() instanceof Gantt ganttView))
			return;
		int height = Math.min(spreadsheetSize.height, ganttView.getScrollableHeight(viewport.getExtentSize().height));
		ganttView.setPreferredSize(new Dimension(viewport.getViewSize().width, height));
		ganttView.clampViewportPosition(viewport, height);
		viewport.revalidate();
	}

	private String getAnnotationFieldId(BarStyles barStyles) {
		Object firstAnnotationField = CollectionUtils.find(barStyles.getRows(), new Predicate() {
			public boolean evaluate(Object object) {
				return object instanceof com.projectlibre1.graphic.configuration.BarStyle
					&& ((com.projectlibre1.graphic.configuration.BarStyle) object).isAnnotation();
			}
		});
		if (!(firstAnnotationField instanceof com.projectlibre1.graphic.configuration.BarStyle))
			return ANNOTATION_FIELD_RESOURCE_NAMES;
		String fieldId = ((com.projectlibre1.graphic.configuration.BarStyle) firstAnnotationField).getBarFormat().getFieldId();
		return fieldId == null ? ANNOTATION_FIELD_RESOURCE_NAMES : fieldId;
	}

	private void applyAnnotationField(BarStyles barStyles, final String fieldId) {
		CollectionUtils.forAllDo(barStyles.getRows(), new Closure() {
			public void execute(Object object) {
				com.projectlibre1.graphic.configuration.BarStyle style =
					(com.projectlibre1.graphic.configuration.BarStyle) object;
				if (style.isAnnotation())
					style.getBarFormat().setFieldId(fieldId);
			}
		});
	}

}

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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.JViewport;
import javax.swing.table.TableColumnModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;

import com.microproject.ui.help.HelpUtil;
import com.microproject.ui.menu.MenuActionConstants;
import com.microproject.ui.menu.MenuManager;
import com.microproject.ui.frame.DocumentFrame;
import com.microproject.ui.view.gantt.Gantt;
import com.microproject.ui.view.model.cache.GraphicNode;
import com.microproject.ui.view.model.cache.NodeModelCache;
import com.microproject.ui.view.model.cache.ViewNodeModelCache;
import com.microproject.ui.view.gantt.GanttBarColorScheme;
import com.microproject.model.task.Task;
import com.microproject.model.task.TaskPathAnalyzer;
import com.microproject.ui.view.model.cache.NodeModelCacheFactory;
import com.microproject.ui.view.model.cache.ReferenceNodeModelCache;
import com.microproject.ui.view.spreadsheet.SpreadSheet;
import com.microproject.ui.view.spreadsheet.SpreadSheetColumnModel;
import com.microproject.ui.view.spreadsheet.SpreadSheetModel;
import com.microproject.ui.view.spreadsheet.SpreadSheetUtils;
import com.microproject.ui.view.timescale.CoordinatesConverter;
import com.microproject.ui.view.timescale.ScaledScrollPane;
import com.microproject.timescale.TimeScale;
import com.microproject.ui.view.views.synchro.ScrollPaneSynchronizer;
import com.microproject.ui.view.views.synchro.Synchronizer;
import com.microproject.configuration.Dictionary;
import com.microproject.field.FieldContext;
import com.microproject.view.configuration.BarStyles;
import com.microproject.view.configuration.GraphicConfiguration;
import com.microproject.node.model.NodeModel;
import com.microproject.schedule.ScheduleEvent;
import com.microproject.schedule.ScheduleEventListener;
import com.microproject.model.task.Project;
import com.microproject.preference.GlobalPreferences;
import com.microproject.model.time.HasStartAndEnd;
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
	private boolean taskSheet = false;
	private int sheetSavedDividerSize = 0;
	private boolean standardProgressLineEnabled = false;
	private boolean trackingProgressLineEnabled = true;
	private boolean spreadsheetGridVisible = Gantt.DEFAULT_GRID_LINES_VISIBLE;
	private String currentAnnotationFieldId = ANNOTATION_FIELD_RESOURCE_NAMES;
	private ChangeListener spreadsheetViewportListener;
	private TaskSelectionController taskSelectionController;
	private boolean synchronizingRowGeometry;
	private Runnable columnHeaderHeightBinding;
	private ComponentAdapter initialProjectFitListener;
	private final TableColumnModelListener dividerColumnListener = new TableColumnModelListener() {
		@Override public void columnAdded(TableColumnModelEvent event) { requestDividerBoundsEnforcement(); }
		@Override public void columnRemoved(TableColumnModelEvent event) { requestDividerBoundsEnforcement(); }
		@Override public void columnMoved(TableColumnModelEvent event) { requestDividerBoundsEnforcement(); }
		@Override public void columnMarginChanged(ChangeEvent event) { requestDividerBoundsEnforcement(); }
		@Override public void columnSelectionChanged(ListSelectionEvent event) { }
	};
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
		this.currentAnnotationFieldId = documentFrame.getGraphicManager().getPreferences().getDefaultGanttBarText();
		setResizeWeight(0.0);
		HelpUtil.addDocHelp(this,"Gantt_Chart");
		setNeedVoidBar(true);
		//setScaled(true);
	}
	public void init(ReferenceNodeModelCache cache, NodeModel model,CoordinatesConverter coord){
		this.coord = coord;
		this.cache = NodeModelCacheFactory.getInstance().createFilteredCache(cache, getViewName(), null);

		fieldContext = new FieldContext();
		fieldContext.setDurationDefaultsResolver(project::getDurationDefaults);
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
		installColumnHeaderHeightSync();

		//sync the height of spreadsheet and gantt
		installSpreadsheetViewportListener();
		installTaskSelectionController();
		cache.update();
		requestResponsiveDividerLayout();

		//Call this last to be sure everything is initialized
		//gantt.insertCacheData(); //useless?

	}
	/**
	 * The task table owns the header row height (wrapped headings make it grow); the Gantt
	 * timescale header follows so both halves and their rows stay aligned.
	 */
	private void installColumnHeaderHeightSync() {
		if (spreadSheet == null || ganttScrollPane == null)
			return;
		if (columnHeaderHeightBinding != null)
			columnHeaderHeightBinding.run();
		columnHeaderHeightBinding = ganttScrollPane.followColumnHeaderHeight(spreadSheet);
	}
	public void cleanUp() {
		removeInitialProjectFitListener();
		if (columnHeaderHeightBinding != null) {
			columnHeaderHeightBinding.run();
			columnHeaderHeightBinding = null;
		}
		removeSpreadsheetViewportListener();
		removeDividerColumnListener();
		removeTaskSelectionController();
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
			((SpreadSheetColumnModel) spreadSheet.getColumnModel()).ensureMinimumReadableWidths();
		}
		spreadSheet.getColumnModel().addColumnModelListener(dividerColumnListener);
		((SpreadSheetModel) spreadSheet.getModel()).setFieldContext(fieldContext);
		project.removeScheduleListener(this); // in case was already attached and recreating (applet)
		project.addScheduleListener(this);
		if (project.isReadOnly()) {
			spreadSheet.setReadOnly(true);
		}
		applySpreadsheetGridStyle();
		com.microproject.ui.theme.ThemeManager.bindToComponent(this, () -> { if (spreadSheet != null) applySpreadsheetGridStyle(); });
		SwingUtilities.invokeLater(() -> {
			if (spreadSheet != null)
				((SpreadSheetColumnModel) spreadSheet.getColumnModel()).ensureMinimumReadableWidths();
		});
		return SpreadSheetUtils.makeSpreadsheetScrollPane(spreadSheet);
   }
   protected JScrollPane createRightScrollPane() {
		gantt = new Gantt(project, "Gantt");
		gantt.setCache(cache);
		gantt.setAnnotationFieldId(currentAnnotationFieldId);
		gantt.setAnnotationPosition(documentFrame.getGraphicManager().getPreferences().getDefaultGanttBarTextPosition());
		gantt.setDefaultTaskBarColor(documentFrame.getGraphicManager().getPreferences().getDefaultGanttBarColor());
		gantt.setDefaultMilestoneShape(documentFrame.getGraphicManager().getPreferences().getDefaultMilestoneShape());
		gantt.setTrackingView(tracking);
		com.microproject.model.task.ProgressLineSettings progressLineSettings = project.getProgressLineSettings();
		if (progressLineSettings != null)
			setProgressLineEnabled(progressLineSettings.showCurrentLine() || progressLineSettings.showRecurringLines());
		gantt.setBarStyles((BarStyles) Dictionary.get(BarStyles.category, DEFAULT_GANTT_BAR_STYLE));
		ganttScrollPane = new ScaledScrollPane(gantt, coord, documentFrame, spreadSheet.getRowHeight());
		ganttScrollPane.preserveCenterDateOnWidthResize();
		ganttScrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
		applySpreadsheetGridStyle();
		return ganttScrollPane;
    }

	@Override
	protected int preferredLeftPaneWidth() {
		if (spreadSheet == null || leftScrollPane == null)
			return -1;
		int equalSplit = equalPaneDividerLocation(getWidth(), getInsets(), getDividerSize());
		if (equalSplit < 0)
			return -1;
		int viewportWidth = leftScrollPane.getViewport().getExtentSize().width;
		int paneChrome = Math.max(0, leftScrollPane.getWidth() - viewportWidth);
		int readableColumnsWidth = spreadSheet.getPreferredSize().width + paneChrome;
		// The task table must be able to show its default readable columns through
		// Finish (#432).  An even split is only a starting point: on narrower windows
		// it leaves the sheet viewport short of the Finish column and clips the value.
		// SplittedView still reserves the Gantt's font-scaled minimum below, so a wide
		// content-driven table never starves the timeline.
		return Math.max(equalSplit, readableColumnsWidth);
	}

	static int equalPaneDividerLocation(int width, Insets insets, int dividerSize) {
		int availableWidth = width - insets.left - insets.right - dividerSize;
		return availableWidth <= 0 ? -1 : availableWidth / 2;
	}

	/** Fits the initial Gantt range after the split pane has received its first layout. */
	public void fitProjectRangeOnInitialDisplay() {
		if (ganttScrollPane == null)
			return;
		initialProjectFitListener = new ComponentAdapter() {
			@Override public void componentResized(ComponentEvent event) { tryInitialProjectFit(); }
		};
		ganttScrollPane.addComponentListener(initialProjectFitListener);
		SwingUtilities.invokeLater(this::tryInitialProjectFit);
	}

	private void tryInitialProjectFit() {
		if (ganttScrollPane != null && SelectedTasksTimescaleZoom.zoomProject(project, coord, ganttScrollPane))
			removeInitialProjectFitListener();
	}

	private void removeInitialProjectFitListener() {
		if (ganttScrollPane != null && initialProjectFitListener != null)
			ganttScrollPane.removeComponentListener(initialProjectFitListener);
		initialProjectFitListener = null;
	}

	@Override
	protected int minimumResponsiveRightPaneWidth() {
		if (gantt == null || gantt.getFont() == null)
			return 96;
		int minimumViewportWidth = gantt.getFontMetrics(gantt.getFont()).charWidth('M') * 16;
		int viewportWidth = ganttScrollPane == null ? 0 : ganttScrollPane.getViewport().getExtentSize().width;
		int paneChrome = ganttScrollPane == null ? 0 : Math.max(0, ganttScrollPane.getWidth() - viewportWidth);
		return minimumViewportWidth + paneChrome;
	}

	@Override
	protected int maximumLeftPaneWidth() {
		if (spreadSheet == null || leftScrollPane == null)
			return Integer.MAX_VALUE;
		TableColumnModel columns = spreadSheet.getColumnModel();
		JViewport viewport = leftScrollPane.getViewport();
		int tableEdge = viewport.getX() + columns.getTotalColumnWidth() - viewport.getViewPosition().x;
		return Math.max(0, tableEdge);
	}

	@Override
	protected boolean enforcesMaximumLeftPaneWidth() {
		return true;
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
		requestDividerBoundsEnforcement();
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
	    int rowHeight = GanttRowHeight.calculate(project, baseLines);
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
			refreshTaskPath();
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
		if (gantt == null || !ScrollPaneSynchronizer.zoomIn(gantt, selectedTaskStart())) {
			coord.zoomIn();
		}
	}
	public void zoomOut() {
		if (gantt == null || !ScrollPaneSynchronizer.zoomOut(gantt, selectedTaskStart())) {
			coord.zoomOut();
		}
	}
	/**
	 * Start date of the first selected task, used as the zoom anchor (Microsoft Project zooms
	 * around the selection while it is visible); NaN when nothing usable is selected.
	 */
	private double selectedTaskStart() {
		if (spreadSheet == null)
			return Double.NaN;
		List impls = spreadSheet.getSelectedNodesImpl();
		if (impls == null || impls.isEmpty() || !(impls.get(0) instanceof HasStartAndEnd interval) || interval.getStart() <= 0L)
			return Double.NaN;
		return interval.getStart();
	}
	// Gantt Chart Format tab switches. The state lives in the Gantt (bar display) and the sheet
	// (outline numbers) of this view and travels with the view workspace.
	public boolean isCriticalTasksVisible() {
		return gantt == null || gantt.isCriticalTasksVisible();
	}
	public void setCriticalTasksVisible(boolean visible) {
		if (gantt != null)
			gantt.setCriticalTasksVisible(visible);
	}
	public boolean isSlackVisible() {
		return gantt != null && gantt.isSlackVisible();
	}
	public void setSlackVisible(boolean visible) {
		if (gantt != null)
			gantt.setSlackVisible(visible);
	}
	public boolean isLateTasksVisible() {
		return gantt != null && gantt.isLateTasksVisible();
	}
	public void setLateTasksVisible(boolean visible) {
		if (gantt != null)
			gantt.setLateTasksVisible(visible);
	}
	public boolean isBaselineVisible() {
		return gantt == null || gantt.isBaselineVisible();
	}
	public void setBaselineVisible(boolean visible) {
		if (gantt != null)
			gantt.setBaselineVisible(visible);
	}
	public boolean isSlippageVisible() {
		return gantt != null && gantt.isSlippageVisible();
	}
	public void setSlippageVisible(boolean visible) {
		if (gantt != null)
			gantt.setSlippageVisible(visible);
	}
	public GanttBarColorScheme getBarColorScheme() {
		return gantt == null ? GanttBarColorScheme.DEFAULT : gantt.getBarColorScheme();
	}
	public void setBarColorScheme(GanttBarColorScheme scheme) {
		if (gantt != null)
			gantt.setBarColorScheme(scheme);
	}
	public boolean isOutlineNumbersVisible() {
		return spreadSheet != null && spreadSheet.isOutlineNumbersVisible();
	}
	public void setOutlineNumbersVisible(boolean visible) {
		if (spreadSheet != null)
			spreadSheet.setOutlineNumbersVisible(visible);
	}
	private boolean summaryTasksVisible = true;
	public boolean supportsSummaryTasksToggle() {
		return true;
	}
	public boolean isSummaryTasksVisible() {
		// The transform is shared by views of the same name, so it is the source of truth when attached.
		if (cache instanceof ViewNodeModelCache viewCache)
			return !viewCache.getViewTransformer().isHideSummaryTasks();
		return summaryTasksVisible;
	}
	/** Hides or shows the summary task rows through the view transform (rows only; tasks are untouched). */
	public void setSummaryTasksVisible(boolean visible) {
		summaryTasksVisible = visible;
		if (cache instanceof ViewNodeModelCache viewCache)
			viewCache.getViewTransformer().setHideSummaryTasks(!visible);
	}
	/** Task Path highlight of the active mode (null when off); see {@link #refreshTaskPath()}. */
	public TaskPathAnalyzer.Kind getTaskPathKind() {
		return gantt == null ? null : gantt.getTaskPathKind();
	}
	/** Turns a Task Path highlight on for the selected tasks, or off with null. */
	public void setTaskPathKind(TaskPathAnalyzer.Kind kind) {
		if (gantt != null)
			gantt.setTaskPath(kind, selectedTasks());
	}
	/** Re-evaluates the Task Path for the current selection; call when the selection changes. */
	public void refreshTaskPath() {
		if (gantt != null && gantt.getTaskPathKind() != null)
			gantt.setTaskPath(gantt.getTaskPathKind(), selectedTasks());
	}
	/** Ribbon Format Bar: the bar format dialog for the first selected task. */
	public void showFormatBarDialog() {
		List<Task> tasks = selectedTasks();
		if (gantt != null && !tasks.isEmpty())
			gantt.showFormatBarDialog(tasks.get(0));
	}

	private List<Task> selectedTasks() {
		List<Task> tasks = new java.util.ArrayList<>();
		if (spreadSheet != null) {
			for (Object impl : spreadSheet.getSelectedNodesImpl()) {
				if (impl instanceof Task task)
					tasks.add(task);
			}
		}
		return tasks;
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
	public TimeScale getTimeScale(int scaleIndex) {
		return coord.getTimescaleManager().getScale(scaleIndex);
	}
	public void setScale(int scaleIndex) {
		if (coord == null)
			return;
		int boundedScale = ScrollPaneSynchronizer.clampTargetScaleIndex(scaleIndex, getScaleCount());
		if (gantt != null && ScrollPaneSynchronizer.zoomToScale(gantt, boundedScale, selectedTaskStart())) {
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
		removeDividerColumnListener();
		removeTaskSelectionController();
		if (coord != null && ganttScrollPane != null) {
			coord.removeTimeScaleListener(ganttScrollPane);
		}
		cleanupContentViews();
		super.reinitialize();
		if (taskSheet && rightScrollPane != null)
			rightScrollPane.setVisible(false);
		setSpreadsheetGridVisible(spreadsheetGridVisible);
		updateHeight(project);
		updateSize();
		installSpreadsheetViewportListener();
		installTaskSelectionController();
		synchronizeGanttHeightWithSpreadsheet(leftScrollPane.getViewport().getViewSize());
		requestResponsiveDividerLayout();
		requestSplitHierarchyValidation();
	}

	private void removeDividerColumnListener() {
		if (spreadSheet != null)
			spreadSheet.getColumnModel().removeColumnModelListener(dividerColumnListener);
	}
	public void restoreWorkspace(WorkspaceSetting w, int context) {
		Workspace ws = (Workspace) w;
		spreadSheet.restoreWorkspace(ws.spreadSheet, context);
		setSpreadsheetGridVisible(gridLinesVisibleFromWorkspace(ws));
		gantt.setProjectStartLineVisible(ws == null || ws.projectStartLineVisible == null ? true : ws.projectStartLineVisible.booleanValue());
		gantt.setStatusDateLineVisible(ws == null || ws.statusDateLineVisible == null ? true : ws.statusDateLineVisible.booleanValue());
		gantt.setCurrentDateLineVisible(ws == null || ws.currentDateLineVisible == null ? true : ws.currentDateLineVisible.booleanValue());
		if (ws != null && ws.projectLineStyle != null) gantt.setProjectLineStyle(ws.projectLineStyle.intValue());
		if (ws != null && ws.statusDateLineStyle != null) gantt.setStatusDateLineStyle(ws.statusDateLineStyle.intValue());
		if (ws != null && ws.currentDateLineStyle != null) gantt.setCurrentDateLineStyle(ws.currentDateLineStyle.intValue());
		if (ws != null && ws.projectLineColor != null) gantt.setProjectLineColor(new java.awt.Color(ws.projectLineColor.intValue(), true));
		if (ws != null && ws.statusDateLineColor != null) gantt.setStatusDateLineColor(new java.awt.Color(ws.statusDateLineColor.intValue(), true));
		if (ws != null && ws.currentDateLineColor != null) gantt.setCurrentDateLineColor(new java.awt.Color(ws.currentDateLineColor.intValue(), true));
		gantt.setTimescaleMajorLineVisible(ws == null || ws.timescaleMajorLineVisible == null ? false : ws.timescaleMajorLineVisible.booleanValue());
		gantt.setTimescaleMinorLineVisible(ws == null || ws.timescaleMinorLineVisible == null ? false : ws.timescaleMinorLineVisible.booleanValue());
		gantt.setNonWorkingBoundaryVisible(ws == null || ws.nonWorkingBoundaryVisible == null ? false : ws.nonWorkingBoundaryVisible.booleanValue());
		if (ws != null && ws.timescaleMajorLineStyle != null) gantt.setTimescaleMajorLineStyle(ws.timescaleMajorLineStyle.intValue());
		if (ws != null && ws.timescaleMinorLineStyle != null) gantt.setTimescaleMinorLineStyle(ws.timescaleMinorLineStyle.intValue());
		if (ws != null && ws.nonWorkingBoundaryStyle != null) gantt.setNonWorkingBoundaryStyle(ws.nonWorkingBoundaryStyle.intValue());
		if (ws != null && ws.timescaleMajorLineColor != null) gantt.setTimescaleMajorLineColor(new java.awt.Color(ws.timescaleMajorLineColor.intValue(), true));
		if (ws != null && ws.timescaleMinorLineColor != null) gantt.setTimescaleMinorLineColor(new java.awt.Color(ws.timescaleMinorLineColor.intValue(), true));
		if (ws != null && ws.nonWorkingBoundaryColor != null) gantt.setNonWorkingBoundaryColor(new java.awt.Color(ws.nonWorkingBoundaryColor.intValue(), true));
		setCurrentAnnotationFieldId(ws.annotationFieldId);
		applyBarFormat(gantt, ws);
		setOutlineNumbersVisible(ws.outlineNumbersVisible != null && ws.outlineNumbersVisible.booleanValue());
		setSummaryTasksVisible(ws.summaryTasksVisible == null || ws.summaryTasksVisible.booleanValue());
		com.microproject.model.task.ProgressLineSettings progressLineSettings = project == null
			? null : project.getProgressLineSettings();
		boolean progressLineVisible = progressLineSettings == null
			? ws.progressLineEnabled
			: progressLineSettings.showCurrentLine() || progressLineSettings.showRecurringLines();
		gantt.setProgressLineEnabled(progressLineVisible);
		if (tracking)
			trackingProgressLineEnabled = progressLineVisible;
		else
			standardProgressLineEnabled = progressLineVisible;
		ganttScrollPane.restoreWorkspace(ws.scrollPane, context);
		restoreDividerLocation(ws.dividerLocation);
	}
	public WorkspaceSetting createWorkspace(int context) {
		Workspace ws = new Workspace();
		ws.spreadSheet = spreadSheet.createWorkspace(context);
		ws.scrollPane = ganttScrollPane.createWorkspace(context);
		ws.progressLineEnabled = gantt.isProgressLineEnabled();
		ws.gridLinesVisible = spreadsheetGridVisible;
		ws.projectStartLineVisible = Boolean.valueOf(gantt.isProjectStartLineVisible());
		ws.statusDateLineVisible = Boolean.valueOf(gantt.isStatusDateLineVisible());
		ws.currentDateLineVisible = Boolean.valueOf(gantt.isCurrentDateLineVisible());
		ws.projectLineStyle = Integer.valueOf(gantt.getProjectLineStyle());
		ws.statusDateLineStyle = Integer.valueOf(gantt.getStatusDateLineStyle());
		ws.currentDateLineStyle = Integer.valueOf(gantt.getCurrentDateLineStyle());
		if (gantt.getProjectLineColor() != null) ws.projectLineColor = Integer.valueOf(gantt.getProjectLineColor().getRGB());
		if (gantt.getStatusDateLineColor() != null) ws.statusDateLineColor = Integer.valueOf(gantt.getStatusDateLineColor().getRGB());
		if (gantt.getCurrentDateLineColor() != null) ws.currentDateLineColor = Integer.valueOf(gantt.getCurrentDateLineColor().getRGB());
		ws.timescaleMajorLineVisible = Boolean.valueOf(gantt.isTimescaleMajorLineVisible());
		ws.timescaleMinorLineVisible = Boolean.valueOf(gantt.isTimescaleMinorLineVisible());
		ws.nonWorkingBoundaryVisible = Boolean.valueOf(gantt.isNonWorkingBoundaryVisible());
		ws.timescaleMajorLineStyle = Integer.valueOf(gantt.getTimescaleMajorLineStyle());
		ws.timescaleMinorLineStyle = Integer.valueOf(gantt.getTimescaleMinorLineStyle());
		ws.nonWorkingBoundaryStyle = Integer.valueOf(gantt.getNonWorkingBoundaryStyle());
		if (gantt.getTimescaleMajorLineColor() != null) ws.timescaleMajorLineColor = Integer.valueOf(gantt.getTimescaleMajorLineColor().getRGB());
		if (gantt.getTimescaleMinorLineColor() != null) ws.timescaleMinorLineColor = Integer.valueOf(gantt.getTimescaleMinorLineColor().getRGB());
		if (gantt.getNonWorkingBoundaryColor() != null) ws.nonWorkingBoundaryColor = Integer.valueOf(gantt.getNonWorkingBoundaryColor().getRGB());
		captureBarFormat(gantt, ws);
		ws.outlineNumbersVisible = Boolean.valueOf(isOutlineNumbersVisible());
		ws.summaryTasksVisible = Boolean.valueOf(isSummaryTasksVisible());
		ws.annotationFieldId = currentAnnotationFieldId;
		ws.dividerLocation = getDividerLocation();
		return ws;
	}

	/** Writes the Gantt Chart Format bar switches of the chart into the workspace. */
	static void captureBarFormat(Gantt gantt, Workspace ws) {
		ws.criticalTasksVisible = Boolean.valueOf(gantt.isCriticalTasksVisible());
		ws.slackVisible = Boolean.valueOf(gantt.isSlackVisible());
		ws.lateTasksVisible = Boolean.valueOf(gantt.isLateTasksVisible());
		ws.baselineVisible = Boolean.valueOf(gantt.isBaselineVisible());
		ws.slippageVisible = Boolean.valueOf(gantt.isSlippageVisible());
		ws.barColorScheme = gantt.getBarColorScheme().getId();
	}

	/** Applies the bar switches of a workspace; workspaces saved before they existed get the defaults. */
	static void applyBarFormat(Gantt gantt, Workspace ws) {
		gantt.setCriticalTasksVisible(ws.criticalTasksVisible == null || ws.criticalTasksVisible.booleanValue());
		gantt.setSlackVisible(ws.slackVisible != null && ws.slackVisible.booleanValue());
		gantt.setLateTasksVisible(ws.lateTasksVisible != null && ws.lateTasksVisible.booleanValue());
		gantt.setBaselineVisible(ws.baselineVisible == null || ws.baselineVisible.booleanValue());
		gantt.setSlippageVisible(ws.slippageVisible != null && ws.slippageVisible.booleanValue());
		gantt.setBarColorScheme(GanttBarColorScheme.fromId(ws.barColorScheme));
	}

	static boolean gridLinesVisibleFromWorkspace(Workspace workspace) {
		// Older serialized workspaces have no field and deserialize it as null.
		return workspace == null || workspace.gridLinesVisible == null
				? Gantt.DEFAULT_GRID_LINES_VISIBLE
				: workspace.gridLinesVisible.booleanValue();
	}

	public static class Workspace implements WorkspaceSetting {
		private static final long serialVersionUID = -407561451956813994L;
		WorkspaceSetting spreadSheet;
		WorkspaceSetting scrollPane;
		boolean progressLineEnabled;
		Boolean gridLinesVisible;
		Boolean projectStartLineVisible;
		Boolean statusDateLineVisible;
		Boolean currentDateLineVisible;
		Integer projectLineStyle;
		Integer statusDateLineStyle;
		Integer currentDateLineStyle;
		Integer projectLineColor;
		Integer statusDateLineColor;
		Integer currentDateLineColor;
		Boolean timescaleMajorLineVisible;
		Boolean timescaleMinorLineVisible;
		Boolean nonWorkingBoundaryVisible;
		// Gantt Chart Format switches; null in workspaces saved before they existed (defaults apply).
		Boolean criticalTasksVisible;
		Boolean slackVisible;
		Boolean lateTasksVisible;
		Boolean baselineVisible;
		Boolean slippageVisible;
		String barColorScheme;
		Boolean outlineNumbersVisible;
		Boolean summaryTasksVisible;
		Integer timescaleMajorLineStyle;
		Integer timescaleMinorLineStyle;
		Integer nonWorkingBoundaryStyle;
		Integer timescaleMajorLineColor;
		Integer timescaleMinorLineColor;
		Integer nonWorkingBoundaryColor;
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
		public Boolean getGridLinesVisible() {
			return gridLinesVisible;
		}
		public void setGridLinesVisible(Boolean gridLinesVisible) {
			this.gridLinesVisible = gridLinesVisible;
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

	/** Centres the current system date in the chart; selection and model data are untouched. */
	public boolean scrollToToday() {
		return gantt != null && gantt.scrollToDate(System.currentTimeMillis());
	}

	public boolean canScrollToTask() {
		return true;
	}
	public boolean canZoomToSelectedTasks() {
		return spreadSheet != null && SelectedTasksTimescaleZoom.canZoom(spreadSheet.getSelectedNodesImpl());
	}
	public void zoomToSelectedTasks() {
		if (spreadSheet != null)
			SelectedTasksTimescaleZoom.zoom(spreadSheet.getSelectedNodesImpl(), coord, ganttScrollPane);
	}
	public boolean canZoomToProject() {
		return SelectedTasksTimescaleZoom.canZoomProject(project);
	}
	public void zoomToProject() {
		SelectedTasksTimescaleZoom.zoomProject(project, coord, ganttScrollPane);
	}
	public SelectedTasksTimescaleZoom.DateRange getVisibleDateRange() {
		return SelectedTasksTimescaleZoom.visibleRange(coord, ganttScrollPane);
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
	/** True while the view is shown as the Task Sheet: the task table with the chart collapsed. */
	public boolean isTaskSheet() {
		return taskSheet;
	}
	/**
	 * Collapses the chart pane (Task Sheet) or brings it back (Gantt Chart views).  The divider is
	 * left where it was, so the chart layout returns unchanged when the sheet is closed.
	 */
	public void setTaskSheet(boolean sheet) {
		if (this.taskSheet == sheet)
			return;
		this.taskSheet = sheet;
		if (rightScrollPane == null)
			return;
		if (sheet) {
			sheetSavedDividerSize = getDividerSize();
			setDividerSize(0);
		} else {
			setDividerSize(sheetSavedDividerSize);
		}
		rightScrollPane.setVisible(!sheet);
		revalidate();
		repaint();
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
					requestDividerBoundsEnforcement();
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
	private void installTaskSelectionController() {
		if (taskSelectionController == null && spreadSheet != null && gantt != null) {
			taskSelectionController = new TaskSelectionController(gantt, spreadSheet);
		}
	}

	private void removeTaskSelectionController() {
		if (taskSelectionController != null) {
			taskSelectionController.close();
			taskSelectionController = null;
		}
	}

	private void applySpreadsheetGridStyle() {
		TaskGanttSyncSupport.applySpreadsheetGridStyle(spreadSheet, gantt, spreadsheetGridVisible, getSpreadsheetGridLineColor());
	}

	private Color getSpreadsheetGridLineColor() {
		Integer configuredColor = documentFrame.getGraphicManager().getPreferences().getGridLineColor();
		if (configuredColor != null) return new Color(configuredColor.intValue());
		// Clear the user override before resolving the current theme palette.
		if (gantt != null) gantt.setGridLineColor(null);
		return TaskGanttSyncSupport.resolveGridLineColor(gantt);
	}

	private void synchronizeGanttHeightWithSpreadsheet(Dimension spreadsheetSize) {
		TaskGanttSyncSupport.synchronizeGanttHeightWithSpreadsheet(rightScrollPane, spreadsheetSize);
	}

}


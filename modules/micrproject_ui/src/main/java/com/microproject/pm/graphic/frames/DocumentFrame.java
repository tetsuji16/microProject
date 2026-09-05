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
package com.microproject.pm.graphic.frames;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;

import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;


import com.microproject.dialog.BaselineDialog;
import com.microproject.dialog.DelegateTaskDialog;
import com.microproject.dialog.FindDialog;
import com.microproject.dialog.ResourceLevelingDialogBox;
import com.microproject.dialog.TeamPlannerDialogBox;
import com.microproject.dialog.TimelineDialogBox;
import com.microproject.dialog.CalendarViewDialogBox;
import com.microproject.dialog.CustomFieldsDialogBox;
import com.microproject.dialog.CustomReportDialogBox;
import com.microproject.dialog.UpdateProjectDialogBox;
import com.microproject.dialog.UpdateTaskDialog;
import com.microproject.dialog.calendar.ChangeWorkingTimeDialogBox;
import com.microproject.menu.MenuActionConstants;
import com.microproject.menu.MenuActionsMap;
import com.microproject.menu.MenuManager;
import com.microproject.pm.graphic.IconManager;
import com.microproject.pm.graphic.collaboration.CollaborationHelper;
import com.microproject.pm.graphic.frames.workspace.NamedFrame;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.common.CommonSpreadSheet;
import com.microproject.pm.graphic.spreadsheet.selection.event.SelectionNodeEvent;
import com.microproject.pm.graphic.spreadsheet.selection.event.SelectionNodeListener;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.graphic.views.BaseView;
import com.microproject.pm.graphic.views.ChartView;
import com.microproject.pm.graphic.chart.ChartMode;
import com.microproject.pm.graphic.views.DockableProjectToolView;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.views.GanttView;
import com.microproject.pm.graphic.views.MainView;
import com.microproject.pm.graphic.views.PertView;
import com.microproject.pm.graphic.views.ProjectView;
import com.microproject.pm.graphic.views.ResourceView;
import com.microproject.pm.graphic.views.Searchable;
import com.microproject.pm.graphic.views.TreeView;
import com.microproject.pm.graphic.views.UsageDetailView;
import com.microproject.toolbar.FilterToolBarManager;
import com.microproject.association.InvalidAssociationException;
import com.microproject.command.UpdateProjectCommand;
import com.microproject.configuration.Configuration;
import com.microproject.document.ObjectEvent;
import com.microproject.document.ObjectSelectionEvent;
import com.microproject.document.ObjectSelectionListener;
import com.microproject.field.Field;
import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeList;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.grouping.core.transform.ViewTransformer;
import com.microproject.grouping.core.transform.filtering.NodeFilter;
import com.microproject.grouping.core.transform.filtering.NotAssignmentFilter;
import com.microproject.grouping.core.transform.filtering.ResourceInTeamFilter;
import com.microproject.job.JobQueue;
import com.microproject.pm.calendar.CalendarService;
import com.microproject.pm.calendar.HasCalendar;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.task.Portfolio;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectEvent;
import com.microproject.pm.task.ProjectFactory;
import com.microproject.pm.task.ProjectListener;
import com.microproject.pm.task.Task;
import com.microproject.preference.GlobalPreferences;
import com.microproject.session.LoadOptions;
import com.microproject.undo.UndoController;
import com.microproject.util.Alert;
import com.microproject.util.ArrayUtils;
import com.microproject.util.ClassUtils;
import com.microproject.util.DataUtils;
import com.microproject.util.Environment;
import com.microproject.workspace.SavableToWorkspace;
import com.microproject.workspace.WorkspaceSetting;

/**
 *
 */
@SuppressWarnings("unchecked")
public class DocumentFrame extends NamedFrame implements
		SelectionNodeListener, UndoableEditListener, MenuActionConstants, ObjectEvent.Listener, ProjectListener, SavableToWorkspace, ObjectSelectionListener {
	private static final long serialVersionUID = 2075764134837908178L;
	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(DocumentFrame.class.getName());
	protected MainView mainView;
	protected final DocumentStatusBar statusBar = new DocumentStatusBar();
	protected GanttView ganttView;
	protected UsageDetailView taskUsageDetailView;
	protected UsageDetailView resourceUsageDetailView;
	protected PertView pertView;
	protected TreeView wbsView;
	protected TreeView rbsView;
	protected ChartView chartView;
	protected ChartView histogramView;
	protected ResourceView resourceView;
	protected ProjectView projectView;
	protected UsageDetailView taskUsageView;
	protected UsageDetailView resourceUsageView;
	protected DockableProjectToolView timelineView;
	protected DockableProjectToolView teamPlannerView;
	protected BaseView reportView;
	// Column selection belongs to this document frame.  Keeping it static leaks
	// one document's tracking layout into every other open document.
	private ArrayList ganttColumns;
	private FindDialog findDialog = null;
	protected CoordinatesConverter coord;
	protected Project project;
	protected GraphicManager graphicManager;
	protected MenuManager menuManager;
	MenuActionsMap actionsMap = null;
	BaseView activeTopView = null;
	BaseView activeBottomView = null;

	// keep state of pushed buttons so i can reset them when a view is reactivated
	String lastTopButton = null;
	String lastBottomButton = ACTION_NO_SUB_WINDOW;
	Workspace workspace;
	FilterToolBarManager filterToolBarManager = null;
	JobQueue jobQueue = null;
	private JRadioButtonMenuItem menuItem = null;
	public GraphicManager getGraphicManager() {
		return graphicManager;
	}

	protected NodeModel getTaskModel() {
		return project.getTaskModel();
	}

	protected NodeModel getResourceModel() {
		return project.getResourceModel();
	}

	public JobQueue getJobQueue(){
		if (jobQueue==null){
			jobQueue=new JobQueue("GraphicManager",true);
		}
		return jobQueue;
	}

	public ReferenceNodeModelCache getTaskNodeModelCache() {
		ReferenceNodeModelCache taskCache = (ReferenceNodeModelCache) project.getTaskCache();
		if (taskCache == null) {
			taskCache =NodeModelCacheFactory.createTaskNodeModelCache(project, getTaskModel());
			project.setTaskCache(taskCache);
		}
		return taskCache;
	}
	public ReferenceNodeModelCache getResourceNodeModelCache() {
		ReferenceNodeModelCache resourceCache = (ReferenceNodeModelCache) project.getResourceCache();
		if (resourceCache == null) {
			resourceCache =NodeModelCacheFactory.createResourceNodeModelCache(project.getResourcePool(), getResourceModel());
			project.setResourceCache(resourceCache);
		}
		return resourceCache;
	}


	public ReferenceNodeModelCache getReferenceCache(boolean task) {
		ReferenceNodeModelCache cache = (task) ? getTaskNodeModelCache()
				: getResourceNodeModelCache();
		return cache;
	}

	public NodeModelCache createCache(boolean task, String viewName) {
		return NodeModelCacheFactory.getInstance().createFilteredCache(
				getReferenceCache(task), viewName,null);
	}



	public DocumentFrame(GraphicManager parentFrame, final Project project,String id) {
		super(id, IconManager.getHalfSizedIcon("view.gantt"));

		this.graphicManager = parentFrame;
		this.menuManager = graphicManager.getMenuManager();
		filterToolBarManager = Environment.isNewLook() ? FilterToolBarManager.create(menuManager) : graphicManager.getFilterToolBarManager();

		this.project = project;
		coord = new CoordinatesConverter(project);
		coord.addTimeScaleListener(event -> updateStatusBarZoom());

		project.addObjectListener(this); // for project name changes
		getGraphicManager().getPreferences().addObjectListener(this);

		project.getObjectSelectionEventManager().addListener(this);
		setPreferredSize(new Dimension(800, 600));
		setMainView(true);

	}
	private void updateStatusBarZoom() {
		statusBar.setZoom(coord.getTimescaleManager().getCurrentScaleIndex(),
				coord.getTimescaleManager().getScaleCount());
	}

	private void setMainView(boolean activate) {
		if (mainView != null)
			remove(mainView); // any previous
		mainView = new MainView();
		mainView.setBorder(null);
		setLayout(new BorderLayout()); // main view fills the center, status bar below
		add(mainView, BorderLayout.CENTER);
		add(statusBar, BorderLayout.SOUTH);
		updateStatusBarZoom();

//		toolBarListener = new ToolBarListener();
//		registerToolBarActions();


		// wait until everything is initialized before activating the gantt view
		if (activate) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if (true || GraphicManager.getLastWorkspace() == null) { // if there was a workspace, it will be restored
						if (isEditingResourcePool()) {
							activateResourceView();
							getGraphicManager().setAllButResourceDisabled(true);

						} else {
							activateGanttView();
							getGraphicManager().setAllButResourceDisabled(false);
						}
					}
					getGraphicManager().setButtonState(null,project); // disable buttons at first
				}});
		}

	}

	List getSelectedNodes(boolean excludeReadOnly) {
		CommonSpreadSheet spreadSheet = getTopSpreadSheet();
		List nodes = null;
		if (spreadSheet != null) {
			nodes = spreadSheet.getSelectedNodes();
			if (nodes != null && nodes.size() == 0) {
				Node currentNode = spreadSheet.getCurrentRowNode();
				if (currentNode != null) {
					nodes = new ArrayList(1);
					nodes.add(currentNode);
				}
			}
		} else if (activeTopView instanceof TreeView) {
			nodes = ((TreeView) activeTopView).getSelectedNodes();
		}
		if (nodes == null || nodes.size() == 0)
			return null;
		if (excludeReadOnly) {
			Iterator i = nodes.iterator();
			while (i.hasNext()) {
				Node n = (Node)i.next();
				Object obj = n.getImpl();
				if (ClassUtils.isObjectReadOnly(obj))
					i.remove();
			}
		}

		return nodes;
	}

	Object getSelectedImpl() {
		CommonSpreadSheet spreadSheet = getTopSpreadSheet();
		if (spreadSheet != null)
			return spreadSheet.getCurrentRowImpl();
		if (activeTopView instanceof TreeView)
			return ((TreeView) activeTopView).getSelectedImpl();
		return null;
	}

	boolean doBaselineDialog(boolean save) {
		finishAnyOperations();

		BaselineDialog baselineDialog = BaselineDialog.getInstance(getGraphicManager(),
				null, save,hasAtLeastOneTaskSelected());
		if (!baselineDialog.doModal())
			return false;
		Integer baselineNumber = Integer.valueOf(baselineDialog.getForm()
				.getBaselineNumber());
		boolean entireProject = baselineDialog.getForm().isEntireProject();
		List selection = entireProject ? null : getSelectedImpls(true);
		return applyBaseline(getProject(), save, baselineDialog.getForm(), selection);
	}

	/**
	 * Applies a confirmed baseline dialog choice.  Keeping this operation separate
	 * from the Swing dialog makes the command path testable all the way through
	 * the project snapshot write, rather than only as far as opening a dialog.
	 */
	static boolean applyBaseline(Project project, boolean save, BaselineDialog.Form form, List selection) {
		if (project == null || form == null) {
			return false;
		}
		Integer baselineNumber = Integer.valueOf(form.getBaselineNumber());
		boolean entireProject = form.isEntireProject();
		if (save)
			project.saveCurrentToSnapshot(baselineNumber, entireProject,
					selection,true);
		else
			project
					.clearSnapshot(baselineNumber, entireProject, selection,true);
//		getProject().fireBaselineChanged(baselineDialog, null, baselineNumber,
//				save);
		return true;
	}

//	void doEnterpriseResourcesDialog() {
//		finishAnyOperations();
//		EnterpriseResourcesDialog enterpriseResourceDialog = EnterpriseResourcesDialog
//				.getInstance(getMainFrame());
//		enterpriseResourceDialog.pack();
//		enterpriseResourceDialog.setModal(false);
//		enterpriseResourceDialog.setLocationRelativeTo(null);//to center on
//															 // screen
//		enterpriseResourceDialog.show();
//	}


	void doChangeWorkingTimeDialog(boolean restrict) {
		finishAnyOperations();
		Object rowObject = getSelectedImpl();
		WorkingCalendar wc = null;
		List documentCalendars = null;
		if (rowObject instanceof HasCalendar) {
			wc = (WorkingCalendar) ((HasCalendar) rowObject).getWorkCalendar();
			if (rowObject instanceof ResourceImpl) {
				documentCalendars = ((ResourceImpl) rowObject)
						.getResourcePool().extractCalendars();
			}
		}
		if (wc == null)
			wc = (WorkingCalendar) getProject().getWorkCalendar();
		CalendarService service = CalendarService.getInstance();


		ChangeWorkingTimeDialogBox dlg = ChangeWorkingTimeDialogBox
				.getInstance(getGraphicManager().getFrame(), project,wc, documentCalendars,restrict,this.getUndoController());
		dlg.doModal();
	}


	void doLevelResourcesDialog() {
		finishAnyOperations();
		if (java.awt.GraphicsEnvironment.isHeadless())
			return;
		ResourceLevelingDialogBox.getInstance(getGraphicManager().getFrame(), project).setVisible(true);
	}

	void doCriticalChainDialog() {
		finishAnyOperations();
		if (java.awt.GraphicsEnvironment.isHeadless())
			return;
		ResourceLevelingDialogBox.getCriticalChainInstance(getGraphicManager().getFrame(), project).setVisible(true);
	}


	void doDelegateTasksDialog() {
		finishAnyOperations();
		activateTopView(getTeamPlannerView(), ACTION_DELEGATE_TASKS);

	}

	void doUpdateTasksDialog() {
		finishAnyOperations();
		List nodes = getSelectedNodes(true); //nodes, not impls!
		if (nodes == null)
			return;

		UpdateTaskDialog dlg = UpdateTaskDialog.getInstance(getGraphicManager().getFrame(),
				nodes);
		dlg.setLocationRelativeTo(null);//to center on screen
		dlg.doModal();

	}

	void doUpdateProjectDialog() {
		finishAnyOperations();
		UpdateProjectDialogBox dlg = UpdateProjectDialogBox.getInstance(
				getGraphicManager().getFrame(), null,hasAtLeastOneTaskSelected());
		if (dlg.doModal()) {
			UpdateProjectCommand cmd = new UpdateProjectCommand(project, dlg
					.getForm().getActiveDate().getTime(), dlg.getForm()
					.getUpdate().booleanValue(), dlg.getForm().getProgress()
					.booleanValue());
			forTasksDo(cmd, dlg.getForm().getEntireProject().booleanValue());
			cmd = null;
		}

	}

	void doDefineCodeDialog() {
		finishAnyOperations();
		List<Task> selected = new ArrayList<>();
		for (Object value : getSelectedImpls(false)) if (value instanceof Task task) selected.add(task);
		new CustomFieldsDialogBox(getGraphicManager().getFrame(), project, selected).setVisible(true);
	}

	void doRecurringTaskDialog() {
		finishAnyOperations();
		new RecurringTaskCoordinator().openDialogAndInsert(this);
	}

	void doTimelineDialog() {
		finishAnyOperations();
		activateTopView(getTimelineView(), ACTION_TIMELINE);
	}

	void doCalendarViewDialog() {
		finishAnyOperations();
		new CalendarViewDialogBox(getGraphicManager().getFrame(), project).setVisible(true);
	}

	void doCustomReportDialog() {
		finishAnyOperations();
		new CustomReportDialogBox(getGraphicManager().getFrame(), project).setVisible(true);
	}

	void doBarDialog() {
		finishAnyOperations();
//		ShapeBarDialogBox.getInstance(getGraphicManager().getFrame(), null).doModal();
	}

	void doSortDialog() {
		finishAnyOperations();
//		SortDialogBox.getInstance(getGraphicManager().getFrame(), null).doModal();
	}

	void doGroupDialog() {
		finishAnyOperations();
//		GroupDefinitionDialogBox.getInstance(getGraphicManager().getFrame(), null).doModal();
	}


	public void doLinkTasks() {
		// Capture the selection before finishing an editor.  Stopping a cell
		// editor can clear the JTable selection, which previously made the
		// ribbon command silently return even though it was enabled.
		List<Node> taskNodes = new ArrayList<>(getSelectedTaskNodes(false, true));
		getGraphicManager().traceUi("link start selectedTasks=" + taskNodes.size()
				+ " undo=" + canUndoState() + " redo=" + canRedoState());
		finishAnyOperations();
		try {
			if (taskNodes.size() < 2) {
				getGraphicManager().traceUi("link rejected reason=selection-too-small selectedTasks=" + taskNodes.size());
				return;
			}
			if (!CollaborationHelper.tryLockNodes(getProject(), taskNodes, this, "link")) {
				getGraphicManager().traceUi("link rejected reason=lock-failed selectedTasks=" + taskNodes.size());
				return;
			}
			List list = NodeList.nodeListToImplList(taskNodes, NotAssignmentFilter.getInstance());
			if (list.size() < 2) {
				getGraphicManager().traceUi("link rejected reason=task-filter selectedTasks=" + list.size());
				return;
			}
			DependencyService.getInstance().connect(list,this,null);
			getActiveSpreadSheet().restoreTaskRowSelection(taskNodes);
			getGraphicManager().traceUi("link complete selectedTasks=" + taskNodes.size()
				+ " dependencies=" + dependencyCount(list) + " undo=" + canUndoState() + " redo=" + canRedoState());
			//DependencyService.getInstance().connect(list,this);
		} catch (InvalidAssociationException e) {
			getGraphicManager().traceUi("link rejected reason=invalid-association message=" + e.getMessage());
			Alert.error(e.getMessage(),this);
		}
	}
	public void doUnlinkTasks() {
		List<Node> taskNodes = new ArrayList<>(getSelectedTaskNodes(false, true));
		getGraphicManager().traceUi("unlink start selectedTasks=" + taskNodes.size()
				+ " undo=" + canUndoState() + " redo=" + canRedoState());
		finishAnyOperations();
		if (taskNodes.isEmpty()) {
			getGraphicManager().traceUi("unlink rejected reason=no-selection");
			return;
		}
		if (!CollaborationHelper.tryLockNodes(getProject(), taskNodes, this, "unlink")) {
			getGraphicManager().traceUi("unlink rejected reason=lock-failed selectedTasks=" + taskNodes.size());
			return;
		}
		List list = NodeList.nodeListToImplList(taskNodes, NotAssignmentFilter.getInstance());
		if (list.isEmpty()) {
			getGraphicManager().traceUi("unlink rejected reason=task-filter");
			return;
		}


		DependencyService.getInstance().removeAnyDependencies(list,this);
		getActiveSpreadSheet().restoreTaskRowSelection(taskNodes);
		getGraphicManager().traceUi("unlink complete selectedTasks=" + taskNodes.size()
				+ " dependencies=" + dependencyCount(list) + " undo=" + canUndoState() + " redo=" + canRedoState());
	}
	public void doUndoRedo(boolean isUndo) {
		if (!isActive())
			return;
		getGraphicManager().traceUi((isUndo ? "undo" : "redo") + " start canUndo=" + canUndoState() + " canRedo=" + canRedoState());
		SelectionSnapshot selectionSnapshot = SelectionSnapshot.capture(getActiveSpreadSheet());
		finishAnyOperations();
		UndoController undoController=getUndoController();
		if (undoController!=null){
			if (isUndo)
				undoController.undo();
			else
				undoController.redo();
			refreshUndoButtons();
			selectionSnapshot.restore();
			getGraphicManager().traceUi((isUndo ? "undo" : "redo") + " complete canUndo=" + canUndoState() + " canRedo=" + canRedoState()
					+ " selectedTasks=" + getSelectedTaskNodes(false, true).size());
		}
	}

	private static int dependencyCount(List<?> tasks) {
		int count = 0;
		for (Object value : tasks) {
			if (value instanceof Task task)
				count += task.getPredecessorList().size();
		}
		return count;
	}

	private boolean canUndoState() {
		UndoController controller = getUndoController();
		return controller != null && controller.canUndo();
	}

	private boolean canRedoState() {
		UndoController controller = getUndoController();
		return controller != null && controller.canRedo();
	}

	private static final class SelectionSnapshot {
		private final CommonSpreadSheet spreadSheet;
		private final List<Node> selectedNodes;
		private final Node node;
		private final Object impl;
		private final int row;
		private final int column;

		private SelectionSnapshot(CommonSpreadSheet spreadSheet, Node node, Object impl, int row, int column,
				List<Node> selectedNodes) {
			this.spreadSheet = spreadSheet;
			this.node = node;
			this.impl = impl;
			this.row = row;
			this.column = column;
			this.selectedNodes = selectedNodes;
		}

		private static SelectionSnapshot capture(CommonSpreadSheet spreadSheet) {
			if (spreadSheet == null)
				return new SelectionSnapshot(null, null, null, -1, -1, Collections.emptyList());
			int row = spreadSheet.getCurrentRow();
			if (row < 0)
				return new SelectionSnapshot(spreadSheet, null, null, -1, -1, spreadSheet.getSelectedNodes());
			int column = spreadSheet.isEditing() ? spreadSheet.getEditingColumn() : spreadSheet.getSelectedColumn();
			CommonSpreadSheet.PendingUndoSelection pendingUndoSelection = spreadSheet.consumePendingUndoSelection(row, column);
			if (pendingUndoSelection != null) {
				return new SelectionSnapshot(spreadSheet, pendingUndoSelection.node(), pendingUndoSelection.impl(),
						pendingUndoSelection.row(), pendingUndoSelection.column(), spreadSheet.getSelectedNodes());
			}
			Node node = spreadSheet.getCurrentRowNode();
			Object impl = (node == null) ? null : node.getImpl();
			return new SelectionSnapshot(spreadSheet, node, impl, row, column, spreadSheet.getSelectedNodes());
		}

		private void restore() {
			if (spreadSheet == null || column < 0)
				return;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if (spreadSheet instanceof SpreadSheet taskSheet && selectedNodes != null && selectedNodes.size() > 1) {
						taskSheet.requestFocusInWindow();
						taskSheet.restoreTaskRowSelection(selectedNodes);
						return;
					}
					int targetRow = resolveRow();
					if (targetRow < 0 || targetRow >= spreadSheet.getRowCount() || column >= spreadSheet.getColumnCount())
						return;
					spreadSheet.requestFocusInWindow();
					spreadSheet.changeSelection(targetRow, column, false, false);
					spreadSheet.scrollRectToVisible(spreadSheet.getCellRect(targetRow, column, true));
				}
			});
		}

		private int resolveRow() {
			if (!(spreadSheet.getModel() instanceof SpreadSheetModel))
				return row;
			SpreadSheetModel model = (SpreadSheetModel) spreadSheet.getModel();
			if (node != null) {
				for (int currentRow = 0; currentRow < spreadSheet.getRowCount(); currentRow++) {
					Node rowNode = model.getNode(currentRow).getNode();
					if (rowNode == node)
						return currentRow;
				}
			}
			if (impl != null) {
				for (int currentRow = 0; currentRow < spreadSheet.getRowCount(); currentRow++) {
					Node rowNode = model.getNode(currentRow).getNode();
					if (rowNode != null && rowNode.getImpl() == impl)
						return currentRow;
				}
			}
			return row;
		}
	}
	public void doZoomIn() {
		if (activeTopView != null)
			activeTopView.zoomIn();
	}
	public void doZoomOut() {
		if (activeTopView != null)
			activeTopView.zoomOut();
	}
	public void doScrollToTask() {
		SpreadSheet ss = getActiveSpreadSheet();
		if (ss != null) {
			if (!hasTaskSelection(true, 1, false))
				return;
			focusSingleTaskSelectionAnchor(ss);
		}
		if (activeTopView != null)
			activeTopView.scrollToTask();
	}

	public boolean canZoomIn() {
		return activeTopView==null?false:activeTopView.canZoomIn();
	}
	public boolean canZoomOut() {
		return activeTopView==null?false:activeTopView.canZoomOut();
	}
	public boolean canScrollToTask() {
		return activeTopView==null?false:activeTopView.canScrollToTask();
	}
	public int getScale() {
		return activeTopView==null?-1:activeTopView.getScale();
	}

	public void doOutdent() {
		SpreadSheet ss = getActiveSpreadSheet();
		if (ss !=null) {
			int[] selectedRows = ss.getSelectedRows();
			List<Node> taskNodes = new ArrayList<>(getSelectedTaskNodes(false, false));
			getGraphicManager().traceUi("outdent start selectedTasks=" + taskNodes.size()
					+ " rows=" + selectedRows.length + " undo=" + canUndoState() + " redo=" + canRedoState());
			finishAnyOperations();
			if (taskNodes.isEmpty()) {
				getGraphicManager().traceUi("outdent rejected reason=no-selection");
				return;
			}
			if (!CollaborationHelper.tryLockNodes(getProject(), taskNodes, this, "outdent")) {
				getGraphicManager().traceUi("outdent rejected reason=lock-failed selectedTasks=" + taskNodes.size());
				return;
			}
			ss.executeAction(MenuActionConstants.ACTION_OUTDENT, selectedRows);
			ss.restoreTaskRowSelection(taskNodes);
			getGraphicManager().traceUi("outdent complete selectedTasks=" + taskNodes.size()
					+ " undo=" + canUndoState() + " redo=" + canRedoState());
		}
	}
	public void doExpand() {
		SpreadSheet ss = getActiveSpreadSheet();
		if (ss !=null) {
			if (!hasTaskSelection(false, 1, false))
				return;
			focusSingleTaskSelectionAnchor(ss);
			ss.executeAction(MenuActionConstants.ACTION_EXPAND);
		}
	}
	public void doCollapse() {
		SpreadSheet ss = getActiveSpreadSheet();
		if (ss !=null) {
			if (!hasTaskSelection(false, 1, false))
				return;
			focusSingleTaskSelectionAnchor(ss);
			ss.executeAction(MenuActionConstants.ACTION_COLLAPSE);
		}
	}

	public void doIndent() {
		SpreadSheet ss = getActiveSpreadSheet();
		if (ss !=null) {
			int[] selectedRows = ss.getSelectedRows();
			List<Node> taskNodes = new ArrayList<>(getSelectedTaskNodes(false, false));
			getGraphicManager().traceUi("indent start selectedTasks=" + taskNodes.size()
					+ " rows=" + selectedRows.length + " undo=" + canUndoState() + " redo=" + canRedoState());
			finishAnyOperations();
			if (taskNodes.isEmpty()) {
				getGraphicManager().traceUi("indent rejected reason=no-selection");
				return;
			}
			if (!CollaborationHelper.tryLockNodes(getProject(), taskNodes, this, "indent")) {
				getGraphicManager().traceUi("indent rejected reason=lock-failed selectedTasks=" + taskNodes.size());
				return;
			}
			ss.executeAction(MenuActionConstants.ACTION_INDENT, selectedRows);
			ss.restoreTaskRowSelection(taskNodes);
			getGraphicManager().traceUi("indent complete selectedTasks=" + taskNodes.size()
					+ " undo=" + canUndoState() + " redo=" + canRedoState());
		}
	}
	public boolean canMoveSelectedTasks(int direction) {
		SpreadSheet spreadSheet=getActiveSpreadSheet();
		return spreadSheet != null && spreadSheet.canMoveSelectedTaskRows(direction,false);
	}
	public void doMoveSelectedTasks(int direction) {
		SpreadSheet spreadSheet=getActiveSpreadSheet();
		if (spreadSheet != null) spreadSheet.moveSelectedTaskRowsFromCommand(direction);
	}
	public void doDelete() {
		SpreadSheet ss = getActiveSpreadSheet();
		if (ss !=null)
			ss.executeAction(MenuActionConstants.ACTION_DELETE);
	}

	public void doCut() {
		SpreadSheet ss = getActiveSpreadSheet();
		if (ss !=null)
			ss.performAction(MenuActionConstants.ACTION_CUT, new ActionEvent(ss,0,null));
			//NodeListTransferHandler.getCutAction(ss).actionPerformed(new ActionEvent(this,0,null));
			//ss.executeAction(SpreadSheet.CUT);
	}
	public void doCopy() {
		SpreadSheet ss = getActiveSpreadSheet();
		if (ss !=null)
			ss.performAction(MenuActionConstants.ACTION_COPY, new ActionEvent(ss,0,null));
			//NodeListTransferHandler.getCopyAction(ss).actionPerformed(new ActionEvent(this,0,null));
			//ss.executeAction(SpreadSheet.COPY);
	}
	public void doPaste() {
		SpreadSheet ss = getActiveSpreadSheet();
		if (ss !=null && canPasteIntoCurrentSelection())
			ss.performAction(MenuActionConstants.ACTION_PASTE, new ActionEvent(ss,0,null));
			//NodeListTransferHandler.getPasteAction(ss).actionPerformed(new ActionEvent(this,0,null));
			//ss.executeAction(SpreadSheet.PASTE);
	}
	public void doPasteInsert() {
		SpreadSheet ss = getActiveSpreadSheet();
		if (ss !=null && canPasteIntoCurrentSelection())
			ss.performAction(MenuActionConstants.ACTION_PASTE_INSERT, new ActionEvent(ss,0,null));
	}


	public GanttView getGanttView() {
		if (ganttView == null) {
			ganttView = new GanttView(this, graphicManager.getMenuManager(),mainView.getSynchronizer());
			ganttView.init(getTaskNodeModelCache(), getTaskModel(), coord);
			ganttView.setSpreadsheetGridVisible(getGraphicManager().getPreferences().isShowRowLines());
			Gantt gantt = ganttView.getGantt();
			if (gantt != null)
				gantt.getInteractor().setModeListener(statusBar::setMode);
			restoreWorkspaceFor(ganttView);
		}
		return ganttView;
	}

	public UsageDetailView getTaskUsageDetailView() {
		if (taskUsageDetailView == null) {
			taskUsageDetailView = new UsageDetailView(this, graphicManager
					.getMenuManager(),mainView.getSynchronizer());
			taskUsageDetailView.init(getTaskNodeModelCache(), true, coord,
					false,  ACTION_TASK_USAGE_DETAIL,addTransformerInitializationClosure());
			restoreWorkspaceFor(taskUsageDetailView);
		}
		return taskUsageDetailView;
	}

	public UsageDetailView getResourceUsageDetailView() {
		if (resourceUsageDetailView == null) {
			resourceUsageDetailView = new UsageDetailView(this, graphicManager
					.getMenuManager(),mainView.getSynchronizer());
			resourceUsageDetailView.init(getResourceNodeModelCache(), false,
					coord, false, ACTION_RESOURCE_USAGE_DETAIL,addTransformerInitializationClosure());
			restoreWorkspaceFor(resourceUsageDetailView);
		}
		return resourceUsageDetailView;
	}

	public PertView getPertView() {
		if (pertView == null) {
			pertView = new PertView(this, graphicManager.getMenuManager());
			pertView.init(getTaskNodeModelCache(), getTaskModel());
			restoreWorkspaceFor(pertView);
		}
		return pertView;
	}

	public TreeView getWBSView() {
		if (wbsView == null) {
			wbsView = new TreeView(this, graphicManager.getMenuManager());
			wbsView.init(getTaskNodeModelCache(), getTaskModel(), ACTION_WBS,addTransformerInitializationClosure());
			restoreWorkspaceFor(wbsView);
		}
		return wbsView;
	}

	public TreeView getRBSView() {
		if (rbsView == null) {
			rbsView = new TreeView(this, graphicManager.getMenuManager());
			rbsView.init(getResourceNodeModelCache(), getResourceModel(),
							ACTION_RBS,addTransformerInitializationClosure());
			restoreWorkspaceFor(rbsView);
		}
		return rbsView;
	}

	public BaseView getReportView() {
		try {
			if (reportView == null) {
				try {
					ClassUtils.forName("net.sf.jasperreports.engine.JasperCompileManager");
				} catch (ClassNotFoundException missingCompiler) {
					return null;
				}
				Class clazz=ClassUtils.forName("com.microproject.reports.view.ReportView");
				reportView=(BaseView)clazz.getConstructor(new Class[]{DocumentFrame.class}).newInstance(new Object[]{this});
				clazz.getMethod("init", new Class[]{CoordinatesConverter.class}).invoke(reportView, new Object[]{coord});
				if (reportView!=null) restoreWorkspaceFor(reportView);
			}
		} catch (Exception e) {
			reportView=null;
		}
		return reportView;
	}

	public ChartView getChartView() {
		if (chartView == null) {
			chartView = new ChartView(this, ChartMode.REPORT_CHART, graphicManager.getMenuManager(),mainView.getSynchronizer(),addTransformerInitializationClosure());
			chartView.init(coord);
			restoreWorkspaceFor(chartView);
		}
		return chartView;
	}

	public ChartView getHistogramView() {
		if (histogramView == null) {
			histogramView = new ChartView(this, ChartMode.RESOURCE_GRAPH, graphicManager
					.getMenuManager(),mainView.getSynchronizer(),addTransformerInitializationClosure());
			histogramView.init(coord);
			restoreWorkspaceFor(histogramView);
		}
		return histogramView;
	}

	public ResourceView getResourceView() {
		if (resourceView == null) {
			resourceView = new ResourceView(getResourceNodeModelCache(),
					getResourceModel(), project.getResourcePool(),!Environment.isProjectLibre() &&project.isReadOnly(),graphicManager.isEditingMasterProject());
			restoreWorkspaceFor(resourceView);
		}
		return resourceView;
	}

	public ProjectView getProjectView() {
		if (projectView == null) {
			Portfolio portfolio = getGraphicManager().getProjectFactory().getPortfolio();
			projectView = new ProjectView(portfolio.getNodeModel(), portfolio);
			restoreWorkspaceFor(projectView);
		}
		return projectView;
	}

	public DockableProjectToolView getTimelineView() {
		if (timelineView == null) {
			timelineView = new DockableProjectToolView(project, ACTION_TIMELINE,
				TimelineDialogBox.createEmbeddedPanel(getGraphicManager().getFrame(), project));
			restoreWorkspaceFor(timelineView);
		}
		return timelineView;
	}

	public DockableProjectToolView getTeamPlannerView() {
		if (teamPlannerView == null) {
			teamPlannerView = new DockableProjectToolView(project, ACTION_DELEGATE_TASKS,
				TeamPlannerDialogBox.createEmbeddedPanel(getGraphicManager().getFrame(), project));
			restoreWorkspaceFor(teamPlannerView);
		}
		return teamPlannerView;
	}

	public UsageDetailView getTaskUsageView() {
		if (taskUsageView == null) {
			taskUsageView = new UsageDetailView(this, graphicManager
					.getMenuManager(),mainView.getSynchronizer());
			taskUsageView.init(getTaskNodeModelCache(), true, coord, true,
					ACTION_TASK_USAGE,addTransformerInitializationClosure());
			restoreWorkspaceFor(taskUsageView);
		}
		return taskUsageView;
	}

	public UsageDetailView getResourceUsageView() {
		if (resourceUsageView == null) {
			resourceUsageView = new UsageDetailView(this, graphicManager
					.getMenuManager(),mainView.getSynchronizer());
			resourceUsageView.init(getResourceNodeModelCache(), false, coord,
					true, ACTION_RESOURCE_USAGE,addTransformerInitializationClosure());
			restoreWorkspaceFor(resourceUsageView);
		}
		return resourceUsageView;
	}

	public void toggleMinWidth() {
		boolean normalMinWidth = (activeTopView == null || activeTopView.hasNormalMinWidth())
			&& (activeBottomView == null || activeBottomView.hasNormalMinWidth());
		coord.toggleMinWidth(normalMinWidth);
	}


	public boolean activateView(String viewName) {
		if (viewName == null || viewName.length() == 0) {
			activateGanttView();
			return true;
		}
		BaseView topView = null;
		BaseView bottomView = null;
		boolean top = true;
		if (viewName.equals(ACTION_GANTT)) {
			activateGanttView();
			return top;
		} else if (viewName.equals(ACTION_TRACKING_GANTT)) {
			activateTrackingGanttView();
			return top;
		} else if (viewName.equals(ACTION_TASK_USAGE_DETAIL))
			topView = getTaskUsageDetailView();
		else if (viewName.equals(ACTION_RESOURCE_USAGE_DETAIL))
			topView = getResourceUsageDetailView();
		else if (viewName.equals(ACTION_NETWORK))
			topView = getPertView();
		else if (viewName.equals(ACTION_WBS))
			topView = getWBSView();
		else if (viewName.equals(ACTION_RBS))
			topView = getRBSView();
		else if (viewName.equals(ACTION_REPORT)) {
			topView = getReportView();
			if (topView == null)
				topView = getGanttView();
		}
		else if (viewName.equals(ACTION_RESOURCES))
			topView = getResourceView();
		else if (viewName.equals(ACTION_PROJECTS))
			topView = getProjectView();
		else if (viewName.equals(ACTION_TIMELINE))
			topView = getTimelineView();
		else if (viewName.equals(ACTION_DELEGATE_TASKS))
			topView = getTeamPlannerView();
		else if (viewName.equals(ACTION_HISTOGRAM)) {
//			if (activeBottomView != getHistogramView())
				 bottomView = getHistogramView();
//			else
//				deactivateBottomView();
		}
		else if (viewName.equals(ACTION_CHARTS))
			bottomView = getChartView();
		else if (viewName.equals(ACTION_TASK_USAGE))
			bottomView = getTaskUsageView();
		else if (viewName.equals(ACTION_RESOURCE_USAGE))
			bottomView = getResourceUsageView();
		else if (viewName.equals(ACTION_NO_SUB_WINDOW))
			deactivateBottomView();

		if (topView != null) {
			activateTopView(topView,viewName);
		}

		if (bottomView != null) {
			boolean clickNew = true;
			if (!Environment.isNewLook()) {
				clickNew = bottomView != activeBottomView; // if clicked on a non pressed button
				deactivateBottomView();
			}
			if (clickNew)
				activateBottomView(bottomView,viewName);
			top = false;
		}
		return top;
	}
	private void activateTopView(BaseView view,String viewName) {
		showWaitCursor(true);
		if (findDialog != null)
			findDialog.setVisible(false);
		CommonSpreadSheet ss = getTopSpreadSheet();
		if (ss != null)
			ss.removeSelectionNodeListener(this);
		deactivateTopView();
		activeTopView = view;
		mainView.setTop((Component)view);
		view.onActivate(true);
		ss = view.getSpreadSheet();
		if (ss != null)
			ss.addSelectionNodeListener(this);

		toggleMinWidth();
		menuManager.setActionSelected(viewName,true);
		lastTopButton = viewName;
		setComboBoxesViewName(view.getViewName());
		getGraphicManager().setTaskInformation(view.showsTasks(),view.showsResources());
		refreshUndoButtons();
		getGraphicManager().setEnabledDocumentMenuActions(true);
		getGraphicManager().updateRibbonContext(viewName);
		showWaitCursor(false);
//this doesn't have any effect		setFrameIcon(menuManager.getToolButtonFromId(viewName).getIcon());

	}

	void deactivateTopView() {
		if (activeTopView == null)
			return;
		if (lastTopButton != null)
			menuManager.setActionSelected(lastTopButton,false);
		// deactivate current ss listener
		CommonSpreadSheet ss = getTopSpreadSheet();
		if (ss != null)
			ss.removeSelectionNodeListener(this);

		mainView.removeTop();
		activeTopView.onActivate(false);
		activeTopView = null;
		toggleMinWidth();
		getGraphicManager().setTaskInformation(false, false);
		refreshUndoButtons();
		getGraphicManager().setEnabledDocumentMenuActions(false);
	}

	public void activateResourceView() {
		activateTopView(getResourceView(),ACTION_RESOURCES);
	}

	public void activateGanttView() {
		if (ganttColumns != null)
			getGanttView().setColumns(ganttColumns);
		getGanttView().setBarStyles("standard");
		getGanttView().setTracking(false);
		activateTopView(getGanttView(),ACTION_GANTT);
	}

	public ArrayList getGanttColumns() {
		return ganttColumns;
	}
	public void activateTrackingGanttView() {
		getGanttView().setBarStyles("Tracking");
		getGanttView().setTracking(true);
		ganttColumns = getGanttView().setColumns("Spreadsheet.Task.tracking");
		activateTopView(getGanttView(),ACTION_TRACKING_GANTT);
	}

	public void activateBottomView(BaseView view,String viewName) {
		boolean same = viewName.equals(lastBottomButton);
		if (same)
			return;
		if (ACTION_NO_SUB_WINDOW.equals(viewName))
			deactivateBottomView();
		else {
			mainView.removeBottom();
		}
		activeBottomView = view;
		view.onActivate(true);
		lastBottomButton = viewName;
		mainView.setBottom((Component) view);
		toggleMinWidth();
		menuManager.setActionSelected(viewName,true);
		refreshUndoButtons();

		if (view instanceof SelectionNodeListener selectionListener)
			pushCurrentSelectionToBottomView(selectionListener);

	}

	private void pushCurrentSelectionToBottomView(SelectionNodeListener selectionListener) {
		if (selectionListener == null)
			return;
		CommonSpreadSheet topSpreadSheet = getTopSpreadSheet();
		if (topSpreadSheet != null) {
			selectionListener.selectionChanged(new SelectionNodeEvent(
				topSpreadSheet,
				SelectionNodeEvent.SELECTION_CHANGED,
				topSpreadSheet.getSelectedNodes(),
				topSpreadSheet.getCurrentRowNode(),
				topSpreadSheet.getSpreadSheetCategory()));
			return;
		}
		if (lastSelectionEvent != null)
			selectionListener.selectionChanged(lastSelectionEvent);
	}

	public void deactivateBottomView() {
		if (activeBottomView == null)
			return;
		menuManager.setActionSelected(ACTION_NO_SUB_WINDOW,true);
		if (lastBottomButton != null)
			menuManager.setActionSelected(lastBottomButton,false);
		activeBottomView.onActivate(false);
		lastBottomButton = ACTION_NO_SUB_WINDOW;
		mainView.removeBottom();
		activeBottomView = null;
		toggleMinWidth();
		refreshUndoButtons();
	}


	/**
	 * @return Returns the mainView.
	 */
	public MainView getMainView() {
		return mainView;
	}

	/**
	 * @return Returns the project.
	 */
	public Project getProject() {
		return project;
	}

	/**
	 * @return Returns the topSpreadSheet.
	 */
	public SpreadSheet getTopSpreadSheet() {
		CommonSpreadSheet ss = null;
		if (activeTopView != null)
			ss = activeTopView.getSpreadSheet();
		return (SpreadSheet) ss;
	}

	/**
	 * @return Returns the topSpreadSheet.
	 */
	public SpreadSheet getActiveSpreadSheet() {
		SpreadSheet ss = getTopSpreadSheet();
		if (ss == null) {
			if (activeBottomView != null)
				ss = activeBottomView.getSpreadSheet();

		}
		return ss;
	}


	protected SelectionNodeEvent lastSelectionEvent=null;
	/**
	 * React to selection changed events and forward them on to any bottom
	 * window
	 */
	public void selectionChanged(SelectionNodeEvent e) {
		lastSelectionEvent=e;
		statusBar.setSelectedCount(e.getNodes() == null ? 0 : e.getNodes().size());
		Component bottom = mainView.getBottomComponent();
		if (bottom != null && bottom instanceof SelectionNodeListener)
			((SelectionNodeListener) bottom).selectionChanged(e);
		graphicManager.selectionChanged(e);
	}

	/**
	 * @return Returns the menuManager.
	 */
	public MenuManager getMenuManager() {
		return menuManager;
	}

	private boolean hasAtLeastOneTaskSelected() {
		return DataUtils.nodeListContainsImplOfType(getSelectedNodes(true), Task.class);
	}

	protected List<Node> getSelectedTaskNodes(boolean excludeReadOnly, boolean allowMixedSelection) {
		List<Node> nodes = getSelectedNodes(excludeReadOnly);
		if (nodes == null || nodes.isEmpty())
			return Collections.emptyList();
		ArrayList<Node> taskNodes = new ArrayList<>(nodes.size());
		for (Node node : nodes) {
			if (node == null)
				continue;
			Object impl = node.getImpl();
			if (impl instanceof Task) {
				taskNodes.add(node);
			} else if (!allowMixedSelection) {
				return Collections.emptyList();
			}
		}
		return taskNodes;
	}

	protected boolean hasTaskSelection(boolean excludeReadOnly, int minCount, boolean allowMixedSelection) {
		return getSelectedTaskNodes(excludeReadOnly, allowMixedSelection).size() >= minCount;
	}

	protected boolean canPasteIntoCurrentSelection() {
		SpreadSheet spreadSheet = getActiveSpreadSheet();
		return spreadSheet != null && spreadSheet.getCurrentRow() >= 0;
	}

	private void focusSingleTaskSelectionAnchor(SpreadSheet spreadSheet) {
		if (spreadSheet == null)
			return;
		int row = spreadSheet.getCurrentRow();
		if (row < 0 || row >= spreadSheet.getRowCount())
			return;
		int column = spreadSheet.isEditing() ? spreadSheet.getEditingColumn() : spreadSheet.getSelectedColumn();
		if (column < 0)
			column = 0;
		if (column >= spreadSheet.getColumnCount())
			column = Math.max(0, spreadSheet.getColumnCount() - 1);
		spreadSheet.getSelectionModel().setSelectionInterval(row, row);
		if (spreadSheet.getColumnCount() > 0) {
			spreadSheet.getColumnModel().getSelectionModel().setSelectionInterval(column, column);
		}
	}
	private void forTasksDo(Consumer<Object> closure, boolean all) {
		DataUtils.forAllDo(closure, all, project.getTaskOutlineIterator(),
				getSelectedNodes(true), Task.class);
	}

	public void undoableEditHappened(UndoableEditEvent e) {
		refreshUndoButtons();
	}

	protected UndoController currentUndoController=null;
	public UndoController getUndoController(){
		return currentUndoController;
	}

	void refreshViewButtons(boolean enable) {
		if (enable)
			refreshUndoButtons();
		if (lastTopButton != null)
			menuManager.setActionSelected(lastTopButton,enable);
		if (lastBottomButton != null)
			menuManager.setActionSelected(lastBottomButton,enable);

	}
	public void refreshUndoButtons() {
		UndoController undoController = null;
		if (activeTopView != null)
			undoController = activeTopView.getUndoController();

		if (undoController!=currentUndoController){
			if (currentUndoController!=null)
				currentUndoController.getEditSupport().removeUndoableEditListener(this);
			if (undoController!=null)
				undoController.getEditSupport().addUndoableEditListener(this);
			currentUndoController=undoController;
		}

		boolean canUndo = false;
		boolean canRedo = false;
//		String undoText = "";
//		String redoText = "";

		if (undoController != null){
			canUndo = undoController.canUndo();
			canRedo = undoController.canRedo();
//			undoText = undoController.getUndoManager().getUndoPresentationName();
//			redoText = undoController.getUndoManager().getRedoPresentationName();
		}
		menuManager.setActionEnabled(ACTION_UNDO,canUndo);
		menuManager.setActionEnabled(ACTION_REDO,canRedo);

	}

	public Node addNodeForImpl(Object impl) {
		return addNodeForImpl(impl,NodeModel.NORMAL);
	}
	public Node addNodeForImpl(Object impl,int eventType) {
		SpreadSheet spreadSheet = (SpreadSheet) getTopSpreadSheet();
		if (impl == null) {
			spreadSheet.executeAction(MenuActionConstants.ACTION_NEW);
			return null;
		} else {
			return spreadSheet.addNodeForImpl(impl,eventType);
		}
	}

/**
 * sees if currently selected row belongs to main project. used to see if can insert a subproject. subprojects can
 * only be inserted into master project
 * @return
 */	public boolean isCurrentRowInMainProject() {
        CommonSpreadSheet spreadSheet=getTopSpreadSheet();
        if (spreadSheet == null)
        	return true;
	    int row = spreadSheet.getCurrentRow();
	    if (row == -1)
	    	return true;
		Node current = spreadSheet.getCurrentRowNode();
        SpreadSheetModel model=(SpreadSheetModel)spreadSheet.getModel();
 		LinkedList previousNodes=model.getPreviousVisibleNodesFromRow(row);
		if (previousNodes == null)
			return true;
		previousNodes.add(current); // treat current node first since going backwards
		ListIterator i = previousNodes.listIterator(previousNodes.size());
		while (i.hasPrevious()) {
			Object o = ((Node)i.previous()).getImpl();
			if (o instanceof Task) {
				if (((Task)o).isInSubproject())
					return false;
				return project == ((Task)o).getOwningProject();
			}
		}

		return true;
	}

	public List getSelectedImpls(boolean excludeReadOnly) {
		return NodeList.nodeListToImplList(getSelectedNodes(excludeReadOnly));
	}


	public void finishAnyOperations() {
		if (!isActive())
			return;
		CommonSpreadSheet topSpreadSheet=getTopSpreadSheet();
		if (topSpreadSheet!=null)
			topSpreadSheet.finishCurrentOperations();
	}

	public void setComboBoxesViewName(String view) {
		if (!Environment.isPlugin() && filterToolBarManager != null)
			filterToolBarManager.setComboBoxesViewName(view);
	}

	public void showWaitCursor(boolean show) {
		getGraphicManager().showWaitCursor(show);
	}

	public void objectChanged(ObjectEvent objectEvent) { // rename tab if project changes
		if (objectEvent.getObject() instanceof Project && objectEvent.isUpdate()) { //Only for projects
			if (objectEvent.getField() == Configuration.getFieldFromId("Field.name")) {
				getGraphicManager().setTabNameAndTitle(this,project);
			}
//			else if (objectEvent.getField() == Configuration.getFieldFromId("Field.showProjectResourcesOnly")) {
//				for (ResourceInTeamFilter filter : resourcesInTeamFilters) {
//					filter.setFilterTeam(project.isShowProjectResourcesOnly());
//				}
//			}

		}
		else if (objectEvent.getObject() instanceof GlobalPreferences && objectEvent.isUpdate()) {
			getGraphicManager().applyPreferenceFont(getGraphicManager().getPreferences());
			//if (objectEvent.getField() == Configuration.getFieldFromId("Field.showProjectResourcesOnly")) {
				for (ResourceInTeamFilter filter : resourcesInTeamFilters) {
					filter.setFilterTeam(getGraphicManager().getPreferences().isShowProjectResourcesOnly());
				}
				ResourceInTeamFilter filter=getGraphicManager().getAssignmentDialogTransformerInitializationClosure();
				if (filter!=null){
					filter.setFilterTeam(getGraphicManager().getPreferences().isShowProjectResourcesOnly());
				}
				if (ganttView != null)
					ganttView.setSpreadsheetGridVisible(getGraphicManager().getPreferences().isShowRowLines());
			//}

		}

	}

	private ArrayList<ResourceInTeamFilter> resourcesInTeamFilters=new ArrayList<ResourceInTeamFilter>();

	public Consumer<Object> addTransformerInitializationClosure(){
		return new Consumer<Object>() { public void accept(Object arg) {
				ViewTransformer transformer=(ViewTransformer)arg;
		        NodeFilter hiddenFilter=transformer.getHiddenFilter();
		        if (hiddenFilter!=null&& hiddenFilter instanceof ResourceInTeamFilter){
		        	ResourceInTeamFilter rf=(ResourceInTeamFilter)hiddenFilter;
		        	rf.setFilterTeam(getGraphicManager().getPreferences().isShowProjectResourcesOnly());
		        	resourcesInTeamFilters.add(rf);
		        }
			}
		};
	}

	void onClose() {
		project = null; // get rid of reference
	}

	boolean isPrintable() {
		return activeTopView != null && activeTopView.isPrintable();
	}

	final CoordinatesConverter getCoord() {
		return coord;
	}

	public void nameChanged(final ProjectEvent e) {
		setTabNameAndTitle(e.getProject());
	}
	public void groupDirtyChanged(final ProjectEvent e) {
		setTabNameAndTitle(e.getProject());
		getGraphicManager().refreshSaveStatus(false);
	}
    void setTabNameAndTitle(Project project) {
    	boolean show = isShowTitleBar();

   		getGraphicManager().setTitle(false);
		setTabTitle(project.getName());
		setShowTitleBar(show);
//		if (parentFrame.getCurrentFrame() == this)
//			parentFrame.setTitle(Messages.getString("Text.ApplicationTitle") + " - " + project.getName());
    }
    String getTopViewId() {
    	if (lastTopButton == null)
    		return ACTION_GANTT;
    	return lastTopButton;
    }


	public void cleanUp() {
		logger.fine("Document Frame Cleanup");
		if (project != null) {
			project.removeProjectListener(this);
			project.removeObjectListener(this);
			project.removeScheduleListener(coord);
		}
		if (getUndoController() != null &&  getUndoController().getEditSupport() != null)
			getUndoController().getEditSupport().removeUndoableEditListener(this);
		if (coord != null)
			coord.removeTimeScaleListener(mainView);
    	forAllViews(new Consumer<Object>() { public void accept(Object v) {
				if (v != null)
					((BaseView)v).cleanUp();
			}});
    	resetViews();
    	if (jobQueue != null)
    		jobQueue.cancel();
    	jobQueue =null;
		project = null;
		coord = null;
		resetViews();
	}

	void resetViews() {
		ganttView = null;
		taskUsageDetailView = null;
		resourceUsageDetailView = null;
		pertView = null;
		wbsView = null;
		rbsView = null;
		chartView = null;
		histogramView = null;
		resourceView = null;
		projectView = null;
		taskUsageView = null;
		resourceUsageView = null;
		timelineView = null;
		teamPlannerView = null;
		reportView = null;
		activeTopView = null;
		activeBottomView = null;
	}
	private void forAllViews(Consumer<Object> c) {
		Object[] views = {
			ganttView,
			taskUsageDetailView,
			resourceUsageDetailView,
			pertView,
			wbsView,
			rbsView,
			chartView,
			histogramView,
			resourceView,
			projectView,
			taskUsageView,
			resourceUsageView,
			reportView,
			timelineView,
			teamPlannerView
		};
		ArrayUtils.forAllDo(views,c);
	}

	public void restoreWorkspace(WorkspaceSetting w, int context) {
		Workspace ws = (Workspace)w;
		workspace = ws;
		setMainView(false);
		if (project == null) {
			project = ProjectFactory.getInstance().findFromId(ws.projectId);
			if (project == null){
				LoadOptions opt=new LoadOptions();
				opt.setId(ws.projectId);
				opt.setSync(true);
				project = ProjectFactory.getInstance().openProject(opt);
			}
		}
		coord.restoreWorkspace(ws.getCoord(), context);

		activateView(resolveRestoredTopViewName(ws.topViewName));
		activateView(ws.bottomViewName);
		if (activeTopView == null) {
			activateGanttView();
		}
		mainView.restoreWorkspace(ws.mainView, context);

	}

	private String resolveRestoredTopViewName(String viewName) {
		if (viewName == null || viewName.length() == 0) {
			return ACTION_GANTT;
		}
		if (ACTION_REPORT.equals(viewName) && getReportView() == null) {
			return ACTION_GANTT;
		}
		return viewName;
	}

	private WorkspaceSetting restoreWorkspaceFor(BaseView view) {
		WorkspaceSetting ws = getWorkspaceFor(view);
		if (ws != null)
			view.restoreWorkspace(ws, SavableToWorkspace.VIEW);
		return ws;
	}
	private WorkspaceSetting getWorkspaceFor(BaseView view) {
		if (workspace == null || workspace.views == null)
			return null;
		return (WorkspaceSetting) workspace.views.get(view.getViewName());
	}

	private void saveViewWorkspace(Workspace ws, String name, BaseView view) {
		if (view != null)
			ws.views.put(name, view.createWorkspace(SavableToWorkspace.VIEW));
	}
	public WorkspaceSetting createWorkspace(int context) {
		Workspace ws = new Workspace();
		ws.projectId = getProject().getUniqueId();
		ws.topViewName = getTopViewId();
		ws.bottomViewName = lastBottomButton;
		ws.coord = coord.createWorkspace(context);
		ws.mainView = mainView.createWorkspace(context);
		ws.saveViewWorkspace(ACTION_GANTT,ganttView);
		ws.saveViewWorkspace(ACTION_TASK_USAGE_DETAIL,taskUsageDetailView);
		ws.saveViewWorkspace(ACTION_RESOURCE_USAGE_DETAIL,resourceUsageDetailView);
		ws.saveViewWorkspace(ACTION_NETWORK,pertView);
		ws.saveViewWorkspace(ACTION_WBS,wbsView);
		ws.saveViewWorkspace(ACTION_RBS,rbsView);
		ws.saveViewWorkspace(ACTION_CHARTS,chartView);
		ws.saveViewWorkspace(ACTION_HISTOGRAM,histogramView);
		ws.saveViewWorkspace(ACTION_RESOURCES,resourceView);
		ws.saveViewWorkspace(ACTION_PROJECTS,projectView);
		ws.saveViewWorkspace(ACTION_TASK_USAGE,taskUsageView);
		ws.saveViewWorkspace(ACTION_RESOURCE_USAGE,resourceUsageView);
		ws.saveViewWorkspace(ACTION_REPORT,reportView);
		ws.saveViewWorkspace(ACTION_TIMELINE,timelineView);
		ws.saveViewWorkspace(ACTION_DELEGATE_TASKS,teamPlannerView);
		return ws;
	}

	public static class Workspace implements WorkspaceSetting  {
		private static final long serialVersionUID = 8836549717587108911L;
		long projectId;
		String topViewName;
		String bottomViewName;
		WorkspaceSetting coord;
		WorkspaceSetting mainView;
		HashMap views = new HashMap();
		public void saveViewWorkspace(String name, BaseView view) {
			if (views  == null)
				views = new HashMap();
			if (view != null)
				views.put(name, view.createWorkspace(SavableToWorkspace.VIEW));
		}
		public final String getBottomViewName() {
			return bottomViewName;
		}
		public final void setBottomViewName(String bottomViewName) {
			this.bottomViewName = bottomViewName;
		}
		public final String getTopViewName() {
			return topViewName;
		}
		public final void setTopViewName(String topViewName) {
			this.topViewName = topViewName;
		}
		public final long getProjectId() {
			return projectId;
		}
		public final void setProjectId(long projectId) {
			this.projectId = projectId;
		}
		public WorkspaceSetting getCoord() {
			return coord;
		}
		public void setCoord(WorkspaceSetting coord) {
			this.coord = coord;
		}
		public HashMap getViews() {
			return views;
		}
		public void setViews(HashMap views) {
			this.views = views;
		}
		public WorkspaceSetting getMainView() {
			return mainView;
		}
		public void setMainView(WorkspaceSetting mainView) {
			this.mainView = mainView;
		}
	}

	public FilterToolBarManager getFilterToolBarManager() {
		return filterToolBarManager;
	}

	public BaseView getActiveBottomView() {
		return activeBottomView;
	}

	public BaseView getActiveTopView() {
		return activeTopView;
	}

	public void setMenuItem(JRadioButtonMenuItem mi) {
		this.menuItem = mi;

	}

	public JRadioButtonMenuItem getMenuItem() {
		return menuItem;
	}
	public void setActive(boolean active) {
		super.setActive(active);
		if (isEditingResourcePool()) {
			getGraphicManager().setAllButResourceDisabled(true);

		} else {
			getGraphicManager().setAllButResourceDisabled(false);
		}

	}

	boolean isEditingResourcePool() {
		return project != null && (project.isResourcePoolProject()
				|| (project.isMaster() && !project.isLocal()));

	}
	public void doFind(Searchable searchable, Field field) {
		doFind(searchable, field, null);
	}
	public void doFind(Searchable searchable, Field field, String initialQuery) {
		if (!isActive())
			return;

    	if (findDialog == null) {
    		findDialog = FindDialog.getInstance(this,searchable,field);
    		findDialog.pack();
    		findDialog.setModal(false);
		}
		findDialog.init(searchable, field, initialQuery);
    	findDialog.setLocationRelativeTo(this);//to center on screen
        findDialog.setVisible(true);

	}

	public void objectSelected(ObjectSelectionEvent e) {
		CommonSpreadSheet spreadSheet = getTopSpreadSheet();
		spreadSheet.selectObject(e.getObject());
		doScrollToTask();
	}
}

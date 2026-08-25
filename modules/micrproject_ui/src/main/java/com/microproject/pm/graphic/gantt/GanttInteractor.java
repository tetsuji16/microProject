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
package com.microproject.pm.graphic.gantt;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.undo.UndoableEditSupport;

import com.microproject.application.task.TaskCommandGateway;
import com.microproject.application.task.TaskCommandResult;
import com.microproject.application.task.TaskCommands.TaskDependencyCommand;
import com.microproject.application.task.TaskCommands.TaskScheduleDragCommand;
import com.microproject.application.task.TaskCommands.TaskProgressCommand;
import com.microproject.application.task.TaskCommands.TaskSplitCommand;
import com.microproject.pm.graphic.graph.GraphInteractor;
import com.microproject.pm.graphic.graph.GraphUI;
import com.microproject.pm.graphic.graph.GraphZone;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.model.cache.GraphicDependency;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.ViewNodeModelCache;
import com.microproject.pm.graphic.model.cache.TaskProjectionSnapshot;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.graphic.views.synchro.ScrollPaneSynchronizer;
import com.microproject.pm.scheduling.IntervalConsumer;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.dependency.HasDependencies;
import com.microproject.pm.scheduling.ConstraintType;
import com.microproject.pm.scheduling.Schedule;
import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.pm.scheduling.ScheduleService;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.ProjectTaskKey;
import com.microproject.pm.task.Task;
import com.microproject.undo.TaskConstraintEdit;
import com.microproject.util.Alert;
import com.microproject.util.ClassUtils;
import com.microproject.util.DateTime;

/**
 *
 */
public class GanttInteractor extends GraphInteractor{
	private static final long serialVersionUID = -555882007216388246L;
	protected static final int BAR_MOVE_START=4;
	protected static final int BAR_MOVE_END=5;
	protected static final int PROGRESS_BAR_MOVE=6;
	protected static final int SPLIT=7;
	private static final int HORIZONTAL_PAN_SPEED_MULTIPLIER = 2;

	protected Consumer<String> modeListener;

	public void setModeListener(Consumer<String> modeListener) {
		this.modeListener = modeListener;
	}

	protected void notifyMode() {
		notifyMode(modeText(state));
	}

	protected void notifyMode(String modeKey) {
		if (modeListener != null)
			modeListener.accept(modeKey);
	}

	private static String modeText(int state) {
		switch (state) {
		case BAR_MOVE_START:
		case BAR_MOVE_END:
			return "StatusBar.Resizing";
		case PROGRESS_BAR_MOVE:
			return "StatusBar.UpdatingProgress";
		case SPLIT:
			return "StatusBar.Splitting";
		case BAR_MOVE:
			return "StatusBar.Dragging";
		case LINK_CREATION:
		case LINK_SELECTION:
			return "StatusBar.Linking";
		default:
			return "StatusBar.Ready";
		}
	}

	protected ScheduleInterval selectedInterval;
	protected int selectedIntervalNumber;
	protected double t;
	private boolean panning;
	private Point panStartScreenPoint;
	private Point panStartViewPosition;
	private Point pendingPanScreenPoint;
	private boolean panUpdateScheduled;
	private long gestureDomainRevision = -1L;
	private long gestureTopologyRevision = -1L;
	private long gestureRenderRevision = -1L;
	private GestureTaskDraft gestureTaskDraft;
	private record GestureTaskDraft(ProjectTaskKey key, long expectedCompleted, long expectedTaskStart,
			long expectedTaskEnd, int expectedConstraintType, long expectedConstraintDate,
			int intervalNumber, long expectedIntervalStart, long expectedIntervalEnd) { }
	/**
	 *
	 */
	public GanttInteractor(GraphUI ui) {
		super(ui);
		popup=new GanttPopupMenu(this);
	}


    private class NodeSelectionIntervalConsumer implements IntervalConsumer{
    	private boolean consumed=false;
    	private GraphicNode node;
    	private double deltaResize0;
    	private double deltaResize1;
    	private double deltaOutside;
		private double completedDeltaT0;
		private double completedDeltaT1;
    	private ScheduleInterval completedInterval;
		private double t;
		private CoordinatesConverter coord;


    	public NodeSelectionIntervalConsumer init(double x,GraphicNode node){
    		this.t=getCoord().toTime(x);
    		consumed=false;
    		selectedInterval=null;
    		completedInterval=null;
    		selectedIntervalNumber=0;
//    		GraphicNode node=(GraphicNode)selected;
//    		long completedT=node.getCompleted();
    		coord=getCoord();
    		completedDeltaT0=coord.toDuration(config.getSelectionProgress0());
    		completedDeltaT1=coord.toDuration(config.getSelectionProgress1());
    		deltaResize0=coord.toDuration(config.getSelectionResize0());
    		deltaResize1=coord.toDuration(config.getSelectionResize1());
    		deltaOutside=coord.toDuration(config.getSelectionSquare());
    		this.node=node;
    		return this;
    	}
    	public void consumeInterval(ScheduleInterval interval){
    		if (consumed) return; //Consumer need to consume all the intervals
    		if (ClassUtils.isObjectReadOnly(((GraphicNode)selected).getNode().getImpl())) // pre
    			return;
			if (coord!=null){
				long completedT=((GraphicNode)selected).getCompleted();
	    		if (completedT>=interval.getStart()&&completedT<=interval.getEnd()){
	    			completedInterval=new ScheduleInterval(interval.getStart(),completedT);
	    			if (interval.getEnd()>interval.getStart()) completedInterval=coord.adaptSmallBarTimeInterval(completedInterval,node,config);
	    		}
				interval=coord.adaptSmallBarTimeInterval(interval, node,config);

			}
    		if (isMilestoneInterval(interval)) {
    			// Milestones have no resizable body. Treat the diamond as a movable point.
    			if (selectedIsNonSummaryNode()) {
    				state=BAR_MOVE;
    			}
    			selectedInterval=interval;
    			consumed=true;
    			return;
    		}
    		if (t<interval.getStart()&&t>=interval.getStart()-deltaOutside){
    			if (selectedIsNonSummaryNode()) state=BAR_MOVE_START;
    		}else if (t>interval.getEnd()&&t<=interval.getEnd()+deltaOutside){
    			if (selectedIsNonSummaryNode()) state=BAR_MOVE_END;
    		}else if (t<interval.getStart()||t>interval.getEnd()){
    			selectedIntervalNumber++;
    			return;
       		}else if (completedInterval!=null&&t>=completedInterval.getEnd()-completedDeltaT0&&t<=completedInterval.getEnd()+completedDeltaT1&&selectedZone!=null&&selectedZone.getZoneId()==GanttUI.PROGRESS_BAR_ZONE_ID){
       			if (selectedIsNonSummaryNode()) state=PROGRESS_BAR_MOVE;
    		}else if (t<=interval.getStart()+deltaResize0){
    			if (selectedIsNonSummaryNode()) state=BAR_MOVE_START;
    		}else if (t>=interval.getEnd()-deltaResize1){
    			if (selectedIsNonSummaryNode()) state=BAR_MOVE_END;
    		} else state= BAR_MOVE;
    		selectedInterval=interval;
			consumed=true;
     	}
    }
    private NodeSelectionIntervalConsumer nodeSelectionIntervalConsumer=new NodeSelectionIntervalConsumer();

    public void mousePressed(MouseEvent e) {
		gestureTaskDraft = null;
    	if (isReadOnly()) {
			// Read-only applies to bar editing, not to inspecting a task.  Keep
			// Task Information available for imported/read-only projects.
			if (SwingUtilities.isLeftMouseButton(e)) {
		select(e.getX(), e.getY());
		notifyBarSelection(e);
				if (e.getClickCount() == 2)
				openTaskInformationAt(e.getX(), e.getY());
			}
    		super.mousePressed(e);
    		return;
    	}
    	getGraph().requestFocusInWindow();
    	if (SwingUtilities.isRightMouseButton(e)) {
			select(e.getX(), e.getY());
			notifyBarSelection(e);
    		super.mousePressed(e);
    		return;
    	}
		if (!SwingUtilities.isLeftMouseButton(e)) {
    		super.mousePressed(e);
    		return;
		}
    	select(e.getX(), e.getY());
		captureGestureRevision();
		captureGestureTaskDraft();
    	notifyBarSelection(e);
    	if (selected == null) {
    		startPan(e);
    		notifyMode("StatusBar.Panning");
    		return;
    	}
		// The Gantt component begins its drag interaction from mousePressed and
		// can consume the following click notification.  Handle a double-click
		// here, after hit-testing has selected the bar, rather than relying on
		// mouseClicked() which is not reliably delivered for this component.
	if (e.getClickCount() == 2 && !(selected instanceof GraphicDependency)) {
			openTaskInformationAt(e.getX(), e.getY());
			return;
	}
		// MS Project opens a task-dependency dialog on a double-click of the
		// link line. A single click only selects the line; do not let the base
		// interactor treat it as a direct action.
		if (selected instanceof GraphicDependency && !opensDependencyProperties(e.getClickCount())) {
			notifyMode();
			return;
		}
    	super.mousePressed(e);
    	notifyMode();
    }

    public void mouseDragged(MouseEvent e) {
    	if (panning) {
    		updatePan(e);
    		e.consume();
    		return;
    	}
    	super.mouseDragged(e);
    }

    public void mouseReleased(MouseEvent e) {
    	if (panning) {
    		stopPan();
			gestureTaskDraft = null;
    		notifyMode("StatusBar.Ready");
    		e.consume();
    		return;
    	}
	getGraph().requestFocusInWindow();
		if (selected instanceof GraphicDependency && !opensDependencyProperties(e.getClickCount())) {
			state=NOTHING_SELECTED;
			gestureTaskDraft = null;
			notifyMode("StatusBar.Ready");
			return;
		}
		try {
			super.mouseReleased(e);
		} finally {
			gestureTaskDraft = null;
		}
    	notifyMode("StatusBar.Ready");
    }

    public void mouseClicked(MouseEvent e) {
		// Double-click handling is deliberately in mousePressed(); see above.
    }

    protected void computeNodeSelection(double x,double y){
		//would have prefered an iterator
    	GraphicNode node=(GraphicNode)selected;
		node.consumeIntervals(nodeSelectionIntervalConsumer.init(x,node));
    }


	protected Shape getBarShadowBounds(double x,double y){
		if (!(selected instanceof GraphicNode node)) {
			return null;
		}
		TaskProjectionSnapshot.Row value = projectionValue(node);
		return new GanttSelectionGeometrySupport(value, projectionRow(node), ((GanttUI)ui).getGanttRenderer().getBarGeometry(value == null ? null : value.key()), getCoord(), (GanttUI)ui, config, x0, x, state, selectedIntervalNumber, selectedInterval)
				.createBarShadowBounds();
    }
	protected Rectangle2D getLinkSelectionShadowBounds(GraphicNode node){
		TaskProjectionSnapshot.Row value = projectionValue(node);
		return new GanttSelectionGeometrySupport(value, projectionRow(node), ((GanttUI)ui).getGanttRenderer().getBarGeometry(value == null ? null : value.key()), getCoord(), (GanttUI)ui, config, x0, x0, state, selectedIntervalNumber, selectedInterval)
				.createLinkSelectionShadowBounds();
	}


    public CoordinatesConverter getCoord(){
    	return ((GanttUI)ui).getCoord();
    }

	protected void setLinkOrigin(){
    	GraphicNode node=(GraphicNode)selected;
		TaskProjectionSnapshot.Row value = projectionValue(node);
		GanttSelectionGeometrySupport geometry = new GanttSelectionGeometrySupport(value, projectionRow(node), ((GanttUI)ui).getGanttRenderer().getBarGeometry(value == null ? null : value.key()), getCoord(), (GanttUI)ui, config, x0, x0, state, selectedIntervalNumber, selectedInterval);
		x0link = geometry.getLinkOriginX();
		y0link = geometry.getLinkOriginY();

    }

    protected boolean allowLinkSelectionToMove(){
    	return beforeLinkState==BAR_MOVE||beforeLinkState==BAR_MOVE_START||beforeLinkState==BAR_MOVE_END;
    }

    protected boolean switchOnLinkCreation(double x, double y){
    	if (state==PROGRESS_BAR_MOVE) return false;
		GraphicNode node=(GraphicNode)selected;
		Object impl = node.getNode().getImpl();
		return impl instanceof HasDependencies &&
				((int)y)/((Gantt)getGraph()).getRowHeight()!=projectionRow(node) ;
    }

	private TaskProjectionSnapshot.Row projectionValue(GraphicNode node) {
		if (!(getGraph().getCache() instanceof ViewNodeModelCache cache) || node == null) return null;
		ViewNodeModelCache.InstalledProjectionSnapshot installed = cache.getInstalledProjectionSnapshot();
		int row = installed.topology().rowOf(node);
		TaskProjectionSnapshot.Row value = installed.values().rowAt(row);
		return row >= 0 && value != null && installed.topology().keyAt(row).equals(value.key()) ? value : null;
	}

	private int projectionRow(GraphicNode node) {
		return getGraph() instanceof Gantt gantt ? gantt.getProjectionRow(node) : -1;
	}

    public Cursor selectCursor(){
    	Cursor cursor=null;
    	switch (state) {
		case BAR_MOVE_START:
			cursor=new Cursor(Cursor.W_RESIZE_CURSOR);
			break;
		case BAR_MOVE_END:
			cursor=new Cursor(Cursor.E_RESIZE_CURSOR);
			break;
		case PROGRESS_BAR_MOVE:
			cursor=getProgressCursor();
			break;
		case SPLIT:
			cursor=getSplitCursor();
			break;
		}
    	if (cursor==null) super.selectCursor();
    	else getGraph().setCursor(cursor);
    	return cursor;
    }

    public boolean executeAction(double x,double y){
		if (selected==null || !canExecutePointerAction(state == LINK_CREATION, state == LINK_SELECTION, x0, x)) return false;
		if (isMutatingGesture() && !isGestureRevisionCurrent())
			return false;
		if (state==BAR_MOVE||state==BAR_MOVE_START||state==BAR_MOVE_END||state==PROGRESS_BAR_MOVE||state==SPLIT){
			if (!(selected instanceof GraphicNode)) return false;
			sourceNode=(GraphicNode)selected;
		}
    	UndoableEditSupport undoSupport = getUndoableEditSupport();
		boolean actionPerformed;
		switch (state) {
		case BAR_MOVE:
		case BAR_MOVE_START:
		case BAR_MOVE_END:
			actionPerformed = applyIntervalDrag((long)getCoord().toDuration(x-x0),undoSupport);
			break;
		case PROGRESS_BAR_MOVE:
			actionPerformed = applyProgressDrag((long)getCoord().toTime(x),undoSupport);
			break;
		case LINK_CREATION:
			actionPerformed = createDependencyLink();
			break;
		case LINK_SELECTION:
			showDependencyPropertiesDialog((GraphicDependency)selected, gestureDomainRevision);
			return true;
		case SPLIT:
			long t=(long)getCoord().toTime(x);
			actionPerformed = applySplit(t);
			break;
		default:
			return false;
		}
		// Every mutating Gantt gesture goes through this one gate.  This prevents
		// new gesture types from silently omitting the root-pane Ctrl+Z refresh.
		return actionPerformed;
    }

	static boolean hasMeaningfulDrag(boolean linkCreation, double startX, double endX) {
		// Link creation can be a vertical drag between bars on the same date.
		return linkCreation || endX != startX;
	}

	static boolean canExecutePointerAction(boolean linkCreation, boolean directAction, double startX, double endX) {
		// A dependency line opens its properties dialog on a click. It is not a
		// drag gesture, so it must not inherit the bar-drag distance guard.
		return directAction || hasMeaningfulDrag(linkCreation, startX, endX);
	}

	static boolean opensDependencyProperties(int clickCount) {
		return clickCount >= 2;
	}

	private boolean createDependencyLink() {
		if (sourceNode == null || destinationNode == null
				|| !(sourceNode.getNode().getImpl() instanceof Task predecessor)
				|| !(destinationNode.getNode().getImpl() instanceof Task successor))
			return false;
		ProjectTaskKey predecessorKey = ProjectTaskKey.from(predecessor).orElse(null);
		ProjectTaskKey successorKey = ProjectTaskKey.from(successor).orElse(null);
		if (predecessorKey == null || successorKey == null)
			return false;
		TaskCommandResult result = ((Gantt)getGraph()).getTaskCommandGateway().createDependency(
				new TaskDependencyCommand(predecessorKey, successorKey, DependencyType.FS, 0L, gestureDomainRevision));
		if (result.failure() != null && result.failure().getMessage() != null)
			Alert.error(result.failure().getMessage());
		return result.committed();
	}

	private void captureGestureRevision() {
		gestureDomainRevision = -1L;
		gestureTopologyRevision = -1L;
		gestureRenderRevision = -1L;
		Project project = getGraph().getProject();
		if (project == null || !(getGraph().getCache() instanceof ViewNodeModelCache cache)) return;
		TaskProjectionSnapshot values = cache.getInstalledProjectionSnapshot().values();
		if (values.domainRevision() != project.getDomainChangeJournal().revision()) return;
		gestureDomainRevision = values.domainRevision();
		gestureTopologyRevision = values.topologyRevision();
		gestureRenderRevision = values.renderRevision();
	}

	/** Captures every optimistic-lock value at press time, before a long drag begins. */
	private void captureGestureTaskDraft() {
		gestureTaskDraft = null;
		if (!(selected instanceof GraphicNode node) || node.getNode() == null
				|| !(node.getNode().getImpl() instanceof Task task))
			return;
		ProjectTaskKey key = ProjectTaskKey.from(task).orElse(null);
		if (key == null)
			return;
		long intervalStart = selectedInterval == null ? task.getStart() : selectedInterval.getStart();
		long intervalEnd = selectedInterval == null ? task.getEnd() : selectedInterval.getEnd();
		gestureTaskDraft = new GestureTaskDraft(key, ScheduleService.getInstance().getCompleted(task),
				task.getStart(), task.getEnd(), task.getConstraintType(), task.getConstraintDate(),
				selectedIntervalNumber, intervalStart, intervalEnd);
	}

	private boolean isGestureRevisionCurrent() {
		Project project = getGraph().getProject();
		if (project == null || project.getDomainChangeJournal().revision() != gestureDomainRevision)
			return false;
		if (!(getGraph().getCache() instanceof ViewNodeModelCache cache)) return false;
		TaskProjectionSnapshot values = cache.getInstalledProjectionSnapshot().values();
		return gestureRevisionsMatch(gestureDomainRevision, gestureTopologyRevision, gestureRenderRevision,
				values.domainRevision(), values.topologyRevision(), values.renderRevision());
	}

	static boolean gestureRevisionsMatch(long capturedDomain, long capturedTopology, long capturedRender,
			long installedDomain, long installedTopology, long installedRender) {
		return capturedDomain == installedDomain && capturedTopology == installedTopology
				&& capturedRender == installedRender;
	}

	private boolean isMutatingGesture() {
		return state == BAR_MOVE || state == BAR_MOVE_START || state == BAR_MOVE_END
				|| state == PROGRESS_BAR_MOVE || state == SPLIT || state == LINK_CREATION
				|| state == LINK_SELECTION;
	}

    private boolean applyIntervalDrag(long dt, UndoableEditSupport undoSupport) {
		GestureTaskDraft draft = gestureTaskDraft;
		if (draft == null || selectedInterval == null) return false;
		long start=draft.expectedIntervalStart();
		long end=draft.expectedIntervalEnd();
		long expectedStart = start;
		long expectedEnd = end;
    	switch (state) {
		case BAR_MOVE:
			start+=dt;
			end+=dt;
			break;
		case BAR_MOVE_START:
			start+=dt;
			break;
		case BAR_MOVE_END:
			end+=dt;
			break;
		default:
			return false;
		}
		if (!changesIntervalAtHourPrecision(selectedInterval, start, end)) {
			// Do not turn a sub-hour, visually ineffective drag into a constraint
			// edit.  The old ordering changed SNET/FNLT before ScheduleService
			// rejected the rounded no-op interval.
			return false;
		}
		boolean updateConstraint = shouldUpdateTaskConstraint();
		int targetConstraintType = updateConstraint ? getConstraintTypeForDrag() : ConstraintType.ASAP;
		return ((Gantt)getGraph()).getTaskCommandGateway().dragSchedule(new TaskScheduleDragCommand(draft.key(),
				draft.intervalNumber(), expectedStart, expectedEnd, start, end, draft.expectedConstraintType(),
				draft.expectedConstraintDate(), updateConstraint, targetConstraintType, gestureDomainRevision)).committed();
    }

	static boolean changesIntervalAtHourPrecision(ScheduleInterval original, long start, long end) {
		return original != null
				&& (original.getStart() != DateTime.hourFloor(start)
						|| original.getEnd() != DateTime.hourFloor(end));
	}

	private boolean applyProgressDrag(long completed, UndoableEditSupport undoSupport) {
		GestureTaskDraft draft = gestureTaskDraft;
		return draft != null && ((Gantt)getGraph()).getTaskCommandGateway().updateProgress(new TaskProgressCommand(
				draft.key(), draft.expectedCompleted(), completed, gestureDomainRevision)).committed();
	}

	private boolean applySplit(long splitAt) {
		GestureTaskDraft draft = gestureTaskDraft;
		return draft != null && ((Gantt)getGraph()).getTaskCommandGateway().split(new TaskSplitCommand(
				draft.key(), draft.expectedTaskStart(), draft.expectedTaskEnd(), splitAt,
				gestureDomainRevision)).committed();
	}

    private Schedule getSourceSchedule() {
    	return (Schedule)sourceNode.getNode().getImpl();
    }

    private Task getSourceTask() {
    	Object impl = sourceNode == null ? null : sourceNode.getNode().getImpl();
    	return impl instanceof Task ? (Task) impl : null;
    }

    private boolean shouldUpdateTaskConstraint() {
    	return sourceNode != null
    			&& getSourceTask() != null
    			&& selectedIntervalNumber == 0
    			&& (state == BAR_MOVE || state == BAR_MOVE_START || state == BAR_MOVE_END);
    }

	private int getConstraintTypeForDrag() {
		return ConstraintType.SNET;
	}

    private boolean isMilestoneInterval(ScheduleInterval interval) {
    	if (interval == null || selected == null || !(selected instanceof GraphicNode)) {
    		return false;
    	}
    	Object impl = ((GraphicNode) selected).getNode().getImpl();
    	return impl instanceof Task && ((Task) impl).isMilestone() && interval.getStart() == interval.getEnd();
    }

    private UndoableEditSupport getUndoableEditSupport() {
    	if (ui.getGraph().getProject().getUndoController() == null) {
    		return null;
    	}
    	return ui.getGraph().getProject().getUndoController().getEditSupport();
    }

    public void setSplitMode(){
    	state=SPLIT;
    	selectCursor();
    }

    private void startPan(MouseEvent e) {
    	panning = true;
    	panStartScreenPoint = getScreenPoint(e);
    	JViewport viewport = getViewport();
    	panStartViewPosition = viewport == null ? null : viewport.getViewPosition();
    	selection = false;
    	state = NOTHING_SELECTED;
    	ScrollPaneSynchronizer.invalidateZoomRestore(getGraph());
    	getGraph().setCursor(new Cursor(Cursor.MOVE_CURSOR));
    }

    private void updatePan(MouseEvent e) {
    	pendingPanScreenPoint = getScreenPoint(e);
    	if (panUpdateScheduled) {
    		return;
    	}
    	panUpdateScheduled = true;
    	SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				applyPendingPan();
			}
		});
    }

    private void applyPendingPan() {
    	panUpdateScheduled = false;
    	JViewport viewport = getViewport();
    	if (!panning || viewport == null || panStartScreenPoint == null || panStartViewPosition == null || pendingPanScreenPoint == null) {
    		return;
    	}

    	Point viewPosition = new Point(panStartViewPosition);
    	viewPosition.x -= (pendingPanScreenPoint.x - panStartScreenPoint.x) * HORIZONTAL_PAN_SPEED_MULTIPLIER;
    	viewPosition.y -= pendingPanScreenPoint.y - panStartScreenPoint.y;
    	clampViewPosition(viewport, viewPosition);
    	viewport.setViewPosition(viewPosition);
    }

    private void stopPan() {
    	applyPendingPan();
    	panning = false;
    	selection = true;
    	panStartScreenPoint = null;
    	panStartViewPosition = null;
    	pendingPanScreenPoint = null;
    	panUpdateScheduled = false;
    	reset();
    	selectCursor();
    }

    private Point getScreenPoint(MouseEvent e) {
    	return new Point(e.getXOnScreen(), e.getYOnScreen());
    }

    private JViewport getViewport() {
    	if (getGraph().getParent() instanceof JViewport) {
    		return (JViewport) getGraph().getParent();
    	}
    	return null;
    }

    private void clampViewPosition(JViewport viewport, Point viewPosition) {
    	Dimension viewSize = viewport.getViewSize();
    	Dimension extentSize = viewport.getExtentSize();
    	int maxX = Math.max(0, viewSize.width - extentSize.width);
    	int maxY = Math.max(0, viewSize.height - extentSize.height);
    	viewPosition.x = clamp(viewPosition.x, 0, maxX);
    	viewPosition.y = clamp(viewPosition.y, 0, maxY);
    }

    private int clamp(int value, int min, int max) {
    	if (value < min) {
    		return min;
    	}
    	if (value > max) {
    		return max;
    	}
    	return value;
    }

    private void openTaskInformationAt(int x, int y) {
    	GraphZone clickedZone = ui.getObjectAt(x, y);
    	Object clickedObject = clickedZone == null ? null : clickedZone.getObject();
		// A Gantt bar can select its node even when the renderer has no zone at
		// the exact click coordinate (notably narrow bars and endpoint markers).
		// Prefer the hit-tested object but fall back to that selection.
		GraphicNode graphicNode = clickedObject instanceof GraphicNode
				? (GraphicNode) clickedObject
				: selected instanceof GraphicNode ? (GraphicNode) selected : null;
		if (graphicNode == null) {
			return;
		}
		Object impl = graphicNode.getNode().getImpl();
		if (!(impl instanceof Task task)) {
			return;
		}
		GraphicManager graphicManager = GraphicManager.getInstance(getGraph());
		if (graphicManager != null)
			// Finish the graph's press/release sequence before showing a dialog.
			// Opening it during mousePressed competes with the drag/selection cleanup.
			SwingUtilities.invokeLater(() -> graphicManager.doInformationDialog(task, false));
    }

    Task getSelectedTask() {
		if (!(selected instanceof GraphicNode graphicNode))
			return null;
		Object impl = graphicNode.getNode().getImpl();
		return impl instanceof Task task ? task : null;
    }

    protected void select(int x,int y){
    	if (selection){
    		selectedZone=ui.getObjectAt(x,y);
    		selected=selectedZone==null?null:selectedZone.getObject();
    		if (state==SPLIT) return;
	    	if (selected==null ){
	    		state=NOTHING_SELECTED;
	    	}else{
	    		 findState(x,y);
	    	}
	    	selectCursor();
    	}
    }

    /**
     * Informs the Gantt that the chart was clicked so the view can keep the
     * task table selection in sync with the chart, matching Microsoft Project:
     * plain click selects the task, Ctrl/Cmd+click toggles it, Shift+click
     * extends the selection, and a left click on empty chart space clears the
     * selection.  Only fired on an actual press (not on hover), so moving the
     * pointer over bars never changes the table selection.
     */
    private void notifyBarSelection(MouseEvent e){
    	if (!(getGraph() instanceof Gantt gantt)) return;
    	GraphicNode node = selected instanceof GraphicNode graphicNode ? graphicNode : null;
    	boolean leftClick = e != null && SwingUtilities.isLeftMouseButton(e);
    	// A right click on empty space (or on a link) keeps the current
    	// selection; only left clicks on empty chart space clear it.
    	if (node == null && !(leftClick && selected == null)) return;
		long domainRevision=-1L;
		long topologyRevision=-1L;
		if (gantt.getCache() instanceof ViewNodeModelCache cache) {
			var installed=cache.getInstalledProjectionSnapshot();
			domainRevision=installed.topology().domainRevision();
			topologyRevision=installed.topology().topologyRevision();
		}
		gantt.notifyBarSelection(new Gantt.BarClick(gantt.getProjectionRowKey(node), domainRevision,
				topologyRevision, isToggleModifier(e), e != null && e.isShiftDown()));
    }

    private static boolean isToggleModifier(MouseEvent e){
    	return e != null && (e.isControlDown() || e.isMetaDown());
    }

    protected boolean isMove(){
    	return state==BAR_MOVE||state==BAR_MOVE_END||state==BAR_MOVE_START||state==PROGRESS_BAR_MOVE;
    }
    protected boolean isDirectAction(){
    	return state==SPLIT||super.isDirectAction();
    }
    protected boolean isZoomRestoreInvalidatingDirectAction(){
    	return state==SPLIT;
    }
    protected boolean isRepaintOnRelease(){
    	return state==BAR_MOVE||state==BAR_MOVE_END||state==BAR_MOVE_START||state==PROGRESS_BAR_MOVE||state==LINK_CREATION;
    }

}


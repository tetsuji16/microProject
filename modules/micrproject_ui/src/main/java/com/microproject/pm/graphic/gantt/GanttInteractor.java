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

import com.microproject.pm.graphic.graph.GraphInteractor;
import com.microproject.pm.graphic.graph.GraphUI;
import com.microproject.pm.graphic.graph.GraphZone;
import com.microproject.pm.graphic.collaboration.CollaborationHelper;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.model.cache.GraphicDependency;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.graphic.views.synchro.ScrollPaneSynchronizer;
import com.microproject.association.InvalidAssociationException;
import com.microproject.pm.scheduling.IntervalConsumer;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import com.microproject.pm.dependency.HasDependencies;
import com.microproject.pm.scheduling.ConstraintType;
import com.microproject.pm.scheduling.Schedule;
import com.microproject.pm.scheduling.ScheduleInterval;
import com.microproject.pm.scheduling.ScheduleService;
import com.microproject.pm.task.Project;
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
		if (e.getClickCount() == 2) {
			openTaskInformationAt(e.getX(), e.getY());
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
    		notifyMode("StatusBar.Ready");
    		e.consume();
    		return;
    	}
    	getGraph().requestFocusInWindow();
    	super.mouseReleased(e);
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
		return new GanttSelectionGeometrySupport(node, getCoord(), (GanttUI)ui, config, x0, x, state, selectedIntervalNumber, selectedInterval)
				.createBarShadowBounds();
    }
	protected Rectangle2D getLinkSelectionShadowBounds(GraphicNode node){
		return new GanttSelectionGeometrySupport(node, getCoord(), (GanttUI)ui, config, x0, x0, state, selectedIntervalNumber, selectedInterval)
				.createLinkSelectionShadowBounds();
	}


    public CoordinatesConverter getCoord(){
    	return ((GanttUI)ui).getCoord();
    }

	protected void setLinkOrigin(){
    	GraphicNode node=(GraphicNode)selected;
		GanttSelectionGeometrySupport geometry = new GanttSelectionGeometrySupport(node, getCoord(), (GanttUI)ui, config, x0, x0, state, selectedIntervalNumber, selectedInterval);
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
				((int)y)/((Gantt)getGraph()).getRowHeight()!=node.getRow() ;
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
		if (selected==null || !hasMeaningfulDrag(state == LINK_CREATION, x0, x)) return false;
    	if (state==BAR_MOVE||state==BAR_MOVE_START||state==BAR_MOVE_END||state==PROGRESS_BAR_MOVE||state==SPLIT){
    		if (!(selected instanceof GraphicNode)) return false;
    		sourceNode=(GraphicNode)selected;
    		if (!CollaborationHelper.tryLockObject(null, sourceNode.getNode(), getGraph(), "edit")) {
    			return false;
    		}
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
			showDependencyPropertiesDialog((GraphicDependency)selected);
			return true;
		case SPLIT:
			long t=(long)getCoord().toTime(x);
			Schedule schedule = getSourceSchedule();
			actionPerformed = ScheduleService.getInstance().split(this,schedule,t,t,undoSupport);
			break;
		default:
			return false;
		}
		// Every mutating Gantt gesture goes through this one gate.  This prevents
		// new gesture types from silently omitting the root-pane Ctrl+Z refresh.
		return refreshUndoState(actionPerformed);
    }

	static boolean hasMeaningfulDrag(boolean linkCreation, double startX, double endX) {
		// Link creation can be a vertical drag between bars on the same date.
		return linkCreation || endX != startX;
	}

	private boolean createDependencyLink() {
		try {
			if (sourceNode != null && !CollaborationHelper.tryLockObject(null, sourceNode.getNode(), getGraph(), "link"))
				return false;
			if (destinationNode != null && !CollaborationHelper.tryLockObject(null, destinationNode.getNode(), getGraph(), "link"))
				return false;
			if (sourceNode == null || destinationNode == null
					|| !(sourceNode.getNode().getImpl() instanceof HasDependencies)
					|| !(destinationNode.getNode().getImpl() instanceof HasDependencies))
				return false;
			// MS Project creates a Finish-to-Start link with zero lag when users drag between bars.
			DependencyService.getInstance().newDependency((HasDependencies)sourceNode.getNode().getImpl(),
					(HasDependencies)destinationNode.getNode().getImpl(), DependencyType.FS, 0, this);
			return true;
		} catch (InvalidAssociationException e) {
			Alert.error(e.getMessage());
			return false;
		}
	}

    private boolean applyIntervalDrag(long dt, UndoableEditSupport undoSupport) {
    	long start=selectedInterval.getStart();
    	long end=selectedInterval.getEnd();
		Schedule schedule = getSourceSchedule();
		Task task = getSourceTask();
		long originalScheduleStart = schedule.getStart();
		long originalScheduleEnd = schedule.getEnd();
		long originalTaskStart = task == null ? 0L : task.getStart();
		long originalTaskEnd = task == null ? 0L : task.getEnd();
		int originalConstraintType = task == null ? ConstraintType.ASAP : task.getConstraintType();
		long originalConstraintDate = task == null ? 0L : task.getConstraintDate();
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
		boolean preparedConstraint = false;
		int targetConstraintType = updateConstraint ? getConstraintTypeForDrag() : ConstraintType.ASAP;
		long requestedConstraintDate = updateConstraint ? getRequestedConstraintDate(start, end) : 0L;
		if (updateConstraint && undoSupport != null) {
			undoSupport.beginUpdate();
		}
		boolean scheduleChanged = false;
		try {
			if (updateConstraint && task != null) {
				preparedConstraint = prepareConstraintForIntervalUpdate(task, targetConstraintType, requestedConstraintDate, originalConstraintType, originalConstraintDate);
			}
			scheduleChanged = ScheduleService.getInstance().setInterval(this,schedule,start,end,selectedInterval,undoSupport);
			if (!scheduleChanged) {
				scheduleChanged = didScheduleChange(schedule, task, originalScheduleStart, originalScheduleEnd, originalTaskStart, originalTaskEnd);
			}
			if (updateConstraint) {
				if (task != null && scheduleChanged) {
					applyConstraintAfterDrag(task, targetConstraintType, getConstraintDateForDrag(task), originalConstraintType, originalConstraintDate, undoSupport);
				} else if (task != null && preparedConstraint) {
					task.setScheduleConstraint(originalConstraintType, originalConstraintDate);
				}
			}
		} finally {
			if (updateConstraint && undoSupport != null) {
				undoSupport.endUpdate();
			}
		}
		return scheduleChanged;
    }

	static boolean changesIntervalAtHourPrecision(ScheduleInterval original, long start, long end) {
		return original != null
				&& (original.getStart() != DateTime.hourFloor(start)
						|| original.getEnd() != DateTime.hourFloor(end));
	}

	private boolean applyProgressDrag(long completed, UndoableEditSupport undoSupport) {
		return ScheduleService.getInstance().setCompleted(this,getSourceSchedule(),completed,undoSupport);
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

	private long getConstraintDateForDrag(Task task) {
		return task.getStart();
	}

	private long getRequestedConstraintDate(long requestedStart, long requestedEnd) {
		return requestedStart;
	}

    private boolean prepareConstraintForIntervalUpdate(Task task, int constraintType, long constraintDate, int originalConstraintType, long originalConstraintDate) {
    	if (task == null) {
    		return false;
    	}
    	if (originalConstraintType == constraintType && originalConstraintDate == constraintDate) {
    		return false;
    	}
    	task.setScheduleConstraint(constraintType, constraintDate);
    	return true;
    }

    private void applyConstraintAfterDrag(Task task, int constraintType, long constraintDate, int originalConstraintType, long originalConstraintDate, UndoableEditSupport undoSupport) {
    	if (task == null) {
    		return;
    	}
    	if (originalConstraintType == constraintType && originalConstraintDate == constraintDate) {
    		return;
    	}
		task.setScheduleConstraint(constraintType, constraintDate);
    	if (undoSupport != null) {
    		undoSupport.postEdit(new TaskConstraintEdit(task, originalConstraintType, originalConstraintDate, constraintType, constraintDate, this));
    	}
    }

    private boolean didScheduleChange(Schedule schedule, Task task, long originalScheduleStart, long originalScheduleEnd, long originalTaskStart, long originalTaskEnd) {
    	if (task != null) {
    		switch (state) {
    		case BAR_MOVE:
    			return task.getStart() != originalTaskStart || task.getEnd() != originalTaskEnd;
    		case BAR_MOVE_START:
    			return task.getStart() != originalTaskStart;
    		case BAR_MOVE_END:
    			return task.getEnd() != originalTaskEnd;
    		default:
    			return false;
    		}
    	}
    	switch (state) {
    	case BAR_MOVE:
    		return schedule.getStart() != originalScheduleStart || schedule.getEnd() != originalScheduleEnd;
    	case BAR_MOVE_START:
    		return schedule.getStart() != originalScheduleStart;
    	case BAR_MOVE_END:
    		return schedule.getEnd() != originalScheduleEnd;
    	default:
    		return false;
    	}
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

    private boolean refreshUndoState(boolean actionPerformed) {
    	if (!actionPerformed) {
    		return false;
    	}
    	GraphicManager graphicManager = GraphicManager.getInstance(getGraph());
    	if (graphicManager != null) {
    		DocumentFrame currentFrame = graphicManager.getCurrentFrame();
    		if (currentFrame != null) {
    			currentFrame.refreshUndoButtons();
    		}
    	}
    	return true;
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
    	gantt.notifyBarSelection(new Gantt.BarClick(node, isToggleModifier(e), e != null && e.isShiftDown()));
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


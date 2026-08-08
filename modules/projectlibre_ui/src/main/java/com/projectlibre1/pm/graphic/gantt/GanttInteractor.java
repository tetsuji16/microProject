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
package com.projectlibre1.pm.graphic.gantt;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.event.MouseEvent;

import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.undo.UndoableEditSupport;

import com.projectlibre1.pm.graphic.graph.GraphInteractor;
import com.projectlibre1.pm.graphic.graph.GraphUI;
import com.projectlibre1.pm.graphic.graph.GraphZone;
import com.projectlibre1.pm.graphic.collaboration.CollaborationHelper;
import com.projectlibre1.pm.graphic.frames.DocumentFrame;
import com.projectlibre1.pm.graphic.frames.GraphicManager;
import com.projectlibre1.pm.graphic.model.cache.GraphicDependency;
import com.projectlibre1.pm.graphic.model.cache.GraphicNode;
import com.projectlibre1.pm.graphic.timescale.CoordinatesConverter;
import com.projectlibre1.pm.graphic.views.synchro.ScrollPaneSynchronizer;
import com.projectlibre1.association.InvalidAssociationException;
import com.projectlibre1.functor.IntervalConsumer;
import com.projectlibre1.pm.dependency.DependencyService;
import com.projectlibre1.pm.dependency.DependencyType;
import com.projectlibre1.pm.dependency.HasDependencies;
import com.projectlibre1.pm.scheduling.ConstraintType;
import com.projectlibre1.pm.scheduling.Schedule;
import com.projectlibre1.pm.scheduling.ScheduleInterval;
import com.projectlibre1.pm.scheduling.ScheduleService;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.pm.task.Task;
import com.projectlibre1.undo.TaskConstraintEdit;
import com.projectlibre1.util.Alert;
import com.projectlibre1.util.ClassUtils;

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
				if (e.getClickCount() == 2)
				openTaskInformationAt(e.getX(), e.getY());
			}
    		super.mousePressed(e);
    		return;
    	}
    	getGraph().requestFocusInWindow();
    	if (SwingUtilities.isRightMouseButton(e)) {
			select(e.getX(), e.getY());
    		super.mousePressed(e);
    		return;
    	}
    	if (!SwingUtilities.isLeftMouseButton(e)) {
    		super.mousePressed(e);
    		return;
    	}

    	select(e.getX(), e.getY());
    	if (selected == null) {
    		startPan(e);
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
    		e.consume();
    		return;
    	}
    	getGraph().requestFocusInWindow();
    	super.mouseReleased(e);
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
    	if (x==x0||selected==null) return false;
    	if (state==BAR_MOVE||state==BAR_MOVE_START||state==BAR_MOVE_END||state==PROGRESS_BAR_MOVE||state==SPLIT){
    		if (!(selected instanceof GraphicNode)) return false;
    		sourceNode=(GraphicNode)selected;
    		if (!CollaborationHelper.tryLockObject(null, sourceNode.getNode(), getGraph(), "edit")) {
    			return false;
    		}
    	}
    	UndoableEditSupport undoSupport = getUndoableEditSupport();
    	switch (state) {
		case BAR_MOVE:
		case BAR_MOVE_START:
		case BAR_MOVE_END:
			return applyIntervalDrag((long)getCoord().toDuration(x-x0),undoSupport);
		case PROGRESS_BAR_MOVE:
			return applyProgressDrag((long)getCoord().toTime(x),undoSupport);
				case LINK_CREATION:
					try {
							if (sourceNode != null && !CollaborationHelper.tryLockObject(null, sourceNode.getNode(), getGraph(), "link")) {
								return false;
							}
							if (destinationNode != null && !CollaborationHelper.tryLockObject(null, destinationNode.getNode(), getGraph(), "link")) {
								return false;
							}
							if (sourceNode!=null&&destinationNode!=null&&
								sourceNode.getNode().getImpl() instanceof HasDependencies &&
								destinationNode.getNode().getImpl() instanceof HasDependencies){
							// MS Project creates a Finish-to-Start link with zero lag when users drag between bars.
							DependencyService.getInstance().newDependency((HasDependencies)sourceNode.getNode().getImpl(),(HasDependencies)destinationNode.getNode().getImpl(),DependencyType.FS,0,this);
						}
					} catch (InvalidAssociationException e) {
						Alert.error(e.getMessage());
						return false;
					}
					return true;
		case LINK_SELECTION:
			showDependencyPropertiesDialog((GraphicDependency)selected);
			return true;
		case SPLIT:
			long t=(long)getCoord().toTime(x);
			Schedule schedule = getSourceSchedule();
			boolean changed = ScheduleService.getInstance().split(this,schedule,t,t,undoSupport);
			return refreshUndoState(changed);
		}
    	return false;
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
		return refreshUndoState(scheduleChanged);
    }

	private boolean applyProgressDrag(long completed, UndoableEditSupport undoSupport) {
		boolean changed = ScheduleService.getInstance().setCompleted(this,getSourceSchedule(),completed,undoSupport);
		return refreshUndoState(changed);
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


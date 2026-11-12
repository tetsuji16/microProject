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
package com.microproject.pm.graphic.gantt;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.swing.JToolTip;
import javax.swing.JViewport;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import com.microproject.strings.Messages;
import com.microproject.timescale.TimeScale;
import com.microproject.undo.EditSupport;

import com.microproject.pm.graphic.graph.GraphInteractor;
import com.microproject.pm.graphic.graph.GraphUI;
import com.microproject.pm.graphic.graph.GraphZone;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.model.cache.GraphicDependency;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.ProjectionRowKey;
import com.microproject.pm.graphic.spreadsheet.command.TaskCommandGateway;
import com.microproject.pm.graphic.spreadsheet.command.TaskCommandResult;
import com.microproject.pm.graphic.spreadsheet.command.TaskScheduleEditIntent;
import com.microproject.ribbon.RibbonCommandResult;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.association.InvalidAssociationException;
import com.microproject.pm.scheduling.IntervalConsumer;
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
	private static final Logger LOGGER = Logger.getLogger(GanttInteractor.class.getName());
	private boolean calendarRangeSelecting;
	private GraphicNode lastCalendarRangeNode;
	private static final long serialVersionUID = -555882007216388246L;
	protected static final int BAR_MOVE_START=4;
	protected static final int BAR_MOVE_END=5;
	protected static final int PROGRESS_BAR_MOVE=6;
	protected static final int SPLIT=7;
	private static final int HORIZONTAL_PAN_SPEED_MULTIPLIER = 2;

	/** Immutable snapshot used while committing a bar-to-bar dependency drag. */
	private record DependencyLinkEndpoints(GraphicNode sourceNode, GraphicNode destinationNode) { }

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
	/** Fallback when the platform reports no drag threshold. */
	private static final int DEFAULT_DRAG_THRESHOLD_PX = 4;
	/**
	 * Date under the pointer when a bar drag started. Pixel positions are not stable
	 * during a drag (auto-scroll can move the converter origin), so every delta is
	 * measured in dates.
	 */
	private double dragStartTime = Double.NaN;
	private Popup dragFeedbackPopup;
	private String dragFeedbackText;
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
			// Progress handles sit exactly at the completed interval boundary. Resolve
			// their hit target before resize-edge tolerance, which can otherwise win
			// when pixel-to-time conversion puts the pointer a fraction past the bar.
			if (completedInterval!=null&&t>=completedInterval.getEnd()-completedDeltaT0
					&&t<=completedInterval.getEnd()+completedDeltaT1&&selectedZone!=null
					&&selectedZone.getZoneId()==GanttUI.PROGRESS_BAR_ZONE_ID){
				if (selectedIsNonSummaryNode()) state=PROGRESS_BAR_MOVE;
			}else if (t<interval.getStart()&&t>=interval.getStart()-deltaOutside){
				if (selectedIsNonSummaryNode()) state=BAR_MOVE_START;
			}else if (t>interval.getEnd()&&t<=interval.getEnd()+deltaOutside){
				if (selectedIsNonSummaryNode()) state=BAR_MOVE_END;
			}else if (t<interval.getStart()||t>interval.getEnd()){
				selectedIntervalNumber++;
				return;
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
		if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
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
		if (selected instanceof GraphicDependency dependency) {
			// Dependency inspection is a Gantt double-click action. Clear the
			// generic link action state before release can dispatch it.
			state = NOTHING_SELECTED;
			if (e.getClickCount() == 2) {
				showDependencyPropertiesDialog(dependency);
			}
			return;
		}
		if (selected == null) {
			// MSP selects the task row when the user clicks the calendar portion
			// of that row, even when no task bar occupies that particular pixel.
			// Do not pass this through GraphInteractor: its drag path treats a
			// selected node as a bar-edit gesture.
			GraphicNode calendarNode = getTaskRowAt(e.getY());
			if (calendarNode != null) {
				// A drag in calendar whitespace selects visible task rows.  It deliberately
				// reuses the chart-to-table BarClick contract rather than entering the
				// bar-edit drag path, so it cannot alter dates or dependencies.
				calendarRangeSelecting = !isToggleModifier(e) && !e.isShiftDown();
				lastCalendarRangeNode = calendarNode;
				notifyMode("StatusBar.Ready");
				return;
			}
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
     dragStartTime = Double.NaN;
     super.mousePressed(e);
     dragStartTime = isMove() && getCoord() != null ? getCoord().toTime(x0) : Double.NaN;
     notifyMode();
    }

    public void mouseDragged(MouseEvent e) {
		if (calendarRangeSelecting) {
			GraphicNode calendarNode = getTaskRowAt(e.getY());
			if (calendarNode != null && calendarNode != lastCalendarRangeNode) {
				if (getGraph() instanceof Gantt gantt && gantt.getCache() != null) {
					var projection = gantt.getCache().getVisibleNodes().getProjectionIndex();
					int row = Math.max(0, e.getY() / Math.max(1, gantt.getRowHeight()));
					var key = row < projection.size() ? projection.keyAt(row) : null;
					gantt.notifyBarSelection(new Gantt.BarClick(calendarNode, key, false, true));
				}
				lastCalendarRangeNode = calendarNode;
			}
			e.consume();
			return;
		}
    	if (panning) {
    		updatePan(e);
    		e.consume();
    		return;
    	}
     super.mouseDragged(e);
     updateDragFeedback(e);
    }

    public void mouseReleased(MouseEvent e) {
		if (e.isPopupTrigger() || wasPopupTriggeredOnPress()) {
			if (calendarRangeSelecting) {
				calendarRangeSelecting = false;
				lastCalendarRangeNode = null;
			}
			if (panning) stopPan();
			super.mouseReleased(e);
			state = NOTHING_SELECTED;
			reset();
			notifyMode("StatusBar.Ready");
			return;
		}
		if (calendarRangeSelecting) {
			calendarRangeSelecting = false;
			lastCalendarRangeNode = null;
			getGraph().requestFocusInWindow();
			notifyMode("StatusBar.Ready");
			e.consume();
			return;
		}
    	if (panning) {
    		stopPan();
    		notifyMode("StatusBar.Ready");
    		e.consume();
    		return;
    	}
    	getGraph().requestFocusInWindow();
    	hideDragFeedback();
    	super.mouseReleased(e);
    	dragStartTime = Double.NaN;
    	notifyMode("StatusBar.Ready");
    }

	@Override
	public void dispose() {
		hideDragFeedback();
		super.dispose();
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
		double startX = dragStartX();
		return new GanttSelectionGeometrySupport(node, getCoord(), (GanttUI)ui, config, startX, snappedShadowX(startX, x), state,
				selectedIntervalNumber, selectedInterval).createBarShadowBounds();
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
				((int)y)/((Gantt)getGraph()).getRowHeight()!=getGraph().getCache().getVisibleNodes()
						.getProjectionIndex().rowForNode(node) ;
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
		if (state == SPLIT)
			LOGGER.info(() -> "Gantt split point dispatched: selected=" + selected + " x=" + x + " x0=" + x0
				+ " interval=" + selectedInterval);
		if (selected==null || !hasMeaningfulAction(state == LINK_CREATION, state == SPLIT, dragStartX(), x)) return false;
    	if (state==BAR_MOVE||state==BAR_MOVE_START||state==BAR_MOVE_END||state==PROGRESS_BAR_MOVE||state==SPLIT){
    		if (!(selected instanceof GraphicNode)) return false;
    		sourceNode=(GraphicNode)selected;
    	}
	EditSupport undoSupport = getEditSupport();
		boolean actionPerformed;
		switch (state) {
		case BAR_MOVE:
		case BAR_MOVE_START:
		case BAR_MOVE_END:
		case PROGRESS_BAR_MOVE:
		case SPLIT:
			RibbonCommandResult scheduleOutcome = executeScheduleEdit(x, undoSupport);
			return refreshUndoState(scheduleOutcome != null
				&& scheduleOutcome.status() == RibbonCommandResult.Status.CHANGED);
		case LINK_CREATION:
			actionPerformed = createDependencyLink();
			break;
		case LINK_SELECTION:
			showDependencyPropertiesDialog((GraphicDependency)selected);
			return true;
		default:
			return false;
		}
		// Every mutating Gantt gesture goes through this one gate.  This prevents
		// new gesture types from silently omitting the root-pane Ctrl+Z refresh.
		return refreshUndoState(actionPerformed);
    }

	private RibbonCommandResult executeScheduleEdit(double x, EditSupport undoSupport) {
		if (sourceNode == null || selectedInterval == null || !(getGraph() instanceof Gantt gantt)
				|| gantt.getCache() == null || !(sourceNode.getNode().getImpl() instanceof Task task))
			return null;
		var projection = gantt.getCache().getVisibleNodes().getProjectionIndex();
		int row = projection.rowForNode(sourceNode);
		if (row < 0 || !(projection.keyAt(row) instanceof ProjectionRowKey.TaskRow taskRow))
			return null;
		TaskScheduleEditIntent.Operation operation;
		Schedule schedule = getSourceSchedule();
		long requestedStart = selectedInterval.getStart();
		long requestedEnd = selectedInterval.getEnd();
		long requestedValue = 0L;
		switch (state) {
		case BAR_MOVE:
			operation = TaskScheduleEditIntent.Operation.MOVE;
			break;
		case BAR_MOVE_START:
			operation = TaskScheduleEditIntent.Operation.RESIZE_START;
			break;
		case BAR_MOVE_END:
			operation = TaskScheduleEditIntent.Operation.RESIZE_END;
			break;
		case PROGRESS_BAR_MOVE:
			operation = TaskScheduleEditIntent.Operation.PROGRESS;
			requestedValue = (long)getCoord().toTime(x);
			break;
		case SPLIT:
			operation = TaskScheduleEditIntent.Operation.SPLIT;
			requestedValue = (long)getCoord().toTime(x);
			break;
		default:
			return null;
		}
		if (operation == TaskScheduleEditIntent.Operation.MOVE || operation == TaskScheduleEditIntent.Operation.RESIZE_START
				|| operation == TaskScheduleEditIntent.Operation.RESIZE_END) {
			// The rendered first interval can be normalized to working time and
			// differ from the task's persisted start. A finish-only resize anchors
			// to the schedule start or it silently shifts the task start.
			long[] requested = requestedInterval(x, schedule.getStart());
			requestedStart = requested[0];
			requestedEnd = requested[1];
		}
		TaskScheduleEditIntent intent = new TaskScheduleEditIntent(taskRow, projection.topologyRevision(), operation,
			schedule.getStart(), schedule.getEnd(), schedule.getCompletedThrough(), selectedInterval.getStart(),
			selectedInterval.getEnd(), task.getConstraintType(), task.getConstraintDate(), requestedStart,
			requestedEnd, requestedValue);
		long committedRequestedValue = requestedValue;
		Schedule committedSchedule = schedule;
		TaskCommandResult result = TaskCommandGateway.executeScheduleEdit(gantt.getCache(), intent, gantt, () -> {
			switch (operation) {
			case MOVE, RESIZE_START, RESIZE_END:
				return applyIntervalDrag(intent.requestedStart(), intent.requestedEnd(), undoSupport);
			case PROGRESS:
				return applyProgressDrag(committedRequestedValue, undoSupport);
			case SPLIT:
				return ScheduleService.getInstance().split(this, committedSchedule, committedRequestedValue,
					committedRequestedValue, undoSupport);
			default:
				return false;
			}
		});
		if (operation == TaskScheduleEditIntent.Operation.SPLIT)
			LOGGER.info(() -> "Gantt split command result: " + result.status() + " reason=" + result.reason());
		long taskId = intent.task().taskKey().taskUniqueId();
		RibbonCommandResult.Status status = switch (result.status()) {
		case CHANGED -> RibbonCommandResult.Status.CHANGED;
		case NO_CHANGE -> RibbonCommandResult.Status.NO_CHANGE;
		default -> RibbonCommandResult.Status.REJECTED;
		};
		String commandId = "gantt.schedule." + operation.name().toLowerCase(java.util.Locale.ROOT);
		RibbonCommandResult outcome = new RibbonCommandResult(commandId, status, result.reason(),
			java.util.List.of(taskId), status == RibbonCommandResult.Status.CHANGED
				? java.util.List.of(taskId) : java.util.List.of(), "");
		GraphicManager graphicManager = GraphicManager.getInstance(getGraph());
		return graphicManager == null ? outcome : graphicManager.recordUserCommandOutcome(outcome);
	}

	/** Pixels the pointer must travel before a press becomes a drag edit (platform threshold). */
	static int dragThresholdPixels() {
		try {
			int threshold = java.awt.dnd.DragSource.getDragThreshold();
			return threshold > 0 ? threshold : DEFAULT_DRAG_THRESHOLD_PX;
		} catch (RuntimeException | LinkageError unavailable) {
			return DEFAULT_DRAG_THRESHOLD_PX;
		}
	}

	/**
	 * Rounds {@code requested} to the minor timescale unit, measured from
	 * {@code original}: sub-day units (hours, minutes) round the delta to the nearest
	 * whole unit; day-or-longer units round it to the nearest whole calendar day. Working
	 * on the delta keeps the time of day of the bar, as Microsoft Project does when a bar
	 * is moved in whole days. Deterministic and independent of the screen.
	 */
	static long snapToMinorUnit(long original, long requested, int calendarField, int number) {
		int count = Math.max(1, number);
		long unitMillis;
		switch (calendarField) {
		case java.util.Calendar.HOUR:
		case java.util.Calendar.HOUR_OF_DAY:
			unitMillis = 3_600_000L * count;
			break;
		case java.util.Calendar.MINUTE:
			unitMillis = 60_000L * count;
			break;
		case java.util.Calendar.SECOND:
			unitMillis = 1_000L * count;
			break;
		case java.util.Calendar.MILLISECOND:
			unitMillis = count;
			break;
		default:
			long days = Math.round((requested - original) / 86_400_000d);
			var calendar = DateTime.calendarInstance();
			calendar.setTimeInMillis(original);
			calendar.add(java.util.Calendar.DAY_OF_MONTH, (int) days);
			return calendar.getTimeInMillis();
		}
		return original + Math.round((requested - original) / (double) unitMillis) * unitMillis;
	}

	private static long snapToScale(long original, long requested, TimeScale scale) {
		return scale == null ? requested
				: snapToMinorUnit(original, requested, scale.getCalendarField1(), scale.getNumber1());
	}

	/** Start x of the current drag in today's coordinates (the origin may have moved since the press). */
	private double dragStartX() {
		return Double.isNaN(dragStartTime) || getCoord() == null ? x0 : getCoord().toX(dragStartTime);
	}

	/** Starts a bar drag at pixel {@code x}, remembering the date under it. */
	void beginDragAt(double x) {
		x0 = x;
		dragStartTime = getCoord().toTime(x);
	}

	/** Date delta (ms) the pointer at {@code x} indicates since the drag started, before snapping. */
	long dragDeltaMillis(double x) {
		return rawDragDelta(x);
	}

	private long rawDragDelta(double x) {
		if (Double.isNaN(dragStartTime))
			return (long) getCoord().toDuration(x - x0);
		return (long) (getCoord().toTime(x) - dragStartTime);
	}

	/** Snapped {start, end} a move/resize drag to {@code x} would request. */
	private long[] requestedInterval(double x, long scheduleStart) {
		TimeScale scale = getCoord().getTimescaleManager().getScale();
		long start = selectedInterval.getStart();
		long end = selectedInterval.getEnd();
		long raw = rawDragDelta(x);
		switch (state) {
		case BAR_MOVE:
			long movedStart = snapToScale(start, start + raw, scale);
			return new long[] { movedStart, end + (movedStart - start) };
		case BAR_MOVE_START:
			return new long[] { snapToScale(start, start + raw, scale), end };
		case BAR_MOVE_END:
			return new long[] { scheduleStart, snapToScale(end, end + raw, scale) };
		default:
			return new long[] { start, end };
		}
	}

	private boolean isBarEditState() {
		return state == BAR_MOVE || state == BAR_MOVE_START || state == BAR_MOVE_END;
	}

	/** Pointer x equivalent to the snapped drag, so the shadow shows where the bar will land. */
	private double snappedShadowX(double startX, double x) {
		if (!isBarEditState() || selectedInterval == null || getCoord() == null || !(sourceOrSelected() instanceof Schedule schedule))
			return x;
		long[] requested = requestedInterval(x, schedule.getStart());
		long delta = state == BAR_MOVE_END ? requested[1] - selectedInterval.getEnd()
				: requested[0] - selectedInterval.getStart();
		return startX + getCoord().toW(delta);
	}

	private Object sourceOrSelected() {
		return selected instanceof GraphicNode node ? node.getNode().getImpl() : null;
	}

	private void updateDragFeedback(MouseEvent e) {
		if (!isBarEditState() || selectedInterval == null || Double.isNaN(dragStartTime) || getCoord() == null
				|| !(sourceOrSelected() instanceof Schedule schedule)
				|| Math.abs(e.getX() - dragStartX()) < dragThresholdPixels()) {
			return;
		}
		long[] requested = requestedInterval(e.getX(), schedule.getStart());
		String text = formatDragFeedback(requested[0], requested[1]);
		if (text.equals(dragFeedbackText))
			return;
		hideDragFeedback();
		dragFeedbackText = text;
		if (GraphicsEnvironment.isHeadless() || !getGraph().isShowing())
			return;
		try {
			JToolTip tip = getGraph().createToolTip();
			tip.setTipText(text);
			Point screen = new Point(e.getX() + 16, e.getY() + 24);
			SwingUtilities.convertPointToScreen(screen, getGraph());
			dragFeedbackPopup = PopupFactory.getSharedInstance().getPopup(getGraph(), tip, screen.x, screen.y);
			dragFeedbackPopup.show();
		} catch (RuntimeException unavailable) {
			dragFeedbackPopup = null;
		}
	}

	private void hideDragFeedback() {
		if (dragFeedbackPopup != null) {
			dragFeedbackPopup.hide();
			dragFeedbackPopup = null;
		}
		dragFeedbackText = null;
	}

	/** "Start: ... Finish: ..." text shown while moving or resizing a bar. */
	static String formatDragFeedback(long start, long end) {
		java.text.DateFormat format = DateTime.utcDateFormatInstance();
		return Messages.getString("Field.start") + ": " + format.format(new java.util.Date(start)) + "   "
				+ Messages.getString("Field.finish") + ": " + format.format(new java.util.Date(end));
	}

	static boolean hasMeaningfulDrag(boolean linkCreation, double startX, double endX) {
		// Link creation can be a vertical drag between bars on the same date.
		return hasMeaningfulAction(linkCreation, false, startX, endX);
	}

	static boolean hasMeaningfulAction(boolean linkCreation, boolean directPointAction, double startX, double endX) {
		// Split mode is executed at the clicked date, not dragged from the previous
		// pointer position.  Its action must not depend on a stale x0 from a prior drag.
		// A press that barely moves is a click (selection only), never a date edit.
		return linkCreation || directPointAction || Math.abs(endX - startX) >= dragThresholdPixels();
	}

	private boolean createDependencyLink() {
		DependencyLinkEndpoints endpoints = resolveDependencyLinkEndpoints();
		if (endpoints == null) {
			recordDependencyLinkOutcome(new TaskCommandResult(TaskCommandResult.Status.INVALID_INTENT,
				"dependency-endpoint-unavailable"), null);
			return false;
		}
		try {
			// MS Project creates a Finish-to-Start link with zero lag when users drag between bars.
			TaskCommandResult result = TaskCommandGateway.createDependency(getGraph().getModel().getCache(), endpoints.sourceNode(),
				endpoints.destinationNode(), this, getGraph());
			recordDependencyLinkOutcome(result, endpoints);
			return result.status() == TaskCommandResult.Status.CHANGED
				|| result.status() == TaskCommandResult.Status.NO_CHANGE;
		} catch (InvalidAssociationException e) {
			recordDependencyLinkOutcome(new TaskCommandResult(TaskCommandResult.Status.REJECTED, e.getMessage()), endpoints);
			Alert.error(e.getMessage());
			return false;
		}
	}

	private void recordDependencyLinkOutcome(TaskCommandResult result, DependencyLinkEndpoints endpoints) {
		List<Long> endpointTaskIds = endpoints == null ? List.of() : List.of(
			((Task) endpoints.sourceNode().getNode().getImpl()).getUniqueId(),
			((Task) endpoints.destinationNode().getNode().getImpl()).getUniqueId());
		RibbonCommandResult.Status status = switch (result.status()) {
		case CHANGED -> RibbonCommandResult.Status.CHANGED;
		case NO_CHANGE -> RibbonCommandResult.Status.NO_CHANGE;
		default -> RibbonCommandResult.Status.REJECTED;
		};
		RibbonCommandResult outcome = new RibbonCommandResult("gantt.dependency.create", status, result.reason(),
			endpointTaskIds, status == RibbonCommandResult.Status.CHANGED ? endpointTaskIds : List.of(), "");
		GraphicManager graphicManager = GraphicManager.getInstance(getGraph());
		if (graphicManager != null)
			graphicManager.recordUserCommandOutcome(outcome);
	}

	/**
	 * Resolves and validates both ends once, before collaboration locks or model
	 * mutation. This keeps the drag commit from locking an invalid target and
	 * gives the command one canonical endpoint snapshot.
	 */
	private DependencyLinkEndpoints resolveDependencyLinkEndpoints() {
		if (sourceNode == null || destinationNode == null || sourceNode == destinationNode
				|| !sourceNode.isLinkable() || !destinationNode.isLinkable()
				|| sourceNode.getNode() == null || destinationNode.getNode() == null) {
			return null;
		}
		Object sourceImpl = sourceNode.getNode().getImpl();
		Object destinationImpl = destinationNode.getNode().getImpl();
		if (!(sourceImpl instanceof HasDependencies source)
				|| !(destinationImpl instanceof HasDependencies destination)) {
			return null;
		}
		return new DependencyLinkEndpoints(sourceNode, destinationNode);
	}

    private boolean applyIntervalDrag(long start, long end, EditSupport undoSupport) {
		Schedule schedule = getSourceSchedule();
		Task task = getSourceTask();
		long originalScheduleStart = schedule.getStart();
		long originalScheduleEnd = schedule.getEnd();
		long originalTaskStart = task == null ? 0L : task.getStart();
		long originalTaskEnd = task == null ? 0L : task.getEnd();
		int originalConstraintTypeCode = task == null ? ConstraintType.Kind.ASAP.code() : task.getConstraintType();
		ConstraintType.Kind originalConstraintType = task == null ? ConstraintType.Kind.ASAP : task.getConstraintTypeKind();
		long originalConstraintDate = task == null ? 0L : task.getConstraintDate();
		if (state != BAR_MOVE && state != BAR_MOVE_START && state != BAR_MOVE_END)
			return false;
		if (!changesIntervalAtHourPrecision(selectedInterval, start, end)) {
			// Do not turn a sub-hour, visually ineffective drag into a constraint
			// edit.  The old ordering changed SNET/FNLT before ScheduleService
			// rejected the rounded no-op interval.
			return false;
		}
		boolean updateConstraint = shouldUpdateTaskConstraint();
		boolean preparedConstraint = false;
		Object beforeIntervalDetailBackup = updateConstraint && undoSupport != null ? schedule.backupDetail() : null;
		ConstraintType.Kind targetConstraintType = updateConstraint ? getConstraintTypeForDrag() : ConstraintType.Kind.ASAP;
		long requestedConstraintDate = updateConstraint ? getRequestedConstraintDate(start, end) : 0L;
		if (updateConstraint && undoSupport != null) {
			undoSupport.beginUpdate();
		}
		boolean scheduleChanged = false;
		try {
			if (updateConstraint && task != null) {
				preparedConstraint = prepareConstraintForIntervalUpdate(task, targetConstraintType, requestedConstraintDate, originalConstraintType, originalConstraintDate);
			}
			scheduleChanged = ScheduleService.getInstance().setInterval(this, schedule, start, end, selectedInterval,
				undoSupport, beforeIntervalDetailBackup);
			if (!scheduleChanged) {
				scheduleChanged = didScheduleChange(schedule, task, originalScheduleStart, originalScheduleEnd, originalTaskStart, originalTaskEnd);
			}
			if (updateConstraint) {
				if (task != null && scheduleChanged) {
					applyConstraintAfterDrag(task, targetConstraintType, getConstraintDateForDrag(task), originalConstraintType, originalConstraintTypeCode, originalConstraintDate, undoSupport);
				} else if (task != null && preparedConstraint) {
					task.setScheduleConstraint(originalConstraintTypeCode, originalConstraintDate);
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

	private boolean applyProgressDrag(long completed, EditSupport undoSupport) {
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
				&& (state == BAR_MOVE || state == BAR_MOVE_START);
	}

	private ConstraintType.Kind getConstraintTypeForDrag() {
		return ConstraintType.Kind.SNET;
	}

	private long getConstraintDateForDrag(Task task) {
		return task.getStart();
	}

	private long getRequestedConstraintDate(long requestedStart, long requestedEnd) {
		return requestedStart;
	}

    private boolean prepareConstraintForIntervalUpdate(Task task, ConstraintType.Kind constraintType, long constraintDate, ConstraintType.Kind originalConstraintType, long originalConstraintDate) {
    	if (task == null) {
    		return false;
    	}
    	if (originalConstraintType == constraintType && originalConstraintDate == constraintDate) {
    		return false;
    	}
	    task.setScheduleConstraint(constraintType, constraintDate);
    	return true;
    }

    private void applyConstraintAfterDrag(Task task, ConstraintType.Kind constraintType, long constraintDate, ConstraintType.Kind originalConstraintType, int originalConstraintTypeCode, long originalConstraintDate, EditSupport undoSupport) {
    	if (task == null) {
    		return;
    	}
    	if (originalConstraintType == constraintType && originalConstraintDate == constraintDate) {
    		return;
    	}
		task.setScheduleConstraint(constraintType, constraintDate);
	    if (undoSupport != null) {
	    undoSupport.postEdit(new TaskConstraintEdit(task, originalConstraintTypeCode, originalConstraintDate, constraintType.code(), constraintDate, this));
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

    private EditSupport getEditSupport() {
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
		if (node == null && leftClick) {
			node = getTaskRowAt(e.getY());
		}
    	// A right click on empty space (or on a link) keeps the current
    	// selection; only left clicks on empty chart space clear it.
    	if (node == null && !(leftClick && selected == null)) return;
		var projection = gantt.getCache() == null ? null
				: gantt.getCache().getVisibleNodes().getProjectionIndex();
		int row = e == null ? -1 : Math.max(0, e.getY() / Math.max(1, gantt.getRowHeight()));
		var key = projection != null && row < projection.size() ? projection.keyAt(row) : null;
		gantt.notifyBarSelection(new Gantt.BarClick(node, key, isToggleModifier(e), e != null && e.isShiftDown()));
    }

	private GraphicNode getTaskRowAt(int y) {
		return ui instanceof GanttUI ganttUi ? ganttUi.getTaskRowAt(y) : null;
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
    protected boolean isRepaintOnRelease(){
    	return state==BAR_MOVE||state==BAR_MOVE_END||state==BAR_MOVE_START||state==PROGRESS_BAR_MOVE||state==LINK_CREATION;
    }

}


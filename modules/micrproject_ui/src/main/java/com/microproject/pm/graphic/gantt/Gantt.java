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

import java.awt.Component;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.undo.AbstractUndoableEdit;

import com.microproject.graphic.configuration.GanttBarFormatOverrides;
import com.microproject.graphic.configuration.GanttBarFormatOverrides.BarFormat;
import com.microproject.pm.graphic.link_routing.DefaultGanttLinkRouting;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.graph.Graph;
import com.microproject.pm.graphic.graph.GraphParams;
import com.microproject.pm.graphic.graph.GraphUI;
import com.microproject.pm.graphic.graph.LinkRouting;
import com.microproject.pm.graphic.network.NetworkParamsImpl;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.graphic.timescale.ScaledComponent;
import com.microproject.pm.graphic.views.synchro.ScrollPaneSynchronizer;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.pm.time.HasStartAndEnd;
import com.microproject.preference.GlobalPreferences;
import com.microproject.strings.Messages;
import com.microproject.timescale.TimeScaleEvent;
import com.microproject.timescale.TimeScaleListener;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.GanttColorPalette;

/**
 *
 */
public class Gantt extends Graph implements ScaledComponent, TimeScaleListener, GanttParams{
//    protected GanttPopupMenu popup;

//    protected DependencyDialog dependencyPropertiesDialog;

	private static final long serialVersionUID = -1806070019043393474L;
	private static final String ZOOM_OUT_ACTION = "gantt.zoomOut";
	private static final String ZOOM_IN_ACTION = "gantt.zoomIn";
	private static final int AUTO_SCROLL_START_THRESHOLD = 150;
	private static final int AUTO_SCROLL_LEFT_PADDING = 50;
	private static final int BOTTOM_SCROLL_BUFFER_ROWS = 5;
	/** Default display policy shared by standalone and task-table Gantt views. */
	public static final boolean DEFAULT_GRID_LINES_VISIBLE = true;
	/** A view-local annotation field value that suppresses all task labels. */
	public static final String ANNOTATION_FIELD_HIDDEN = "Gantt.Annotation.Hidden";
	private boolean progressLineEnabled = false;
	private boolean gridLinesVisible = DEFAULT_GRID_LINES_VISIBLE;
	private Color customGridLineColor;
	private String annotationFieldId;
	private String annotationPosition = GlobalPreferences.GANTT_BAR_TEXT_POSITION_AUTO;
	private String formatViewName = GanttBarFormatOverrides.STANDARD_VIEW;
	/** Rows whose full calendar width is highlighted because they are selected in the task table. */
	private Set<Integer> highlightedRows = Collections.emptySet();
	private Consumer<BarClick> barSelectionListener;

	/**
	 * A click on the Gantt chart that drives task selection, mirroring
	 * Microsoft Project: a plain click selects the task, Ctrl/Cmd+click
	 * toggles it in the selection, Shift+click extends the selection, and a
	 * click on empty chart space (node == null) clears the selection.
	 */
	public record BarClick(GraphicNode node, boolean toggle, boolean extend) {
	}
	public Gantt(Project project,String viewName) {
		this(new GanttModel(project,viewName),project);
	}
	protected Gantt(GanttModel model, Project project) {
		super(model,project);
		this.setToolTipText(Messages.getString("Text.rightClickForOptions"));
		setFocusable(true);
		installKeyboardActions();

	}

	public void cleanUp() {
		barSelectionListener = null;
		var coord = getCoord();
		if (coord != null) {
			coord.removeTimeScaleListener(this);
			coord.removeTimeScaleListener((GanttModel) model);
		}
		super.cleanUp();
	}

	public void updateUI() {
		setUI(new GanttUI(this));
		invalidate();
	}

	/**
	 * Returns the annotation field selected for this chart only. Bar styles are
	 * shared configuration objects, so view state must not be stored in them.
	 */
	public String getAnnotationFieldId() {
		return annotationFieldId;
	}

	public void setAnnotationFieldId(String annotationFieldId) {
		this.annotationFieldId = annotationFieldId;
	}
	public String getAnnotationPosition() { return annotationPosition; }
	public void setAnnotationPosition(String value) {
		annotationPosition = GlobalPreferences.GANTT_BAR_TEXT_POSITION_LEFT.equals(value)
				|| GlobalPreferences.GANTT_BAR_TEXT_POSITION_RIGHT.equals(value)
				? value : GlobalPreferences.GANTT_BAR_TEXT_POSITION_AUTO;
	}

	public boolean isAnnotationHidden() {
		return ANNOTATION_FIELD_HIDDEN.equals(annotationFieldId);
	}

	/**
	 * Rows whose complete calendar width should be highlighted in the chart,
	 * mirroring the selection made in the task table on the left. Row indexes
	 * refer to the shared node cache that backs both the table and the chart.
	 */
	public void setHighlightedRows(Set<Integer> rows) {
		Set<Integer> copy = (rows == null || rows.isEmpty()) ? Collections.emptySet() : new HashSet<>(rows);
		if (copy.equals(highlightedRows)) {
			return;
		}
		highlightedRows = copy;
		repaint();
	}

	public Set<Integer> getHighlightedRows() {
		return highlightedRows;
	}

	/**
	 * Registers a callback invoked when the user clicks the chart (a task bar
	 * or empty chart space). The view uses it to keep the task table selection
	 * in sync with the chart.
	 */
	public void setBarSelectionListener(Consumer<BarClick> listener) {
		barSelectionListener = listener;
	}

	void notifyBarSelection(BarClick click) {
		repaint();
		if (barSelectionListener != null) {
			barSelectionListener.accept(click);
		}
	}

//	public GanttPopupMenu getPopup() {
//		return popup;
//	}



	public CoordinatesConverter getCoord() {
		return ((GanttModel) model).getCoord();
	}

	public GanttInteractor getInteractor() {
		return ((GanttUI) ui).getInteractor();
	}

	public void setCoord(CoordinatesConverter coord) {
		var modelCoord = getCoord();
		if (modelCoord != null) {
			modelCoord.removeTimeScaleListener(this);
		}
		coord.addTimeScaleListener(this);
		((GanttModel) model).setCoord(coord);
	}

	public void timeScaleChanged(TimeScaleEvent e) {
		updateSize();
// 		Component p;
// 		if ((p=getParent()) instanceof JViewport){
// 			//JViewport vp=(JViewport)p;
// 	 		if ((p=p.getParent()) instanceof ScaledScrollPane){
// 	 			ScaledScrollPane scp=(ScaledScrollPane)p;
// 	 			scp.updateTimeScaleComponentSize();
// 	 		}
//
// 		}
 	}



	public int getRow(double y){
		return (int) (y / (double) getRowHeight());
	}


	public int getRowHeight(){
	    return ((GanttModel)model).getRowHeight();
	}
	public void setRowHeight(int rowHeight){
		((GanttModel)model).setRowHeight(rowHeight);
	}
//	public int getColumnHeaderHeight() {
//		return ((GanttModel)model).getColumnHeaderHeight();
//	}
//	public void setColumnHeaderHeight(int columnHeaderHeight) {
//		((GanttModel)model).setColumnHeaderHeight(columnHeaderHeight);
//	}
	public Font getColumnHeaderFont() {
		return null;
	}
	public void setColumnHeaderFont(Font columnHeaderFont) {
	}

	protected LinkRouting routing = new DefaultGanttLinkRouting();//new QuadraticGanttLinkRouting();
	public LinkRouting getRouting(){
		return routing;
	}
	public void setRouting(LinkRouting routing) {
		this.routing = routing;
	}

	public boolean isProgressLineEnabled() {
		return progressLineEnabled;
	}

	public void setProgressLineEnabled(boolean progressLineEnabled) {
		this.progressLineEnabled = progressLineEnabled;
		repaint();
	}

	public boolean isGridLinesVisible() {
		return gridLinesVisible;
	}

	public void setGridLinesVisible(boolean visible) {
		this.gridLinesVisible = visible;
		repaint();
	}

	public Color getGridLineColor() {
		if (customGridLineColor != null) return customGridLineColor;
		if (getUI() != null && getUI().getGraphRenderer() instanceof GanttRenderer ganttRenderer) {
			GanttColorPalette palette = ganttRenderer.getPalette();
			if (palette != null) {
				return palette.getGridLine();
			}
		}
		return FlatUiSupport.tableGridColor();
	}

	public void setGridLineColor(Color color) {
		customGridLineColor = color;
		repaint();
	}

	public String getFormatViewName() {
		return formatViewName;
	}

	public void setTrackingView(boolean tracking) {
		formatViewName = tracking
				? GanttBarFormatOverrides.TRACKING_VIEW
				: GanttBarFormatOverrides.STANDARD_VIEW;
		repaint();
	}

	public BarFormat getBarFormat(Task task) {
		if (task == null)
			return BarFormat.automatic();
		return project.getGanttBarFormatOverrides().get(formatViewName, task.getUniqueId());
	}

	/**
	 * Returns the colors currently used to paint this task, including automatic
	 * palette colors and any individual overrides. UI previews must use this
	 * instead of a fixed fallback color.
	 */
	public GanttRenderer.DisplayedBarColors getDisplayedBarColors(Task task) {
		if (getUI() instanceof GanttUI ganttUi)
			return ganttUi.getGanttRenderer().resolveDisplayedBarColors(task);
		return new GanttRenderer.DisplayedBarColors(
				BarColorField.DEFAULT_BAR_RGB,
				BarColorField.DEFAULT_BAR_RGB,
				BarColorField.DEFAULT_BAR_RGB);
	}

	public void applyBarFormat(Task task, BarFormat format) {
		if (task == null || project.isReadOnly())
			return;
		BarFormat normalized = format == null ? BarFormat.automatic() : format;
		BarFormat previous = getBarFormat(task);
		if (Objects.equals(previous, normalized))
			return;
		setBarFormat(task, normalized);
		if (project.getUndoController() != null) {
			project.getUndoController().getEditSupport().postEdit(
					new BarFormatEdit(this, task.getUniqueId(), formatViewName, previous, normalized));
		}
		GraphicManager manager = GraphicManager.getInstance(this);
		if (manager != null && manager.getCurrentFrame() != null)
			manager.getCurrentFrame().refreshUndoButtons();
	}

	private void setBarFormat(Task task, BarFormat format) {
		project.getGanttBarFormatOverrides().set(formatViewName, task.getUniqueId(), format);
		project.setDirty(true);
		repaint();
	}

	private void setBarFormat(long taskUniqueId, String viewName, BarFormat format) {
		project.getGanttBarFormatOverrides().set(viewName, taskUniqueId, format);
		project.setDirty(true);
		repaint();
	}

	private static final class BarFormatEdit extends AbstractUndoableEdit {
		private static final long serialVersionUID = 1L;
		private final Gantt gantt;
		private final long taskUniqueId;
		private final String viewName;
		private final BarFormat before;
		private final BarFormat after;

		private BarFormatEdit(Gantt gantt, long taskUniqueId, String viewName, BarFormat before, BarFormat after) {
			this.gantt = gantt;
			this.taskUniqueId = taskUniqueId;
			this.viewName = viewName;
			this.before = before;
			this.after = after;
		}

		@Override
		public String getPresentationName() {
			return Messages.getString("Gantt.FormatBar.title");
		}

		@Override
		public void undo() {
			super.undo();
			gantt.setBarFormat(taskUniqueId, viewName, before);
		}

		@Override
		public void redo() {
			super.redo();
			gantt.setBarFormat(taskUniqueId, viewName, after);
		}
	}

	private void installKeyboardActions() {
		var inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
		var actionMap = getActionMap();

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.CTRL_DOWN_MASK), ZOOM_OUT_ACTION);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.CTRL_DOWN_MASK), ZOOM_IN_ACTION);

		actionMap.put(ZOOM_OUT_ACTION, createZoomAction(() -> ScrollPaneSynchronizer.zoomOut(Gantt.this)));
		actionMap.put(ZOOM_IN_ACTION, createZoomAction(() -> ScrollPaneSynchronizer.zoomIn(Gantt.this)));
	}

	private static AbstractAction createZoomAction(Runnable zoomOperation) {
		return new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				zoomOperation.run();
			}
		};
	}

	public void updateSize(){
//		Component c=this;
//		while ((c=c.getParent())!=null&&(!(c instanceof JViewport)));
//		if (c instanceof JViewport){
//			JViewport v=(JViewport)c;
//			v.setViewSize(new Dimension((int)Math.ceil(getCoord().getWidth()),v.getViewSize().height));
//		}
		((GraphUI) ui).updateShapes();
		synchronizeViewportSize();
		revalidate();
	}

	public void synchronizeViewportSize() {
		var parent = getParent();
		if (parent instanceof JViewport viewport) {
			int height = getScrollableHeight(viewport.getExtentSize().height);
			viewport.setViewSize(new Dimension(getDrawingWidth(), height));
			setPreferredSize(new Dimension(getDrawingWidth(), height));
			clampViewportPosition(viewport, height);
			return;
		}
		setPreferredSize(new Dimension(getDrawingWidth(), getScrollableHeight(getVisibleRect().height)));
	}

	private int getDrawingWidth() {
		var coord = getCoord();
		return coord == null ? 0 : (int) Math.ceil(coord.getWidth());
	}

	public int getScrollableHeight(int viewportHeight) {
		int rowCount = getCache() == null ? 0 : getCache().getSize();
		int bufferedRowsHeight = (rowCount + BOTTOM_SCROLL_BUFFER_ROWS) * getRowHeight();
		return Math.max(viewportHeight, bufferedRowsHeight);
	}

	public void clampViewportPosition(JViewport viewport, int viewHeight) {
		Point position = viewport.getViewPosition();
		int maxY = Math.max(0, viewHeight - viewport.getExtentSize().height);
		if (position.y > maxY) {
			position.y = maxY;
			viewport.setViewPosition(position);
		}
	}

	public Rectangle getGanttBounds(){
		return getDrawingBounds();
	}
	public boolean useTextures() {
		return true;
	}
	public Rectangle getPrintBounds() {
		return null;
	}
	public void setPrintBounds(Rectangle printBounds) {
	}
	public int getPrintCols() {
		return 0;
	}
	public int getPrintRows() {
		return 0;
	}


	public void scrollToTask(HasStartAndEnd interval,boolean automatic){
		var coord = getCoord();
		if (interval == null || coord == null) {
			return;
		}
		var start = coord.toX(interval.getStart());
		var end = coord.toX(interval.getEnd());
		var visible = getVisibleRect();
		if (automatic && isAlreadyVisible(visible, start, end))
			return; //already visible

		var parent = getParent();
		if (parent instanceof JViewport viewport) {
			var position = viewport.getViewPosition();
			if (start < AUTO_SCROLL_START_THRESHOLD) {
				position.x = 0;
			} else {
				position.x = (int) Math.ceil(start) - AUTO_SCROLL_LEFT_PADDING; // 3 days 1/3
				if (position.x < 0) {
					position.x = 0;
				}
			}
			viewport.setViewPosition(position);
		}
		//scrollRectToVisible(visible);
	}

	private static boolean isAlreadyVisible(Rectangle visible, double start, double end) {
		return (start >= visible.x && start <= visible.x + visible.width)
				|| (end >= visible.x && end <= visible.x + visible.width)
				|| (start < visible.x && end > visible.x + visible.width);
	}
	public boolean isLeftPartVisible() {
		return true;
	}
	public boolean isRightPartVisible() {
		return true;
	}
	public void setLeftPartVisible(boolean visible){}
	public void setRightPartVisible(boolean visible){}
	public boolean isSupportLeftAndRightParts(){return false;}
	public void setSupportLeftAndRightParts(boolean supports){}
	public GraphParams createSafePrintCopy(){return this;}


}


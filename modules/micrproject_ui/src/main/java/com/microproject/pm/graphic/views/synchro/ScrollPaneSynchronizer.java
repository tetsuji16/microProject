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
package com.microproject.pm.graphic.views.synchro;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.DoubleUnaryOperator;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.timescale.TimeScale;
import com.microproject.util.DateTime;

/**
 * 
 */
public class ScrollPaneSynchronizer {
	private static final Map ganttSynchronizers = new WeakHashMap();

	public static final int HORIZONTAL = JSplitPane.VERTICAL_SPLIT;

	public static final int VERTICAL = JSplitPane.HORIZONTAL_SPLIT;

	protected JScrollPane scrollPane1;

	protected JScrollPane scrollPane2;

	protected int orientation;

	protected ChangeListener listener = null;

	protected ChangeListener scrollPane1Listener = null;

	protected ChangeListener scrollPane2Listener = null;

	protected MouseWheelListener scrollPane1WheelListener = null;

	protected MouseWheelListener scrollPane2WheelListener = null;

	protected MouseWheelEvent scrollPane1LastWheelEvent = null;

	protected MouseWheelEvent scrollPane2LastWheelEvent = null;

	protected ArrayList scrollPane1WheelTargets = new ArrayList();

	protected ArrayList scrollPane2WheelTargets = new ArrayList();

	protected int defaultScrollBarPolicy1;

	protected int defaultScrollBarPolicy2;

	protected boolean defaultWheelScrollingEnabled1;

	protected boolean defaultWheelScrollingEnabled2;

	protected AdjustmentListener scrollPane1HorizontalAdjustmentListener = null;

	protected AdjustmentListener scrollPane2HorizontalAdjustmentListener = null;
	
	protected boolean bottomBarActivated=true;
	protected boolean bottomBarEnabled=false;

	protected boolean active = false;
	private boolean synchronizingViewport = false;

	protected ZoomRestoreState scrollPane1ZoomRestoreState = null;

	protected ZoomRestoreState scrollPane2ZoomRestoreState = null;

	protected int programmaticHorizontalScrollCount = 0;

	private static class ZoomRestoreState {
		private Double keptLeftDate = null;

		private double resolve(double currentLeftDate) {
			return keptLeftDate == null ? currentLeftDate : keptLeftDate.doubleValue();
		}

		private void update(double leftDate) {
			keptLeftDate = Double.valueOf(leftDate);
		}

		private void reset() {
			keptLeftDate = null;
		}
	}
	/**
	 * @param scrollPane1
	 * @param scrollPane2
	 * @param position
	 */
	public ScrollPaneSynchronizer(JScrollPane scrollPane1,
			JScrollPane scrollPane2, int orientation) {
		this.scrollPane1 = scrollPane1;
		this.scrollPane2 = scrollPane2;
		this.orientation = orientation;
	}

	/**
	 * @return Returns the bottomBarActivated.
	 */
	public boolean isBottomBarActivated() {
		return bottomBarActivated;
	}
	/**
	 * @param bottomBarActivated The bottomBarActivated to set.
	 */
	public void setBottomBarActivated(boolean bottomBarActivated) {
		this.bottomBarActivated = bottomBarActivated;
	}
	/**
	 * @return Returns the bottomBarEnabled.
	 */
	public boolean isBottomBarEnabled() {
		return bottomBarEnabled;
	}
	/**
	 * @param bottomBarEnabled The bottomBarEnabled to set.
	 */
	public void setBottomBarEnabled(boolean bottomBarEnabled) {
		this.bottomBarEnabled = bottomBarEnabled;
	}
	public void activateSynchro() {
		if (active) {
			return;
		}
		active = true;
		if (orientation == HORIZONTAL) {
			defaultScrollBarPolicy1 = scrollPane1.getVerticalScrollBarPolicy();
			defaultScrollBarPolicy2 = scrollPane2.getVerticalScrollBarPolicy();
			defaultWheelScrollingEnabled1 = scrollPane1.isWheelScrollingEnabled();
			defaultWheelScrollingEnabled2 = scrollPane2.isWheelScrollingEnabled();
			scrollPane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			scrollPane2.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			scrollPane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
			scrollPane2.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			scrollPane1.setWheelScrollingEnabled(false);
			scrollPane2.setWheelScrollingEnabled(false);

			 scrollPane1Listener = new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (synchronizingViewport) return;
					synchronizingViewport = true;
					try {
					JViewport vp1 = scrollPane1.getViewport();
					JViewport vp2 = scrollPane2.getViewport();
					Point p1 = vp1.getViewPosition();
					Point p2 = vp2.getViewPosition();
					p2.setLocation((int) p2.getX(), (int) p1.getY());
					vp2.setViewPosition(p2);
					vp2.revalidate();
					} finally {
						synchronizingViewport = false;
					}
				}
			};
			scrollPane2Listener = new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (synchronizingViewport) return;
					synchronizingViewport = true;
					try {
					JViewport vp1 = scrollPane1.getViewport();
					JViewport vp2 = scrollPane2.getViewport();
					Point p1 = vp1.getViewPosition();
					Point p2 = vp2.getViewPosition();
					p1.setLocation((int) p1.getX(), (int) p2.getY());
					vp1.setViewPosition(p1);
					vp1.revalidate();
					} finally {
						synchronizingViewport = false;
					}
				}
			};
			scrollPane1.getViewport().addChangeListener(scrollPane1Listener);
			scrollPane2.getViewport().addChangeListener(scrollPane2Listener);
			scrollPane1ZoomRestoreState = createZoomRestoreState(scrollPane1);
			scrollPane2ZoomRestoreState = createZoomRestoreState(scrollPane2);
			registerGanttSynchronizer(scrollPane1, scrollPane1ZoomRestoreState);
			registerGanttSynchronizer(scrollPane2, scrollPane2ZoomRestoreState);
			scrollPane1HorizontalAdjustmentListener = createHorizontalAdjustmentListener(scrollPane1ZoomRestoreState);
			scrollPane2HorizontalAdjustmentListener = createHorizontalAdjustmentListener(scrollPane2ZoomRestoreState);
			scrollPane1.getHorizontalScrollBar().addAdjustmentListener(scrollPane1HorizontalAdjustmentListener);
			scrollPane2.getHorizontalScrollBar().addAdjustmentListener(scrollPane2HorizontalAdjustmentListener);

			scrollPane1WheelListener = new MouseWheelListener() {
				public void mouseWheelMoved(MouseWheelEvent e) {
					if (e == scrollPane1LastWheelEvent) {
						return;
					}
					scrollPane1LastWheelEvent = e;
					if (handleZoomWheel(scrollPane1, e)) {
						e.consume();
						return;
					}
					if (e.isShiftDown()) {
						scrollHorizontally(scrollPane1, e);
					} else {
						scrollVertically(scrollPane1, e);
					}
					e.consume();
				}
			};
			scrollPane2WheelListener = new MouseWheelListener() {
				public void mouseWheelMoved(MouseWheelEvent e) {
					if (e == scrollPane2LastWheelEvent) {
						return;
					}
					scrollPane2LastWheelEvent = e;
					if (handleZoomWheel(scrollPane2, e)) {
						e.consume();
						return;
					}
					if (e.isShiftDown()) {
						scrollHorizontally(scrollPane2, e);
					} else {
						scrollVertically(scrollPane2, e);
					}
					e.consume();
				}
			};
			registerMouseWheelTargets(scrollPane1, scrollPane1WheelListener, scrollPane1WheelTargets);
			registerMouseWheelTargets(scrollPane2, scrollPane2WheelListener, scrollPane2WheelTargets);

		} else if (orientation == VERTICAL) {
			defaultScrollBarPolicy1 = scrollPane1.getHorizontalScrollBarPolicy();
			defaultScrollBarPolicy2 = scrollPane2.getHorizontalScrollBarPolicy();
			scrollPane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			scrollPane2.setHorizontalScrollBarPolicy((bottomBarActivated) ? JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS : JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			scrollPane2.getHorizontalScrollBar().setEnabled(bottomBarEnabled);

			listener = new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (synchronizingViewport) return;
					synchronizingViewport = true;
					try {
					JViewport vp1 = scrollPane1.getViewport();
					JViewport vp2 = scrollPane2.getViewport();

					Point p1 = vp1.getViewPosition();
					Point p2 = vp2.getViewPosition();
					p2.setLocation((int) p1.getX(), (int) p2.getY());
					vp2.setViewPosition(p2);

					Dimension d1 = vp1.getViewSize();
					Dimension d2 = vp2.getViewSize();
					d2.setSize((int) d1.getWidth(), (int) d2.getHeight());

					vp2.setViewSize(d2);
					((JComponent) vp2.getView()).setPreferredSize(d2);

					vp2.revalidate();
					} finally {
						synchronizingViewport = false;
					}
				}
			};
			scrollPane1.getViewport().addChangeListener(listener);
		}
	}

	public void deactivateSynchro() {
		if (!active) {
			return;
		}
		active = false;
		if (orientation == HORIZONTAL) {
			if (scrollPane1Listener != null) {
				scrollPane1.getViewport().removeChangeListener(scrollPane1Listener);
				scrollPane1Listener = null;
			}
			if (scrollPane2Listener != null) {
				scrollPane2.getViewport().removeChangeListener(scrollPane2Listener);
				scrollPane2Listener = null;
			}
			unregisterGanttSynchronizer(scrollPane1);
			unregisterGanttSynchronizer(scrollPane2);
			scrollPane1ZoomRestoreState = null;
			scrollPane2ZoomRestoreState = null;
			if (scrollPane1HorizontalAdjustmentListener != null) {
				scrollPane1.getHorizontalScrollBar().removeAdjustmentListener(scrollPane1HorizontalAdjustmentListener);
				scrollPane1HorizontalAdjustmentListener = null;
			}
			if (scrollPane2HorizontalAdjustmentListener != null) {
				scrollPane2.getHorizontalScrollBar().removeAdjustmentListener(scrollPane2HorizontalAdjustmentListener);
				scrollPane2HorizontalAdjustmentListener = null;
			}
			unregisterMouseWheelTargets(scrollPane1WheelTargets, scrollPane1WheelListener);
			unregisterMouseWheelTargets(scrollPane2WheelTargets, scrollPane2WheelListener);
			scrollPane1WheelListener = null;
			scrollPane2WheelListener = null;
			scrollPane1.setWheelScrollingEnabled(defaultWheelScrollingEnabled1);
			scrollPane2.setWheelScrollingEnabled(defaultWheelScrollingEnabled2);
			scrollPane1.setVerticalScrollBarPolicy(defaultScrollBarPolicy1);
			scrollPane2.setVerticalScrollBarPolicy(defaultScrollBarPolicy2);
		} else if (orientation == VERTICAL) {
			if (listener != null) {
				scrollPane1.getViewport().removeChangeListener(listener);
				listener = null;
			}
			scrollPane1.setHorizontalScrollBarPolicy(defaultScrollBarPolicy1);
			scrollPane2.setHorizontalScrollBarPolicy(defaultScrollBarPolicy2);
			scrollPane2.getHorizontalScrollBar().setEnabled(true);
		}
	}

	private void scrollVertically(JScrollPane scrollPane, MouseWheelEvent e) {
		int steps = getWheelSteps(e);
		if (steps == 0) {
			return;
		}

		JViewport viewport = scrollPane.getViewport();
		if (viewport == null) {
			return;
		}
		int scrollAmount = Math.abs(steps) * getVerticalScrollStep(scrollPane);
		Point viewPosition = viewport.getViewPosition();
		viewPosition.y += steps > 0 ? scrollAmount : -scrollAmount;
		clampViewPosition(viewport, viewPosition);
		viewport.setViewPosition(viewPosition);
	}

	private void scrollHorizontally(JScrollPane scrollPane, MouseWheelEvent e) {
		int steps = getWheelSteps(e);
		if (steps == 0) {
			return;
		}

		JViewport viewport = scrollPane.getViewport();
		if (viewport == null) {
			return;
		}
		int direction = steps > 0 ? 1 : -1;
		int scrollAmount = Math.abs(steps) * getHorizontalScrollStep(scrollPane, direction);
		Point viewPosition = viewport.getViewPosition();
		viewPosition.x += steps > 0 ? scrollAmount : -scrollAmount;
		clampViewPosition(viewport, viewPosition);
		setViewportViewPosition(viewport, viewPosition);
		updateKeptLeftDate(getZoomRestoreState(scrollPane), resolveViewportLeftEdgeDate(coord -> coord, viewPosition));
	}

	private boolean handleZoomWheel(JScrollPane scrollPane, MouseWheelEvent e) {
		if (!e.isControlDown()) {
			return false;
		}
		return performZoom(scrollPane, getWheelSteps(e), resolveWheelCursorX(scrollPane, e));
	}

	/**
	 * Content x coordinate under the mouse cursor, or -1 when it cannot be
	 * resolved (callers then fall back to left-edge anchored zoom).
	 */
	private int resolveWheelCursorX(JScrollPane scrollPane, MouseWheelEvent e) {
		JViewport viewport = scrollPane.getViewport();
		if (viewport == null || viewport.getView() == null || !(e.getSource() instanceof Component source)) {
			return -1;
		}
		return SwingUtilities.convertPoint(source, e.getPoint(), viewport.getView()).x;
	}

	public static boolean zoomIn(Component component) {
		return performZoom(component, -1);
	}

	public static boolean zoomOut(Component component) {
		return performZoom(component, 1);
	}

	public static boolean zoomToScale(Component component, int targetScaleIndex) {
		JScrollPane scrollPane = findScrollPane(component);
		ScrollPaneSynchronizer synchronizer = findSynchronizer(component);
		if (scrollPane == null || synchronizer == null) {
			return false;
		}
		return synchronizer.zoomToScale(scrollPane, targetScaleIndex);
	}

	private static boolean performZoom(Component component, int steps) {
		JScrollPane scrollPane = findScrollPane(component);
		ScrollPaneSynchronizer synchronizer = findSynchronizer(component);
		if (scrollPane == null || synchronizer == null) {
			return false;
		}
		return synchronizer.performZoom(scrollPane, steps);
	}

	private boolean performZoom(JScrollPane scrollPane, int steps) {
		return performZoom(scrollPane, steps, -1);
	}

	private boolean performZoom(JScrollPane scrollPane, int steps, int cursorX) {
		if (steps == 0) {
			return true;
		}

		JViewport viewport = scrollPane.getViewport();
		if (viewport == null) {
			return false;
		}
		Component view = viewport.getView();
		if (!(view instanceof Gantt)) {
			return false;
		}

		CoordinatesConverter coord = ((Gantt) view).getCoord();
		if (coord == null) {
			return true;
		}

		ZoomRestoreState zoomRestoreState = getZoomRestoreState(scrollPane);
		double anchorDate = cursorX >= 0
				? coord.toTime(cursorX)
				: resolveViewportLeftEdgeDate(coord::toTime, viewport.getViewPosition());
		double keptLeftDate = cursorX >= 0 ? anchorDate : resolveKeptLeftDate(zoomRestoreState, anchorDate);
		performZoomStep(scrollPane, coord, zoomRestoreState, keptLeftDate, steps < 0, cursorX);
		return true;
	}

	private boolean zoomToScale(JScrollPane scrollPane, int targetScaleIndex) {
		JViewport viewport = scrollPane.getViewport();
		if (viewport == null) {
			return false;
		}
		Component view = viewport.getView();
		if (!(view instanceof Gantt gantt)) {
			return false;
		}

		CoordinatesConverter coord = gantt.getCoord();
		if (coord == null) {
			return true;
		}

		int currentScaleIndex = coord.getTimescaleManager().getCurrentScaleIndex();
		int boundedTargetScaleIndex = clampTargetScaleIndex(targetScaleIndex, coord.getTimescaleManager().getScaleCount());
		if (currentScaleIndex == boundedTargetScaleIndex) {
			return true;
		}

		ZoomRestoreState zoomRestoreState = getZoomRestoreState(scrollPane);
		double keptLeftDate = resolveKeptLeftDate(zoomRestoreState,
				resolveViewportLeftEdgeDate(coord::toTime, viewport.getViewPosition()));
		while (currentScaleIndex < boundedTargetScaleIndex) {
			performZoomStep(scrollPane, coord, zoomRestoreState, keptLeftDate, false);
			int nextScaleIndex = coord.getTimescaleManager().getCurrentScaleIndex();
			if (nextScaleIndex == currentScaleIndex) {
				break;
			}
			currentScaleIndex = nextScaleIndex;
		}
		while (currentScaleIndex > boundedTargetScaleIndex) {
			performZoomStep(scrollPane, coord, zoomRestoreState, keptLeftDate, true);
			int nextScaleIndex = coord.getTimescaleManager().getCurrentScaleIndex();
			if (nextScaleIndex == currentScaleIndex) {
				break;
			}
			currentScaleIndex = nextScaleIndex;
		}
		return true;
	}

	private void performZoomStep(JScrollPane scrollPane, CoordinatesConverter coord, ZoomRestoreState zoomRestoreState,
			double keptLeftDate, boolean zoomIn) {
		performZoomStep(scrollPane, coord, zoomRestoreState, keptLeftDate, zoomIn, -1);
	}

	private void performZoomStep(JScrollPane scrollPane, CoordinatesConverter coord, ZoomRestoreState zoomRestoreState,
			double keptLeftDate, boolean zoomIn, int cursorX) {
		boolean zoomed = false;
		if (zoomIn) {
			if (coord.canZoomIn()) {
				coord.zoomIn();
				zoomed = true;
			}
		} else if (coord.canZoomOut()) {
			coord.zoomOut();
			zoomed = true;
		}
		if (!zoomed) {
			return;
		}

		JViewport viewport = scrollPane.getViewport();
		if (viewport == null) {
			return;
		}
		Point newViewPosition = viewport.getViewPosition();
		double minimumLeftDate = resolveMinimumLeftDate(coord.getTimescaleManager().getScale(),
				coord.getProject().getEarliestStartingTaskOrStart(), keptLeftDate);
		double restoreDate = chooseZoomLeftDate(keptLeftDate, minimumLeftDate);
		int restoreX = restoreViewportX(coord::toX, restoreDate);
		newViewPosition.x = cursorX >= 0 ? restoreX - cursorX : restoreX;
		clampViewPosition(viewport, newViewPosition);
		setViewportViewPosition(viewport, newViewPosition);
		updateKeptLeftDate(zoomRestoreState, resolveViewportLeftEdgeDate(coord::toTime, newViewPosition));
	}

	static double resolveViewportLeftEdgeDate(DoubleUnaryOperator xToTime, Point viewPosition) {
		int leftEdgeX = viewPosition == null ? 0 : Math.max(0, viewPosition.x);
		return xToTime.applyAsDouble(leftEdgeX);
	}

	static int restoreViewportX(DoubleUnaryOperator timeToX, double restoreDate) {
		return (int) Math.round(timeToX.applyAsDouble(restoreDate));
	}

	static double chooseZoomLeftDate(double keptLeftDate, double minimumLeftDate) {
		return Math.max(keptLeftDate, minimumLeftDate);
	}

	public static int clampTargetScaleIndex(int targetScaleIndex, int scaleCount) {
		if (scaleCount <= 0) {
			return 0;
		}
		return Math.max(0, Math.min(targetScaleIndex, scaleCount - 1));
	}

	static double resolveMinimumLeftDate(TimeScale scale, long earliestTaskDate, double fallbackDate) {
		if (scale == null || earliestTaskDate <= 0L) {
			return fallbackDate;
		}
		var calendar = DateTime.calendarInstance();
		calendar.setTimeInMillis(earliestTaskDate);
		scale.floor1(calendar);
		calendar.add(scale.getCalendarField1(), -scale.getNumber1());
		return calendar.getTimeInMillis();
	}

	private ZoomRestoreState createZoomRestoreState(JScrollPane scrollPane) {
		return isGanttScrollPane(scrollPane) ? new ZoomRestoreState() : null;
	}

	private double resolveKeptLeftDate(ZoomRestoreState state, double currentLeftDate) {
		return state == null ? currentLeftDate : state.resolve(currentLeftDate);
	}

	private void updateKeptLeftDate(ZoomRestoreState state, double leftDate) {
		if (state == null) {
			return;
		}
		state.update(leftDate);
	}

	private boolean isGanttScrollPane(JScrollPane scrollPane) {
		if (scrollPane == null || scrollPane.getViewport() == null) {
			return false;
		}
		return scrollPane.getViewport().getView() instanceof Gantt;
	}

	private ZoomRestoreState getZoomRestoreState(JScrollPane scrollPane) {
		if (scrollPane == scrollPane1) {
			return scrollPane1ZoomRestoreState;
		}
		if (scrollPane == scrollPane2) {
			return scrollPane2ZoomRestoreState;
		}
		return null;
	}

	private void registerGanttSynchronizer(JScrollPane scrollPane, ZoomRestoreState state) {
		if (state == null || scrollPane == null || scrollPane.getViewport() == null) {
			return;
		}
		Component view = scrollPane.getViewport().getView();
		if (view != null) {
			ganttSynchronizers.put(view, this);
		}
	}

	private void unregisterGanttSynchronizer(JScrollPane scrollPane) {
		if (scrollPane == null || scrollPane.getViewport() == null) {
			return;
		}
		Component view = scrollPane.getViewport().getView();
		if (view != null) {
			ganttSynchronizers.remove(view);
		}
	}

	private AdjustmentListener createHorizontalAdjustmentListener(final ZoomRestoreState state) {
		return new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				if (programmaticHorizontalScrollCount > 0) {
					return;
				}
				JScrollPane owner = e.getAdjustable() == scrollPane1.getHorizontalScrollBar() ? scrollPane1 : scrollPane2;
				JViewport viewport = owner == null ? null : owner.getViewport();
				if (viewport == null) {
					return;
				}
				if (e.getValueIsAdjusting() || e.getAdjustmentType() == AdjustmentEvent.TRACK || e.getAdjustmentType() == AdjustmentEvent.UNIT_INCREMENT
						|| e.getAdjustmentType() == AdjustmentEvent.UNIT_DECREMENT || e.getAdjustmentType() == AdjustmentEvent.BLOCK_INCREMENT
						|| e.getAdjustmentType() == AdjustmentEvent.BLOCK_DECREMENT) {
					updateKeptLeftDate(state, resolveViewportLeftEdgeDate(coord -> coord, viewport.getViewPosition()));
				}
			}
		};
	}

	private void setViewportViewPosition(JViewport viewport, Point viewPosition) {
		programmaticHorizontalScrollCount++;
		try {
			viewport.setViewPosition(viewPosition);
		} finally {
			programmaticHorizontalScrollCount--;
		}
	}

	private void invalidateZoomRestoreState(ZoomRestoreState state) {
		if (state != null) {
			state.reset();
		}
	}

	private void invalidateAllZoomRestoreState() {
		invalidateZoomRestoreState(scrollPane1ZoomRestoreState);
		invalidateZoomRestoreState(scrollPane2ZoomRestoreState);
	}

	public static void invalidateZoomRestore(Component component) {
		if (component == null) {
			return;
		}
		Component current = component;
		while (current != null) {
			ScrollPaneSynchronizer synchronizer = (ScrollPaneSynchronizer) ganttSynchronizers.get(current);
			if (synchronizer != null) {
				synchronizer.invalidateAllZoomRestoreState();
				return;
			}
			current = current.getParent();
		}
	}

	private int getWheelSteps(MouseWheelEvent e) {
		double rotation = e.getPreciseWheelRotation();
		if (rotation == 0.0d) {
			return 0;
		}
		return rotation > 0.0d ? 1 : -1;
	}

	private int getVerticalScrollStep(JScrollPane scrollPane) {
		int synchronizedRowHeight = getSynchronizedRowHeight();
		if (synchronizedRowHeight > 0) {
			return synchronizedRowHeight * 5;
		}

		JViewport viewport = scrollPane.getViewport();
		if (viewport != null) {
			Component view = viewport.getView();
			if (view instanceof javax.swing.JTable) {
				int rowHeight = ((javax.swing.JTable) view).getRowHeight();
				if (rowHeight > 0) {
					return rowHeight * 5;
				}
			}
			if (view instanceof Scrollable) {
				Rectangle visibleRect = viewport.getViewRect();
				int step = ((Scrollable) view).getScrollableUnitIncrement(visibleRect, SwingConstants.VERTICAL, 1);
				if (step > 0) {
					return step * 5;
				}
			}
		}
		int fallback = scrollPane.getVerticalScrollBar().getUnitIncrement(1);
		if (fallback <= 0) {
			fallback = 1;
		}
		return fallback * 5;
	}

	private int getSynchronizedRowHeight() {
		int leftRowHeight = getViewRowHeight(scrollPane1);
		if (leftRowHeight > 0) {
			return leftRowHeight;
		}
		return getViewRowHeight(scrollPane2);
	}

	private int getViewRowHeight(JScrollPane scrollPane) {
		if (scrollPane == null) {
			return -1;
		}
		JViewport viewport = scrollPane.getViewport();
		if (viewport == null) {
			return -1;
		}
		Component view = viewport.getView();
		if (view instanceof javax.swing.JTable) {
			int rowHeight = ((javax.swing.JTable) view).getRowHeight();
			return rowHeight > 0 ? rowHeight : -1;
		}
		if (view instanceof com.microproject.pm.graphic.gantt.Gantt) {
			int rowHeight = ((com.microproject.pm.graphic.gantt.Gantt) view).getRowHeight();
			return rowHeight > 0 ? rowHeight : -1;
		}
		return -1;
	}

	private int getHorizontalScrollStep(JScrollPane scrollPane, int direction) {
		int step = scrollPane.getHorizontalScrollBar().getUnitIncrement(direction);
		if (step <= 0) {
			step = 1;
		}
		return step;
	}

	private void clampViewPosition(JViewport viewport, Point viewPosition) {
		Dimension viewSize = viewport.getViewSize();
		Dimension extentSize = viewport.getExtentSize();
		int maxX = Math.max(0, viewSize.width - extentSize.width);
		int maxY = Math.max(0, viewSize.height - extentSize.height);
		if (viewPosition.x < 0) {
			viewPosition.x = 0;
		} else if (viewPosition.x > maxX) {
			viewPosition.x = maxX;
		}
		if (viewPosition.y < 0) {
			viewPosition.y = 0;
		} else if (viewPosition.y > maxY) {
			viewPosition.y = maxY;
		}
	}

	private void registerMouseWheelTargets(JScrollPane scrollPane, MouseWheelListener listener, ArrayList targets) {
		registerMouseWheelTargets(scrollPane.getViewport(), listener, targets);
		registerMouseWheelTargets(scrollPane.getViewport() == null ? null : scrollPane.getViewport().getView(), listener, targets);
		if (scrollPane.getRowHeader() != null) {
			registerMouseWheelTargets(scrollPane.getRowHeader(), listener, targets);
			registerMouseWheelTargets(scrollPane.getRowHeader().getView(), listener, targets);
		}
		if (scrollPane.getColumnHeader() != null) {
			registerMouseWheelTargets(scrollPane.getColumnHeader(), listener, targets);
			registerMouseWheelTargets(scrollPane.getColumnHeader().getView(), listener, targets);
		}
	}

	private void registerMouseWheelTargets(Component component, MouseWheelListener listener, ArrayList targets) {
		if (component == null || listener == null) {
			return;
		}
		component.addMouseWheelListener(listener);
		targets.add(component);
		if (component instanceof Container) {
			Component[] children = ((Container) component).getComponents();
			for (int i = 0; i < children.length; i++) {
				registerMouseWheelTargets(children[i], listener, targets);
			}
		}
	}

	private void unregisterMouseWheelTargets(ArrayList targets, MouseWheelListener listener) {
		if (listener == null) {
			targets.clear();
			return;
		}
		for (int i = 0; i < targets.size(); i++) {
			Component component = (Component) targets.get(i);
			component.removeMouseWheelListener(listener);
		}
		targets.clear();
	}

	private static ScrollPaneSynchronizer findSynchronizer(Component component) {
		Component current = component;
		while (current != null) {
			ScrollPaneSynchronizer synchronizer = (ScrollPaneSynchronizer) ganttSynchronizers.get(current);
			if (synchronizer != null) {
				return synchronizer;
			}
			current = current.getParent();
		}
		return null;
	}

	private static JScrollPane findScrollPane(Component component) {
		Component current = component;
		while (current != null && !(current instanceof JScrollPane)) {
			current = current.getParent();
		}
		return current instanceof JScrollPane ? (JScrollPane) current : null;
	}

    /**
     * @return Returns the orientation.
     */
    public int getOrientation() {
        return orientation;
    }
    /**
     * @param orientation The orientation to set.
     */
    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }
    /**
     * @return Returns the scrollPane1.
     */
    public JScrollPane getScrollPane1() {
        return scrollPane1;
    }
    /**
     * @param scrollPane1 The scrollPane1 to set.
     */
    public void setScrollPane1(JScrollPane scrollPane1) {
        this.scrollPane1 = scrollPane1;
    }
    /**
     * @return Returns the scrollPane2.
     */
    public JScrollPane getScrollPane2() {
        return scrollPane2;
    }
    /**
     * @param scrollPane2 The scrollPane2 to set.
     */
    public void setScrollPane2(JScrollPane scrollPane2) {
        this.scrollPane2 = scrollPane2;
    }
}


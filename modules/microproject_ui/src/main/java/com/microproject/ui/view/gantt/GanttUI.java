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
package com.microproject.ui.view.gantt;

import com.microproject.ui.view.graph.GraphUI;
import com.microproject.ui.view.graph.GraphZone;
import com.microproject.ui.view.model.cache.GraphicNode;
import com.microproject.ui.view.timescale.CoordinatesConverter;
import com.microproject.model.task.Task;

/**
 *
 */
public class GanttUI extends GraphUI{
	private static final long serialVersionUID = -5650277887013240824L;

	public static final int PROGRESS_BAR_ZONE_ID=1;
	/**
	 *
	 */
	public GanttUI(Gantt gantt) {
		super(gantt,new GanttRenderer(gantt));
		interactor=new GanttInteractor(this);
	}

	public GanttRenderer getGanttRenderer() {
		return (GanttRenderer)graphRenderer;
	}

	public GanttInteractor getInteractor() {
		return (GanttInteractor) interactor;
	}





    public double getBarY(int row){
		return ((Gantt)graph).getRowHeights().yOf(row)+config.getGanttBarYOffset();
    }

	public GraphZone getNodeAt(double x,double y){
		int row=getRowAt(y);
		if (row<0||row>=graph.getModel().getCache().getSize()) return null;
		GraphicNode node=(GraphicNode)graph.getModel().getCache().getElementAt(row);
		double y0=getBarY(row)+node.getGanttShapeOffset();//row*rowHeight+config.getGanttBarYOffset();
		double h=node.getGanttShapeHeight();
		double delta=config.getSelectionSquare();
		if (y<y0/*-delta*/||y>y0/*+delta*/+h) return null;
		CoordinatesConverter coord=getCoord();
		double t=coord.toTime(x);
		double deltat=coord.toDuration(delta);
		GanttRenderer renderer = getGanttRenderer();
		GraphZone zone = new GraphZone();
		if (((Gantt) graph).isProgressLineEnabled()
				&& renderer.shouldIncludeInProgressLine(node)
				&& isWithinProgressLineHitTarget(renderer, node, x, y)) {
			zone.setObject(node);
			zone.setZoneId(PROGRESS_BAR_ZONE_ID);
			return zone;
		}
		if  (node.contains(t,deltat,deltat,coord)==null) return null;
		double progessH=config.getGanttProgressBarHeight();
		zone.setObject(node);
		if (y>=y0+h/2-progessH/2&&y<y0+h/2+progessH/2) zone.setZoneId(PROGRESS_BAR_ZONE_ID);
		return zone;


    }

	/**
	 * Returns the task represented by a visible Gantt row without requiring the
	 * pointer to be over its bar.  Selection uses this row-level hit target;
	 * editing gestures continue to use {@link #getNodeAt(double, double)} so a
	 * click in calendar whitespace can never start moving a task bar.
	 */
	public GraphicNode getTaskRowAt(double y) {
		Gantt gantt = (Gantt) graph;
		int row = getRowAt(y);
		if (row < 0 || graph.getModel().getCache() == null || row >= graph.getModel().getCache().getSize()) {
			return null;
		}
		Object element = graph.getModel().getCache().getElementAt(row);
		return element instanceof GraphicNode node ? node : null;
	}

	/** Returns -1 for y coordinates outside the actual row area. */
	public int getRowAt(double y) {
		Gantt gantt = (Gantt) graph;
		if (gantt.getCache() == null || y < 0 || y >= gantt.getRowHeights().totalHeight(gantt.getCache().getSize())) return -1;
		return gantt.getRowHeights().rowAt((int) Math.floor(y));
	}



	public CoordinatesConverter getCoord() {
		return ((GanttParams)graphRenderer.getGraphInfo()).getCoord();
	}

	private boolean isWithinProgressLineHitTarget(GanttRenderer renderer, GraphicNode node, double x, double y) {
		Task task = (Task) node.getNode().getImpl();
		double radius = Math.max(config.getSelectionSquare(),
			Math.max(config.getGanttProgressBarHeight() / 2.0d, 4.0d));
		return isWithinProgressLineHitTarget(x, y,
			renderer.getProgressLineX(getCoord(), task), renderer.getProgressLineY(node), radius);
	}

	static boolean isWithinProgressLineHitTarget(double x, double y,
			double progressX, double progressY, double radius) {
		return Math.abs(x - progressX) <= radius && Math.abs(y - progressY) <= radius;
	}
}


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

import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

import com.microproject.pm.graphic.graph.GraphUI;
import com.microproject.pm.graphic.graph.GraphZone;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.GraphicDependency;
import com.microproject.pm.graphic.model.cache.ViewNodeModelCache;
import com.microproject.pm.graphic.model.cache.TaskProjectionSnapshot;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;

/**
 *
 */
public class GanttUI extends GraphUI{
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
    	return row*((Gantt)graph).getRowHeight()+config.getGanttBarYOffset();
    }

    public GraphZone getNodeAt(double x,double y){
		double rowHeight=((Gantt)graph).getRowHeight();
		int row=(int)Math.floor(y/rowHeight);
		if (!(graph.getModel().getCache() instanceof ViewNodeModelCache cache)) return null;
		if (!getGanttRenderer().isGeometryCurrent()) return null;
		ViewNodeModelCache.InstalledProjectionSnapshot installed = cache.getInstalledProjectionSnapshot();
		if (!isCurrentDomainRevision(installed)) return null;
		if (row<0||row>=installed.topology().rows().size()) return null;
		TaskProjectionSnapshot.Row value = installed.values().rowAt(row);
		var projected = installed.topology().rows().get(row);
		if (value == null || !value.key().equals(projected.key()) || !value.schedule()) return null;
		GraphicNode node=projected.node();
		GanttBarGeometry geometry = getGanttRenderer().getBarGeometry(value.key());
		double y0=getBarY(row)+geometry.offset();//row*rowHeight+config.getGanttBarYOffset();
		double h=geometry.height();
		double delta=config.getSelectionSquare();
		if (y<y0/*-delta*/||y>y0/*+delta*/+h) return null;
		CoordinatesConverter coord=getCoord();
		double t=coord.toTime(x);
		double deltat=coord.toDuration(delta);
		boolean contains = false;
		java.util.List<TaskProjectionSnapshot.Interval> intervals = value.intervals().isEmpty()
				? java.util.List.of(new TaskProjectionSnapshot.Interval(value.start(), value.end())) : value.intervals();
		for (TaskProjectionSnapshot.Interval interval : intervals)
			if (t >= interval.start() - deltat && t <= interval.end() + deltat) { contains = true; break; }
		if (!contains) return null;
		double progessH=config.getGanttProgressBarHeight();
		GraphZone zone=new GraphZone();
		zone.setObject(node);
		if (y>=y0+h/2-progessH/2&&y<y0+h/2+progessH/2) zone.setZoneId(PROGRESS_BAR_ZONE_ID);
		return zone;


    }

	@Override public GraphZone getLinkAt(double x, double y) {
		if (!(graph.getModel().getCache() instanceof ViewNodeModelCache cache)) return null;
		if (!getGanttRenderer().isGeometryCurrent()) return null;
		ViewNodeModelCache.InstalledProjectionSnapshot installed = cache.getInstalledProjectionSnapshot();
		if (!isCurrentDomainRevision(installed)) return null;
		for (TaskProjectionSnapshot.Edge edge : installed.values().edges()) {
			GeneralPath path = getGanttRenderer().findDependencyPath(edge);
			if (path != null && hits(path, x, y)) {
				GraphicDependency dependency = resolveDependency(cache, installed, edge);
				return dependency == null ? null : new GraphZone(dependency);
			}
		}
		return null;
	}

	private boolean isCurrentDomainRevision(ViewNodeModelCache.InstalledProjectionSnapshot installed) {
		var project = graph.getProject();
		return project != null && domainRevisionMatches(installed.values().domainRevision(),
				project.getDomainChangeJournal().revision());
	}

	static boolean domainRevisionMatches(long installedRevision, long currentRevision) {
		return installedRevision == currentRevision;
	}

	private boolean hits(GeneralPath path, double x, double y) {
		double delta = config.getSelectionSquare();
		if (delta == 0) return path.contains(x, y);
		Rectangle2D zone = new Rectangle2D.Double(x - delta, y - delta, 2 * delta + 1, 2 * delta + 1);
		if (!path.intersects(zone)) return false;
		double flatness = config.getLinkFlatness();
		double lastX = -1.0d;
		double lastY = -1.0d;
		for (PathIterator iterator = flatness <= 0 ? path.getPathIterator(null)
				: path.getPathIterator(null, flatness); !iterator.isDone(); iterator.next()) {
			int type = iterator.currentSegment(segment);
			if (type == PathIterator.SEG_LINETO
					&& Line2D.ptSegDist(lastX, lastY, segment[0], segment[1], x, y) <= delta) return true;
			lastX = segment[0];
			lastY = segment[1];
		}
		return false;
	}

	private static GraphicDependency resolveDependency(ViewNodeModelCache cache,
			ViewNodeModelCache.InstalledProjectionSnapshot installed, TaskProjectionSnapshot.Edge edge) {
		int predecessorRow = installed.topology().rowOf(edge.predecessor());
		int successorRow = installed.topology().rowOf(edge.successor());
		if (predecessorRow < 0 || successorRow < 0) return null;
		GraphicNode predecessor = installed.topology().rows().get(predecessorRow).node();
		GraphicNode successor = installed.topology().rows().get(successorRow).node();
		@SuppressWarnings("unchecked")
		Iterator<GraphicDependency> dependencies = cache.getVisibleDependencies().getIterator();
		while (dependencies.hasNext()) {
			GraphicDependency dependency = dependencies.next();
			if (dependency.getPredecessor() == predecessor && dependency.getSuccessor() == successor
					&& dependency.getType() == edge.type()) return dependency;
		}
		return null;
	}



	public CoordinatesConverter getCoord() {
		return ((GanttParams)graphRenderer.getGraphInfo()).getCoord();
	}
}

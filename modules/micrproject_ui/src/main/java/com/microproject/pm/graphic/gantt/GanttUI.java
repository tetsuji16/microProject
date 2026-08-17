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

import com.microproject.pm.graphic.graph.GraphUI;
import com.microproject.pm.graphic.graph.GraphZone;
import com.microproject.pm.graphic.model.cache.GraphicNode;
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
		if (row<0||row>=graph.getModel().getCache().getSize()) return null;
		GraphicNode node=(GraphicNode)graph.getModel().getCache().getElementAt(row);
		double y0=getBarY(row)+node.getGanttShapeOffset();//row*rowHeight+config.getGanttBarYOffset();
		double h=node.getGanttShapeHeight();
		double delta=config.getSelectionSquare();
		if (y<y0/*-delta*/||y>y0/*+delta*/+h) return null;
		CoordinatesConverter coord=getCoord();
		double t=coord.toTime(x);
		double deltat=coord.toDuration(delta);
		if  (node.contains(t,deltat,deltat,coord)==null) return null;
		double progessH=config.getGanttProgressBarHeight();
		GraphZone zone=new GraphZone();
		zone.setObject(node);
		if (y>=y0+h/2-progessH/2&&y<y0+h/2+progessH/2) zone.setZoneId(PROGRESS_BAR_ZONE_ID);
		return zone;


    }



	public CoordinatesConverter getCoord() {
		return ((GanttParams)graphRenderer.getGraphInfo()).getCoord();
	}
}


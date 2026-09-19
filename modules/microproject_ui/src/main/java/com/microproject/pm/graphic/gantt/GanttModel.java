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

import java.awt.geom.AffineTransform;
import java.io.Serializable;

import com.microproject.pm.graphic.graph.GraphModel;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.association.InvalidAssociationException;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.pm.task.Project;
import com.microproject.timescale.TimeScaleEvent;
import com.microproject.timescale.TimeScaleListener;

/**
 *
 */
public class GanttModel extends GraphModel implements TimeScaleListener, Serializable {
	private static final long serialVersionUID = 357529278107413145L;

	//timescale
	protected CoordinatesConverter coord;
    
	//baselines
	protected int rowHeight;
	
    
    
    
    
	public GanttModel(Project project,String viewName) {
		super(project,viewName);
		rowHeight=GraphicConfiguration.getInstance().getRowHeight();
	}

	
	
	public AffineTransform getTransform(double w){
		var sx = ((double) (coord.getEnd() - coord.getOrigin())) / w;
		var sy = 1.0 / getRowHeight();
		return new AffineTransform(sx,0,0,sy,coord.getOrigin(),0);
	}
	
	
//cache: edges	
	public void createEdge(GraphicNode startNode,GraphicNode endNode) throws InvalidAssociationException{
		getCache().createDependency(startNode,endNode);
	}

	
	
	
	
//timescale	
    public CoordinatesConverter getCoord() {
        return coord;
    }
    public void setCoord(CoordinatesConverter coord) {
        if (this.coord != null) {
        	this.coord.removeTimeScaleListener(this);
        }
        this.coord = coord;
		coord.addTimeScaleListener(this);
    }
	public void timeScaleChanged(TimeScaleEvent e) {
//		if (e.getType()!=TimeScaleEvent.END_ONLY_CHANGE){
//			//update all
//			Map propertyMap=new Hashtable();
//			for (int i=0; i<cache.getSize();i++){
//				GraphicNode gnode=(GraphicNode)cache.getElementAt(i);
//				setCellBounds(propertyMap,gnode,i);
//			}
//			edit(propertyMap, null, null, null,"NO_SETVALUE");
//		}
	}
	
	
	
    public int getRowHeight() {
        return rowHeight;
    }
    public void setRowHeight(int rowHeight) {
        this.rowHeight = rowHeight;
    }

	
//	public void splitBar(GanttBarCell cell,double x){
//		long t=(long)coord.toTime(x);	
//		Schedule task=(Schedule)cell.getGanttCell().getNode().getNode().getImpl();
//		ScheduleService.getInstance().split(this,task,t,t);
//	}
	
//progress
//	public void updateProgress(GraphicNode node){
//		long completed=(long)coord.toTime(node.getGanttCell().getProgress());
//		Schedule schedule=(Schedule)node.getNode().getImpl();
//		ScheduleService.getInstance().setCompleted(this,schedule,completed);
//	}

	
	
	
}


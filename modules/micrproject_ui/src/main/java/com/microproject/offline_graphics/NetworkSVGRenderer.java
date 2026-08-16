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
package com.microproject.offline_graphics;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import com.microproject.pm.graphic.graph.GraphParams;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.graphic.model.event.CacheListener;
import com.microproject.pm.graphic.model.event.CompositeCacheEvent;
import com.microproject.pm.graphic.network.NetworkParamsImpl;
import com.microproject.pm.graphic.network.NetworkRenderer;
import com.microproject.pm.graphic.pert.PertLayout;
import com.microproject.pm.graphic.pert.PertRenderer;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.graphic.xbs.XbsLayout;
import com.microproject.pm.graphic.xbs.XbsRenderer;
import com.microproject.configuration.Dictionary;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.pm.task.Project;

public class NetworkSVGRenderer implements SVGRenderer, CacheListener, Cloneable{
	public static final int PERT=1;
	public static final int WBS=2;
	public static final int RBS=3;
	protected NetworkParamsImpl params;
	protected CoordinatesConverter coord;
	protected SpreadSheet spreadSheet;
	protected NetworkRenderer renderer;
	protected Project project;
	public void init(Project project, ReferenceNodeModelCache refCache) {
		init(project, NodeModelCacheFactory.getInstance().createFilteredCache((ReferenceNodeModelCache)refCache,"Network",null),PERT,-1);
	}
	public void init(Project project, NodeModelCache cache,int type,int scale) {
		this.project=project;
		params=new NetworkParamsImpl();
		params.setNetworkLayout(type==PERT?new PertLayout(params):new XbsLayout(params));
		String viewName=null;
//		NodeModelCache cache=null;
		switch (type) {
			case PERT:
				viewName="pert";
//				cache=NodeModelCacheFactory.getInstance().createFilteredCache((ReferenceNodeModelCache)refCache,"Network",null);
				renderer=new PertRenderer(params);
				renderer.setVertical(false);
				break;
			case WBS:
				viewName="WBS";
//				cache=NodeModelCacheFactory.getInstance().createFilteredCache((ReferenceNodeModelCache)refCache,"WBS",null);
				renderer=new XbsRenderer(params);
				renderer.setVertical(true);
				break;
			case RBS:
				viewName="RBS";
//				cache=NodeModelCacheFactory.getInstance().createFilteredCache((ReferenceNodeModelCache)refCache,"RBS",null);
				renderer=new XbsRenderer(params);
				renderer.setVertical(true);
				break;
		}
		params.setBarStyles((BarStyles) Dictionary.get(BarStyles.category,viewName));
		params.setZoom(scale);
		params.setCache(cache);
		cache.addNodeModelListener(this);
//		renderer=new PertRenderer(params);
//		renderer.setVertical(false);
		cache.update();
		params.updateLayout();
	}


	public Object clone(){
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
	public SVGRenderer createSafePrintCopy(){
		NetworkSVGRenderer c=(NetworkSVGRenderer)clone();
		c.params=(NetworkParamsImpl)c.params.createSafePrintCopy();
		c.renderer.setGraphInfo(c.params);
		return c;
	}


	public void paint(Graphics2D g){
		paint(g,-1,-1);
	}
	public void paint(Graphics2D g,int prow,int pcol){
		Rectangle drawingBounds=params.getDrawingBounds();
		if (prow==-1){
			g.drawRect(0, 0, drawingBounds.width, drawingBounds.height);
			renderer.paint(g);
		}else{
			Rectangle printBounds=params.getPrintBounds();
			Rectangle networkPrintBounds=params.getNetworkPrintBounds(prow, pcol);
			g.translate(-pcol*printBounds.width,-prow*printBounds.height);
			//g.draw(networkPrintBounds);
			renderer.paint(g,networkPrintBounds);

		}
	 }

	public Dimension getCanvasSize(){
		return params.getDrawingBounds().getSize();
	}

	public void graphicNodesCompositeEvent(CompositeCacheEvent e) {
		params.getNetworkLayout().graphicNodesCompositeEvent(e);
	}

	public GraphParams getParams() {
		return params;
	}
	public Project getProject() {
		return project;
	}

}


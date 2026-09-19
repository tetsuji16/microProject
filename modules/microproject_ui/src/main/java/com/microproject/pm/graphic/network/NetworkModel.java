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
package com.microproject.pm.graphic.network;

import java.awt.Rectangle;

import com.microproject.pm.graphic.graph.GraphModel;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.event.CompositeCacheEvent;
import com.microproject.pm.graphic.network.layout.NetworkLayout;
import com.microproject.association.InvalidAssociationException;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.pm.task.Project;

/**
 *
 */
public class NetworkModel extends GraphModel{
	protected NetworkLayout networkLayout;
	public NetworkModel(Project project,String viewName) {
		super(project,viewName);
	}
	
	public void setNetworkLayout(NetworkLayout networkLayout) {
		this.networkLayout = networkLayout;
	}

	public void setCache(NodeModelCache cache){
		networkLayout.setCache(cache);
		super.setCache(cache);
	}
	public void setBarStyles(BarStyles barStyles) {
		networkLayout.setBarStyles(barStyles);
		super.setBarStyles(barStyles);
	}
	
//cache: edges	
	public void createEdge(GraphicNode startNode,GraphicNode endNode) throws InvalidAssociationException{
		getCache().createDependency(startNode,endNode);
	}
	
	public void updateCellBounds(){
		networkLayout.updateBounds();
	}
	
	public NetworkLayout getNetworkLayout() {
		return networkLayout;
	}
	public Rectangle getBounds(){
		return networkLayout.getBounds();
	}

	public void graphicNodesCompositeEvent(CompositeCacheEvent compositeEvent){
		networkLayout.graphicNodesCompositeEvent(compositeEvent);
		super.graphicNodesCompositeEvent(compositeEvent);
	}
	
	
	
	
}


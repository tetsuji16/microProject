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
package com.microproject.pm.graphic.pert;

import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ListIterator;


import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.event.CompositeCacheEvent;
import com.microproject.pm.graphic.network.NetworkDisplayOptions;
import com.microproject.pm.graphic.network.NetworkParams;
import com.microproject.pm.graphic.network.layout.AbstractNetworkLayout;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.graphic.configuration.TexturedShape;

/**
 *
 */
public class PertLayout extends AbstractNetworkLayout {
	protected DependencyGraph dependencyGraph=new DependencyGraph();
	public PertLayout(NetworkParams network){
		super(network);
	}
	public void setCache(NodeModelCache cache){
		super.setCache(cache);
		dependencyGraph.setCache(cache);
	}
	
	
	public void updateBounds(){
			dependencyGraph.updatePertLevels();
		
			GraphicConfiguration config=GraphicConfiguration.getInstance();
		
			Point2D origin=new Point2D.Double(config.getPertXOffset(),config.getPertYOffset());
			NetworkDisplayOptions options=network.getDisplayOptions();
			java.awt.Dimension boxSize=options.boxSize(config.getPertCellWidth(),config.getPertCellHeight());
			int spacing=options.getBoxSpacing();
			Rectangle2D ref=new Rectangle2D.Double(config.getPertXOffset(),config.getPertYOffset(),boxSize.width,boxSize.height);
			int row=0;
			int col=-1;
			setEmpty();
		
			// Get manual positions from Project if available
					java.util.Map<Long, java.awt.geom.Point2D> manualPositions = null;
					if (network instanceof com.microproject.pm.graphic.network.Network) {
						com.microproject.pm.graphic.network.Network net = (com.microproject.pm.graphic.network.Network) network;
						com.microproject.pm.task.Project project = net.getProject();
						if (project != null && project.getManualPertPositions() != null && !project.getManualPertPositions().isEmpty()) {
							manualPositions = project.getManualPertPositions();
						}
					}
		
			for (ListIterator<GraphicNode> i = cache.getIterator(); i.hasNext();) {
				GraphicNode current = i.next();
				int currentCol=cache.getPertLevel(current)-1;
				if (currentCol<=col) row++;
				col=currentCol;
			
				TexturedShape texturedShape=findShape(current);
				if (texturedShape==null) continue;
			
				double centerX, centerY;
				// Check if this node has a manual position
				Object impl = current.getNode().getImpl();
				if (manualPositions != null && impl instanceof com.microproject.pm.task.Task) {
					Long taskUid = ((com.microproject.pm.task.Task) impl).getUniqueId();
					java.awt.geom.Point2D manualPos = manualPositions.get(taskUid);
					if (manualPos != null) {
						// Use manual position (already at 100% zoom logical coordinates)
						centerX = manualPos.getX();
						centerY = manualPos.getY();
					} else {
						// Calculate automatic position
						centerX = origin.getX() + (ref.getMaxX() + spacing) * col + ref.getWidth() / 2;
						centerY = origin.getY() + (ref.getMaxY() + spacing) * row + ref.getHeight() / 2;
					}
				} else {
					// Calculate automatic position
					centerX = origin.getX() + (ref.getMaxX() + spacing) * col + ref.getWidth() / 2;
					centerY = origin.getY() + (ref.getMaxY() + spacing) * row + ref.getHeight() / 2;
				}
			
				GeneralPath shape = texturedShape.toGeneralPath(ref.getWidth(), ref.getHeight(), centerX - ref.getWidth() / 2, centerY, null);
				current.setPertShape(shape, centerX, centerY);
				Rectangle cellBounds = network.scale(shape.getBounds());
				if (isEmpty())
					bounds.setBounds(cellBounds);
				else Rectangle.union(bounds, cellBounds, bounds);
			}
			fireLayoutChanged();
		}
	
	public void graphicNodesCompositeEvent(CompositeCacheEvent compositeEvent) {
 
		if (compositeEvent.getRemovedEdges()!=null) dependencyGraph.removeDependencies(compositeEvent.getRemovedEdges());
	    if (compositeEvent.getInsertedEdges()!=null) dependencyGraph.insertDependencies(compositeEvent.getInsertedEdges());
		updateBounds();
	}
}


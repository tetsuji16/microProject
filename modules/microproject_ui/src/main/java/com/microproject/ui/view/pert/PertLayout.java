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
package com.microproject.ui.view.pert;

import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ListIterator;
import java.util.Map;


import com.microproject.ui.view.model.cache.GraphicNode;
import com.microproject.ui.view.model.cache.NodeModelCache;
import com.microproject.ui.view.model.event.CompositeCacheEvent;
import com.microproject.ui.view.network.NetworkDisplayOptions;
import com.microproject.ui.view.network.NetworkParams;
import com.microproject.view.network.PertManualPositions;
import com.microproject.ui.view.network.layout.AbstractNetworkLayout;
import com.microproject.view.configuration.GraphicConfiguration;
import com.microproject.view.configuration.TexturedShape;

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
			// Get manual positions from network params
			Map<Long, PertManualPositions.Position> manualPositions = network.getManualPositions();
			for (ListIterator<GraphicNode> i = cache.getIterator(); i.hasNext();) {
				GraphicNode current = i.next();
				int currentCol=cache.getPertLevel(current)-1;
				if (currentCol<=col) row++;
				col=currentCol;

				TexturedShape texturedShape=findShape(current);
				if (texturedShape==null) continue;

				// Check if this task has a manual position
				Long taskUid = current.getNode().getImpl() instanceof com.microproject.model.task.Task
					? ((com.microproject.model.task.Task) current.getNode().getImpl()).getUniqueId()
					: null;
				PertManualPositions.Position manualPos = taskUid != null ? manualPositions.get(taskUid) : null;

				double centerX, centerY;
				boolean manuallyPositioned = manualPos != null;
				if (manuallyPositioned) {
					// Use manual position (logical coordinates at 100% zoom)
					centerX = manualPos.x();
					centerY = manualPos.y();
				} else {
					// Calculate automatic position
					centerX=origin.getX()+(ref.getMaxX()+spacing)*col+ref.getWidth()/2;
					centerY=origin.getY()+(ref.getMaxY()+spacing)*row+ref.getHeight()/2;
				}
				double shapeY = manuallyPositioned ? centerY - ref.getHeight() / 2 : centerY;
				GeneralPath shape=texturedShape.toGeneralPath(ref.getWidth(),ref.getHeight(),centerX-ref.getWidth()/2,shapeY,null);
				current.setPertShape(shape,centerX,centerY);
				Rectangle cellBounds=network.scale(shape.getBounds());
				if (isEmpty())
					bounds.setBounds(cellBounds);
					else Rectangle.union(bounds,cellBounds,bounds);
			}
			fireLayoutChanged();
		}

	public void graphicNodesCompositeEvent(CompositeCacheEvent compositeEvent) {

		if (compositeEvent.getRemovedEdges()!=null) dependencyGraph.removeDependencies(compositeEvent.getRemovedEdges());
		if (compositeEvent.getInsertedEdges()!=null) dependencyGraph.insertDependencies(compositeEvent.getInsertedEdges());
		updateBounds();
	}
}


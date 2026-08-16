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
package com.microproject.pm.graphic.xbs;

import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Iterator;

import com.microproject.pm.graphic.graph.GraphParams;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.network.NetworkParams;
import com.microproject.pm.graphic.network.NetworkRenderer;

public class XbsRenderer extends NetworkRenderer {
	public XbsRenderer(){
		super();
		//setVertical(true);
	}
	public XbsRenderer(GraphParams graphInfo){
		super(graphInfo);
		//setVertical(true);
	}
	protected GeneralPath getNonScaledShape(GraphicNode node){
		return node.getXbsShape();
	}
	protected Point2D getNonScaledCenter(GraphicNode node){
		return node.getXbsCenter();
	}
	protected void translateNonScaledShape(GraphicNode node, double dx,double dy){
		node.translateXbsShape(dx,dy);
	}
	
	public Iterator getDependenciesIterator(){
   		return ((XbsLayout)((NetworkParams)graphInfo).getNetworkLayout()).getDependencies().iterator();
  	}

}


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
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

import com.microproject.pm.graphic.graph.GraphParams;
import com.microproject.pm.graphic.network.layout.NetworkLayout;
import com.microproject.pm.graphic.network.layout.NetworkLayoutListener;

public interface NetworkParams extends GraphParams,NetworkLayoutListener{	
	public AffineTransform getTransform();
	public int getZoom();

	public default GeneralPath scale(GeneralPath path){
		if (path == null)
			return null;
		GeneralPath transformed=(GeneralPath)path.clone();
		transformed.transform(getTransform());
		return transformed;
	}
	public default Point2D scale(Point2D p){
		AffineTransform t=getTransform();
		return new Point2D.Double(p.getX()*t.getScaleX()+t.getTranslateX(),p.getY()*t.getScaleY()+t.getTranslateY());
	}
	public default Point2D scaleVector(Point2D p){
		AffineTransform t=getTransform();
		return new Point2D.Double(p.getX()*t.getScaleX(),p.getY()*t.getScaleY());
	}
	public default Point2D scaleVector_1(Point2D p){
		AffineTransform t=getTransform();
		return new Point2D.Double(p.getX()/t.getScaleX(),p.getY()/t.getScaleY());
	}

	public default Rectangle scale(Rectangle r){
		AffineTransform t=getTransform();
		if (t==null) return r;
		Rectangle sr=new Rectangle();
		sr.setFrameFromDiagonal(
				r.getMinX()*t.getScaleX()+t.getTranslateX(),
				r.getMinY()*t.getScaleY()+t.getTranslateY(),
				r.getMaxX()*t.getScaleX()+t.getTranslateX(),
				r.getMaxY()*t.getScaleY()+t.getTranslateY()
			);
		return sr;
	}
	public NetworkLayout getNetworkLayout();
	

}


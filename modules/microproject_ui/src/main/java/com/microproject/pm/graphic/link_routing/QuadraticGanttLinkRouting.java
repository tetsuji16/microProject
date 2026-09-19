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
package com.microproject.pm.graphic.link_routing;

import java.awt.geom.GeneralPath;

import com.microproject.pm.dependency.DependencyType;

/**
 *
 */
public class QuadraticGanttLinkRouting extends GanttLinkRouting{
	private static final long serialVersionUID = -1617695945785858247L;

	public void routePath(GeneralPath path,double x0,double y0,double x1,double y1,double y2,double y1floor,double y1ceil,int type){
		this.path=path;
		int fromSign=(type==DependencyType.SF||type==DependencyType.SS)?-1:1;
		int toSign=(type==DependencyType.FS||type==DependencyType.SS)?-1:1;
		
		double fromDeltaX = 5.0;
		double toDeltaX = 15.0;
		double maxDeltaXVerticalArrow = 20;
		double deltaQ=10;
		double deltaXb=Math.min(deltaQ,Math.abs(y1-y0)/2);
		double signY=(y1>=y0)?1:-1;
		
		double x2=x0+fromSign*fromDeltaX;
		double x2b=x2+fromSign*2*deltaXb;
		double x3=x1+toSign*toDeltaX;
		double x3b=x3+toSign*2*deltaXb;
		double x2bb=(x2b+x2)/2;
		double x3bb=(x3b+x3)/2;
		
		double y0b=y0+signY*deltaXb;
		double y1b=y1-signY*deltaXb;
		double y0bb=y0+signY*Math.min(deltaQ,Math.abs(y2-y0)/2);
		double y1bb=y1-signY*Math.min(deltaQ,Math.abs(y1-y2)/2);
		double y2b0=y2-signY*Math.min(deltaQ,Math.abs(y2-y0)/2);
		double y2b1=y2+signY*Math.min(deltaQ,Math.abs(y1-y2)/2);

		resetLinkPoints();
		addLinkPoint(x0,y0);
		if (type==DependencyType.FS&&verticalArrow&&x1+maxDeltaXVerticalArrow>=x2bb){
			double x4=Math.max(x1,x2bb);
			double x4b=x4-deltaXb;
			addLinkPoint(x4b,y0);
			addLinkPoint(x4,y0,false);
			addLinkPoint(x4,y0b,false);
			quad();
			addLinkPoint(x4,(y1>=y0)?y1ceil:y1floor);
			return;
		}
		switch (type) {
		case DependencyType.FS:
		case DependencyType.SF:
			if (type==DependencyType.FS&&x3>=x2||x3<=x2&&type==DependencyType.SF){
				addLinkPoint(x3b,y0);
				addLinkPoint(x3bb,y0,false);
				addLinkPoint(x3bb,y0b,false);
				quad();
				if (y0b!=y1b) addLinkPoint(x3bb,y1b);
				addLinkPoint(x3bb,y1,false);
				addLinkPoint(x3,y1,false);
				quad();
			}else{
				addLinkPoint(x2,y0);
				addLinkPoint(x2bb,y0,false);
				addLinkPoint(x2bb,y0bb,false);
				quad();
				if (y0bb!=y2b0) addLinkPoint(x2bb,y2b0);
				addLinkPoint(x2bb,y2,false);
				addLinkPoint(x2,y2,false);
				quad();
				addLinkPoint(x3,y2);
				addLinkPoint(x3bb,y2,false);
				addLinkPoint(x3bb,y2b1,false);
				quad();
				if (y1bb!=y2b1) addLinkPoint(x3bb,y1bb);
				addLinkPoint(x3bb,y1,false);
				addLinkPoint(x3,y1,false);
				quad();
			}
			break;
		case DependencyType.SS:
		case DependencyType.FF:{
			double x5,x5b;
			if (type==DependencyType.SS){
				x5 = (x2 < x3) ? x2 : x3;
				x5b = (x2b < x3b) ? x2b : x3b;
			}else{
				x5 = (x2 > x3) ? x2 : x3;
				x5b = (x2b > x3b) ? x2b : x3b;
			}
			double x5bb=(x5b+x5)/2;
			addLinkPoint(x5, y0);
			addLinkPoint(x5bb, y0,false);
			addLinkPoint(x5bb, y0b,false);
			quad();
			if (y0b!=y1b) addLinkPoint(x5bb, y1b);
			addLinkPoint(x5bb, y1,false);
			addLinkPoint(x5, y1,false);
			quad();
			break;
		}
		default:
			return;
		}
		addLinkPoint(x1,y1);
	}
}


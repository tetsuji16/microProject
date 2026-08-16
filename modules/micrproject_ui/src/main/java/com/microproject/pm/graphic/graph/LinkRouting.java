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
package com.microproject.pm.graphic.graph;

import java.awt.geom.GeneralPath;

/**
 *
 */
public abstract class LinkRouting {

	protected float lx0,lx1,lx2,ly0,ly1,ly2,fx0,fx1,fy0,fy1;
	protected int nbPoints;
	protected GeneralPath path;
	protected void addLinkPoint(double x,double y){
		addLinkPoint(x,y,true);
	}
	protected void addLinkPoint(double x,double y,boolean line){
		lx2=lx1;
		ly2=ly1;
		lx1=lx0;
		ly1=ly0;
		lx0=(int)Math.round(x);
		ly0=(int)Math.round(y);
		if (nbPoints==0){
			fx0=lx0;
			fy0=ly0;
			path.moveTo(lx0,ly0);
		}else{
			if (nbPoints==1){
				fx1=lx0;
				fy1=ly0;
			}
			if (line)path.lineTo(lx0,ly0);
		}
		nbPoints++;
	}
	protected void resetLinkPoints(){
		nbPoints=0;
		lx0=-1;
		lx1=-1;
		ly0=-1;
		ly1=-1;
		fx0=-1;
		fx1=-1;
		fy0=-1;
		fy1=-1;
		path.reset();
	}
	protected void line(){
		path.lineTo(lx0,ly0);
	}
	protected void quad(){
		path.quadTo(lx1,ly1,lx0,ly0);
	}
	protected void curve(){
		path.curveTo(lx2,ly2,lx1,ly1,lx0,ly0);
	}
	
	//public abstract void routePath(GeneralPath path,double x0,double y0,double x1,double y1,double[] extraPoints, int type);
	
	
	
	public float getFirstX() {
		return fx0;
	}
	public float getFirstY() {
		return fy0;
	}
	public float getLastX() {
		return lx0;
	}
	public float getLastY() {
		return ly0;
	}
	
	public double getFirstAngle() {
		return Math.atan2(fy1-fy0,fx0-fx1);
	}
	public double getLastAngle() {
		return Math.atan2(ly1-ly0,lx0-lx1);
	}
}


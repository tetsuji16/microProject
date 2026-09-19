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
package com.microproject.print;

import java.awt.geom.AffineTransform;

import com.microproject.offline_graphics.SVGRenderer;

public class ViewPrintableParamsImpl implements ViewPrintableParams{
	protected AffineTransform transform;
	protected SVGRenderer renderer;
	protected int documentRowCount,documentColCount;
	protected double totalZoomX,totalZoomY;
	public ViewPrintableParamsImpl(AffineTransform transform,
			SVGRenderer renderer, int documentRowCount, int documentColCount,
			double totalZoomX, double totalZoomY) {
		super();
		this.transform = transform;
		this.renderer = renderer;
		this.documentRowCount = documentRowCount;
		this.documentColCount = documentColCount;
		this.totalZoomX = totalZoomX;
		this.totalZoomY = totalZoomY;
	}
	public AffineTransform getTransform() {
		return transform;
	}
	public void setTransform(AffineTransform transform) {
		this.transform = transform;
	}
	public SVGRenderer getRenderer() {
		return renderer;
	}
	public void setRenderer(SVGRenderer renderer) {
		this.renderer = renderer;
	}
	public int getDocumentRowCount() {
		return documentRowCount;
	}
	public void setDocumentRowCount(int documentRowCount) {
		this.documentRowCount = documentRowCount;
	}
	public int getDocumentColCount() {
		return documentColCount;
	}
	public void setDocumentColCount(int documentColCount) {
		this.documentColCount = documentColCount;
	}
	public double getTotalZoomX() {
		return totalZoomX;
	}
	public void setTotalZoomX(double totalZoomX) {
		this.totalZoomX = totalZoomX;
	}
	public double getTotalZoomY() {
		return totalZoomY;
	}
	public void setTotalZoomY(double totalZoomY) {
		this.totalZoomY = totalZoomY;
	}

}


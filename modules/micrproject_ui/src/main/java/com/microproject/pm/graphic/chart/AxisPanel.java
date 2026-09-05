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
package com.microproject.pm.graphic.chart;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JPanel;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.ui.RectangleEdge;

import com.microproject.util.FlatUiSupport;

/**
 * An axis panel that shows a right justified JFreeChart axis associated with a chart
 */
public class AxisPanel extends JPanel {
	private static final long serialVersionUID = -43845080081033994L;

	private ValueAxis axis;
	
	private ChartInfo chartInfo;
	AxisPanel(ChartInfo chartInfo) {
		this.chartInfo = chartInfo;
		FlatUiSupport.applyTableHeaderStyle(this);
	}
	/**
	 * @param axis The axis to set.
	 */
	void setAxis(ValueAxis axis) {
		this.axis = axis;
	}
	
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics); // have to do this first for background
		if (axis == null)		
			return; // no axis, don't paint it
		
		Rectangle plotArea = new Rectangle(getSize()); // use full panel size
		RectangleEdge edge = RectangleEdge.LEFT; // plot is on the right
		
		axis.setVisible(true); // this enables this axis to draw.  Later on the axis is made invisible so the chart axis won't draw
		FlatUiSupport.enableAntialiasing((Graphics2D)graphics);

		Dimension d = getSize();
		int cursor = d.width; // set cursor to full width
		int footerOffset = (int) Math.round(chartInfo.getFooterHeight());
		int headerOffset = 0;//(int) Math.round(chartInfo.getHeaderHeight());
		Rectangle dataArea = new Rectangle(cursor,headerOffset,d.width,d.height - headerOffset - footerOffset); // draw right justified
	
		axis.draw((Graphics2D)graphics,
				cursor,
				plotArea,
				dataArea,
				edge,
				null);

		axis.setVisible(false);	//now make it invisible so chart doesn't show it
	}
}

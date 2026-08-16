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
package com.microproject.pm.graphic.gantt;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;

import com.microproject.pm.graphic.graph.GraphParams;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;

public interface GanttParams extends GraphParams{
	public int getRowHeight();
	public void setRowHeight(int rowHeight);
	public CoordinatesConverter getCoord();
	public void setCoord(CoordinatesConverter coord);
	public Font getColumnHeaderFont();
	public void setColumnHeaderFont(Font columnHeaderFont);
	public Rectangle getGanttBounds();
	public boolean isGridLinesVisible();
	public void setGridLinesVisible(boolean visible);
	public Color getGridLineColor();
	public void setGridLineColor(Color color);
}


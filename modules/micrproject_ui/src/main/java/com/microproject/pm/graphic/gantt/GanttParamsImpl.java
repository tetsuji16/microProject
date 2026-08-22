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
import java.io.Serializable;

import com.microproject.pm.graphic.link_routing.DefaultGanttLinkRouting;
import com.microproject.pm.graphic.graph.GraphParams;
import com.microproject.pm.graphic.graph.LinkRouting;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.configuration.Dictionary;
import com.microproject.graphic.configuration.BarStyles;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.util.FlatUiSupport;

public class GanttParamsImpl implements GanttParams, Serializable,Cloneable {
	private static final long serialVersionUID = 2314555242629487089L;
	private static final String DEFAULT_BAR_STYLES = "standard";
	protected NodeModelCache cache;
	protected BarStyles barStyles;
	protected GraphicConfiguration configuration;

	protected Font columnHeaderFont;
	protected LinkRouting routing=new DefaultGanttLinkRouting();
	protected CoordinatesConverter coord;
	protected Rectangle printBounds;
	protected boolean rightPartVisible=true,leftPartVisible=true;
	protected int rowHeight;
	protected boolean gridLinesVisible = true;
	protected Color gridLineColor = FlatUiSupport.tableGridColor();

	public GanttParamsImpl(){
		configuration = GraphicConfiguration.getInstance();
		barStyles = (BarStyles) Dictionary.get(BarStyles.category, DEFAULT_BAR_STYLES);
		columnHeaderFont = FlatUiSupport.ganttHeaderFont();
		routing = new DefaultGanttLinkRouting();
		rowHeight = configuration.getRowHeight();
	}

	public GraphicConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(GraphicConfiguration configuration) {
		this.configuration = configuration;
	}

	public BarStyles getBarStyles() {
		return barStyles;
	}
	public void setBarStyles(BarStyles barStyles) {
		this.barStyles = barStyles;
	}
	public NodeModelCache getCache() {
		return cache;
	}
	public void setCache(NodeModelCache cache) {
		this.cache = cache;
	}
	public CoordinatesConverter getCoord() {
		return coord;
	}
	public void setCoord(CoordinatesConverter coord) {
		this.coord = coord;
	}
	public LinkRouting getRouting() {
		return routing;
	}
	public void setRouting(LinkRouting routing) {
		this.routing = routing;
	}
	public int getRowHeight() {
		return rowHeight;
	}
	public void setRowHeight(int rowHeight) {
		this.rowHeight=rowHeight;
	}
	public Rectangle getGanttBounds() {
		return new Rectangle(0, configuration.getColumnHeaderHeight(), (int) Math.ceil(coord.getWidth()), getRowHeight() * cache.getSize());
//		return new Rectangle(0,configuration.getColumnHeaderHeight(),(int)Math.ceil(coord.getWidth()),configuration.getRowHeight()*cache.getSize());
	}
	public Rectangle getDrawingBounds() {
		return getGanttBounds();
	}

	public Font getColumnHeaderFont() {
		return columnHeaderFont;
	}
	public void setColumnHeaderFont(Font columnHeaderFont) {
		this.columnHeaderFont = columnHeaderFont;
	}
	public boolean useTextures() {
		return false;
	}

	public Rectangle getPrintBounds() {
		return printBounds;
	}

	public void setPrintBounds(Rectangle printBounds) {
		this.printBounds = printBounds;
		updateDrawingBounds();
	}
	public void updateDrawingBounds(){}

	public int getPrintCols(){
		return (int) Math.ceil(getGanttBounds().getWidth() / getPrintBounds().getWidth());
	}
	public int getPrintRows(){
		return (int) Math.ceil(getGanttBounds().getHeight() / getPrintBounds().getHeight());
	}

	public boolean isLeftPartVisible() {
		return leftPartVisible;
	}

	public void setLeftPartVisible(boolean leftPartVisible) {
		this.leftPartVisible = leftPartVisible;
	}

	public boolean isRightPartVisible() {
		return rightPartVisible;
	}

	public void setRightPartVisible(boolean rightPartVisible) {
		this.rightPartVisible = rightPartVisible;
	}
	protected boolean supportLeftAndRightParts=false;
	public boolean isSupportLeftAndRightParts(){
		return supportLeftAndRightParts;
	}
	public void setSupportLeftAndRightParts(boolean supports){
		this.supportLeftAndRightParts = supports;
	}

	public boolean isGridLinesVisible() {
		return gridLinesVisible;
	}

	public void setGridLinesVisible(boolean visible) {
		this.gridLinesVisible = visible;
	}

	public Color getGridLineColor() {
		return gridLineColor;
	}

	public void setGridLineColor(Color color) {
		this.gridLineColor = color == null ? FlatUiSupport.tableGridColor() : color;
	}

	public Object clone(){
		try {
			return super.clone();
		} catch (CloneNotSupportedException ignored) {
			throw new InternalError();
		}
	}
	public GraphParams createSafePrintCopy(){
		var copy = (GanttParamsImpl) clone();
		if (copy.printBounds != null) {
			copy.printBounds = (Rectangle) copy.printBounds.clone();
		}
		return copy;
	}


}


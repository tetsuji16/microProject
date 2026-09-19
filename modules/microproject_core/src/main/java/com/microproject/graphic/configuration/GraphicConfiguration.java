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
package com.microproject.graphic.configuration;

import org.apache.commons.digester.Digester;

import com.microproject.configuration.Configuration;

/**
 *
 */
public class GraphicConfiguration {
	protected int columnHeaderHeight;
	protected int printFooterHeight;
	protected int rowHeaderWidth;
	protected int rowChartHeaderWidth;
	protected int rowHeight;
	protected int ganttBarHeight;
	protected int ganttBarYOffset;
	protected int ganttBarAnnotationXOffset;
	protected int ganttBarAnnotationYOffset;
	protected int ganttProgressBarHeight;
	protected int ganttBarMinWidth;
	protected int baselineHeight;
	protected int pertCellWidth;
	protected int pertCellHeight;
	protected int pertXOffset;
	protected int pertYOffset;
	protected int treeCellWidth;
	protected int treeCellHeight;
	protected int treeXOffset;
	protected int treeYOffset;
	protected int collapseLevel;
	protected double selectionSquare;
	protected double networkCellSelectionSquare;
	protected double selectionProgress0;
	protected double selectionProgress1;
	protected double selectionResize0;
	protected double selectionResize1;
	protected double linkFlatness=0;
	
	public static GraphicConfiguration getInstance(){
		return Configuration.getInstance().getGraphicConfiguation();
	}
	
	public static void addDigesterEvents(Digester digester){
		digester.addObjectCreate("*/graphic", "com.microproject.graphic.configuration.GraphicConfiguration");
	    digester.addSetProperties("*/graphic");
		digester.addSetNext("*/graphic", "setGraphicConfiguation", "com.microproject.graphic.configuration.GraphicConfiguration");

	}
	
	
	/**
	 * @return Returns the columnHeaderHeight.
	 */
	public int getColumnHeaderHeight() {
		return columnHeaderHeight;
	}
	/**
	 * @param columnHeaderHeight The columnHeaderHeight to set.
	 */
	public void setColumnHeaderHeight(int columnHeaderHeight) {
		this.columnHeaderHeight = columnHeaderHeight;
	}
    public int getPrintFooterHeight() {
		return printFooterHeight;
	}

	public void setPrintFooterHeight(int printFooterHeight) {
		this.printFooterHeight = printFooterHeight;
	}

	public int getRowHeaderWidth() {
        return rowHeaderWidth;
    }
    public void setRowHeaderWidth(int rowHeaderWidth) {
        this.rowHeaderWidth = rowHeaderWidth;
    }
	/**
	 * @return Returns the rowChartHeaderWidth.
	 */
	public int getRowChartHeaderWidth() {
		return rowChartHeaderWidth;
	}
	/**
	 * @param rowChartHeaderWidth The rowChartHeaderWidth to set.
	 */
	public void setRowChartHeaderWidth(int rowChartHeaderWidth) {
		this.rowChartHeaderWidth = rowChartHeaderWidth;
	}
	/**
	 * @return Returns the rowHeight.
	 */
	public int getRowHeight() {
		return rowHeight;
	}
	/**
	 * @param rowHeight The rowHeight to set.
	 */
	public void setRowHeight(int rowHeight) {
		this.rowHeight = rowHeight;
	}
	/**
	 * @return Returns the ganttBarHeight.
	 */
	public int getGanttBarHeight() {
		return ganttBarHeight;
	}
	/**
	 * @param ganttBarHeight The ganttBarHeight to set.
	 */
	public void setGanttBarHeight(int ganttBarHeight) {
		this.ganttBarHeight = ganttBarHeight;
	}
	/**
	 * @return Returns the ganttProgressBarHeight.
	 */
	public int getGanttProgressBarHeight() {
		return ganttProgressBarHeight;
	}
	/**
	 * @param ganttProgressBarHeight The ganttProgressBarHeight to set.
	 */
	public void setGanttProgressBarHeight(int ganttProgressBarHeight) {
		this.ganttProgressBarHeight = ganttProgressBarHeight;
	}
    /**
     * @return Returns the ganttBarYOffset.
     */
    public int getGanttBarYOffset() {
        return ganttBarYOffset;
    }
    /**
     * @param ganttBarYOffset The ganttBarYOffset to set.
     */
    public void setGanttBarYOffset(int ganttBarYOffset) {
        this.ganttBarYOffset = ganttBarYOffset;
    }
    
	public int getBaselineHeight() {
		return baselineHeight;
	}
	public void setBaselineHeight(int baselineHeight) {
		this.baselineHeight = baselineHeight;
	}
	
	
	
	public int getPertCellHeight() {
		return pertCellHeight;
	}
	public void setPertCellHeight(int pertCellHeight) {
		this.pertCellHeight = pertCellHeight;
	}
	public int getPertCellWidth() {
		return pertCellWidth;
	}
	public void setPertCellWidth(int pertCellWidth) {
		this.pertCellWidth = pertCellWidth;
	}
	public int getPertXOffset() {
		return pertXOffset;
	}
	public void setPertXOffset(int pertXOffset) {
		this.pertXOffset = pertXOffset;
	}
	public int getPertYOffset() {
		return pertYOffset;
	}
	public void setPertYOffset(int pertYOffset) {
		this.pertYOffset = pertYOffset;
	}
	
	
	
	
	public int getTreeCellHeight() {
		return treeCellHeight;
	}
	public void setTreeCellHeight(int treeCellHeight) {
		this.treeCellHeight = treeCellHeight;
	}
	public int getTreeCellWidth() {
		return treeCellWidth;
	}
	public void setTreeCellWidth(int treeCellWidth) {
		this.treeCellWidth = treeCellWidth;
	}
	public int getTreeXOffset() {
		return treeXOffset;
	}
	public void setTreeXOffset(int treeXOffset) {
		this.treeXOffset = treeXOffset;
	}
	public int getTreeYOffset() {
		return treeYOffset;
	}
	public void setTreeYOffset(int treeYOffset) {
		this.treeYOffset = treeYOffset;
	}
	
	
	public int getCollapseLevel() {
		return collapseLevel;
	}
	public void setCollapseLevel(int collapseLevel) {
		this.collapseLevel = collapseLevel;
	}
	
	
	public double getSelectionSquare() {
		return selectionSquare;
	}
	public void setSelectionSquare(double selectionSquare) {
		this.selectionSquare = selectionSquare;
	}
	
	
	public double getLinkFlatness() {
		return linkFlatness;
	}
	public void setLinkFlatness(double linkFlatness) {
		this.linkFlatness = linkFlatness;
	}
	
	
	public double getSelectionProgress0() {
		return selectionProgress0;
	}
	public void setSelectionProgress0(double selectionProgress0) {
		this.selectionProgress0 = selectionProgress0;
	}
	public double getSelectionProgress1() {
		return selectionProgress1;
	}
	public void setSelectionProgress1(double selectionProgress1) {
		this.selectionProgress1 = selectionProgress1;
	}
	public double getSelectionResize0() {
		return selectionResize0;
	}
	public void setSelectionResize0(double selectionResize0) {
		this.selectionResize0 = selectionResize0;
	}
	public double getSelectionResize1() {
		return selectionResize1;
	}
	public void setSelectionResize1(double selectionResize1) {
		this.selectionResize1 = selectionResize1;
	}
	
	
	public double getNetworkCellSelectionSquare() {
		return networkCellSelectionSquare;
	}
	public void setNetworkCellSelectionSquare(double networkCellSelectionSquare) {
		this.networkCellSelectionSquare = networkCellSelectionSquare;
	}

	public int getGanttBarAnnotationXOffset() {
		return ganttBarAnnotationXOffset;
	}

	public void setGanttBarAnnotationXOffset(int ganttBarAnnotationXOffset) {
		this.ganttBarAnnotationXOffset = ganttBarAnnotationXOffset;
	}

	public int getGanttBarAnnotationYOffset() {
		return ganttBarAnnotationYOffset;
	}

	public void setGanttBarAnnotationYOffset(int ganttBarAnnotationYOffset) {
		this.ganttBarAnnotationYOffset = ganttBarAnnotationYOffset;
	}

	public int getGanttBarMinWidth() {
		return ganttBarMinWidth;
	}

	public void setGanttBarMinWidth(int ganttBarMinWidth) {
		this.ganttBarMinWidth = ganttBarMinWidth;
	}

	
}

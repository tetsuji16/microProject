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
package com.microproject.offline_graphics;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.microproject.pm.graphic.gantt.GanttRenderer;
import com.microproject.pm.graphic.graph.GraphParams;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.model.cache.ReferenceNodeModelCache;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetParamsImpl;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetRenderer;
import com.microproject.pm.graphic.spreadsheet.renderer.FontManager;
import com.microproject.pm.graphic.timescale.CoordinatesConverter;
import com.microproject.pm.graphic.timescale.TimeScaleComponent;
import com.microproject.print.FooterRenderer;
import com.microproject.print.PrintSettings;
import com.microproject.configuration.Settings;
import com.microproject.graphic.configuration.GraphicConfiguration;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.pm.snapshot.Snapshottable;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.pm.task.TaskSnapshot;
import com.microproject.workspace.SavableToWorkspace;

/*
 *
 */
public class GanttSVGRenderer implements SVGRenderer,Cloneable{
	protected SpreadSheetParamsImpl params;
	protected CoordinatesConverter coord;
	protected GanttRenderer ganttRenderer;
	protected SpreadSheetRenderer spreadSheetRenderer;
	protected FooterRenderer footerRenderer;
	protected Project project;
	public void init(Project project, ReferenceNodeModelCache refCache) {
		SpreadSheetFieldArray fieldArray=null;
		PrintSettings printSettings=project.getPrintSettings(SavableToWorkspace.PERSIST);
		if (printSettings!=null) fieldArray=printSettings.getFieldArray();
		init(project,NodeModelCacheFactory.getInstance().createFilteredCache((ReferenceNodeModelCache)refCache,"OfflineGantt",null),fieldArray,null,-1,true);
	}
	public void init(Project project, NodeModelCache cache,SpreadSheetFieldArray fieldArray,List<Integer> colWidth,int scale,boolean printGantt) {
		this.project=project;
		coord = new CoordinatesConverter(project);
		if (scale!=-1) coord.getTimescaleManager().setCurrentScaleIndex(scale);
		params=new SpreadSheetParamsImpl(fieldArray,colWidth,printGantt);
		int rowHeight=project.getRowHeight(new TreeSet<Integer>());
		params.setRowHeight(rowHeight);

		params.setCache(cache);
		params.setCoord(coord);
		ganttRenderer=new GanttRenderer(params);
		params.setGridLineColor(ganttRenderer.getPalette().getGridLine());
		spreadSheetRenderer=new SpreadSheetRenderer(params);
		footerRenderer=new FooterRenderer(params);
		cache.update();
	}



	public Object clone(){
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}
	public SVGRenderer createSafePrintCopy(){
		GanttSVGRenderer c=(GanttSVGRenderer)clone();
		c.params=(SpreadSheetParamsImpl)c.params.createSafePrintCopy();
		c.ganttRenderer=new GanttRenderer(c.params);
		c.params.setGridLineColor(c.ganttRenderer.getPalette().getGridLine());
		c.spreadSheetRenderer=new SpreadSheetRenderer(c.params);
		c.footerRenderer=new FooterRenderer(c.params);

		return c;
	}


	public void paint(Graphics2D g){
		paint(g,-1,-1);
	}
	public void paint(Graphics2D g,int prow,int pcol){
		Rectangle drawingBounds=params.getDrawingBounds();
		int ganttX=params.getSpreadSheetBounds().width;
		int ganttY=0;
		Rectangle spreadsheetPrintBounds=null;
		Rectangle ganttPrintBounds=null;
		boolean drawSpreadsheet=true;
		boolean drawGantt=true;
		int rowH=params.getConfiguration().getColumnHeaderHeight();




		if (prow==-1){
		}else{
			spreadsheetPrintBounds=params.getSpreadsheetPrintBounds(prow,pcol,false);
			if (spreadsheetPrintBounds==null||!params.isLeftPartVisible()) drawSpreadsheet=false;
			ganttPrintBounds=params.getGanttPrintBounds(prow,pcol);
			if (ganttPrintBounds==null) drawGantt=false;
			else{
				ganttX=params.getGanttDeltaX(prow, pcol);
				ganttY=-ganttPrintBounds.y;

			}
		}


		if (drawSpreadsheet){
			spreadSheetRenderer.paint(g,prow,pcol);
		}
		if (drawGantt){
			g.translate(ganttX,0);
			if (ganttPrintBounds!=null) g.setClip(new Rectangle(ganttPrintBounds.x,0,ganttPrintBounds.width,rowH));
			TimeScaleComponent.paintTimeScale(g,params,FontManager.getOfflineDefaultFont());
			if (ganttPrintBounds!=null) g.setClip(null);
			g.translate(0, ganttY+params.getConfiguration().getColumnHeaderHeight());
			if (ganttPrintBounds==null) ganttRenderer.paint(g,null);
			else ganttRenderer.paint(g,new Rectangle(ganttPrintBounds.x,ganttPrintBounds.y+1,ganttPrintBounds.width,ganttPrintBounds.height-1));//1 pixel offset needed for edge
			g.translate(-ganttX, -ganttY-params.getConfiguration().getColumnHeaderHeight());
		}

		g.setColor(Color.BLACK);
		if (prow==-1){
				g.drawRect(0, 0, drawingBounds.width, drawingBounds.height);
				g.drawLine(drawingBounds.x, drawingBounds.y, drawingBounds.x+drawingBounds.width, drawingBounds.y);
		}else{
			int footerH=params.getConfiguration().getPrintFooterHeight();
			Rectangle printBounds=params.getPrintBounds();
			int footerY=params.getPrintBounds().height-footerH;
			int nbCols=params.getPrintCols();
			if (drawSpreadsheet&&!drawGantt){
				g.drawRect(0, 0, spreadsheetPrintBounds.width, /*spreadsheetPrintBounds.height+rowH*/printBounds.height);
				g.drawLine(0, rowH, spreadsheetPrintBounds.width, rowH);
				g.drawLine(0, spreadsheetPrintBounds.height+rowH, spreadsheetPrintBounds.width, spreadsheetPrintBounds.height+rowH);
				g.drawLine(0, footerY, spreadsheetPrintBounds.width, footerY);
				footerRenderer.paint(g, prow*nbCols+pcol, new Rectangle(0,footerY,spreadsheetPrintBounds.width,footerH),project.getName());
			}else if (!drawSpreadsheet&&drawGantt){
				g.drawRect(0, 0, ganttPrintBounds.width, printBounds.height);
				g.drawLine(0, rowH, ganttPrintBounds.width, rowH);
				g.drawLine(0, ganttPrintBounds.height+rowH, ganttPrintBounds.width, ganttPrintBounds.height+rowH);
				g.drawLine(0, footerY, ganttPrintBounds.width, footerY);
				footerRenderer.paint(g, prow*nbCols+pcol, new Rectangle(0,footerY,ganttPrintBounds.width,footerH),project.getName());
			}else if (drawSpreadsheet&&drawGantt){
				g.drawRect(0, 0, spreadsheetPrintBounds.width+ganttPrintBounds.width, printBounds.height);
				g.drawLine(0, rowH, spreadsheetPrintBounds.width+ganttPrintBounds.width, rowH);
				g.drawLine(0, spreadsheetPrintBounds.height+rowH, spreadsheetPrintBounds.width+ganttPrintBounds.width, spreadsheetPrintBounds.height+rowH);
				g.drawLine(0, footerY, spreadsheetPrintBounds.width+ganttPrintBounds.width, footerY);
				footerRenderer.paint(g, prow*nbCols+pcol, new Rectangle(0,footerY,spreadsheetPrintBounds.width+ganttPrintBounds.width,footerH),project.getName());
			}


		}




	 }



	public Dimension getCanvasSize(){
		return params.getDrawingBounds().getSize();
	}

	public GraphParams getParams() {
		return params;
	}
	public Project getProject() {
		return project;
	}

}


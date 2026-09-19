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
package com.microproject.pm.graphic.spreadsheet;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.table.TableColumn;

import com.microproject.pm.graphic.Renderer;
import com.microproject.pm.graphic.graph.GraphParams;
import com.microproject.pm.graphic.model.cache.GraphicNode;
import com.microproject.pm.graphic.spreadsheet.renderer.OfflineRenderer;
import com.microproject.field.Field;
import com.microproject.util.FlatUiSupport;

public class SpreadSheetRenderer extends Renderer{
//	protected Stroke cellStroke=new BasicStroke(0.25f);
//	protected Stroke spreadSheetStroke=new BasicStroke(0.5f);
//	protected Color cellColor=Color.GRAY;
//	protected Color spreadSheetColor=Color.BLACK;

	protected SpreadSheetParams params;
	public SpreadSheetRenderer(GraphParams graphInfo){
		super(graphInfo);
		params=(SpreadSheetParams)graphInfo;
	}
	public SpreadSheetRenderer(){
		super();
	}
	public void paint(Graphics g){
		paint(g,-1,-1);
	}
	public void paint(Graphics g,int prow, int pcol) {
		Graphics2D g2=(Graphics2D)g;
		int h=params.getSpreadSheetBounds().y;
		int rowH=params.getRowHeight();
		int row0=0;
		int row1=Integer.MAX_VALUE;
		int col0=0;
		int col1=Integer.MAX_VALUE;
		Rectangle spreadsheetBounds=params.getSpreadSheetBounds();
		if (prow!=-1){
			SpreadSheetParamsImpl.PageInfo colInfo=params.getColPageInfo(pcol);
			SpreadSheetParamsImpl.PageInfo rowInfo=params.getRowPageInfo(prow);
			col0=colInfo.getStart();
			col1=colInfo.getEnd();
			row0=rowInfo.getStart();
			row1=rowInfo.getEnd();
			Rectangle printSpreadsheetBounds=((SpreadSheetParamsImpl)params).getSpreadsheetPrintBounds(prow, pcol,false);
			spreadsheetBounds=new Rectangle(spreadsheetBounds.x,spreadsheetBounds.y,printSpreadsheetBounds.width,printSpreadsheetBounds.height);
		}
//		System.out.println("spreadsheetBounds="+spreadsheetBounds);
		int row=row0;
		for (Iterator i=graphInfo.getCache().getIterator(row0);i.hasNext()&&row<=row1;row++){
			GraphicNode gnode=(GraphicNode)i.next();
			paintRow(g2, row, row0, h,col0,col1,gnode,spreadsheetBounds);
			h+=rowH;
		}
		paintColumnHeader(g2,col0,col1,spreadsheetBounds);
	}

	private int idColMargin=2,colMargin=2,idColumnIndex=0;
	protected int getColMargin(int colIndex){
		return (idColumnIndex==colIndex)?idColMargin:colMargin;
	}

	protected void paintColumnHeader(Graphics2D g2,int col0,int col1,Rectangle spreadsheetBounds){
		TableColumn c;
		int w=spreadsheetBounds.x;
		int h=spreadsheetBounds.y-params.getConfiguration().getColumnHeaderHeight();
		fillBackground(g2, spreadsheetBounds.x, h, spreadsheetBounds.width, params.getConfiguration().getColumnHeaderHeight(), FlatUiSupport.headerBackground());
		int col=0;
		for (Iterator i=params.getColumnIterator();i.hasNext()&&col<=col1;col++){
			c=(TableColumn)i.next();
			if (col<col0) continue;

	    	int cwidth=c.getPreferredWidth()+2*getColMargin(col);

			OfflineRenderer renderer=(OfflineRenderer)c.getHeaderRenderer();
			if (renderer!=null){ //rowHeader is null
				JComponent component=(JComponent)renderer.getComponent(((Field)params.getFieldArray().get(col)).getName(), null, (Field)params.getFieldArray().get(col), params);
		    	boolean opaque=component.isOpaque();
		    	//component.setDoubleBuffered(false);
		    	component.setOpaque(false);
		    	//component.setForeground(Color.BLACK);
				component.setSize(cwidth, params.getConfiguration().getColumnHeaderHeight());
		    	g2.translate(w,h);
		    	component.doLayout();
		    	//g2.setClip(0, 0, cwidth, params.getConfiguration().getColumnHeaderHeight());
		    	component.print(g2);
		    	//g2.setClip(null);
		    	g2.translate(-w,-h);
				component.setOpaque(opaque);
			}
			w+=cwidth;
//			g2.setStroke(spreadSheetStroke);
//			g2.setColor(spreadSheetColor);
			paintGridLine(g2, new Line2D.Double(w,h,w,spreadsheetBounds.getMaxY()));
			//g2.drawLine(w,h,w,spreadsheetBounds.y+spreadsheetBounds.height);
		}
	}

	protected void paintRow(Graphics2D g2, int row, int row0, int h,int col0,int col1,GraphicNode node,Rectangle spreadsheetBounds){
		TableColumn c;
		int w=spreadsheetBounds.x;
		fillBackground(g2, spreadsheetBounds.x, h, spreadsheetBounds.width, params.getRowHeight(), FlatUiSupport.dataSurfaceBackground());
		int col=0;
		for (Iterator i=params.getColumnIterator();i.hasNext()&&col<=col1;col++){
			c=(TableColumn)i.next();
			if (col<col0) continue;
			//cell content
			//GraphicNode node = SpreadSheetUtils.getNodeFromCacheRow(row,1/*rowMultiple*/,params.getCache());
			Object value=SpreadSheetUtils.getValueAt(node.getNode(), col, params.getCache(), params.getColumnModel(), params.getFieldContext());

	    	Field field=(Field)params.getFieldArray().get(col);

			int compWidth=c.getPreferredWidth();
	    	int cwidth=compWidth+2*getColMargin(col);

			OfflineRenderer renderer=(OfflineRenderer)c.getCellRenderer();
			JComponent component=(JComponent)renderer.getComponent(value, node, field, params);
	    	//component.setDoubleBuffered(false);
	    	boolean opaque=component.isOpaque();
			component.setOpaque(false);
	    	//component.setForeground(Color.BLACK);
			component.setSize(compWidth, params.getRowHeight());
	    	g2.translate(w+getColMargin(col),h);
	    	//g2.setClip(0, 0, compWidth, params.getRowHeight());
	    	component.doLayout();
	    	component.print(g2);
	    	//g2.setClip(null);
	    	g2.translate(-w-getColMargin(col),-h);
			component.setOpaque(opaque);
			w+=cwidth;
		}
		if (row!=row0) paintGridLine(g2, new Line2D.Double(0,h,w,h));
	}

	private void fillBackground(Graphics2D g2, int x, int y, int width, int height, Color background) {
		Color oldColor = g2.getColor();
		g2.setColor(background);
		g2.fillRect(x, y, width, height);
		g2.setColor(oldColor);
	}

	private void paintGridLine(Graphics2D g2, Line2D line) {
		Color oldColor = g2.getColor();
		Color gridLineColor = params.getGridLineColor();
		g2.setColor(gridLineColor == null ? FlatUiSupport.tableGridColor() : gridLineColor);
		g2.draw(line);
		g2.setColor(oldColor);
	}




}


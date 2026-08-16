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
package org.projectlibre.print;

import java.util.Enumeration;

import javax.swing.table.TableColumn;

import com.microproject.pm.graphic.graph.GraphParams;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetParams;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetParamsImpl;
import com.microproject.print.ExtendedPrintService;
import com.microproject.graphic.configuration.GraphicConfiguration;

public class ProjectLibrePrintServiceImpl implements ExtendedPrintService {

	public double getWRatio(int pageCount, double pageWidth,GraphParams params) {
		double newPageWidth;
		if (!(params instanceof SpreadSheetParamsImpl) || !params.isLeftPartVisible()){
			//no spreadsheet
			newPageWidth=params.getDrawingBounds().getWidth()/pageCount;
		}else{
			SpreadSheetParamsImpl spParams=(SpreadSheetParamsImpl)params;
			double totalWidth=getWidthWithPaging(-1.0,spParams);

			//use dichotomy to find the pageWidth matching the chosen pageCount
			double minPageWidth=totalWidth/pageCount;
			double maxPageWidth=totalWidth;
			newPageWidth=(minPageWidth+maxPageWidth)/2;
			double currentWidth;
			while (maxPageWidth-minPageWidth>0.5){
				currentWidth=getWidthWithPaging(newPageWidth,spParams);
				if (currentWidth<newPageWidth*pageCount) maxPageWidth=newPageWidth;
				else if (currentWidth>newPageWidth*pageCount) minPageWidth=newPageWidth;
				else {
					maxPageWidth=newPageWidth;
					break;
				}
				newPageWidth=(minPageWidth+maxPageWidth)/2;
			}
			newPageWidth=maxPageWidth;
		}
		double r=pageWidth/newPageWidth;
		//adding a margin, round issues
		double margin=1.0; //1 pixel
		if (r<1) r=pageWidth/(newPageWidth+margin/r);
		return r;
	}


	public double getHRatio(int pageCount,double pageHeight,GraphParams params) {
		if (params==null||!(params instanceof SpreadSheetParams)) return -1.0;
		SpreadSheetParams sp=(SpreadSheetParams)params;
		double newPageHeight=(Math.ceil(((double)params.getCache().getSize())/pageCount)*sp.getRowHeight()+GraphicConfiguration.getInstance().getColumnHeaderHeight()+GraphicConfiguration.getInstance().getPrintFooterHeight());
		return pageHeight/newPageHeight;
	}


	protected double getWidthWithPaging(double pageWidth,SpreadSheetParamsImpl spParams) {
		return getSpreadSheetWidthWithPaging(pageWidth,spParams)+getGanttWidthWithPaging(spParams);
	}

	protected double getSpreadSheetWidthWithPaging(double pageWidth,SpreadSheetParamsImpl spParams) {
		double spreadsheetWidth=0.0;		
		if (pageWidth<0) // return default page width
			spreadsheetWidth=spParams.getSpreadSheetWidth();
		else {
			//intitial spreadsheet Width
			double currentSpreadsheetWidth=GraphicConfiguration.getInstance().getRowHeaderWidth()+2*spParams.getIdColMargin();				
			for (Enumeration<TableColumn> e=spParams.getColumnModel().getColumns();e.hasMoreElements();){
				TableColumn col=e.nextElement();			
				int colWidth=col.getPreferredWidth()+2*spParams.getColMargin();
				//increment until it exceed page width
				if (currentSpreadsheetWidth+colWidth>pageWidth){
					spreadsheetWidth+=pageWidth; //next page to avoid cutting
					currentSpreadsheetWidth=0;
				}

				currentSpreadsheetWidth+=colWidth;
			}
			spreadsheetWidth+=currentSpreadsheetWidth;
		}
		return spreadsheetWidth;
	}

	protected double getGanttWidthWithPaging(SpreadSheetParamsImpl spParams) {
		if (spParams.isRightPartVisible())
			return spParams.getGanttBounds().getWidth();
		else return 0.0;
	}

}


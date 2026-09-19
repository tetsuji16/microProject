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

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.microproject.pm.graphic.gantt.GanttParamsImpl;
import com.microproject.pm.graphic.graph.GraphParams;
import com.microproject.pm.graphic.spreadsheet.common.SpreadSheetRowHeaderColumnModel;
import com.microproject.configuration.Dictionary;
import com.microproject.field.FieldContext;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.strings.Messages;

public class SpreadSheetParamsImpl extends GanttParamsImpl implements SpreadSheetParams {
	private static final Logger logger = Logger.getLogger(SpreadSheetParamsImpl.class.getName());
	protected String spreadsheetCategory;
	protected SpreadSheetFieldArray fieldArray;
	protected List<Integer> colWidth;
	protected FieldContext fieldContext;
	protected SpreadSheetColumnModel columnModel;
	protected SpreadSheetRowHeaderColumnModel headerColumnModel;
	protected int spreadSheetWidth=-1,idColMargin=2,colMargin=2;
	//protected boolean printGantt;
//	public SpreadSheetParamsImpl(){
//		super();
//		headerColumnModel=new SpreadSheetRowHeaderColumnModel();
//		spreadsheetCategory=SpreadSheetCategories.taskSpreadsheetCategory;
//		setFieldArray((SpreadSheetFieldArray) Dictionary.get(spreadsheetCategory,Messages.getString("Spreadsheet.Task.entry")));
//	}
	public SpreadSheetParamsImpl(SpreadSheetFieldArray fieldArray,List<Integer> colWidth,boolean printGantt){
		super();
		headerColumnModel=new SpreadSheetRowHeaderColumnModel();
		spreadsheetCategory=SpreadSheetCategories.taskSpreadsheetCategory;
		setFieldArray(fieldArray==null?((SpreadSheetFieldArray) Dictionary.get(spreadsheetCategory,Messages.getString("Spreadsheet.Task.entry"))):fieldArray,fieldArray==null?null:(colWidth==null?fieldArray.getWidths():colWidth));
		setRightPartVisible(printGantt);

	}
	public SpreadSheetFieldArray getFieldArray() {
		return fieldArray;
	}
	public void setFieldArray(SpreadSheetFieldArray fieldArray,List<Integer> colWidth) {
		this.fieldArray = fieldArray;
		this.colWidth=colWidth;
		columnModel=new SpreadSheetColumnModel(fieldArray,colWidth);
		columnModel.setSvg(true);
		initColumns(columnModel, fieldArray.size());
		initColumns(headerColumnModel, 1);


		fieldContext = new FieldContext();
		fieldContext.setLeftAssociation(true);

		updateWidth();
	}

	public String getSpreadsheetCategory() {
		return spreadsheetCategory;
	}
	public FieldContext getFieldContext() {
		return fieldContext;
	}

	public void initColumns(TableColumnModel cm,int columnCount) {
		while (cm.getColumnCount() > 0) {
			cm.removeColumn(cm.getColumn(0));
		}

//		int index=0;
//		for (Iterator i=fieldArray.iterator();i.hasNext();index++) {
//			Field field=(Field)i.next();
//			TableColumn c = new TableColumn(index);
//			c.setHeaderValue(field.getName());
//			cm.addColumn(c);
//		}
		for (int col=0;col<columnCount;col++) {
			TableColumn c = new TableColumn(col);
			c.setHeaderValue(""+col);
			cm.addColumn(c);
		}
	}



	public Rectangle getSpreadSheetBounds(){
		//return new Rectangle(0,configuration.getColumnHeaderHeight(),spreadSheetWidth,configuration.getRowHeight()*cache.getSize());
		return new Rectangle(0,configuration.getColumnHeaderHeight(),spreadSheetWidth,getRowHeight()*cache.getSize());
	}
	public Rectangle getDrawingBounds() {
		return new Rectangle(0,configuration.getColumnHeaderHeight(),(isLeftPartVisible()?(getSpreadSheetBounds().width):0)+(isRightPartVisible()?(getGanttBounds().width):0),getSpreadSheetBounds().height+configuration.getColumnHeaderHeight());
	}

	public int getSpreadSheetWidth() {
		//return isLeftPartVisible()?spreadSheetWidth:0;
		return spreadSheetWidth;
	}
	public SpreadSheetColumnModel getColumnModel() {
		return columnModel;
	}
	public SpreadSheetRowHeaderColumnModel getHeaderColumnModel() {
		return headerColumnModel;
	}
	public Iterator getColumnIterator(){
		return new Iterator(){
			protected Enumeration headerE=headerColumnModel.getColumns();
			protected Enumeration e=columnModel.getColumns();
			public boolean hasNext() {
				return headerE.hasMoreElements()||e.hasMoreElements();
			}
			public Object next() {
				if (headerE.hasMoreElements()) return headerE.nextElement();
				else return e.nextElement();
			}
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public void updateDrawingBounds(){
		updateWidth();
	}
	private void updateWidth(){
		spreadSheetWidth=calculateSpreadSheetWidth();
	}
	private int calculateSpreadSheetWidth(){
		int spWidth=getConfiguration().getRowHeaderWidth()+2*idColMargin;
		TableColumn c;
		for (Enumeration e=columnModel.getColumns();e.hasMoreElements();){
			c=(TableColumn)e.nextElement();
			int cwidth=c.getPreferredWidth()+2*colMargin;
			spWidth+=cwidth;
		}
		return spWidth;
	}
	public void setPrintBounds(Rectangle printBounds) {
		super.setPrintBounds(printBounds);
		updatePages();
	}
	private void updatePages(){
		if (printBounds==null) return;
		if (colPageInfo==null) colPageInfo=new ArrayList();
		else  colPageInfo.clear();
		if (rowPageInfo==null) rowPageInfo=new ArrayList();
		else  rowPageInfo.clear();

		int w=getConfiguration().getRowHeaderWidth()+2*idColMargin;
		TableColumn c;
		int start=0;
		int current=1;
		int x=0;
		for (Enumeration e=columnModel.getColumns();e.hasMoreElements();current++){
			c=(TableColumn)e.nextElement();
			if (w+c.getPreferredWidth()+2*colMargin>printBounds.width){
				colPageInfo.add(new PageInfo(start,current-1,x,w));
				start=current;
				x+=w;
				w=0;
			}
			w+=c.getPreferredWidth()+2*colMargin;
		}
		colPageInfo.add(new PageInfo(start,current-1,x,w));

		int rowsPerPage=getRowsPerPage();
		int count=cache.getSize();
		int pageCount=count/rowsPerPage;
		for (current=0;current<pageCount;current++)
			rowPageInfo.add(new PageInfo(current*rowsPerPage,(current+1)*rowsPerPage-1,current*rowsPerPage*getRowHeight(),rowsPerPage*getRowHeight()));
		int lastRows=count%rowsPerPage;
		if (lastRows!=0) rowPageInfo.add(new PageInfo(current*rowsPerPage,count-1,current*rowsPerPage*getRowHeight(),lastRows*getRowHeight()));
	}

	public class PageInfo{
		protected int start,end,x,width;

		public PageInfo(int start, int end,int x,int width) {
			super();
			this.start = start;
			this.end = end;
			this.x=x;
			this.width=width;
		}

		public int getEnd() {
			return end;
		}

		public void setEnd(int end) {
			this.end = end;
		}
		public int getStart() {
			return start;
		}

		public void setStart(int start) {
			this.start = start;
		}

		public int getWidth() {
			return width;
		}

		public void setWidth(int width) {
			this.width = width;
		}

		public int getX() {
			return x;
		}

		public void setX(int x) {
			this.x = x;
		}
	}

	protected ArrayList colPageInfo,rowPageInfo;
	public PageInfo getRowPageInfo(int row){
		return (row<0||row>=rowPageInfo.size())?null:(PageInfo)rowPageInfo.get(row);
	}
	public PageInfo getColPageInfo(int col){
		return (col<0||col>=colPageInfo.size())?null:(PageInfo)colPageInfo.get(col);
	}

	public int getRowsPerPage(){
//		int rowsPerPage=(printBounds.height-configuration.getColumnHeaderHeight()-configuration.getPrintFooterHeight())/configuration.getRowHeight();
		int rowsPerPage=(printBounds.height-configuration.getColumnHeaderHeight()-configuration.getPrintFooterHeight())/getRowHeight();
		if (rowsPerPage<=0){
			logger.log(Level.WARNING, "Invalid rows per page computed: {0}", rowsPerPage);
			rowsPerPage=1;
		}
		return rowsPerPage;
	}

	public Rectangle getSpreadsheetPrintBounds(int row,int col,boolean ifVisibleOnly){
		if (ifVisibleOnly&&!isLeftPartVisible()) return null;
		PageInfo colInfo=getColPageInfo(col);
		PageInfo rowInfo=getRowPageInfo(row);
		if (colInfo==null||rowInfo==null) return null;
		return new Rectangle(colInfo.getX(),rowInfo.getX()+configuration.getColumnHeaderHeight(),colInfo.getWidth(),rowInfo.getWidth());
	}

	public Rectangle getGanttPrintBounds(int row,int col){
		if (!isRightPartVisible()) return null;
		int spLastCol=colPageInfo.size()-1;
		if (col<spLastCol&&isLeftPartVisible()) return null;
		int totalWidth=getGanttBounds().width;
		Rectangle spreadsheetBounds=getSpreadsheetPrintBounds(row,spLastCol,true);
		if ((col==spLastCol||!isLeftPartVisible())&&spreadsheetBounds!=null){
			int x=spreadsheetBounds.width;
			int width=getPrintBounds().width-spreadsheetBounds.width;
			//if (x+width>totalWidth) width=totalWidth-x;
			if (width>totalWidth) width=totalWidth;
			Rectangle ganttBounds=new Rectangle(0,spreadsheetBounds.y-configuration.getColumnHeaderHeight(),width,spreadsheetBounds.height);
			if (ganttBounds.width==0) return null;
			else return ganttBounds;
		}else{
			spreadsheetBounds=getSpreadsheetPrintBounds(row,spLastCol,false);
			int x=-getGanttDeltaX(row, col);
			int width=getPrintBounds().width;
			if (x+width>totalWidth) width=totalWidth-x;
			Rectangle ganttBounds=new Rectangle(x,spreadsheetBounds==null?0:spreadsheetBounds.y-configuration.getColumnHeaderHeight(),width,spreadsheetBounds==null?0:spreadsheetBounds.height);
			return ganttBounds;
		}
	}
	public int getGanttDeltaX(int row,int col){
		int spLastCol=colPageInfo.size()-1;
		if (col<spLastCol&&isLeftPartVisible()) return -1;
		Rectangle spreadsheetBounds=getSpreadsheetPrintBounds(row,spLastCol,true);
		if (spreadsheetBounds==null)
			return -col*getPrintBounds().width;
		else return spreadsheetBounds.width-(col-spLastCol)*getPrintBounds().width;
	}


	public int getPrintCols(){
		int spColCount=colPageInfo.size();
		PageInfo colInfo=getColPageInfo(spColCount-1);
		int width=(isLeftPartVisible()?colInfo.width:0)+(isRightPartVisible()?getGanttBounds().width:0);
		return (isLeftPartVisible()?(spColCount-1):0)+(int)Math.ceil(width/getPrintBounds().getWidth());
	}
	public int getPrintRows(){
		int size=rowPageInfo.size();
		return size==0?1:size;
	}

	public int getColMargin() {
		return colMargin;
	}
	public int getIdColMargin() {
		return idColMargin;
	}

	public GraphParams createSafePrintCopy(){
		SpreadSheetParamsImpl c=(SpreadSheetParamsImpl)super.createSafePrintCopy();
		if (c.colPageInfo!=null) c.colPageInfo=(ArrayList) colPageInfo.clone();
		if (c.rowPageInfo!=null) c.rowPageInfo=(ArrayList) rowPageInfo.clone();
		return c;
	}


}


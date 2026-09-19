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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import com.microproject.job.JobRunnable;

public class DocumentPrintable implements Printable,Pageable{
	protected ViewPrintableParams printableParams;
	protected JobRunnable jr;
	protected PrinterJob printerJob;
	protected PageFormat pageFormat;
	public DocumentPrintable(){
	}
	public DocumentPrintable(ViewPrintableParams printableParams){
		this.printableParams=printableParams;
	}

	public int getNumberOfPages() {
		return printableParams.getDocumentRowCount()*printableParams.getDocumentColCount();
	}
	public int print (Graphics g, PageFormat pageFormat, int page) throws PrinterException {
		return print(g, page);
	}

	public JobRunnable getJr() {
		return jr;
	}
	public void setJr(JobRunnable jr) {
		this.jr = jr;
	}
	public PrinterJob getPrinterJob() {
		return printerJob;
	}
	public void setPrinterJob(PrinterJob printerJob) {
		this.printerJob = printerJob;
	}
	public PageFormat getPageFormat() {
		return pageFormat;
	}
	public void setPageFormat(PageFormat pageFormat) {
		this.pageFormat = pageFormat;
	}
	public PageFormat getPageFormat(int pageIndex){
		return pageFormat;
	}
	public Printable getPrintable(int pageIndex){
		return this;
	}

	public void update(){
		PrintDocument.updatePageLayout(printableParams,pageFormat);
	}

	public int print (Graphics g, int page) throws PrinterException {
		int pageCount=getNumberOfPages();
		//System.out.println(page+"/"+(pageCount-1));
		if (page<pageCount){
			if (jr!=null&&jr.getJob().isCanceled()) printerJob.cancel();
			Graphics2D g2 = (Graphics2D) g;
			AffineTransform svgTransform=g2.getTransform();
			Color svgColor=g2.getColor();
			Stroke svgStroke=g2.getStroke();

			g2.transform(printableParams.getTransform());
			//System.out.println("Print transform="+printableParams.getTransform()+" zx="+printableParams.getTotalZoomX()+", zy="+printableParams.getTotalZoomY());
			g2.setStroke(spreadSheetStroke);
			g2.setColor(spreadSheetColor);

			printMain(g2,page);

			g2.setColor(svgColor);
			g2.setStroke(svgStroke);
			g2.setTransform(svgTransform);
			if (jr!=null){
				//System.out.println("Progress: "+(page+1));
				if (jr.getJob().isCanceled()){
					printerJob.cancel();
					return NO_SUCH_PAGE;
				}
				int pCount=getNumberOfPages();
				if (pCount==0) pCount=1;
				jr.setProgress(((float)(page+1))/pCount);
			}

			return PAGE_EXISTS;
		} return NO_SUCH_PAGE;
	}

	protected void printMain(Graphics2D g2, int page) throws PrinterException {

	}
	protected Stroke cellStroke=new BasicStroke(0.25f);
	protected Stroke spreadSheetStroke=new BasicStroke(0.5f);
	protected Color cellColor=Color.GRAY;
	protected Color spreadSheetColor=Color.BLACK;

}


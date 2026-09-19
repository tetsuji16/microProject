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


import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microproject.offline_graphics.SVGRenderer;
import com.microproject.pm.graphic.graph.GraphParams;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetParams;
import com.microproject.pm.graphic.spreadsheet.renderer.FontManager;
import com.microproject.job.Job;
import com.microproject.job.JobQueue;
import com.microproject.job.JobRunnable;
import com.microproject.session.SessionFactory;
import com.microproject.strings.Messages;
import com.microproject.util.Alert;

public class GraphPageable extends PrintDocument implements ViewPrintableParams{
	private static final Logger logger = Logger.getLogger(GraphPageable.class.getName());
	protected SVGRenderer renderer;
	protected int documentRowCount,documentColCount;
	protected ViewPrintable defaultPrintable;

	public GraphPageable (SVGRenderer renderer,boolean printOnly,boolean pdfAsDefault,boolean localSettings) {
		this(renderer,false,printOnly,pdfAsDefault,localSettings);
   }
	private GraphPageable (SVGRenderer renderer,boolean pdfOnly,boolean printOnly,boolean pdfAsDefault,boolean localSettings) {
		   super(renderer.getProject(),pdfOnly,printOnly,pdfAsDefault,localSettings);
		   FontManager.setOfflineDefaultFont(FontManager.getDefaultFont());
		   this.renderer=renderer;

		   GraphParams params=renderer.getParams();

		   if (params instanceof SpreadSheetParams && params.isLeftPartVisible() && params.isRightPartVisible()){
			   params.setLeftPartVisible(showSpreadsheet);
			   params.setRightPartVisible(showGantt);
		   }else{
			   showSpreadsheet=params.isLeftPartVisible();
			   showGantt=params.isRightPartVisible();
		   }

			zoomX=scaleToSettings.getWidth();
			zoomY=scaleToSettings.isConstrainProportions()?zoomX:scaleToSettings.getHeight();
			saveZoom();
			if (!scaleToSelected){
				updateZoom(fitToSettings.getColumns(), fitToSettings.getRows());
			}

	    }


	public int getNumberOfPages() {
		return documentRowCount*documentColCount;
	}
	public PageFormat getPageFormat(int page) throws IndexOutOfBoundsException {
		return getPageFormat();
	}
	public Printable getPrintable(int page) throws IndexOutOfBoundsException {
		return getDefaultPrintable();
	}
	public ViewPrintable getSafePrintable(){
		return new ViewPrintable(new ViewPrintableParamsImpl(getTransform(),renderer.createSafePrintCopy(),documentRowCount,documentColCount,getTotalZoomX(),getTotalZoomY()));
	}

	public ExtendedPageFormat getSafePageFormat(){
		return new ExtendedPageFormat(getPageFormat());
	}

	public  PrintPreviewFrame getPrintPreviewFrame(){
		if (printPreview==null){
			printPreview = new PrintPreviewFrame(this);
		}
		return printPreview;
	}

	public void preview() {
		getPrintPreviewFrame().pack();
		getPrintPreviewFrame().setVisible(true);
	}

	public void print() {
		if (printService instanceof PDFPrintService){
			Alert.error(Messages.getString("PageSetupDialog.NotValidPrinter"));
			return;
		}
		try {
			final PrinterJob printerJob=PrinterJob.getPrinterJob();
			printerJob.setPrintService(printService);
			setPageFormat(validatePageFormat(printerJob));
			update();
			if (printPreview!=null) printPreview.updatePanel();

			ViewPrintable vp=getSafePrintable();
			vp.setPageFormat(getSafePageFormat());
			vp.update();
//			printerJob.setPageable(printable);
			printerJob.setPrintable(vp, vp.getPageFormat());

			ViewPrintable printable=getSafePrintable();
			ExtendedPageFormat pageFormat=getSafePageFormat();
			printerJob.setPrintable(printable, pageFormat);
//			printable.setPageFormat(getSafePageFormat());
//			printerJob.setPageable(printable);
			if (printerJob.printDialog()) {
				//update();


				final JobQueue jobQueue=SessionFactory.getInstance().getJobQueue();
				Job j=new Job(jobQueue,"Printing","Printing...",true,getPrintPreviewFrame());
				j.addRunnable(new JobRunnable("Printing",1.0f){
					public Object run() throws Exception{
						try {
							ViewPrintable vp=getSafePrintable();
							vp.setJr(this);
							vp.setPrinterJob(printerJob);
							vp.setPageFormat(getSafePageFormat());
							vp.update();
//							printerJob.setPageable(printable);
							printerJob.setPrintable(vp, vp.getPageFormat());
							vp.setJr(this);
							vp.setPrinterJob(printerJob);
							printerJob.print();
						}catch (PrinterException e) {
							Alert.error(e.getMessage());
						}catch (Exception e) {
							logger.log(Level.WARNING, "Graph print job failed", e);
						}
						return null;
					}
				});
				jobQueue.schedule(j);
			}
		} catch (PrinterException e) {
			Alert.error(e.getMessage());
		}
	}

	public int getColumnCount(){
		return documentColCount;
	}


	public void update(){
		PrintDocument.updatePageLayout(this,pageFormat);
	}


	public ViewPrintable getDefaultPrintable(){
		if (defaultPrintable==null) defaultPrintable=new ViewPrintable(this);
		return defaultPrintable;
	}
	public void printWithDefault(Graphics g,int page) throws PrinterException{
		getDefaultPrintable().print(g,page);
	}





	public SVGRenderer getRenderer() {
		return renderer;
	}


	public int getDocumentColCount() {
		return documentColCount;
	}


	public int getDocumentRowCount() {
		return documentRowCount;
	}
	public void setDocumentRowCount(int documentRowCount) {
		this.documentRowCount = documentRowCount;
	}
	public void setDocumentColCount(int documentColCount) {
		this.documentColCount = documentColCount;
	}

	ExtendedPrintService extendedPrintService=ExtendedPrintServiceFactory.getExtendedPrintService();
	public void updateZoom(int pw,int ph){
		double iw=pageFormat.getImageableWidth();
		double ih=pageFormat.getImageableHeight();
		SVGRenderer renderer=getRenderer();
		GraphParams params=renderer.getParams();
		double zw=extendedPrintService.getWRatio(pw, iw,params);
		double zh=ph<=0?zw:extendedPrintService.getHRatio(ph, ih,params);
		setTotalZoomX(zw);
		setTotalZoomY(zh);
		update();
	}


}


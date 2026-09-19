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

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSizeName;

import com.microproject.offline_graphics.SVGRenderer;
import com.microproject.pm.graphic.graph.GraphParams;
import com.microproject.pm.task.Project;

/**
 *
 */
public abstract class PrintDocument implements Pageable{
	public static final double DEFAULT_ZOOM=0.77;
	private static final Logger logger = Logger.getLogger(PrintDocument.class.getName());
	protected ExtendedPageFormat pageFormat;
	protected PrintService[] printServices;
	protected PrintService[] realPrintServices;
	protected PrintService printService;
	protected PDFPrintService pdfPrintService;
	//protected DocPrintJob printJob;;
	//protected PrinterJob job;
	protected PrintPreviewFrame printPreview;
	protected PrintSettings printSettings;



	boolean scaleToSelected=true;
	ScaleToSettings scaleToSettings=null;
	FitToSettings fitToSettings=null;
	boolean showSpreadsheet=true;
	boolean showGantt=true;
	protected double svgZoomX,svgZoomY;

	/**
	 *
	 */
	public PrintDocument(Project project,boolean pdfOnly,boolean printOnly, boolean pdfAsDefault,boolean localSettings) {
		printSettings=PrintSettingsManager.getSettings(localSettings?null:project);
		pdfPrintService=new PDFPrintService(); //Don't want to define a printService for PDF
		if (pdfOnly){
			realPrintServices=new PrintService[]{};
			printServices=new PrintService[]{pdfPrintService};
			setPrintService(pdfPrintService,true);
		}else{
			realPrintServices=PrintServiceLookup.lookupPrintServices(DocFlavor.SERVICE_FORMATTED.PRINTABLE, null);
			PrintService defaultService=PrintServiceLookup.lookupDefaultPrintService();
			if (realPrintServices.length==0&&defaultService!=null) realPrintServices=new PrintService[]{defaultService}; //strange but it can occur
			printServices=new PrintService[realPrintServices.length+1];
			printServices[0]=pdfPrintService;
			for (int i=0;i<realPrintServices.length;i++) printServices[i+1]=realPrintServices[i];

			if (!printOnly&&pdfAsDefault) defaultService=pdfPrintService;

			if (!printOnly&&printSettings.isPdfService()){
				defaultService=pdfPrintService;
			}else if (printSettings.getPrintServiceName()!=null){
				PrintService d=null;
				for (int i=0;i<printServices.length;i++){
					if (printSettings.getPrintServiceName().equals(printServices[i].getName())){
						d=printServices[i];
						break;
					}
				}
				if (d!=null) defaultService=d;
			}
			setPrintService(defaultService==null?pdfPrintService:defaultService,true);
		}
		List<ViewSettings> viewSettings=printSettings.getViewSettings();
		if (viewSettings!=null){
			for (ViewSettings v: viewSettings){
				if (v instanceof GanttSettings){
					GanttSettings gs=(GanttSettings)v;
					showGantt=gs.isGanttVisible();
					showSpreadsheet=gs.isSpreadSheetVisible();
				}
			}
		}

		List<ScalingSettings> scalingSettings=printSettings.getScalingSettings();
		if (scalingSettings!=null){
			int index=0;
			for (ScalingSettings s:scalingSettings){
				if (s instanceof ScaleToSettings){
					scaleToSettings=(ScaleToSettings)s;
					if (printSettings.scalingIndex==index) scaleToSelected=true;
				}
				else if (s instanceof FitToSettings){
					fitToSettings=(FitToSettings)s;
					if (printSettings.scalingIndex==index) scaleToSelected=false;
				}
				index++;
			}
		}
		if (scaleToSettings==null) scaleToSettings=new ScaleToSettings();
		if (fitToSettings==null) fitToSettings=new FitToSettings();
		if (fitToSettings.getColumns()<=0) fitToSettings.setColumns(1);
		if (printSettings.isEmpty()){
			scaleToSelected=false;
			fitToSettings.setColumns(1);
			fitToSettings.setRows(FitToSettings.AUTOMATIC);
		}


	}


	public PrintService[] getPrintServices() {
		return printServices;
	}


	public PrintService getPrintService() {
		return printService;
	}


	public void setPrintService(PrintService printService,boolean useDefautSettings) {
		//boolean pageFormatAlreadyExists=true;
		this.printService = printService;
		if (useDefautSettings&&printSettings!=null&&pageFormat!=null){
			ExtendedPageFormat newPageFormat=printSettings.getPageFormat();
			if (newPageFormat!=null) newPageFormat.copy(pageFormat);
		}
		if (pageFormat==null){
			try{
				if (printSettings.isEmpty()||printSettings.getPageFormat()==null){
						MediaSizeName mediaSizeName=getDefaultMediaSizeName();
						MediaPrintableArea mediaPrintableArea=getDefaultMediaPrintableArea(mediaSizeName);
						pageFormat=new ExtendedPageFormat(mediaSizeName,mediaPrintableArea);
				}else{
					pageFormat=(ExtendedPageFormat)printSettings.getPageFormat().clone();
				}
				//pageFormatAlreadyExists=false;
			}catch (Exception e) {logger.log(Level.WARNING, "Failed to initialize page format", e);}
		}
		if (pageFormat==null){
			pageFormat=new ExtendedPageFormat();
			//pageFormatAlreadyExists=false;
		}
//		if (!pageFormatAlreadyExists){
//			pageFormat.setOrientation(PageFormat.LANDSCAPE);
//		}

		if (printSettings.isEmpty()){
			printSettings.setPdfService(printService instanceof PDFPrintService);
			printSettings.setPrintServiceName(printService.getName());
			printSettings.setPageFormat(pageFormat);
			printSettings.setEmpty(false);
		}
	}

	public MediaSizeName getDefaultMediaSizeName(){
		return ExtendedPageFormat.getDefaultMediaSizeName(printService);
	}
	public MediaPrintableArea getDefaultMediaPrintableArea(MediaSizeName mediaSizeName){
		return ExtendedPageFormat.getDefaultMediaPrintableArea(printService,mediaSizeName);
	}


	public ExtendedPageFormat getPageFormat() {
		return pageFormat;
	}
	public void setPageFormat(ExtendedPageFormat pageFormat) {
		if (pageFormat == null) return;
		if (this.pageFormat == null) this.pageFormat=(ExtendedPageFormat)pageFormat.clone();
		else pageFormat.copy(this.pageFormat);
	}

	public abstract void print();

	public int getColumnCount(){
		return 1;
	}



	public void update(){}

	public static void updatePageLayout(ViewPrintableParams printableParams,PageFormat pageFormat){
		SVGRenderer renderer=printableParams.getRenderer();
		GraphParams params=renderer.getParams();
		double zx=printableParams.getTotalZoomX();
		double zy=printableParams.getTotalZoomY();
		int pageW=(int)Math.ceil((pageFormat.getImageableWidth()-1)/zx);
		int pageH=(int)Math.ceil((pageFormat.getImageableHeight()-1)/zy);
		params.setPrintBounds(new Rectangle(0,0,pageW,pageH));
		printableParams.setDocumentColCount(params.getPrintCols());
		printableParams.setDocumentRowCount(params.getPrintRows());
	}

	protected double zoomX=1.0;
	public double getZoomX() {
		return zoomX;
	}
	public void setZoomX(double zoomX) {
		this.zoomX = zoomX;
	}
	protected double zoomY=1.0;
	public double getZoomY() {
		return zoomY;
	}
	public void setZoomY(double zoomY) {
		this.zoomY = zoomY;
	}

	public double getTotalZoomX(){
		return zoomX*DEFAULT_ZOOM;
	}
	public void setTotalZoomX(double z){
		zoomX=z/DEFAULT_ZOOM;
	}
	public double getTotalZoomY(){
		return zoomY*DEFAULT_ZOOM;
	}
	public void setTotalZoomY(double z){
		zoomY=z/DEFAULT_ZOOM;
	}

	public AffineTransform getTransform(){
		return new AffineTransform(zoomX*DEFAULT_ZOOM,0.0,0.0,zoomY*DEFAULT_ZOOM,pageFormat.getImageableX(),pageFormat.getImageableY());
	}


	public PrintSettings getPrintSettings() {
		return printSettings;
	}


	public FitToSettings getFitToSettings() {
		return fitToSettings;
	}


	public void setFitToSettings(FitToSettings fitToSettings) {
		this.fitToSettings = fitToSettings;
	}


	public boolean isScaleToSelected() {
		return scaleToSelected;
	}


	public void setScaleToSelected(boolean scaleToSelected) {
		this.scaleToSelected = scaleToSelected;
	}


	public ScaleToSettings getScaleToSettings() {
		return scaleToSettings;
	}


	public void setScaleToSettings(ScaleToSettings scaleToSettings) {
		this.scaleToSettings = scaleToSettings;
	}


	public boolean isShowGantt() {
		return showGantt;
	}


	public void setShowGantt(boolean showGantt) {
		this.showGantt = showGantt;
	}


	public boolean isShowSpreadsheet() {
		return showSpreadsheet;
	}


	public void setShowSpreadsheet(boolean showSpreadsheet) {
		this.showSpreadsheet = showSpreadsheet;
	}


	public double getSvgZoomX() {
		return svgZoomX;
	}



	public double getSvgZoomY() {
		return svgZoomY;
	}


	public void saveZoom() {
		svgZoomX=zoomX;
		svgZoomY=zoomY;
	}
	public void restoreZoom() {
		zoomX=svgZoomX;
		zoomY=svgZoomY;
	}

	public ExtendedPageFormat validatePageFormat(PrinterJob printerJob) throws PrinterException {
		ExtendedPageFormat validated=(ExtendedPageFormat)getPageFormat().clone();
		PageFormat validatedPageFormat=printerJob.validatePage(validated);
		if (!(validatedPageFormat instanceof ExtendedPageFormat)){
			validated.setOrientation(validatedPageFormat.getOrientation());
			validated.setPrintableArea(new MediaPrintableArea(
				(float)(validatedPageFormat.getImageableX()/PageSize.POINTS_PER_INCH*PageSize.INCH),
				(float)(validatedPageFormat.getImageableY()/PageSize.POINTS_PER_INCH*PageSize.INCH),
				(float)(validatedPageFormat.getImageableWidth()/PageSize.POINTS_PER_INCH*PageSize.INCH),
				(float)(validatedPageFormat.getImageableHeight()/PageSize.POINTS_PER_INCH*PageSize.INCH),
				MediaPrintableArea.INCH));
		}
		return validated;
	}


	public PDFPrintService getPdfPrintService() {
		return pdfPrintService;
	}




}

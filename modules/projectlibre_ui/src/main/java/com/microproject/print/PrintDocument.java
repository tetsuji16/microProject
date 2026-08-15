/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License 
 * Version 1.0 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public 
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of 
 * software over a computer network and provide for limited attribution for the 
 * Original Developer. In addition, Exhibit A has been modified to be consistent 
 * with Exhibit B. 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the 
 * specific language governing rights and limitations under the License. The 
 * Original Code is ProjectLibre. The Original Developer is the Initial Developer 
 * and is ProjectLibre Inc. All portions of the code written by ProjectLibre are 
 * Copyright (c) 2012-2019. All Rights Reserved. All portions of the code written by 
 * ProjectLibre are Copyright (c) 2012-2019. All Rights Reserved. Contributor 
 * ProjectLibre, Inc.
 *
 * Alternatively, the contents of this file may be used under the terms of the 
 * ProjectLibre End-User License Agreement (the ProjectLibre License) in which case 
 * the provisions of the ProjectLibre License are applicable instead of those above. 
 * If you wish to allow use of your version of this file only under the terms of the 
 * ProjectLibre License and not to allow others to use your version of this file 
 * under the CPAL, indicate your decision by deleting the provisions above and 
 * replace them with the notice and other provisions required by the ProjectLibre 
 * License. If you do not delete the provisions above, a recipient may use your 
 * version of this file under either the CPAL or the ProjectLibre Licenses. 
 *
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text of the notices 
 * in the Source Code files of the Original Code. You should use the text of this 
 * Exhibit A rather than the text found in the Original Code Source Code for Your 
 * Modifications.] 
 *
 * EXHIBIT B. Attribution Information for ProjectLibre required
 *
 * Attribution Copyright Notice: Copyright (c) 2012-2019, ProjectLibre, Inc.
 * Attribution Phrase (not exceeding 10 words): 
 * ProjectLibre, open source project management software.
 * Attribution URL: http://www.projectlibre.com
 * Graphic Image as provided in the Covered Code as file: projectlibre-logo.png with 
 * alternatives listed on http://www.projectlibre.com/logo 
 *
 * Display of Attribution Information is required in Larger Works which are defined 
 * in the CPAL as a work which combines Covered Code or portions thereof with code 
 * not governed by the terms of the CPAL. However, in addition to the other notice 
 * obligations, all copies of the Covered Code in Executable and Source Code form 
 * distributed must, as a form of attribution of the original author, include on 
 * each user interface screen the "ProjectLibre" logo visible to all users. 
 * The ProjectLibre logo should be located horizontally aligned with the menu bar 
 * The 
 * logo must be at least 144 x 31 pixels. When users click on the "ProjectLibre" 
 * logo it must direct them back to http://www.projectlibre.com. 
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
		//if (printSettings==null) System.out.println("PrintSettings: null");
		//else System.out.println("PrintSettings: "+printSettings.getPrintServiceName()+", "+printSettings.getPageFormat().getSizeName()+", "+printSettings.getPageFormat().getOrientation()+", "+printSettings.getPageFormat().getSize()+", "+printSettings.getPageFormat().getPrintableArea());
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


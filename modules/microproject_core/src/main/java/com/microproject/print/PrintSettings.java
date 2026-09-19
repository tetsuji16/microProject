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

import java.util.ArrayList;

import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.workspace.SavableToWorkspace;
import com.microproject.workspace.WorkspaceSetting;

public class PrintSettings implements WorkspaceSetting,Cloneable{
	static final long serialVersionUID = 78672828119291L;
	protected ExtendedPageFormat pageFormat;
	protected boolean pdfService=false;
	protected transient String printServiceName;
	protected ArrayList<ViewSettings> viewSettings;
	protected ArrayList<ScalingSettings> scalingSettings;
	protected int scalingIndex;
	protected transient SpreadSheetFieldArray fieldArray;
	protected transient boolean empty;
	public WorkspaceSetting spreadsheetWorkspace;


	public ExtendedPageFormat getPageFormat() {
		return pageFormat;
	}

	public void setPageFormat(ExtendedPageFormat pageFormat) {
		this.pageFormat = pageFormat;
	}

	public boolean isPdfService() {
		return pdfService;
	}

	public void setPdfService(boolean pdfService) {
		this.pdfService = pdfService;
	}

	public String getPrintServiceName() {
		return printServiceName;
	}

	public void setPrintServiceName(String printServiceName) {
		this.printServiceName = printServiceName;
	}

	public Object clone(){
		try {
			PrintSettings c=(PrintSettings)super.clone();
			if (pageFormat!=null) c.pageFormat=(ExtendedPageFormat)pageFormat.clone();
			if (c.viewSettings!=null){
				c.viewSettings=new ArrayList<ViewSettings>(viewSettings.size());
				for (ViewSettings s: viewSettings) c.viewSettings.add((ViewSettings)s.clone());
			}
			if (c.scalingSettings!=null){
				c.scalingSettings=new ArrayList<ScalingSettings>(scalingSettings.size());
				for (ScalingSettings s: scalingSettings) c.scalingSettings.add((ScalingSettings)s.clone());
			}
			return c;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError("PrintSettings must remain Cloneable", e);
		}
	}

	public ArrayList<ScalingSettings> getScalingSettings() {
		return scalingSettings;
	}

	public ArrayList<ViewSettings> getViewSettings() {
		return viewSettings;
	}

	public int getScalingIndex() {
		return scalingIndex;
	}

	public void setScalingIndex(int scalingIndex) {
		this.scalingIndex = scalingIndex;
	}

	public void setScalingSettings(ArrayList<ScalingSettings> scalingSettings) {
		this.scalingSettings = scalingSettings;
	}

	public void setViewSettings(ArrayList<ViewSettings> viewSettings) {
		this.viewSettings = viewSettings;
	}

	public SpreadSheetFieldArray getFieldArray() {
		return fieldArray;
	}

	public void setFieldArray(SpreadSheetFieldArray fieldArray) {
		this.fieldArray = fieldArray;
	}


	public void init(){
		if (spreadsheetWorkspace != null) {
			fieldArray = new SpreadSheetFieldArray();
			fieldArray.setCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			fieldArray.restoreWorkspace(spreadsheetWorkspace, SavableToWorkspace.PERSIST);
			fieldArray.setName("PrintSettings");
			//Dictionary.add(fieldArray);
		}

	}
	public void updateWorkspace(){
		if (fieldArray != null)
			spreadsheetWorkspace = fieldArray.createWorkspace(SavableToWorkspace.PERSIST);

	}

	public boolean isEmpty() {
		return empty;
	}

	public void setEmpty(boolean empty) {
		this.empty = empty;
	}

}

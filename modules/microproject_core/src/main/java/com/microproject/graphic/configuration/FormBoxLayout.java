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

import java.awt.Font;

import javax.swing.plaf.FontUIResource;

import com.microproject.util.Environment;
import com.microproject.util.FontUtil;




public class FormBoxLayout {
	boolean defaultZoom=false;
	String columnGrid=null;
	String rowGrid=null;
	String border=null;
	String titleFont=null,labelFont=null,valueFont=null;
	public String getBorder() {
		return border;
	}
	public void setBorder(String border) {
		this.border = border;
	}
	public String getColumnGrid() {
		return columnGrid;
	}
	public void setColumnGrid(String columnGrid) {
		this.columnGrid = columnGrid;
	}
	public String getLabelFont() {
		return labelFont;
	}
	public void setLabelFont(String labelFont) {
		this.labelFont = labelFont;
	}
	public String getRowGrid() {
		return rowGrid;
	}
	public void setRowGrid(String rowGrid) {
		this.rowGrid = rowGrid;
	}
	public String getTitleFont() {
		return titleFont;
	}
	public void setTitleFont(String titleFont) {
		this.titleFont = titleFont;
	}
	public String getValueFont() {
		return valueFont;
	}
	public void setValueFont(String valueFont) {
		this.valueFont = valueFont;
	}
	public boolean isDefaultZoom() {
		return defaultZoom;
	}
	public void setDefaultZoom(boolean defaultZoom) {
		this.defaultZoom = defaultZoom;
	}
	
	
	
	public Font getFont(String type){
		String fontName;
		if ("title".equals(type)) fontName=titleFont;
		else if ("label".equals(type)) fontName=labelFont;
		else if ("value".equals(type)) fontName=valueFont;
		else return null;
		return FontUtil.getFont(fontName, Environment.NETWORK_FONT);
	}
	
	
	
}

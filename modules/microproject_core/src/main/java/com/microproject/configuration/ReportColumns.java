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
package com.microproject.configuration;

import com.microproject.graphic.configuration.SpreadSheetFieldArray;


/**
 * Holds the definition of report columns that is read in from the view config
 */
public class ReportColumns implements Cloneable {
	private String idSpreadSheet = null;
	private String categorySpreadSheet = null;
	private String groupbyField = null;
	/**
	 * @return Returns the groupbyField.
	 */
	public String getGroupbyField() {
		return groupbyField;
	}
	/**
	 * @param groupbyField The groupbyField to set.
	 */
	public void setGroupbyField(String groupbyField) {
		this.groupbyField = groupbyField;
	}
	/**
	 * @return Returns the idSpreadSheet.
	 */
	public String getIdSpreadSheet() {
		return idSpreadSheet;
	}
	/**
	 * @param idSpreadSheet The idSpreadSheet to set.
	 */
	public void setIdSpreadSheet(String idSpreadSheet) {
		this.idSpreadSheet = idSpreadSheet;
	}
	
	public String toString() {
		return idSpreadSheet;
	}
	/**
	 * @return Returns the categorySpreadSheet.
	 */
	public String getCategorySpreadSheet() {
		return categorySpreadSheet;
	}
	/**
	 * @param categorySpreadSheet The categorySpreadSheet to set.
	 */
	public void setCategorySpreadSheet(String categorySpreadSheet) {
		this.categorySpreadSheet = categorySpreadSheet;
	}
	
	public SpreadSheetFieldArray getFieldArray() {
		return SpreadSheetFieldArray.getFromId(getCategorySpreadSheet(), getIdSpreadSheet());
	}
}

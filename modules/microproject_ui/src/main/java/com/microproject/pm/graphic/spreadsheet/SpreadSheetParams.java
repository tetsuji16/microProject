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
import java.util.Iterator;
import java.util.List;

import com.microproject.pm.graphic.gantt.GanttParams;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetParamsImpl.PageInfo;
import com.microproject.pm.graphic.spreadsheet.common.SpreadSheetRowHeaderColumnModel;
import com.microproject.field.FieldContext;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;

public interface SpreadSheetParams extends GanttParams {
	public String getSpreadsheetCategory();
	public SpreadSheetFieldArray getFieldArray();
	public void setFieldArray(SpreadSheetFieldArray fieldArray,List<Integer> colWidth);
	public FieldContext getFieldContext();
	public Rectangle getSpreadSheetBounds();
	public SpreadSheetColumnModel getColumnModel();
	public SpreadSheetRowHeaderColumnModel getHeaderColumnModel();
	public Iterator getColumnIterator();

	public PageInfo getRowPageInfo(int row);
	public PageInfo getColPageInfo(int col);
}


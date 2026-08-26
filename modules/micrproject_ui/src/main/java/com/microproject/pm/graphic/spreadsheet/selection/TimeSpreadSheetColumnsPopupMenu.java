/*
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
 * furnished to do so subject to the following conditions:
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
 */
package com.microproject.pm.graphic.spreadsheet.selection;

import java.util.ArrayList;

import com.microproject.configuration.Dictionary;
import com.microproject.field.Field;
import com.microproject.pm.graphic.spreadsheet.time.TimeSpreadSheet;

/**
 * Time-phased spreadsheet column picker: each {@link Field} of the time
 * spreadsheet's first Dictionary layout is a toggleable column, and selecting
 * one adds it to the visible field array. Shares the menu-building logic with
 * {@link SpreadSheetColumnsPopupMenu}.
 */
public class TimeSpreadSheetColumnsPopupMenu extends SpreadSheetColumnsPopupMenu {
	public TimeSpreadSheetColumnsPopupMenu(TimeSpreadSheet spreadSheet, String type) {
		super(spreadSheet, type);
	}

	@Override
	protected Object[] getColumnDefinitions() {
		// The time spreadsheet stores its fields as the first Dictionary layout.
		return ((ArrayList<?>) Dictionary.getAll(type)[0]).toArray();
	}

	@Override
	protected boolean isSelected(Object item) {
		return ((TimeSpreadSheet) spreadSheet).getSelectedFieldArray().contains(item);
	}

	@Override
	protected void applySelection(Object item) {
		((TimeSpreadSheet) spreadSheet).selectFieldArray((Field) item);
	}
}

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
package com.microproject.pm.graphic.views;

import com.microproject.configuration.Dictionary;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.strings.Messages;

final class SpreadsheetViewSupport {
	private SpreadsheetViewSupport() {
	}

	static SpreadSheetFieldArray getProjectFields() {
		return (SpreadSheetFieldArray) Dictionary.get(SpreadSheetCategories.projectSpreadsheetCategory,
				Messages.getString("Spreadsheet.Project.default")); //$NON-NLS-1$
	}

	static SpreadSheetFieldArray getResourceFields() {
		return (SpreadSheetFieldArray) Dictionary.get(SpreadSheetCategories.resourceSpreadsheetCategory,
				Messages.getString("Spreadsheet.Resource.entryWorkResources")); //$NON-NLS-1$
	}

	static SpreadSheetFieldArray getTaskFields() {
		return getTaskFields("Spreadsheet.Task.entry"); //$NON-NLS-1$
	}

	static SpreadSheetFieldArray getTaskFields(String messageKey) {
		return (SpreadSheetFieldArray) Dictionary.get(SpreadSheetCategories.taskSpreadsheetCategory,
				Messages.getString(messageKey));
	}

	static SpreadSheetFieldArray resolveTaskFields(SpreadSheetFieldArray projectFields) {
		SpreadSheetFieldArray fields = projectFields != null ? projectFields : getTaskFields();
		return fields != null ? fields : new SpreadSheetFieldArray();
	}

	static void cleanup(SpreadSheet spreadSheet) {
		if (spreadSheet != null) {
			spreadSheet.cleanUp();
		}
	}
}

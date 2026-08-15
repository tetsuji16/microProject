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

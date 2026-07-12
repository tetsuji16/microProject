package com.projectlibre1.pm.graphic.views;

import com.projectlibre1.configuration.Dictionary;
import com.projectlibre1.graphic.configuration.SpreadSheetFieldArray;
import com.projectlibre1.graphic.configuration.SpreadSheetCategories;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheet;
import com.projectlibre1.strings.Messages;

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

	static void cleanup(SpreadSheet spreadSheet) {
		if (spreadSheet != null) {
			spreadSheet.cleanUp();
		}
	}
}

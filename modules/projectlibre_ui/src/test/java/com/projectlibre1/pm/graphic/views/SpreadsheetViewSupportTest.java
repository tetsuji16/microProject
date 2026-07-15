package com.projectlibre1.pm.graphic.views;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.projectlibre1.graphic.configuration.SpreadSheetFieldArray;

class SpreadsheetViewSupportTest {
	@Test
	void defaultTaskFieldsAlwaysResolveToDefinition() {
		assertNotNull(SpreadsheetViewSupport.resolveTaskFields(null));
	}

	@Test
	void projectTaskFieldsTakePrecedenceOverDictionaryDefaults() {
		SpreadSheetFieldArray projectFields = new SpreadSheetFieldArray();

		assertSame(projectFields, SpreadsheetViewSupport.resolveTaskFields(projectFields));
	}
}

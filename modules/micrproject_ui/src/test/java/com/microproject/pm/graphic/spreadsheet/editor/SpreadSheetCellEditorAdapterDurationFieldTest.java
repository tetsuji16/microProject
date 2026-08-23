/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.editor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microproject.field.Field;

import org.junit.jupiter.api.Test;

class SpreadSheetCellEditorAdapterDurationFieldTest {
	@Test void percentColumnIsNotParsedAsDurationWhenNextToADurationColumn() {
		Field percent = new Field();
		percent.setPercent(true);
		Field duration = new Field();
		duration.setDuration(true);
		Field[] fields = { percent, duration };

		assertFalse(SpreadSheetCellEditorAdapter.isDurationField(0, index -> fields[index], index -> fields[index]));
		assertTrue(SpreadSheetCellEditorAdapter.isDurationField(1, index -> fields[index], index -> fields[index]));
	}
}

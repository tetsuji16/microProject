/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.spreadsheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.spreadsheet.SpreadSheet.FillDirection;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet.FillStep;

class SpreadSheetFillPlanTest {
	@Test
	void fillDownCopiesTheFirstSelectedRowIntoTheRest() {
		assertEquals(List.of(new FillStep(2, 1, 3, 1), new FillStep(2, 1, 4, 1)),
			SpreadSheet.planFill(new int[] {4, 2, 3}, new int[] {1}, FillDirection.DOWN));
	}

	@Test
	void fillUpCopiesTheLastSelectedRowIntoTheRest() {
		assertEquals(List.of(new FillStep(4, 0, 2, 0), new FillStep(4, 0, 3, 0)),
			SpreadSheet.planFill(new int[] {2, 3, 4}, new int[] {0}, FillDirection.UP));
	}

	@Test
	void fillRightAndLeftWorkAlongTheSelectedColumns() {
		assertEquals(List.of(new FillStep(5, 1, 5, 2), new FillStep(5, 1, 5, 3)),
			SpreadSheet.planFill(new int[] {5}, new int[] {3, 1, 2}, FillDirection.RIGHT));
		assertEquals(List.of(new FillStep(5, 3, 5, 1), new FillStep(5, 3, 5, 2)),
			SpreadSheet.planFill(new int[] {5}, new int[] {1, 2, 3}, FillDirection.LEFT));
	}

	@Test
	void fillCoversEverySelectedColumnAndPlansNothingForASingleLine() {
		assertEquals(2, SpreadSheet.planFill(new int[] {0, 1}, new int[] {0, 1}, FillDirection.DOWN).size());
		assertTrue(SpreadSheet.planFill(new int[] {3}, new int[] {0, 1}, FillDirection.DOWN).isEmpty());
		assertTrue(SpreadSheet.planFill(new int[] {3}, new int[] {0, 1}, FillDirection.UP).isEmpty());
		assertTrue(SpreadSheet.planFill(new int[] {0, 1}, new int[] {2}, FillDirection.RIGHT).isEmpty());
		assertTrue(SpreadSheet.planFill(new int[] {}, new int[] {}, FillDirection.DOWN).isEmpty());
	}
}

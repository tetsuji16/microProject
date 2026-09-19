/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

import com.microproject.field.Field;

class ColumnDialogTest {
	@Test
	void filtersByDisplayNameOrIdAndOmitsAlreadyVisibleFields() {
		Field duration = field("Field.duration", "Duration");
		Field displayOnTimeline = field("Field.displayOnTimeline", "Display on Timeline");
		Field cost = field("Field.cost", "Cost");

		assertEquals(List.of(displayOnTimeline),
				ColumnDialog.filterFields(List.of(duration, displayOnTimeline, cost), List.of(duration), "timeline"));
		assertEquals(List.of(cost),
				ColumnDialog.filterFields(List.of(duration, displayOnTimeline, cost), List.of(), "FIELD.COST"));
	}

	@Test
	void laysOutFilterAndFieldChoiceOnSeparateRows() {
		JTextField filter = new JTextField();
		JComboBox<Field> fields = new JComboBox<>();

		JComponent panel = ColumnDialog.buildContentPanel(filter, fields);

		assertEquals(panel, filter.getParent());
		assertEquals(panel, fields.getParent());
	}

	private static Field field(String id, String name) {
		Field field = new Field();
		field.setId(id);
		field.setName(name);
		return field;
	}
}

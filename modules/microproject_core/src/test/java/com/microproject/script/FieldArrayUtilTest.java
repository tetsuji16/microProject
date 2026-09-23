/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

import com.microproject.field.Field;
import com.microproject.graphic.configuration.SpreadSheetFieldArray;

class FieldArrayUtilTest {
	@Test
	void removesWebExcludedFieldsFromACloneAndKeepsNonProjectIds() {
		SpreadSheetFieldArray configured = new SpreadSheetFieldArray();
		configured.setCategory(FieldArrayUtil.projectFieldArrayCategory);
		Field indicators = field("Field.indicators");
		Field id = field("Field.id");
		Field name = field("Field.name");
		configured.add(indicators);
		configured.add(id);
		configured.add(name);

		SpreadSheetFieldArray webFields = FieldArrayUtil.removeNonWebFields(configured);

		assertNotSame(configured, webFields);
		assertEquals(3, configured.size());
		assertEquals(java.util.List.of(name), webFields);
	}

	@Test
	void removesIndicatorsButKeepsIdForOtherCategories() {
		SpreadSheetFieldArray configured = new SpreadSheetFieldArray();
		configured.setCategory(FieldArrayUtil.taskFieldArrayCategory);
		Field indicators = field("Field.indicators");
		Field id = field("Field.id");
		configured.add(indicators);
		configured.add(id);

		assertEquals(java.util.List.of(id), FieldArrayUtil.removeNonWebFields(configured));
	}

	private static Field field(String id) {
		Field field = new Field();
		field.setId(id);
		return field;
	}
}

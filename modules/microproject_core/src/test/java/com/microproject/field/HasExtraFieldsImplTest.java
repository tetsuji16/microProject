package com.microproject.field;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class HasExtraFieldsImplTest {
	@Test
	void extraFieldsRetainInsertionOrderAndHeterogeneousValues() {
		HasExtraFieldsImpl extraFields = new HasExtraFieldsImpl();
		extraFields.getExtraFields().put("customText", "value");
		extraFields.getExtraFields().put("customNumber", 12);

		assertEquals(List.of("customText", "customNumber"), List.copyOf(extraFields.getExtraFields().keySet()));
		assertEquals("value", extraFields.getExtraFields().get("customText"));
		assertEquals(12, extraFields.getExtraFields().get("customNumber"));
	}
}
